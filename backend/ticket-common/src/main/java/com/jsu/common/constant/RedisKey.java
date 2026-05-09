package com.jsu.common.constant;

/**
 * Redis Key常量定义
 * 统一管理Redis中的key格式，避免key冲突
 *
 * Key命名规范：
 * - 使用冒号(:)分隔层级
 * - 第一段：业务模块标识（如seckill、user、order）
 * - 第二段：功能类型（如stock、limit、lock）
 * - 后面的段落：具体参数（如sessionId、userId）
 */
public class RedisKey {

    // ==================== 秒杀相关 ====================

    /**
     * 秒杀场次库存
     * Key格式：seckill:stock:{sessionId}
     * 说明：sessionId级别，防止同票档多场次库存冲突
     */
    public static final String SECKILL_STOCK = "seckill:stock:{sessionId}";

    /**
     * 秒杀预热标记
     * Key格式：seckill:warmup:{sessionId}
     * 说明：标记该场次是否已预热到Redis
     */
    public static final String SECKILL_WARMUP = "seckill:warmup:{sessionId}";

    /**
     * 用户秒杀限购计数
     * Key格式：seckill:limit:{sessionId}:{userId}
     * 说明：记录用户对某场次的已购数量
     */
    public static final String SECKILL_LIMIT = "seckill:limit:{sessionId}:{userId}";

    /**
     * 秒杀分布式锁
     * Key格式：seckill:lock:{sessionId}:{userId}
     * 说明：防止用户重复点击抢购
     */
    public static final String SECKILL_LOCK = "seckill:lock:{sessionId}:{userId}";

    /**
     * 抢购场次锁
     * Key格式：seckill:session:lock:{sessionId}
     * 说明：场次级别的操作锁
     */
    public static final String SECKILL_SESSION_LOCK = "seckill:session:lock:{sessionId}";

    /**
     * 抢购排队队列
     * Key格式：seckill:queue:{sessionId}
     * 说明：用于高并发时的请求排队
     */
    public static final String SECKILL_QUEUE = "seckill:queue:{sessionId}";

    // ==================== 用户相关 ====================

    /**
     * 用户Token
     * Key格式：user:token:{userId}
     * 说明：存储用户登录Token
     */
    public static final String USER_TOKEN = "user:token:{userId}";

    /**
     * 手机验证码
     * Key格式：user:captcha:{phone}
     * 说明：存储短信验证码及有效期
     */
    public static final String USER_CAPTCHA = "user:captcha:{phone}";

    /**
     * 用户Session
     * Key格式：user:session:{userId}
     * 说明：存储用户会话信息
     */
    public static final String USER_SESSION = "user:session:{userId}";

    // ==================== 订单相关 ====================

    /**
     * 订单消息队列
     * Key格式：order:msg:queue
     * 说明：订单相关的异步消息队列
     */
    public static final String ORDER_MSG_QUEUE = "order:msg:queue";

    /**
     * 订单状态缓存
     * Key格式：order:status:{orderNo}
     * 说明：缓存订单状态，减少数据库查询
     */
    public static final String ORDER_STATUS = "order:status:{orderNo}";

    // ==================== 限流相关 ====================

    /**
     * 接口限流
     * Key格式：rate:limit:api:{userId}
     * 说明：限制用户访问某接口的频率
     */
    public static final String RATE_LIMIT_API = "rate:limit:api:{userId}";

    /**
     * IP限流
     * Key格式：rate:limit:ip:{ip}
     * 说明：限制某IP的访问频率
     */
    public static final String RATE_LIMIT_IP = "rate:limit:ip:{ip}";

    // ==================== 黑名单 ====================

    /**
     * IP黑名单
     * Key格式：blacklist:ip
     * 说明：Set类型，存储被封禁的IP地址
     */
    public static final String BLACKLIST_IP = "blacklist:ip";

    /**
     * 用户黑名单
     * Key格式：blacklist:user
     * 说明：Set类型，存储被封禁的用户ID
     */
    public static final String BLACKLIST_USER = "blacklist:user";

    /**
     * 格式化Redis Key
     * 将模板中的{param}占位符替换为实际参数值
     *
     * @param pattern 模板字符串，如："seckill:stock:{sessionId}"
     * @param params 替换参数
     * @return 格式化后的Key，如："seckill:stock:123"
     *
     * @example
     * RedisKey.format(RedisKey.SECKILL_STOCK, 123)
     * // 返回：seckill:stock:123
     *
     * RedisKey.format(RedisKey.SECKILL_LIMIT, 123, 456)
     * // 返回：seckill:limit:123:456
     */
    public static String format(String pattern, Object... params) {
        String result = pattern;
        for (Object param : params) {
            result = result.replaceFirst("\\{[^}]+\\}", param.toString());
        }
        return result;
    }
}
