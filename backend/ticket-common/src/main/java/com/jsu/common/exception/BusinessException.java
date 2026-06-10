package com.jsu.common.exception;

import com.jsu.common.result.ResultCode;

/**
 * 业务异常类
 * 用于处理业务逻辑中的各种异常情况
 */
public class BusinessException extends BaseException {

    public BusinessException() {
        super();
    }

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(int code, String message) {
        super(code, message);
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getCode(), resultCode.getMessage());
    }
}
