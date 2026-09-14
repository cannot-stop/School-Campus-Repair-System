package com.campus.repair.common;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 前端表单校验的轻量校验器（对应设计书"参数校验/必填项校验"要求）。
 */
public final class Validate {

    private final Map<String, String> errors = new LinkedHashMap<String, String>();

    public static Validate create() {
        return new Validate();
    }

    /** 校验字符串必填 */
    public Validate required(String field, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            errors.put(field, label + "不能为空");
        }
        return this;
    }

    /** 校验字符串长度 */
    public Validate maxLength(String field, String label, String value, int max) {
        if (value != null && value.trim().length() > max) {
            errors.put(field, label + "长度不能超过" + max + "个字符");
        }
        return this;
    }

    /** 校验对象必填 */
    public Validate required(String field, String label, Object value) {
        if (value == null) {
            errors.put(field, label + "不能为空");
        }
        return this;
    }

    /** 校验手机号 */
    public Validate phone(String field, String value) {
        if (value == null || !value.matches("^1[3-9]\\d{9}$")) {
            errors.put(field, "手机号格式不正确");
        }
        return this;
    }

    /** 校验整数范围 */
    public Validate range(String field, String label, Integer value, int min, int max) {
        if (value == null || value < min || value > max) {
            errors.put(field, label + "必须在" + min + "～" + max + "之间");
        }
        return this;
    }

    /** 追加自定义错误 */
    public Validate add(String field, String message) {
        errors.put(field, message);
        return this;
    }

    /** 是否存在错误 */
    public boolean hasError() {
        return !errors.isEmpty();
    }

    /** 错误明细 */
    public Map<String, String> getErrors() {
        return errors;
    }

    /** 校验失败时抛出业务异常 */
    public void throwIfInvalid() {
        if (hasError()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : errors.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("；");
                }
                sb.append(entry.getValue());
            }
            throw new BusinessException(ResultCode.PARAM_ERROR, sb.toString());
        }
    }

    /** 空安全的字符串判断 */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** 空安全的字符串取值 */
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private Validate() {
    }

    /** 校验失败明细（用于序列化） */
    public Map<String, String> asSerializableMap() {
        return new LinkedHashMap<String, String>(errors);
    }
}
