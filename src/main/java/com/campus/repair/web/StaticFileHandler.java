package com.campus.repair.web;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * 静态资源处理器：向前端页面提供 HTML/CSS/JS/图片等静态资源。
 *
 * <p>前端采用静态页面（等价于设计书中的 JSP 视图层）通过 JSON 接口与后端交互，
 * 因此部署时既可直接使用内置服务器，也可放入任意 Web 容器（Tomcat/Nginx）。</p>
 */
public class StaticFileHandler {

    private final File webRoot;
    private static final Map<String, String> CONTENT_TYPES = new HashMap<String, String>();

    static {
        CONTENT_TYPES.put("html", "text/html; charset=UTF-8");
        CONTENT_TYPES.put("htm", "text/html; charset=UTF-8");
        CONTENT_TYPES.put("css", "text/css; charset=UTF-8");
        CONTENT_TYPES.put("js", "application/javascript; charset=UTF-8");
        CONTENT_TYPES.put("json", "application/json; charset=UTF-8");
        CONTENT_TYPES.put("png", "image/png");
        CONTENT_TYPES.put("jpg", "image/jpeg");
        CONTENT_TYPES.put("jpeg", "image/jpeg");
        CONTENT_TYPES.put("gif", "image/gif");
        CONTENT_TYPES.put("svg", "image/svg+xml");
        CONTENT_TYPES.put("ico", "image/x-icon");
        CONTENT_TYPES.put("txt", "text/plain; charset=UTF-8");
        CONTENT_TYPES.put("woff", "font/woff");
        CONTENT_TYPES.put("woff2", "font/woff2");
    }

    public StaticFileHandler(File webRoot) {
        this.webRoot = webRoot;
    }

    public File getWebRoot() {
        return webRoot;
    }

    /** 解析静态资源文件，不存在或越权访问返回 null */
    public File resolve(String path) {
        if (path == null) {
            return null;
        }
        String relative = path;
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        if (relative.isEmpty()) {
            relative = "index.html";
        }
        // 阻止目录穿越
        if (relative.contains("..")) {
            return null;
        }
        File file = new File(webRoot, relative);
        if (file.isDirectory()) {
            file = new File(file, "index.html");
        }
        if (!file.isFile()) {
            return null;
        }
        try {
            String canonicalRoot = webRoot.getCanonicalPath();
            String canonicalFile = file.getCanonicalPath();
            if (!canonicalFile.startsWith(canonicalRoot)) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return file;
    }

    /** 写出文件内容 */
    public void write(File file, OutputStream out) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        out.write(bytes);
        out.flush();
    }

    /** 根据扩展名判断 Content-Type */
    public String contentType(File file) {
        String name = file.getName();
        int index = name.lastIndexOf('.');
        String ext = index < 0 ? "" : name.substring(index + 1).toLowerCase();
        String type = CONTENT_TYPES.get(ext);
        return type == null ? "application/octet-stream" : type;
    }
}
