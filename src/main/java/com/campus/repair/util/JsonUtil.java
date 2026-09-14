package com.campus.repair.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轻量 JSON 序列化工具：控制层把 Result/实体序列化为 JSON 响应，无需引入第三方库。
 *
 * <p>支持：字符串、数值、布尔、{@link Date}（输出 yyyy-MM-dd HH:mm:ss）、枚举、Map、
 * 集合、数组以及普通 JavaBean（按 getter 反射输出）。</p>
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    /** 序列化为 JSON 字符串 */
    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof CharSequence || value instanceof Character) {
            appendString(sb, value.toString());
            return;
        }
        if (value instanceof BigDecimal) {
            sb.append(((BigDecimal) value).toPlainString());
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
            return;
        }
        if (value instanceof Enum) {
            appendString(sb, ((Enum<?>) value).name());
            return;
        }
        if (value instanceof Date) {
            appendString(sb, DateUtil.format((Date) value));
            return;
        }
        if (value instanceof Map) {
            writeMap(sb, (Map<?, ?>) value);
            return;
        }
        if (value instanceof Collection) {
            writeCollection(sb, (Collection<?>) value);
            return;
        }
        if (value.getClass().isArray()) {
            writeArray(sb, value);
            return;
        }
        writeBean(sb, value);
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            appendString(sb, String.valueOf(entry.getKey()));
            sb.append(':');
            write(sb, entry.getValue());
        }
        sb.append('}');
    }

    private static void writeCollection(StringBuilder sb, Collection<?> collection) {
        sb.append('[');
        boolean first = true;
        for (Object item : collection) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            write(sb, item);
        }
        sb.append(']');
    }

    private static void writeArray(StringBuilder sb, Object array) {
        sb.append('[');
        int length = java.lang.reflect.Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            write(sb, java.lang.reflect.Array.get(array, i));
        }
        sb.append(']');
    }

    /** 按 getter 反射输出 Bean（忽略 class 属性与静态字段） */
    private static void writeBean(StringBuilder sb, Object bean) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        for (Method method : bean.getClass().getMethods()) {
            String name = method.getName();
            if (method.getParameterTypes().length != 0 || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!name.startsWith("get") && !name.startsWith("is")) {
                continue;
            }
            if ("getClass".equals(name) || "getDeclaredFields".equals(name)) {
                continue;
            }
            String property = name.startsWith("get") ? name.substring(3) : name.substring(2);
            if (property.isEmpty()) {
                continue;
            }
            property = Character.toLowerCase(property.charAt(0)) + property.substring(1);
            try {
                Object value = method.invoke(bean);
                values.put(property, value);
            } catch (Exception ignored) {
                // 跳过不可访问的属性
            }
        }
        writeMap(sb, values);
    }

    private static void appendString(StringBuilder sb, String text) {
        if (text == null) {
            sb.append("null");
            return;
        }
        sb.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    /** 简易 JSON 解析（仅支持对象与字符串/数值字段，用于接口测试与命令行工具） */
    public static Map<String, String> parseFlatObject(String json) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (json == null) {
            return result;
        }
        String text = json.trim();
        if (text.startsWith("{")) {
            text = text.substring(1);
        }
        if (text.endsWith("}")) {
            text = text.substring(0, text.length() - 1);
        }
        String[] pairs = text.split(",");
        for (String pair : pairs) {
            int index = pair.indexOf(':');
            if (index <= 0) {
                continue;
            }
            String key = pair.substring(0, index).trim().replace("\"", "");
            String value = pair.substring(index + 1).trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
            result.put(key, value);
        }
        return result;
    }

    /** 从 JSON 文本中读取某个字符串字段（供 HTTP 客户端解析响应） */
    public static String readField(String json, String field) {
        if (json == null || field == null) {
            return null;
        }
        String key = "\"" + field + "\"";
        int start = json.indexOf(key);
        if (start < 0) {
            return null;
        }
        int colon = json.indexOf(':', start + key.length());
        if (colon < 0) {
            return null;
        }
        int index = colon + 1;
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
        if (index >= json.length()) {
            return null;
        }
        if (json.charAt(index) == '"') {
            StringBuilder sb = new StringBuilder();
            index++;
            while (index < json.length()) {
                char c = json.charAt(index);
                if (c == '\\') {
                    index++;
                    if (index < json.length()) {
                        sb.append(json.charAt(index));
                    }
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
                index++;
            }
            return sb.toString();
        }
        int end = index;
        while (end < json.length() && ",}]".indexOf(json.charAt(end)) < 0) {
            end++;
        }
        return json.substring(index, end).trim();
    }

    /** 反射读取字段（供自测断言使用） */
    public static Object field(Object bean, String name) {
        try {
            Field field = bean.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(bean);
        } catch (Exception e) {
            return null;
        }
    }
}
