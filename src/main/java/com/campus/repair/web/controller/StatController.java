package com.campus.repair.web.controller;

import java.util.HashMap;
import java.util.Map;

import com.campus.repair.domain.Evaluation;
import com.campus.repair.domain.Role;
import com.campus.repair.web.RequestContext;
import com.campus.repair.web.Route;
import com.campus.repair.web.View;

/**
 * 统计分析控制器（对应设计书 2.2.6 StatController 与 2.2.1"维修统计、故障类型分析、绩效统计"）。
 *
 * <p>方法：report()（总览）、category()、building()、worker()、monthly()、material()、auditEval()。</p>
 */
public class StatController extends BaseController {

    /** 统计总览 */
    @Route(value = "/api/stat/overview", roles = {"manager", "admin"})
    public View overview(RequestContext ctx) {
        return View.ok(statService.overview());
    }

    /** 故障类型分析 */
    @Route(value = "/api/stat/category", roles = {"manager", "admin"})
    public View category(RequestContext ctx) {
        return View.ok(statService.categoryAnalysis());
    }

    /** 楼栋分布分析 */
    @Route(value = "/api/stat/building", roles = {"manager", "admin"})
    public View building(RequestContext ctx) {
        return View.ok(statService.buildingAnalysis());
    }

    /** 维修人员绩效统计 */
    @Route(value = "/api/stat/worker", roles = {"manager", "admin"})
    public View worker(RequestContext ctx) {
        return View.ok(statService.workerPerformance());
    }

    /** 月度趋势 */
    @Route(value = "/api/stat/monthly", roles = {"manager", "admin"})
    public View monthly(RequestContext ctx) {
        return View.ok(statService.monthlyTrend());
    }

    /** 耗材消耗排行与成本 */
    @Route(value = "/api/stat/material", roles = {"manager", "admin"})
    public View material(RequestContext ctx) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("ranking", statService.materialRanking());
        data.put("totalCost", statService.materialCost());
        return View.ok(data);
    }

    /** 统计报表汇总（一次性返回全部指标，供统计页面使用） */
    @Route(value = "/api/stat/report", roles = {"manager", "admin"})
    public View report(RequestContext ctx) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("overview", statService.overview());
        data.put("category", statService.categoryAnalysis());
        data.put("building", statService.buildingAnalysis());
        data.put("worker", statService.workerPerformance());
        data.put("monthly", statService.monthlyTrend());
        data.put("material", statService.materialRanking());
        data.put("materialCost", statService.materialCost());
        data.put("evaluations", statService.evaluationList());
        return View.ok(data);
    }

    /** 评价审核（对应设计书 StatController.auditEval()） */
    @Route(value = "/api/stat/auditEval", roles = {"manager", "admin"})
    public View auditEval(RequestContext ctx) {
        Evaluation evaluation = evaluationService.audit(ctx.getUserId(), ctx.requireIntParam("evalId"),
                ctx.intParam("auditStatus"), ctx.param("reason"));
        return success("评价审核完成", evaluation);
    }
}
