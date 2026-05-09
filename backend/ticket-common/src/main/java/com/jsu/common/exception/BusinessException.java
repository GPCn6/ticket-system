package com.jsu.common.exception;

import com.jsu.common.result.ResultCode;

/**
 * 业务异常类
 * 用于处理业务逻辑中的各种异常情况
 *
 * 使用示例：
 * - throw new BusinessException("用户名已存在");
 * - throw new BusinessException(ResultCode.USER_NOT_EXIST);
 * - throw new BusinessException(1001, "自定义错误信息");
 */
public class BusinessException extends BaseException {
    public BusinessException() {
        super();
    }

    /**
     * 使用自定义消息创建异常
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 使用错误码和消息创建异常
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(code, message);
    }

    /**
     * 使用ResultCode枚举创建异常
     *
     * @param resultCode 结果码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getCode(), resultCode.getMessage());
    }
}
