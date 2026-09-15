package com.campus.repair.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * 系统配置（对应设计书 2.1"辅助类：提供公共工具类、通用结果封装、异常处理和配置管理"）。
 *
 * <p>配置读取顺序：命令行参数 -Dcampus.config=路径 &gt; 环境变量 CAMPUS_CONFIG
 * &gt; classpath 下 config.properties &gt; 内置默认值。</p>
 *
 * <p>关键配置项：</p>
 * <ul>
 *   <li>db.url / db.username / db.password / db.driver：MySQL 连接信息；</li>
 *   <li>storage.mode：jdbc（MySQL 持久化）或 memory（内存库，用于无数据库环境演示与自测）；</li>
 *   <li>server.port / server.contextPath：内嵌服务器监听端口与上下文路径；</li>
 *   <li>dispatch.acceptDeadlineMinutes：接单时限（设计书要求派单后 2 小时）；</li>
 *   <li>order.confirmDeadlineHours：完成登记后结果确认时限（设计书要求 24 小时）。</li>
 * </ul>
 */
public final class AppConfig {

    private static final String DEFAULT_CONFIG = "config.properties";

    private static Properties props = new Properties();
    private static boolean loaded = false;

    private AppConfig() {
    }

    /** 载入配置（幂等） */
    public static synchronized void load() {
        if (loaded) {
            return;
        }
        Properties merged = new Properties();
        // 1. classpath 默认配置
        InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG);
        if (in != null) {
            try {
                Reader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
                merged.load(reader);
                reader.close();
            } catch (IOException e) {
                // 忽略，使用内置默认值
            } finally {
                close(in);
            }
        }
        // 2. 外部配置文件
        String external = System.getProperty("campus.config");
        if (external == null || external.trim().isEmpty()) {
            external = System.getenv("CAMPUS_CONFIG");
        }
        if (external != null && !external.trim().isEmpty()) {
            File file = new File(external.trim());
            if (file.isFile()) {
                InputStream fin = null;
                try {
                    fin = new FileInputStream(file);
                    Reader reader = new InputStreamReader(fin, Charset.forName("UTF-8"));
                    merged.load(reader);
                    reader.close();
                } catch (IOException e) {
                    throw new IllegalStateException("配置文件读取失败：" + file.getAbsolutePath(), e);
                } finally {
                    close(fin);
                }
            }
        }
        // 3. 系统属性覆盖
        for (String name : merged.stringPropertyNames()) {
            String override = System.getProperty(name);
            if (override != null) {
                merged.setProperty(name, override);
            }
        }
        props = merged;
        loaded = true;
    }

    /**
     * 读取配置项。
     *
     * <p>取值优先级：环境变量（CAMPUS_DB_PASSWORD 形式，用于避免在配置文件中明文写库密码）
     * &gt; 系统属性（-Dkey=value）&gt; config.properties &gt; 默认值。</p>
     */
    public static String get(String key, String defaultValue) {
        load();
        String fromEnv = System.getenv(envName(key));
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return fromEnv.trim();
        }
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return value.trim();
    }

    /** 配置项 key 对应的环境变量名：db.password → CAMPUS_DB_PASSWORD */
    public static String envName(String key) {
        return "CAMPUS_" + key.toUpperCase(java.util.Locale.ROOT).replace('.', '_').replace('-', '_');
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    public static Properties all() {
        load();
        return props;
    }

    /** 接单时限（分钟） */
    public static int acceptDeadlineMinutes() {
        return getInt("dispatch.acceptDeadlineMinutes", 120);
    }

    /** 结果确认时限（小时） */
    public static int confirmDeadlineHours() {
        return getInt("order.confirmDeadlineHours", 24);
    }

    private static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }
}
