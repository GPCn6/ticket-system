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

    private void attachDetails(Order order) {
        if (order == null) {
            return;
        }
        if (order.getShowId() != null) {
            order.setShow(showService.getById(order.getShowId()));
        }
        if (order.getTicketId() != null) {
            order.setTicket(ticketService.getById(order.getTicketId()));
        }
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
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order create(Order order) {
        if (order.getShowId() == null || order.getTicketId() == null) {
            throw new IllegalArgumentException("缺少演出或票档信息");
        }

        var show = showService.getById(order.getShowId());
        if (show == null || show.getStatus() == null
                || !Integer.valueOf(ShowStatus.ONSALE.getCode()).equals(show.getStatus())) {
            throw new BusinessException("该演出当前不可下单");
        }
        var ticket = ticketService.getById(order.getTicketId());
        if (ticket == null) {
            throw new BusinessException("票档不存在");
        }
        if (!order.getShowId().equals(ticket.getShowId())) {
            throw new BusinessException("票档与演出不匹配");
        }
        if (order.getQuantity() == null || order.getQuantity() < 1) {
            order.setQuantity(1);
        }
        if (ticket.getAvailableStock() == null || ticket.getAvailableStock() < order.getQuantity()) {
            throw new BusinessException("库存不足");
        }

        // 扣减库存（乐观锁防止超卖）
        boolean deducted = ticketService.deductStock(order.getTicketId(), order.getQuantity());
        if (!deducted) {
            throw new BusinessException("库存不足");
        }

        // 计算订单金额
        if (ticket.getPrice() != null) {
            order.setTotalAmount(ticket.getPrice().multiply(BigDecimal.valueOf(order.getQuantity())));
        }
        if (order.getTotalAmount() == null) {
            throw new BusinessException("无法计算订单金额，请检查票档与数量");
        }

        order.setOrderNo(StringUtils.generateOrderNo());
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setIsSeckill(0);
        order.setExpireTime(LocalDateTime.now().plusMinutes(15));

        orderMapper.insert(order);
        attachDetails(order);
        return order;
    }

    @Override
    public Order getById(Long id) {
        Order order = orderMapper.selectById(id);
        attachDetails(order);
        return order;
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        attachDetails(order);
        return order;
    }

    @Override
    public IPage<Order> getUserOrders(Page<Order> page, Long userId, Integer status) {
        IPage<Order> result = orderMapper.selectUserOrders(page, userId, status);
        result.getRecords().forEach(this::attachDetails);
        return result;
    }

    @Override
    public IPage<Order> getAllOrders(Page<Order> page, Integer status) {
        IPage<Order> result = orderMapper.selectAllOrders(page, status);
        result.getRecords().forEach(this::attachDetails);
        return result;
    }

    /**
     * 取消订单
     * 仅可取消状态为"待支付"的订单
     * 取消后自动恢复票档库存（同步，避免MQ延迟导致的库存不一致）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return false;
        }
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            return false;
        }
        int updated = orderMapper.cancelPending(id, OrderStatus.CANCELLED.getCode(), OrderStatus.PENDING.getCode());
        if (updated > 0) {
            return true;
        }
        return false;
    }

    /**
     * 支付订单
     * 校验订单状态，设置支付时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
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
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("订单已过期，请重新下单");
        }
        return orderMapper.payPending(orderNo, OrderStatus.PAID.getCode(), OrderStatus.PENDING.getCode(), LocalDateTime.now()) > 0;
    }
}
