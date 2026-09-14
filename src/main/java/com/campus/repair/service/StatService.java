package com.campus.repair.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.EvaluationDao;
import com.campus.repair.dao.MaterialUsageDao;
import com.campus.repair.dao.RepairOrderDao;
import com.campus.repair.dao.RepairTaskDao;
import com.campus.repair.dao.WorkerDao;
import com.campus.repair.domain.Evaluation;
import com.campus.repair.domain.MaterialUsage;
import com.campus.repair.domain.OrderStatus;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.StatRow;
import com.campus.repair.domain.TaskStatus;
import com.campus.repair.domain.Worker;
import com.campus.repair.util.DateUtil;

/**
 * 统计分析服务（对应设计书 2.2.6 StatService 与 2.2.1"维修统计、故障类型分析、绩效统计"）。
 *
 * <p>指标：报修总量与状态分布、故障类型（类别）分析、楼栋分布、维修人员绩效
 * （完成量、完成率、平均耗时、平均评分、耗材成本）、月度趋势、耗材消耗排行。</p>
 */
public class StatService {

    private final RepairOrderDao orderDao = DaoFactory.repairOrderDao();
    private final RepairTaskDao taskDao = DaoFactory.repairTaskDao();
    private final WorkerDao workerDao = DaoFactory.workerDao();
    private final EvaluationDao evaluationDao = DaoFactory.evaluationDao();
    private final MaterialUsageDao usageDao = DaoFactory.materialUsageDao();

    /** 总览指标（维修管理员/系统管理员工作台） */
    public Map<String, Object> overview() {
        Map<String, Object> data = new HashMap<String, Object>();
        long total = orderDao.countAll();
        long finished = orderDao.countByStatus(Integer.valueOf(OrderStatus.FINISHED.getCode()));
        data.put("total", Long.valueOf(total));
        data.put("finished", Long.valueOf(finished));
        data.put("finishRate", Double.valueOf(total == 0 ? 0d
                : Math.round(finished * 1000d / total) / 10d));
        data.put("pendingAudit", Long.valueOf(orderDao.countByStatus(Integer.valueOf(OrderStatus.PENDING_AUDIT.getCode()))));
        data.put("pendingDispatch", Long.valueOf(orderDao.countByStatus(Integer.valueOf(OrderStatus.PENDING_DISPATCH.getCode()))));
        data.put("processing", Long.valueOf(orderDao.countByStatus(Integer.valueOf(OrderStatus.DISPATCHED.getCode()))
                + orderDao.countByStatus(Integer.valueOf(OrderStatus.REPAIRING.getCode()))));
        data.put("pendingConfirm", Long.valueOf(orderDao.countByStatus(Integer.valueOf(OrderStatus.PENDING_CONFIRM.getCode()))));
        data.put("canceled", Long.valueOf(orderDao.countByStatus(Integer.valueOf(OrderStatus.CANCELED.getCode()))
                + orderDao.countByStatus(Integer.valueOf(OrderStatus.REJECTED.getCode()))));
        data.put("avgScore", Double.valueOf(evaluationDao.avgScore()));
        data.put("materialCost", materialCost());
        data.put("avgHours", Double.valueOf(avgProcessHours()));
        data.put("statusDistribution", statusDistribution());
        return data;
    }

    /** 状态分布（用于图表） */
    public List<Map<String, Object>> statusDistribution() {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (OrderStatus status : OrderStatus.values()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("status", Integer.valueOf(status.getCode()));
            row.put("statusText", status.getText());
            row.put("count", Long.valueOf(orderDao.countByStatus(Integer.valueOf(status.getCode()))));
            rows.add(row);
        }
        return rows;
    }

    /** 故障类型分析（按报修类别） */
    public List<StatRow> categoryAnalysis() {
        Map<String, StatRow> map = new LinkedHashMap<String, StatRow>();
        for (RepairOrder order : allOrders()) {
            String key = order.getCategory() == null ? "未分类" : order.getCategory();
            StatRow row = map.get(key);
            if (row == null) {
                row = new StatRow();
                row.setGroupName(key);
                map.put(key, row);
            }
            row.setOrderCount(row.getOrderCount() + 1);
            if (order.getStatus() != null && order.getStatus().intValue() == OrderStatus.FINISHED.getCode()) {
                row.setFinishedCount(row.getFinishedCount() + 1);
            }
        }
        List<StatRow> rows = new ArrayList<StatRow>(map.values());
        rows.sort(new java.util.Comparator<StatRow>() {
            @Override
            public int compare(StatRow a, StatRow b) {
                return (int) (b.getOrderCount() - a.getOrderCount());
            }
        });
        return rows;
    }

    /** 楼栋分布分析 */
    public List<StatRow> buildingAnalysis() {
        Map<String, StatRow> map = new LinkedHashMap<String, StatRow>();
        for (RepairOrder order : allOrders()) {
            String key = order.getBuilding() == null ? "未填写" : order.getBuilding();
            StatRow row = map.get(key);
            if (row == null) {
                row = new StatRow();
                row.setGroupName(key);
                map.put(key, row);
            }
            row.setOrderCount(row.getOrderCount() + 1);
            if (order.getStatus() != null && order.getStatus().intValue() == OrderStatus.FINISHED.getCode()) {
                row.setFinishedCount(row.getFinishedCount() + 1);
            }
        }
        List<StatRow> rows = new ArrayList<StatRow>(map.values());
        rows.sort(new java.util.Comparator<StatRow>() {
            @Override
            public int compare(StatRow a, StatRow b) {
                return (int) (b.getOrderCount() - a.getOrderCount());
            }
        });
        return rows;
    }

    /** 维修人员绩效统计 */
    public List<StatRow> workerPerformance() {
        List<StatRow> rows = new ArrayList<StatRow>();
        for (Worker worker : workerDao.findAll()) {
            StatRow row = new StatRow();
            row.setGroupName(worker.getName());
            long finished = 0;
            double totalHours = 0;
            int hoursCount = 0;
            for (RepairTask task : taskDao.findByWorker(worker.getWorkerId())) {
                if (task.getStatus() != null && task.getStatus().intValue() == TaskStatus.FINISHED.getCode()) {
                    finished++;
                }
                if (task.getAcceptTime() != null && task.getFinishTime() != null) {
                    totalHours += DateUtil.hoursBetween(task.getAcceptTime(), task.getFinishTime());
                    hoursCount++;
                }
            }
            long assigned = taskDao.findByWorker(worker.getWorkerId()).size();
            row.setOrderCount(assigned);
            row.setFinishedCount(finished);
            row.setProcessingCount(assigned - finished);
            row.setAvgHours(hoursCount == 0 ? 0d : round1(totalHours / hoursCount));
            row.setAvgScore(round1(evaluationDao.avgScoreByWorker(worker.getWorkerId())));
            row.setMaterialCost(materialCostOfWorker(worker.getWorkerId()));
            rows.add(row);
        }
        rows.sort(new java.util.Comparator<StatRow>() {
            @Override
            public int compare(StatRow a, StatRow b) {
                return (int) (b.getFinishedCount() - a.getFinishedCount());
            }
        });
        return rows;
    }

    /** 月度趋势（近 6 个月） */
    public List<StatRow> monthlyTrend() {
        Map<String, StatRow> map = new LinkedHashMap<String, StatRow>();
        for (RepairOrder order : allOrders()) {
            Date time = order.getCreateTime();
            if (time == null) {
                continue;
            }
            String key = DateUtil.monthLabel(time);
            StatRow row = map.get(key);
            if (row == null) {
                row = new StatRow();
                row.setGroupName(key);
                map.put(key, row);
            }
            row.setOrderCount(row.getOrderCount() + 1);
            if (order.getStatus() != null && order.getStatus().intValue() == OrderStatus.FINISHED.getCode()) {
                row.setFinishedCount(row.getFinishedCount() + 1);
            }
        }
        List<StatRow> rows = new ArrayList<StatRow>(map.values());
        rows.sort(new java.util.Comparator<StatRow>() {
            @Override
            public int compare(StatRow a, StatRow b) {
                return a.getGroupName().compareTo(b.getGroupName());
            }
        });
        return rows;
    }

    /** 耗材消耗排行 */
    public List<StatRow> materialRanking() {
        Map<Integer, StatRow> map = new LinkedHashMap<Integer, StatRow>();
        for (MaterialUsage usage : usageDao.findAll()) {
            StatRow row = map.get(usage.getMatId());
            if (row == null) {
                row = new StatRow();
                row.setGroupName(usage.getMatName() == null ? ("耗材#" + usage.getMatId()) : usage.getMatName());
                row.setMaterialCost(BigDecimal.ZERO);
                map.put(usage.getMatId(), row);
            }
            row.setOrderCount(row.getOrderCount() + (usage.getUseCount() == null ? 0 : usage.getUseCount()));
            row.setMaterialCost(row.getMaterialCost().add(
                    usage.getAmount() == null ? BigDecimal.ZERO : usage.getAmount()));
        }
        List<StatRow> rows = new ArrayList<StatRow>(map.values());
        rows.sort(new java.util.Comparator<StatRow>() {
            @Override
            public int compare(StatRow a, StatRow b) {
                return b.getMaterialCost().compareTo(a.getMaterialCost());
            }
        });
        return rows;
    }

    /** 耗材成本合计 */
    public BigDecimal materialCost() {
        BigDecimal total = BigDecimal.ZERO;
        for (MaterialUsage usage : usageDao.findAll()) {
            if (usage.getAmount() != null) {
                total = total.add(usage.getAmount());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** 平均处理时长（受理到完成登记，小时） */
    public double avgProcessHours() {
        double total = 0;
        int count = 0;
        for (RepairTask task : taskDao.findAll()) {
            if (task.getAcceptTime() != null && task.getFinishTime() != null) {
                total += DateUtil.hoursBetween(task.getAcceptTime(), task.getFinishTime());
                count++;
            }
        }
        return count == 0 ? 0d : round1(total / count);
    }

    /** 评价得分明细（用于评价统计页） */
    public List<Evaluation> evaluationList() {
        return evaluationDao.findAll();
    }

    /** 维修人员当前负载（派单参考） */
    public List<Worker> workerLoad() {
        return workerDao.findAll();
    }

    private BigDecimal materialCostOfWorker(Integer workerId) {
        BigDecimal total = BigDecimal.ZERO;
        for (RepairTask task : taskDao.findByWorker(workerId)) {
            for (MaterialUsage usage : usageDao.findByTask(task.getTaskId())) {
                if (usage.getAmount() != null) {
                    total = total.add(usage.getAmount());
                }
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private List<RepairOrder> allOrders() {
        com.campus.repair.domain.OrderQuery query = new com.campus.repair.domain.OrderQuery();
        query.setPageSize(100000);
        return orderDao.findByCondition(query);
    }

    private static double round1(double value) {
        return Math.round(value * 10d) / 10d;
    }
}
