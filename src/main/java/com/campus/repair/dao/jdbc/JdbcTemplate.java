package com.campus.repair.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.campus.repair.config.AppConfig;

/**
 * JDBC 执行模板：统一处理连接获取、参数绑定、异常转换与资源释放。
 *
 * <p>对应设计书 2.1 数据访问层职责。当 storage.mode=jdbc 时由各 DaoImpl 使用。</p>
 */
public final class JdbcTemplate {

    private JdbcTemplate() {
    }

    /** 查询列表 */
    public static <T> List<T> queryList(String sql, Class<T> type, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Database.getConnection();
            ps = conn.prepareStatement(sql);
            bind(ps, params);
            rs = ps.executeQuery();
            return RowMapper.mapList(rs, type);
        } catch (SQLException e) {
            throw new IllegalStateException("查询失败：" + sql + " —— " + e.getMessage(), e);
        } finally {
            close(rs, ps, conn);
        }
    }

    /** 查询单条 */
    public static <T> T queryOne(String sql, Class<T> type, Object... params) {
        List<T> list = queryList(sql, type, params);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 查询行数 */
    public static long queryCount(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Database.getConnection();
            ps = conn.prepareStatement(sql);
            bind(ps, params);
            rs = ps.executeQuery();
            return RowMapper.count(rs);
        } catch (SQLException e) {
            throw new IllegalStateException("统计失败：" + sql + " —— " + e.getMessage(), e);
        } finally {
            close(rs, ps, conn);
        }
    }

    /** 查询单值（字符串/数值） */
    public static Object queryScalar(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Database.getConnection();
            ps = conn.prepareStatement(sql);
            bind(ps, params);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getObject(1);
            }
            return null;
        } catch (SQLException e) {
            throw new IllegalStateException("查询失败：" + sql + " —— " + e.getMessage(), e);
        } finally {
            close(rs, ps, conn);
        }
    }

    /** 查询自定义行（列名 → 值） */
    public static List<java.util.Map<String, Object>> queryRows(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Database.getConnection();
            ps = conn.prepareStatement(sql);
            bind(ps, params);
            rs = ps.executeQuery();
            List<java.util.Map<String, Object>> rows = new ArrayList<java.util.Map<String, Object>>();
            while (rs.next()) {
                rows.add(RowMapper.toMap(rs));
            }
            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException("查询失败：" + sql + " —— " + e.getMessage(), e);
        } finally {
            close(rs, ps, conn);
        }
    }

    /** 执行更新（insert/update/delete），返回影响行数 */
    public static int update(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = Database.getConnection();
            ps = conn.prepareStatement(sql);
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("更新失败：" + sql + " —— " + e.getMessage(), e);
        } finally {
            close(null, ps, conn);
        }
    }

    /** 执行插入并返回自增主键 */
    public static int insertReturnKey(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Database.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            bind(ps, params);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new IllegalStateException("插入失败：" + sql + " —— " + e.getMessage(), e);
        } finally {
            close(rs, ps, conn);
        }
    }

    /** 执行脚本文件（按分号切分），供命令行初始化使用 */
    public static int executeScript(String scriptContent) {
        if (scriptContent == null) {
            return 0;
        }
        Connection conn = null;
        Statement st = null;
        try {
            conn = Database.getConnection();
            st = conn.createStatement();
            int count = 0;
            for (String raw : splitSql(scriptContent)) {
                st.execute(raw);
                count++;
            }
            return count;
        } catch (SQLException e) {
            throw new IllegalStateException("脚本执行失败：" + e.getMessage(), e);
        } finally {
            close(null, st, conn);
        }
    }

    /** 简单 SQL 脚本切分：忽略注释行，按分号断句 */
    public static List<String> splitSql(String script) {
        List<String> statements = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (String line : script.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.startsWith("#") || trimmed.isEmpty()) {
                continue;
            }
            sb.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String sql = sb.toString().trim();
                sql = sql.substring(0, sql.length() - 1).trim();
                if (!sql.isEmpty()) {
                    statements.add(sql);
                }
                sb.setLength(0);
            }
        }
        String rest = sb.toString().trim();
        if (!rest.isEmpty()) {
            statements.add(rest);
        }
        return statements;
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value instanceof java.util.Date && !(value instanceof java.sql.Date)
                    && !(value instanceof java.sql.Timestamp)) {
                value = new java.sql.Timestamp(((java.util.Date) value).getTime());
            }
            if (value != null && value.getClass().isEnum()) {
                value = value.toString();
            }
            ps.setObject(i + 1, value);
        }
    }

    private static void close(ResultSet rs, Statement st, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
                // ignore
            }
        }
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ignored) {
                // ignore
            }
        }
        Database.release(conn);
    }

    /** 当前是否为 JDBC 模式 */
    public static boolean enabled() {
        return AppConfig.isJdbcMode();
    }
}
