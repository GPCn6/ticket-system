package com.jsu.common.exception;

/**
 * 基础异常类
 * 所有业务异常的父类
 *
 * RuntimeException：运行时异常，无需强制捕获
 */
public class BaseException extends RuntimeException {
    /** 错误码 */
    private Integer code;
    /** 错误消息 */
    private String message;

    public BaseException() {
    }

    /**
     * 使用消息创建异常
     *
     * @param message 错误消息
     */
    public BaseException(String message) {
        super(message);
        this.message = message;
    }

    /**
     * 使用错误码和消息创建异常
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
