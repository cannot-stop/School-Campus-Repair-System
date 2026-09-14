package com.campus.repair.web.controller;

import java.util.HashMap;
import java.util.Map;

import com.campus.repair.common.PageResult;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.Role;
import com.campus.repair.web.RequestContext;
import com.campus.repair.web.Route;
import com.campus.repair.web.View;

/**
 * 报修管理控制器（对应设计书 2.2.6 ReportController 与 2.2.3 报修管理模块设计）。
 *
 * <p>方法：submit()、list()、detail()、cancel()、urge()、audit()、statistics()。</p>
 */
public class ReportController extends BaseController {

    /** 提交报修（报修人） */
    @Route(value = "/api/report/submit", roles = {"reporter"})
    public View submit(RequestContext ctx) {
        RepairOrder order = reportService.submit(ctx.getUserId(), ctx.getParams().asMap());
        return success("报修提交成功，报修单号 " + order.getOrderId() + "，等待维修管理员审核", order);
    }

    /** 报修查询（按时间、状态、类别等条件查询本人报修单，列表分页展示） */
    @Route("/api/report/list")
    public View list(RequestContext ctx) {
        PageResult<RepairOrder> page = reportService.query(ctx.getUserId(), ctx.getParams().asMap());
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("rows", page.getRows());
        data.put("total", Long.valueOf(page.getTotal()));
        data.put("pageNum", Integer.valueOf(page.getPageNum()));
        data.put("pageSize", Integer.valueOf(page.getPageSize()));
        data.put("pages", Integer.valueOf(page.getPages()));
        return View.ok(data);
    }

    /** 待审核报修单（维修管理员） */
    @Route(value = "/api/report/pendingAudit", roles = {"manager", "admin"})
    public View pendingAudit(RequestContext ctx) {
        return View.ok(reportService.queryPendingAudit(ctx.getParams().asMap(), false));
    }

    /** 报修详情 */
    @Route("/api/report/detail")
    public View detail(RequestContext ctx) {
        RepairOrder order = reportService.detail(ctx.getUserId(), ctx.requireIntParam("orderId"));
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("order", order);
        data.put("tasks", reportService.taskHistory(ctx.getUserId(), order.getOrderId()));
        data.put("usages", reportService.usageOfOrder(order.getOrderId()));
        return View.ok(data);
    }

    /** 撤销报修 */
    @Route(value = "/api/report/cancel", roles = {"reporter"})
    public View cancel(RequestContext ctx) {
        RepairOrder order = reportService.cancel(ctx.getUserId(), ctx.requireIntParam("orderId"), ctx.param("reason"));
        return success("报修单已撤销", order);
    }

    /** 报修催办 */
    @Route(value = "/api/report/urge", roles = {"reporter"})
    public View urge(RequestContext ctx) {
        RepairOrder order = reportService.urge(ctx.getUserId(), ctx.requireIntParam("orderId"), ctx.param("reason"));
        return success("已向维修管理员催办，请耐心等待处理", order);
    }

    /** 报修审核（受理/驳回） */
    @Route(value = "/api/report/audit", roles = {"manager", "admin"})
    public View audit(RequestContext ctx) {
        boolean accept = ctx.param("accept") == null || ctx.boolParam("accept");
        RepairOrder order = reportService.audit(ctx.getUserId(), ctx.requireIntParam("orderId"),
                accept, ctx.param("reason"), ctx.param("priority"));
        return success(accept ? "报修单已受理，等待派单" : "报修单已驳回并通知报修人", order);
    }

    /** 报修统计概览 */
    @Route("/api/report/statistics")
    public View statistics(RequestContext ctx) {
        return View.ok(reportService.statistics(ctx.getUserId(), ctx.getRole()));
    }

    /** 提交报修表单可选项（楼栋、类别、耗材） */
    @Route("/api/report/options")
    public View options(RequestContext ctx) {
        return View.ok(reportService.formOptions());
    }

    /** 超时监督：超时未接单任务与超时未确认报修单 */
    @Route(value = "/api/report/overdue", roles = {"manager", "admin"})
    public View overdue(RequestContext ctx) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("overdueTasks", reportService.overdueTasks());
        data.put("overdueConfirms", reportService.overdueConfirms());
        return View.ok(data);
    }
}
