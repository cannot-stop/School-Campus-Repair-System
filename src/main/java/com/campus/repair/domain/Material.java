package com.campus.repair.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 耗材实体（对应表 2.14 t_material）。
 */
public class Material implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 耗材ID，主键 */
    private Integer matId;
    /** 耗材名称 */
    private String matName;
    /** 规格型号 */
    private String spec;
    /** 库存数量 */
    private Integer stock;
    /** 单价 */
    private BigDecimal unitPrice;

    /** 是否库存不足（低于安全阈值 5） */
    public boolean isLowStock() {
        return stock == null || stock.intValue() < 5;
    }

    public Integer getMatId() {
        return matId;
    }

    public void setMatId(Integer matId) {
        this.matId = matId;
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

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /** 展示名（名称 + 规格） */
    public String getDisplayName() {
        return spec == null || spec.trim().isEmpty() ? matName : matName + " / " + spec;
    }

    @Override
    public String toString() {
        return "Material{matId=" + matId + ", matName='" + matName + "', stock=" + stock + "}";
    }
}
