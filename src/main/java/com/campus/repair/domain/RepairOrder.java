package com.campus.repair.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 报修单实体（对应表 2.11 t_repair_order）。
 */
public class RepairOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报修单ID，主键 */
    private Integer orderId;
    /** 报修人ID，外键 */
    private Integer userId;
    /** 楼栋 */
    private String building;
    /** 楼层 */
    private String floor;
    /** 房间号 */
    private String room;
    /** 报修类别（水电/木工/设备等） */
    private String category;
    /** 故障描述 */
    private String description;
    /** 现场照片URL（逗号分隔） */
    private String images;
    /** 状态（0待审核/1待派单/2已派单/3维修中/4待确认/5已完成/6已撤销/7已驳回） */
    private Integer status;
    /** 优先级（0普通/1紧急） */
    private Integer priority;
    /** 催办次数 */
    private Integer urgeCount;
    /** 驳回原因 */
    private String rejectReason;
    /** 提交时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

    // ------------------------- 关联展示字段（非表字段） -------------------------

    /** 报修人登录名 */
    private String reporterUsername;
    /** 报修人姓名 */
    private String reporterName;
    /** 报修人手机号 */
    private String reporterPhone;
    /** 当前任务ID */
    private Integer taskId;
    /** 当前维修人员ID */
    private Integer workerId;
    /** 当前维修人员姓名 */
    private String workerName;
    /** 当前维修人员手机号 */
    private String workerPhone;
    /** 当前任务状态 */
    private Integer taskStatus;
    /** 派单时间 */
    private Date dispatchTime;
    /** 接单时间 */
    private Date acceptTime;
    /** 完成登记时间 */
    private Date finishTime;
    /** 维修结果 */
    private String result;
    /** 备注/延期原因 */
    private String remark;
    /** 评价分数 */
    private Integer evalScore;
    /** 评价内容 */
    private String evalComment;
    /** 管理员回复 */
    private String evalReply;
    /** 进度反馈列表 */
    private List<Progress> progressList = new ArrayList<Progress>();

    /** 报修地点（拼接展示） */
    public String getLocation() {
        StringBuilder sb = new StringBuilder();
        if (building != null) {
            sb.append(building);
        }
        if (floor != null && !floor.isEmpty()) {
            sb.append(' ').append(floor).append("层");
        }
        if (room != null && !room.isEmpty()) {
            sb.append(' ').append(room);
        }
        return sb.toString();
    }

    /** 状态中文名 */
    public String getStatusText() {
        return OrderStatus.textOf(status);
    }

    /** 任务状态中文名 */
    public String getTaskStatusText() {
        return TaskStatus.textOf(taskStatus);
    }

    /** 优先级中文名 */
    public String getPriorityText() {
        return priority != null && priority.intValue() == 1 ? "紧急" : "普通";
    }

    /** 是否可撤销 */
    public boolean isCancelable() {
        OrderStatus status1 = OrderStatus.of(status);
        return status1 != null && status1.cancelable();
    }

    /** 是否可催办 */
    public boolean isUrgable() {
        OrderStatus status1 = OrderStatus.of(status);
        return status1 != null && status1.urgable();
    }

    /** 是否可评价（已完成且未评价） */
    public boolean isEvaluable() {
        return status != null && status.intValue() == OrderStatus.FINISHED.getCode() && evalScore == null;
    }

    /** 现场照片列表 */
    public List<String> getImageList() {
        List<String> list = new ArrayList<String>();
        if (images != null && !images.trim().isEmpty()) {
            for (String item : images.split(",")) {
                if (!item.trim().isEmpty()) {
                    list.add(item.trim());
                }
            }
        }
        return list;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
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

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getUrgeCount() {
        return urgeCount;
    }

    public void setUrgeCount(Integer urgeCount) {
        this.urgeCount = urgeCount;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getReporterUsername() {
        return reporterUsername;
    }

    public void setReporterUsername(String reporterUsername) {
        this.reporterUsername = reporterUsername;
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

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Integer workerId) {
        this.workerId = workerId;
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

    public Integer getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(Integer taskStatus) {
        this.taskStatus = taskStatus;
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

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
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

    public Integer getEvalScore() {
        return evalScore;
    }

    public void setEvalScore(Integer evalScore) {
        this.evalScore = evalScore;
    }

    public String getEvalComment() {
        return evalComment;
    }

    public void setEvalComment(String evalComment) {
        this.evalComment = evalComment;
    }

    public String getEvalReply() {
        return evalReply;
    }

    public void setEvalReply(String evalReply) {
        this.evalReply = evalReply;
    }

    public List<Progress> getProgressList() {
        return progressList;
    }

    public void setProgressList(List<Progress> progressList) {
        this.progressList = progressList;
    }

    @Override
    public String toString() {
        return "RepairOrder{orderId=" + orderId + ", status=" + status + ", location='" + getLocation() + "'}";
    }
}
