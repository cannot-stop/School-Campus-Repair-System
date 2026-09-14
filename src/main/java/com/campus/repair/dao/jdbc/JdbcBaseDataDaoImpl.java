package com.campus.repair.dao.jdbc;

import java.util.ArrayList;
import java.util.List;

import com.campus.repair.dao.BaseDataDao;

/**
 * 基础数据数据访问 JDBC 实现。
 */
public class JdbcBaseDataDaoImpl implements BaseDataDao {

    @Override
    public int insert(String dataType, String dataValue, int sortNo) {
        if (exists(dataType, dataValue)) {
            return 0;
        }
        return JdbcTemplate.update("INSERT INTO t_base_data (data_type, data_value, sort_no) VALUES (?, ?, ?)",
                dataType, dataValue, Integer.valueOf(sortNo));
    }

    @Override
    public int delete(String dataType, String dataValue) {
        return JdbcTemplate.update("DELETE FROM t_base_data WHERE data_type = ? AND data_value = ?", dataType, dataValue);
    }

    @Override
    public List<String> findValues(String dataType) {
        List<java.util.Map<String, Object>> rows = JdbcTemplate.queryRows(
                "SELECT data_value FROM t_base_data WHERE data_type = ? ORDER BY sort_no, data_id", dataType);
        List<String> values = new ArrayList<String>();
        for (java.util.Map<String, Object> row : rows) {
            Object value = row.get("data_value");
            if (value != null) {
                values.add(value.toString());
            }
        }
        return values;
    }

    @Override
    public boolean exists(String dataType, String dataValue) {
        return JdbcTemplate.queryCount("SELECT COUNT(*) FROM t_base_data WHERE data_type = ? AND data_value = ?",
                dataType, dataValue) > 0;
    }
}
