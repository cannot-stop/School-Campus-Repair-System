package com.campus.repair.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.campus.repair.domain.Material;

/**
 * 耗材 Mapper 接口（对应设计书 2.2.6 MaterialDao 与表 2.14）。
 */
public interface MaterialMapper {

    int insert(Material material);

    int update(Material material);

    int delete(@Param("matId") Integer matId);

    Material findById(@Param("matId") Integer matId);

    Material findByNameAndSpec(@Param("matName") String matName, @Param("spec") String spec);

    List<Material> findAll();

    List<Material> findLowStock(@Param("threshold") int threshold);

    /** 扣减库存：SQL 带 stock >= useCount 条件，并发下不会出现负库存 */
    int deductStock(@Param("matId") Integer matId, @Param("useCount") int useCount);

    int addStock(@Param("matId") Integer matId, @Param("count") int count);
}
