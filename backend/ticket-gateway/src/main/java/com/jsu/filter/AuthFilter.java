package com.jsu.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final String jwtSecret;
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    public AuthFilter(
            @Value("${jwt.secret}") String jwtSecret,
            ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        this.jwtSecret = jwtSecret;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() == null
                ? "" : exchange.getRequest().getMethod().name().toUpperCase(Locale.ROOT);

        if (isPublicEndpoint(path, method)) {
            return chain.filter(exchange);
        }

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
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + tokenId;

            return reactiveRedisTemplate.hasKey(blacklistKey)
                    .flatMap(isBlacklisted -> {
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            return unauthorized(exchange);
                        }
                        if (isAdminEndpoint(path, method) && !"admin".equalsIgnoreCase(role)) {
                            return forbidden(exchange);
                        }
                        return forwardWithUserHeaders(exchange, chain, userId, role);
                    });
        } catch (Exception e) {
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String message = "{\"code\": 401, \"message\": \"unauthorized\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String message = "{\"code\": 403, \"message\": \"forbidden\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private boolean isPublicEndpoint(String path, String method) {
        if ("/api/user/login".equals(path) || "/api/user/register".equals(path)) {
            return true;
        }
        if ("GET".equals(method) && (path.startsWith("/api/show/") || path.startsWith("/api/ticket/"))) {
            return true;
        }
        return "GET".equals(method) && ("/api/seckill/active".equals(path)
                || "/api/seckill/upcoming".equals(path)
                || path.startsWith("/api/seckill/detail/"));
    }

    private Mono<Void> forwardWithUserHeaders(ServerWebExchange exchange, GatewayFilterChain chain,
                                              String userId, String role) {
        var requestBuilder = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                });
        if (userId != null) {
            requestBuilder.header(USER_ID_HEADER, userId);
            requestBuilder.header(USER_ROLE_HEADER, role == null ? "user" : role);
        }
        return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
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
