package com.jsu.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 网关认证过滤器
 * 功能：
 * 1. JWT Token验证
 * 2. 用户身份提取（userId、role）
 * 3. 权限校验（区分普通用户和管理员接口）
 *
 * @Component 注册为Spring组件，被Gateway自动加载
 * @GlobalFilter 实现全局过滤器，对所有路由生效
 * @Ordered(-100) 执行顺序，负数优先级高
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    /**
     * JWT加密密钥（与UserServiceImpl保持一致）
     */
    private static final String SECRET_KEY = "TicketSystemSecretKey2026ForJWTTokenGeneration";

    /**
     * 过滤器核心方法
     * 对每个请求进行认证检查
     *
     * @param exchange 请求上下文
     * @param chain 过滤器链
     * @return 响应
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() == null
                ? ""
                : exchange.getRequest().getMethod().name().toUpperCase(Locale.ROOT);

        // 1. 放行公开接口（登录、注册、查询类接口）
        if (isPublicEndpoint(path, method)) {
            return chain.filter(exchange);
        }

        // 2. 提取Token
        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }
        token = token.substring(7);

        try {
            // 3. 验证Token，解析Claims
            SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String userId = claims.getSubject();      // 用户ID
            String role = claims.get("role", String.class);  // 用户角色

            // 4. 管理员接口权限校验
            if (isAdminEndpoint(path, method) && !"admin".equalsIgnoreCase(role)) {
                return forbidden(exchange);
            }

            // 5. 将用户信息注入请求头，传递给下游服务
            ServerWebExchange mutableExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Role", role == null ? "user" : role)
                            .build())
                    .build();

            return chain.filter(mutableExchange);
        } catch (Exception e) {
            // Token解析失败（过期、签名错误等）
            return unauthorized(exchange);
        }
    }

    /**
     * 返回401未授权响应
     */
    private Mono<Void> unauthorized(org.springframework.web.server.ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String message = "{\"code\": 401, \"message\": \"未授权\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * 返回403禁止访问响应
     */
    private Mono<Void> forbidden(org.springframework.web.server.ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String message = "{\"code\": 403, \"message\": \"无权限访问\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * 判断是否为公开接口（无需认证）
     * 包括：登录、注册、GET请求的查询类接口
     */
    private boolean isPublicEndpoint(String path, String method) {
        // 用户登录和注册
        if ("/api/user/login".equals(path) || "/api/user/register".equals(path)) {
            return true;
        }
        // 演出和票档的查询接口（GET）
        if ("GET".equals(method) && (path.startsWith("/api/show/") || path.startsWith("/api/ticket/"))) {
            return true;
        }
        // 秒杀相关查询接口（GET）
        if ("GET".equals(method) && ("/api/seckill/active".equals(path)
                || "/api/seckill/upcoming".equals(path)
                || path.startsWith("/api/seckill/detail/"))) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否为管理员专用接口
     * POST/PUT/DELETE的增删改接口需要admin权限
     * 注意：秒杀执行接口(/seckill/execute)虽然也是POST，但属于普通用户接口
     */
    private boolean isAdminEndpoint(String path, String method) {
        if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
            // 秒杀执行是普通用户接口，放行
            if (path.equals("/api/seckill/execute")) {
                return false;
            }
            // 演出、票档、秒杀管理的增删改接口需要管理员权限
            return path.startsWith("/api/show/")
                    || path.startsWith("/api/ticket/")
                    || path.startsWith("/api/seckill/");
        }
        return false;
    }

    /**
     * 过滤器执行顺序
     * 返回-100表示在大部分过滤器之前执行
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
