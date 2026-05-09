package com.jsu.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/api/user/**")
                        .uri("http://localhost:8081"))
                .route("biz-service", r -> r
                        .path("/api/show/**", "/api/ticket/**", "/api/order/**", "/api/seckill/**")
                        .uri("http://localhost:8082"))
                .route("static-resources", r -> r
                        .path("/static/**", "/favicon.ico")
                        .uri("http://localhost:3000"))
                .route("frontend", r -> r
                        .path("/**")
                        .uri("http://localhost:3000"))
                .build();
    }
}
