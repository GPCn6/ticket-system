package com.jsu.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息队列配置
 *
 * 消息模型：Topic Exchange（主题交换机）
 * 功能：秒杀成功后异步创建订单、同步库存
 *
 * 消息流向：
 * ┌─────────────────────────────────────────────────────┐
 * │                ticket.exchange（Topic）              │
 * │   order.create → order.create.queue（订单创建）       │
 * │   order.cancel  → order.cancel.queue（订单取消）     │
 * │   stock.deduct  → stock.deduct.queue（库存扣减）     │
 * │   stock.restore → stock.restore.queue（库存恢复）    │
 * └─────────────────────────────────────────────────────┘
 *
 * @Configuration Spring配置类注解
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 交换机 ====================
    /** 票务系统主题交换机 */
    public static final String TICKET_EXCHANGE = "ticket.exchange";

    // ==================== 队列名 ====================
    /** 订单创建队列：秒杀成功后异步创建正式订单 */
    public static final String ORDER_CREATE_QUEUE = "order.create.queue";
    /** 订单取消队列：取消订单时恢复库存 */
    public static final String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    /** 库存扣减队列：同步Redis库存到数据库 */
    public static final String STOCK_DEDUCT_QUEUE = "stock.deduct.queue";
    /** 库存恢复队列：订单取消时恢复库存到数据库和Redis */
    public static final String STOCK_RESTORE_QUEUE = "stock.restore.queue";

    // ==================== 路由键 ====================
    /** 订单创建路由键 */
    public static final String ORDER_CREATE_KEY = "order.create";
    /** 订单取消路由键 */
    public static final String ORDER_CANCEL_KEY = "order.cancel";
    /** 库存扣减路由键 */
    public static final String STOCK_DEDUCT_KEY = "stock.deduct";
    /** 库存恢复路由键 */
    public static final String STOCK_RESTORE_KEY = "stock.restore";

    /**
     * JSON消息转换器
     * 将Java对象序列化为JSON格式进行消息传输
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 主题交换机（topic）
     * 支持路由键匹配，将消息路由到绑定的队列
     */
    @Bean
    public TopicExchange ticketExchange() {
        // name: 交换机名称, durable: 持久化, autoDelete: 不自动删除
        return new TopicExchange(TICKET_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(ORDER_CREATE_QUEUE).build();
    }

    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_QUEUE).build();
    }

    @Bean
    public Queue stockDeductQueue() {
        return QueueBuilder.durable(STOCK_DEDUCT_QUEUE).build();
    }

    @Bean
    public Queue stockRestoreQueue() {
        return QueueBuilder.durable(STOCK_RESTORE_QUEUE).build();
    }

    /**
     * 将队列绑定到交换机，使用对应的路由键
     */
    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue())
                .to(ticketExchange())
                .with(ORDER_CREATE_KEY);
    }

    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue())
                .to(ticketExchange())
                .with(ORDER_CANCEL_KEY);
    }

    @Bean
    public Binding stockDeductBinding() {
        return BindingBuilder.bind(stockDeductQueue())
                .to(ticketExchange())
                .with(STOCK_DEDUCT_KEY);
    }

    @Bean
    public Binding stockRestoreBinding() {
        return BindingBuilder.bind(stockRestoreQueue())
                .to(ticketExchange())
                .with(STOCK_RESTORE_KEY);
    }
}