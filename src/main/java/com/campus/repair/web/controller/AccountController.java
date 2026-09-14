package com.campus.repair.web.controller;

import java.util.HashMap;
import java.util.Map;

import com.campus.repair.config.AppConfig;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.domain.Role;
import com.campus.repair.domain.User;
import com.campus.repair.web.RequestContext;
import com.campus.repair.web.Route;
import com.campus.repair.web.Session;
import com.campus.repair.web.SessionManager;
import com.campus.repair.web.View;

/**
 * 账户管理控制器（对应设计书 2.2.6 AccountController 与 2.2.2 账户管理模块设计）。
 *
 * <p>方法：register()、login()、logout()、profile()、modifyInfo()、modifyPassword()、
 * auditUser()、listUsers()、lockUser()、baseData()、saveBaseData()。</p>
 */
public class AccountController extends BaseController {

    /** 账户注册 */
    @Route(value = "/api/account/register", publicRoute = true)
    public View register(RequestContext ctx) {
        User user = accountService.register(ctx.getParams().asMap());
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("auditStatusText", user.getAuditStatusText());
        return View.json(com.campus.repair.common.Result.success(
                "注册成功，账户已提交实名审核，审核通过后即可登录", data));
    }

    /** 登录 */
    @Route(value = "/api/account/login", publicRoute = true)
    public View login(RequestContext ctx) {
        User user = accountService.login(ctx.param("username"), ctx.param("password"));
        Session session = SessionManager.create(user);
        accountService.markOnline(user.getUserId(), ctx.param("location"));
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("token", session.getToken());
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        data.put("roleText", Role.textOf(user.getRole()));
        data.put("loginTime", com.campus.repair.util.DateUtil.format(session.getLoginTime()));
        return View.json(com.campus.repair.common.Result.success("登录成功，欢迎回来 " + user.getRealName(), data));
    }

    /** 注销 */
    @Route("/api/account/logout")
    public View logout(RequestContext ctx) {
        accountService.logout(ctx.getUserId());
        SessionManager.invalidate(ctx.getSession().getToken());
        return View.json(com.campus.repair.common.Result.success("已安全退出登录", null));
    }

    /** 个人信息与工作台统计 */
    @Route("/api/account/profile")
    public View profile(RequestContext ctx) {
        Map<String, Object> data = accountService.profile(ctx.getUserId());
        data.put("token", ctx.getSession().getToken());
        data.put("storageMode", DaoFactory.mode());
        data.put("dbAvailable", Boolean.valueOf(!DaoFactory.isMemoryMode()));
        return View.ok(data);
    }

    /** 修改账户信息（手机号、姓名） */
    @Route("/api/account/modifyInfo")
    public View modifyInfo(RequestContext ctx) {
        User user = accountService.modifyInfo(ctx.getUserId(), ctx.param("phone"), ctx.param("realName"));
        return View.json(com.campus.repair.common.Result.success("账户信息修改成功", user));
    }

    /** 修改密码 */
    @Route("/api/account/modifyPassword")
    public View modifyPassword(RequestContext ctx) {
        accountService.modifyPassword(ctx.getUserId(), ctx.param("oldPassword"),
                ctx.param("newPassword"), ctx.param("confirmPassword"));
        return View.json(com.campus.repair.common.Result.success("密码修改成功，请使用新密码登录", null));
    }

    /** 退出当前会话并同步维修工离线状态 */
    @Route("/api/account/offline")
    public View offline(RequestContext ctx) {
        accountService.logout(ctx.getUserId());
        SessionManager.invalidate(ctx.getSession().getToken());
        return View.json(com.campus.repair.common.Result.success("已切换为离线状态", null));
    }

    // ------------------------------------------------------------ 系统管理员

    /** 账户列表 */
    @Route(value = "/api/account/list", roles = {"admin"})
    public View list(RequestContext ctx) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("users", accountService.listUsers(ctx.param("role")));
        data.put("statistics", accountService.userStatistics());
        data.put("online", Integer.valueOf(SessionManager.onlineCount()));
        return View.ok(data);
    }

    /** 待审核账户 */
    @Route(value = "/api/account/pending", roles = {"admin"})
    public View pending(RequestContext ctx) {
        return View.ok(accountService.listPendingAudit());
    }

    /** 账户实名审核 */
    @Route(value = "/api/account/audit", roles = {"admin"})
    public View audit(RequestContext ctx) {
        User user = accountService.auditUser(ctx.getUserId(), ctx.requireIntParam("userId"),
                ctx.intParam("auditStatus"), ctx.param("reason"));
        return View.json(com.campus.repair.common.Result.success("审核完成，已通知用户", user));
    }

    /** 锁定/解锁账户 */
    @Route(value = "/api/account/lock", roles = {"admin"})
    public View lock(RequestContext ctx) {
        boolean locked = ctx.boolParam("locked");
        User user = accountService.lockUser(ctx.getUserId(), ctx.requireIntParam("userId"), locked);
        return View.json(com.campus.repair.common.Result.success(locked ? "账户已锁定" : "账户已解锁", user));
    }

    /** 基础数据查询（楼栋、报修类别、维修工种） */
    @Route(value = "/api/account/baseData")
    public View baseData(RequestContext ctx) {
        String type = ctx.param("type", "building");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("type", type);
        data.put("values", accountService.baseData(type));
        data.put("buildings", accountService.baseData("building"));
        data.put("categories", accountService.baseData("category"));
        data.put("skills", accountService.baseData("skill"));
        return View.ok(data);
    }

    /** 基础数据维护 */
    @Route(value = "/api/account/saveBaseData", roles = {"admin"})
    public View saveBaseData(RequestContext ctx) {
        accountService.saveBaseData(ctx.getUserId(), ctx.param("type"), ctx.param("value"),
                ctx.param("sortNo"), ctx.boolParam("delete"));
        return View.json(com.campus.repair.common.Result.success("基础数据已更新", null));
    }

    /** 系统运行信息（公开，供前端展示部署形态） */
    @Route(value = "/api/system/info", publicRoute = true)
    public View systemInfo(RequestContext ctx) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("systemName", AppConfig.get("system.name", "校园报修系统"));
        data.put("version", AppConfig.get("system.version", "1.0.0"));
        data.put("storageMode", DaoFactory.mode());
        data.put("online", Integer.valueOf(SessionManager.onlineCount()));
        data.put("serverTime", com.campus.repair.util.DateUtil.format(new java.util.Date()));
        data.put("dbUrl", DaoFactory.isMemoryMode() ? "内存库（演示模式）" : AppConfig.get("db.url", ""));
        return View.ok(data);
    }
}
