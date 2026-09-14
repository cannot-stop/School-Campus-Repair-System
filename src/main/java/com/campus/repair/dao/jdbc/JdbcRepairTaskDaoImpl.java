package com.campus.repair.dao.jdbc;

import java.util.Date;
import java.util.List;

import com.campus.repair.dao.RepairTaskDao;
import com.campus.repair.domain.RepairTask;

/**
 * 维修任务数据访问 JDBC 实现（对应设计书 2.2.6 RepairTaskDao）。
 */
public class JdbcRepairTaskDaoImpl implements RepairTaskDao {

    private static final String COLUMNS =
            "t.task_id, t.order_id, t.worker_id, t.dispatch_time, t.accept_time, t.start_time, t.finish_time,"
                    + " t.status, t.result, t.remark,"
                    + " w.name AS worker_name, w.phone AS worker_phone,"
                    + " o.order_id AS order_no, o.category, o.description, o.priority, o.building, o.floor, o.room,"
                    + " u.real_name AS reporter_name, u.phone AS reporter_phone";

    private static final String FROM =
            " FROM t_repair_task t"
                    + " LEFT JOIN t_worker w ON w.worker_id = t.worker_id"
                    + " LEFT JOIN t_repair_order o ON o.order_id = t.order_id"
                    + " LEFT JOIN t_user u ON u.user_id = o.user_id";

    @Override
    public int insert(RepairTask task) {
        String sql = "INSERT INTO t_repair_task (order_id, worker_id, dispatch_time, accept_time, start_time,"
                + " finish_time, status, result, remark) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int id = JdbcTemplate.insertReturnKey(sql, task.getOrderId(), task.getWorkerId(),
                stamp(task.getDispatchTime(), true), stamp(task.getAcceptTime(), false),
                stamp(task.getStartTime(), false), stamp(task.getFinishTime(), false),
                task.getStatus() == null ? Integer.valueOf(0) : task.getStatus(),
                task.getResult(), task.getRemark());
        task.setTaskId(Integer.valueOf(id));
        return id > 0 ? 1 : 0;
    }

    @Override
    public int update(RepairTask task) {
        String sql = "UPDATE t_repair_task SET worker_id = ?, accept_time = ?, start_time = ?, finish_time = ?,"
                + " status = ?, result = ?, remark = ? WHERE task_id = ?";
        return JdbcTemplate.update(sql, task.getWorkerId(), stamp(task.getAcceptTime(), false),
                stamp(task.getStartTime(), false), stamp(task.getFinishTime(), false), task.getStatus(),
                task.getResult(), task.getRemark(), task.getTaskId());
    }

    @Override
    public int updateStatus(Integer taskId, Integer status) {
        return JdbcTemplate.update("UPDATE t_repair_task SET status = ? WHERE task_id = ?", status, taskId);
    }

    @Override
    public int updateFinish(Integer taskId, Integer status, String result, String remark) {
        return JdbcTemplate.update("UPDATE t_repair_task SET status = ?, result = ?, remark = ?, finish_time = ?"
                + " WHERE task_id = ?", status, result, remark, stamp(new Date(), true), taskId);
    }

    @Override
    public int delete(Integer taskId) {
        return JdbcTemplate.update("DELETE FROM t_repair_task WHERE task_id = ?", taskId);
    }

    @Override
    public RepairTask findById(Integer taskId) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + FROM + " WHERE t.task_id = ?", RepairTask.class, taskId);
    }

    @Override
    public RepairTask findActiveByOrder(Integer orderId) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + FROM + " WHERE t.order_id = ?"
                + " AND t.status NOT IN (4, 5, 6) ORDER BY t.task_id DESC LIMIT 1", RepairTask.class, orderId);
    }

    @Override
    public List<RepairTask> findByOrder(Integer orderId) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + FROM + " WHERE t.order_id = ? ORDER BY t.task_id DESC",
                RepairTask.class, orderId);
    }

    @Override
    public List<RepairTask> findByWorker(Integer workerId) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + FROM + " WHERE t.worker_id = ?"
                + " ORDER BY t.status ASC, o.priority DESC, t.dispatch_time DESC", RepairTask.class, workerId);
    }

    @Override
    public List<RepairTask> findByWorkerAndStatus(Integer workerId, Integer status) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + FROM + " WHERE t.worker_id = ? AND t.status = ?"
                + " ORDER BY o.priority DESC, t.dispatch_time ASC", RepairTask.class, workerId, status);
    }

    @Override
    public List<RepairTask> findByStatus(Integer status) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + FROM + " WHERE t.status = ?"
                + " ORDER BY t.dispatch_time ASC", RepairTask.class, status);
    }

    @Override
    public List<RepairTask> findAll() {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + FROM + " ORDER BY t.task_id DESC", RepairTask.class);
    }

    @Override
    public long countActiveByWorker(Integer workerId) {
        String sql = "SELECT COUNT(*) FROM t_repair_task WHERE worker_id = ? AND status IN (0, 1, 2, 4)";
        return JdbcTemplate.queryCount(sql, workerId);
    }

    @Override
    public long countByStatus(Integer status) {
        return JdbcTemplate.queryCount("SELECT COUNT(*) FROM t_repair_task WHERE status = ?", status);
    }

    private static java.sql.Timestamp stamp(Date value, boolean nowIfNull) {
        if (value == null) {
            return nowIfNull ? new java.sql.Timestamp(System.currentTimeMillis()) : null;
        }
        return new java.sql.Timestamp(value.getTime());
    }
}
