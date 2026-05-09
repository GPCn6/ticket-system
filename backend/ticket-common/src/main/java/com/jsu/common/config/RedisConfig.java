package com.jsu.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 使用Jackson2JsonRedisSerializer序列化值
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);

        // 键使用StringRedisSerializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 用于Lua脚本执行的StringRedisTemplate
     * 避免Jackson序列化给ARGV参数加引号导致Lua中tonumber解析失败
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    /**
     * 扣减库存Lua脚本
     */
    @Bean
    public DefaultRedisScript<Long> decreaseStockScript() {
        String script = """
                local quantity = tonumber(ARGV[1])
                local user_id = ARGV[2]
                local max_per_user = tonumber(ARGV[3])
                local user_limit_key = KEYS[2]
                local user_bought = tonumber(redis.call('GET', user_limit_key) or '0')
                
                if user_bought + quantity > max_per_user then
                    return -3
                end
                
                local stock_key = KEYS[1]
                local current_stock = tonumber(redis.call('GET', stock_key) or '0')
                
                if current_stock < quantity then
                    return -1
                end
                
                local remaining = redis.call('DECRBY', stock_key, quantity)
                
                if remaining < 0 then
                    redis.call('INCRBY', stock_key, quantity)
                    return -1
                end
                
                redis.call('INCRBY', user_limit_key, quantity)
                redis.call('EXPIRE', user_limit_key, 86400)
                
                return remaining
                """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 恢复库存Lua脚本
     */
    @Bean
    public DefaultRedisScript<Long> restoreStockScript() {
        String script = """
                local quantity = tonumber(ARGV[1])
                local stock_key = KEYS[1]
                local user_limit_key = KEYS[2]
                local remaining = redis.call('INCRBY', stock_key, quantity)
                local user_bought = tonumber(redis.call('GET', user_limit_key) or '0')
                if user_bought > 0 then
                    local new_bought = user_bought - quantity
                    if new_bought < 0 then new_bought = 0 end
                    redis.call('SET', user_limit_key, new_bought)
                end
                return remaining
                """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 抢购锁Lua脚本
     */
    @Bean
    public DefaultRedisScript<Long> seckillLockScript() {
        String script = """
                local lock_key = KEYS[1]
                local user_id = ARGV[1]
                local expire_time = tonumber(ARGV[2])
                local result = redis.call('SET', lock_key, user_id, 'NX', 'PX', expire_time)
                if result then return 1 else return 0 end
                """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 抢购锁释放Lua脚本
     */
    @Bean
    public DefaultRedisScript<Long> seckillUnlockScript() {
        String script = """
                local lock_key = KEYS[1]
                local user_id = ARGV[1]
                local current_owner = redis.call('GET', lock_key)
                if current_owner == user_id then
                    redis.call('DEL', lock_key)
                    return 1
                else
                    return 0
                end
                """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 接口限流Lua脚本
     */
    @Bean
    public DefaultRedisScript<Long> rateLimitScript() {
        String script = """
                local key = KEYS[1]
                local window = tonumber(ARGV[1])
                local max_requests = tonumber(ARGV[2])
                local current = tonumber(redis.call('GET', key) or '0')
                if current >= max_requests then
                    return 0
                end
                redis.call('INCR', key)
                if current == 0 then
                    redis.call('EXPIRE', key, window)
                end
                return 1
                """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}