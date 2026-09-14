package com.campus.repair.dao.mybatis;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import com.campus.repair.dao.MaterialDao;
import com.campus.repair.dao.mapper.MaterialMapper;
import com.campus.repair.domain.Material;

/**
 * 耗材数据访问 MyBatis 实现（对应设计书 2.2.6 MaterialDao 与表 2.14）。
 * SQL 见 {@code resources/mapper/MaterialMapper.xml}。
 */
public class MyBatisMaterialDaoImpl extends MyBatisDaoSupport implements MaterialDao {

    private MaterialMapper mapper(SqlSession session) {
        return session.getMapper(MaterialMapper.class);
    }

    @Override
    public int insert(Material material) {
        if (material.getStock() == null) {
            material.setStock(Integer.valueOf(0));
        }
        return mutate(session -> mapper(session).insert(material));
    }

    @Override
    public int update(Material material) {
        return mutate(session -> mapper(session).update(material));
    }

    @Override
    public int delete(Integer matId) {
        return mutate(session -> mapper(session).delete(matId));
    }

    @Override
    public Material findById(Integer matId) {
        return query(session -> mapper(session).findById(matId));
    }

    @Override
    public Material findByNameAndSpec(String matName, String spec) {
        String target = spec == null || spec.trim().isEmpty() ? null : spec.trim();
        return query(session -> mapper(session).findByNameAndSpec(matName, target));
    }

    @Override
    public List<Material> findAll() {
        return query(session -> mapper(session).findAll());
    }

    @Override
    public List<Material> findLowStock(int threshold) {
        return query(session -> mapper(session).findLowStock(threshold));
    }

    /**
     * 扣减库存。
     * SQL 带 {@code AND stock >= #{useCount}} 条件，库存不足时返回 0 行，
     * 由 Service 层抛出"库存不足"业务异常（并发安全）。
     */
    @Override
    public int deductStock(Integer matId, int useCount) {
        return mutate(session -> mapper(session).deductStock(matId, useCount));
    }

    @Override
    public int addStock(Integer matId, int count) {
        return mutate(session -> mapper(session).addStock(matId, count));
    }

    /** 未使用：时间字段由 SQL 的 NOW() 维护 */
    protected Date now() {
        return new Date();
    }
}
