package com.campus.repair.domain;

/**
 * 维修任务状态（对应 t_repair_task.status）。
 * 0待接单/1维修中/2待确认/3已完成/4转单/5退单/6已退回派单池。
 */
public enum TaskStatus {

    PENDING_ACCEPT(0, "待接单"),
    REPAIRING(1, "维修中"),
    PENDING_CONFIRM(2, "待确认"),
    FINISHED(3, "已完成"),
    TRANSFERRED(4, "已转单"),
    REFUSED(5, "已退单"),
    RETURNED(6, "已退回派单池");

    private final int code;
    private final String text;

    TaskStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public static TaskStatus of(Integer code) {
        if (code != null) {
            for (TaskStatus value : values()) {
                if (value.code == code.intValue()) {
                    return value;
                }
            }
        }
        return null;
    }

    public static String textOf(Integer code) {
        TaskStatus status = of(code);
        return status == null ? "未知" : status.text;
    }

    /** 任务是否处于进行中（占用维修工在单量） */
    public boolean active() {
        return this == PENDING_ACCEPT || this == REPAIRING || this == PENDING_CONFIRM;
    }
}
