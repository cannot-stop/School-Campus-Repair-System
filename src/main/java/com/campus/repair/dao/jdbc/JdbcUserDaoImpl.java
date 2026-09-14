package com.campus.repair.dao.jdbc;

import java.util.List;

import com.campus.repair.dao.UserDao;
import com.campus.repair.domain.User;

/**
 * 用户数据访问 JDBC 实现（MyBatis 的等价 SQL 手写实现，对应设计书 2.2.6 UserDao）。
 */
public class JdbcUserDaoImpl implements UserDao {

    private static final String COLUMNS =
            "user_id, username, password, real_name, student_no, phone, role, audit_status, is_locked, create_time";

    @Override
    public int insert(User user) {
        String sql = "INSERT INTO t_user (username, password, real_name, student_no, phone, role, audit_status, is_locked, create_time)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int id = JdbcTemplate.insertReturnKey(sql,
                user.getUsername(), user.getPassword(), user.getRealName(), user.getStudentNo(),
                user.getPhone(), user.getRole(),
                user.getAuditStatus() == null ? Integer.valueOf(0) : user.getAuditStatus(),
                user.getIsLocked() == null ? Integer.valueOf(0) : user.getIsLocked(),
                now(user.getCreateTime()));
        user.setUserId(Integer.valueOf(id));
        return id > 0 ? 1 : 0;
    }

    @Override
    public int update(User user) {
        String sql = "UPDATE t_user SET real_name = ?, student_no = ?, phone = ?, role = ? WHERE user_id = ?";
        return JdbcTemplate.update(sql, user.getRealName(), user.getStudentNo(), user.getPhone(),
                user.getRole(), user.getUserId());
    }

    @Override
    public int updatePassword(Integer userId, String password) {
        return JdbcTemplate.update("UPDATE t_user SET password = ? WHERE user_id = ?", password, userId);
    }

    @Override
    public int updateAuditStatus(Integer userId, Integer auditStatus) {
        return JdbcTemplate.update("UPDATE t_user SET audit_status = ? WHERE user_id = ?", auditStatus, userId);
    }

    @Override
    public int updateLocked(Integer userId, Integer isLocked) {
        return JdbcTemplate.update("UPDATE t_user SET is_locked = ? WHERE user_id = ?", isLocked, userId);
    }

    @Override
    public int delete(Integer userId) {
        return JdbcTemplate.update("DELETE FROM t_user WHERE user_id = ?", userId);
    }

    @Override
    public User findById(Integer userId) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + " FROM t_user WHERE user_id = ?", User.class, userId);
    }

    @Override
    public User findByUsername(String username) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + " FROM t_user WHERE username = ?", User.class, username);
    }

    @Override
    public User findByPhone(String phone) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + " FROM t_user WHERE phone = ?", User.class, phone);
    }

    @Override
    public User findByStudentNo(String studentNo) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + " FROM t_user WHERE student_no = ?", User.class, studentNo);
    }

    @Override
    public List<User> findByRole(String role) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_user WHERE role = ? ORDER BY user_id DESC",
                User.class, role);
    }

    @Override
    public List<User> findByAuditStatus(Integer auditStatus) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_user WHERE audit_status = ? ORDER BY create_time DESC",
                User.class, auditStatus);
    }

    @Override
    public List<User> findAll() {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_user ORDER BY user_id", User.class);
    }

    @Override
    public long countByRole(String role) {
        return JdbcTemplate.queryCount("SELECT COUNT(*) FROM t_user WHERE role = ?", role);
    }

    /** 时间字段空值兜底 */
    private static java.sql.Timestamp now(java.util.Date value) {
        return value == null
                ? new java.sql.Timestamp(System.currentTimeMillis())
                : new java.sql.Timestamp(value.getTime());
    }
}
