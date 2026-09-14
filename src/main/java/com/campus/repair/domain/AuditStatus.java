package com.campus.repair.domain;

/**
 * 统一审核状态（用户实名审核 audit_status、评价审核 audit_status）。
 * 0待审/1通过/2驳回。
 */
public enum AuditStatus {

    PENDING(0, "待审核"),
    PASSED(1, "已通过"),
    REJECTED(2, "已驳回");

    private final int code;
    private final String text;

    AuditStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public static AuditStatus of(Integer code) {
        if (code != null) {
            for (AuditStatus value : values()) {
                if (value.code == code.intValue()) {
                    return value;
                }
            }
        }
        return null;
    }

    public static String textOf(Integer code) {
        AuditStatus status = of(code);
        return status == null ? "未知" : status.text;
    }
}
