package com.campus.repair.dao;

import java.util.List;

import com.campus.repair.domain.Material;

/**
 * 耗材数据访问接口（对应设计书 2.2.6 MaterialDao）。
 */
public interface MaterialDao {

    int insert(Material material);

    int update(Material material);

    int delete(Integer matId);

    Material findById(Integer matId);

    Material findByNameAndSpec(String matName, String spec);

    List<Material> findAll();

    /** 库存不足（低于阈值）的耗材 */
    List<Material> findLowStock(int threshold);

    /**
     * 扣减库存（库存不足时返回 0，由 Service 抛出业务异常）。
     * 通过 SQL 条件更新保证并发安全：stock >= useCount 才执行扣减。
     */
    int deductStock(Integer matId, int useCount);

    /** 补库 */
    int addStock(Integer matId, int count);
}
