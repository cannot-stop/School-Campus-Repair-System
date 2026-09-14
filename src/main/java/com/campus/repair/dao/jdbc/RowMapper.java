package com.campus.repair.dao.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 查询结果映射器：把 ResultSet 转换为领域对象。
 *
 * <p>对应设计书 2.1 数据访问层"负责 SQL 语句执行与结果对象映射"。
 * 采用"列名 → 属性名"的下划线转驼峰约定映射，无需为每张表编写重复的 setter 代码。</p>
 */
public final class RowMapper {

    private RowMapper() {
    }

    /** 读取单行 */
    public static <T> T map(ResultSet rs, Class<T> type) throws SQLException {
        Map<String, Object> row = toMap(rs);
        return toBean(row, type);
    }

    /** 读取多行 */
    public static <T> List<T> mapList(ResultSet rs, Class<T> type) throws SQLException {
        List<T> list = new ArrayList<T>();
        while (rs.next()) {
            list.add(toBean(toMap(rs), type));
        }
        return list;
    }

    /** 单列单值 */
    public static long count(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return rs.getLong(1);
        }
        return 0L;
    }

    /** 当前行转换为 Map（列名 → 值，列名统一转小写下划线形式） */
    public static Map<String, Object> toMap(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        for (int i = 1; i <= columnCount; i++) {
            String label = meta.getColumnLabel(i);
            if (label == null || label.isEmpty()) {
                label = meta.getColumnName(i);
            }
            row.put(label.toLowerCase(Locale.ROOT), rs.getObject(i));
        }
        return row;
    }

    /**
     * Map 转实体对象：按字段名匹配（支持下划线转驼峰），仅处理字符串/包装类型/String/Date/BigDecimal。
     */
    public static <T> T toBean(Map<String, Object> row, Class<T> type) {
        try {
            T bean = type.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String property = camel(entry.getKey());
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                setProperty(bean, type, property, value);
            }
            return bean;
        } catch (Exception e) {
            throw new IllegalStateException("结果映射失败：" + type.getName(), e);
        }
    }

    /** 下划线转驼峰 */
    public static String camel(String column) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < column.length(); i++) {
            char c = column.charAt(i);
            if (c == '_') {
                upper = true;
                continue;
            }
            sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            upper = false;
        }
        return sb.toString();
    }

    private static void setProperty(Object bean, Class<?> type, String property, Object value) {
        String setter = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        Class<?> valueType = value.getClass();
        try {
            type.getMethod(setter, valueType).invoke(bean, value);
            return;
        } catch (NoSuchMethodException ignored) {
            // 继续尝试兼容类型的 setter
        } catch (Exception e) {
            throw new IllegalStateException("属性设置失败：" + property, e);
        }
        // 数值类型兼容（TINYINT 可能返回 Integer，DATETIME 返回 Timestamp 等）
        for (java.lang.reflect.Method method : type.getMethods()) {
            if (!method.getName().equals(setter) || method.getParameterTypes().length != 1) {
                continue;
            }
            Class<?> target = method.getParameterTypes()[0];
            Object converted = convert(value, target);
            if (converted == null) {
                continue;
            }
            try {
                method.invoke(bean, converted);
                return;
            } catch (Exception e) {
                throw new IllegalStateException("属性设置失败：" + property, e);
            }
        }
    }

    private static Object convert(Object value, Class<?> target) {
        if (target.isInstance(value)) {
            return value;
        }
        if (value instanceof Number) {
            Number number = (Number) value;
            if (target == Integer.class || target == int.class) {
                return Integer.valueOf(number.intValue());
            }
            if (target == Long.class || target == long.class) {
                return Long.valueOf(number.longValue());
            }
            if (target == Double.class || target == double.class) {
                return Double.valueOf(number.doubleValue());
            }
            if (target == java.math.BigDecimal.class) {
                return new java.math.BigDecimal(number.toString());
            }
        }
        // MySQL Connector/J 8.x 默认把 DATETIME 映射为 java.time.LocalDateTime，
        // 这里统一转换为 java.util.Date（实体字段类型），否则时间字段会读取为空。
        if (value instanceof java.time.LocalDateTime) {
            java.time.LocalDateTime dateTime = (java.time.LocalDateTime) value;
            if (target == Date.class || target == java.sql.Timestamp.class || target == Object.class) {
                return java.util.Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
            }
            if (target == java.time.LocalDateTime.class) {
                return dateTime;
            }
        }
        if (value instanceof java.time.LocalDate) {
            java.time.LocalDate date = (java.time.LocalDate) value;
            if (target == Date.class) {
                return java.util.Date.from(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
            }
        }
        if (value instanceof java.time.LocalTime && target == Date.class) {
            java.time.LocalTime time = (java.time.LocalTime) value;
            return java.util.Date.from(java.time.LocalDate.now().atTime(time)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof String) {
            String text = (String) value;
            if (target == Integer.class || target == int.class) {
                try {
                    return Integer.valueOf(Integer.parseInt(text));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            if (target == Date.class) {
                return com.campus.repair.util.DateUtil.parse(text);
            }
        }
        return null;
    }
}
