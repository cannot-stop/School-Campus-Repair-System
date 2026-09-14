package com.campus.repair.common;

/**
 * 统一业务状态码。
 */
public final class ResultCode {

    /** 成功 */
    public static final int SUCCESS = 0;
    /** 参数校验失败 */
    public static final int PARAM_ERROR = 1001;
    /** 业务规则校验失败 */
    public static final int BUSINESS_ERROR = 1002;
    /** 未登录 */
    public static final int UNAUTHORIZED = 1003;
    /** 无权限 */
    public static final int FORBIDDEN = 1004;
    /** 数据不存在 */
    public static final int NOT_FOUND = 1005;
    /** 系统内部错误 */
    public static final int SYSTEM_ERROR = 5000;

    private ResultCode() {
    }
}
