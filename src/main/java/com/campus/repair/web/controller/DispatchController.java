package com.campus.repair.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.campus.repair.domain.DispatchCandidate;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.web.RequestContext;
import com.campus.repair.web.Route;
import com.campus.repair.web.View;

/**
 * 派单管理控制器（对应设计书 2.2.6 DispatchController 与 2.2.4 派单管理模块设计）。
 *
 * <p>方法：autoDispatch()（智能派单）、manualDispatch()、accept()、transfer()、refuse()、candidates()。</p>
 */
public class DispatchController extends BaseController {

    /** 待派单报修单列表 */
    @Route(value = "/api/dispatch/pending", roles = {"manager", "admin"})
    public View pending(RequestContext ctx) {
        List<RepairOrder> orders = dispatchService.pendingDispatchOrders();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("rows", orders);
        data.put("workers", accountService.listWorkers());
        return View.ok(data);
    }

    /** 候选维修人员匹配度排序（含推荐理由） */
    @Route(value = "/api/dispatch/candidates", roles = {"manager", "admin"})
    public View candidates(RequestContext ctx) {
        List<DispatchCandidate> candidates = dispatchService.rankCandidates(ctx.requireIntParam("orderId"));
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("candidates", candidates);
        return View.ok(data);
    }

    /** 智能派单 */
    @Route(value = "/api/dispatch/auto", roles = {"manager", "admin"})
    public View auto(RequestContext ctx) {
        Map<String, Object> data = dispatchService.smartDispatch(ctx.getUserId(), ctx.requireIntParam("orderId"));
        return success("智能派单成功，已通知维修人员接单", data);
    }

    /** 人工派单 */
    @Route(value = "/api/dispatch/manual", roles = {"manager", "admin"})
    public View manual(RequestContext ctx) {
        RepairTask task = dispatchService.manualDispatch(ctx.getUserId(), ctx.requireIntParam("orderId"),
                ctx.requireIntParam("workerId"), ctx.param("remark"));
        return success("派单成功，已创建维修任务（任务号 " + task.getTaskId() + "）", task);
    }

    /** 接单（维修人员） */
    @Route(value = "/api/dispatch/accept", roles = {"worker"})
    public View accept(RequestContext ctx) {
        RepairTask task = dispatchService.accept(ctx.getUserId(), ctx.requireIntParam("taskId"));
        return success("接单成功，请及时开始维修", task);
    }

    /** 转单申请 */
    @Route(value = "/api/dispatch/transfer", roles = {"worker"})
    public View transfer(RequestContext ctx) {
        RepairTask task = dispatchService.transfer(ctx.getUserId(), ctx.requireIntParam("taskId"),
                ctx.intParam("targetWorkerId"), ctx.param("reason"));
        return success("转单申请已提交，任务已退回派单池", task);
    }

    /** 退单申请 */
    @Route(value = "/api/dispatch/refuse", roles = {"worker"})
    public View refuse(RequestContext ctx) {
        RepairTask task = dispatchService.refuse(ctx.getUserId(), ctx.requireIntParam("taskId"), ctx.param("reason"));
        return success("退单申请已提交，任务已退回派单池", task);
    }

    /** 我的维修任务列表（维修人员） */
    @Route(value = "/api/dispatch/myTasks", roles = {"worker"})
    public View myTasks(RequestContext ctx) {
        return View.ok(dispatchService.workerTasks(ctx.getUserId(), ctx.intParam("status")));
    }

    /** 维修工负载与技能（派单页面辅助信息） */
    @Route(value = "/api/dispatch/workers", roles = {"manager", "admin"})
    public View workers(RequestContext ctx) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("workers", statService.workerLoad());
        return View.ok(data);
    }

    /** 在单量校正（管理员维护操作） */
    @Route(value = "/api/dispatch/recalculate", roles = {"manager", "admin"})
    public View recalculate(RequestContext ctx) {
        int changed = dispatchService.recalculateLoad();
        return success("已根据进行中任务重新计算在单量，更新 " + changed + " 名维修人员", Integer.valueOf(changed));
    }

    /** 手动触发超时提醒（默认由定时任务每分钟执行一次） */
    @Route(value = "/api/dispatch/remind", roles = {"manager", "admin"})
    public View remind(RequestContext ctx) {
        int accept = dispatchService.remindOverdueAccept();
        int confirm = dispatchService.remindOverdueConfirm();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("acceptReminded", Integer.valueOf(accept));
        data.put("confirmReminded", Integer.valueOf(confirm));
        return success("超时提醒已发送（接单超时 " + accept + " 条，确认超时 " + confirm + " 条）", data);
    }
}
