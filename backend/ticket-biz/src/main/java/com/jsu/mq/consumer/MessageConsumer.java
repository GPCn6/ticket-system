package com.jsu.mq.consumer;

import com.jsu.common.constant.OrderStatus;
import com.jsu.common.constant.RedisKey;
import com.jsu.common.util.StringUtils;
import com.jsu.entity.Order;
import com.jsu.mapper.OrderMapper;
import com.jsu.mapper.SeckillSessionMapper;
import com.jsu.mq.producer.MessageProducer;
import com.jsu.service.OrderService;
import org.springframework.dao.DuplicateKeyException;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 消息队列消费者
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
     */
    @RabbitListener(queues = "order.create.queue")
    public void handleOrderCreateMessage(Order order, Channel channel, Message message) throws IOException {
        boolean isSeckillOrder = order.getId() != null && order.getId() == -1L;

        try {
            log.info("收到订单创建消息: {}, isSeckill={}", order, isSeckillOrder);

            // 优先使用 Producer 侧生成的订单号（保证MQ重试幂等）
            if (order.getOrderNo() == null) {
                order.setOrderNo(StringUtils.generateOrderNo());
            }
            order.setStatus(OrderStatus.PENDING.getCode());
            order.setIsSeckill(isSeckillOrder ? 1 : 0);
            order.setExpireTime(LocalDateTime.now().plusMinutes(15));

            if (isSeckillOrder) {
                try {
                    order.setId(null);
                    orderMapper.insert(order);
                    log.info("秒杀订单创建成功: orderNo={}", order.getOrderNo());
                    messageProducer.sendStockDeductMessage(
                            order.getSessionId(),
                            order.getTicketId(),
                            order.getQuantity(),
                            order.getOrderNo()
                    );
                } catch (DuplicateKeyException e) {
                    log.warn("秒杀订单已存在（幂等性忽略）: orderNo={}", order.getOrderNo());
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    return;
                }
            } else {
                orderService.create(order);
            }

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("订单创建消息处理失败，丢弃消息: orderNo={}", order.getOrderNo(), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }

    /**
     * 处理订单取消消息
     */
    @RabbitListener(queues = "order.cancel.queue")
    @Transactional
    public void handleOrderCancelMessage(Order order, Channel channel, Message message) throws IOException {
        try {
            log.info("收到订单取消消息: {}", order);

            Order latestOrder = orderMapper.selectById(order.getId());
            if (latestOrder == null) {
                log.warn("订单不存在: orderId={}", order.getId());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            if (latestOrder.getStatus() == OrderStatus.CANCELLED.getCode()) {
                log.info("订单已被取消，幂等确认: orderId={}", order.getId());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            boolean updated = orderService.cancel(order.getId());
            if (!updated) {
                log.warn("订单取消失败: orderId={}", order.getId());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

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
     */
    @RabbitListener(queues = "stock.deduct.queue")
    @Transactional
    public void handleStockDeductMessage(MessageProducer.StockDeductMessage message, Channel channel, Message amqpMessage) throws IOException {
        try {
            log.info("收到库存扣减消息: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity());

            boolean success = ticketService.deductStock(message.getTicketId(), message.getQuantity());
            if (!success) {
                log.warn("票档库存扣减失败: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity());
            } else {
                log.info("票档库存扣减成功: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity());
            }

            if (message.getSessionId() != null) {
                int rows = seckillSessionMapper.deductStock(message.getSessionId(), message.getQuantity());
                if (rows > 0) {
                    log.info("秒杀场次库存扣减成功: sessionId={}, quantity={}", message.getSessionId(), message.getQuantity());
                } else {
                    log.warn("秒杀场次库存扣减失败（可能已售罄）: sessionId={}", message.getSessionId());
                }
            }

            channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("库存扣减消息处理失败，丢弃消息: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity(), e);
            channel.basicNack(amqpMessage.getMessageProperties().getDeliveryTag(), false, false);
        }
    }

    /**
     * 处理库存恢复消息
     */
    @RabbitListener(queues = "stock.restore.queue")
    @Transactional
    public void handleStockRestoreMessage(MessageProducer.StockRestoreMessage message, Channel channel, Message amqpMessage) throws IOException {
        try {
            log.info("收到库存恢复消息: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity());

            boolean success = ticketService.restoreStock(message.getTicketId(), message.getQuantity());
            if (success) {
                log.info("票档库存恢复成功: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity());
            } else {
                log.warn("票档库存恢复失败: ticketId={}, quantity={}", message.getTicketId(), message.getQuantity());
            }

            if (message.getSessionId() != null) {
                seckillSessionMapper.restoreStock(message.getSessionId(), message.getQuantity());
                log.info("秒杀场次库存恢复: sessionId={}, quantity={}", message.getSessionId(), message.getQuantity());
            }

            if (message.isSeckillOrder()) {
                String stockKey = RedisKey.format(RedisKey.SECKILL_STOCK, message.getSessionId());
                stringRedisTemplate.opsForValue().increment(stockKey, message.getQuantity());
                log.info("秒杀订单Redis库存恢复: sessionId={}, quantity={}", message.getSessionId(), message.getQuantity());

                if (message.getUserId() != null) {
                    String limitKey = RedisKey.format(RedisKey.SECKILL_LIMIT, message.getSessionId(), message.getUserId());
                    String limitVal = stringRedisTemplate.opsForValue().get(limitKey);
                    if (limitVal != null) {
                        int newBought = Math.max(0, Integer.parseInt(limitVal) - message.getQuantity());
                        if (newBought <= 0) {
                            stringRedisTemplate.delete(limitKey);
                            log.info("清空用户限购计数: sessionId={}, userId={}", message.getSessionId(), message.getUserId());
                        } else {
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

    // ==================== 死信队列消费者（人工补偿入口） ====================

    @RabbitListener(queues = "order.create.dlq")
    public void handleOrderCreateDeadLetter(Message message, Channel channel) throws IOException {
        try {
            log.error("订单创建死信消息: deliveryTag={}, body={}",
                    message.getMessageProperties().getDeliveryTag(),
                    new String(message.getBody(), StandardCharsets.UTF_8));
            // 死信消息需人工排查并补偿，确认后手动 ack
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理订单创建死信消息异常", e);
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @RabbitListener(queues = "order.cancel.dlq")
    public void handleOrderCancelDeadLetter(Message message, Channel channel) throws IOException {
        try {
            log.error("订单取消死信消息: deliveryTag={}, body={}",
                    message.getMessageProperties().getDeliveryTag(),
                    new String(message.getBody(), StandardCharsets.UTF_8));
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理订单取消死信消息异常", e);
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @RabbitListener(queues = "stock.deduct.dlq")
    public void handleStockDeductDeadLetter(Message message, Channel channel) throws IOException {
        try {
            log.error("库存扣减死信消息: deliveryTag={}, body={}",
                    message.getMessageProperties().getDeliveryTag(),
                    new String(message.getBody(), StandardCharsets.UTF_8));
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理库存扣减死信消息异常", e);
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @RabbitListener(queues = "stock.restore.dlq")
    public void handleStockRestoreDeadLetter(Message message, Channel channel) throws IOException {
        try {
            log.error("库存恢复死信消息: deliveryTag={}, body={}",
                    message.getMessageProperties().getDeliveryTag(),
                    new String(message.getBody(), StandardCharsets.UTF_8));
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理库存恢复死信消息异常", e);
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        }
    }
}
