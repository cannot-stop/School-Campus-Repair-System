package com.campus.repair.dao;

import java.util.List;

import com.campus.repair.domain.MaterialUsage;

/**
 * 耗材使用数据访问接口（对应设计书 2.2.6 MaterialUsageDao）。
 */
public interface MaterialUsageDao {

    int insert(MaterialUsage usage);

    /** 同一任务同一耗材重复登记时累加数量 */
    int updateCount(Integer useId, int useCount);

    int delete(Integer useId);

    int deleteByTask(Integer taskId);

    MaterialUsage findById(Integer useId);

    MaterialUsage findByTaskAndMaterial(Integer taskId, Integer matId);

    List<MaterialUsage> findByTask(Integer taskId);

    List<MaterialUsage> findAll();
}
