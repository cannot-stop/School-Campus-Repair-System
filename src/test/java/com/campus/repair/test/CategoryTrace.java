package com.campus.repair.test;

import java.util.HashMap;
import java.util.Map;

import com.campus.repair.boot.DemoDataLoader;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.domain.DispatchCandidate;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.Worker;
import com.campus.repair.service.DispatchService;
import com.campus.repair.service.ReportService;

/**
 * 报修单类别字段传递链路定点验证：
 * 提交 → 入库（内存库）→ 审核 → 重新读取 → 匹配度计算。
 */
public class CategoryTrace {

    public static void main(String[] args) throws Exception {
        System.setProperty("storage.mode", "memory");
        java.io.PrintStream out = new java.io.PrintStream(
                new java.io.FileOutputStream("build/category-trace2.log"), true, "UTF-8");
        DaoFactory.init();
        DemoDataLoader.load();

        ReportService reportService = new ReportService();
        Map<String, String> form = new HashMap<String, String>();
        form.put("building", "实验楼");
        form.put("floor", "2");
        form.put("room", "215");
        form.put("category", "水电");
        form.put("description", "定点验证：走廊灯不亮");
        form.put("priority", "1");

        out.println("提交前 form.category=[" + form.get("category") + "]");
        RepairOrder created = reportService.submit(Integer.valueOf(1), form);
        out.println("提交后 created.orderId=" + created.getOrderId() + " category=[" + created.getCategory() + "]");
        out.println("内存库中 category=[" + raw(created.getOrderId()) + "]");

        RepairOrder reloaded = DaoFactory.repairOrderDao().findById(created.getOrderId());
        out.println("findById category=[" + reloaded.getCategory() + "]");

        RepairOrder audited = reportService.audit(Integer.valueOf(6), created.getOrderId(), true, null, "1");
        out.println("审核后 audited.category=[" + audited.getCategory() + "] 内存库=[" + raw(created.getOrderId()) + "]");

        RepairOrder afterAudit = DaoFactory.repairOrderDao().findById(created.getOrderId());
        out.println("审核后 findById category=[" + afterAudit.getCategory() + "]");

        DispatchService dispatchService = new DispatchService();
        java.util.List<DispatchCandidate> candidates = dispatchService.rankCandidates(created.getOrderId());
        for (DispatchCandidate c : candidates) {
            out.println("候选 " + c.getWorker().getName() + " skills=[" + c.getWorker().getSkillTags()
                    + "] skill=" + c.getSkillScore() + " total=" + c.getTotalScore());
        }
        Worker best = candidates.get(0).getWorker();
        out.println("首位技能得分=" + candidates.get(0).getSkillScore()
                + " 是否匹配=" + best.getSkillTags().contains(afterAudit.getCategory()));
        out.close();
    }

    private static String raw(Integer orderId) {
        RepairOrder order = com.campus.repair.dao.memory.MemoryStore.ORDERS.get(orderId);
        return order == null ? "未找到" : order.getCategory();
    }
}
