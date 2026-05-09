package com.jsu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 业务服务启动类
 *
 * 功能：核心业务模块，包含演出、票档、订单、秒杀等所有业务逻辑
 *
 * 核心模块：
 * 1. 演出管理：ShowController + ShowServiceImpl（CRUD、分类、搜索、热门）
 * 2. 票档管理：TicketController + TicketServiceImpl（CRUD、库存扣减/恢复）
 * 3. 订单管理：OrderController + OrderServiceImpl（创建、支付、取消）
 * 4. 秒杀系统：SeckillController + SeckillServiceImpl（Redis+Lua+MQ）
 * 5. 消息队列：MessageProducer + MessageConsumer（RabbitMQ异步处理）
 *
 * 架构亮点：
 * - Redis预热库存 + Lua脚本原子扣减 → 解决高并发秒杀超卖
 * - RabbitMQ异步创建订单 → 削峰填谷
 * - MyBatis-Plus乐观锁扣减库存 → 防止普通下单超卖
 *
 * 端口：8082
 * 数据库表：biz_show, biz_ticket, biz_order, biz_seckill_session
 *
 * @SpringBootApplication Spring Boot启动
 * @EnableScheduling 启用定时任务（如订单超时取消）
 * @MapperScan 扫描com.jsu.mapper包下的MyBatis Mapper接口
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.jsu.mapper")
public class TickerBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(TickerBizApplication.class, args);
    }

}
