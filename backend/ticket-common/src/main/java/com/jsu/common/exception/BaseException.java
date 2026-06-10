package com.jsu.common.exception;

/**
 * 异常基类
 * 提供 code 和 message 属性，供子类继承
 */
public class BaseException extends RuntimeException {

    private int code;

    public BaseException() {
        super();
        this.code = 500;
    }

    public BaseException(String message) {
        super(message);
        this.code = 500;
    }

    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
