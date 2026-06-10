package com.jsu.common.result;

/**
 * 统一返回码枚举
 *
 * 状态码规范：
 * - 2xx：成功
 * - 4xx：客户端错误（参数错误、未授权等）
 * - 5xx：服务器错误
 * - 1xxx：用户相关业务错误
 * - 2xxx：业务逻辑错误（库存、秒杀、订单等）
 */
public enum ResultCode {

    SUCCESS(200, "成功"),
    FAIL(500, "失败"),

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),

    SYSTEM_ERROR(500, "系统错误"),
    DATABASE_ERROR(501, "数据库错误"),

    USER_NOT_EXIST(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    TOKEN_EXPIRED(1003, "Token已过期"),

    STOCK_INSUFFICIENT(2001, "库存不足"),
    SECKILL_FINISHED(2002, "抢购已结束"),
    SECKILL_NOT_START(2003, "抢购未开始"),
    EXCEED_LIMIT(2004, "超出限购数量"),
    ORDER_EXPIRED(2005, "订单已过期"),
    ORDER_NOT_EXIST(2006, "订单不存在"),
    ;

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
