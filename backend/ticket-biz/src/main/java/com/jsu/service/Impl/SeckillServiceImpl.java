package com.jsu.service.Impl;

import com.jsu.common.constant.RedisKey;
import com.jsu.common.exception.BusinessException;
import com.jsu.common.result.Result;
import com.jsu.common.result.ResultCode;
import com.jsu.entity.Order;
import com.jsu.entity.SeckillSession;
import com.jsu.entity.Ticket;
import com.jsu.mapper.SeckillSessionMapper;
import com.jsu.mq.producer.MessageProducer;
import com.jsu.service.SeckillService;
import com.jsu.service.ShowService;
import com.jsu.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 秒杀服务实现类
 *
 * 核心方案：
 * 1. 预热缓存：秒杀开始前主动调用 warmUpStock 将DB库存同步到Redis
 * 2. Lua脚本：扣减库存 + 限购校验原子执行
 * 3. MQ异步：削峰填谷创建订单
 *
 * 预热是"事前动作"，不在秒杀热路径中自动执行。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillSessionMapper seckillSessionMapper;
    private final TicketService ticketService;
    private final ShowService showService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final MessageProducer messageProducer;
    private final DefaultRedisScript<Long> decreaseStockScript;
    private final DefaultRedisScript<Long> seckillLockScript;

    /**
     * 为场次附加详情、Redis实时库存和预热状态
     */
    private void attachSession(SeckillSession session) {
        if (session == null) return;

        if (session.getShowId() != null) {
            session.setShow(showService.getById(session.getShowId()));
        }
        if (session.getTicketId() != null) {
            session.setTicket(ticketService.getById(session.getTicketId()));

            String stockKey = RedisKey.format(RedisKey.SECKILL_STOCK, session.getId());
            String warmupKey = RedisKey.format(RedisKey.SECKILL_WARMUP, session.getId());

            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(warmupKey))) {
                // 已预热：展示 Redis 实时库存
                String redisStock = stringRedisTemplate.opsForValue().get(stockKey);
                session.setStock(redisStock != null ? Integer.parseInt(redisStock) : 0);
                session.setWarmedUp(true);
            } else {
                // 未预热：展示 DB 原始库存
                session.setWarmedUp(false);
            }
        }
    }

    /**
     * 预热秒杀库存到Redis
     */
    @Override
    public int warmUpStock(Long sessionId) {
        SeckillSession session = seckillSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("抢购场次不存在");
        }
        Ticket ticket = ticketService.getById(session.getTicketId());
        if (ticket == null) {
            throw new BusinessException("关联票档不存在");
        }
        int actualStock = session.getStock() != null ? session.getStock() : 0;
        // 限制：预热库存不能超过票档可用库存
        if (ticket.getAvailableStock() != null && actualStock > ticket.getAvailableStock()) {
            log.warn("秒杀库存({})超过票档可用库存({})，已自动限制: sessionId={}",
                    actualStock, ticket.getAvailableStock(), sessionId);
            actualStock = ticket.getAvailableStock();
            session.setStock(actualStock);
            seckillSessionMapper.updateById(session);
        }

        String stockKey = RedisKey.format(RedisKey.SECKILL_STOCK, sessionId);
        String warmupKey = RedisKey.format(RedisKey.SECKILL_WARMUP, sessionId);
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(actualStock));
        stringRedisTemplate.opsForValue().set(warmupKey, "1");
        log.info("预热成功: sessionId={}, ticketId={}, stock={}", sessionId, session.getTicketId(), actualStock);
        return actualStock;
    }

    /**
     * 批量预热所有即将开始的秒杀场次
     */
    @Override
    public int batchWarmUp() {
        List<SeckillSession> upcoming = seckillSessionMapper.selectUpcomingSessions();
        if (upcoming.isEmpty()) {
            log.info("没有即将开始的秒杀场次需要预热");
            return 0;
        }
        int count = 0;
        for (SeckillSession session : upcoming) {
            try {
                warmUpStock(session.getId());
                count++;
            } catch (Exception e) {
                log.error("批量预热失败: sessionId={}", session.getId(), e);
            }
        }
        log.info("批量预热完成: 成功 {} / 总计 {}", count, upcoming.size());
        return count;
    }

    /**
     * 执行秒杀
     */
    @Override
    public Result<?> seckill(Long sessionId, Long userId, int quantity) {
        try {
            if (sessionId == null || userId == null || quantity < 1 || quantity > 2) {
                return Result.fail("请求参数无效");
            }
            SeckillSession session = seckillSessionMapper.selectById(sessionId);
            if (session == null) {
                return Result.fail(ResultCode.NOT_FOUND);
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(session.getStartTime())) {
                return Result.fail(ResultCode.SECKILL_NOT_START);
            }
            if (now.isAfter(session.getEndTime())) {
                return Result.fail(ResultCode.SECKILL_FINISHED);
            }

            // 检查预热库存
            String stockKey = RedisKey.format(RedisKey.SECKILL_STOCK, sessionId);
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
                log.warn("秒杀场次未预热: sessionId={}", sessionId);
                return Result.fail(ResultCode.STOCK_INSUFFICIENT);
            }

            // 分布式锁防同一用户重复请求
            String lockKey = RedisKey.format(RedisKey.SECKILL_LOCK, sessionId, userId);
            Long lockResult = stringRedisTemplate.execute(
                    seckillLockScript,
                    Arrays.asList(lockKey),
                    userId.toString(),
                    "10000"
            );
            if (lockResult == null || lockResult == 0) {
                return Result.fail("请勿重复抢购");
            }

            try {
                // Lua脚本：扣减库存 + 限购校验
                String limitKey = RedisKey.format(RedisKey.SECKILL_LIMIT, sessionId, userId);
                Long result = stringRedisTemplate.execute(
                        decreaseStockScript,
                        Arrays.asList(stockKey, limitKey),
                        String.valueOf(quantity),
                        userId.toString(),
                        "2"
                );

                if (result == null) return Result.fail("抢购失败");
                if (result == -1) return Result.fail(ResultCode.STOCK_INSUFFICIENT);
                if (result == -3) return Result.fail(ResultCode.EXCEED_LIMIT);

                Ticket ticket = ticketService.getById(session.getTicketId());
                if (ticket == null) return Result.fail("票档不存在");

                Order order = new Order();
                // Producer侧生成订单号，保证MQ重试时幂等
                order.setOrderNo(com.jsu.common.util.StringUtils.generateOrderNo());
                order.setId(-1L);
                order.setSessionId(sessionId);
                order.setUserId(userId);
                order.setShowId(session.getShowId());
                order.setTicketId(session.getTicketId());
                order.setQuantity(quantity);
                order.setTotalAmount(session.getSeckillPrice() != null
                    ? session.getSeckillPrice().multiply(BigDecimal.valueOf(quantity))
                    : ticket.getPrice().multiply(BigDecimal.valueOf(quantity)));

                log.info("发送秒杀订单消息: userId={}, showId={}, ticketId={}, quantity={}",
                        userId, session.getShowId(), session.getTicketId(), quantity);
                messageProducer.sendOrderCreateMessage(order);
                return Result.success(Map.of("requestId", order.getOrderNo(), "status", "ACCEPTED"));
            } finally {
                stringRedisTemplate.delete(lockKey);
            }
        } catch (Exception e) {
            log.error("抢购异常: sessionId={}, userId={}", sessionId, userId, e);
            return Result.fail("系统异常: " + e.getMessage());
        }
    }

    @Override
    public List<SeckillSession> getActiveSessions() {
        List<SeckillSession> list = seckillSessionMapper.selectActiveSessions();
        list.forEach(this::attachSession);
        return list;
    }

    @Override
    public List<SeckillSession> getUpcomingSessions() {
        List<SeckillSession> list = seckillSessionMapper.selectUpcomingSessions();
        list.forEach(this::attachSession);
        return list;
    }

    @Override
    public List<SeckillSession> getAllSessions() {
        List<SeckillSession> list = seckillSessionMapper.selectAll();
        list.forEach(this::attachSession);
        return list;
    }

    @Override
    public SeckillSession getById(Long id) {
        SeckillSession session = seckillSessionMapper.selectById(id);
        attachSession(session);
        return session;
    }

    @Override
    public void create(SeckillSession session) {
        seckillSessionMapper.insert(session);
    }

    /**
     * 更新场次
     * 如果该场次已预热，则清除Redis缓存，运营需重新预热
     */
    @Override
    @Transactional
    public void update(SeckillSession session) {
        seckillSessionMapper.updateById(session);
        // 清除Redis预热缓存
        String stockKey = RedisKey.format(RedisKey.SECKILL_STOCK, session.getId());
        String warmupKey = RedisKey.format(RedisKey.SECKILL_WARMUP, session.getId());
        stringRedisTemplate.delete(stockKey);
        stringRedisTemplate.delete(warmupKey);
        // 自动重新预热（更新后立即生效，无需手动操作）
        try {
            warmUpStock(session.getId());
            log.info("场次已更新并自动重新预热: sessionId={}", session.getId());
        } catch (Exception e) {
            log.warn("场次更新后自动预热失败，可稍后手动预热: sessionId={}", session.getId(), e);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 删除时也清除Redis缓存
        String stockKey = RedisKey.format(RedisKey.SECKILL_STOCK, id);
        String warmupKey = RedisKey.format(RedisKey.SECKILL_WARMUP, id);
        stringRedisTemplate.delete(stockKey);
        stringRedisTemplate.delete(warmupKey);
        seckillSessionMapper.deleteById(id);
    }
}

