package com.jsu.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 网关认证过滤器
 *
 * 功能：
 * 1. JWT Token 验证（密钥从配置读取）
 * 2. Token 黑名单校验（Redis 校验，支持登出即失效）
 * 3. 用户身份提取（userId、role）
 * 4. 权限校验（区分普通用户和管理员接口）
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    /** Token 黑名单 Redis Key 前缀 */
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    private final String jwtSecret;
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    public AuthFilter(
            @Value("${jwt.secret:TicketSystemSecretKey2026ForJWTTokenGeneration}") String jwtSecret,
            ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        this.jwtSecret = jwtSecret;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() == null
                ? "" : exchange.getRequest().getMethod().name().toUpperCase(Locale.ROOT);

        // 放行公开接口
        if (isPublicEndpoint(path, method)) {
            return chain.filter(exchange);
        }

        // 提取 Token
        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }
        token = token.substring(7);

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenId = claims.get("tokenId", String.class);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // Token 黑名单校验（异步 Redis 查询）
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + tokenId;
            return reactiveRedisTemplate.hasKey(blacklistKey)
                    .flatMap(isBlacklisted -> {
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            return unauthorized(exchange);
                        }
                        // 管理员接口权限校验
                        if (isAdminEndpoint(path, method) && !"admin".equalsIgnoreCase(role)) {
                            return forbidden(exchange);
                        }
                        // 注入用户信息到请求头
                        ServerWebExchange mutableExchange = exchange.mutate()
                                .request(exchange.getRequest().mutate()
                                        .header("X-User-Id", userId)
                                        .header("X-User-Role", role == null ? "user" : role)
                                        .build())
                                .build();
                        return chain.filter(mutableExchange);
                    });
        } catch (Exception e) {
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String message = "{\"code\": 401, \"message\": \"未授权\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String message = "{\"code\": 403, \"message\": \"无权限访问\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private boolean isPublicEndpoint(String path, String method) {
        if ("/api/user/login".equals(path) || "/api/user/register".equals(path)
                || "/api/user/login-debug".equals(path)) {
            return true;
        }
        if ("GET".equals(method) && (path.startsWith("/api/show/") || path.startsWith("/api/ticket/"))) {
            return true;
        }
        if ("GET".equals(method) && ("/api/seckill/active".equals(path)
                || "/api/seckill/upcoming".equals(path)
                || path.startsWith("/api/seckill/detail/"))) {
            return true;
        }
        return false;
    }

    private boolean isAdminEndpoint(String path, String method) {
        if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
            if (path.equals("/api/seckill/execute")) {
                return false;
            }
            return path.startsWith("/api/show/")
                    || path.startsWith("/api/ticket/")
                    || path.startsWith("/api/seckill/");
        }
        return false;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
