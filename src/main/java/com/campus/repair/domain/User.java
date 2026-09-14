package com.campus.repair.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户实体（对应表 2.10 t_user）。
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID，主键 */
    private Integer userId;
    /** 用户名 */
    private String username;
    /** 密码（加盐加密存储） */
    private String password;
    /** 真实姓名 */
    private String realName;
    /** 学号/工号 */
    private String studentNo;
    /** 手机号 */
    private String phone;
    /** 角色（reporter/worker/manager/admin） */
    private String role;
    /** 审核状态（0待审/1通过/2驳回） */
    private Integer auditStatus;
    /** 是否锁定（0否/1是） */
    private Integer isLocked;
    /** 注册时间 */
    private Date createTime;

    // ------------------------- 展示字段（非表字段） -------------------------

    /** 角色中文名 */
    public String getRoleText() {
        return Role.textOf(role);
    }

    /** 审核状态中文名 */
    public String getAuditStatusText() {
        return AuditStatus.textOf(auditStatus);
    }

    /** 是否审核通过 */
    public boolean isAudited() {
        return auditStatus != null && auditStatus.intValue() == AuditStatus.PASSED.getCode();
    }

    /** 是否锁定 */
    public boolean isLockedFlag() {
        return isLocked != null && isLocked.intValue() == 1;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(Integer auditStatus) {
        this.auditStatus = auditStatus;
    }

    public Integer getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Integer isLocked) {
        this.isLocked = isLocked;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "User{userId=" + userId + ", username='" + username + "', role='" + role + "'}";
    }
}
