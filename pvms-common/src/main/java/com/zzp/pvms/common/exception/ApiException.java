package com.zzp.pvms.common.exception;

import lombok.Getter;

/**
 * 自定义 API 异常
 */
@Getter
public class ApiException extends RuntimeException {
    private final long code;

    public ApiException(String message) {
        super(message);
        this.code = 500;
    }

    public ApiException(long code, String message) {
        super(message);
        this.code = code;
    }
}
