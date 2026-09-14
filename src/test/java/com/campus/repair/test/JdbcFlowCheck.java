package com.campus.repair.test;

import java.util.HashMap;
import java.util.Map;

import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.jdbc.Database;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.Worker;
import com.campus.repair.service.DispatchService;
import com.campus.repair.service.RepairService;
import com.campus.repair.service.ReportService;

/**
 * JDBC 模式的定点验证：提交 → 审核 → 派单 → 接单 → 开始维修，
 * 直接读取数据库核对 start_time / accept_time 是否写入。
 */
public class JdbcFlowCheck {

    public static void main(String[] args) throws Exception {
        System.setProperty("storage.mode", "jdbc");
        System.setProperty("db.password", "123456");
        java.io.PrintStream out = new java.io.PrintStream(
                new java.io.FileOutputStream("build/jdbc-flow.log"), true, "UTF-8");
        DaoFactory.init();
        out.println("存储模式 = " + DaoFactory.mode());
        out.println("数据库连接 = " + Database.testConnection());

        ReportService reportService = new ReportService();
        Map<String, String> form = new HashMap<String, String>();
        form.put("building", "实验楼");
        form.put("floor", "3");
        form.put("room", "301");
        form.put("category", "水电");
        form.put("description", "JDBC 定点验证：灯具损坏");
        form.put("priority", "0");
        RepairOrder order = reportService.submit(Integer.valueOf(1), form);
        out.println("提交报修 orderId=" + order.getOrderId() + " category=" + order.getCategory());
        reportService.audit(Integer.valueOf(6), order.getOrderId(), true, null, null);

        DispatchService dispatchService = new DispatchService();
        Worker worker = DaoFactory.workerDao().findBySkill("水电").get(0);
        RepairTask task = dispatchService.manualDispatch(Integer.valueOf(6), order.getOrderId(), worker.getWorkerId(), "定点验证");
        out.println("派单任务 taskId=" + task.getTaskId() + " worker=" + worker.getName());

        dispatchService.accept(worker.getUserId(), task.getTaskId());
        RepairTask accepted = DaoFactory.repairTaskDao().findById(task.getTaskId());
        out.println("接单后 acceptTime=" + accepted.getAcceptTime() + " status=" + accepted.getStatus());

        RepairService repairService = new RepairService();
        repairService.start(worker.getUserId(), task.getTaskId(), "已到现场开始处理");
        RepairTask started = DaoFactory.repairTaskDao().findById(task.getTaskId());
        out.println("开始维修后 startTime=" + started.getStartTime() + " acceptTime=" + started.getAcceptTime());

        Object dbStart = com.campus.repair.dao.jdbc.JdbcTemplate.queryScalar(
                "SELECT start_time FROM t_repair_task WHERE task_id = ?", task.getTaskId());
        Object dbAccept = com.campus.repair.dao.jdbc.JdbcTemplate.queryScalar(
                "SELECT accept_time FROM t_repair_task WHERE task_id = ?", task.getTaskId());
        out.println("数据库 start_time=" + dbStart + " accept_time=" + dbAccept);
        java.util.List<java.util.Map<String, Object>> rows = com.campus.repair.dao.jdbc.JdbcTemplate.queryRows(
                "SELECT t.task_id, t.accept_time, t.start_time, t.finish_time, t.status FROM t_repair_task t WHERE t.task_id = ?",
                task.getTaskId());
        out.println("直查行 = " + rows);
        if (!rows.isEmpty()) {
            Object v = rows.get(0).get("accept_time");
            out.println("accept_time 类型 = " + (v == null ? "null" : v.getClass().getName()));
            Object v2 = rows.get(0).get("start_time");
            out.println("start_time 类型 = " + (v2 == null ? "null" : v2.getClass().getName()));
        }
        java.util.List<RepairTask> mapped = com.campus.repair.dao.jdbc.JdbcTemplate.queryList(
                "SELECT t.task_id, t.accept_time, t.start_time, t.finish_time, t.status FROM t_repair_task t WHERE t.task_id = ?",
                RepairTask.class, task.getTaskId());
        out.println("映射对象 acceptTime=" + (mapped.isEmpty() ? "-" : mapped.get(0).getAcceptTime())
                + " startTime=" + (mapped.isEmpty() ? "-" : mapped.get(0).getStartTime())
                + " finishTime=" + (mapped.isEmpty() ? "-" : mapped.get(0).getFinishTime()));
        RepairTask full = DaoFactory.repairTaskDao().findById(task.getTaskId());
        out.println("完整查询 acceptTime=" + full.getAcceptTime() + " startTime=" + full.getStartTime()
                + " status=" + full.getStatus());

        boolean ok = dbStart != null;
        out.println(ok ? "JDBC 定点验证通过：开始维修时间已持久化 -> PASS"
                : "JDBC 定点验证失败：开始维修时间未持久化 -> FAIL");
        out.close();
        System.exit(ok ? 0 : 1);
    }
}
