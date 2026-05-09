package com.jsu.common.constant;

/**
 * 订单状态枚举
 * 定义订单的完整生命周期状态
 *
 * 状态流转：
 * 待支付 → 已支付 → 已完成（正常使用）
 * 待支付 → 已取消（用户取消/超时）
 * 已支付 → 已退款（售后处理）
 */
public enum OrderStatus {
    /** 待支付：订单创建后等待用户付款，15分钟后自动失效 */
    PENDING(0, "待支付"),

    /** 已支付：用户已完成付款，等待入场/使用 */
    PAID(1, "已支付"),

    /** 已取消：用户主动取消或支付超时自动取消 */
    CANCELLED(2, "已取消"),

    /** 已退款：已支付订单退款成功 */
    REFUNDED(3, "已退款"),

    /** 已完成：演出结束或核销后状态 */
    COMPLETED(4, "已完成");

    private final int code;
    private final String name;

    OrderStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 获取状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取状态名称
     */
    public String getName() {
        return name;
    }

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 对应的订单状态枚举，未找到返回null
     */
    public static OrderStatus getByCode(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}