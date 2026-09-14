package com.campus.repair.test;

import java.util.ArrayList;
import java.util.List;

import com.campus.repair.common.BusinessException;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.memory.MemoryStore;
import com.campus.repair.domain.DispatchCandidate;
import com.campus.repair.domain.Material;
import com.campus.repair.domain.OrderStatus;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.TaskStatus;
import com.campus.repair.domain.User;
import com.campus.repair.domain.Worker;
import com.campus.repair.service.AccountService;
import com.campus.repair.service.DispatchService;
import com.campus.repair.service.EvaluationService;
import com.campus.repair.service.MaterialService;
import com.campus.repair.service.MessageService;
import com.campus.repair.service.RepairService;
import com.campus.repair.service.ReportService;
import com.campus.repair.service.StatService;

/**
 * 业务自测程序：在内存库模式下按设计书的需求规定走通完整业务流程，验证状态机、
 * 权限控制、业务规则与统计指标。
 *
 * <p>运行方式：pwsh -File scripts\build.ps1 -WithSelfTest</p>
 */
public class SelfTestRunner {

    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> FAILURES = new ArrayList<String>();

    private static final AccountService ACCOUNT = new AccountService();
    private static final ReportService REPORT = new ReportService();
    private static final DispatchService DISPATCH = new DispatchService();
    private static final RepairService REPAIR = new RepairService();
    private static final MaterialService MATERIAL = new MaterialService();
    private static final EvaluationService EVAL = new EvaluationService();
    private static final StatService STAT = new StatService();
    private static final MessageService MESSAGE = new MessageService();

    /** 输出流：同时写入控制台与 UTF-8 编码的测试报告文件 */
    private static java.io.PrintStream out;

    /** 期望失败的用例动作 */
    private interface Action {
        void run() throws Exception;
    }

    /** 期望失败的用例 */
    private static final class Case {
        private final String name;
        private final Action action;

        Case(String name, Action action) {
            this.name = name;
            this.action = action;
        }
    }

    /** 流程上下文 */
    private static final class Context {
        private int reporterId;
        private int managerId;
        private int adminId;
        private int workerUserId;
        private int worker2UserId;
        private int waterWorkerId;
        private int materialId;
        private RepairOrder order;
        private RepairTask task;
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("storage.mode", "memory");
        // 自测报告以 UTF-8 写入 build/selftest.log，同时输出到控制台
        java.io.File logDir = new java.io.File("build");
        if (!logDir.isDirectory()) {
            logDir.mkdirs();
        }
        out = new java.io.PrintStream(new java.io.FileOutputStream("build/selftest.log"), true, "UTF-8");
        try {
            run();
        } catch (Throwable e) {
            out.println();
            out.println("[异常] 自测流程中断：" + e);
            e.printStackTrace(out);
            System.exit(1);
        }
        out.close();
        System.exit(failed > 0 ? 1 : 0);
    }

    private static void run() {
        out.println("==================== 校园报修系统 · 业务自测 ====================");

        final Context c = new Context();
        final List<Case> cases = new ArrayList<Case>();

        // ---------------------------------------------------------- 用例 1
        section("用例 1：账户注册（表 1.2）");
        c.reporterId = register("stu_test", "123456", "测试学生", "S2024001", "13900000001", "reporter", null);
        check("注册成功并生成用户ID", c.reporterId > 0);
        User reporter = DaoFactory.userDao().findById(Integer.valueOf(c.reporterId));
        check("注册后审核状态为待审核", reporter.getAuditStatus().intValue() == 0);
        check("密码加盐加密存储（64 位摘要）",
                !"123456".equals(reporter.getPassword()) && reporter.getPassword().length() == 64);
        cases.add(new Case("用户名重复注册被拒绝", () -> register("stu_test", "123456", "重复用户",
                "S2024002", "13900000002", "reporter", null)));
        cases.add(new Case("手机号重复注册被拒绝", () -> register("stu_test2", "123456", "重复手机",
                "S2024003", "13900000001", "reporter", null)));
        cases.add(new Case("弱密码（纯数字）注册被拒绝", () -> register("stu_test3", "12345678", "弱密码",
                "S2024004", "13900000004", "reporter", null)));
        cases.add(new Case("未审核账户无法登录", () -> ACCOUNT.login("stu_test", "123456")));

        User admin = createUser("admin_test", "admin123", "测试管理员", "A0001", "13900000009", "admin", 1);
        c.adminId = admin.getUserId().intValue();
        User manager = createUser("mgr_test", "manager123", "测试调度员", "M0001", "13900000008", "manager", 1);
        c.managerId = manager.getUserId().intValue();

        // ---------------------------------------------------------- 用例 2
        section("用例 2：账户审核与登录（表 1.3 / 2.2.2.3）");
        ACCOUNT.auditUser(Integer.valueOf(c.adminId), Integer.valueOf(c.reporterId), Integer.valueOf(1), null);
        check("审核通过后可以登录", ACCOUNT.login("stu_test", "123456") != null);
        cases.add(new Case("密码错误被拒绝", () -> ACCOUNT.login("stu_test", "wrong-password")));
        check("审核状态更新为已通过",
                DaoFactory.userDao().findById(Integer.valueOf(c.reporterId)).getAuditStatus().intValue() == 1);

        // ---------------------------------------------------------- 用例 3
        section("用例 3：基础数据与维修人员档案（表 2.13）");
        DaoFactory.baseDataDao().insert("building", "测试教学楼", 1);
        DaoFactory.baseDataDao().insert("category", "水电", 1);
        DaoFactory.baseDataDao().insert("category", "木工", 2);
        c.workerUserId = registerWorker("worker_test", "worker123", "测试维修工", "W0001", "13900000003", "水电", c.adminId);
        c.worker2UserId = registerWorker("worker_test2", "worker123", "测试木工", "W0002", "13900000004", "木工", c.adminId);
        check("维修人员注册后自动建立档案",
                DaoFactory.workerDao().findByUserId(Integer.valueOf(c.workerUserId)) != null);

        // ---------------------------------------------------------- 用例 4
        section("用例 4：提交报修（表 1.5）");
        c.order = REPORT.submit(Integer.valueOf(c.reporterId), form(
                "building", "测试教学楼", "floor", "3", "room", "308",
                "category", "水电", "description", "卫生间水龙头漏水严重", "priority", "0"));
        check("报修单创建成功", c.order.getOrderId() != null);
        check("报修单初始状态为待审核", c.order.getStatus().intValue() == OrderStatus.PENDING_AUDIT.getCode());
        check("维修管理员收到待审核通知", MESSAGE.unreadCount(Integer.valueOf(c.managerId)) > 0);
        cases.add(new Case("非系统维护楼栋被拒绝", () -> REPORT.submit(Integer.valueOf(c.reporterId), form(
                "building", "不存在的楼栋", "floor", "1", "room", "101",
                "category", "水电", "description", "测试", "priority", "0"))));
        cases.add(new Case("故障描述等必填项缺失被拒绝", () -> REPORT.submit(Integer.valueOf(c.reporterId), form(
                "building", "测试教学楼", "floor", "1", "room", "101",
                "category", "水电", "description", "", "priority", "0"))));

        // ---------------------------------------------------------- 用例 5
        section("用例 5：报修审核与驳回（表 1.8）");
        cases.add(new Case("报修人无权审核报修单", () -> REPORT.audit(Integer.valueOf(c.reporterId),
                c.order.getOrderId(), true, null, null)));
        cases.add(new Case("驳回未填写原因被拒绝", () -> REPORT.audit(Integer.valueOf(c.managerId),
                c.order.getOrderId(), false, "", null)));
        RepairOrder audited = REPORT.audit(Integer.valueOf(c.managerId), c.order.getOrderId(), true, null, "0");
        check("审核通过后状态为待派单", audited.getStatus().intValue() == OrderStatus.PENDING_DISPATCH.getCode());
        check("报修人收到审核结果通知", MESSAGE.unreadCount(Integer.valueOf(c.reporterId)) > 0);
        // 回归校验：审核时只调整优先级的局部更新不得清空报修地点/类别/描述
        RepairOrder afterAudit = DaoFactory.repairOrderDao().findById(c.order.getOrderId());
        check("审核后报修类别未被清空（" + afterAudit.getCategory() + "）",
                "水电".equals(afterAudit.getCategory()));
        check("审核后报修地点与描述未被清空",
                afterAudit.getBuilding() != null && afterAudit.getDescription() != null
                        && afterAudit.getDescription().length() > 0);
        check("审核后可据此正确计算技能匹配", DISPATCH.rankCandidates(c.order.getOrderId())
                .get(0).getSkillScore() > 0);

        // ---------------------------------------------------------- 用例 6
        section("用例 6：智能派单匹配度算法（DispatchService.calcMatchScore）");
        Worker waterWorker = DaoFactory.workerDao().findByUserId(Integer.valueOf(c.workerUserId));
        Worker woodWorker = DaoFactory.workerDao().findByUserId(Integer.valueOf(c.worker2UserId));
        c.waterWorkerId = waterWorker.getWorkerId().intValue();
        DispatchCandidate waterCandidate = DispatchCandidate.score(waterWorker, "水电", Integer.valueOf(0));
        DispatchCandidate woodCandidate = DispatchCandidate.score(woodWorker, "水电", Integer.valueOf(0));
        check("技能匹配的维修工得分更高", waterCandidate.getTotalScore() > woodCandidate.getTotalScore());
        check("技能完全匹配得 40 分", waterCandidate.getSkillScore() == 40);
        check("技能不匹配得 0 分", woodCandidate.getSkillScore() == 0);
        check("总分位于 0～100 区间",
                waterCandidate.getTotalScore() <= 100 && waterCandidate.getTotalScore() >= 0);
        List<DispatchCandidate> candidates = DISPATCH.rankCandidates(c.order.getOrderId());
        check("候选列表按总分降序", candidates.size() >= 2
                && candidates.get(0).getTotalScore() >= candidates.get(1).getTotalScore());
        check("排名第一为技能匹配的维修工",
                candidates.get(0).getWorker().getWorkerId().equals(waterWorker.getWorkerId()));
        check("推荐理由非空", !candidates.get(0).getReasons().isEmpty());

        // ---------------------------------------------------------- 用例 7
        section("用例 7：派单与接单（表 1.8 / 2.2.4）");
        cases.add(new Case("报修人无权派单", () -> DISPATCH.manualDispatch(Integer.valueOf(c.reporterId),
                c.order.getOrderId(), Integer.valueOf(c.waterWorkerId), null)));
        c.task = DISPATCH.manualDispatch(Integer.valueOf(c.managerId), c.order.getOrderId(),
                Integer.valueOf(c.waterWorkerId), "人工派单");
        check("派单后创建维修任务", c.task.getTaskId() != null);
        check("任务初始状态为待接单", c.task.getStatus().intValue() == TaskStatus.PENDING_ACCEPT.getCode());
        check("报修单状态变为已派单", DaoFactory.repairOrderDao().findById(c.order.getOrderId())
                .getStatus().intValue() == OrderStatus.DISPATCHED.getCode());
        check("维修人员在单量 +1",
                DaoFactory.workerDao().findById(Integer.valueOf(c.waterWorkerId)).currentOrderCount() == 1);
        cases.add(new Case("非待派单状态不能重复派单", () -> DISPATCH.manualDispatch(Integer.valueOf(c.managerId),
                c.order.getOrderId(), Integer.valueOf(c.waterWorkerId), null)));
        cases.add(new Case("非本人任务不能接单", () -> DISPATCH.accept(Integer.valueOf(c.worker2UserId),
                c.task.getTaskId())));
        RepairTask accepted = DISPATCH.accept(Integer.valueOf(c.workerUserId), c.task.getTaskId());
        check("接单后任务状态为维修中", accepted.getStatus().intValue() == TaskStatus.REPAIRING.getCode());
        check("接单后报修单状态为维修中", DaoFactory.repairOrderDao().findById(c.order.getOrderId())
                .getStatus().intValue() == OrderStatus.REPAIRING.getCode());

        // ---------------------------------------------------------- 用例 8
        section("用例 8：维修处理与耗材扣减（表 1.9 / 1.10）");
        Material material = new Material();
        material.setMatName("测试水龙头");
        material.setSpec("DN15");
        material.setStock(Integer.valueOf(3));
        material.setUnitPrice(new java.math.BigDecimal("25.00"));
        DaoFactory.materialDao().insert(material);
        c.materialId = material.getMatId().intValue();

        REPAIR.start(Integer.valueOf(c.workerUserId), c.task.getTaskId(), "已到现场");
        check("开始维修记录开始时间",
                DaoFactory.repairTaskDao().findById(c.task.getTaskId()).getStartTime() != null);
        REPAIR.feedback(Integer.valueOf(c.workerUserId), c.task.getTaskId(), "正在更换水龙头", null);
        REPAIR.feedback(Integer.valueOf(c.workerUserId), c.task.getTaskId(), null, "配件缺货，延期半天");
        check("进度反馈与延期原因均已记录",
                DaoFactory.progressDao().findByTask(c.task.getTaskId()).size() >= 3
                        && DaoFactory.repairTaskDao().findById(c.task.getTaskId()).getRemark().contains("配件缺货"));
        cases.add(new Case("超过库存的耗材登记被拒绝", () -> REPAIR.registerMaterial(Integer.valueOf(c.workerUserId),
                c.task.getTaskId(), items(c.materialId, 5), false)));
        check("扣减失败后库存保持不变（事务一致性）",
                DaoFactory.materialDao().findById(Integer.valueOf(c.materialId)).getStock().intValue() == 3);
        REPAIR.registerMaterial(Integer.valueOf(c.workerUserId), c.task.getTaskId(), items(c.materialId, 2), false);
        check("耗材库存按登记数量扣减",
                DaoFactory.materialDao().findById(Integer.valueOf(c.materialId)).getStock().intValue() == 1);
        check("生成耗材使用记录", DaoFactory.materialUsageDao().findByTask(c.task.getTaskId()).size() == 1);
        cases.add(new Case("维修结果为空时不能提交完成", () -> REPAIR.finish(Integer.valueOf(c.workerUserId),
                c.task.getTaskId(), "", null, null, false)));
        RepairTask finished = REPAIR.finish(Integer.valueOf(c.workerUserId), c.task.getTaskId(),
                "更换水龙头与三角阀，试水无渗漏。", null, items(c.materialId, 1), false);
        check("完成登记后任务状态为待确认",
                finished.getStatus().intValue() == TaskStatus.PENDING_CONFIRM.getCode());
        check("报修单状态变为待确认", DaoFactory.repairOrderDao().findById(c.order.getOrderId())
                .getStatus().intValue() == OrderStatus.PENDING_CONFIRM.getCode());
        check("登记后库存扣减至 0",
                DaoFactory.materialDao().findById(Integer.valueOf(c.materialId)).getStock().intValue() == 0);
        cases.add(new Case("维修人员无权确认维修结果", () -> REPAIR.confirm(Integer.valueOf(c.workerUserId),
                c.order.getOrderId(), true, null)));

        // ---------------------------------------------------------- 用例 9
        section("用例 9：结果确认与耗材回滚（表 1.10）");
        cases.add(new Case("确认不通过未填写原因被拒绝", () -> REPAIR.confirm(Integer.valueOf(c.reporterId),
                c.order.getOrderId(), false, "")));
        RepairOrder rejected = REPAIR.confirm(Integer.valueOf(c.managerId), c.order.getOrderId(), false, "漏水问题仍存在");
        check("确认不通过后报修单回到维修中",
                rejected.getStatus().intValue() == OrderStatus.REPAIRING.getCode());
        int stockAfterRollback = DaoFactory.materialDao().findById(Integer.valueOf(c.materialId)).getStock().intValue();
        check("确认不通过后耗材库存已回滚（当前 " + stockAfterRollback + "，登记用量 3）", stockAfterRollback == 3);
        REPAIR.registerMaterial(Integer.valueOf(c.workerUserId), c.task.getTaskId(), items(c.materialId, 1), false);
        REPAIR.finish(Integer.valueOf(c.workerUserId), c.task.getTaskId(), "重新更换并试水合格。", null, null, false);
        RepairOrder confirmed = REPAIR.confirm(Integer.valueOf(c.reporterId), c.order.getOrderId(), true, null);
        check("确认通过后报修单状态为已完成",
                confirmed.getStatus().intValue() == OrderStatus.FINISHED.getCode());
        check("任务状态为已完成", DaoFactory.repairTaskDao().findById(c.task.getTaskId())
                .getStatus().intValue() == TaskStatus.FINISHED.getCode());
        check("完成后维修人员在单量归零",
                DaoFactory.workerDao().findById(Integer.valueOf(c.waterWorkerId)).currentOrderCount() == 0);

        // ---------------------------------------------------------- 用例 10
        section("用例 10：评价与评价审核（2.2.1 评价与统计分析模块）");
        cases.add(new Case("非本人报修单不能评价", () -> EVAL.submit(Integer.valueOf(c.managerId),
                c.order.getOrderId(), "5", "越权评价")));
        cases.add(new Case("评分越界被拒绝", () -> EVAL.submit(Integer.valueOf(c.reporterId),
                c.order.getOrderId(), "6", "评分越界")));
        EVAL.submit(Integer.valueOf(c.reporterId), c.order.getOrderId(), "5", "维修及时，态度好");
        check("评价提交成功", DaoFactory.evaluationDao().findByOrder(c.order.getOrderId()) != null);
        cases.add(new Case("重复评价被拒绝", () -> EVAL.submit(Integer.valueOf(c.reporterId),
                c.order.getOrderId(), "4", "重复评价")));
        int evalId = DaoFactory.evaluationDao().findByOrder(c.order.getOrderId()).getEvalId().intValue();
        EVAL.audit(Integer.valueOf(c.managerId), Integer.valueOf(evalId), Integer.valueOf(1), null);
        EVAL.reply(Integer.valueOf(c.managerId), Integer.valueOf(evalId), "感谢您的评价");
        check("评价审核与回复成功", "感谢您的评价".equals(
                DaoFactory.evaluationDao().findById(Integer.valueOf(evalId)).getReply()));

        // ---------------------------------------------------------- 用例 11
        section("用例 11：催办与撤销（表 1.1 / 1.6）");
        RepairOrder order2 = REPORT.submit(Integer.valueOf(c.reporterId), form(
                "building", "测试教学楼", "floor", "2", "room", "201",
                "category", "木工", "description", "课桌抽屉损坏", "priority", "1"));
        check("紧急报修优先级生效", order2.getPriority().intValue() == 1);
        cases.add(new Case("待审核状态不能催办", () -> REPORT.urge(Integer.valueOf(c.reporterId),
                order2.getOrderId(), "未审核即催办")));
        REPORT.audit(Integer.valueOf(c.managerId), order2.getOrderId(), true, null, null);
        check("审核通过后状态为待派单", DaoFactory.repairOrderDao().findById(order2.getOrderId())
                .getStatus().intValue() == OrderStatus.PENDING_DISPATCH.getCode());
        REPORT.urge(Integer.valueOf(c.reporterId), order2.getOrderId(), "影响上课");
        check("催办次数累加", DaoFactory.repairOrderDao().findById(order2.getOrderId()).getUrgeCount().intValue() == 1);
        cases.add(new Case("他人报修单不能催办", () -> REPORT.urge(Integer.valueOf(c.managerId),
                order2.getOrderId(), "越权催办")));
        RepairOrder canceled = REPORT.cancel(Integer.valueOf(c.reporterId), order2.getOrderId(), "已自行修好");
        check("待派单状态可撤销", canceled.getStatus().intValue() == OrderStatus.CANCELED.getCode());
        cases.add(new Case("已完成的报修单不能撤销", () -> REPORT.cancel(Integer.valueOf(c.reporterId),
                c.order.getOrderId(), "尝试撤销")));

        // ---------------------------------------------------------- 用例 12
        section("用例 12：审核驳回、退单与转单（表 1.8）");
        RepairOrder order3 = REPORT.submit(Integer.valueOf(c.reporterId), form(
                "building", "测试教学楼", "floor", "1", "room", "108",
                "category", "水电", "description", "插座无电", "priority", "0"));
        cases.add(new Case("驳回未填写原因被拒绝", () -> REPORT.audit(Integer.valueOf(c.managerId),
                order3.getOrderId(), false, null, null)));
        RepairOrder rejectedOrder = REPORT.audit(Integer.valueOf(c.managerId), order3.getOrderId(), false,
                "该区域已列入改造计划", null);
        check("驳回后状态为已驳回", rejectedOrder.getStatus().intValue() == OrderStatus.REJECTED.getCode());
        check("驳回原因已记录", rejectedOrder.getRejectReason() != null);
        check("报修人收到驳回通知", MESSAGE.unreadCount(Integer.valueOf(c.reporterId)) > 0);

        RepairOrder order4 = REPORT.submit(Integer.valueOf(c.reporterId), form(
                "building", "测试教学楼", "floor", "1", "room", "109",
                "category", "水电", "description", "灯管闪烁", "priority", "0"));
        REPORT.audit(Integer.valueOf(c.managerId), order4.getOrderId(), true, null, null);
        RepairTask task4 = DISPATCH.manualDispatch(Integer.valueOf(c.managerId), order4.getOrderId(),
                Integer.valueOf(c.waterWorkerId), null);
        cases.add(new Case("退单未填写原因被拒绝", () -> DISPATCH.refuse(Integer.valueOf(c.workerUserId),
                task4.getTaskId(), "")));
        RepairTask refused = DISPATCH.refuse(Integer.valueOf(c.workerUserId), task4.getTaskId(), "缺少登高工具");
        check("退单后任务状态为已退单", refused.getStatus().intValue() == TaskStatus.REFUSED.getCode());
        check("退单后报修单回到待派单", DaoFactory.repairOrderDao().findById(order4.getOrderId())
                .getStatus().intValue() == OrderStatus.PENDING_DISPATCH.getCode());

        RepairTask task4b = DISPATCH.manualDispatch(Integer.valueOf(c.managerId), order4.getOrderId(),
                Integer.valueOf(c.waterWorkerId), "重新派单");
        cases.add(new Case("转单未填写原因被拒绝", () -> DISPATCH.transfer(Integer.valueOf(c.workerUserId),
                task4b.getTaskId(), null, "")));
        RepairTask transferred = DISPATCH.transfer(Integer.valueOf(c.workerUserId), task4b.getTaskId(), null, "需木工配合");
        check("转单后任务回到派单池", transferred.getStatus().intValue() == TaskStatus.RETURNED.getCode());

        // ---------------------------------------------------------- 用例 13
        section("用例 13：统计分析指标（2.2.6 StatService）");
        java.util.Map<String, Object> overview = STAT.overview();
        check("统计总览包含报修总量", ((Long) overview.get("total")).longValue() >= 4);
        check("统计总览包含完成率", ((Double) overview.get("finishRate")).doubleValue() > 0);
        check("故障类型分析包含类别数据", STAT.categoryAnalysis().size() >= 1);
        check("维修人员绩效包含全部维修工", STAT.workerPerformance().size() >= 2);
        check("月度趋势包含当月数据", STAT.monthlyTrend().size() >= 1);
        check("耗材消耗排行包含测试耗材", STAT.materialRanking().size() >= 1);
        check("评价平均分为 5.0",
                Math.abs(((Double) STAT.overview().get("avgScore")).doubleValue() - 5.0d) < 0.01d);
        check("耗材成本合计大于 0", STAT.materialCost().doubleValue() > 0);

        // ---------------------------------------------------------- 用例 14
        section("用例 14：消息通知与在线状态（表 2.17 / 2.13）");
        check("报修人收到多条业务通知", MESSAGE.myMessages(Integer.valueOf(c.reporterId)).size() >= 3);
        MESSAGE.markAllRead(Integer.valueOf(c.reporterId));
        check("全部标记已读后未读数为 0", MESSAGE.unreadCount(Integer.valueOf(c.reporterId)) == 0);
        ACCOUNT.markOnline(Integer.valueOf(c.workerUserId), "测试教学楼值班室");
        Worker online = DaoFactory.workerDao().findByUserId(Integer.valueOf(c.workerUserId));
        check("维修工登录后置为在线并记录位置",
                online.isOnline() && "测试教学楼值班室".equals(online.getLocation()));

        // ---------------------------------------------------------- 用例 15
        section("用例 15：接单超时提醒（表 1.8 接单时限 2 小时）");
        RepairOrder order5 = REPORT.submit(Integer.valueOf(c.reporterId), form(
                "building", "测试教学楼", "floor", "4", "room", "401",
                "category", "水电", "description", "洗手池堵塞", "priority", "0"));
        REPORT.audit(Integer.valueOf(c.managerId), order5.getOrderId(), true, null, null);
        RepairTask task5 = DISPATCH.manualDispatch(Integer.valueOf(c.managerId), order5.getOrderId(),
                Integer.valueOf(c.waterWorkerId), null);
        RepairTask stored = DaoFactory.repairTaskDao().findById(task5.getTaskId());
        stored.setDispatchTime(com.campus.repair.util.DateUtil.hoursAgo(3));
        DaoFactory.repairTaskDao().update(stored);
        check("超时未接单任务被识别", REPORT.overdueTasks().size() >= 1);
        check("超时提醒已发送", DISPATCH.remindOverdueAccept() >= 1);

        // ---------------------------------------------------------- 用例 16
        section("用例 16：账户信息维护与权限控制（表 1.4）");
        cases.add(new Case("修改密码时原密码错误被拒绝", () -> ACCOUNT.modifyPassword(Integer.valueOf(c.reporterId),
                "wrong", "newpass123", "newpass123")));
        ACCOUNT.modifyPassword(Integer.valueOf(c.reporterId), "123456", "newpass123", "newpass123");
        check("修改密码后可用新密码登录", ACCOUNT.login("stu_test", "newpass123") != null);
        cases.add(new Case("手机号格式错误被拒绝", () -> ACCOUNT.modifyInfo(Integer.valueOf(c.reporterId),
                "12345", "测试学生")));
        cases.add(new Case("非系统管理员不能审核账户", () -> ACCOUNT.auditUser(Integer.valueOf(c.managerId),
                Integer.valueOf(c.reporterId), Integer.valueOf(2), "越权审核")));
        ACCOUNT.lockUser(Integer.valueOf(c.adminId), Integer.valueOf(c.reporterId), true);
        check("管理员锁定后账户 is_locked 置为 1",
                DaoFactory.userDao().findById(Integer.valueOf(c.reporterId)).isLockedFlag());
        check("锁定后无法登录", loginRejected("stu_test", "newpass123"));
        ACCOUNT.lockUser(Integer.valueOf(c.adminId), Integer.valueOf(c.reporterId), false);
        check("解锁后可正常登录", ACCOUNT.login("stu_test", "newpass123") != null);

        // ---------------------------------------------------------- 用例 17
        section("用例 17：登录失败锁定阈值（2.2.2.2 达到阈值后锁定账户）");
        int lockedId = register("locked_test", "123456", "锁定测试", "S2024999", "13900000077", "reporter", null);
        ACCOUNT.auditUser(Integer.valueOf(c.adminId), Integer.valueOf(lockedId), Integer.valueOf(1), null);
        for (int i = 0; i < 5; i++) {
            try {
                ACCOUNT.login("locked_test", "bad-password");
            } catch (BusinessException ignored) {
                // 预期失败
            }
        }
        cases.add(new Case("连续 5 次密码错误后账户被锁定", () -> ACCOUNT.login("locked_test", "123456")));

        // ---------------------------------------------------------- 用例 18
        section("用例 18：数据一致性校验（在单量与进行中任务）");
        int activeTasks = 0;
        for (RepairTask item : DaoFactory.repairTaskDao().findAll()) {
            TaskStatus status = TaskStatus.of(item.getStatus());
            if (status != null && status.active()) {
                activeTasks++;
            }
        }
        int totalLoad = 0;
        for (Worker worker : DaoFactory.workerDao().findAll()) {
            totalLoad += worker.currentOrderCount();
        }
        check("在单量合计等于进行中任务数（" + totalLoad + " / " + activeTasks + "）", totalLoad == activeTasks);

        // ---------------------------------------------------------- 异常用例
        section("异常用例：业务规则拦截（共 " + cases.size() + " 项）");
        for (Case item : cases) {
            expectFail(item.name, item.action);
        }

        // ------------------------------------------------------------ 汇总
        out.println();
        out.println("==================== 自测结果 ====================");
        out.println("通过：" + passed + " 项，失败：" + failed + " 项");
        if (failed > 0) {
            for (String failure : FAILURES) {
                out.println("  [FAIL] " + failure);
            }
            return;
        }
        out.println("全部业务规则校验通过 -> PASS");
        out.println("内存库数据：用户 " + MemoryStore.USERS.size() + " 个，报修单 " + MemoryStore.ORDERS.size()
                + " 张，维修任务 " + MemoryStore.TASKS.size() + " 个，耗材使用记录 " + MemoryStore.USAGES.size()
                + " 条，消息 " + MemoryStore.MESSAGES.size() + " 条");
    }

    // ------------------------------------------------------------ 辅助方法

    private static int register(String username, String password, String realName,
                                String studentNo, String phone, String role, String skillTags) {
        java.util.Map<String, String> form = form(
                "username", username, "password", password, "realName", realName,
                "studentNo", studentNo, "phone", phone, "role", role);
        if (skillTags != null) {
            form.put("skillTags", skillTags);
        }
        return ACCOUNT.registerForTest(form).getUserId().intValue();
    }

    /** 判断某账户登录是否被拒绝（用于锁定校验） */
    private static boolean loginRejected(String username, String password) {
        try {
            ACCOUNT.login(username, password);
            return false;
        } catch (BusinessException e) {
            return true;
        }
    }

    private static int registerWorker(String username, String password, String realName, String studentNo,
                                      String phone, String skill, int adminId) {        int userId = register(username, password, realName, studentNo, phone, "worker", skill);
        ACCOUNT.auditUser(Integer.valueOf(adminId), Integer.valueOf(userId), Integer.valueOf(1), null);
        return userId;
    }

    private static User createUser(String username, String password, String realName, String studentNo,
                                   String phone, String role, int auditStatus) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(com.campus.repair.util.PasswordUtil.encrypt(password));
        user.setRealName(realName);
        user.setStudentNo(studentNo);
        user.setPhone(phone);
        user.setRole(role);
        user.setAuditStatus(Integer.valueOf(auditStatus));
        user.setIsLocked(Integer.valueOf(0));
        user.setCreateTime(new java.util.Date());
        DaoFactory.userDao().insert(user);
        return DaoFactory.userDao().findById(user.getUserId());
    }

    private static java.util.Map<String, String> form(String... pairs) {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static java.util.List<MaterialService.MaterialItem> items(int matId, int useCount) {
        java.util.List<MaterialService.MaterialItem> list = new ArrayList<MaterialService.MaterialItem>();
        list.add(new MaterialService.MaterialItem(Integer.valueOf(matId), useCount));
        return list;
    }

    private static void section(String title) {
        out.println();
        out.println("---- " + title + " ----");
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            out.println("  [OK] " + name);
        } else {
            failed++;
            FAILURES.add(name);
            out.println("  [FAIL] " + name);
        }
    }

    private static void expectFail(String name, Action action) {
        try {
            action.run();
            failed++;
            FAILURES.add(name + "（预期失败但执行成功）");
            out.println("  [FAIL] " + name + " —— 预期失败但执行成功");
        } catch (BusinessException e) {
            passed++;
            out.println("  [OK] " + name + "（正确拦截：" + e.getMessage() + "）");
        } catch (Exception e) {
            failed++;
            FAILURES.add(name + "（异常类型不符：" + e.getClass().getSimpleName() + "）");
            out.println("  [FAIL] " + name + " —— 异常类型不符：" + e);
        }
    }
}
