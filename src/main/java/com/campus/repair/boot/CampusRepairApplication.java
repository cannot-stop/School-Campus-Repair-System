package com.campus.repair.boot;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import com.campus.repair.common.Result;
import com.campus.repair.config.AppConfig;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.jdbc.Database;
import com.campus.repair.service.DispatchService;
import com.campus.repair.util.DateUtil;
import com.campus.repair.util.JsonUtil;
import com.campus.repair.web.Dispatcher;
import com.campus.repair.web.ParamMap;
import com.campus.repair.web.SessionManager;
import com.campus.repair.web.StaticFileHandler;
import com.campus.repair.web.View;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * 校园报修系统启动入口（对应设计书 2.1 表示层与控制层的运行载体）。
 *
 * <p>使用 JDK 内置 HTTP 服务器承载前端静态页面与 JSON 接口，零第三方依赖，开箱即可运行；
 * 部署到 Tomcat 等容器时，可将 webapp 目录内容作为 Web 资源发布，接口契约完全一致。</p>
 *
 * <p>启动参数（系统属性）：<br>
 * -Dserver.port=8080            监听端口<br>
 * -Dstorage.mode=jdbc|memory    存储模式<br>
 * -Dweb.root=webapp             前端资源目录<br>
 * -Dcampus.config=路径          外部配置文件</p>
 */
public final class CampusRepairApplication {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Map<String, String> MIME = new HashMap<String, String>();

    static {
        MIME.put("html", "text/html; charset=UTF-8");
        MIME.put("css", "text/css; charset=UTF-8");
        MIME.put("js", "application/javascript; charset=UTF-8");
        MIME.put("json", "application/json; charset=UTF-8");
        MIME.put("png", "image/png");
        MIME.put("jpg", "image/jpeg");
        MIME.put("svg", "image/svg+xml");
        MIME.put("ico", "image/x-icon");
    }

    private CampusRepairApplication() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig.load();
        if (args.length > 0) {
            for (String arg : args) {
                if (arg.startsWith("--port=")) {
                    System.setProperty("server.port", arg.substring("--port=".length()));
                } else if (arg.startsWith("--mode=")) {
                    System.setProperty("storage.mode", arg.substring("--mode=".length()));
                } else if (arg.startsWith("--web=")) {
                    System.setProperty("web.root", arg.substring("--web=".length()));
                }
            }
        }
        DaoFactory.init();
        int port = AppConfig.getInt("server.port", 8080);
        File webRoot = resolveWebRoot();
        Dispatcher dispatcher = new Dispatcher();

        // 内存库模式装载演示数据（内容与 db/seed.sql 一致）
        DemoDataLoader.load();

        banner(dispatcher, port, webRoot);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> handle(exchange, dispatcher, webRoot));
        server.setExecutor(Executors.newFixedThreadPool(24));
        server.start();

        // 定时任务：接单超时提醒、结果确认超时提醒、会话清理
        Timer timer = new Timer("campus-repair-scheduler", true);
        final DispatchService dispatchService = new DispatchService();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    int accept = dispatchService.remindOverdueAccept();
                    int confirm = dispatchService.remindOverdueConfirm();
                    int expired = SessionManager.cleanExpired();
                    if (accept > 0 || confirm > 0 || expired > 0) {
                        System.out.println("[" + DateUtil.format(new java.util.Date()) + "] 定时任务：接单超时提醒 "
                                + accept + " 条，确认超时提醒 " + confirm + " 条，清理过期会话 " + expired + " 个");
                    }
                } catch (Exception e) {
                    System.err.println("定时任务执行失败：" + e.getMessage());
                }
            }
        }, 30000L, 60000L);

        System.out.println("服务已启动：http://127.0.0.1:" + port + "/");
        System.out.println("按 Ctrl+C 停止服务。");
    }

    /** 处理一次 HTTP 请求：优先匹配 API 路由，其次静态资源 */
    private static void handle(HttpExchange exchange, Dispatcher dispatcher, File webRoot) throws IOException {
        String rawPath = exchange.getRequestURI().getRawPath();
        String query = exchange.getRequestURI().getRawQuery();
        String path = rawPath;
        if (query != null && !query.isEmpty()) {
            path = rawPath + "?" + query;
        }
        try {
            if (rawPath.startsWith("/api/")) {
                String body = ParamMap.readBody(exchange.getRequestBody(), 2 * 1024 * 1024);
                String token = readToken(exchange, query);
                View view = dispatcher.dispatch(exchange.getRequestMethod(), path, body, token);
                if (view.isRedirect()) {
                    Headers headers = exchange.getResponseHeaders();
                    headers.set("Location", String.valueOf(view.getJson().getData()));
                    exchange.sendResponseHeaders(302, -1);
                    return;
                }
                byte[] bytes = dispatcher.render(view).getBytes(UTF8);
                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Type", "application/json; charset=UTF-8");
                headers.set("Cache-Control", "no-store");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream out = exchange.getResponseBody();
                out.write(bytes);
                out.flush();
                out.close();
                return;
            }
            // 静态资源
            File file = new File(webRoot, rawPath);
            if (rawPath.equals("/") || rawPath.isEmpty()) {
                file = new File(webRoot, "index.html");
            }
            if (!file.isFile()) {
                file = new File(webRoot, "404.html");
                if (!file.isFile()) {
                    String text = "404 Not Found: " + rawPath;
                    exchange.sendResponseHeaders(404, text.getBytes(UTF8).length);
                    OutputStream out = exchange.getResponseBody();
                    out.write(text.getBytes(UTF8));
                    out.close();
                    return;
                }
            }
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String type = guessType(file.getName());
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", type);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream out = exchange.getResponseBody();
            out.write(bytes);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            byte[] bytes = JsonUtil.toJson(Result.fail(5000, "服务器内部错误：" + e.getMessage())).getBytes(UTF8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream out = exchange.getResponseBody();
            out.write(bytes);
            out.close();
        } finally {
            exchange.close();
        }
    }

    /**
     * 读取会话令牌：Cookie 优先，其次请求参数 token。
     * 浏览器可能同时携带多个同名 Cookie（历史遗留 path=/ 与 path=/上下文），
     * 因此优先返回能够对应到有效会话的那个。
     */
    private static String readToken(HttpExchange exchange, String query) {
        java.util.List<String> candidates = new java.util.ArrayList<String>();
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) {
            for (String item : cookie.split(";")) {
                String trimmed = item.trim();
                if (trimmed.startsWith("CRS_TOKEN=")) {
                    String value = trimmed.substring("CRS_TOKEN=".length());
                    if (!value.isEmpty()) {
                        candidates.add(value);
                    }
                }
            }
        }
        Map<String, String> params = ParamMap.parseUrlEncoded(query);
        String param = params.get("token");
        if (param != null && !param.isEmpty()) {
            candidates.add(param);
        }
        for (String candidate : candidates) {
            if (com.campus.repair.web.SessionManager.get(candidate) != null) {
                return candidate;
            }
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /** 解析前端资源目录 */
    private static File resolveWebRoot() {
        String configured = System.getProperty("web.root", AppConfig.get("web.root", "webapp"));
        File dir = new File(configured);
        if (!dir.isDirectory()) {
            File alt = new File("campus-repair-system/" + configured);
            if (alt.isDirectory()) {
                dir = alt;
            }
        }
        return dir;
    }

    private static String guessType(String name) {
        int index = name.lastIndexOf('.');
        String ext = index < 0 ? "" : name.substring(index + 1).toLowerCase();
        String type = MIME.get(ext);
        return type == null ? "application/octet-stream" : type;
    }

    /** 启动横幅：输出系统信息、存储模式与接口清单 */
    private static void banner(Dispatcher dispatcher, int port, File webRoot) {
        System.out.println("============================================================");
        System.out.println(" 校园报修系统 " + AppConfig.get("system.version", "1.0.0")
                + "（依据《校园报修系统需求分析和系统详细设计书》实现）");
        System.out.println("============================================================");
        System.out.println(" 存储模式   : " + DaoFactory.mode()
                + ("jdbc".equals(DaoFactory.mode()) ? "（MySQL 持久化）" : "（内存库，重启后数据重置）"));
        if (DaoFactory.isJdbcMode()) {
            System.out.println(" 数据库连接 : " + Database.testConnection());
        }
        System.out.println(" 前端资源   : " + webRoot.getAbsolutePath());
        System.out.println(" 监听端口   : " + port);
        System.out.println(" 接口数量   : " + dispatcher.routeCount());
        if (DaoFactory.isMemoryMode()) {
            System.out.println(" " + DemoDataLoader.summary());
        }
        System.out.println("------------------------------------------------------------");
        for (String route : dispatcher.routeList()) {
            System.out.println("  " + route);
        }
        System.out.println("============================================================");
    }
}
