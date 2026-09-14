package com.campus.repair.domain;

/**
 * 用户角色（对应 t_user.role）。
 */
public enum Role {

    REPORTER("reporter", "报修人"),
    WORKER("worker", "维修人员"),
    MANAGER("manager", "维修管理员"),
    ADMIN("admin", "系统管理员");

    private final String code;
    private final String text;

    Role(String code, String text) {
        this.code = code;
        this.text = text;
    }

    public String getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public static Role of(String code) {
        if (code != null) {
            for (Role value : values()) {
                if (value.code.equalsIgnoreCase(code)) {
                    return value;
                }
            }
        }
        return null;
    }

    public static String textOf(String code) {
        Role role = of(code);
        return role == null ? "未知" : role.text;
    }
}
