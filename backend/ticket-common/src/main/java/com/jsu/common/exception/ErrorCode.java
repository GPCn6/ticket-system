package com.jsu.common.exception;

/**
 * 错误码定义
 *
 * 错误码规范：
 * - 1xxx：用户相关错误
 * - 2xxx：业务相关错误
 * - 3xxx：订单相关错误
 * - 4xxx：库存相关错误
 * - 5xxx：系统相关错误
 */
public class ErrorCode {
    // ==================== 系统错误（5xx） ====================
    /** 系统内部错误 */
    public static final int SYSTEM_ERROR = 500;
    /** 数据库错误 */
    public static final int DATABASE_ERROR = 501;

    // ==================== 用户错误（10xx） ====================
    /** 用户不存在 */
    public static final int USER_NOT_EXIST = 1001;
    /** 密码错误 */
    public static final int PASSWORD_ERROR = 1002;
    /** Token已过期 */
    public static final int TOKEN_EXPIRED = 1003;
    /** Token无效 */
    public static final int TOKEN_INVALID = 1004;

    // ==================== 业务错误（20xx） ====================
    /** 库存不足 */
    public static final int STOCK_INSUFFICIENT = 2001;
    /** 秒杀已结束 */
    public static final int SEckILL_FINISHED = 2002;
    /** 秒杀未开始 */
    public static final int SEckILL_NOT_START = 2003;
    /** 超过购买限制 */
    public static final int EXCEED_LIMIT = 2004;
    /** 订单已过期 */
    public static final int ORDER_EXPIRED = 2005;
    /** 订单不存在 */
    public static final int ORDER_NOT_EXIST = 2006;
    /** 订单已支付 */
    public static final int ORDER_PAID = 2007;

    // ==================== 订单错误（30xx） ====================
    /** 订单创建失败 */
    public static final int ORDER_CREATE_FAIL = 3001;
    /** 订单支付失败 */
    public static final int ORDER_PAY_FAIL = 3002;
    /** 订单取消失败 */
    public static final int ORDER_CANCEL_FAIL = 3003;

    // ==================== 库存错误（40xx） ====================
    /** 库存更新失败 */
    public static final int STOCK_UPDATE_FAIL = 4001;
    /** 库存同步失败 */
    public static final int STOCK_SYNC_FAIL = 4002;
}
