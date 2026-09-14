package com.campus.repair.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.campus.repair.domain.User;

/**
 * 用户 Mapper 接口（对应设计书 2.2.6 UserDao）。
 *
 * <p>SQL 定义在 {@code mapper/UserMapper.xml} 中，与设计书表 2.10 的物理结构一一对应。</p>
 */
public interface UserMapper {

    int insert(User user);

    int update(User user);

    int updatePassword(@Param("userId") Integer userId, @Param("password") String password);

    int updateAuditStatus(@Param("userId") Integer userId, @Param("auditStatus") Integer auditStatus);

    int updateLocked(@Param("userId") Integer userId, @Param("isLocked") Integer isLocked);

    int delete(@Param("userId") Integer userId);

    User findById(@Param("userId") Integer userId);

    User findByUsername(@Param("username") String username);

    User findByPhone(@Param("phone") String phone);

    User findByStudentNo(@Param("studentNo") String studentNo);

    List<User> findByRole(@Param("role") String role);

    List<User> findByAuditStatus(@Param("auditStatus") Integer auditStatus);

    List<User> findAll();

    long countByRole(@Param("role") String role);
}
