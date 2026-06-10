package com.jsu.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息队列配置
 *
 * 企业级特性：
 * - Topic Exchange（主题交换机）
 * - 死信队列（DLQ）：消费失败的消息自动路由到 DLQ，防止消息丢失
 * - 每个处理队列绑定对应的死信队列
 *
 * 消息流向：
 *   ticket.exchange (Topic)
 *     ├─ order.create  → order.create.queue     + order.create.dlq（死信）
 *     ├─ order.cancel  → order.cancel.queue     + order.cancel.dlq
 *     ├─ stock.deduct  → stock.deduct.queue     + stock.deduct.dlq
 *     └─ stock.restore → stock.restore.queue    + stock.restore.dlq
 */
@Configuration
public class RabbitMQConfig {

    public static final String TICKET_EXCHANGE = "ticket.exchange";
    public static final String DLX_EXCHANGE = "ticket.dlx";

    // 处理队列
    public static final String ORDER_CREATE_QUEUE = "order.create.queue";
    public static final String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    public static final String STOCK_DEDUCT_QUEUE = "stock.deduct.queue";
    public static final String STOCK_RESTORE_QUEUE = "stock.restore.queue";

    // 死信队列
    public static final String ORDER_CREATE_DLQ = "order.create.dlq";
    public static final String ORDER_CANCEL_DLQ = "order.cancel.dlq";
    public static final String STOCK_DEDUCT_DLQ = "stock.deduct.dlq";
    public static final String STOCK_RESTORE_DLQ = "stock.restore.dlq";

    public static final String ORDER_CREATE_KEY = "order.create";
    public static final String ORDER_CANCEL_KEY = "order.cancel";
    public static final String STOCK_DEDUCT_KEY = "stock.deduct";
    public static final String STOCK_RESTORE_KEY = "stock.restore";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ==================== 正常交换机 ====================

    @Bean
    public TopicExchange ticketExchange() {
        return new TopicExchange(TICKET_EXCHANGE, true, false);
    }

    // ==================== 死信交换机 ====================

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    /**
     * 构建队列（含死信配置）
     */
    private Queue durableQueue(String name, String routingKey) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(routingKey + ".dead")
                .build();
    }

    private Queue durableDlq(String name) {
        return QueueBuilder.durable(name).build();
    }

    // ==================== 正常队列 + 绑定 ====================

    @Bean
    public Queue orderCreateQueue() {
        return durableQueue(ORDER_CREATE_QUEUE, ORDER_CREATE_KEY);
    }

    @Bean
    public Queue orderCancelQueue() {
        return durableQueue(ORDER_CANCEL_QUEUE, ORDER_CANCEL_KEY);
    }

    @Bean
    public Queue stockDeductQueue() {
        return durableQueue(STOCK_DEDUCT_QUEUE, STOCK_DEDUCT_KEY);
    }

    @Bean
    public Queue stockRestoreQueue() {
        return durableQueue(STOCK_RESTORE_QUEUE, STOCK_RESTORE_KEY);
    }

    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue()).to(ticketExchange()).with(ORDER_CREATE_KEY);
    }

    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(ticketExchange()).with(ORDER_CANCEL_KEY);
    }

    @Bean
    public Binding stockDeductBinding() {
        return BindingBuilder.bind(stockDeductQueue()).to(ticketExchange()).with(STOCK_DEDUCT_KEY);
    }

    @Bean
    public Binding stockRestoreBinding() {
        return BindingBuilder.bind(stockRestoreQueue()).to(ticketExchange()).with(STOCK_RESTORE_KEY);
    }

    // ==================== 死信队列 + 绑定 ====================

    @Bean
    public Queue orderCreateDlq() {
        return durableDlq(ORDER_CREATE_DLQ);
    }

    @Bean
    public Queue orderCancelDlq() {
        return durableDlq(ORDER_CANCEL_DLQ);
    }

    @Bean
    public Queue stockDeductDlq() {
        return durableDlq(STOCK_DEDUCT_DLQ);
    }

    @Bean
    public Queue stockRestoreDlq() {
        return durableDlq(STOCK_RESTORE_DLQ);
    }

    @Bean
    public Binding orderCreateDlqBinding() {
        return BindingBuilder.bind(orderCreateDlq()).to(dlxExchange()).with(ORDER_CREATE_KEY + ".dead");
    }

    @Bean
    public Binding orderCancelDlqBinding() {
        return BindingBuilder.bind(orderCancelDlq()).to(dlxExchange()).with(ORDER_CANCEL_KEY + ".dead");
    }

    @Bean
    public Binding stockDeductDlqBinding() {
        return BindingBuilder.bind(stockDeductDlq()).to(dlxExchange()).with(STOCK_DEDUCT_KEY + ".dead");
    }

    @Bean
    public Binding stockRestoreDlqBinding() {
        return BindingBuilder.bind(stockRestoreDlq()).to(dlxExchange()).with(STOCK_RESTORE_KEY + ".dead");
    }
}
