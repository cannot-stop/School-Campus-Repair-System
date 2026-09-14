package com.campus.repair.web;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.Result;
import com.campus.repair.common.ResultCode;
import com.campus.repair.util.JsonUtil;

/**
 * 前端控制器（对应设计书 2.1 控制层"采用前端控制器与各功能控制器处理用户请求"）。
 *
 * <p>职责：路由匹配、参数封装、登录与角色权限校验、业务服务调用、统一异常处理与结果渲染。</p>
 */
public class Dispatcher {

    private final Map<String, Handler> exactRoutes = new LinkedHashMap<String, Handler>();
    private final List<Handler> prefixRoutes = new ArrayList<Handler>();
    private final Map<String, Object> controllers = new LinkedHashMap<String, Object>();

    public Dispatcher() {
        register(new com.campus.repair.web.controller.AccountController());
        register(new com.campus.repair.web.controller.ReportController());
        register(new com.campus.repair.web.controller.DispatchController());
        register(new com.campus.repair.web.controller.RepairController());
        register(new com.campus.repair.web.controller.MaterialController());
        register(new com.campus.repair.web.controller.EvaluationController());
        register(new com.campus.repair.web.controller.MessageController());
        register(new com.campus.repair.web.controller.StatController());
    }

    /** 注册控制器：扫描 @Route 注解方法 */
    public void register(Object controller) {
        controllers.put(controller.getClass().getSimpleName(), controller);
        Method[] methods = controller.getClass().getMethods();
        for (Method method : methods) {
            Route route = method.getAnnotation(Route.class);
            if (route == null) {
                continue;
            }
            Handler handler = new Handler(route, controller, method);
            String path = normalize(route.value());
            if (path.endsWith("/*")) {
                prefixRoutes.add(handler);
            } else {
                exactRoutes.put(path, handler);
            }
        }
    }

    /** 已注册的控制器 */
    public Map<String, Object> getControllers() {
        return controllers;
    }

    /** 已注册的路由数量 */
    public int routeCount() {
        return exactRoutes.size() + prefixRoutes.size();
    }

    /** 路由清单（启动日志展示） */
    public List<String> routeList() {
        List<String> list = new ArrayList<String>();
        for (Handler handler : exactRoutes.values()) {
            list.add(handler.describe());
        }
        for (Handler handler : prefixRoutes) {
            list.add(handler.describe());
        }
        return list;
    }

    /**
     * 处理一次请求。
     *
     * @param method HTTP 方法
     * @param path   请求路径（可含查询串）
     * @param body   请求体
     * @param token  会话令牌
     */
    public View dispatch(String method, String path, String body, String token) {
        String purePath = path;
        String query = null;
        int mark = path.indexOf('?');
        if (mark >= 0) {
            purePath = path.substring(0, mark);
            query = path.substring(mark + 1);
        }
        purePath = normalize(purePath);

        ParamMap params = ParamMap.fromQuery(query);
        if (body != null && !body.trim().isEmpty()) {
            String trimmed = body.trim();
            if (trimmed.startsWith("{")) {
                params.putAll(ParamMap.parseJson(trimmed));
            } else if (trimmed.contains("=")) {
                params.putAll(ParamMap.parseUrlEncoded(trimmed));
            }
        }
        return dispatchWithParams(method, purePath, params, token);
    }

    /**
     * 以调用方已解析好的参数处理请求（供 Servlet 容器适配层使用：
     * 容器已通过 getParameter() 解析表单、并通过输入流读取 JSON，此处不再重复读取请求体）。
     *
     * @param method HTTP 方法
     * @param path   请求路径（仅路径，不含查询串）
     * @param params 已解析的参数
     * @param token  会话令牌
     */
    public View dispatchWithParams(String method, String path, ParamMap params, String token) {
        String purePath = normalize(path);
        if (params == null) {
            params = ParamMap.create();
        }

        Handler handler = match(purePath);
        if (handler == null) {
            return View.fail(ResultCode.NOT_FOUND, "接口不存在：" + purePath);
        }

        Session session = SessionManager.get(token);
        try {
            if (!handler.publicRoute) {
                if (session == null) {
                    return View.fail(ResultCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
                }
                if (!session.hasRole(handler.roles)) {
                    return View.fail(ResultCode.FORBIDDEN,
                            "当前角色（" + session.getRoleText() + "）无权访问该功能");
                }
            }
            RequestContext ctx = new RequestContext(method, purePath, params, session, null);
            Object value = handler.method.invoke(handler.controller, ctx);
            if (value instanceof View) {
                return (View) value;
            }
            return View.ok(value);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof BusinessException) {
                BusinessException be = (BusinessException) cause;
                return View.fail(be.getCode(), be.getMessage());
            }
            cause.printStackTrace();
            return View.fail(ResultCode.SYSTEM_ERROR, "服务器内部错误：" + cause.getMessage());
        } catch (BusinessException e) {
            return View.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return View.fail(ResultCode.SYSTEM_ERROR, "服务器内部错误：" + e.getMessage());
        }
    }

    /** 渲染 View 为响应文本 */
    public String render(View view) {
        if (view == null) {
            return JsonUtil.toJson(Result.fail(ResultCode.SYSTEM_ERROR, "空响应"));
        }
        if (view.isRedirect()) {
            return String.valueOf(view.getJson().getData());
        }
        return JsonUtil.toJson(view.getJson());
    }

    // ------------------------------------------------------------ 内部方法

    private Handler match(String path) {
        Handler handler = exactRoutes.get(path);
        if (handler != null) {
            return handler;
        }
        for (Handler prefix : prefixRoutes) {
            String base = prefix.path.substring(0, prefix.path.length() - 2);
            if (path.startsWith(base + "/")) {
                return prefix;
            }
        }
        return null;
    }

    private static String normalize(String path) {
        if (path == null) {
            return "/";
        }
        String value = path.trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /** 路由处理器 */
    private static class Handler {
        private final String path;
        private final String[] roles;
        private final boolean publicRoute;
        private final Object controller;
        private final Method method;

        Handler(Route route, Object controller, Method method) {
            this.path = normalize(route.value());
            this.roles = route.roles();
            this.publicRoute = route.publicRoute();
            this.controller = controller;
            this.method = method;
        }

        String describe() {
            return path + " → " + controller.getClass().getSimpleName() + "." + method.getName() + "()"
                    + (publicRoute ? " [公开]" : (roles.length == 0 ? " [登录]" : " [" + String.join("/", roles) + "]"));
        }
    }

    /** 供测试使用：直接按路径调用 */
    public Map<String, Object> routeMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        for (Map.Entry<String, Handler> entry : exactRoutes.entrySet()) {
            map.put(entry.getKey(), entry.getValue().describe());
        }
        return map;
    }
}
