package com.jsu.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; return count", Long.class);

    public RateLimitFilter(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        super(Config.class);
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            String key = "rate:limit:ip:" + ip;

            return reactiveRedisTemplate.execute(rateLimitScript, List.of(key), List.of(String.valueOf(config.getWindow())))
                    .next().flatMap(count -> {
                        if (count > config.getLimit()) {
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            return exchange.getResponse().setComplete();
                        }
                        return chain.filter(exchange);
                    });
        };
    }

    public static class Config {
        private int limit = 100; // 默认每分钟100次
        private int window = 60; // 默认60秒

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getWindow() {
            return window;
        }

        public void setWindow(int window) {
            this.window = window;
        }
    }
}
