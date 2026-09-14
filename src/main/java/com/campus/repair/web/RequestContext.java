package com.campus.repair.web;

import java.util.HashMap;
import java.util.Map;

/**
 * 请求上下文：控制层方法统一入参。
 */
public class RequestContext {

    private final String method;
    private final String path;
    private final ParamMap params;
    private final Session session;
    private final String remoteAddress;

    public RequestContext(String method, String path, ParamMap params, Session session, String remoteAddress) {
        this.method = method;
        this.path = path;
        this.params = params;
        this.session = session;
        this.remoteAddress = remoteAddress;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public ParamMap getParams() {
        return params;
    }

    public Session getSession() {
        return session;
    }

    public Integer getUserId() {
        return session == null ? null : session.getUserId();
    }

    public String getRole() {
        return session == null ? null : session.getRole();
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String param(String name) {
        return params.get(name);
    }

    public String param(String name, String defaultValue) {
        return params.get(name, defaultValue);
    }

    public Integer intParam(String name) {
        return params.getInt(name);
    }

    public Integer requireIntParam(String name) {
        Integer value = params.getInt(name);
        if (value == null) {
            throw new com.campus.repair.common.BusinessException(
                    com.campus.repair.common.ResultCode.PARAM_ERROR, "参数 " + name + " 缺失或格式不正确");
        }
        return value;
    }

    public boolean boolParam(String name) {
        return params.getBoolean(name);
    }

    /** 常用返回：成功 */
    public View ok(Object data) {
        return View.ok(data);
    }

    /** 常用返回：成功（键值对） */
    public View ok(String key, Object value) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(key, value);
        return View.ok(data);
    }
}
