package com.campus.repair.dao.jdbc;

import java.util.List;

import com.campus.repair.dao.MaterialDao;
import com.campus.repair.domain.Material;

/**
 * 耗材数据访问 JDBC 实现（对应设计书 2.2.6 MaterialDao）。
 */
public class JdbcMaterialDaoImpl implements MaterialDao {

    private static final String COLUMNS = "mat_id, mat_name, spec, stock, unit_price";

    @Override
    public int insert(Material material) {
        String sql = "INSERT INTO t_material (mat_name, spec, stock, unit_price) VALUES (?, ?, ?, ?)";
        int id = JdbcTemplate.insertReturnKey(sql, material.getMatName(), material.getSpec(),
                material.getStock() == null ? Integer.valueOf(0) : material.getStock(), material.getUnitPrice());
        material.setMatId(Integer.valueOf(id));
        return id > 0 ? 1 : 0;
    }

    @Override
    public int update(Material material) {
        String sql = "UPDATE t_material SET mat_name = ?, spec = ?, stock = ?, unit_price = ? WHERE mat_id = ?";
        return JdbcTemplate.update(sql, material.getMatName(), material.getSpec(), material.getStock(),
                material.getUnitPrice(), material.getMatId());
    }

    @Override
    public int delete(Integer matId) {
        return JdbcTemplate.update("DELETE FROM t_material WHERE mat_id = ?", matId);
    }

    @Override
    public Material findById(Integer matId) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + " FROM t_material WHERE mat_id = ?", Material.class, matId);
    }

    @Override
    public Material findByNameAndSpec(String matName, String spec) {
        if (spec == null) {
            return JdbcTemplate.queryOne("SELECT " + COLUMNS + " FROM t_material WHERE mat_name = ? AND spec IS NULL",
                    Material.class, matName);
        }
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + " FROM t_material WHERE mat_name = ? AND spec = ?",
                Material.class, matName, spec);
    }

    @Override
    public List<Material> findAll() {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_material ORDER BY mat_id", Material.class);
    }

    @Override
    public List<Material> findLowStock(int threshold) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_material WHERE stock < ? ORDER BY stock ASC",
                Material.class, Integer.valueOf(threshold));
    }

    @Override
    public int deductStock(Integer matId, int useCount) {
        // 条件更新保证并发下不会出现负库存
        return JdbcTemplate.update("UPDATE t_material SET stock = stock - ? WHERE mat_id = ? AND stock >= ?",
                Integer.valueOf(useCount), matId, Integer.valueOf(useCount));
    }

    @Override
    public int addStock(Integer matId, int count) {
        return JdbcTemplate.update("UPDATE t_material SET stock = stock + ? WHERE mat_id = ?",
                Integer.valueOf(count), matId);
    }
}
