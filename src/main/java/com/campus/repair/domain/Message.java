package com.campus.repair.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息通知实体（对应表 2.17 t_message）。
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息ID，主键 */
    private Integer msgId;
    /** 接收用户ID，外键 */
    private Integer userId;
    /** 消息内容 */
    private String content;
    /** 消息类型（报修/派单/催办/审核等） */
    private String msgType;
    /** 是否已读（0未读/1已读） */
    private Integer isRead;
    /** 推送时间 */
    private Date createTime;

    /** 是否已读 */
    public boolean isReadFlag() {
        return isRead != null && isRead.intValue() == 1;
    }

    public Integer getMsgId() {
        return msgId;
    }

    public void setMsgId(Integer msgId) {
        this.msgId = msgId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public Integer getIsRead() {
        return isRead;
    }

    public void setIsRead(Integer isRead) {
        this.isRead = isRead;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "Message{msgId=" + msgId + ", userId=" + userId + ", type='" + msgType + "'}";
    }
}
