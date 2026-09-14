package com.campus.repair.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 路由注解：声明控制器方法对应的接口路径与访问角色。
 *
 * <p>对应设计书 2.1 控制层"处理用户请求，完成参数封装、调用业务服务并进行视图跳转"，
 * 同时承担基于角色的访问控制（未声明 roles 表示登录即可访问，publicRoute = true 表示无需登录）。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Route {

    /** 接口路径，如 /api/account/login；支持以 /* 结尾表示前缀匹配 */
    String value();

    /** 允许访问的角色（Role 常量：reporter/worker/manager/admin），为空表示所有登录用户 */
    String[] roles() default {};

    /** 是否公开（无需登录），如登录、注册、系统信息 */
    boolean publicRoute() default false;
}
