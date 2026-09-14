package com.campus.repair.domain;

/**
 * 报修单状态（对应 t_repair_order.status）。
 *
 * <p>取值与设计书表 2.11 一致：0待审核/1待派单/2已派单/3维修中/4待确认/5已完成/6已撤销；
 * 补充 7已驳回，用于承载设计书 1.1.2.3"不受理的报修单需填写退回原因并通知报修人"的规则。</p>
 */
public enum OrderStatus {

    PENDING_AUDIT(0, "待审核"),
    PENDING_DISPATCH(1, "待派单"),
    DISPATCHED(2, "已派单"),
    REPAIRING(3, "维修中"),
    PENDING_CONFIRM(4, "待确认"),
    FINISHED(5, "已完成"),
    CANCELED(6, "已撤销"),
    REJECTED(7, "已驳回");

    private final int code;
    private final String text;

    OrderStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public static OrderStatus of(Integer code) {
        if (code != null) {
            for (OrderStatus value : values()) {
                if (value.code == code.intValue()) {
                    return value;
                }
            }
        }
        return null;
    }

    public static String textOf(Integer code) {
        OrderStatus status = of(code);
        return status == null ? "未知" : status.text;
    }

    /** 报修人是否可撤销（仅待审核、待派单可撤销） */
    public boolean cancelable() {
        return this == PENDING_AUDIT || this == PENDING_DISPATCH;
    }

    /** 报修人是否可催办（审核通过且未结束） */
    public boolean urgable() {
        return this == PENDING_DISPATCH || this == DISPATCHED || this == REPAIRING || this == PENDING_CONFIRM;
    }
}
