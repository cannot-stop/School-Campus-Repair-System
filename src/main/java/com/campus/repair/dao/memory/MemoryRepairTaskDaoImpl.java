package com.campus.repair.dao.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.campus.repair.dao.RepairTaskDao;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.TaskStatus;
import com.campus.repair.domain.User;
import com.campus.repair.domain.Worker;

/**
 * 维修任务数据访问内存实现。
 */
public class MemoryRepairTaskDaoImpl implements RepairTaskDao {

    @Override
    public int insert(RepairTask task) {
        int id = MemoryStore.nextId(MemoryStore.TASK_SEQ, task.getTaskId());
        task.setTaskId(Integer.valueOf(id));
        if (task.getDispatchTime() == null) {
            task.setDispatchTime(new Date());
        }
        if (task.getStatus() == null) {
            task.setStatus(Integer.valueOf(TaskStatus.PENDING_ACCEPT.getCode()));
        }
        MemoryStore.TASKS.put(Integer.valueOf(id), task);
        return 1;
    }

    @Override
    public int update(RepairTask task) {
        RepairTask exists = MemoryStore.TASKS.get(task.getTaskId());
        if (exists == null) {
            return 0;
        }
        exists.setWorkerId(task.getWorkerId());
        exists.setAcceptTime(task.getAcceptTime());
        exists.setStartTime(task.getStartTime());
        exists.setFinishTime(task.getFinishTime());
        exists.setStatus(task.getStatus());
        exists.setResult(task.getResult());
        exists.setRemark(task.getRemark());
        return 1;
    }

    @Override
    public int updateStatus(Integer taskId, Integer status) {
        RepairTask exists = MemoryStore.TASKS.get(taskId);
        if (exists == null) {
            return 0;
        }
        exists.setStatus(status);
        return 1;
    }

    @Override
    public int updateFinish(Integer taskId, Integer status, String result, String remark) {
        RepairTask exists = MemoryStore.TASKS.get(taskId);
        if (exists == null) {
            return 0;
        }
        exists.setStatus(status);
        exists.setResult(result);
        exists.setRemark(remark);
        exists.setFinishTime(new Date());
        return 1;
    }

    @Override
    public int delete(Integer taskId) {
        return MemoryStore.TASKS.remove(taskId) == null ? 0 : 1;
    }

    @Override
    public RepairTask findById(Integer taskId) {
        return enrich(MemoryStore.TASKS.get(taskId));
    }

    @Override
    public RepairTask findActiveByOrder(Integer orderId) {
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
        return enrich(found);
    }

    @Override
    public List<RepairTask> findByOrder(Integer orderId) {
        List<RepairTask> result = new ArrayList<RepairTask>();
        for (RepairTask task : MemoryStore.TASKS.values()) {
            if (orderId.equals(task.getOrderId())) {
                result.add(enrich(task));
            }
        }
        return result;
    }

    @Override
    public List<RepairTask> findByWorker(Integer workerId) {
        List<RepairTask> result = new ArrayList<RepairTask>();
        for (RepairTask task : MemoryStore.TASKS.values()) {
            if (workerId.equals(task.getWorkerId())) {
                result.add(enrich(task));
            }
        }
        result.sort(new Comparator<RepairTask>() {
            @Override
            public int compare(RepairTask a, RepairTask b) {
                int sa = a.getStatus() == null ? 0 : a.getStatus().intValue();
                int sb = b.getStatus() == null ? 0 : b.getStatus().intValue();
                return sa - sb;
            }
        });
        return result;
    }

    @Override
    public List<RepairTask> findByWorkerAndStatus(Integer workerId, Integer status) {
        List<RepairTask> result = new ArrayList<RepairTask>();
        for (RepairTask task : findByWorker(workerId)) {
            if (status.equals(task.getStatus())) {
                result.add(task);
            }
        }
        return result;
    }

    @Override
    public List<RepairTask> findByStatus(Integer status) {
        List<RepairTask> result = new ArrayList<RepairTask>();
        for (RepairTask task : MemoryStore.TASKS.values()) {
            if (status.equals(task.getStatus())) {
                result.add(enrich(task));
            }
        }
        return result;
    }

    @Override
    public List<RepairTask> findAll() {
        List<RepairTask> result = new ArrayList<RepairTask>();
        for (RepairTask task : MemoryStore.TASKS.values()) {
            result.add(enrich(task));
        }
        return result;
    }

    @Override
    public long countActiveByWorker(Integer workerId) {
        long count = 0;
        for (RepairTask task : MemoryStore.TASKS.values()) {
            if (!workerId.equals(task.getWorkerId())) {
                continue;
            }
            TaskStatus status = TaskStatus.of(task.getStatus());
            if (status != null && (status.active() || status == TaskStatus.TRANSFERRED)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public long countByStatus(Integer status) {
        long count = 0;
        for (RepairTask task : MemoryStore.TASKS.values()) {
            if (status.equals(task.getStatus())) {
                count++;
            }
        }
        return count;
    }

    /** 带出报修单与报修人信息 */
    public static RepairTask enrich(RepairTask task) {
        if (task == null) {
            return null;
        }
        Worker worker = MemoryStore.WORKERS.get(task.getWorkerId());
        if (worker != null) {
            task.setWorkerName(worker.getName());
            task.setWorkerPhone(worker.getPhone());
        }
        RepairOrder order = MemoryStore.ORDERS.get(task.getOrderId());
        if (order != null) {
            task.setOrderNo(order.getOrderId());
            task.setLocation(order.getLocation());
            task.setCategory(order.getCategory());
            task.setDescription(order.getDescription());
            task.setPriority(order.getPriority());
            User reporter = MemoryStore.USERS.get(order.getUserId());
            if (reporter != null) {
                task.setReporterName(reporter.getRealName());
                task.setReporterPhone(reporter.getPhone());
            }
        }
        return task;
    }
}
