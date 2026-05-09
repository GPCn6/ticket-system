package com.jsu.mq.consumer;

import com.jsu.common.constant.OrderStatus;
import com.jsu.common.constant.RedisKey;
import com.jsu.common.util.StringUtils;
import com.jsu.entity.Order;
import com.jsu.mapper.OrderMapper;
import com.jsu.mapper.SeckillSessionMapper;
import com.jsu.mq.producer.MessageProducer;
import com.jsu.service.OrderService;
import com.jsu.service.TicketService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 消息队列消费者
 * 处理订单和库存相关的异步消息
 *
 * 消息队列：
 * - order.create.queue：订单创建队列
 * - order.cancel.queue：订单取消队列
 * - stock.deduct.queue：库存扣减队列
 * - stock.restore.queue：库存恢复队列
 *
 * 消息确认机制：
 * - basicAck：确认消息处理成功
 * - basicNack：确认消息处理失败，丢弃消息（不重试，防止死循环）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageConsumer {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final TicketService ticketService;
    private final MessageProducer messageProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillSessionMapper seckillSessionMapper;

    /**
     * 处理订单创建消息
     * 秒杀订单异步创建，同时发送库存扣减消息
     *
     * @param order 订单信息
     * @param channel RabbitMQ通道
     * @param message 原始消息
     */
    @RabbitListener(queues = "order.create.queue")
    public void handleOrderCreateMessage(Order order, Channel channel, Message message) throws IOException {
        // 判断是否为秒杀订单（id为-1表示秒杀订单）
        boolean isSeckillOrder = order.getId() != null && order.getId() == -1L;

        try {
            log.info("收到订单创建消息: {}, isSeckill={}", order, isSeckillOrder);

            // 设置订单基本信息
            order.setOrderNo(StringUtils.generateOrderNo());
            order.setStatus(OrderStatus.PENDING.getCode());
            order.setIsSeckill(isSeckillOrder ? 1 : 0);
            order.setExpireTime(LocalDateTime.now().plusMinutes(15));  // 15分钟支付过期

            if (isSeckillOrder) {
                // 秒杀订单：直接插入数据库
                order.setId(null);  // 清除-1标记，让数据库自增ID
                orderMapper.insert(order);
                log.info("秒杀订单创建成功: orderNo={}", order.getOrderNo());
                // 发送库存扣减消息，同步数据库库存
                messageProducer.sendStockDeductMessage(
                        order.getSessionId(),
                        order.getTicketId(),
                        order.getQuantity(),
                        order.getOrderNo()
                );
            } else {
                // 普通订单：调用Service创建（包含库存扣减）
                orderService.create(order);
            }

            // 确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("订单创建消息处理失败，丢弃消息: orderNo={}", order.getOrderNo(), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }

    /**
     * 处理订单取消消息
     * 确保订单取消后库存正确恢复
     *
     * @param order 订单信息
     * @param channel RabbitMQ通道
     * @param message 原始消息
     */
    @RabbitListener(queues = "order.cancel.queue")
    @Transactional
    public void handleOrderCancelMessage(Order order, Channel channel, Message message) throws IOException {
        try {
            log.info("收到订单取消消息: {}", order);

            // 重新查询最新订单状态（避免重复处理）
            Order latestOrder = orderMapper.selectById(order.getId());
            if (latestOrder == null) {
                log.warn("订单不存在: orderId={}", order.getId());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 如果订单已取消，仍需发送库存恢复消息
            if (latestOrder.getStatus() == OrderStatus.CANCELLED.getCode()) {
                log.info("订单已被取消，直接发送库存恢复消息: orderId={}", order.getId());
                boolean isSeckillOrder = latestOrder.getIsSeckill() != null && latestOrder.getIsSeckill() == 1;
                messageProducer.sendStockRestoreMessage(
                        latestOrder.getSessionId(),
                        latestOrder.getTicketId(),
                        latestOrder.getQuantity(),
                        latestOrder.getOrderNo(),
                        latestOrder.getUserId(),
                        isSeckillOrder
                );
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 订单仍为待支付状态，执行取消
            boolean updated = orderService.cancel(order.getId());
            if (!updated) {
                log.warn("订单取消失败: orderId={}", order.getId());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 发送库存恢复消息
            boolean isSeckillOrder = latestOrder.getIsSeckill() != null && latestOrder.getIsSeckill() == 1;
            messageProducer.sendStockRestoreMessage(
                    latestOrder.getSessionId(),
                    latestOrder.getTicketId(),
                    latestOrder.getQuantity(),
                    latestOrder.getOrderNo(),
                    latestOrder.getUserId(),
                    isSeckillOrder
            );

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("订单取消消息处理成功: orderNo={}", latestOrder.getOrderNo());

        } catch (Exception e) {
            log.error("订单取消消息处理失败，丢弃消息", e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }

    /**
     * 处理库存扣减消息
     * 同步票档库存和秒杀场次库存到数据库
     *
     * @param message 库存扣减消息
     * @param channel RabbitMQ通道
     * @param amqpMessage 原始消息
     */
    @RabbitListener(queues = "stock.deduct.queue")
    @Transactional
    public void handleStockDeductMessage(MessageProducer.StockDeductMessage message, Channel channel, Message amqpMessage) throws IOException {
        try {
            log.info("收到库存扣减消息: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity());

            // 扣减票档数据库库存
            boolean success = ticketService.deductStock(message.getTicketId(), message.getQuantity());
            if (!success) {
                log.warn("票档库存扣减失败: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity());
            } else {
                log.info("票档库存扣减成功: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity());
            }

            // 同步扣减秒杀场次数据库库存（保持DB与Redis一致）
            if (message.getSessionId() != null) {
                int rows = seckillSessionMapper.deductStock(message.getSessionId(), message.getQuantity());
                if (rows > 0) {
                    log.info("秒杀场次库存扣减成功: sessionId={}, quantity={}", message.getSessionId(), message.getQuantity());
                } else {
                    log.warn("秒杀场次库存扣减失败（可能已售罄）: sessionId={}", message.getSessionId());
                }
            }

            // 确认消息
            channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("库存扣减消息处理失败，丢弃消息: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity(), e);
            channel.basicNack(amqpMessage.getMessageProperties().getDeliveryTag(), false, false);
        }
    }

    /**
     * 处理库存恢复消息
     * 恢复票档库存、秒杀场次库存和Redis库存
     *
     * @param message 库存恢复消息
     * @param channel RabbitMQ通道
     * @param amqpMessage 原始消息
     */
    @RabbitListener(queues = "stock.restore.queue")
    @Transactional
    public void handleStockRestoreMessage(MessageProducer.StockRestoreMessage message, Channel channel, Message amqpMessage) throws IOException {
        try {
            log.info("收到库存恢复消息: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity());

            // 恢复票档数据库库存
            boolean success = ticketService.restoreStock(message.getTicketId(), message.getQuantity());
            if (success) {
                log.info("票档库存恢复成功: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity());
            } else {
                log.warn("票档库存恢复失败: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity());
            }

            // 同步恢复秒杀场次数据库库存
            if (message.getSessionId() != null) {
                seckillSessionMapper.restoreStock(message.getSessionId(), message.getQuantity());
                log.info("秒杀场次库存恢复: sessionId={}, quantity={}", message.getSessionId(), message.getQuantity());
            }

            // 秒杀订单：恢复Redis库存和限购计数
            if (message.isSeckillOrder()) {
                // 恢复Redis秒杀库存
                String stockKey = RedisKey.format(RedisKey.SECKILL_STOCK, message.getSessionId());
                stringRedisTemplate.opsForValue().increment(stockKey, message.getQuantity());
                log.info("秒杀订单Redis库存恢复: sessionId={}, quantity={}", message.getSessionId(), message.getQuantity());

                // 恢复Redis限购计数，允许用户重新下单
                if (message.getUserId() != null) {
                    String limitKey = RedisKey.format(RedisKey.SECKILL_LIMIT, message.getSessionId(), message.getUserId());
                    String limitVal = stringRedisTemplate.opsForValue().get(limitKey);
                    if (limitVal != null) {
                        int newBought = Math.max(0, Integer.parseInt(limitVal) - message.getQuantity());
                        if (newBought <= 0) {
                            // 限购计数归零，删除key
                            stringRedisTemplate.delete(limitKey);
                            log.info("清空用户限购计数: sessionId={}, userId={}", message.getSessionId(), message.getUserId());
                        } else {
                            // 更新限购计数
                            stringRedisTemplate.opsForValue().set(limitKey, String.valueOf(newBought), java.time.Duration.ofSeconds(86400));
                            log.info("更新用户限购计数: sessionId={}, userId={}, newBought={}", message.getSessionId(), message.getUserId(), newBought);
                        }
                    }
                }
            }

            channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("库存恢复消息处理失败，丢弃消息: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity(), e);
            channel.basicNack(amqpMessage.getMessageProperties().getDeliveryTag(), false, false);
        }
    }
}
