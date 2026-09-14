package com.campus.repair.dao.jdbc;

import java.util.List;

import com.campus.repair.dao.ProgressDao;
import com.campus.repair.domain.Progress;

/**
 * 维修进度反馈数据访问 JDBC 实现。
 */
public class JdbcProgressDaoImpl implements ProgressDao {

    private static final String COLUMNS = "progress_id, task_id, content, create_time";

    @Override
    public int insert(Progress progress) {
        String sql = "INSERT INTO t_progress (task_id, content, create_time) VALUES (?, ?, ?)";
        int id = JdbcTemplate.insertReturnKey(sql, progress.getTaskId(), progress.getContent(),
                new java.sql.Timestamp(progress.getCreateTime() == null
                        ? System.currentTimeMillis() : progress.getCreateTime().getTime()));
        progress.setProgressId(Integer.valueOf(id));
        return id > 0 ? 1 : 0;
    }

    @Override
    public int deleteByTask(Integer taskId) {
        return JdbcTemplate.update("DELETE FROM t_progress WHERE task_id = ?", taskId);
    }

    @Override
    public List<Progress> findByTask(Integer taskId) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_progress WHERE task_id = ? ORDER BY create_time",
                Progress.class, taskId);
    }

    @Override
    public List<Progress> findAll() {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_progress ORDER BY create_time DESC", Progress.class);
    }
}
