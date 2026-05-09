package com.jsu.task;

import com.jsu.entity.Order;
import com.jsu.mapper.OrderMapper;
import com.jsu.mq.producer.MessageProducer;
import com.jsu.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCancelScheduler {

    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final MessageProducer messageProducer;

    @Scheduled(fixedRate = 60000)
    public void cancelExpiredOrders() {
        List<Order> expiredOrders = orderMapper.selectExpiredOrders();
        if (expiredOrders.isEmpty()) {
            return;
        }
        log.info("扫描到 {} 个过期订单待取消", expiredOrders.size());
        for (Order order : expiredOrders) {
            try {
                boolean cancelled = orderService.cancel(order.getId());
                if (!cancelled) {
                    log.warn("过期订单取消失败（可能已被取消）: orderNo={}", order.getOrderNo());
                    continue;
                }
                // 通过MQ恢复库存（包含票档库存、秒杀场次库存、Redis库存）
                boolean isSeckill = order.getIsSeckill() != null && order.getIsSeckill() == 1;
                messageProducer.sendStockRestoreMessage(
                        order.getSessionId(),
                        order.getTicketId(),
                        order.getQuantity(),
                        order.getOrderNo(),
                        order.getUserId(),
                        isSeckill
                );
                log.info("过期订单已取消，库存恢复中: orderNo={}, isSeckill={}", order.getOrderNo(), isSeckill);
            } catch (Exception e) {
                log.error("处理过期订单失败: orderNo={}", order.getOrderNo(), e);
            }
        }
    }
}
