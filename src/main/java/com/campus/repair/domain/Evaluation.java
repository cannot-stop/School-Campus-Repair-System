package com.campus.repair.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 评价实体（对应表 2.16 t_evaluation）。
 */
public class Evaluation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评价ID，主键 */
    private Integer evalId;
    /** 报修单ID，外键 */
    private Integer orderId;
    /** 评分（1～5分） */
    private Integer score;
    /** 文字评价 */
    private String comment;
    /** 管理员回复 */
    private String reply;
    /** 评价审核状态（0待审/1通过/2驳回） */
    private Integer auditStatus;
    /** 评价时间 */
    private Date createTime;

    // ------------------------- 关联展示字段（非表字段） -------------------------

    /** 报修单号 */
    private Integer orderNo;
    /** 报修人姓名 */
    private String reporterName;
    /** 维修人员姓名 */
    private String workerName;
    /** 报修类别 */
    private String category;

    /** 审核状态中文名 */
    public String getAuditStatusText() {
        return AuditStatus.textOf(auditStatus);
    }

    public Integer getEvalId() {
        return evalId;
    }

    public void setEvalId(Integer evalId) {
        this.evalId = evalId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public Integer getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(Integer auditStatus) {
        this.auditStatus = auditStatus;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "Evaluation{evalId=" + evalId + ", orderId=" + orderId + ", score=" + score + "}";
    }
}
