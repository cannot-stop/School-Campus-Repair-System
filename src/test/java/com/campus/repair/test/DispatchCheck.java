package com.campus.repair.test;

import com.campus.repair.boot.DemoDataLoader;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.domain.DispatchCandidate;
import com.campus.repair.domain.OrderStatus;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.Worker;
import com.campus.repair.service.DispatchService;
import com.campus.repair.service.ReportService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能派单匹配度与演示数据的定点验证程序。
 *
 * <p>用于确认：演示维修人员的技能标签与报修类别能正确匹配、匹配度评分符合模型定义。</p>
 */
public class DispatchCheck {

    /** 输出流：UTF-8 写入 build/dispatch-check.log */
    private static java.io.PrintStream out;

    public static void main(String[] args) throws Exception {
        System.setProperty("storage.mode", "memory");
        out = new java.io.PrintStream(new java.io.FileOutputStream("build/dispatch-check.log"), true, "UTF-8");
        DaoFactory.init();
        DemoDataLoader.load();

        out.println("=== 维修人员技能标签 ===");
        for (Worker worker : DaoFactory.workerDao().findAll()) {
            out.println("workerId=" + worker.getWorkerId() + " name=" + worker.getName()
                    + " skills=[" + worker.getSkillTags() + "] online=" + worker.isOnline()
                    + " load=" + worker.currentOrderCount());
        }

        out.println();
        out.println("=== 演示报修单类别 ===");
        com.campus.repair.domain.OrderQuery allQuery = new com.campus.repair.domain.OrderQuery();
        allQuery.setPageSize(100);
        for (RepairOrder order : DaoFactory.repairOrderDao().findByCondition(allQuery)) {
            out.println("orderId=" + order.getOrderId() + " category=[" + order.getCategory()
                    + "] status=" + OrderStatus.textOf(order.getStatus()));
        }

        // 用「待派单」的报修单（演示数据中的 5 号：图书馆 网络）做匹配度验证
        RepairOrder target = DaoFactory.repairOrderDao().findById(Integer.valueOf(5));
        out.println();
        out.println("=== 匹配度计算（报修单 5，类别：" + target.getCategory() + "）===");
        DispatchService dispatchService = new DispatchService();
        List<DispatchCandidate> candidates = dispatchService.rankCandidates(target.getOrderId());
        for (DispatchCandidate candidate : candidates) {
            out.println("  " + candidate.getWorker().getName()
                    + " skills=[" + candidate.getWorker().getSkillTags() + "]"
                    + " total=" + candidate.getTotalScore()
                    + " skill=" + candidate.getSkillScore()
                    + " load=" + candidate.getLoadScore()
                    + " online=" + candidate.getOnlineScore()
                    + " urgent=" + candidate.getUrgentScore()
                    + " level=" + candidate.getLevelText());
        }

        // 再用新提交的「水电」报修单验证一次（与 E2E 用例一致）
        ReportService reportService = new ReportService();
        Map<String, String> form = new HashMap<String, String>();
        form.put("building", "实验楼");
        form.put("floor", "2");
        form.put("room", "215");
        form.put("category", "水电");
        form.put("description", "定点验证：走廊灯不亮");
        form.put("priority", "1");
        RepairOrder created = reportService.submit(Integer.valueOf(1), form);
        reportService.audit(Integer.valueOf(6), created.getOrderId(), true, null, "1");
        out.println();
        out.println("=== 新报修单（#" + created.getOrderId() + "，类别：水电，紧急）匹配度 ===");
        out.println("  [DEBUG] created.category=[" + created.getCategory() + "] len=" + (created.getCategory() == null ? -1 : created.getCategory().length()));
        com.campus.repair.domain.RepairOrder raw = com.campus.repair.dao.memory.MemoryStore.ORDERS.get(created.getOrderId());
        out.println("  [DEBUG] store.category=[" + (raw == null ? "ORDER-NULL" : raw.getCategory()) + "]");
        out.println("  [DEBUG] store keys=" + com.campus.repair.dao.memory.MemoryStore.ORDERS.keySet());
        out.println("  [DEBUG] order8 status=" + (raw == null ? "-" : raw.getStatus()) + " statusText=" + (raw == null ? "-" : raw.getStatusText()));
        RepairOrder reloaded = DaoFactory.repairOrderDao().findById(created.getOrderId());
        out.println("  [DEBUG] reloaded.category=[" + reloaded.getCategory() + "] equals=" + "水电".equals(reloaded.getCategory()));
        Worker w1 = DaoFactory.workerDao().findById(Integer.valueOf(1));
        out.println("  [DEBUG] worker1.skills=[" + w1.getSkillTags() + "] contains(category)=" + w1.getSkillTags().contains(reloaded.getCategory()));
        out.println("  [DEBUG] score(direct)=" + DispatchCandidate.score(w1, reloaded.getCategory(), Integer.valueOf(1)).getSkillScore()
                + " score(literal)= " + DispatchCandidate.score(w1, "水电", Integer.valueOf(1)).getSkillScore());
        List<DispatchCandidate> list = dispatchService.rankCandidates(created.getOrderId());
        Worker top = list.get(0).getWorker();
        out.println("  首位：" + top.getName() + " skills=[" + top.getSkillTags() + "] total="
                + list.get(0).getTotalScore() + " skill=" + list.get(0).getSkillScore());
        Map<String, Object> auto = dispatchService.smartDispatch(Integer.valueOf(6), created.getOrderId());
        out.println("  智能派单结果：任务 #" + ((com.campus.repair.domain.RepairTask) auto.get("task")).getTaskId()
                + " 派给 " + ((com.campus.repair.domain.RepairTask) auto.get("task")).getWorkerName());

        boolean ok = list.get(0).getSkillScore() > 0;
        out.println();
        out.println(ok ? "定点验证通过：技能匹配得分 > 0 -> PASS" : "定点验证失败：技能匹配得分 = 0 -> FAIL");
        System.exit(ok ? 0 : 1);
    }
}
