package com.campus.repair.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 维修进度反馈实体（对应 t_progress，承载设计书 1.1.2.4"进度反馈"与延期原因记录）。
 */
public class Progress implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 进度ID，主键 */
    private Integer progressId;
    /** 维修任务ID，外键 */
    private Integer taskId;
    /** 进度说明（延期原因必填） */
    private String content;
    /** 反馈时间 */
    private Date createTime;

    public Integer getProgressId() {
        return progressId;
    }

    public void setProgressId(Integer progressId) {
        this.progressId = progressId;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "Progress{taskId=" + taskId + ", content='" + content + "'}";
    }
}
