package com.campus.repair.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.ResultCode;
import com.campus.repair.common.Validate;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.MaterialUsageDao;
import com.campus.repair.dao.ProgressDao;
import com.campus.repair.dao.RepairOrderDao;
import com.campus.repair.dao.RepairTaskDao;
import com.campus.repair.dao.WorkerDao;
import com.campus.repair.domain.MaterialUsage;
import com.campus.repair.domain.OrderStatus;
import com.campus.repair.domain.Progress;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.Role;
import com.campus.repair.domain.TaskStatus;
import com.campus.repair.domain.User;
import com.campus.repair.domain.Worker;

/**
 * 维修业务服务（对应设计书 2.2.6 RepairService 与 2.2.5 维修管理模块设计）。
 *
 * <p>职责：开始维修、进度反馈、耗材登记、维修完成登记、结果确认（含库存回滚）。</p>
 */
public class RepairService {

    private final RepairTaskDao taskDao = DaoFactory.repairTaskDao();
    private final RepairOrderDao orderDao = DaoFactory.repairOrderDao();
    private final WorkerDao workerDao = DaoFactory.workerDao();
    private final MaterialUsageDao usageDao = DaoFactory.materialUsageDao();
    private final ProgressDao progressDao = DaoFactory.progressDao();
    private final MaterialService materialService = new MaterialService();

    // ------------------------------------------------------------ 开始维修

    /**
     * 开始维修（设计书表 1.10 开始维修功能点：到达现场后确认开始维修，系统记录开始维修时间）。
     */
    public RepairTask start(Integer userId, Integer taskId, String remark) {
        Worker worker = requireWorker(userId);
        RepairTask task = requireOwnTask(worker, taskId);
        TaskStatus status = TaskStatus.of(task.getStatus());
        if (status != TaskStatus.PENDING_ACCEPT && status != TaskStatus.REPAIRING) {
            throw new BusinessException("当前任务状态（" + task.getStatusText() + "）不能开始维修");
        }
        task.setStatus(Integer.valueOf(TaskStatus.REPAIRING.getCode()));
        if (task.getAcceptTime() == null) {
            task.setAcceptTime(new Date());
        }
        task.setStartTime(new Date());
        if (!Validate.isBlank(remark)) {
            task.setRemark(remark.trim());
        }
        taskDao.update(task);
        orderDao.updateStatus(task.getOrderId(), Integer.valueOf(OrderStatus.REPAIRING.getCode()));
        addProgress(task.getTaskId(), "维修人员已到达现场并开始维修" + (Validate.isBlank(remark) ? "。" : "：" + remark.trim()));
        sendMessage(orderOf(task).getUserId(), "您的报修单（报修单号 " + task.getOrderId() + "）维修人员已开始维修。", "维修");
        return taskDao.findById(taskId);
    }

    // ------------------------------------------------------------ 进度反馈

    /**
     * 进度反馈（设计书表 1.10 进度反馈功能点：随时反馈维修进度，填写延期原因）。
     * 业务规则：延期原因必填；超时任务自动提醒维修管理员。
     */
    public Progress feedback(Integer userId, Integer taskId, String content, String delayReason) {
        Worker worker = requireWorker(userId);
        RepairTask task = requireOwnTask(worker, taskId);
        TaskStatus status = TaskStatus.of(task.getStatus());
        if (status == null || !status.active()) {
            throw new BusinessException("当前任务状态（" + task.getStatusText() + "）无法反馈进度");
        }
        String text = Validate.trim(content);
        String delay = Validate.trim(delayReason);
        if (Validate.isBlank(text) && Validate.isBlank(delay)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请填写进度说明或延期原因");
        }
        StringBuilder message = new StringBuilder();
        if (!Validate.isBlank(text)) {
            message.append(text);
        }
        if (!Validate.isBlank(delay)) {
            if (message.length() > 0) {
                message.append("｜");
            }
            message.append("延期原因：").append(delay);
            task.setRemark("延期原因：" + delay);
            taskDao.update(task);
            // 超时任务自动提醒维修管理员
            sendToManagers("报修单（" + task.getOrderId() + "）维修延期：" + delay, "维修");
        }
        Progress progress = addProgress(task.getTaskId(), message.toString());
        // 延期时同步告知报修人
        if (!Validate.isBlank(delay)) {
            sendMessage(orderOf(task).getUserId(), "您的报修单（报修单号 " + task.getOrderId()
                    + "）维修进度更新：延期原因 " + delay, "维修");
        }
        return progress;
    }

    // ------------------------------------------------------------ 耗材登记

    /**
     * 耗材登记（设计书表 1.10 耗材登记功能点）。
     * 业务规则：登记数量不得超过库存；库存不足时提示并允许先登记后补库（记录待补库标记）。
     *
     * @param allowOverStock 库存不足时是否允许先登记（对应备选事件流）
     */
    public List<MaterialUsage> registerMaterial(Integer userId, Integer taskId,
                                                List<MaterialService.MaterialItem> items, boolean allowOverStock) {
        Worker worker = requireWorker(userId);
        RepairTask task = requireOwnTask(worker, taskId);
        TaskStatus status = TaskStatus.of(task.getStatus());
        if (status == null || !status.active()) {
            throw new BusinessException("当前任务状态（" + task.getStatusText() + "）无法登记耗材");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择需要登记的耗材");
        }
        List<MaterialUsage> result = new ArrayList<MaterialUsage>();
        MaterialService.DeductResult deductResult;
        if (allowOverStock) {
            // 先登记后补库：仅记录使用，不校验库存（补库后可再次登记）
            for (MaterialService.MaterialItem item : items) {
                result.add(saveUsage(taskId, item));
            }
            addProgress(taskId, "登记耗材（库存不足，先登记后补库）");
            return result;
        }
        deductResult = materialService.deduct(items);
        boolean success = false;
        try {
            for (MaterialService.MaterialItem item : items) {
                result.add(saveUsage(taskId, item));
            }
            success = true;
        } finally {
            if (!success) {
                materialService.restore(deductResult);
            }
        }
        StringBuilder text = new StringBuilder("登记耗材：");
        for (int i = 0; i < result.size(); i++) {
            if (i > 0) {
                text.append("、");
            }
            text.append(result.get(i).getMatName()).append(" × ").append(result.get(i).getUseCount());
        }
        addProgress(taskId, text.toString());
        return result;
    }

    // -------------------------------------------------------- 维修完成登记

    /**
     * 维修完成登记（对应设计书表 1.9 维修完成登记功能点）。
     *
     * <p>主事件流：填写维修结果 → 登记更换耗材 → 提交完成 → 校验任务状态并保存 →
     * 扣减库存并生成耗材使用记录 → 状态置为"待确认"并通知维修管理员与报修人。</p>
     */
    public RepairTask finish(Integer userId, Integer taskId, String result, String remark,
                             List<MaterialService.MaterialItem> items, boolean allowOverStock) {
        Worker worker = requireWorker(userId);
        RepairTask task = requireOwnTask(worker, taskId);
        if (!Integer.valueOf(TaskStatus.REPAIRING.getCode()).equals(task.getStatus())) {
            throw new BusinessException("仅维修中状态的任务可以提交完成，当前状态：" + task.getStatusText());
        }
        Validate.create()
                .required("result", "维修结果", result)
                .maxLength("result", "维修结果", result, 255)
                .maxLength("remark", "备注", remark, 255)
                .throwIfInvalid();

        final RepairTask[] holder = new RepairTask[1];
        TxTemplate.execute(new TxTemplate.Action<Void>() {
            @Override
            public Void run() {
                // 先保存任务结果与状态
                task.setStatus(Integer.valueOf(TaskStatus.PENDING_CONFIRM.getCode()));
                task.setResult(result.trim());
                if (!Validate.isBlank(remark)) {
                    task.setRemark(remark.trim());
                }
                task.setFinishTime(new Date());
                taskDao.update(task);

                // 校验任务状态（并发场景下防止重复提交）
                RepairTask current = taskDao.findById(taskId);
                if (current == null
                        || !Integer.valueOf(TaskStatus.PENDING_CONFIRM.getCode()).equals(current.getStatus())) {
                    throw new BusinessException("任务状态异常，提交失败");
                }

                // 扣减耗材库存并生成使用记录（同一事务内，保证一致性）
                if (items != null && !items.isEmpty()) {
                    if (allowOverStock) {
                        for (MaterialService.MaterialItem item : items) {
                            saveUsage(taskId, item);
                        }
                    } else {
                        MaterialService.DeductResult deductResult = materialService.deduct(items);
                        boolean ok = false;
                        try {
                            for (MaterialService.MaterialItem item : items) {
                                saveUsage(taskId, item);
                            }
                            ok = true;
                        } finally {
                            if (!ok) {
                                materialService.restore(deductResult);
                            }
                        }
                    }
                }
                orderDao.updateStatus(task.getOrderId(), Integer.valueOf(OrderStatus.PENDING_CONFIRM.getCode()));
                addProgress(taskId, "维修完成登记：" + result.trim());
                holder[0] = task;
                return null;
            }
        });

        RepairOrder order = orderOf(task);
        sendMessage(order.getUserId(), "您的报修单（报修单号 " + order.getOrderId()
                + "）维修已完成（维修人员：" + worker.getName() + "），请及时确认维修结果。", "维修");
        sendToManagers("报修单（" + order.getOrderId() + "）已完成登记，等待确认。", "维修");
        return taskDao.findById(taskId);
    }

    // ------------------------------------------------------------ 结果确认

    /**
     * 结果确认（设计书表 1.10 结果确认功能点：确认后报修单归档；确认不通过的报修单退回重新处理）。
     */
    public RepairOrder confirm(Integer userId, Integer orderId, boolean pass, String reason) {
        User user = requireUser(userId);
        RepairOrder order = orderDao.findById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报修单不存在");
        }
        boolean isReporter = Role.REPORTER.getCode().equals(user.getRole()) && userId.equals(order.getUserId());
        boolean isManager = Role.MANAGER.getCode().equals(user.getRole()) || Role.ADMIN.getCode().equals(user.getRole());
        if (!isReporter && !isManager) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅报修人或维修管理员可以确认维修结果");
        }
        if (order.getStatus() == null || order.getStatus().intValue() != OrderStatus.PENDING_CONFIRM.getCode()) {
            throw new BusinessException("仅待确认状态的报修单可以确认，当前状态：" + order.getStatusText());
        }
        if (!pass && Validate.isBlank(reason)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "确认不通过时必须填写原因");
        }
        RepairTask task = order.getTaskId() == null ? null : taskDao.findById(order.getTaskId());
        if (pass) {
            orderDao.updateStatus(orderId, Integer.valueOf(OrderStatus.FINISHED.getCode()));
            if (task != null) {
                task.setStatus(Integer.valueOf(TaskStatus.FINISHED.getCode()));
                taskDao.update(task);
                workerDao.addCurrentOrders(task.getWorkerId(), -1);
                Worker worker = workerDao.findById(task.getWorkerId());
                if (worker != null) {
                    sendMessage(worker.getUserId(), "报修单（" + orderId + "）的维修结果已确认，任务完成。", "确认");
                }
            }
            sendToManagers("报修单（" + orderId + "）的维修结果已确认，报修单归档。", "确认");
            sendMessage(order.getUserId(), "报修单（" + orderId + "）已完成归档，欢迎对本次维修服务进行评价。", "确认");
        } else {
            // 确认不通过：退回重新处理，并回滚已扣减的耗材库存
            orderDao.updateStatus(orderId, Integer.valueOf(OrderStatus.REPAIRING.getCode()));
            if (task != null) {
                materialService.restoreByTask(task.getTaskId());
                task.setStatus(Integer.valueOf(TaskStatus.REPAIRING.getCode()));
                task.setRemark("确认不通过：" + reason.trim());
                taskDao.update(task);
                addProgress(task.getTaskId(), "结果确认不通过，退回重新处理：" + reason.trim());
                Worker worker = workerDao.findById(task.getWorkerId());
                if (worker != null) {
                    sendMessage(worker.getUserId(), "报修单（" + orderId + "）的维修结果确认未通过：" + reason.trim()
                            + "，请重新处理。", "确认");
                }
            }
            sendToManagers("报修单（" + orderId + "）的维修结果确认未通过，已退回重新处理。", "确认");
        }
        return orderDao.findById(orderId);
    }

    // ------------------------------------------------------------ 查询

    /** 维修任务详情（维修人员处理页面） */
    public Map<String, Object> taskDetail(Integer userId, Integer taskId) {
        User user = requireUser(userId);
        RepairTask task = taskDao.findById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "维修任务不存在");
        }
        if (Role.WORKER.getCode().equals(user.getRole())) {
            Worker worker = requireWorker(userId);
            if (!worker.getWorkerId().equals(task.getWorkerId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只能查看本人的维修任务");
            }
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("task", task);
        data.put("order", orderDao.findById(task.getOrderId()));
        data.put("progressList", progressDao.findByTask(taskId));
        data.put("usageList", usageDao.findByTask(taskId));
        data.put("materials", materialService.listAll());
        data.put("acceptDeadlineMinutes", Integer.valueOf(com.campus.repair.config.AppConfig.acceptDeadlineMinutes()));
        data.put("confirmDeadlineHours", Integer.valueOf(com.campus.repair.config.AppConfig.confirmDeadlineHours()));
        return data;
    }

    /** 维修工工作台统计 */
    public Map<String, Object> workerStatistics(Integer userId) {
        Worker worker = requireWorker(userId);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("worker", worker);
        data.put("pending", Long.valueOf(taskDao.countActiveByWorker(worker.getWorkerId())));
        data.put("waiting", Integer.valueOf(taskDao.findByWorkerAndStatus(worker.getWorkerId(),
                Integer.valueOf(TaskStatus.PENDING_ACCEPT.getCode())).size()));
        data.put("repairing", Integer.valueOf(taskDao.findByWorkerAndStatus(worker.getWorkerId(),
                Integer.valueOf(TaskStatus.REPAIRING.getCode())).size()));
        data.put("pendingConfirm", Integer.valueOf(taskDao.findByWorkerAndStatus(worker.getWorkerId(),
                Integer.valueOf(TaskStatus.PENDING_CONFIRM.getCode())).size()));
        data.put("finished", Integer.valueOf(taskDao.findByWorkerAndStatus(worker.getWorkerId(),
                Integer.valueOf(TaskStatus.FINISHED.getCode())).size()));
        data.put("avgScore", Double.valueOf(DaoFactory.evaluationDao().avgScoreByWorker(worker.getWorkerId())));
        return data;
    }

    // ------------------------------------------------------------ 内部方法

    private MaterialUsage saveUsage(Integer taskId, MaterialService.MaterialItem item) {
        MaterialUsage exists = usageDao.findByTaskAndMaterial(taskId, item.getMatId());
        if (exists != null) {
            int newCount = (exists.getUseCount() == null ? 0 : exists.getUseCount().intValue()) + item.getUseCount();
            usageDao.updateCount(exists.getUseId(), newCount);
            exists.setUseCount(Integer.valueOf(newCount));
            return exists;
        }
        MaterialUsage usage = new MaterialUsage();
        usage.setTaskId(taskId);
        usage.setMatId(item.getMatId());
        usage.setUseCount(Integer.valueOf(item.getUseCount()));
        usage.setUseTime(new Date());
        usageDao.insert(usage);
        return usageDao.findById(usage.getUseId());
    }

    private Progress addProgress(Integer taskId, String content) {
        Progress progress = new Progress();
        progress.setTaskId(taskId);
        progress.setContent(content);
        progress.setCreateTime(new Date());
        progressDao.insert(progress);
        return progress;
    }

    private RepairTask requireOwnTask(Worker worker, Integer taskId) {
        RepairTask task = taskDao.findById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "维修任务不存在");
        }
        if (!worker.getWorkerId().equals(task.getWorkerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作派发给本人的维修任务");
        }
        return task;
    }

    private Worker requireWorker(Integer userId) {
        Worker worker = workerDao.findByUserId(userId);
        if (worker == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前账户未关联维修人员档案");
        }
        return worker;
    }

    private User requireUser(Integer userId) {
        User user = DaoFactory.userDao().findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态失效，请重新登录");
        }
        return user;
    }

    private RepairOrder orderOf(RepairTask task) {
        RepairOrder order = orderDao.findById(task.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报修单不存在");
        }
        return order;
    }

    private void sendMessage(Integer userId, String content, String type) {
        new MessageService().send(userId, content, type);
    }

    private void sendToManagers(String content, String type) {
        new MessageService().sendToManagers(content, type);
    }
}
