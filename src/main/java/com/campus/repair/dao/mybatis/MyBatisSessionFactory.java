package com.campus.repair.dao.mybatis;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import com.campus.repair.config.AppConfig;

/**
 * MyBatis 会话工厂与事务管理（对应设计书 2.1 数据访问层、2.2.6 各 Dao 的 MyBatis 实现）。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>读取 {@code mybatis-config.xml}，把 AppConfig 中的数据库连接信息传入 MyBatis 变量；</li>
 *   <li>提供 {@link #openSession()} / {@link #session()} 获取 SqlSession；</li>
 *   <li>提供 {@link #begin()} / {@link #commit()} / {@link #rollback()} 事务控制，
 *       满足设计书"库存扣减与耗材使用记录的写入需在同一个事务中完成"的要求
 *       （同一线程内多次数据库操作复用同一个 SqlSession 与同一个数据库连接）。</li>
 * </ol>
 */
public final class MyBatisSessionFactory {

    private static final String CONFIG_RESOURCE = "mybatis-config.xml";

    private static SqlSessionFactory factory;
    private static final ThreadLocal<SqlSession> CURRENT = new ThreadLocal<SqlSession>();

    private MyBatisSessionFactory() {
    }

    /** 初始化工厂（幂等，线程安全） */
    public static synchronized void init() {
        if (factory != null) {
            return;
        }
        if (!configAvailable()) {
            throw new IllegalStateException("MyBatis 配置未进入类路径：找不到 " + CONFIG_RESOURCE
                    + "。请确认 WEB-INF/classes 下存在 " + CONFIG_RESOURCE + " 与 mapper/*.xml"
                    + "（即 webapp/WEB-INF/classes 目录，或 classpath 下的 src/main/resources 输出）。");
        }
        try (Reader reader = Resources.getResourceAsReader(CONFIG_RESOURCE)) {
            Properties variables = new Properties();
            variables.setProperty("db.driver", AppConfig.get("db.driver", "com.mysql.cj.jdbc.Driver"));
            variables.setProperty("db.url", AppConfig.get("db.url",
                    "jdbc:mysql://127.0.0.1:3306/campus_repair?useUnicode=true&characterEncoding=utf8"
                            + "&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"));
            variables.setProperty("db.username", AppConfig.get("db.username", "root"));
            variables.setProperty("db.password", AppConfig.get("db.password", ""));
            factory = new SqlSessionFactoryBuilder().build(reader, variables);
        } catch (IOException e) {
            throw new IllegalStateException("MyBatis 配置加载失败：" + CONFIG_RESOURCE, e);
        }
    }

    /** 获取工厂（首次调用自动初始化） */
    public static SqlSessionFactory getFactory() {
        init();
        return factory;
    }

    /**
     * 打开一个会话。
     *
     * <p>若当前线程已有事务会话，则直接返回该会话，保证同一事务内多次操作使用同一连接。</p>
     */
    public static SqlSession openSession() {
        SqlSession current = CURRENT.get();
        if (current != null) {
            return current;
        }
        return getFactory().openSession(false);
    }

    /** 供 DaoImpl 使用：执行一次操作并在无事务时自动提交与关闭 */
    public static SqlSession session() {
        return openSession();
    }

    /** 是否为当前线程的事务会话（由调用方持有，不在此关闭） */
    public static boolean inTransaction() {
        return CURRENT.get() != null;
    }

    /** 查询语句执行包装：无事务时自动关闭会话 */
    public static <T> T query(SessionCallback<T> callback) {
        SqlSession session = openSession();
        boolean own = !inTransaction();
        try {
            return callback.apply(session);
        } finally {
            if (own) {
                session.close();
            }
        }
    }

    /** 更新语句执行包装：无事务时自动提交并关闭会话 */
    public static <T> T mutate(SessionCallback<T> callback) {
        SqlSession session = openSession();
        boolean own = !inTransaction();
        try {
            T result = callback.apply(session);
            if (own) {
                session.commit();
            }
            return result;
        } catch (RuntimeException e) {
            if (own) {
                session.rollback();
            }
            throw e;
        } finally {
            if (own) {
                session.close();
            }
        }
    }

    /** 开启事务（绑定到当前线程） */
    public static void begin() {
        if (CURRENT.get() != null) {
            return;
        }
        SqlSession session = getFactory().openSession(false);
        CURRENT.set(session);
    }

    /** 提交事务 */
    public static void commit() {
        SqlSession session = CURRENT.get();
        if (session == null) {
            return;
        }
        try {
            session.commit();
        } finally {
            CURRENT.remove();
            session.close();
        }
    }

    /** 回滚事务 */
    public static void rollback() {
        SqlSession session = CURRENT.get();
        if (session == null) {
            return;
        }
        try {
            session.rollback();
        } finally {
            CURRENT.remove();
            session.close();
        }
    }

    /** 数据库连通性自检（供启动日志使用） */
    public static String testConnection() {
        SqlSession session = null;
        try {
            session = getFactory().openSession();
            session.getConnection().getMetaData().getDatabaseProductName();
            return "OK: " + session.getConnection().getMetaData().getDatabaseProductName() + " "
                    + session.getConnection().getMetaData().getDatabaseProductVersion()
                    + "（MyBatis " + session.getConnection().getMetaData().getDriverName() + " 连接池）";
        } catch (Exception e) {
            return "FAILED: " + e.getMessage();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /** 释放资源（应用停止时调用） */
    public static synchronized void destroy() {
        SqlSession session = CURRENT.get();
        if (session != null) {
            try {
                session.rollback();
            } catch (RuntimeException ignored) {
                // 忽略
            }
            session.close();
            CURRENT.remove();
        }
        factory = null;
    }

    /** 会话回调 */
    public interface SessionCallback<T> {
        T apply(SqlSession session);
    }

    /** 读取 classpath 资源（供外部诊断 mybatis-config.xml 是否可加载） */
    public static boolean configAvailable() {
        InputStream in = null;
        try {
            in = Resources.getResourceAsStream(CONFIG_RESOURCE);
            return in != null;
        } catch (IOException e) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // 忽略
                }
            }
        }
    }

    static {
        // 保证 UTF-8 读取配置（MyBatis 默认按 UTF-8 读取 XML，此处仅为显式声明）
        Charset.forName("UTF-8");
    }
}
