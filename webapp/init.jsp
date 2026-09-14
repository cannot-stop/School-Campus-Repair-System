<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.campus.repair.dao.DaoFactory" %>
<%@ page import="com.campus.repair.config.AppConfig" %>
<%--
    校园报修系统 · Servlet 容器部署起始页
    职责：
      1) 把容器的上下文路径注入页面（window.__CRS_BASE__ 与 <base>），
         使前端在 Tomcat 的任意上下文（如 /campus_repair_system_war）下都能正确调用 /api 接口；
      2) 渲染登录表单，与 webapp/index.html 完全一致。

    部署后的访问地址示例：http://localhost:8080/campus_repair_system_war/init.jsp
--%>
<%
    String ctx = request.getContextPath();
    if (ctx == null || "/".equals(ctx)) {
        ctx = "";
    }
    String storageMode = DaoFactory.mode();
    String systemName = AppConfig.get("system.name", "校园报修系统");
    String version = AppConfig.get("system.version", "1.0.0");
    // 安全校验：上下文路径只允许字母、数字、下划线、连字符与斜杠，防止注入
    if (!ctx.matches("^(/[A-Za-z0-9_\\-]+)*$")) {
        ctx = "";
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= systemName %> · 登录</title>
    <base href="<%= request.getRequestURL().substring(0, request.getRequestURL().length() - "init.jsp".length()) %>">
    <script>window.__CRS_BASE__ = "<%= ctx %>";</script>
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="auth-body">
<div class="auth-wrap">
    <div class="auth-hero">
        <h1><%= systemName %></h1>
        <p class="subtitle">Campus Repair Service System v<%= version %></p>
        <ul class="hero-list">
            <li>在线提交报修，随时查看处理进度</li>
            <li>维修管理员审核派单，支持智能派单匹配</li>
            <li>维修人员移动处理，进度与耗材全程留痕</li>
            <li>维修结果确认、评价与统计分析闭环</li>
        </ul>
        <div class="hero-footer">
            <span>依据《校园报修系统需求分析和系统详细设计书》实现</span>
            <span>｜ 上下文路径：<%= ctx.isEmpty() ? "/（根上下文）" : ctx %></span>
        </div>
    </div>
    <div class="auth-card">
        <h2>用户登录</h2>
        <div id="alert" class="alert hidden"></div>
        <form id="loginForm" autocomplete="off">
            <label>用户名<input type="text" name="username" placeholder="请输入用户名" required></label>
            <label>密码<input type="password" name="password" placeholder="请输入密码" required></label>
            <label>当前位置（维修人员填写，用于派单参考）
                <input type="text" name="location" placeholder="如：1号教学楼值班室">
            </label>
            <button type="submit" class="btn btn-primary btn-block">登录</button>
        </form>
        <div class="auth-links">
            <a href="register.html">还没有账户？立即注册</a>
        </div>
        <div class="demo-accounts">
            <div class="demo-title">演示账号（数据库由 db/seed.sql 初始化）</div>
            <table class="table table-compact">
                <thead><tr><th>角色</th><th>用户名</th><th>密码</th></tr></thead>
                <tbody>
                <tr><td>报修人（学生）</td><td>student</td><td>123456</td></tr>
                <tr><td>报修人（教职工）</td><td>teacher</td><td>123456</td></tr>
                <tr><td>维修人员</td><td>worker01</td><td>worker123</td></tr>
                <tr><td>维修管理员</td><td>manager</td><td>manager123</td></tr>
                <tr><td>系统管理员</td><td>admin</td><td>admin123</td></tr>
                </tbody>
            </table>
            <div class="demo-tip">
                当前存储模式：<%= "jdbc".equals(storageMode) ? "MySQL 持久化" : "内存库（演示）" %>
                ｜ 上下文路径：<%= ctx.isEmpty() ? "/" : ctx %>
            </div>
        </div>
    </div>
</div>
<script src="js/common.js"></script>
<script src="js/login.js"></script>
</body>
</html>
