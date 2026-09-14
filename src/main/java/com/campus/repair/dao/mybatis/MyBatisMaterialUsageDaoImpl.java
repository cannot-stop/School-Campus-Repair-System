package com.campus.repair.dao.mybatis;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import com.campus.repair.dao.MaterialUsageDao;
import com.campus.repair.dao.mapper.MaterialUsageMapper;
import com.campus.repair.domain.MaterialUsage;

/**
 * 耗材使用数据访问 MyBatis 实现（对应设计书 2.2.6 MaterialUsageDao 与表 2.15）。
 * SQL 见 {@code resources/mapper/MaterialUsageMapper.xml}，查询带出耗材名称与单价。
 */
public class MyBatisMaterialUsageDaoImpl extends MyBatisDaoSupport implements MaterialUsageDao {

    private MaterialUsageMapper mapper(SqlSession session) {
        return session.getMapper(MaterialUsageMapper.class);
    }

    @Override
    public int insert(MaterialUsage usage) {
        if (usage.getUseTime() == null) {
            usage.setUseTime(new Date());
        }
        return mutate(session -> mapper(session).insert(usage));
    }

    @Override
    public int updateCount(Integer useId, int useCount) {
        return mutate(session -> mapper(session).updateCount(useId, useCount));
    }

    @Override
    public int delete(Integer useId) {
        return mutate(session -> mapper(session).delete(useId));
    }

    @Override
    public int deleteByTask(Integer taskId) {
        return mutate(session -> mapper(session).deleteByTask(taskId));
    }

    @Override
    public MaterialUsage findById(Integer useId) {
        return query(session -> mapper(session).findById(useId));
    }

    @Override
    public MaterialUsage findByTaskAndMaterial(Integer taskId, Integer matId) {
        return query(session -> mapper(session).findByTaskAndMaterial(taskId, matId));
    }

    @Override
    public List<MaterialUsage> findByTask(Integer taskId) {
        return query(session -> mapper(session).findByTask(taskId));
    }

    @Override
    public List<MaterialUsage> findAll() {
        return query(session -> mapper(session).findAll());
    }
}
