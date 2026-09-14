package com.campus.repair.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.ResultCode;
import com.campus.repair.common.Validate;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.MaterialDao;
import com.campus.repair.domain.Material;

/**
 * 耗材服务（对应设计书 2.2.6 中 RepairService.finishRepair() 调用的耗材扣减能力）。
 *
 * <p>职责：耗材库存维护、耗材扣减（含库存不足校验与事务一致性）、补库、低价库存预警。</p>
 */
public class MaterialService {

    /** 库存预警阈值 */
    private static final int LOW_STOCK_THRESHOLD = 10;

    private final MaterialDao materialDao = DaoFactory.materialDao();

    /** 扣减结果：记录本次实际扣减的耗材与数量，供事务回滚时恢复库存 */
    public static class DeductResult {
        private final List<Material> items = new ArrayList<Material>();
        private final List<Integer> counts = new ArrayList<Integer>();

        public List<Material> getItems() {
            return items;
        }

        public List<Integer> getCounts() {
            return counts;
        }

        void add(Material material, int count) {
            items.add(material);
            counts.add(Integer.valueOf(count));
        }

        /** 总金额 */
        public BigDecimal totalAmount() {
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < items.size(); i++) {
                Material material = items.get(i);
                BigDecimal price = material.getUnitPrice() == null ? BigDecimal.ZERO : material.getUnitPrice();
                total = total.add(price.multiply(new BigDecimal(counts.get(i).intValue())));
            }
            return total;
        }
    }

    /** 耗材清单 */
    public List<Material> listAll() {
        return materialDao.findAll();
    }

    /** 库存预警清单 */
    public List<Material> listLowStock() {
        return materialDao.findLowStock(LOW_STOCK_THRESHOLD);
    }

    /** 新增/修改耗材（维修管理员维护） */
    public Material save(String matId, String matName, String spec, String stock, String unitPrice) {
        Validate.create()
                .required("matName", "耗材名称", matName)
                .required("stock", "库存数量", stock)
                .required("unitPrice", "单价", unitPrice)
                .throwIfInvalid();
        Material material;
        boolean create = true;
        Integer id = parseInt(matId);
        if (id == null) {
            Material exists = materialDao.findByNameAndSpec(matName.trim(), Validate.trim(spec));
            if (exists != null) {
                throw new BusinessException("同名同规格耗材已存在");
            }
            material = new Material();
        } else {
            material = materialDao.findById(id);
            if (material == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "耗材不存在");
            }
            create = false;
        }
        try {
            int stockValue = Integer.parseInt(stock.trim());
            if (stockValue < 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "库存数量不能为负数");
            }
            material.setStock(Integer.valueOf(stockValue));
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "库存数量必须为整数");
        }
        try {
            BigDecimal price = new BigDecimal(unitPrice.trim());
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "单价不能为负数");
            }
            material.setUnitPrice(price);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "单价必须为数字");
        }
        material.setMatName(matName.trim());
        material.setSpec(Validate.trim(spec));
        if (create) {
            materialDao.insert(material);
        } else {
            materialDao.update(material);
        }
        return materialDao.findById(material.getMatId());
    }

    /** 删除耗材 */
    public void delete(String matId) {
        Integer id = parseInt(matId);
        if (id == null || materialDao.findById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "耗材不存在");
        }
        materialDao.delete(id);
    }

    /** 补库 */
    public Material replenish(Integer matId, int count) {
        if (count <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "补库数量必须大于 0");
        }
        if (materialDao.addStock(matId, count) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "耗材不存在");
        }
        return materialDao.findById(matId);
    }

    /**
     * 批量扣减库存（设计书 1.1.2.4 备选事件流："耗材库存不足：系统提示库存不足，允许先登记后补库"）。
     *
     * <p>任一条耗材库存不足即整体失败，由调用方在事务中回滚。</p>
     *
     * @param items 形如 [{matId:1, useCount:2}, ...]
     */
    public DeductResult deduct(List<MaterialItem> items) {
        DeductResult result = new DeductResult();
        if (items == null || items.isEmpty()) {
            return result;
        }
        for (MaterialItem item : items) {
            if (item.getMatId() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "耗材ID不能为空");
            }
            if (item.getUseCount() <= 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "耗材使用数量必须大于 0");
            }
            Material material = materialDao.findById(item.getMatId());
            if (material == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "耗材不存在（ID：" + item.getMatId() + "）");
            }
            int stock = material.getStock() == null ? 0 : material.getStock().intValue();
            if (stock < item.getUseCount()) {
                throw new BusinessException("耗材「" + material.getMatName() + "」库存不足，当前库存 "
                        + stock + "，本次需使用 " + item.getUseCount() + "，请先补库");
            }
            if (materialDao.deductStock(item.getMatId(), item.getUseCount()) == 0) {
                throw new BusinessException("耗材「" + material.getMatName() + "」扣减失败，请重试");
            }
            result.add(material, item.getUseCount());
        }
        return result;
    }

    /** 回滚已扣减的库存（结果确认不通过、退单等场景） */
    public void restore(DeductResult result) {
        if (result == null) {
            return;
        }
        for (int i = 0; i < result.getItems().size(); i++) {
            materialDao.addStock(result.getItems().get(i).getMatId(), result.getCounts().get(i).intValue());
        }
    }

    /** 回滚指定任务的耗材占用（按使用记录恢复库存并删除记录） */
    public void restoreByTask(Integer taskId) {
        List<com.campus.repair.domain.MaterialUsage> usages = DaoFactory.materialUsageDao().findByTask(taskId);
        for (com.campus.repair.domain.MaterialUsage usage : usages) {
            materialDao.addStock(usage.getMatId(), usage.getUseCount() == null ? 0 : usage.getUseCount().intValue());
        }
        DaoFactory.materialUsageDao().deleteByTask(taskId);
    }

    /** 耗材使用项 */
    public static class MaterialItem {
        private Integer matId;
        private int useCount;

        public MaterialItem() {
        }

        public MaterialItem(Integer matId, int useCount) {
            this.matId = matId;
            this.useCount = useCount;
        }

        public Integer getMatId() {
            return matId;
        }

        public void setMatId(Integer matId) {
            this.matId = matId;
        }

        public int getUseCount() {
            return useCount;
        }

        public void setUseCount(int useCount) {
            this.useCount = useCount;
        }
    }

    private Integer parseInt(String value) {
        if (Validate.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
