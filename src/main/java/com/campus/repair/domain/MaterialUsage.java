package com.campus.repair.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 耗材使用实体（对应表 2.15 t_material_usage）。
 */
public class MaterialUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 使用记录ID，主键 */
    private Integer useId;
    /** 维修任务ID，外键 */
    private Integer taskId;
    /** 耗材ID，外键 */
    private Integer matId;
    /** 使用数量 */
    private Integer useCount;
    /** 使用时间 */
    private Date useTime;

    // ------------------------- 关联展示字段（非表字段） -------------------------

    /** 耗材名称 */
    private String matName;
    /** 规格型号 */
    private String spec;
    /** 单价 */
    private BigDecimal unitPrice;

    /** 小计金额（对应统计模块的耗材成本统计） */
    public BigDecimal getAmount() {
        if (unitPrice == null || useCount == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(new BigDecimal(useCount));
    }

    public Integer getUseId() {
        return useId;
    }

    public void setUseId(Integer useId) {
        this.useId = useId;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getMatId() {
        return matId;
    }

    public void setMatId(Integer matId) {
        this.matId = matId;
    }

    public Integer getUseCount() {
        return useCount;
    }

    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
    }

    public Date getUseTime() {
        return useTime;
    }

    public void setUseTime(Date useTime) {
        this.useTime = useTime;
    }

    public String getMatName() {
        return matName;
    }

    public void setMatName(String matName) {
        this.matName = matName;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    @Override
    public String toString() {
        return "MaterialUsage{taskId=" + taskId + ", matId=" + matId + ", useCount=" + useCount + "}";
    }
}
