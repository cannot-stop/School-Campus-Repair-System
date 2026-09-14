package com.campus.repair.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.PageResult;
import com.campus.repair.common.ResultCode;
import com.campus.repair.common.Validate;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.MaterialDao;
import com.campus.repair.dao.MaterialUsageDao;
import com.campus.repair.dao.MessageDao;
import com.campus.repair.dao.ProgressDao;
import com.campus.repair.dao.RepairOrderDao;
import com.campus.repair.dao.RepairTaskDao;
import com.campus.repair.dao.UserDao;
import com.campus.repair.domain.MaterialUsage;
import com.campus.repair.domain.OrderQuery;
import com.campus.repair.domain.OrderStatus;
import com.campus.repair.domain.Progress;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.Role;
import com.campus.repair.domain.TaskStatus;
import com.campus.repair.domain.User;
import com.campus.repair.util.DateUtil;

/**
 * 报修业务服务（对应设计书 2.2.6 ReportService 与 2.2.3 报修管理模块设计）。
 *
 * <p>职责：提交报修、报修查询、报修详情、撤销报修、报修催办、报修审核（受理/驳回）。</p>
 */
public class ReportService {

    private final RepairOrderDao orderDao = DaoFactory.repairOrderDao();
    private final RepairTaskDao taskDao = DaoFactory.repairTaskDao();
    private final UserDao userDao = DaoFactory.userDao();
    private final MessageDao messageDao = DaoFactory.messageDao();
    private final MaterialUsageDao usageDao = DaoFactory.materialUsageDao();
    private final MaterialDao materialDao = DaoFactory.materialDao();
    private final ProgressDao progressDao = DaoFactory.progressDao();

    // -------------------------------------------------------------- 提交报修

    /**
     * 提交报修（对应设计书表 1.5 提交报修功能点）。
     *
     * <p>主事件流：填写报修地点/类别/故障描述 → 提交 → 保存报修单（状态"待审核"）
     * → 通知维修管理员待审核 → 返回报修详情。</p>
     */
    public RepairOrder submit(Integer userId, Map<String, String> form) {
        User user = requireUser(userId);
        if (!Role.REPORTER.getCode().equals(user.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅报修人可以提交报修申请");
        }
        String building = Validate.trim(form.get("building"));
        String floor = Validate.trim(form.get("floor"));
        String room = Validate.trim(form.get("room"));
        String category = Validate.trim(form.get("category"));
        String description = Validate.trim(form.get("description"));
        String images = Validate.trim(form.get("images"));
        String priorityText = Validate.trim(form.get("priority"));

        Validate.create()
                .required("building", "报修楼栋", building)
                .required("floor", "楼层", floor)
                .required("room", "房间号", room)
                .required("category", "报修类别", category)
                .required("description", "故障描述", description)
                .maxLength("description", "故障描述", description, 500)
                .throwIfInvalid();

        // 业务规则：报修地点必须属于系统维护的楼栋
        List<String> buildings = DaoFactory.baseDataDao().findValues("building");
        if (!buildings.isEmpty() && !buildings.contains(building)) {
            throw new BusinessException("报修楼栋必须是系统维护的楼栋：" + String.join("、", buildings));
        }
        int priority = 0;
        if (priorityText != null && ("1".equals(priorityText) || "紧急".equals(priorityText))) {
            priority = 1;
        }

        final int finalPriority = priority;
        final String finalImages = images == null ? null : images.trim();
        RepairOrder order = TxTemplate.execute(new TxTemplate.Action<RepairOrder>() {
            @Override
            public RepairOrder run() {
                RepairOrder entity = new RepairOrder();
                entity.setUserId(userId);
                entity.setBuilding(building);
                entity.setFloor(floor);
                entity.setRoom(room);
                entity.setCategory(category);
                entity.setDescription(description);
                entity.setImages(finalImages);
                entity.setStatus(Integer.valueOf(OrderStatus.PENDING_AUDIT.getCode()));
                entity.setPriority(Integer.valueOf(finalPriority));
                entity.setUrgeCount(Integer.valueOf(0));
                entity.setCreateTime(new Date());
                entity.setUpdateTime(new Date());
                orderDao.insert(entity);

                // 通知维修管理员有新报修待审核
                for (User manager : userDao.findByRole(Role.MANAGER.getCode())) {
                    sendMessage(manager.getUserId(),
                            "您有 1 条新的报修申请待审核（报修单号 " + entity.getOrderId() + "）。", "报修");
                }
                return entity;
            }
        });
        return detail(userId, order.getOrderId());
    }

    // -------------------------------------------------------------- 查询

    /** 报修查询（设计书 2.2.3.2 报修查询与撤销功能点） */
    public PageResult<RepairOrder> query(Integer userId, Map<String, String> params) {
        User user = requireUser(userId);
        OrderQuery query = buildQuery(params);
        // 报修人仅能查询本人报修单
        if (Role.REPORTER.getCode().equals(user.getRole())) {
            query.setUserId(userId);
        } else if (Role.WORKER.getCode().equals(user.getRole())) {
            com.campus.repair.domain.Worker worker = DaoFactory.workerDao().findByUserId(userId);
            query.setWorkerId(worker == null ? Integer.valueOf(-1) : worker.getWorkerId());
        }
        return orderDao.pageByCondition(query);
    }

    /** 待审核报修单（维修管理员） */
    public PageResult<RepairOrder> queryPendingAudit(Map<String, String> params, boolean pendingDispatch) {
        Map<String, String> copy = new HashMap<String, String>(params == null ? new HashMap<String, String>() : params);
        copy.put("status", String.valueOf(pendingDispatch
                ? OrderStatus.PENDING_DISPATCH.getCode() : OrderStatus.PENDING_AUDIT.getCode()));
        return orderDao.pageByCondition(buildQuery(copy));
    }

    /** 报修详情（含任务、进度、耗材与评价） */
    public RepairOrder detail(Integer viewerId, Integer orderId) {
        RepairOrder order = orderDao.findById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报修单不存在");
        }
        checkViewPermission(viewerId, order);
        if (order.getTaskId() != null) {
            List<Progress> progresses = progressDao.findByTask(order.getTaskId());
            order.setProgressList(progresses);
            List<MaterialUsage> usages = usageDao.findByTask(order.getTaskId());
            StringBuilder remark = new StringBuilder();
            for (MaterialUsage usage : usages) {
                if (remark.length() > 0) {
                    remark.append("；");
                }
                remark.append(usage.getMatName() == null ? ("耗材#" + usage.getMatId()) : usage.getMatName())
                        .append(" × ").append(usage.getUseCount());
            }
            order.setRemark(order.getRemark() == null ? remark.toString()
                    : order.getRemark() + "｜耗材：" + remark);
        }
        return order;
    }

    /** 报修单状态流转轨迹（用于详情页时间线） */
    public List<RepairTask> taskHistory(Integer viewerId, Integer orderId) {
        RepairOrder order = orderDao.findById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报修单不存在");
        }
        checkViewPermission(viewerId, order);
        return taskDao.findByOrder(orderId);
    }

    // -------------------------------------------------------------- 撤销

    /** 撤销报修（表 1.6：待审核或待派单状态可撤销） */
    public RepairOrder cancel(Integer userId, Integer orderId, String reason) {
        RepairOrder order = orderDao.findById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报修单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能撤销本人提交的报修单");
        }
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status == null || !status.cancelable()) {
            throw new BusinessException("当前状态（" + OrderStatus.textOf(order.getStatus())
                    + "）不允许撤销，仅待审核或待派单状态的报修单可撤销");
        }
        orderDao.updateStatus(orderId, Integer.valueOf(OrderStatus.CANCELED.getCode()));
        String text = "报修单（" + orderId + "）已被报修人撤销"
                + (Validate.isBlank(reason) ? "。" : "，原因：" + reason.trim());
        for (User manager : userDao.findByRole(Role.MANAGER.getCode())) {
            sendMessage(manager.getUserId(), text, "报修");
        }
        return orderDao.findById(orderId);
    }

    // -------------------------------------------------------------- 催办

    /** 报修催办（表 1.1：对长时间未处理的报修进行催办） */
    public RepairOrder urge(Integer userId, Integer orderId, String reason) {
        RepairOrder order = orderDao.findById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报修单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能催办本人提交的报修单");
        }
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status == null || !status.urgable()) {
            throw new BusinessException("当前状态（" + OrderStatus.textOf(order.getStatus()) + "）无需催办");
        }
        orderDao.increaseUrgeCount(orderId);
        String content = "报修单（" + orderId + "）已被报修人催办"
                + (Validate.isBlank(reason) ? "，请尽快处理。" : "，原因：" + reason.trim());
        // 通知维修管理员与当前维修人员
        for (User manager : userDao.findByRole(Role.MANAGER.getCode())) {
            sendMessage(manager.getUserId(), content, "催办");
        }
        if (order.getWorkerId() != null) {
            com.campus.repair.domain.Worker worker = DaoFactory.workerDao().findById(order.getWorkerId());
            if (worker != null) {
                sendMessage(worker.getUserId(), content, "催办");
            }
        }
        return orderDao.findById(orderId);
    }

    // -------------------------------------------------------------- 审核

    /**
     * 报修审核（设计书表 1.8 报修审核功能点）。
     *
     * @param accept   是否受理
     * @param reason   不受理时的退回原因
     * @param priority 受理时可调整优先级
     */
    public RepairOrder audit(Integer operatorId, Integer orderId, boolean accept, String reason, String priority) {
        requireManager(operatorId);
        RepairOrder order = orderDao.findById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报修单不存在");
        }
        if (order.getStatus() == null || order.getStatus().intValue() != OrderStatus.PENDING_AUDIT.getCode()) {
            throw new BusinessException("仅待审核状态的报修单可以审核，当前状态：" + order.getStatusText());
        }
        if (!accept && Validate.isBlank(reason)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不受理的报修单必须填写退回原因");
        }
        if (accept) {
            orderDao.updateStatus(orderId, Integer.valueOf(OrderStatus.PENDING_DISPATCH.getCode()));
            if (priority != null && !priority.trim().isEmpty()) {
                RepairOrder update = new RepairOrder();
                update.setOrderId(orderId);
                update.setPriority(Integer.valueOf("1".equals(priority.trim()) ? 1 : 0));
                orderDao.update(update);
            }
            sendMessage(order.getUserId(), "您的报修申请（报修单号 " + orderId + "）已通过审核，等待派单。", "审核");
        } else {
            orderDao.updateReject(orderId, Integer.valueOf(OrderStatus.REJECTED.getCode()), reason.trim());
            sendMessage(order.getUserId(), "您的报修申请（报修单号 " + orderId + "）未通过审核：" + reason.trim(), "审核");
        }
        return orderDao.findById(orderId);
    }

    /** 报修统计概览（报修人/管理员首页） */
    public Map<String, Object> statistics(Integer userId, String role) {
        Map<String, Object> data = new HashMap<String, Object>();
        if (Role.REPORTER.getCode().equals(role)) {
            OrderQuery query = new OrderQuery();
            query.setUserId(userId);
            query.setPageSize(1000);
            List<RepairOrder> orders = orderDao.findByCondition(query);
            data.put("total", Integer.valueOf(orders.size()));
            data.put("processing", Long.valueOf(countStatus(orders, OrderStatus.PENDING_AUDIT, OrderStatus.PENDING_DISPATCH,
                    OrderStatus.DISPATCHED, OrderStatus.REPAIRING, OrderStatus.PENDING_CONFIRM)));
            data.put("finished", Long.valueOf(countStatus(orders, OrderStatus.FINISHED)));
            data.put("canceled", Long.valueOf(countStatus(orders, OrderStatus.CANCELED, OrderStatus.REJECTED)));
            return data;
        }
        data.put("pendingAudit", Long.valueOf(orderDao.countByStatus(Integer.valueOf(OrderStatus.PENDING_AUDIT.getCode()))));
        data.put("pendingDispatch", Long.valueOf(orderDao.countByStatus(Integer.valueOf(OrderStatus.PENDING_DISPATCH.getCode()))));
        data.put("repairing", Long.valueOf(orderDao.countByStatus(Integer.valueOf(OrderStatus.REPAIRING.getCode()))));
        data.put("pendingConfirm", Long.valueOf(orderDao.countByStatus(Integer.valueOf(OrderStatus.PENDING_CONFIRM.getCode()))));
        data.put("finished", Long.valueOf(orderDao.countByStatus(Integer.valueOf(OrderStatus.FINISHED.getCode()))));
        data.put("total", Long.valueOf(orderDao.countAll()));
        data.put("avgScore", Double.valueOf(DaoFactory.evaluationDao().avgScore()));
        return data;
    }

    /** 超时未接单任务（派单后超过 2 小时未接单，供管理员监督与提醒） */
    public List<RepairTask> overdueTasks() {
        int deadline = com.campus.repair.config.AppConfig.acceptDeadlineMinutes();
        List<RepairTask> result = new java.util.ArrayList<RepairTask>();
        for (RepairTask task : taskDao.findByStatus(Integer.valueOf(TaskStatus.PENDING_ACCEPT.getCode()))) {
            if (task.isAcceptOverdue(deadline * 60000L)) {
                result.add(task);
            }
        }
        return result;
    }

    /** 超时未确认的报修单（完成登记超过 24 小时未确认结果） */
    public List<RepairOrder> overdueConfirms() {
        int hours = com.campus.repair.config.AppConfig.confirmDeadlineHours();
        List<RepairOrder> result = new java.util.ArrayList<RepairOrder>();
        OrderQuery query = new OrderQuery();
        query.setStatus(Integer.valueOf(OrderStatus.PENDING_CONFIRM.getCode()));
        query.setPageSize(1000);
        for (RepairOrder order : orderDao.findByCondition(query)) {
            if (order.getFinishTime() != null && DateUtil.hoursSince(order.getFinishTime()) > hours) {
                result.add(order);
            }
        }
        return result;
    }

    /** 耗材使用明细（详情页展示） */
    public List<MaterialUsage> usageOfOrder(Integer orderId) {
        RepairOrder order = orderDao.findById(orderId);
        if (order == null || order.getTaskId() == null) {
            return new java.util.ArrayList<MaterialUsage>();
        }
        return usageDao.findByTask(order.getTaskId());
    }

    /** 可供报修人选择的基础数据 */
    public Map<String, Object> formOptions() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("buildings", DaoFactory.baseDataDao().findValues("building"));
        data.put("categories", DaoFactory.baseDataDao().findValues("category"));
        data.put("materials", materialDao.findAll());
        return data;
    }

    // ------------------------------------------------------------ 内部方法

    private OrderQuery buildQuery(Map<String, String> params) {
        OrderQuery query = new OrderQuery();
        if (params == null) {
            return query;
        }
        query.setStatus(parseInt(params.get("status")));
        query.setPriority(parseInt(params.get("priority")));
        query.setCategory(Validate.trim(params.get("category")));
        query.setBuilding(Validate.trim(params.get("building")));
        query.setKeyword(Validate.trim(params.get("keyword")));
        query.setOrderId(parseInt(params.get("orderId")));
        query.setBeginTime(DateUtil.parse(params.get("beginTime")));
        query.setEndTime(DateUtil.parseEndOfDay(params.get("endTime")));
        Integer pageNum = parseInt(params.get("pageNum"));
        Integer pageSize = parseInt(params.get("pageSize"));
        if (pageNum != null && pageNum.intValue() > 0) {
            query.setPageNum(pageNum.intValue());
        }
        if (pageSize != null && pageSize.intValue() > 0) {
            query.setPageSize(Math.min(pageSize.intValue(), 200));
        }
        return query;
    }

    private Integer parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private long countStatus(List<RepairOrder> orders, OrderStatus... statuses) {
        long count = 0;
        for (RepairOrder order : orders) {
            for (OrderStatus status : statuses) {
                if (order.getStatus() != null && order.getStatus().intValue() == status.getCode()) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private void checkViewPermission(Integer viewerId, RepairOrder order) {
        if (viewerId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        User viewer = userDao.findById(viewerId);
        if (viewer == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态失效，请重新登录");
        }
        if (Role.ADMIN.getCode().equals(viewer.getRole()) || Role.MANAGER.getCode().equals(viewer.getRole())) {
            return;
        }
        if (Role.REPORTER.getCode().equals(viewer.getRole())) {
            if (!viewerId.equals(order.getUserId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只能查看本人提交的报修单");
            }
            return;
        }
        if (Role.WORKER.getCode().equals(viewer.getRole())) {
            com.campus.repair.domain.Worker worker = DaoFactory.workerDao().findByUserId(viewerId);
            if (worker == null || order.getWorkerId() == null || !worker.getWorkerId().equals(order.getWorkerId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只能查看派发给本人的维修任务");
            }
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该报修单");
    }

    private User requireUser(Integer userId) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态失效，请重新登录");
        }
        return user;
    }

    private void requireManager(Integer operatorId) {
        User operator = requireUser(operatorId);
        if (!Role.MANAGER.getCode().equals(operator.getRole()) && !Role.ADMIN.getCode().equals(operator.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅维修管理员可执行该操作");
        }
    }

    private void sendMessage(Integer userId, String content, String type) {
        new MessageService().send(userId, content, type);
    }

    /** 供统计模块复用：查询条件构造 */
    public OrderQuery queryForStatistics(Map<String, String> params) {
        return buildQuery(params);
    }

    /** 消息统计（未读提醒） */
    public long unreadMessages(Integer userId) {
        return messageDao.countUnread(userId);
    }
}
