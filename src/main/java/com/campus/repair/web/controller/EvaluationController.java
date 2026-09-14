package com.campus.repair.web.controller;

import java.util.HashMap;
import java.util.Map;

import com.campus.repair.common.PageResult;
import com.campus.repair.domain.Evaluation;
import com.campus.repair.domain.Role;
import com.campus.repair.web.RequestContext;
import com.campus.repair.web.Route;
import com.campus.repair.web.View;

/**
 * 评价服务控制器（对应设计书 2.2.1"报修评价、评价审核回复"与 StatController.auditEval()）。
 */
public class EvaluationController extends BaseController {

    /** 提交评价（报修人，评分 1～5 分） */
    @Route(value = "/api/eval/submit", roles = {"reporter"})
    public View submit(RequestContext ctx) {
        Evaluation evaluation = evaluationService.submit(ctx.getUserId(), ctx.requireIntParam("orderId"),
                ctx.param("score"), ctx.param("comment"));
        return success("评价提交成功，感谢您的反馈", evaluation);
    }

    /** 评价列表 */
    @Route("/api/eval/list")
    public View list(RequestContext ctx) {
        PageResult<Evaluation> page = evaluationService.query(ctx.getUserId(), ctx.getParams().asMap());
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("rows", page.getRows());
        data.put("total", Long.valueOf(page.getTotal()));
        data.put("pageNum", Integer.valueOf(page.getPageNum()));
        data.put("pageSize", Integer.valueOf(page.getPageSize()));
        data.put("pages", Integer.valueOf(page.getPages()));
        return View.ok(data);
    }

    /** 评价审核回复（维修管理员） */
    @Route(value = "/api/eval/reply", roles = {"manager", "admin"})
    public View reply(RequestContext ctx) {
        Evaluation evaluation = evaluationService.reply(ctx.getUserId(), ctx.requireIntParam("evalId"),
                ctx.param("reply"));
        return success("回复成功", evaluation);
    }

    /** 评价审核（通过/驳回） */
    @Route(value = "/api/eval/audit", roles = {"manager", "admin"})
    public View audit(RequestContext ctx) {
        Evaluation evaluation = evaluationService.audit(ctx.getUserId(), ctx.requireIntParam("evalId"),
                ctx.intParam("auditStatus"), ctx.param("reason"));
        return success("评价审核完成", evaluation);
    }

    /** 待审核评价 */
    @Route(value = "/api/eval/pending", roles = {"manager", "admin"})
    public View pending(RequestContext ctx) {
        return View.ok(evaluationService.pendingAudit());
    }

    /** 评价统计（平均分与分布） */
    @Route("/api/eval/statistics")
    public View statistics(RequestContext ctx) {
        return View.ok(evaluationService.statistics());
    }
}
