package com.campus.repair.dao.jdbc;

import com.campus.repair.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 数据库连接管理（对应设计书 2.1 数据访问层）。
 *
 * <p>提供轻量连接池（避免引入第三方依赖）：按需创建连接，归还后缓存复用；
 * 事务由 {@link #begin()} / {@link #commit()} / {@link #rollback()} 在同一条连接上完成，
 * 满足设计书"库存扣减与耗材使用记录的写入需在同一个事务中完成"的要求。</p>
 */
public final class Database {

    private static final Deque<Connection> POOL = new ArrayDeque<Connection>();
    private static final ThreadLocal<Connection> TX = new ThreadLocal<Connection>();
    private static final int MAX_POOL_SIZE = 8;

    private static boolean driverLoaded = false;

    private Database() {
    }

    /** 载入 JDBC 驱动 */
    private static synchronized void loadDriver() throws SQLException {
        if (driverLoaded) {
            return;
        }
        String driver = AppConfig.get("db.driver", "com.mysql.cj.jdbc.Driver");
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("未找到数据库驱动 " + driver + "，请确认已将 mysql-connector-j 加入 classpath", e);
        }
        driverLoaded = true;
    }

    /** 获取连接（若处于事务中则返回事务连接） */
    public static Connection getConnection() throws SQLException {
        Connection tx = TX.get();
        if (tx != null) {
            return tx;
        }
        loadDriver();
        synchronized (POOL) {
            while (!POOL.isEmpty()) {
                Connection conn = POOL.pollFirst();
                try {
                    if (conn.isClosed() || !conn.isValid(2)) {
                        continue;
                    }
                    return conn;
                } catch (SQLException e) {
                    // 丢弃失效连接
                }
            }
        }
        return DriverManager.getConnection(
                AppConfig.get("db.url", "jdbc:mysql://127.0.0.1:3306/campus_repair"
                        + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"),
                AppConfig.get("db.username", "root"),
                AppConfig.get("db.password", ""));
    }

    /** 归还连接 */
    public static void release(Connection conn) {
        if (conn == null || TX.get() == conn) {
            return;
        }
        synchronized (POOL) {
            if (POOL.size() < MAX_POOL_SIZE) {
                POOL.addLast(conn);
                return;
            }
        }
        try {
            conn.close();
        } catch (SQLException ignored) {
            // ignore
        }
    }

    /** 开启事务（绑定到当前线程） */
    public static void begin() throws SQLException {
        if (TX.get() != null) {
            return;
        }
        Connection conn = getConnection();
        conn.setAutoCommit(false);
        TX.set(conn);
    }

    /** 提交事务 */
    public static void commit() {
        Connection conn = TX.get();
        if (conn == null) {
            return;
        }
        try {
            conn.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("事务提交失败", e);
        } finally {
            TX.remove();
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
                // ignore
            }
            release(conn);
        }
    }

    /** 回滚事务 */
    public static void rollback() {
        Connection conn = TX.get();
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException ignored) {
            // ignore
        } finally {
            TX.remove();
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
                // ignore
            }
            release(conn);
        }
    }

    /** 是否处于事务中 */
    public static boolean inTransaction() {
        return TX.get() != null;
    }

    /** 连接可用性检测（启动自检使用） */
    public static String testConnection() {
        Connection conn = null;
        try {
            conn = getConnection();
            return "OK: " + conn.getMetaData().getDatabaseProductName() + " "
                    + conn.getMetaData().getDatabaseProductVersion();
        } catch (Exception e) {
            return "FAILED: " + e.getMessage();
        } finally {
            release(conn);
        }
    }
}
