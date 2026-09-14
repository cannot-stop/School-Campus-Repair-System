package com.campus.repair.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.jdbc.JdbcTemplate;
import com.campus.repair.dao.mybatis.MyBatisSessionFactory;
import com.campus.repair.dal.MyBatisCapabilityProbe;
import com.campus.repair.domain.Evaluation;
import com.campus.repair.domain.Material;
import com.campus.repair.domain.MaterialUsage;
import com.campus.repair.domain.OrderQuery;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.Worker;
import com.campus.repair.service.DispatchService;
import com.campus.repair.service.EvaluationService;
import com.campus.repair.service.MaterialService;
import com.campus.repair.service.RepairService;
import com.campus.repair.service.ReportService;
import com.campus.repair.service.StatService;

/**
 * MyBatis Mapper 定点验证：在 {@code storage.mode=mybatis} 下走完整业务链路，
 * 逐项核对 DAO 是否由 MyBatis Mapper（resources/mapper/*.xml）驱动。
 *
 * <p>验证内容：</p>
 * <ol>
 *   <li>每个 DAO 实现类都是 MyBatis 版本（类名以 MyBatis 开头）；</li>
 *   <li>提交报修 → 审核 → 派单 → 接单 → 开始维修 → 登记耗材 → 完成 → 确认；
 *       每一步都通过 MyBatis Mapper 读写 MySQL；</li>
 *   <li>分页查询、列表查询、统计查询、评价与平均分等复杂语句可用；</li>
 *   <li>关键字段（时间字段、关联字段、耗材单价）能正确回读；</li>
 *   <li>数据库事务一致性：耗材库存与使用记录在同一事务内提交。</li>
 * </ol>
 */
public class MyBatisCheck {

    private static int passed = 0;
    private static int failed = 0;
    private static java.io.PrintStream out;

    public static void main(String[] args) throws Exception {
        System.setProperty("storage.mode", "mybatis");
        System.setProperty("db.password", "123456");
        out = new java.io.PrintStream(
                new java.io.FileOutputStream("build/mybatis-check.log"), true, "UTF-8");

        DaoFactory.init();
        out.println("==================== MyBatis Mapper 定点验证 ====================");
        out.println("存储模式     = " + DaoFactory.mode());
        out.println("MyBatis 配置 = " + (MyBatisSessionFactory.configAvailable() ? "已加载 mybatis-config.xml" : "未找到"));
        out.println("连接自检     = " + MyBatisSessionFactory.testConnection());

        // ---------------------------------------------------------- 1) 实现类核对
        out.println();
        out.println("---- 1. DAO 实现类（应为 MyBatis 版本）----");
        check("UserDaoImpl 使用 MyBatis", MyBatisCapabilityProbe.daoClass("userDao").startsWith("com.campus.repair.dao.mybatis.MyBatis"));
        check("WorkerDaoImpl 使用 MyBatis", MyBatisCapabilityProbe.daoClass("workerDao").contains("MyBatisWorkerDaoImpl"));
        check("RepairOrderDaoImpl 使用 MyBatis", MyBatisCapabilityProbe.daoClass("repairOrderDao").contains("MyBatisRepairOrderDaoImpl"));
        check("RepairTaskDaoImpl 使用 MyBatis", MyBatisCapabilityProbe.daoClass("repairTaskDao").contains("MyBatisRepairTaskDaoImpl"));
        check("MaterialDaoImpl 使用 MyBatis", MyBatisCapabilityProbe.daoClass("materialDao").contains("MyBatisMaterialDaoImpl"));
        check("MaterialUsageDaoImpl 使用 MyBatis", MyBatisCapabilityProbe.daoClass("materialUsageDao").contains("MyBatisMaterialUsageDaoImpl"));
        check("EvaluationDaoImpl 使用 MyBatis", MyBatisCapabilityProbe.daoClass("evaluationDao").contains("MyBatisEvaluationDaoImpl"));
        check("MessageDaoImpl 使用 MyBatis", MyBatisCapabilityProbe.daoClass("messageDao").contains("MyBatisMessageDaoImpl"));
        check("ProgressDaoImpl 使用 MyBatis", MyBatisCapabilityProbe.daoClass("progressDao").contains("MyBatisProgressDaoImpl"));
        check("BaseDataDaoImpl 使用 MyBatis", MyBatisCapabilityProbe.daoClass("baseDataDao").contains("MyBatisBaseDataDaoImpl"));
        out.println("  已注册 Mapper 接口 = " + MyBatisCapabilityProbe.mapperNames());

        // ---------------------------------------------------------- 2) 基础查询
        out.println();
        out.println("---- 2. 基础查询（Mapper 动态 SQL）----");
        int accountCount = DaoFactory.userDao().findAll().size();
        check("t_user 查询可用（账户 " + accountCount + " 个）", accountCount > 0);
        List<Worker> workers = DaoFactory.workerDao().findAvailable();
        check("t_worker 在线维修工查询可用（" + workers.size() + " 人）", workers.size() > 0);
        List<String> buildings = DaoFactory.baseDataDao().findValues("building");
        check("t_base_data 基础数据查询可用（楼栋 " + buildings.size() + " 个）", buildings.size() > 0);
        Worker waterWorker = DaoFactory.workerDao().findBySkill("水电").get(0);
        check("技能标签模糊查询可用（" + waterWorker.getName() + "）", waterWorker.getWorkerId() != null);

        // ---------------------------------------------------------- 3) 报修闭环
        out.println();
        out.println("---- 3. 报修业务闭环（全部经 MyBatis Mapper）----");
        ReportService reportService = new ReportService();
        Map<String, String> form = new HashMap<String, String>();
        form.put("building", "实验楼");
        form.put("floor", "3");
        form.put("room", "305");
        form.put("category", "水电");
        form.put("description", "MyBatis 验证：走廊灯不亮");
        form.put("priority", "1");
        RepairOrder order = reportService.submit(Integer.valueOf(1), form);
        check("提交报修（insert + 主键回填）orderId=" + order.getOrderId(),
                order.getOrderId() != null && order.getCategory().equals("水电"));

        // 关联查询：报修人信息由 LEFT JOIN 带出
        check("详情查询带出报修人（" + order.getReporterName() + "）",
                order.getReporterName() != null && order.getReporterPhone() != null);

        RepairOrder audited = reportService.audit(Integer.valueOf(6), order.getOrderId(), true, null, "1");
        check("审核受理（updateSelective 局部更新）状态=" + audited.getStatusText(),
                audited.getStatus().intValue() == 1 && "水电".equals(audited.getCategory()));
        check("审核后优先级已更新为紧急", audited.getPriority().intValue() == 1);

        OrderQuery query = new OrderQuery();
        query.setStatus(Integer.valueOf(1));
        query.setPageSize(5);
        com.campus.repair.common.PageResult<RepairOrder> page = reportService.query(Integer.valueOf(6), new HashMap<String, String>() {
            {
                put("status", "1");
                put("pageSize", "5");
            }
        });
        check("按条件分页查询（total=" + page.getTotal() + "，本页 " + page.getRows().size() + " 条）",
                page.getTotal() > 0 && page.getRows().size() <= 5);

        DispatchService dispatchService = new DispatchService();
        // 在单量断言按"增量"比较：库里可能已有历史遗留的未完成任务，
        // 用绝对值（==1 / ==0）只在刚执行完 db/seed.sql 的干净库上成立，无法重复运行。
        int loadBefore = DaoFactory.workerDao().findById(waterWorker.getWorkerId()).currentOrderCount();
        RepairTask task = dispatchService.manualDispatch(Integer.valueOf(6), order.getOrderId(),
                waterWorker.getWorkerId(), "MyBatis 验证派单");
        check("人工派单（insert 任务 + 更新报修单状态）任务号=" + task.getTaskId(),
                task.getTaskId() != null && task.getStatus().intValue() == 0);
        check("派单后在单量 +1（" + loadBefore + " → " + (loadBefore + 1) + "）",
                DaoFactory.workerDao().findById(waterWorker.getWorkerId()).currentOrderCount() == loadBefore + 1);

        dispatchService.accept(waterWorker.getUserId(), task.getTaskId());
        RepairTask accepted = DaoFactory.repairTaskDao().findById(task.getTaskId());
        check("接单（accept_time 写入并回读：" + accepted.getAcceptTime() + "）",
                accepted.getAcceptTime() != null && accepted.getStatus().intValue() == 1);
        check("任务查询带出报修地点（" + accepted.getLocation() + "）", accepted.getLocation() != null);

        RepairService repairService = new RepairService();
        repairService.start(waterWorker.getUserId(), task.getTaskId(), "MyBatis 验证：已到现场");
        RepairTask started = DaoFactory.repairTaskDao().findById(task.getTaskId());
        check("开始维修（start_time 写入并回读：" + started.getStartTime() + "）", started.getStartTime() != null);

        Material material = DaoFactory.materialDao().findAll().get(0);
        int stockBefore = material.getStock().intValue();
        repairService.registerMaterial(waterWorker.getUserId(), task.getTaskId(),
                java.util.Collections.singletonList(new MaterialService.MaterialItem(material.getMatId(), 2)), false);
        Material afterUse = DaoFactory.materialDao().findById(material.getMatId());
        check("耗材登记（条件更新扣减库存 " + stockBefore + " → " + afterUse.getStock() + "）",
                afterUse.getStock().intValue() == stockBefore - 2);
        List<MaterialUsage> usages = DaoFactory.materialUsageDao().findByTask(task.getTaskId());
        check("耗材使用记录带出名称与单价（" + (usages.isEmpty() ? "-" : usages.get(0).getMatName()) + "）",
                !usages.isEmpty() && usages.get(0).getUnitPrice() != null && usages.get(0).getAmount().doubleValue() > 0);

        repairService.finish(waterWorker.getUserId(), task.getTaskId(), "更换灯管并复测合格。", null, null, false);
        RepairTask finished = DaoFactory.repairTaskDao().findById(task.getTaskId());
        check("完成登记（finish_time 由 SQL NOW() 写入：" + finished.getFinishTime() + "）",
                finished.getFinishTime() != null && finished.getStatus().intValue() == 2);

        RepairOrder confirmed = repairService.confirm(Integer.valueOf(1), order.getOrderId(), true, null);
        check("结果确认（报修单归档）状态=" + confirmed.getStatusText(), confirmed.getStatus().intValue() == 5);
        check("确认后回到派单前的在单量（" + (loadBefore + 1) + " → " + loadBefore + "）",
                DaoFactory.workerDao().findById(waterWorker.getWorkerId()).currentOrderCount() == loadBefore);

        // ---------------------------------------------------------- 4) 评价与统计
        out.println();
        out.println("---- 4. 评价与统计分析（Mapper 聚合查询）----");
        EvaluationService evaluationService = new EvaluationService();
        Evaluation evaluation = evaluationService.submit(Integer.valueOf(1), order.getOrderId(), "5", "MyBatis 验证：服务很好");
        check("提交评价（insert + 唯一约束）evalId=" + evaluation.getEvalId(), evaluation.getEvalId() != null);
        evaluationService.audit(Integer.valueOf(6), evaluation.getEvalId(), Integer.valueOf(1), null);
        evaluationService.reply(Integer.valueOf(6), evaluation.getEvalId(), "感谢评价");
        check("评价审核与回复（update）", "感谢评价".equals(
                DaoFactory.evaluationDao().findById(evaluation.getEvalId()).getReply()));
        double avg = DaoFactory.evaluationDao().avgScore();
        check("平均评分聚合查询（avg=" + Math.round(avg * 100) / 100d + "）", avg > 0);

        StatService statService = new StatService();
        Map<String, Object> overview = statService.overview();
        check("统计总览（报修总量 " + overview.get("total") + "）", ((Long) overview.get("total")).longValue() > 0);
        check("故障类型分析（" + statService.categoryAnalysis().size() + " 类）", !statService.categoryAnalysis().isEmpty());
        check("维修人员绩效（" + statService.workerPerformance().size() + " 人）",
                !statService.workerPerformance().isEmpty());
        check("耗材成本统计（" + statService.materialCost() + " 元）", statService.materialCost().doubleValue() > 0);
        check("月度趋势", !statService.monthlyTrend().isEmpty());

        // ---------------------------------------------------------- 5) 事务一致性
        out.println();
        out.println("---- 5. 事务一致性（MyBatis SqlSession 事务）----");
        int stockNow = DaoFactory.materialDao().findById(material.getMatId()).getStock().intValue();
        boolean rejected = false;
        try {
            repairService.registerMaterial(waterWorker.getUserId(), task.getTaskId(),
                    java.util.Collections.singletonList(new MaterialService.MaterialItem(material.getMatId(), stockNow + 500)), false);
        } catch (RuntimeException e) {
            rejected = true;
        }
        int stockAfterFail = DaoFactory.materialDao().findById(material.getMatId()).getStock().intValue();
        check("超库存登记被拒绝且库存未变（" + stockNow + " → " + stockAfterFail + "）",
                rejected && stockAfterFail == stockNow);

        Object updateTime = JdbcTemplate.queryScalar("SELECT update_time FROM t_repair_order WHERE order_id = ?",
                order.getOrderId());
        check("update_time 由 SQL 维护（" + updateTime + "）", updateTime != null);

        out.println();
        out.println("==================== 验证结果 ====================");
        out.println("通过：" + passed + " 项，失败：" + failed + " 项");
        if (failed > 0) {
            out.println("存在失败项，请检查上方 [FAIL] 标记");
        } else {
            out.println("MyBatis Mapper 层全部验证通过 -> PASS");
        }
        out.close();
        System.exit(failed > 0 ? 1 : 0);
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            out.println("  [OK] " + name);
        } else {
            failed++;
            out.println("  [FAIL] " + name);
        }
    }
}
