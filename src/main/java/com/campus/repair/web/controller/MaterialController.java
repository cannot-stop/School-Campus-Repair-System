package com.campus.repair.web.controller;

import java.util.HashMap;
import java.util.Map;

import com.campus.repair.domain.Material;
import com.campus.repair.domain.Role;
import com.campus.repair.web.RequestContext;
import com.campus.repair.web.Route;
import com.campus.repair.web.View;

/**
 * 耗材管理控制器（对应设计书 2.2.6 MaterialDao 与表 1.10 耗材登记功能点）。
 */
public class MaterialController extends BaseController {

    /** 耗材清单（含库存预警） */
    @Route("/api/material/list")
    public View list(RequestContext ctx) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("materials", materialService.listAll());
        data.put("lowStock", materialService.listLowStock());
        return View.ok(data);
    }

    /** 新增/修改耗材 */
    @Route(value = "/api/material/save", roles = {"manager", "admin"})
    public View save(RequestContext ctx) {
        Material material = materialService.save(ctx.param("matId"), ctx.param("matName"), ctx.param("spec"),
                ctx.param("stock"), ctx.param("unitPrice"));
        return success("耗材信息已保存", material);
    }

    /** 删除耗材 */
    @Route(value = "/api/material/delete", roles = {"manager", "admin"})
    public View delete(RequestContext ctx) {
        materialService.delete(ctx.param("matId"));
        return success("耗材已删除", null);
    }

    /** 补库 */
    @Route(value = "/api/material/addStock", roles = {"manager", "admin"})
    public View addStock(RequestContext ctx) {
        Integer count = ctx.intParam("count");
        Material material = materialService.replenish(ctx.requireIntParam("matId"), count == null ? 0 : count.intValue());
        return success("补库成功，当前库存 " + material.getStock(), material);
    }
}
