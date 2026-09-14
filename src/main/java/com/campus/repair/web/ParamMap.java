package com.campus.repair.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 请求参数封装：合并 URL 查询串与表单/JSON 请求体。
 *
 * <p>等价于 JSP/Servlet 场景下的 request.getParameter()，测试与内嵌服务器共用。</p>
 */
public class ParamMap {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final Map<String, String> values = new LinkedHashMap<String, String>();

    public static ParamMap create() {
        return new ParamMap();
    }

    /** 解析查询串（a=1&b=2） */
    public static ParamMap fromQuery(String query) {
        ParamMap params = new ParamMap();
        params.putAll(parseUrlEncoded(query));
        return params;
    }

    public ParamMap putAll(Map<String, String> source) {
        if (source != null) {
            for (Map.Entry<String, String> entry : source.entrySet()) {
                if (entry.getValue() != null) {
                    values.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return this;
    }

    public ParamMap put(String key, String value) {
        if (value != null) {
            values.put(key, value);
        }
        return this;
    }

    public String get(String key) {
        return values.get(key);
    }

    public String get(String key, String defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : value;
    }

    public Integer getInt(String key) {
        String value = values.get(key);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean getBoolean(String key) {
        String value = values.get(key);
        if (value == null) {
            return false;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()) || "on".equalsIgnoreCase(value.trim());
    }

    public Map<String, String> asMap() {
        return values;
    }

    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    /** 解析 application/x-www-form-urlencoded 文本 */
    public static Map<String, String> parseUrlEncoded(String text) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        for (String pair : text.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int index = pair.indexOf('=');
            String key;
            String value;
            if (index < 0) {
                key = pair;
                value = "";
            } else {
                key = pair.substring(0, index);
                value = pair.substring(index + 1);
            }
            result.put(decode(key), decode(value));
        }
        return result;
    }

    /** 解析 JSON 平铺对象（支持字符串/数值/布尔/嵌套一层的数组字段忽略） */
    public static Map<String, String> parseJson(String json) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (json == null) {
            return result;
        }
        String text = json.trim();
        if (!text.startsWith("{")) {
            return parseUrlEncoded(text);
        }
        int index = 1;
        while (index < text.length()) {
            int keyStart = text.indexOf('"', index);
            if (keyStart < 0) {
                break;
            }
            int keyEnd = text.indexOf('"', keyStart + 1);
            if (keyEnd < 0) {
                break;
            }
            String key = text.substring(keyStart + 1, keyEnd);
            int colon = text.indexOf(':', keyEnd);
            if (colon < 0) {
                break;
            }
            int valueStart = colon + 1;
            while (valueStart < text.length() && Character.isWhitespace(text.charAt(valueStart))) {
                valueStart++;
            }
            if (valueStart >= text.length()) {
                break;
            }
            char first = text.charAt(valueStart);
            String value;
            if (first == '"') {
                int end = valueStart + 1;
                StringBuilder sb = new StringBuilder();
                while (end < text.length()) {
                    char c = text.charAt(end);
                    if (c == '\\' && end + 1 < text.length()) {
                        sb.append(text.charAt(end + 1));
                        end += 2;
                        continue;
                    }
                    if (c == '"') {
                        break;
                    }
                    sb.append(c);
                    end++;
                }
                value = sb.toString();
                index = end + 1;
            } else if (first == '[' || first == '{') {
                char close = first == '[' ? ']' : '}';
                int depth = 0;
                int end = valueStart;
                while (end < text.length()) {
                    char c = text.charAt(end);
                    if (c == first) {
                        depth++;
                    } else if (c == close) {
                        depth--;
                        if (depth == 0) {
                            break;
                        }
                    }
                    end++;
                }
                value = text.substring(valueStart, Math.min(end + 1, text.length()));
                index = end + 1;
            } else {
                int end = valueStart;
                while (end < text.length() && ",}".indexOf(text.charAt(end)) < 0) {
                    end++;
                }
                value = text.substring(valueStart, end).trim();
                index = end + 1;
            }
            result.put(key, value);
            int comma = text.indexOf(',', index);
            if (comma < 0) {
                break;
            }
            index = comma + 1;
        }
        return result;
    }

    /** 解析形如 [{matId:1,useCount:2},{...}] 的数组为若干 Map */
    public static List<Map<String, String>> parseJsonArray(String json) {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        if (json == null || json.trim().isEmpty()) {
            return list;
        }
        String text = json.trim();
        int index = 0;
        while (index < text.length()) {
            int start = text.indexOf('{', index);
            if (start < 0) {
                break;
            }
            int depth = 0;
            int end = start;
            while (end < text.length()) {
                char c = text.charAt(end);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        break;
                    }
                }
                end++;
            }
            if (end >= text.length()) {
                break;
            }
            list.add(parseJson(text.substring(start, end + 1)));
            index = end + 1;
        }
        return list;
    }

    /** 读取输入流全部内容 */
    public static String readBody(InputStream in, int maxBytes) throws IOException {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        int read;
        while ((read = in.read(buffer)) > 0) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("请求体过大");
            }
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), UTF8);
    }

    private static String decode(String text) {
        try {
            return URLDecoder.decode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return text;
        } catch (IllegalArgumentException e) {
            return text;
        }
    }
}
