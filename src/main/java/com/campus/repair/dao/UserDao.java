package com.campus.repair.dao;

import java.util.List;

import com.campus.repair.domain.User;

/**
 * 用户数据访问接口（对应设计书 2.2.6 UserDao）。
 */
public interface UserDao {

    int insert(User user);

    int update(User user);

    int updatePassword(Integer userId, String password);

    int updateAuditStatus(Integer userId, Integer auditStatus);

    int updateLocked(Integer userId, Integer isLocked);

    int delete(Integer userId);

    User findById(Integer userId);

    User findByUsername(String username);

    User findByPhone(String phone);

    User findByStudentNo(String studentNo);

    List<User> findByRole(String role);

    List<User> findByAuditStatus(Integer auditStatus);

    List<User> findAll();

    long countByRole(String role);
}
