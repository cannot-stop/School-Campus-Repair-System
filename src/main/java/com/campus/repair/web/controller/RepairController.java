package com.campus.repair.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.ResultCode;
import com.campus.repair.domain.MaterialUsage;
import com.campus.repair.domain.Progress;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.Role;
import com.campus.repair.service.MaterialService;
import com.campus.repair.web.ParamMap;
import com.campus.repair.web.RequestContext;
import com.campus.repair.web.Route;
import com.campus.repair.web.View;

/**
 * 维修管理控制器（对应设计书 2.2.6 RepairController 与 2.2.5 维修管理模块设计）。
 *
 * <p>方法：start()、feedback()、material()、finish()、confirm()、detail()。</p>
 */
public class RepairController extends BaseController {

    /** 开始维修（维修人员"开始维修"） */
    @Route(value = "/api/repair/start", roles = {"worker"})
    public View start(RequestContext ctx) {
        RepairTask task = repairService.start(ctx.getUserId(), ctx.requireIntParam("taskId"), ctx.param("remark"));
        return success("已记录开始维修时间", task);
    }

    /** 进度反馈（含延期原因） */
    @Route(value = "/api/repair/feedback", roles = {"worker"})
    public View feedback(RequestContext ctx) {
        Progress progress = repairService.feedback(ctx.getUserId(), ctx.requireIntParam("taskId"),
                ctx.param("content"), ctx.param("delayReason"));
        return success("进度反馈已提交", progress);
    }

    /** 耗材登记 */
    @Route(value = "/api/repair/material", roles = {"worker"})
    public View material(RequestContext ctx) {
        List<MaterialUsage> usages = repairService.registerMaterial(ctx.getUserId(), ctx.requireIntParam("taskId"),
                parseItems(ctx), ctx.boolParam("allowOverStock"));
        return success("耗材登记成功，共 " + usages.size() + " 项", usages);
    }

    /** 维修完成登记 */
    @Route(value = "/api/repair/finish", roles = {"worker"})
    public View finish(RequestContext ctx) {
        RepairTask task = repairService.finish(ctx.getUserId(), ctx.requireIntParam("taskId"),
                ctx.param("result"), ctx.param("remark"), parseItems(ctx), ctx.boolParam("allowOverStock"));
        return success("维修完成登记成功，等待确认", task);
    }

    /** 维修任务详情（维修人员处理页面） */
    @Route(value = "/api/repair/detail", roles = {"worker"})
    public View detail(RequestContext ctx) {
        return View.ok(repairService.taskDetail(ctx.getUserId(), ctx.requireIntParam("taskId")));
    }

    /** 结果确认（报修人或维修管理员） */
    @Route(value = "/api/repair/confirm", roles = {"reporter", "manager", "admin"})
    public View confirm(RequestContext ctx) {
        boolean pass = ctx.param("pass") == null || ctx.boolParam("pass");
        RepairOrder order = repairService.confirm(ctx.getUserId(), ctx.requireIntParam("orderId"),
                pass, ctx.param("reason"));
        return success(pass ? "维修结果已确认，报修单归档" : "已退回维修人员重新处理", order);
    }

    /** 维修工工作台统计 */
    @Route(value = "/api/repair/statistics", roles = {"worker"})
    public View statistics(RequestContext ctx) {
        return View.ok(repairService.workerStatistics(ctx.getUserId()));
    }

    /** 维修人员更新在线状态与位置 */
    @Route(value = "/api/repair/status", roles = {"worker"})
    public View updateStatus(RequestContext ctx) {
        Integer status = ctx.intParam("status");
        Map<String, Object> data = new HashMap<String, Object>();
        com.campus.repair.domain.Worker worker = com.campus.repair.dao.DaoFactory.workerDao()
                .findByUserId(ctx.getUserId());
        if (worker == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到维修人员档案");
        }
        if (status != null) {
            com.campus.repair.dao.DaoFactory.workerDao().updateStatus(worker.getWorkerId(), status);
        }
        if (ctx.param("location") != null) {
            com.campus.repair.dao.DaoFactory.workerDao().updateLocation(worker.getWorkerId(), ctx.param("location"));
        }
        data.put("worker", com.campus.repair.dao.DaoFactory.workerDao().findById(worker.getWorkerId()));
        return success("状态已更新", data);
    }

    /** 解析耗材明细：支持 items=[{matId,useCount}] 或 matId/useCount 重复参数 */
    private List<MaterialService.MaterialItem> parseItems(RequestContext ctx) {
        List<MaterialService.MaterialItem> items = new ArrayList<MaterialService.MaterialItem>();
        String json = ctx.param("items");
        if (json != null && !json.trim().isEmpty()) {
            for (Map<String, String> row : ParamMap.parseJsonArray(json)) {
                Integer matId = toInt(row.get("matId"));
                Integer useCount = toInt(row.get("useCount"));
                if (matId == null) {
                    continue;
                }
                items.add(new MaterialService.MaterialItem(matId, useCount == null ? 1 : useCount.intValue()));
            }
            return items;
        }
        // 兼容表单形式：matId=1&useCount=2
        Integer matId = ctx.intParam("matId");
        if (matId != null) {
            Integer useCount = ctx.intParam("useCount");
            items.add(new MaterialService.MaterialItem(matId, useCount == null ? 1 : useCount.intValue()));
        }
        return items;
    }

    private Integer toInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
