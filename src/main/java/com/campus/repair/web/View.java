package com.campus.repair.web;

import java.util.Map;

import com.campus.repair.common.Result;

/**
 * 控制层处理结果：JSON 数据 或 静态页面路径。
 */
public class View {

    private final Result<?> json;
    private final String page;
    private final String contentType;

    private View(Result<?> json, String page, String contentType) {
        this.json = json;
        this.page = page;
        this.contentType = contentType;
    }

    /** 返回 JSON 结果 */
    public static View json(Result<?> result) {
        return new View(result, null, "application/json; charset=UTF-8");
    }

    /** 返回文本 */
    public static View text(String content) {
        Result<String> result = Result.success(content);
        return new View(result, null, "text/plain; charset=UTF-8");
    }

    /** 转发到静态页面 */
    public static View page(String pagePath) {
        return new View(null, pagePath, "text/html; charset=UTF-8");
    }

    /** 重定向 */
    public static View redirect(String location) {
        return new View(Result.success((Object) location), null, "redirect");
    }

    public boolean isJson() {
        return json != null && !"redirect".equals(contentType);
    }

    public boolean isRedirect() {
        return "redirect".equals(contentType);
    }

    public Result<?> getJson() {
        return json;
    }

    public String getPage() {
        return page;
    }

    public String getContentType() {
        return contentType;
    }

    /** 便捷方法：根据数据构建成功结果 */
    public static View ok(Object data) {
        return json(Result.success(data));
    }

    /** 便捷方法：错误结果 */
    public static View fail(int code, String message) {
        return json(Result.fail(code, message));
    }

    /** 便捷方法：Map 数据 */
    public static View ok(Map<String, Object> data) {
        return json(Result.success(data));
    }
}
