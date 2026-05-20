package com.zzp.pvms.auth.exception;

import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public Result<Void> handleApiException(ApiException exception) {
        return Result.failed(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        return Result.failed(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return Result.failed("系统异常，请稍后重试");
    }
}
