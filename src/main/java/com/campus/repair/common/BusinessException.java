package com.campus.repair.common;

/**
 * 业务异常。Service 层校验失败时抛出，由控制层统一转换为 Result。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        this(ResultCode.BUSINESS_ERROR, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
