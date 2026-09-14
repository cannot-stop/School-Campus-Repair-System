package com.campus.repair.common;

import java.io.Serializable;

/**
 * 统一返回结果封装，供控制层向视图层/接口调用方返回。
 *
 * @param <T> 数据类型
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码，0 表示成功 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 业务数据 */
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>(ResultCode.SUCCESS, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<T>(ResultCode.SUCCESS, message, data);
    }

    public static <T> Result<T> success() {
        return new Result<T>(ResultCode.SUCCESS, "操作成功", null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<T>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<T>(ResultCode.BUSINESS_ERROR, message, null);
    }

    public boolean isSuccess() {
        return code == ResultCode.SUCCESS;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
