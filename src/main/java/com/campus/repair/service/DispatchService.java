package com.campus.repair.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.ResultCode;
import com.campus.repair.common.Validate;
import com.campus.repair.config.AppConfig;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.MessageDao;
import com.campus.repair.dao.RepairOrderDao;
import com.campus.repair.dao.RepairTaskDao;
import com.campus.repair.dao.UserDao;
import com.campus.repair.dao.WorkerDao;
import com.campus.repair.domain.DispatchCandidate;
import com.campus.repair.domain.OrderStatus;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.Role;
import com.campus.repair.domain.TaskStatus;
import com.campus.repair.domain.User;
import com.campus.repair.domain.Worker;
import com.campus.repair.util.DateUtil;

/**
 * 派单业务服务（对应设计书 2.2.6 DispatchService 与 2.2.4 派单管理模块设计）。
 *
 * <p>职责：智能派单（DispatchService.smartDispatch / calcMatchScore）、人工派单、接单、
 * 转单、退单，以及接单超时提醒。</p>
 *
 * <p>说明：设计书正文未给出智能派单算法的需求规定与设计细节，本实现按设计书提到的
 * "按技能标签与在单量匹配"的要求补充了可解释的加权评分算法（见 {@link DispatchCandidate}），
 * 并保留人工派单作为算法无法匹配时的兜底手段（与设计书 2.2.4.1 描述一致）。</p>
 */
public class DispatchService {

    private final RepairOrderDao orderDao = DaoFactory.repairOrderDao();
    private final RepairTaskDao taskDao = DaoFactory.repairTaskDao();
    private final WorkerDao workerDao = DaoFactory.workerDao();
    private final UserDao userDao = DaoFactory.userDao();
    private final MessageDao messageDao = DaoFactory.messageDao();

    // ---------------------------------------------------------- 智能派单

    /**
     * 候选维修人员匹配度排序（设计书 2.2.6 calcMatchScore）。
     * 供派单页面展示"推荐维修人员"列表与推荐理由。
     */
    public List<DispatchCandidate> rankCandidates(Integer orderId) {
        RepairOrder order = requireOrder(orderId);
        List<Worker> workers = workerDao.findAll();
        List<DispatchCandidate> candidates = new ArrayList<DispatchCandidate>();
        for (Worker worker : workers) {
            DispatchCandidate candidate = DispatchCandidate.score(worker, order.getCategory(), order.getPriority());
            candidates.add(candidate);
        }
        candidates.sort(new Comparator<DispatchCandidate>() {
            @Override
            public int compare(DispatchCandidate a, DispatchCandidate b) {
                if (a.getTotalScore() != b.getTotalScore()) {
                    return b.getTotalScore() - a.getTotalScore();
                }
                return a.getWorker().currentOrderCount() - b.getWorker().currentOrderCount();
            }
        });
        return candidates;
    }

    /**
     * 智能派单（设计书 DispatchService.smartDispatch）。
     *
     * <p>匹配总分最高且技能匹配分大于 0 的在线维修人员；若无合适人选则抛出业务异常，
     * 由维修管理员改用人工派单。</p>
     */
    public Map<String, Object> smartDispatch(Integer operatorId, Integer orderId) {
        requireManager(operatorId);
        RepairOrder order = requireDispatchable(orderId);
        List<DispatchCandidate> candidates = rankCandidates(orderId);
        DispatchCandidate best = null;
        for (DispatchCandidate candidate : candidates) {
            if (candidate.getSkillScore() > 0 && candidate.getWorker().isOnline()) {
                best = candidate;
                break;
            }
        }
        if (best == null) {
            throw new BusinessException("没有技能匹配且在线可用的维修人员，请使用人工派单");
        }
        RepairTask task = createTask(operatorId, order, best.getWorker().getWorkerId(), "智能派单匹配");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("task", task);
        data.put("candidate", best);
        data.put("candidates", candidates);
        return data;
    }

    // ---------------------------------------------------------- 人工派单

    /**
     * 人工派单（设计书 2.2.4.1 人工派单功能点设计）。
     *
     * <p>维修管理员查看待派单报修单及维修人员当前在单量、技能标签，手动选择维修人员后创建维修任务。</p>
     */
    public RepairTask manualDispatch(Integer operatorId, Integer orderId, Integer workerId, String remark) {
        requireManager(operatorId);
        RepairOrder order = requireDispatchable(orderId);
        Worker worker = workerDao.findById(workerId);
        if (worker == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "维修人员不存在");
        }
        if (!worker.isOnline()) {
            throw new BusinessException("维修人员「" + worker.getName() + "」当前离线，无法派单");
        }
        return createTask(operatorId, order, workerId, remark);
    }

    // -------------------------------------------------------------- 接单

    /**
     * 接单（设计书 2.2.4.2 接单功能点设计）。
     *
     * <p>维修人员接收任务后任务状态由"待接单"变为"维修中"，报修单状态同步为"维修中"。</p>
     */
    public RepairTask accept(Integer userId, Integer taskId) {
        Worker worker = requireWorkerByUser(userId);
        RepairTask task = requireTask(taskId);
        if (!worker.getWorkerId().equals(task.getWorkerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能接收派发给本人的维修任务");
        }
        if (!Integer.valueOf(TaskStatus.PENDING_ACCEPT.getCode()).equals(task.getStatus())) {
            throw new BusinessException("当前任务状态（" + task.getStatusText() + "）不允许接单");
        }
        task.setStatus(Integer.valueOf(TaskStatus.REPAIRING.getCode()));
        task.setAcceptTime(new Date());
        taskDao.update(task);
        orderDao.updateStatus(task.getOrderId(), Integer.valueOf(OrderStatus.REPAIRING.getCode()));
        RepairOrder order = orderDao.findById(task.getOrderId());
        sendMessage(order.getUserId(), "您的报修单（报修单号 " + order.getOrderId() + "）维修人员「"
                + worker.getName() + "」已接单，正在处理中。", "派单");
        sendToManagers("报修单（" + order.getOrderId() + "）已由「" + worker.getName() + "」接单。", "派单");
        return taskDao.findById(taskId);
    }

    // -------------------------------------------------------------- 转单

    /**
     * 转单（设计书表 1.1"申请转单或退单"与类设计 DispatchController.transfer()）。
     * 维修人员可申请将任务转给其他维修人员，由维修管理员指定接手人。
     */
    public RepairTask transfer(Integer userId, Integer taskId, Integer targetWorkerId, String reason) {
        Worker worker = requireWorkerByUser(userId);
        RepairTask task = requireTask(taskId);
        if (!worker.getWorkerId().equals(task.getWorkerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能转出本人的维修任务");
        }
        TaskStatus status = TaskStatus.of(task.getStatus());
        if (status == null || !status.active()) {
            throw new BusinessException("当前任务状态（" + task.getStatusText() + "）不允许转单");
        }
        Validate.create().required("reason", "转单原因", reason).throwIfInvalid();

        // 转单后回到派单池，等待管理员重新派单（无指定接手人时）
        task.setStatus(Integer.valueOf(TaskStatus.RETURNED.getCode()));
        task.setRemark("转单申请：" + reason.trim() + "（原维修人员：" + worker.getName() + "）");
        taskDao.update(task);
        orderDao.updateStatus(task.getOrderId(), Integer.valueOf(OrderStatus.PENDING_DISPATCH.getCode()));
        workerDao.addCurrentOrders(worker.getWorkerId(), -1);
        sendToManagers("维修人员「" + worker.getName() + "」申请转单（报修单号 " + task.getOrderId()
                + "），原因：" + reason.trim() + "，请重新派单。", "派单");

        if (targetWorkerId != null) {
            return manualDispatch(managersId(), task.getOrderId(), targetWorkerId, "转单重派");
        }
        return taskDao.findById(taskId);
    }

    // -------------------------------------------------------------- 退单

    /**
     * 退单（设计书表 1.8 退单功能点：维修人员因故无法完成维修任务时发起退单申请，
     * 经确认后退回派单池重新派单）。
     */
    public RepairTask refuse(Integer userId, Integer taskId, String reason) {
        Worker worker = requireWorkerByUser(userId);
        RepairTask task = requireTask(taskId);
        if (!worker.getWorkerId().equals(task.getWorkerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能退掉本人的维修任务");
        }
        TaskStatus status = TaskStatus.of(task.getStatus());
        if (status == null || !status.active()) {
            throw new BusinessException("当前任务状态（" + task.getStatusText() + "）不允许退单");
        }
        Validate.create().required("reason", "退单原因", reason).throwIfInvalid();
        task.setStatus(Integer.valueOf(TaskStatus.REFUSED.getCode()));
        task.setRemark("退单原因：" + reason.trim());
        taskDao.update(task);
        orderDao.updateStatus(task.getOrderId(), Integer.valueOf(OrderStatus.PENDING_DISPATCH.getCode()));
        workerDao.addCurrentOrders(worker.getWorkerId(), -1);
        sendToManagers("维修人员「" + worker.getName() + "」申请退单（报修单号 " + task.getOrderId()
                + "），原因：" + reason.trim() + "，已退回派单池。", "派单");
        RepairOrder order = orderDao.findById(task.getOrderId());
        sendMessage(order.getUserId(), "您的报修单（报修单号 " + order.getOrderId()
                + "）正在重新安排维修人员，请耐心等待。", "派单");
        return taskDao.findById(taskId);
    }

    // ---------------------------------------------------- 待派单/超时监督

    /** 待派单报修单列表（含派单推荐） */
    public List<RepairOrder> pendingDispatchOrders() {
        com.campus.repair.domain.OrderQuery query = new com.campus.repair.domain.OrderQuery();
        query.setStatus(Integer.valueOf(OrderStatus.PENDING_DISPATCH.getCode()));
        query.setPageSize(200);
        return orderDao.findByCondition(query);
    }

    /** 维修人员任务列表 */
    public List<RepairTask> workerTasks(Integer userId, Integer status) {
        Worker worker = requireWorkerByUser(userId);
        if (status == null) {
            return taskDao.findByWorker(worker.getWorkerId());
        }
        return taskDao.findByWorkerAndStatus(worker.getWorkerId(), status);
    }

    /** 维修工在单量校正（依据进行中任务数） */
    public int recalculateLoad() {
        int changed = 0;
        for (Worker worker : workerDao.findAll()) {
            long active = taskDao.countActiveByWorker(worker.getWorkerId());
            int current = worker.currentOrderCount();
            if (current != (int) active) {
                workerDao.addCurrentOrders(worker.getWorkerId(), (int) active - current);
                changed++;
            }
        }
        return changed;
    }

    /**
     * 接单超时提醒（设计书表 1.8："接单时限为派单后 2 小时，超时自动提醒"）。
     * 由定时任务周期调用。
     */
    public int remindOverdueAccept() {
        int deadlineMinutes = AppConfig.acceptDeadlineMinutes();
        long deadlineMillis = deadlineMinutes * 60000L;
        int count = 0;
        for (RepairTask task : taskDao.findByStatus(Integer.valueOf(TaskStatus.PENDING_ACCEPT.getCode()))) {
            if (!task.isAcceptOverdue(deadlineMillis)) {
                continue;
            }
            double hours = DateUtil.hoursSince(task.getDispatchTime());
            String content = "维修任务（报修单号 " + task.getOrderId() + "）已派单超过 "
                    + deadlineMinutes + " 分钟仍未接单（已 " + Math.round(hours * 10) / 10.0 + " 小时），请尽快处理。";
            Worker worker = workerDao.findById(task.getWorkerId());
            if (worker != null) {
                sendMessage(worker.getUserId(), content, "派单");
            }
            sendToManagers(content, "催办");
            count++;
        }
        return count;
    }

    /**
     * 结果确认超时提醒（设计书 1.1.2.4："完成后 24 小时内需完成结果确认"）。
     */
    public int remindOverdueConfirm() {
        int hours = AppConfig.confirmDeadlineHours();
        int count = 0;
        com.campus.repair.domain.OrderQuery query = new com.campus.repair.domain.OrderQuery();
        query.setStatus(Integer.valueOf(OrderStatus.PENDING_CONFIRM.getCode()));
        query.setPageSize(500);
        for (RepairOrder order : orderDao.findByCondition(query)) {
            if (order.getFinishTime() == null || DateUtil.hoursSince(order.getFinishTime()) <= hours) {
                continue;
            }
            sendMessage(order.getUserId(), "您的报修单（报修单号 " + order.getOrderId() + "）维修已完成超过 "
                    + hours + " 小时，请尽快确认维修结果。", "确认");
            sendToManagers("报修单（" + order.getOrderId() + "）完成登记后超过 " + hours + " 小时未确认结果。", "确认");
            count++;
        }
        return count;
    }

    // ------------------------------------------------------------ 内部方法

    /** 创建维修任务（派单核心逻辑） */
    private RepairTask createTask(Integer operatorId, RepairOrder order, Integer workerId, String remark) {
        final RepairTask[] holder = new RepairTask[1];
        TxTemplate.execute(new TxTemplate.Action<Void>() {
            @Override
            public Void run() {
                RepairTask task = new RepairTask();
                task.setOrderId(order.getOrderId());
                task.setWorkerId(workerId);
                task.setDispatchTime(new Date());
                task.setStatus(Integer.valueOf(TaskStatus.PENDING_ACCEPT.getCode()));
                task.setRemark(remark);
                taskDao.insert(task);
                orderDao.updateStatus(order.getOrderId(), Integer.valueOf(OrderStatus.DISPATCHED.getCode()));
                workerDao.addCurrentOrders(workerId, 1);
                holder[0] = task;
                return null;
            }
        });
        Worker worker = workerDao.findById(workerId);
        sendMessage(worker.getUserId(), "您有一条新的维修任务待接收（报修单号 " + order.getOrderId() + "，地点："
                + order.getLocation() + "，类别：" + order.getCategory() + "）。请在 "
                + AppConfig.acceptDeadlineMinutes() + " 分钟内接单。", "派单");
        sendMessage(order.getUserId(), "您的报修单（报修单号 " + order.getOrderId() + "）已派单给维修人员「"
                + worker.getName() + "」，联系电话：" + worker.getPhone() + "。", "派单");
        return taskDao.findById(holder[0].getTaskId());
    }

    private RepairOrder requireOrder(Integer orderId) {
        RepairOrder order = orderDao.findById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报修单不存在");
        }
        return order;
    }

    /** 校验报修单可派单 */
    private RepairOrder requireDispatchable(Integer orderId) {
        RepairOrder order = requireOrder(orderId);
        if (order.getStatus() == null || order.getStatus().intValue() != OrderStatus.PENDING_DISPATCH.getCode()) {
            throw new BusinessException("仅待派单状态的报修单可以派单，当前状态：" + order.getStatusText());
        }
        RepairTask active = taskDao.findActiveByOrder(orderId);
        if (active != null) {
            throw new BusinessException("该报修单已存在进行中的维修任务（任务号 " + active.getTaskId() + "）");
        }
        return order;
    }

    private RepairTask requireTask(Integer taskId) {
        RepairTask task = taskDao.findById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "维修任务不存在");
        }
        return task;
    }

    private Worker requireWorkerByUser(Integer userId) {
        Worker worker = workerDao.findByUserId(userId);
        if (worker == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前账户未关联维修人员档案");
        }
        return worker;
    }

    private void requireManager(Integer operatorId) {
        User operator = userDao.findById(operatorId);
        if (operator == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态失效，请重新登录");
        }
        if (!Role.MANAGER.getCode().equals(operator.getRole()) && !Role.ADMIN.getCode().equals(operator.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅维修管理员可执行派单操作");
        }
    }

    /** 取一名维修管理员 ID（转单重派时作为操作人） */
    private Integer managersId() {
        List<User> managers = userDao.findByRole(Role.MANAGER.getCode());
        if (managers.isEmpty()) {
            List<User> admins = userDao.findByRole(Role.ADMIN.getCode());
            if (admins.isEmpty()) {
                throw new BusinessException("系统中不存在维修管理员账户");
            }
            return admins.get(0).getUserId();
        }
        return managers.get(0).getUserId();
    }

    private void sendMessage(Integer userId, String content, String type) {
        new MessageService().send(userId, content, type);
    }

    private void sendToManagers(String content, String type) {
        new MessageService().sendToManagers(content, type);
    }
}
