package com.campus.repair.domain;

/**
 * 角色编码常量（供注解使用：注解参数必须是编译期常量，不能直接引用枚举）。
 *
 * <p>取值与 {@link Role#getCode()} 保持一致。</p>
 */
public final class Roles {

    public static final String REPORTER = "reporter";
    public static final String WORKER = "worker";
    public static final String MANAGER = "manager";
    public static final String ADMIN = "admin";

    private Roles() {
    }
}
