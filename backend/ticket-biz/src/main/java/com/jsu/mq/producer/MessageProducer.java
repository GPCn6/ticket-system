package com.jsu.mq.producer;

import com.jsu.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 消息队列生产者
 * 负责向RabbitMQ发送订单和库存相关的异步消息
 *
 * 使用消息队列的目的：
 * 1. 异步处理：秒杀成功后异步创建订单，提高响应速度
 * 2. 削峰填谷：缓解高并发对数据库的压力
 * 3. 系统解耦：订单模块与库存模块通过消息通信
 *
 * 交换机：ticket.exchange
 * 路由键：
 * - order.create：订单创建
 * - order.cancel：订单取消
 * - stock.deduct：库存扣减
 * - stock.restore：库存恢复
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单创建消息
     * 用于秒杀成功后异步创建正式订单
     *
     * @param order 订单信息
     */
    public void sendOrderCreateMessage(Order order) {
        try {
            rabbitTemplate.convertAndSend("ticket.exchange", "order.create", order);
            log.info("订单创建消息发送成功: orderNo={}", order.getOrderNo());
        } catch (Exception e) {
            log.error("订单创建消息发送失败: orderNo={}", order.getOrderNo(), e);
        }
    }

    /**
     * 发送订单取消消息
     *
     * @param order 订单信息
     */
    public void sendOrderCancelMessage(Order order) {
        try {
            rabbitTemplate.convertAndSend("ticket.exchange", "order.cancel", order);
            log.info("订单取消消息发送成功: orderNo={}", order.getOrderNo());
        } catch (Exception e) {
            log.error("订单取消消息发送失败: orderNo={}", order.getOrderNo(), e);
        }
    }

    /**
     * 发送库存扣减消息
     * 用于异步同步数据库库存
     *
     * @param sessionId 秒杀场次ID（普通订单为null）
     * @param ticketId 票档ID
     * @param quantity 扣减数量
     * @param orderNo 订单号
     */
    public void sendStockDeductMessage(Long sessionId, Long ticketId, int quantity, String orderNo) {
        try {
            StockDeductMessage message = new StockDeductMessage();
            message.setSessionId(sessionId);
            message.setTicketId(ticketId);
            message.setQuantity(quantity);
            message.setOrderNo(orderNo);
            rabbitTemplate.convertAndSend("ticket.exchange", "stock.deduct", message);
            log.info("库存扣减消息发送成功: sessionId={}, ticketId={}, quantity={}", sessionId, ticketId, quantity);
        } catch (Exception e) {
            log.error("库存扣减消息发送失败: ticketId={}, quantity={}", ticketId, quantity, e);
        }
    }

    /**
     * 发送库存恢复消息（非秒杀订单）
     * 用于订单取消时恢复票档库存
     */
    public void sendStockRestoreMessage(Long sessionId, Long ticketId, int quantity, String orderNo, Long userId) {
        sendStockRestoreMessage(sessionId, ticketId, quantity, orderNo, userId, false);
    }

    /**
     * 发送库存恢复消息
     * 订单取消后恢复库存，包括：
     * - 票档可用库存
     * - 秒杀场次库存（Redis）
     *
     * @param sessionId 秒杀场次ID
     * @param ticketId 票档ID
     * @param quantity 恢复数量
     * @param orderNo 订单号
     * @param userId 用户ID
     * @param isSeckillOrder 是否秒杀订单
     */
    public void sendStockRestoreMessage(Long sessionId, Long ticketId, int quantity, String orderNo, Long userId, boolean isSeckillOrder) {
        try {
            StockRestoreMessage message = new StockRestoreMessage();
            message.setSessionId(sessionId);
            message.setTicketId(ticketId);
            message.setQuantity(quantity);
            message.setOrderNo(orderNo);
            message.setUserId(userId);
            message.setReason("ORDER_CANCELLED");
            message.setSeckillOrder(isSeckillOrder);
            rabbitTemplate.convertAndSend("ticket.exchange", "stock.restore", message);
            log.info("库存恢复消息发送成功: sessionId={}, ticketId={}, quantity={}, isSeckill={}", sessionId, ticketId, quantity, isSeckillOrder);
        } catch (Exception e) {
            log.error("库存恢复消息发送失败: ticketId={}, quantity={}", ticketId, quantity, e);
        }
    }


    /**
     * 库存扣减消息体
     * 用于异步同步Redis库存到数据库
     */
    public static class StockDeductMessage {
        private Long sessionId;
        private Long ticketId;
        private int quantity;
        private String orderNo;

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getTicketId() {
            return ticketId;
        }

        public void setTicketId(Long ticketId) {
            this.ticketId = ticketId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }
    }

    /**
     * 库存恢复消息体
     * 用于订单取消时恢复票档库存和秒杀Redis库存
     */
    public static class StockRestoreMessage {
        private Long sessionId;
        private Long ticketId;
        private int quantity;
        private String orderNo;
        private Long userId;
        private String reason;
        private boolean seckillOrder;

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getTicketId() {
            return ticketId;
        }

        public void setTicketId(Long ticketId) {
            this.ticketId = ticketId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public boolean isSeckillOrder() {
            return seckillOrder;
        }

        public void setSeckillOrder(boolean seckillOrder) {
            this.seckillOrder = seckillOrder;
        }
    }
}
