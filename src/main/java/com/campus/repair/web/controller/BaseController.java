package com.campus.repair.web.controller;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.ResultCode;
import com.campus.repair.domain.Role;
import com.campus.repair.domain.User;
import com.campus.repair.service.AccountService;
import com.campus.repair.service.DispatchService;
import com.campus.repair.service.EvaluationService;
import com.campus.repair.service.MaterialService;
import com.campus.repair.service.MessageService;
import com.campus.repair.service.RepairService;
import com.campus.repair.service.ReportService;
import com.campus.repair.service.StatService;
import com.campus.repair.web.RequestContext;
import com.campus.repair.web.View;

/**
 * 控制器基类：集中持有各业务服务实例，并提供角色校验辅助方法。
 *
 * <p>对应设计书 2.2.6"系统整体采用 MVC 分层类结构"：控制层只负责请求接收、参数校验与视图跳转，
 * 业务规则一律由 Service 层实现。</p>
 */
public abstract class BaseController {

    protected static final ReportService reportService = new ReportService();
    protected static final DispatchService dispatchService = new DispatchService();
    protected static final RepairService repairService = new RepairService();
    protected static final MaterialService materialService = new MaterialService();
    protected static final EvaluationService evaluationService = new EvaluationService();
    protected static final StatService statService = new StatService();
    protected static final MessageService messageService = new MessageService();
    protected static final AccountService accountService = new AccountService();

    protected View ok(Object data) {
        return View.ok(data);
    }

    protected View success(String message, Object data) {
        return View.json(com.campus.repair.common.Result.success(message, data));
    }

    /** 校验当前登录用户属于指定角色 */
    protected void requireRole(RequestContext ctx, String... roles) {
        if (ctx.getSession() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        if (!ctx.getSession().hasRole(roles)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前角色（" + ctx.getSession().getRoleText() + "）无权执行该操作");
        }
    }

    protected boolean isAdmin(RequestContext ctx) {
        return ctx.getSession() != null && Role.ADMIN.equals(Role.of(ctx.getRole()));
    }

    /** 当前登录用户实体 */
    protected User currentUser(RequestContext ctx) {
        User user = com.campus.repair.dao.DaoFactory.userDao().findById(ctx.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态失效，请重新登录");
        }
        return user;
    }
}
