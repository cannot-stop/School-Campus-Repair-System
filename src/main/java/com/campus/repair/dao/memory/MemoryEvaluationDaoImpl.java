package com.campus.repair.dao.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.campus.repair.dao.EvaluationDao;
import com.campus.repair.domain.Evaluation;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.User;
import com.campus.repair.domain.Worker;

/**
 * 评价数据访问内存实现。
 */
public class MemoryEvaluationDaoImpl implements EvaluationDao {

    @Override
    public int insert(Evaluation evaluation) {
        int id = MemoryStore.nextId(MemoryStore.EVALUATION_SEQ, evaluation.getEvalId());
        evaluation.setEvalId(Integer.valueOf(id));
        if (evaluation.getCreateTime() == null) {
            evaluation.setCreateTime(new Date());
        }
        if (evaluation.getAuditStatus() == null) {
            evaluation.setAuditStatus(Integer.valueOf(0));
        }
        MemoryStore.EVALUATIONS.put(Integer.valueOf(id), evaluation);
        return 1;
    }

    @Override
    public int update(Evaluation evaluation) {
        Evaluation exists = MemoryStore.EVALUATIONS.get(evaluation.getEvalId());
        if (exists == null) {
            return 0;
        }
        exists.setScore(evaluation.getScore());
        exists.setComment(evaluation.getComment());
        exists.setReply(evaluation.getReply());
        exists.setAuditStatus(evaluation.getAuditStatus());
        return 1;
    }

    @Override
    public int updateReply(Integer evalId, String reply) {
        Evaluation exists = MemoryStore.EVALUATIONS.get(evalId);
        if (exists == null) {
            return 0;
        }
        exists.setReply(reply);
        return 1;
    }

    @Override
    public int updateAuditStatus(Integer evalId, Integer auditStatus) {
        Evaluation exists = MemoryStore.EVALUATIONS.get(evalId);
        if (exists == null) {
            return 0;
        }
        exists.setAuditStatus(auditStatus);
        return 1;
    }

    @Override
    public int delete(Integer evalId) {
        return MemoryStore.EVALUATIONS.remove(evalId) == null ? 0 : 1;
    }

    @Override
    public Evaluation findById(Integer evalId) {
        return enrich(MemoryStore.EVALUATIONS.get(evalId));
    }

    @Override
    public Evaluation findByOrder(Integer orderId) {
        for (Evaluation evaluation : MemoryStore.EVALUATIONS.values()) {
            if (orderId.equals(evaluation.getOrderId())) {
                return enrich(evaluation);
            }
        }
        return null;
    }

    @Override
    public List<Evaluation> findAll() {
        List<Evaluation> result = new ArrayList<Evaluation>();
        for (Evaluation evaluation : MemoryStore.EVALUATIONS.values()) {
            result.add(enrich(evaluation));
        }
        result.sort(new Comparator<Evaluation>() {
            @Override
            public int compare(Evaluation a, Evaluation b) {
                return b.getEvalId().compareTo(a.getEvalId());
            }
        });
        return result;
    }

    @Override
    public List<Evaluation> findByAuditStatus(Integer auditStatus) {
        List<Evaluation> result = new ArrayList<Evaluation>();
        for (Evaluation evaluation : findAll()) {
            if (auditStatus.equals(evaluation.getAuditStatus())) {
                result.add(evaluation);
            }
        }
        return result;
    }

    @Override
    public List<Evaluation> findByWorker(Integer workerId) {
        List<Evaluation> result = new ArrayList<Evaluation>();
        for (Evaluation evaluation : findAll()) {
            RepairTask task = activeTask(evaluation.getOrderId());
            if (task != null && workerId.equals(task.getWorkerId())) {
                result.add(evaluation);
            }
        }
        return result;
    }

    @Override
    public double avgScore() {
        double sum = 0;
        int count = 0;
        for (Evaluation evaluation : MemoryStore.EVALUATIONS.values()) {
            if (evaluation.getScore() != null && Integer.valueOf(1).equals(evaluation.getAuditStatus())) {
                sum += evaluation.getScore().intValue();
                count++;
            }
        }
        return count == 0 ? 0d : sum / count;
    }

    @Override
    public double avgScoreByWorker(Integer workerId) {
        double sum = 0;
        int count = 0;
        for (Evaluation evaluation : MemoryStore.EVALUATIONS.values()) {
            RepairTask task = activeTask(evaluation.getOrderId());
            if (task != null && workerId.equals(task.getWorkerId()) && evaluation.getScore() != null) {
                sum += evaluation.getScore().intValue();
                count++;
            }
        }
        return count == 0 ? 0d : sum / count;
    }

    @Override
    public long countAll() {
        return MemoryStore.EVALUATIONS.size();
    }

    /** 带出报修单、报修人与维修人员信息 */
    public static Evaluation enrich(Evaluation evaluation) {
        if (evaluation == null) {
            return null;
        }
        RepairOrder order = MemoryStore.ORDERS.get(evaluation.getOrderId());
        if (order != null) {
            evaluation.setOrderNo(order.getOrderId());
            evaluation.setCategory(order.getCategory());
            User reporter = MemoryStore.USERS.get(order.getUserId());
            if (reporter != null) {
                evaluation.setReporterName(reporter.getRealName());
            }
        }
        RepairTask task = activeTask(evaluation.getOrderId());
        if (task != null) {
            Worker worker = MemoryStore.WORKERS.get(task.getWorkerId());
            if (worker != null) {
                evaluation.setWorkerName(worker.getName());
            }
        }
        return evaluation;
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
