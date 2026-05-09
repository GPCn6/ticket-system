package com.jsu.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / Knife4j 接口文档配置
 *
 * 功能：自动生成 RESTful API 接口文档
 * 访问地址：http://localhost:端口/doc.html
 *
 * 支持分组标签（@Tag）：
 * - 用户管理：/api/user/**
 * - 演出管理：/api/show/**
 * - 票档管理：/api/ticket/**
 * - 订单管理：/api/order/**
 * - 抢购管理：/api/seckill/**
 *
 * @Configuration Spring配置类注解
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置 OpenAPI 基本信息
     *
     * @return OpenAPI 实例
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("票务系统 API 文档")
                        .version("1.0.0")
                        .description("高并发票务系统后端接口文档")
                        .contact(new Contact()
                                .name("JSU")
                                .email("contact@jsu.com")));
    }
}
