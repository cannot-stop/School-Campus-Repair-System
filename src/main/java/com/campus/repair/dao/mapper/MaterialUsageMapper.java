package com.campus.repair.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.campus.repair.domain.MaterialUsage;

/**
 * 耗材使用 Mapper 接口（对应设计书 2.2.6 MaterialUsageDao 与表 2.15）。
 *
 * <p>查询语句通过 LEFT JOIN 带出耗材名称、规格与单价，供成本统计使用。</p>
 */
public interface MaterialUsageMapper {

    int insert(MaterialUsage usage);

    int updateCount(@Param("useId") Integer useId, @Param("useCount") int useCount);

    int delete(@Param("useId") Integer useId);

    int deleteByTask(@Param("taskId") Integer taskId);

    MaterialUsage findById(@Param("useId") Integer useId);

    MaterialUsage findByTaskAndMaterial(@Param("taskId") Integer taskId, @Param("matId") Integer matId);

    List<MaterialUsage> findByTask(@Param("taskId") Integer taskId);

    List<MaterialUsage> findAll();
}
