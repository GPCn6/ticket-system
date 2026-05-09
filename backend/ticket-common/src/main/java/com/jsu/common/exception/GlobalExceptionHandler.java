package com.jsu.common.exception;

import com.jsu.common.result.Result;
import com.jsu.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 功能：统一处理系统中抛出的各种异常，保证返回格式统一
 *
 * 处理顺序：
 * 1. BusinessException：自定义业务异常，返回错误码和消息
 * 2. MethodArgumentNotValidException：参数校验失败，返回字段级错误
 * 3. Exception：未知异常，返回500系统错误
 *
 * @RestControllerAdvice 全局Controller增强，自动捕获异常并返回JSON
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常（BusinessException）
     * 返回业务定义的错误码和提示信息
     *
     * @param e 业务异常
     * @return Result包装的失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        logger.error("Business Exception: {}", e.getMessage(), e);
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid + @NotBlank等注解触发）
     * 收集所有校验失败的字段和错误信息
     *
     * @param e 参数校验异常
     * @return Result包装的参数错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        logger.error("Parameter Validation Exception: {}", message);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理未捕获的未知异常
     * 兜底处理，避免用户看到堆栈信息
     *
     * @param e 未知异常
     * @return Result包装的系统错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        logger.error("System Exception: {} - {}", e.getClass().getName(), e.getMessage(), e);
        e.printStackTrace();
        return Result.fail(ResultCode.SYSTEM_ERROR.getCode(), "系统异常，请稍后重试: " + e.getClass().getSimpleName());
    }
}
