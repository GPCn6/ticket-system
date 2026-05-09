package com.jsu.common.constant;

/**
 * 演出状态枚举
 * 定义演出在系统中的上下架状态
 *
 * 状态说明：
 * - OFFLINE：下架状态，用户不可见，不可下单
 * - ONSALE：上架/销售中，用户可见，可正常下单
 * - ONGOING：演出进行中（预留状态）
 */
public enum ShowStatus {
    /** 下架：用户不可见，不可下单 */
    OFFLINE(0, "下架"),

    /** 上架/销售中：用户可见，可正常下单 */
    ONSALE(1, "上架"),

    /** 进行中：演出正在进行（预留状态） */
    ONGOING(2, "进行中");

    private final int code;
    private final String name;

    ShowStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 对应的演出状态枚举
     */
    public static ShowStatus getByCode(int code) {
        for (ShowStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}