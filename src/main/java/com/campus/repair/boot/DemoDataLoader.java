package com.campus.repair.boot;

import java.math.BigDecimal;
import java.util.Date;

import com.campus.repair.dao.DaoFactory;
import com.campus.repair.domain.AuditStatus;
import com.campus.repair.domain.Evaluation;
import com.campus.repair.domain.Material;
import com.campus.repair.domain.MaterialUsage;
import com.campus.repair.domain.Message;
import com.campus.repair.domain.OrderStatus;
import com.campus.repair.domain.Progress;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.TaskStatus;
import com.campus.repair.domain.User;
import com.campus.repair.domain.Worker;
import com.campus.repair.util.DateUtil;
import com.campus.repair.util.PasswordUtil;

/**
 * 内存库演示数据装载器（仅 storage.mode=memory 时执行）。
 *
 * <p>数据内容与 db/seed.sql 保持一致，便于在未部署 MySQL 的环境中直接体验完整业务闭环，
 * 覆盖：待审核、待派单、已派单、维修中、待确认、已完成、已驳回等全部状态。</p>
 */
public final class DemoDataLoader {

    private DemoDataLoader() {
    }

    /** 演示账号密码（与 README 说明一致） */
    public static final String PWD_REPORTER = "123456";
    public static final String PWD_WORKER = "worker123";
    public static final String PWD_MANAGER = "manager123";
    public static final String PWD_ADMIN = "admin123";

    /** 装载演示数据 */
    public static void load() {
        if (!DaoFactory.isMemoryMode()) {
            return;
        }
        if (DaoFactory.userDao().findAll().size() > 0) {
            return;
        }

        // ---------------------------------------------------------- 基础数据
        String[][] baseData = {
                {"building", "1号教学楼"}, {"building", "2号教学楼"}, {"building", "实验楼"},
                {"building", "图书馆"}, {"building", "学生公寓1栋"}, {"building", "学生公寓2栋"},
                {"building", "行政楼"}, {"building", "体育馆"},
                {"category", "水电"}, {"category", "木工"}, {"category", "设备"},
                {"category", "门窗"}, {"category", "网络"}, {"category", "其他"},
                {"skill", "水电"}, {"skill", "木工"}, {"skill", "设备"}, {"skill", "门窗"}, {"skill", "网络"}
        };
        int sort = 1;
        for (String[] item : baseData) {
            DaoFactory.baseDataDao().insert(item[0], item[1], sort++);
        }

        // ---------------------------------------------------------------- 用户
        user(1, "student", PWD_REPORTER, "李明", "2021010101", "13800000001", "reporter", 1, 0, days(30));
        user(2, "teacher", PWD_REPORTER, "王芳", "T20190101", "13800000002", "reporter", 1, 0, days(30));
        user(3, "worker01", PWD_WORKER, "赵强", "W2020001", "13800000003", "worker", 1, 0, days(30));
        user(4, "worker02", PWD_WORKER, "孙勇", "W2020002", "13800000004", "worker", 1, 0, days(30));
        user(5, "worker03", PWD_WORKER, "周涛", "W2020003", "13800000005", "worker", 1, 0, days(30));
        user(6, "manager", PWD_MANAGER, "陈静", "M2018001", "13800000006", "manager", 1, 0, days(30));
        user(7, "admin", PWD_ADMIN, "系统管理员", "A0001", "13800000007", "admin", 1, 0, days(30));
        user(8, "student02", PWD_REPORTER, "张伟", "2021010102", "13800000008", "reporter", 0, 0, days(2));

        // ------------------------------------------------------------ 维修人员
        worker(1, 3, "赵强", "13800000003", "水电,设备", 0, "1号教学楼值班室", 1);
        worker(2, 4, "孙勇", "13800000004", "木工,门窗", 0, "维修间", 1);
        worker(3, 5, "周涛", "13800000005", "设备,网络", 0, "实验楼", 1);

        // ---------------------------------------------------------------- 耗材
        material(1, "LED灯管", "T8 18W", 120, "15.50");
        material(2, "水龙头", "DN15 单冷", 40, "32.00");
        material(3, "三角阀", "DN15", 60, "18.00");
        material(4, "PVC水管", "DN25", 80, "9.80");
        material(5, "门锁芯", "通用型", 25, "45.00");
        material(6, "合页", "4寸不锈钢", 100, "6.50");
        material(7, "网线", "超六类（米）", 500, "2.20");
        material(8, "网络模块", "RJ45 千兆", 30, "12.00");
        material(9, "空气开关", "C63 2P", 20, "28.00");
        material(10, "密封胶", "中性硅酮（支）", 50, "14.00");

        // -------------------------------------------------------------- 报修单
        // 1 已完成（含评价）  2 已完成（含评价）  3 待确认  4 维修中  5 待派单  6 待审核  7 已驳回
        order(1, 1, "学生公寓1栋", "3", "312", "水电",
                "卫生间水龙头持续漏水，关不紧，地面已积水。", 5, 0, 0, null, days(25), days(25));
        order(2, 2, "1号教学楼", "2", "205", "设备",
                "多媒体讲台投影仪无法开机，指示灯不亮。", 5, 1, 0, null, days(23), days(23));
        order(3, 1, "学生公寓2栋", "5", "501", "水电",
                "走廊声控灯不亮，晚上出入不便。", 4, 0, 0, null, days(6), days(2));
        order(4, 2, "实验楼", "3", "308", "木工",
                "实验台抽屉滑轨损坏，抽屉卡住无法拉开。", 3, 0, 1, null, days(4), days(2));
        order(5, 1, "图书馆", "4", "自习区", "网络",
                "自习区无线网络信号弱，频繁掉线。", 1, 0, 0, null, days(2), days(2));
        order(6, 2, "行政楼", "1", "101", "门窗",
                "办公室窗户把手松动，关不严。", 0, 0, 0, null, hours(6), null);
        order(7, 1, "体育馆", "1", "器材室", "设备",
                "跑步机显示屏黑屏，无法启动。", 7, 0, 0, "该设备已报废，请走资产报损流程。", days(3), days(3));

        // ------------------------------------------------------------ 维修任务
        task(1, 1, 1, days(24), days(24), days(24), days(23), TaskStatus.FINISHED.getCode(),
                "更换水龙头与三角阀，试水无渗漏。", null);
        task(2, 2, 3, days(22), days(22), days(22), days(21), TaskStatus.FINISHED.getCode(),
                "更换电源模块，投影仪恢复正常。", null);
        task(3, 3, 1, days(2), days(2), hours(30), hours(26), TaskStatus.PENDING_CONFIRM.getCode(),
                "更换声控开关与灯管，测试正常。", null);
        task(4, 4, 2, days(3), days(3), hours(28), null, TaskStatus.REPAIRING.getCode(),
                null, "原滑轨型号缺货，预计延期1天。");

        // ------------------------------------------------------------ 耗材使用
        usage(1, 1, 2, 1, days(23));
        usage(2, 1, 3, 1, days(23));
        usage(3, 2, 9, 1, days(21));
        usage(4, 3, 1, 2, hours(26));

        // ---------------------------------------------------------------- 评价
        evaluation(1, 1, 5, "师傅来得很快，维修干净利落，赞！", "感谢反馈，我们会继续努力。", 1, days(23));
        evaluation(2, 2, 4, "修好了，希望下次能再快一点。", null, 1, days(21));
        evaluation(3, 3, 5, "灯换好了，走廊亮堂多了。", null, 0, hours(20));

        // ------------------------------------------------------------ 进度反馈
        progress(1, 1, "已到现场，确认需更换水龙头。", days(24));
        progress(2, 2, "检测为电源模块故障，需更换空气开关。", days(22));
        progress(3, 3, "已更换声控开关与灯管，等待确认。", hours(26));
        progress(4, 4, "原滑轨型号缺货，预计延期1天。", hours(28));

        // ------------------------------------------------------------ 消息通知
        message(1, 6, "您有 1 条新的报修申请待审核（报修单号 6）。", "报修", 0, hours(6));
        message(2, 1, "您的报修单（报修单号 5）已通过审核，等待派单。", "审核", 1, days(2));
        message(3, 5, "您有一条新的维修任务待接收（报修单号 5）。", "派单", 0, days(2));
        message(4, 2, "您的报修单（报修单号 7）未通过审核：该设备已报废，请走资产报损流程。", "审核", 0, days(3));
        message(5, 1, "您的报修单（报修单号 3）维修已完成，请及时确认维修结果。", "维修", 0, hours(26));
        message(6, 7, "用户「student02（张伟）」提交了实名审核申请，请及时审核。", "审核", 0, days(2));
    }

    // ------------------------------------------------------------ 构造方法

    private static void user(int id, String username, String password, String realName, String studentNo,
                             String phone, String role, int auditStatus, int locked, Date createTime) {
        User user = new User();
        user.setUserId(Integer.valueOf(id));
        user.setUsername(username);
        user.setPassword(PasswordUtil.encrypt(password));
        user.setRealName(realName);
        user.setStudentNo(studentNo);
        user.setPhone(phone);
        user.setRole(role);
        user.setAuditStatus(Integer.valueOf(auditStatus));
        user.setIsLocked(Integer.valueOf(locked));
        user.setCreateTime(createTime);
        DaoFactory.userDao().insert(user);
    }

    private static void worker(int id, int userId, String name, String phone, String skills,
                               int currentOrders, String location, int status) {
        Worker worker = new Worker();
        worker.setWorkerId(Integer.valueOf(id));
        worker.setUserId(Integer.valueOf(userId));
        worker.setName(name);
        worker.setPhone(phone);
        worker.setSkillTags(skills);
        worker.setCurrentOrders(Integer.valueOf(currentOrders));
        worker.setLocation(location);
        worker.setStatus(Integer.valueOf(status));
        DaoFactory.workerDao().insert(worker);
    }

    private static void material(int id, String name, String spec, int stock, String price) {
        Material material = new Material();
        material.setMatId(Integer.valueOf(id));
        material.setMatName(name);
        material.setSpec(spec);
        material.setStock(Integer.valueOf(stock));
        material.setUnitPrice(new BigDecimal(price));
        DaoFactory.materialDao().insert(material);
    }

    private static void order(int id, int userId, String building, String floor, String room, String category,
                              String description, int status, int priority, int urgeCount, String rejectReason,
                              Date createTime, Date updateTime) {
        RepairOrder order = new RepairOrder();
        order.setOrderId(Integer.valueOf(id));
        order.setUserId(Integer.valueOf(userId));
        order.setBuilding(building);
        order.setFloor(floor);
        order.setRoom(room);
        order.setCategory(category);
        order.setDescription(description);
        order.setStatus(Integer.valueOf(status));
        order.setPriority(Integer.valueOf(priority));
        order.setUrgeCount(Integer.valueOf(urgeCount));
        order.setRejectReason(rejectReason);
        order.setCreateTime(createTime);
        order.setUpdateTime(updateTime);
        DaoFactory.repairOrderDao().insert(order);
    }

    private static void task(int id, int orderId, int workerId, Date dispatchTime, Date acceptTime,
                             Date startTime, Date finishTime, int status, String result, String remark) {
        RepairTask task = new RepairTask();
        task.setTaskId(Integer.valueOf(id));
        task.setOrderId(Integer.valueOf(orderId));
        task.setWorkerId(Integer.valueOf(workerId));
        task.setDispatchTime(dispatchTime);
        task.setAcceptTime(acceptTime);
        task.setStartTime(startTime);
        task.setFinishTime(finishTime);
        task.setStatus(Integer.valueOf(status));
        task.setResult(result);
        task.setRemark(remark);
        DaoFactory.repairTaskDao().insert(task);
    }

    private static void usage(int id, int taskId, int matId, int useCount, Date useTime) {
        MaterialUsage usage = new MaterialUsage();
        usage.setUseId(Integer.valueOf(id));
        usage.setTaskId(Integer.valueOf(taskId));
        usage.setMatId(Integer.valueOf(matId));
        usage.setUseCount(Integer.valueOf(useCount));
        usage.setUseTime(useTime);
        DaoFactory.materialUsageDao().insert(usage);
    }

    private static void evaluation(int id, int orderId, int score, String comment, String reply,
                                   int auditStatus, Date createTime) {
        Evaluation evaluation = new Evaluation();
        evaluation.setEvalId(Integer.valueOf(id));
        evaluation.setOrderId(Integer.valueOf(orderId));
        evaluation.setScore(Integer.valueOf(score));
        evaluation.setComment(comment);
        evaluation.setReply(reply);
        evaluation.setAuditStatus(Integer.valueOf(auditStatus));
        evaluation.setCreateTime(createTime);
        DaoFactory.evaluationDao().insert(evaluation);
    }

    private static void progress(int id, int taskId, String content, Date createTime) {
        Progress progress = new Progress();
        progress.setProgressId(Integer.valueOf(id));
        progress.setTaskId(Integer.valueOf(taskId));
        progress.setContent(content);
        progress.setCreateTime(createTime);
        DaoFactory.progressDao().insert(progress);
    }

    private static void message(int id, int userId, String content, String msgType, int isRead, Date createTime) {
        Message message = new Message();
        message.setMsgId(Integer.valueOf(id));
        message.setUserId(Integer.valueOf(userId));
        message.setContent(content);
        message.setMsgType(msgType);
        message.setIsRead(Integer.valueOf(isRead));
        message.setCreateTime(createTime);
        DaoFactory.messageDao().insert(message);
    }

    private static Date days(int days) {
        return new Date(System.currentTimeMillis() - days * 86400000L);
    }

    private static Date hours(int hours) {
        return new Date(System.currentTimeMillis() - hours * 3600000L);
    }

    /** 演示数据摘要（启动日志输出） */
    public static String summary() {
        return "演示数据：" + DaoFactory.userDao().findAll().size() + " 个账户、"
                + DaoFactory.repairOrderDao().countAll() + " 张报修单、"
                + DaoFactory.repairTaskDao().findAll().size() + " 个维修任务、"
                + DaoFactory.materialDao().findAll().size() + " 种耗材；"
                + "账号 " + AuditStatus.PASSED.getText() + "：student/teacher（报修人）、worker01~03（维修工）、manager（维修管理员）、admin（系统管理员）";
    }
}
