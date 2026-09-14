package com.campus.repair.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 报修单查询条件（对应设计书 1.1.2.2"按时间、状态、类别等条件查询"）。
 */
public class OrderQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报修单ID */
    private Integer orderId;
    /** 报修人ID（报修人只能查询本人报修单） */
    private Integer userId;
    /** 状态 */
    private Integer status;
    /** 类别 */
    private String category;
    /** 楼栋 */
    private String building;
    /** 优先级 */
    private Integer priority;
    /** 关键字（故障描述/房间号模糊匹配） */
    private String keyword;
    /** 提交时间起 */
    private Date beginTime;
    /** 提交时间止 */
    private Date endTime;
    /** 维修人员ID（用于查询该维修工的任务，按任务关联过滤） */
    private Integer workerId;
    /** 页码，从 1 开始 */
    private int pageNum = 1;
    /** 每页条数 */
    private int pageSize = 10;

    public int getOffset() {
        return (Math.max(pageNum, 1) - 1) * Math.max(pageSize, 1);
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Date getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Date beginTime) {
        this.beginTime = beginTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Integer workerId) {
        this.workerId = workerId;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
