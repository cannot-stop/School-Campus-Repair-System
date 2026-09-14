package com.campus.repair.web.servlet;

import com.campus.repair.config.AppConfig;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.jdbc.Database;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * Web 容器启动监听器（对应设计书 2.1 控制层的运行环境初始化）。
 *
 * <p>职责：读取容器上下文参数作为配置 → 初始化数据访问工厂 → 装载演示数据（内存库模式）
 * → 启动自检并打印清晰诊断，避免出现"接口能访问但账号登不进"这类静默失败。</p>
 *
 * <p>配置优先级（高 → 低）：环境变量（如 CAMPUS_DB_PASSWORD）&gt; web.xml 的 context-param
 * &gt; classpath 下 config.properties &gt; 内置默认值。</p>
 *
 * <p><b>编译说明</b>：本类依赖 Jakarta Servlet API（servlet-api.jar）与 JSP API，
 * 仅在部署到 Tomcat/Jetty 等容器时需要编译；使用内置服务器运行时无需本包。</p>
 */
public class CampusContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // 1) 容器上下文参数（web.xml）作为配置来源，等价于内置服务器的 -D 参数
        String[] keys = {"storage.mode", "db.driver", "db.url", "db.username", "db.password",
                "web.root", "dispatch.acceptDeadlineMinutes", "order.confirmDeadlineHours"};
        for (String key : keys) {
            String value = event.getServletContext().getInitParameter(key);
            if (value != null && !value.trim().isEmpty()) {
                System.setProperty(key, value.trim());
            }
        }
        AppConfig.load();

        // 2) 初始化数据访问层
        DaoFactory.init();

        // 3) 内存库模式装载演示数据（内容与 db/seed.sql 一致）
        if (DaoFactory.isMemoryMode()) {
            try {
                com.campus.repair.boot.DemoDataLoader.load();
            } catch (RuntimeException e) {
                log(event, "[错误] 演示数据装载失败：" + e);
            }
        }

        // 4) 启动自检：数据库连通性 + 可用账号数
        int accountCount = countAccounts();
        String dbStatus = DaoFactory.isJdbcMode() ? Database.testConnection() : "（内存库模式，未连接数据库）";
        log(event, "校园报修系统启动完成：存储模式=" + DaoFactory.mode()
                + "，数据库=" + dbStatus + "，可用账户数=" + accountCount);

        if (DaoFactory.isJdbcMode() && dbStatus.startsWith("FAILED")) {
            log(event, "[错误] 数据库连接失败，登录将不可用。请检查 web.xml 的 db.url/db.username/db.password，"
                    + "或设置环境变量 CAMPUS_DB_PASSWORD 覆盖密码；并确认已执行 db/schema.sql 与 db/seed.sql。");
        } else if (accountCount == 0) {
            log(event, "[错误] 当前没有任何账户数据，登录将不可用："
                    + (DaoFactory.isJdbcMode() ? "请执行 db/schema.sql 与 db/seed.sql。" : "内存库演示数据装载失败。"));
        } else {
            log(event, "演示账号：student/123456、teacher/123456、worker01-03/worker123、"
                    + "manager/manager123、admin/admin123");
        }
    }

    /** 统计当前可用账户数（用于启动自检） */
    private int countAccounts() {
        try {
            return DaoFactory.userDao().findAll().size();
        } catch (RuntimeException e) {
            log(null, "[错误] 账户数据读取失败：" + e.getMessage());
            return -1;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().log("校园报修系统已停止");
    }

    /** 同时写入容器日志与标准输出（便于在 IDEA 控制台直接看到诊断） */
    private void log(ServletContextEvent event, String message) {
        if (event != null) {
            event.getServletContext().log(message);
        }
        System.out.println("[CampusRepair] " + message);
    }
}
