package com.campus.repair.dao.mybatis;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import com.campus.repair.dao.UserDao;
import com.campus.repair.dao.mapper.UserMapper;
import com.campus.repair.domain.User;

/**
 * 用户数据访问 MyBatis 实现（对应设计书 2.2.6 UserDao 的 MyBatis 版本）。
 *
 * <p>SQL 全部定义在 {@code resources/mapper/UserMapper.xml}，
 * 本类只负责「取 Mapper → 调用方法 → 返回值转换」，不出现任何 SQL 字符串。</p>
 */
public class MyBatisUserDaoImpl extends MyBatisDaoSupport implements UserDao {

    private UserMapper mapper(SqlSession session) {
        return session.getMapper(UserMapper.class);
    }

    @Override
    public int insert(User user) {
        if (user.getAuditStatus() == null) {
            user.setAuditStatus(Integer.valueOf(0));
        }
        if (user.getIsLocked() == null) {
            user.setIsLocked(Integer.valueOf(0));
        }
        if (user.getCreateTime() == null) {
            user.setCreateTime(new Date());
        }
        // 主键由 useGeneratedKeys 回填到 user.userId（Service 层随后需要使用该 ID）
        return mutate(session -> mapper(session).insert(user));
    }

    @Override
    public int update(User user) {
        return mutate(session -> mapper(session).update(user));
    }

    @Override
    public int updatePassword(Integer userId, String password) {
        return mutate(session -> mapper(session).updatePassword(userId, password));
    }

    @Override
    public int updateAuditStatus(Integer userId, Integer auditStatus) {
        return mutate(session -> mapper(session).updateAuditStatus(userId, auditStatus));
    }

    @Override
    public int updateLocked(Integer userId, Integer isLocked) {
        return mutate(session -> mapper(session).updateLocked(userId, isLocked));
    }

    @Override
    public int delete(Integer userId) {
        return mutate(session -> mapper(session).delete(userId));
    }

    @Override
    public User findById(Integer userId) {
        return query(session -> mapper(session).findById(userId));
    }

    @Override
    public User findByUsername(String username) {
        return query(session -> mapper(session).findByUsername(username));
    }

    @Override
    public User findByPhone(String phone) {
        return query(session -> mapper(session).findByPhone(phone));
    }

    @Override
    public User findByStudentNo(String studentNo) {
        return query(session -> mapper(session).findByStudentNo(studentNo));
    }

    @Override
    public List<User> findByRole(String role) {
        return query(session -> mapper(session).findByRole(role));
    }

    @Override
    public List<User> findByAuditStatus(Integer auditStatus) {
        return query(session -> mapper(session).findByAuditStatus(auditStatus));
    }

    @Override
    public List<User> findAll() {
        return query(session -> mapper(session).findAll());
    }

    @Override
    public long countByRole(String role) {
        Long count = query(session -> Long.valueOf(mapper(session).countByRole(role)));
        return count == null ? 0L : count.longValue();
    }
}
