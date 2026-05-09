package com.jsu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户服务启动类
 *
 * 功能：用户模块的独立服务
 * 核心接口：
 * 1. POST  /api/user/login         → 用户登录（JWT Token认证）
 * 2. POST  /api/user/register      → 用户注册
 * 3. GET   /api/user/info          → 获取用户信息
 * 4. PUT   /api/user/update        → 更新用户信息
 * 5. PUT   /api/user/reset-password → 重置密码
 * 6. POST  /api/user/logout        → 用户登出
 *
 * 端口：8081
 * 数据库：ticket_system.sys_user 表
 *
 * @SpringBootApplication Spring Boot启动
 * @MapperScan 扫描com.jsu.mapper包下的MyBatis Mapper接口
 */
@SpringBootApplication
@MapperScan("com.jsu.mapper")
public class TicketUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketUserApplication.class, args);
    }

}
