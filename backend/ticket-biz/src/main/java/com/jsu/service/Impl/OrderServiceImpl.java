package com.jsu.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsu.common.constant.OrderStatus;
import com.jsu.common.constant.ShowStatus;
import com.jsu.common.exception.BusinessException;
import com.jsu.common.util.StringUtils;
import com.jsu.entity.Order;
import com.jsu.entity.SeckillSession;
import com.jsu.mapper.OrderMapper;
import com.jsu.mapper.SeckillSessionMapper;
import com.jsu.service.OrderService;
import com.jsu.service.ShowService;
import com.jsu.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单服务实现类
 * 处理订单创建、查询、取消、支付等核心业务逻辑
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final ShowService showService;
    private final TicketService ticketService;
    private final SeckillSessionMapper seckillSessionMapper;

    /**
     * 为订单附加详细信息
     * 加载关联的演出、票档、秒杀价格等信息
     *
     * @param order 订单对象
     */
    private void attachDetails(Order order) {
        if (order == null) {
            return;
        }
        // 加载关联演出信息
        if (order.getShowId() != null) {
            order.setShow(showService.getById(order.getShowId()));
        }
        // 加载关联票档信息
        if (order.getTicketId() != null) {
            order.setTicket(ticketService.getById(order.getTicketId()));
        }
        // 秒杀订单：加载秒杀价格
        if (order.getIsSeckill() != null && order.getIsSeckill() == 1 && order.getSessionId() != null) {
            SeckillSession session = seckillSessionMapper.selectById(order.getSessionId());
            if (session != null && session.getSeckillPrice() != null) {
                order.setSeckillPrice(session.getSeckillPrice());
            }
        }
    }

    /**
     * 创建订单
     * 核心流程：
     * 1. 校验演出状态（必须为"上架"状态）
     * 2. 校验票档存在且属于指定演出
     * 3. 检查库存是否充足
     * 4. 扣减票档库存（乐观锁）
     * 5. 计算订单金额
     * 6. 生成唯一订单号
     * 7. 设置15分钟支付过期时间
     *
     * @param order 订单信息
     * @return 创建成功的订单
     * @throws BusinessException 演出不可用/票档不存在/库存不足等
     */
    @Override
    public Order create(Order order) {
        // 校验必填参数
        if (order.getShowId() == null || order.getTicketId() == null) {
            throw new IllegalArgumentException("缺少演出或票档信息");
        }

        // 校验演出状态（必须为上架中）
        var show = showService.getById(order.getShowId());
        if (show == null || show.getStatus() == null || show.getStatus() != ShowStatus.ONSALE.getCode()) {
            throw new BusinessException("该演出当前不可下单");
        }
        // 校验票档存在性
        var ticket = ticketService.getById(order.getTicketId());
        if (ticket == null) {
            throw new BusinessException("票档不存在");
        }
        // 校验票档与演出匹配
        if (!order.getShowId().equals(ticket.getShowId())) {
            throw new BusinessException("票档与演出不匹配");
        }
        // 设置默认购买数量
        if (order.getQuantity() == null || order.getQuantity() < 1) {
            order.setQuantity(1);
        }
        // 校验库存
        if (ticket.getAvailableStock() == null || ticket.getAvailableStock() < order.getQuantity()) {
            throw new BusinessException("库存不足");
        }

        // 扣减库存（使用乐观锁，防止超卖）
        boolean deducted = ticketService.deductStock(order.getTicketId(), order.getQuantity());
        if (!deducted) {
            throw new BusinessException("库存不足");
        }

        // 计算订单金额（票档单价 × 数量）
        if (order.getTicketId() != null
                && (order.getTotalAmount() == null || order.getTotalAmount().signum() <= 0)) {
            if (ticket != null && ticket.getPrice() != null) {
                order.setTotalAmount(ticket.getPrice().multiply(BigDecimal.valueOf(order.getQuantity())));
            }
        }
        if (order.getTotalAmount() == null) {
            throw new BusinessException("无法计算订单金额，请检查票档与数量");
        }

        // 设置订单信息
        order.setOrderNo(StringUtils.generateOrderNo());     // 生成唯一订单号
        order.setStatus(OrderStatus.PENDING.getCode());     // 待支付状态
        order.setIsSeckill(0);                              // 普通订单，非秒杀
        order.setExpireTime(LocalDateTime.now().plusMinutes(15)); // 15分钟支付过期时间

        // 插入数据库
        orderMapper.insert(order);
        attachDetails(order);
        return order;
    }

    /**
     * 根据ID获取订单详情
     */
    @Override
    public Order getById(Long id) {
        Order order = orderMapper.selectById(id);
        attachDetails(order);
        return order;
    }

    /**
     * 根据订单号获取订单
     */
    @Override
    public Order getByOrderNo(String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        attachDetails(order);
        return order;
    }

    /**
     * 获取用户的订单列表（分页）
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param status 订单状态筛选（可选）
     * @return 分页后的订单列表
     */
    @Override
    public IPage<Order> getUserOrders(Page<Order> page, Long userId, Integer status) {
        IPage<Order> result = orderMapper.selectUserOrders(page, userId, status);
        result.getRecords().forEach(this::attachDetails);
        return result;
    }

    /**
     * 获取所有订单（管理员用，分页）
     */
    @Override
    public IPage<Order> getAllOrders(Page<Order> page, Integer status) {
        IPage<Order> result = orderMapper.selectAllOrders(page, status);
        result.getRecords().forEach(this::attachDetails);
        return result;
    }

    /**
     * 取消订单
     * 仅可取消状态为"待支付"的订单
     *
     * @param id 订单ID
     * @return true=取消成功，false=订单不存在或状态不允许取消
     */
    @Override
    @Transactional
    public boolean cancel(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return false;
        }
        // 只能取消待支付状态的订单
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            return false;
        }
        order.setStatus(OrderStatus.CANCELLED.getCode());
        return orderMapper.updateById(order) > 0;
    }

    /**
     * 支付订单
     * 校验订单状态，设置支付时间
     *
     * @param orderNo 订单号
     * @return true=支付成功
     * @throws BusinessException 订单不存在/已取消/状态异常/已过期
     */
    @Override
    @Transactional
    public boolean pay(String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() == OrderStatus.CANCELLED.getCode()) {
            throw new BusinessException("订单已取消，无法支付");
        }
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new BusinessException("订单状态异常，无法支付");
        }
        // 检查订单是否过期
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("订单已过期，请重新下单");
        }
        // 更新为已支付状态
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        return orderMapper.updateById(order) > 0;
    }
}
