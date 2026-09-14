package com.campus.repair.domain;

import java.io.Serializable;

/**
 * 维修人员实体（对应表 2.13 t_worker）。
 */
public class Worker implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 维修人员ID，主键 */
    private Integer workerId;
    /** 关联用户ID，外键 */
    private Integer userId;
    /** 姓名 */
    private String name;
    /** 手机号 */
    private String phone;
    /** 技能标签（如水电,木工,设备） */
    private String skillTags;
    /** 当前在单量 */
    private Integer currentOrders;
    /** 当前位置信息 */
    private String location;
    /** 状态（0离线/1在线） */
    private Integer status;

    public Integer getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Integer workerId) {
        this.workerId = workerId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSkillTags() {
        return skillTags;
    }

    public void setSkillTags(String skillTags) {
        this.skillTags = skillTags;
    }

    public Integer getCurrentOrders() {
        return currentOrders;
    }

    public void setCurrentOrders(Integer currentOrders) {
        this.currentOrders = currentOrders;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    /** 状态中文名 */
    public String getStatusText() {
        return status != null && status.intValue() == 1 ? "在线" : "离线";
    }

    /** 是否在线 */
    public boolean isOnline() {
        return status != null && status.intValue() == 1;
    }

    /** 在单量（空值按 0 处理） */
    public int currentOrderCount() {
        return currentOrders == null ? 0 : currentOrders.intValue();
    }

    @Override
    public String toString() {
        return "Worker{workerId=" + workerId + ", name='" + name + "', skills='" + skillTags + "'}";
    }
}
