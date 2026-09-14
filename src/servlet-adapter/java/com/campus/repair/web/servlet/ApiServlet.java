package com.campus.repair.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import com.campus.repair.web.Dispatcher;
import com.campus.repair.web.ParamMap;
import com.campus.repair.web.SessionManager;
import com.campus.repair.web.View;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 前端控制器 Servlet（对应设计书 2.1"采用前端控制器与各功能控制器处理用户请求"）。
 *
 * <p>把容器的 {@link HttpServletRequest} 适配为系统内部的 {@link ParamMap}，
 * 复用同一套 Controller / Service / DAO，并保持与内置服务器完全一致的接口契约。</p>
 *
 * <p><b>编译说明</b>：依赖 Jakarta Servlet API 6.0，仅在 Tomcat 10+/Jetty 11+ 部署时需要编译。</p>
 */
@WebServlet(name = "apiServlet", urlPatterns = "/api/*")
public class ApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String TOKEN_COOKIE = "CRS_TOKEN";

    /** 全局唯一的调度器（线程安全） */
    private static final Dispatcher DISPATCHER = new Dispatcher();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handle(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handle(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handle(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handle(request, response);
    }

    /** 统一处理：参数适配 → 调度 → 输出 */
    private void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 必须在读取任何参数之前设置编码，否则表单/查询串中的中文会乱码
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        Map<String, String> params = new LinkedHashMap<String, String>();

        // 1. 查询串与表单参数：由容器解析（getParameterNames 会触发表单体解析）
        String contentType = request.getContentType();
        boolean jsonBody = contentType != null && contentType.toLowerCase().contains("application/json");

        if (jsonBody) {
            // JSON 请求体：显式读取输入流后自行解析（不能用 getParameter，否则会冲突）
            String body = ParamMap.readBody(request.getInputStream(), 2 * 1024 * 1024);
            params.putAll(ParamMap.parseJson(body));
        }

        // 2. 查询串参数（URL 上的 ?a=1&b=2）
        String query = request.getQueryString();
        if (query != null && !query.isEmpty()) {
            params.putAll(ParamMap.parseUrlEncoded(query));
        }

        // 3. 表单参数（application/x-www-form-urlencoded）
        if (!jsonBody) {
            Enumeration<String> names = request.getParameterNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String value = request.getParameter(name);
                if (value != null) {
                    params.put(name, value);
                }
            }
        } else {
            // JSON 场景下仍可能带少量查询参数（已在上一步处理），此处补入其余非体参数
            Enumeration<String> names = request.getParameterNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String value = request.getParameter(name);
                if (value != null && !params.containsKey(name)) {
                    params.put(name, value);
                }
            }
        }

        ParamMap paramMap = ParamMap.create().putAll(params);
        String token = resolveToken(request);

        // 关键：容器的 requestURI 含有上下文路径（如 /campus_repair_system_war/api/account/login），
        // 而路由表以 /api/... 为基准，必须先剥离上下文路径，否则所有接口都会返回"接口不存在"。
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.isEmpty()) {
            path = "/";
        }

        View view = DISPATCHER.dispatchWithParams(request.getMethod(), path, paramMap, token);

        if (view.isRedirect()) {
            response.sendRedirect(String.valueOf(view.getJson().getData()));
            return;
        }
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        PrintWriter writer = response.getWriter();
        writer.write(DISPATCHER.render(view));
        writer.flush();
    }

    /**
     * 读取会话令牌：Cookie 优先，其次请求参数。
     *
     * <p>浏览器可能同时携带多个同名 Cookie（例如历史上以 path=/ 与 path=/应用上下文 各写入一次），
     * 因此这里收集全部候选值，优先返回能够对应到有效会话的那个，避免误判为未登录。</p>
     */
    private String resolveToken(HttpServletRequest request) {
        java.util.List<String> candidates = new java.util.ArrayList<String>();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (TOKEN_COOKIE.equals(cookie.getName()) && cookie.getValue() != null
                        && !cookie.getValue().isEmpty()) {
                    candidates.add(cookie.getValue());
                }
            }
        }
        String param = request.getParameter("token");
        if (param != null && !param.isEmpty()) {
            candidates.add(param);
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object value = session.getAttribute(TOKEN_COOKIE);
            if (value != null) {
                candidates.add(String.valueOf(value));
            }
        }
        for (String candidate : candidates) {
            if (SessionManager.get(candidate) != null) {
                return candidate;
            }
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /** 退出登录时同步失效会话（供 /api/account/logout 调用后由容器清理） */
    static void invalidateContainerSession(String token) {
        SessionManager.invalidate(token);
    }
}
