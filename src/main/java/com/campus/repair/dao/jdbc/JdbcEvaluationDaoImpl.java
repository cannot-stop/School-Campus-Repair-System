package com.campus.repair.dao.jdbc;

import java.util.List;

import com.campus.repair.dao.EvaluationDao;
import com.campus.repair.domain.Evaluation;

/**
 * 评价数据访问 JDBC 实现（对应设计书 2.2.6 EvaluationDao）。
 */
public class JdbcEvaluationDaoImpl implements EvaluationDao {

    private static final String COLUMNS =
            "e.eval_id, e.order_id, e.score, e.comment, e.reply, e.audit_status, e.create_time,"
                    + " o.order_id AS order_no, o.category,"
                    + " ru.real_name AS reporter_name, w.name AS worker_name";

    private static final String FROM =
            " FROM t_evaluation e"
                    + " LEFT JOIN t_repair_order o ON o.order_id = e.order_id"
                    + " LEFT JOIN t_user ru ON ru.user_id = o.user_id"
                    + " LEFT JOIN t_repair_task t ON t.task_id = ("
                    + "     SELECT t2.task_id FROM t_repair_task t2 WHERE t2.order_id = o.order_id"
                    + "     AND t2.status NOT IN (4, 5, 6) ORDER BY t2.task_id DESC LIMIT 1)"
                    + " LEFT JOIN t_worker w ON w.worker_id = t.worker_id";

    @Override
    public int insert(Evaluation evaluation) {
        String sql = "INSERT INTO t_evaluation (order_id, score, comment, reply, audit_status, create_time)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        int id = JdbcTemplate.insertReturnKey(sql, evaluation.getOrderId(), evaluation.getScore(),
                evaluation.getComment(), evaluation.getReply(),
                evaluation.getAuditStatus() == null ? Integer.valueOf(0) : evaluation.getAuditStatus(),
                new java.sql.Timestamp(evaluation.getCreateTime() == null
                        ? System.currentTimeMillis() : evaluation.getCreateTime().getTime()));
        evaluation.setEvalId(Integer.valueOf(id));
        return id > 0 ? 1 : 0;
    }

    @Override
    public int update(Evaluation evaluation) {
        String sql = "UPDATE t_evaluation SET score = ?, comment = ?, reply = ?, audit_status = ? WHERE eval_id = ?";
        return JdbcTemplate.update(sql, evaluation.getScore(), evaluation.getComment(), evaluation.getReply(),
                evaluation.getAuditStatus(), evaluation.getEvalId());
    }

    @Override
    public int updateReply(Integer evalId, String reply) {
        return JdbcTemplate.update("UPDATE t_evaluation SET reply = ? WHERE eval_id = ?", reply, evalId);
    }

    @Override
    public int updateAuditStatus(Integer evalId, Integer auditStatus) {
        return JdbcTemplate.update("UPDATE t_evaluation SET audit_status = ? WHERE eval_id = ?", auditStatus, evalId);
    }

    @Override
    public int delete(Integer evalId) {
        return JdbcTemplate.update("DELETE FROM t_evaluation WHERE eval_id = ?", evalId);
    }

    @Override
    public Evaluation findById(Integer evalId) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + FROM + " WHERE e.eval_id = ?", Evaluation.class, evalId);
    }

    @Override
    public Evaluation findByOrder(Integer orderId) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + FROM + " WHERE e.order_id = ?", Evaluation.class, orderId);
    }

    @Override
    public List<Evaluation> findAll() {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + FROM + " ORDER BY e.create_time DESC", Evaluation.class);
    }

    @Override
    public List<Evaluation> findByAuditStatus(Integer auditStatus) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + FROM + " WHERE e.audit_status = ?"
                + " ORDER BY e.create_time DESC", Evaluation.class, auditStatus);
    }

    @Override
    public List<Evaluation> findByWorker(Integer workerId) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + FROM + " WHERE t.worker_id = ?"
                + " ORDER BY e.create_time DESC", Evaluation.class, workerId);
    }

    @Override
    public double avgScore() {
        Object value = JdbcTemplate.queryScalar("SELECT AVG(score) FROM t_evaluation WHERE audit_status = 1");
        return toDouble(value);
    }

    @Override
    public double avgScoreByWorker(Integer workerId) {
        String sql = "SELECT AVG(e.score) FROM t_evaluation e"
                + " JOIN t_repair_task t ON t.order_id = e.order_id AND t.status = 3"
                + " WHERE t.worker_id = ?";
        return toDouble(JdbcTemplate.queryScalar(sql, workerId));
    }

    @Override
    public long countAll() {
        return JdbcTemplate.queryCount("SELECT COUNT(*) FROM t_evaluation");
    }

    private static double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0d;
    }
}
