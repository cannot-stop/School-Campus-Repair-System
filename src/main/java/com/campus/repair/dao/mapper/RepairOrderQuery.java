package com.campus.repair.dao.mapper;

import java.io.Serializable;
import java.util.Date;

/**
 * 报修单分页查询参数（MyBatis Mapper 入参对象）。
 *
 * <p>对应设计书 2.2.3.2 的"按时间、状态、类别等条件查询"，
 * 字段名与 {@code OrderQuery} 保持一致，由 DaoImpl 转换后传入 Mapper。</p>
 */
public class RepairOrderQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报修单ID */
    private Integer orderId;
    /** 报修人ID（报修人仅能查询本人报修单） */
    private Integer userId;
    /** 状态 */
    private Integer status;
    /** 类别 */
    private String category;
    /** 楼栋 */
    private String building;
    /** 优先级 */
    private Integer priority;
    /** 关键字（描述 / 房间号 / 楼栋 模糊匹配） */
    private String keyword;
    /** 提交时间起 */
    private Date beginTime;
    /** 提交时间止 */
    private Date endTime;
    /** 维修人员ID（按当前任务关联过滤） */
    private Integer workerId;
    /** 每页条数（LIMIT） */
    private Integer limit;
    /** 偏移量（OFFSET） */
    private Integer offset;

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

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }
}
