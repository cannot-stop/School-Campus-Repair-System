package com.campus.repair.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 维修任务实体（对应表 2.12 t_repair_task）。
 */
public class RepairTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID，主键 */
    private Integer taskId;
    /** 报修单ID，外键 */
    private Integer orderId;
    /** 维修人员ID，外键 */
    private Integer workerId;
    /** 派单时间 */
    private Date dispatchTime;
    /** 接单时间 */
    private Date acceptTime;
    /** 开始维修时间 */
    private Date startTime;
    /** 完成登记时间 */
    private Date finishTime;
    /** 状态（0待接单/1维修中/2待确认/3已完成/4转单/5退单/6退回派单池） */
    private Integer status;
    /** 维修结果 */
    private String result;
    /** 备注/延期原因 */
    private String remark;

    // ------------------------- 关联展示字段（非表字段） -------------------------

    /** 维修人员姓名 */
    private String workerName;
    /** 维修人员手机号 */
    private String workerPhone;
    /** 报修单号 */
    private Integer orderNo;
    /** 报修地点 */
    private String location;
    /** 报修类别 */
    private String category;
    /** 故障描述 */
    private String description;
    /** 报修人姓名 */
    private String reporterName;
    /** 报修人手机号 */
    private String reporterPhone;
    /** 优先级 */
    private Integer priority;

    /** 状态中文名 */
    public String getStatusText() {
        return TaskStatus.textOf(status);
    }

    /** 是否已超过接单时限（派单后 2 小时未接单） */
    public boolean isAcceptOverdue(long deadlineMillis) {
        return (status == null || status.intValue() == TaskStatus.PENDING_ACCEPT.getCode())
                && dispatchTime != null
                && System.currentTimeMillis() - dispatchTime.getTime() > deadlineMillis;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Integer workerId) {
        this.workerId = workerId;
    }

    public Date getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(Date dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public Date getAcceptTime() {
        return acceptTime;
    }

    public void setAcceptTime(Date acceptTime) {
        this.acceptTime = acceptTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public String getWorkerPhone() {
        return workerPhone;
    }

    public void setWorkerPhone(String workerPhone) {
        this.workerPhone = workerPhone;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getReporterPhone() {
        return reporterPhone;
    }

    public void setReporterPhone(String reporterPhone) {
        this.reporterPhone = reporterPhone;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "RepairTask{taskId=" + taskId + ", orderId=" + orderId + ", workerId=" + workerId
                + ", status=" + status + "}";
    }
}
