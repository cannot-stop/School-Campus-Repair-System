package com.campus.repair.dao.memory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.campus.repair.common.PageResult;
import com.campus.repair.dao.RepairOrderDao;
import com.campus.repair.domain.Evaluation;
import com.campus.repair.domain.OrderQuery;
import com.campus.repair.domain.Progress;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.User;
import com.campus.repair.domain.Worker;

/**
 * 报修单数据访问内存实现。
 *
 * <p>查询带出报修人、当前任务、维修人员与评价信息，语义与 JdbcRepairOrderDaoImpl 的 JOIN 查询一致。</p>
 */
public class MemoryRepairOrderDaoImpl implements RepairOrderDao {

    @Override
    public int insert(RepairOrder order) {
        int id = MemoryStore.nextId(MemoryStore.ORDER_SEQ, order.getOrderId());
        order.setOrderId(Integer.valueOf(id));
        if (order.getCreateTime() == null) {
            order.setCreateTime(new Date());
        }
        if (order.getStatus() == null) {
            order.setStatus(Integer.valueOf(0));
        }
        if (order.getPriority() == null) {
            order.setPriority(Integer.valueOf(0));
        }
        if (order.getUrgeCount() == null) {
            order.setUrgeCount(Integer.valueOf(0));
        }
        MemoryStore.ORDERS.put(Integer.valueOf(id), order);
        return 1;
    }

    /**
     * 更新报修单：仅覆盖非空字段。
     *
     * <p>Service 层可能只设置部分字段（例如审核时只调整优先级），
     * 因此本方法对 null 字段保持原值，避免合法的业务字段被覆盖为空。</p>
     */
    @Override
    public int update(RepairOrder order) {
        RepairOrder exists = MemoryStore.ORDERS.get(order.getOrderId());
        if (exists == null) {
            return 0;
        }
        if (order.getBuilding() != null) {
            exists.setBuilding(order.getBuilding());
        }
        if (order.getFloor() != null) {
            exists.setFloor(order.getFloor());
        }
        if (order.getRoom() != null) {
            exists.setRoom(order.getRoom());
        }
        if (order.getCategory() != null) {
            exists.setCategory(order.getCategory());
        }
        if (order.getDescription() != null) {
            exists.setDescription(order.getDescription());
        }
        if (order.getImages() != null) {
            exists.setImages(order.getImages());
        }
        if (order.getStatus() != null) {
            exists.setStatus(order.getStatus());
        }
        if (order.getPriority() != null) {
            exists.setPriority(order.getPriority());
        }
        if (order.getRejectReason() != null) {
            exists.setRejectReason(order.getRejectReason());
        }
        exists.setUpdateTime(new Date());
        return 1;
    }

    @Override
    public int updateStatus(Integer orderId, Integer status) {
        RepairOrder exists = MemoryStore.ORDERS.get(orderId);
        if (exists == null) {
            return 0;
        }
        exists.setStatus(status);
        exists.setUpdateTime(new Date());
        return 1;
    }

    @Override
    public int updateReject(Integer orderId, Integer status, String rejectReason) {
        RepairOrder exists = MemoryStore.ORDERS.get(orderId);
        if (exists == null) {
            return 0;
        }
        exists.setStatus(status);
        exists.setRejectReason(rejectReason);
        exists.setUpdateTime(new Date());
        return 1;
    }

    @Override
    public int increaseUrgeCount(Integer orderId) {
        RepairOrder exists = MemoryStore.ORDERS.get(orderId);
        if (exists == null) {
            return 0;
        }
        exists.setUrgeCount(Integer.valueOf(exists.getUrgeCount() == null ? 1 : exists.getUrgeCount() + 1));
        exists.setUpdateTime(new Date());
        return 1;
    }

    @Override
    public int delete(Integer orderId) {
        return MemoryStore.ORDERS.remove(orderId) == null ? 0 : 1;
    }

    @Override
    public RepairOrder findById(Integer orderId) {
        return enrich(MemoryStore.ORDERS.get(orderId));
    }

    @Override
    public List<RepairOrder> findByCondition(OrderQuery query) {
        return MemoryQuery.page(MemoryQuery.filterOrders(new ArrayList<RepairOrder>(MemoryStore.ORDERS.values()), query),
                query == null ? 1 : query.getPageNum(), query == null ? 10 : query.getPageSize()).getRows();
    }

    @Override
    public long countByCondition(OrderQuery query) {
        return MemoryQuery.filterOrders(new ArrayList<RepairOrder>(MemoryStore.ORDERS.values()), query).size();
    }

    @Override
    public PageResult<RepairOrder> pageByCondition(OrderQuery query) {
        List<RepairOrder> filtered = MemoryQuery.filterOrders(
                new ArrayList<RepairOrder>(MemoryStore.ORDERS.values()), query);
        PageResult<RepairOrder> page = MemoryQuery.page(filtered,
                query == null ? 1 : query.getPageNum(), query == null ? 10 : query.getPageSize());
        List<RepairOrder> enriched = new ArrayList<RepairOrder>();
        for (RepairOrder order : page.getRows()) {
            enriched.add(enrich(order));
        }
        page.setRows(enriched);
        return page;
    }

    @Override
    public List<RepairOrder> findByWorker(Integer workerId, Integer status) {
        List<RepairOrder> result = new ArrayList<RepairOrder>();
        for (RepairOrder order : MemoryStore.ORDERS.values()) {
            RepairTask task = activeTask(order.getOrderId());
            if (task == null || !workerId.equals(task.getWorkerId())) {
                continue;
            }
            if (status != null && !status.equals(order.getStatus())) {
                continue;
            }
            result.add(enrich(order));
        }
        return result;
    }

    @Override
    public List<RepairOrder> findByUser(Integer userId) {
        List<RepairOrder> result = new ArrayList<RepairOrder>();
        for (RepairOrder order : MemoryStore.ORDERS.values()) {
            if (userId.equals(order.getUserId())) {
                result.add(enrich(order));
            }
        }
        return result;
    }

    @Override
    public long countByStatus(Integer status) {
        long count = 0;
        for (RepairOrder order : MemoryStore.ORDERS.values()) {
            if (status.equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public long countAll() {
        return MemoryStore.ORDERS.size();
    }

    /** 带出关联信息 */
    public static RepairOrder enrich(RepairOrder order) {
        if (order == null) {
            return null;
        }
        User reporter = MemoryStore.USERS.get(order.getUserId());
        if (reporter != null) {
            order.setReporterUsername(reporter.getUsername());
            order.setReporterName(reporter.getRealName());
            order.setReporterPhone(reporter.getPhone());
        }
        RepairTask task = activeTask(order.getOrderId());
        if (task != null) {
            order.setTaskId(task.getTaskId());
            order.setWorkerId(task.getWorkerId());
            order.setTaskStatus(task.getStatus());
            order.setDispatchTime(task.getDispatchTime());
            order.setAcceptTime(task.getAcceptTime());
            order.setFinishTime(task.getFinishTime());
            order.setResult(task.getResult());
            order.setRemark(task.getRemark());
            Worker worker = MemoryStore.WORKERS.get(task.getWorkerId());
            if (worker != null) {
                order.setWorkerName(worker.getName());
                order.setWorkerPhone(worker.getPhone());
            }
            List<Progress> progresses = new ArrayList<Progress>();
            for (Progress progress : MemoryStore.PROGRESSES.values()) {
                if (task.getTaskId().equals(progress.getTaskId())) {
                    progresses.add(progress);
                }
            }
            order.setProgressList(progresses);
        }
        for (Evaluation evaluation : MemoryStore.EVALUATIONS.values()) {
            if (order.getOrderId().equals(evaluation.getOrderId())) {
                order.setEvalScore(evaluation.getScore());
                order.setEvalComment(evaluation.getComment());
                order.setEvalReply(evaluation.getReply());
                break;
            }
        }
        return order;
    }

    private static RepairTask activeTask(Integer orderId) {
        RepairTask found = null;
        for (RepairTask task : MemoryStore.TASKS.values()) {
            if (!orderId.equals(task.getOrderId())) {
                continue;
            }
            Integer status = task.getStatus();
            if (status != null && (status.intValue() == 4 || status.intValue() == 5 || status.intValue() == 6)) {
                continue;
            }
            if (found == null || task.getTaskId().intValue() > found.getTaskId().intValue()) {
                found = task;
            }
        }
        return found;
    }
}
