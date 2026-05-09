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

/**
 * 秒杀服务实现类
 * 核心功能：基于Redis+Lua脚本实现高并发秒杀
 *
 * 技术方案：
 * 1. Redis预热库存 → 减少数据库压力
 * 2. Lua脚本原子操作 → 保证库存扣减和限购校验的原子性
 * 3. 分布式锁 → 防止用户重复抢购
 * 4. MQ异步创建订单 → 削峰填谷
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
    private final DefaultRedisScript<Long> decreaseStockScript;  // 库存扣减Lua脚本
    private final DefaultRedisScript<Long> seckillLockScript;    // 分布式锁Lua脚本

    /**
     * 为秒杀场次附加详细信息
     * 加载关联的演出、票档，并查询Redis中的实时库存
     *
     * @param session 秒杀场次对象
     */
    private void attachSession(SeckillSession session) {
        if (session == null) {
            return;
        }
        // 加载关联演出信息
        if (session.getShowId() != null) {
            session.setShow(showService.getById(session.getShowId()));
        }
        // 加载关联票档信息
        if (session.getTicketId() != null) {
            session.setTicket(ticketService.getById(session.getTicketId()));

            String stockKey = RedisKey.format(RedisKey.SECKILL_STOCK, session.getId());
            String warmupKey = RedisKey.format(RedisKey.SECKILL_WARMUP, session.getId());

            // 已预热：使用Redis实时库存
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(warmupKey))) {
                String redisStock = stringRedisTemplate.opsForValue().get(stockKey);
                session.setStock(redisStock != null ? Integer.parseInt(redisStock) : 0);
            } else if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
                // 兼容旧数据：补上warmup标记
                String redisStock = stringRedisTemplate.opsForValue().get(stockKey);
                if (redisStock != null) {
                    session.setStock(Integer.parseInt(redisStock));
                    stringRedisTemplate.opsForValue().set(warmupKey, "1");
                }
            }
            // 未预热：使用数据库原始库存
        }
    }

    /**
     * 执行秒杀
     *
     * 核心流程：
     * 1. 校验秒杀场次是否在有效期内
     * 2. 自动预热库存（如果Redis中没有）
     * 3. 用户级分布式锁（防止重复点击）
     * 4. 执行Lua脚本扣减库存+校验限购
     * 5. 发送订单创建消息到MQ
     *
     * @param sessionId 秒杀场次ID
     * @param userId 用户ID
     * @param quantity 购买数量
     * @return 秒杀结果
     */
    @Override
    public Result<?> seckill(Long sessionId, Long userId, int quantity) {
        try {
            // 1. 校验秒杀场次存在
            SeckillSession session = seckillSessionMapper.selectById(sessionId);
            if (session == null) {
                return Result.fail(ResultCode.NOT_FOUND);
            }

            // 2. 校验秒杀时间
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(session.getStartTime())) {
                return Result.fail(ResultCode.SEckILL_NOT_START);
            }
            if (now.isAfter(session.getEndTime())) {
                return Result.fail(ResultCode.SEckILL_FINISHED);
            }

            // 3. 自动预热：如果Redis中还没有该场次的库存key，从DB加载
            String autoStockKey = RedisKey.format(RedisKey.SECKILL_STOCK, sessionId);
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(autoStockKey))) {
                Ticket ticket4warm = ticketService.getById(session.getTicketId());
                int warmStock = Math.min(
                        session.getStock() != null ? session.getStock() : 0,
                        ticket4warm != null && ticket4warm.getAvailableStock() != null
                                ? ticket4warm.getAvailableStock() : 0
                );
                stringRedisTemplate.opsForValue().set(autoStockKey, String.valueOf(warmStock));
                stringRedisTemplate.opsForValue().set(
                        RedisKey.format(RedisKey.SECKILL_WARMUP, sessionId), "1");
                log.info("自动预热秒杀库存: sessionId={}, stock={}", sessionId, warmStock);
            }

            // 4. 获取用户分布式锁（防止同一用户重复点击）
            String lockKey = RedisKey.format(RedisKey.SECKILL_LOCK, sessionId, userId);
            Long lockResult = stringRedisTemplate.execute(
                    seckillLockScript,
                    Arrays.asList(lockKey),
                    userId.toString(),
                    "10000"  // 锁过期时间：10秒
            );
            if (lockResult == null || lockResult == 0) {
                return Result.fail("请勿重复抢购");
            }

            try {
                // 5. 执行Lua脚本：扣减库存 + 校验限购
                String stockKey = RedisKey.format(RedisKey.SECKILL_STOCK, sessionId);
                String limitKey = RedisKey.format(RedisKey.SECKILL_LIMIT, sessionId, userId);
                Long result = stringRedisTemplate.execute(
                        decreaseStockScript,
                        Arrays.asList(stockKey, limitKey),
                        String.valueOf(quantity),
                        userId.toString(),
                        "2"  // 每人限购数量
                );

                // 6. 处理Lua脚本返回值
                if (result == null) {
                    return Result.fail("抢购失败");
                }
                if (result == -1) {
                    return Result.fail(ResultCode.STOCK_INSUFFICIENT);
                }
                if (result == -3) {
                    return Result.fail(ResultCode.EXCEED_LIMIT);
                }

                // 7. 构建订单信息
                Ticket ticket = ticketService.getById(session.getTicketId());
                if (ticket == null) {
                    return Result.fail("票档不存在");
                }
                Order order = new Order();
                order.setId(-1L); // 标记为秒杀订单（异步创建）
                order.setSessionId(sessionId);
                order.setUserId(userId);
                order.setShowId(session.getShowId());
                order.setTicketId(session.getTicketId());
                order.setQuantity(quantity);
                order.setTotalAmount(session.getSeckillPrice().multiply(BigDecimal.valueOf(quantity)));

                // 8. 发送订单创建消息到MQ（异步处理）
                log.info("准备发送秒杀订单消息: userId={}, showId={}, ticketId={}, quantity={}",
                        userId, session.getShowId(), session.getTicketId(), quantity);
                messageProducer.sendOrderCreateMessage(order);
                log.info("秒杀订单消息发送成功: sessionId={}, userId={}, quantity={}", sessionId, userId, quantity);

                return Result.success("抢购成功，正在处理订单");
            } finally {
                // 9. 释放分布式锁
                String lockKey2 = RedisKey.format(RedisKey.SECKILL_LOCK, sessionId, userId);
                redisTemplate.delete(lockKey2);
            }
        } catch (Exception e) {
            log.error("抢购异常: sessionId={}, userId={}", sessionId, userId, e);
            return Result.fail("系统异常: " + e.getMessage());
        }
    }

    /**
     * 预热秒杀库存到Redis
     * 将数据库中的库存同步到Redis，提高秒杀时的并发处理能力
     *
     * @param sessionId 秒杀场次ID
     */
    @Override
    public void warmUpStock(Long sessionId) {
        SeckillSession session = seckillSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("抢购场次不存在");
        }

        // 校验：秒杀库存不能超过票档可用库存
        Ticket ticket = ticketService.getById(session.getTicketId());
        if (ticket == null) {
            throw new BusinessException("关联票档不存在");
        }
        if (session.getStock() > ticket.getAvailableStock()) {
            log.warn("秒杀库存({})超过票档可用库存({})，将使用票档库存: sessionId={}",
                    session.getStock(), ticket.getAvailableStock(), sessionId);
            session.setStock(ticket.getAvailableStock());
            seckillSessionMapper.updateById(session);
        }

        // 写入预热库存（sessionId级别，防止同票档多场次冲突）
        String stockKey = RedisKey.format(RedisKey.SECKILL_STOCK, sessionId);
        String warmupKey = RedisKey.format(RedisKey.SECKILL_WARMUP, sessionId);
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(session.getStock()));
        stringRedisTemplate.opsForValue().set(warmupKey, "1");  // 标记已预热
        log.info("预热抢购场次库存: sessionId={}, ticketId={}, stock={}",
                sessionId, session.getTicketId(), session.getStock());
    }

    /**
     * 获取进行中的秒杀场次
     * 时间范围：startTime <= now <= endTime
     */
    @Override
    public List<SeckillSession> getActiveSessions() {
        List<SeckillSession> list = seckillSessionMapper.selectActiveSessions();
        list.forEach(this::attachSession);
        return list;
    }

    /**
     * 获取即将开始的秒杀场次
     * 时间范围：now < startTime
     */
    @Override
    public List<SeckillSession> getUpcomingSessions() {
        List<SeckillSession> list = seckillSessionMapper.selectUpcomingSessions();
        list.forEach(this::attachSession);
        return list;
    }

    /**
     * 获取所有秒杀场次（管理员用）
     */
    @Override
    public List<SeckillSession> getAllSessions() {
        List<SeckillSession> list = seckillSessionMapper.selectAll();
        list.forEach(this::attachSession);
        return list;
    }

    /**
     * 根据ID获取秒杀场次
     */
    @Override
    public SeckillSession getById(Long id) {
        SeckillSession session = seckillSessionMapper.selectById(id);
        attachSession(session);
        return session;
    }

    /**
     * 创建秒杀场次
     */
    @Override
    public void create(SeckillSession session) {
        seckillSessionMapper.insert(session);
    }

    /**
     * 更新秒杀场次
     */
    @Override
    @Transactional
    public void update(SeckillSession session) {
        seckillSessionMapper.updateById(session);
    }

    /**
     * 删除秒杀场次
     */
    @Override
    @Transactional
    public void delete(Long id) {
        seckillSessionMapper.deleteById(id);
    }
}
