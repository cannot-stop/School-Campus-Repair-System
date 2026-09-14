package com.campus.repair.domain;

/**
 * 消息类型（对应 t_message.msg_type）。
 */
public final class MessageType {

    /** 提交报修（通知维修管理员待审核） */
    public static final String REPORT = "报修";
    /** 派单（通知维修人员新任务） */
    public static final String DISPATCH = "派单";
    /** 催办 */
    public static final String URGE = "催办";
    /** 审核结果 */
    public static final String AUDIT = "审核";
    /** 维修进度/完成 */
    public static final String REPAIR = "维修";
    /** 结果确认 */
    public static final String CONFIRM = "确认";
    /** 评价相关 */
    public static final String EVALUATION = "评价";
    /** 系统通知 */
    public static final String SYSTEM = "系统";

    private MessageType() {
    }
}
