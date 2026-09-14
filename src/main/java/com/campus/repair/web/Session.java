package com.campus.repair.web;

import java.util.Date;

import com.campus.repair.domain.Role;
import com.campus.repair.domain.User;

/**
 * 会话对象（对应设计书 1.1.2.1"建立登录会话"与"注销后需重新登录才能访问系统功能"）。
 */
public class Session {

    private final String token;
    private final Integer userId;
    private final String username;
    private final String realName;
    private final String role;
    private final long createTime;
    private volatile long lastAccessTime;

    public Session(String token, User user) {
        this.token = token;
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.realName = user.getRealName();
        this.role = user.getRole();
        this.createTime = System.currentTimeMillis();
        this.lastAccessTime = this.createTime;
    }

    public String getToken() {
        return token;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRealName() {
        return realName;
    }

    public String getRole() {
        return role;
    }

    public String getRoleText() {
        return Role.textOf(role);
    }

    public void touch() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    public long getIdleMillis() {
        return System.currentTimeMillis() - lastAccessTime;
    }

    public Date getLoginTime() {
        return new Date(createTime);
    }

    public boolean hasRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return true;
        }
        for (String item : roles) {
            if ("*".equals(item) || role.equals(item)) {
                return true;
            }
        }
        return false;
    }
}
