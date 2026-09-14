package com.campus.repair.dao.jdbc;

import java.util.List;

import com.campus.repair.dao.MaterialUsageDao;
import com.campus.repair.domain.MaterialUsage;

/**
 * 耗材使用数据访问 JDBC 实现（对应设计书 2.2.6 MaterialUsageDao）。
 */
public class JdbcMaterialUsageDaoImpl implements MaterialUsageDao {

    private static final String COLUMNS =
            "u.use_id, u.task_id, u.mat_id, u.use_count, u.use_time,"
                    + " m.mat_name, m.spec, m.unit_price";

    private static final String FROM = " FROM t_material_usage u LEFT JOIN t_material m ON m.mat_id = u.mat_id";

    @Override
    public int insert(MaterialUsage usage) {
        String sql = "INSERT INTO t_material_usage (task_id, mat_id, use_count, use_time) VALUES (?, ?, ?, ?)";
        int id = JdbcTemplate.insertReturnKey(sql, usage.getTaskId(), usage.getMatId(), usage.getUseCount(),
                new java.sql.Timestamp(usage.getUseTime() == null
                        ? System.currentTimeMillis() : usage.getUseTime().getTime()));
        usage.setUseId(Integer.valueOf(id));
        return id > 0 ? 1 : 0;
    }

    @Override
    public int updateCount(Integer useId, int useCount) {
        return JdbcTemplate.update("UPDATE t_material_usage SET use_count = ? WHERE use_id = ?",
                Integer.valueOf(useCount), useId);
    }

    @Override
    public int delete(Integer useId) {
        return JdbcTemplate.update("DELETE FROM t_material_usage WHERE use_id = ?", useId);
    }

    @Override
    public int deleteByTask(Integer taskId) {
        return JdbcTemplate.update("DELETE FROM t_material_usage WHERE task_id = ?", taskId);
    }

    @Override
    public MaterialUsage findById(Integer useId) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + FROM + " WHERE u.use_id = ?",
                MaterialUsage.class, useId);
    }

    @Override
    public MaterialUsage findByTaskAndMaterial(Integer taskId, Integer matId) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + FROM + " WHERE u.task_id = ? AND u.mat_id = ?",
                MaterialUsage.class, taskId, matId);
    }

    @Override
    public List<MaterialUsage> findByTask(Integer taskId) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + FROM + " WHERE u.task_id = ? ORDER BY u.use_id",
                MaterialUsage.class, taskId);
    }

    @Override
    public List<MaterialUsage> findAll() {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + FROM + " ORDER BY u.use_id", MaterialUsage.class);
    }
}
