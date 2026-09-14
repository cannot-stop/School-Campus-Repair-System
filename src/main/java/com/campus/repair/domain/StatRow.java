package com.campus.repair.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 维修绩效统计行（对应设计书 2.2.1"评价与统计分析模块"之"维修统计、故障类型分析、绩效统计"）。
 */
public class StatRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分组名称（类别名 / 维修工姓名 / 月份） */
    private String groupName;
    /** 报修单数量 */
    private long orderCount;
    /** 已完成数量 */
    private long finishedCount;
    /** 进行中数量 */
    private long processingCount;
    /** 平均处理时长（小时） */
    private double avgHours;
    /** 平均评分 */
    private double avgScore;
    /** 耗材成本合计 */
    private BigDecimal materialCost = BigDecimal.ZERO;

    /** 完成率（百分比，保留一位小数） */
    public double getFinishRate() {
        if (orderCount == 0) {
            return 0d;
        }
        return Math.round(finishedCount * 1000d / orderCount) / 10d;
    }

    /** 平均评分的展示文本 */
    public String getAvgScoreText() {
        return avgScore <= 0 ? "—" : String.valueOf(Math.round(avgScore * 10) / 10d);
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public long getFinishedCount() {
        return finishedCount;
    }

    public void setFinishedCount(long finishedCount) {
        this.finishedCount = finishedCount;
    }

    public long getProcessingCount() {
        return processingCount;
    }

    public void setProcessingCount(long processingCount) {
        this.processingCount = processingCount;
    }

    public double getAvgHours() {
        return avgHours;
    }

    public void setAvgHours(double avgHours) {
        this.avgHours = avgHours;
    }

    public double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(double avgScore) {
        this.avgScore = avgScore;
    }

    public BigDecimal getMaterialCost() {
        return materialCost;
    }

    public void setMaterialCost(BigDecimal materialCost) {
        this.materialCost = materialCost;
    }

    @Override
    public String toString() {
        return "StatRow{group='" + groupName + "', orders=" + orderCount + ", finished=" + finishedCount + "}";
    }
}
