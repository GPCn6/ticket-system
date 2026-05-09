package com.jsu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关服务启动类
 *
 * 功能：所有API请求的统一入口
 * 1. 路由转发：根据路径将请求转发到对应的服务（user-service、biz-service）
 * 2. JWT认证：在网关层统一进行Token验证和权限校验（详见 AuthFilter）
 * 3. 前后端分离：前端静态资源代理到 localhost:3000（Vue开发服务器）
 * 4. 跨域处理：配置全局CORS策略
 *
 * 技术栈：Spring Cloud Gateway（Reactive非阻塞）
 *
 * @SpringBootApplication Spring Boot应用启动注解
 */
@SpringBootApplication
public class TicketGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketGatewayApplication.class, args);
    }

}
