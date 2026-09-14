package com.campus.repair.dao.jdbc;

import java.util.List;

import com.campus.repair.dao.WorkerDao;
import com.campus.repair.domain.Worker;

/**
 * 维修人员数据访问 JDBC 实现（对应设计书 2.2.6 WorkerDao）。
 */
public class JdbcWorkerDaoImpl implements WorkerDao {

    private static final String COLUMNS =
            "worker_id, user_id, name, phone, skill_tags, current_orders, location, status";

    @Override
    public int insert(Worker worker) {
        String sql = "INSERT INTO t_worker (user_id, name, phone, skill_tags, current_orders, location, status)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        int id = JdbcTemplate.insertReturnKey(sql, worker.getUserId(), worker.getName(), worker.getPhone(),
                worker.getSkillTags(),
                worker.getCurrentOrders() == null ? Integer.valueOf(0) : worker.getCurrentOrders(),
                worker.getLocation(),
                worker.getStatus() == null ? Integer.valueOf(1) : worker.getStatus());
        worker.setWorkerId(Integer.valueOf(id));
        return id > 0 ? 1 : 0;
    }

    @Override
    public int update(Worker worker) {
        String sql = "UPDATE t_worker SET name = ?, phone = ?, skill_tags = ?, current_orders = ?, location = ?, status = ?"
                + " WHERE worker_id = ?";
        return JdbcTemplate.update(sql, worker.getName(), worker.getPhone(), worker.getSkillTags(),
                worker.getCurrentOrders(), worker.getLocation(), worker.getStatus(), worker.getWorkerId());
    }

    @Override
    public int updateStatus(Integer workerId, Integer status) {
        return JdbcTemplate.update("UPDATE t_worker SET status = ? WHERE worker_id = ?", status, workerId);
    }

    @Override
    public int updateLocation(Integer workerId, String location) {
        return JdbcTemplate.update("UPDATE t_worker SET location = ? WHERE worker_id = ?", location, workerId);
    }

    @Override
    public int addCurrentOrders(Integer workerId, int delta) {
        return JdbcTemplate.update(
                "UPDATE t_worker SET current_orders = GREATEST(COALESCE(current_orders, 0) + ?, 0) WHERE worker_id = ?",
                Integer.valueOf(delta), workerId);
    }

    @Override
    public int delete(Integer workerId) {
        return JdbcTemplate.update("DELETE FROM t_worker WHERE worker_id = ?", workerId);
    }

    @Override
    public Worker findById(Integer workerId) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + " FROM t_worker WHERE worker_id = ?", Worker.class, workerId);
    }

    @Override
    public Worker findByUserId(Integer userId) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + " FROM t_worker WHERE user_id = ?", Worker.class, userId);
    }

    @Override
    public List<Worker> findAll() {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_worker ORDER BY worker_id", Worker.class);
    }

    @Override
    public List<Worker> findAvailable() {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_worker WHERE status = 1"
                + " ORDER BY COALESCE(current_orders, 0) ASC, worker_id ASC", Worker.class);
    }

    @Override
    public List<Worker> findBySkill(String skill) {
        if (skill == null || skill.trim().isEmpty()) {
            return findAll();
        }
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_worker WHERE skill_tags LIKE ?"
                + " ORDER BY COALESCE(current_orders, 0) ASC", Worker.class, "%" + skill.trim() + "%");
    }
}
