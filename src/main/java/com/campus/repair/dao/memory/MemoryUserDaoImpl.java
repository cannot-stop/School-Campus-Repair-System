package com.campus.repair.dao.memory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.campus.repair.dao.UserDao;
import com.campus.repair.domain.User;

/**
 * 用户数据访问内存实现（用于无 MySQL 环境的运行与自动化自测）。
 */
public class MemoryUserDaoImpl implements UserDao {

    @Override
    public int insert(User user) {
        int id = MemoryStore.nextId(MemoryStore.USER_SEQ, user.getUserId());
        user.setUserId(Integer.valueOf(id));
        if (user.getCreateTime() == null) {
            user.setCreateTime(new Date());
        }
        if (user.getAuditStatus() == null) {
            user.setAuditStatus(Integer.valueOf(0));
        }
        if (user.getIsLocked() == null) {
            user.setIsLocked(Integer.valueOf(0));
        }
        MemoryStore.USERS.put(Integer.valueOf(id), user);
        return 1;
    }

    @Override
    public int update(User user) {
        User exists = MemoryStore.USERS.get(user.getUserId());
        if (exists == null) {
            return 0;
        }
        exists.setRealName(user.getRealName());
        exists.setStudentNo(user.getStudentNo());
        exists.setPhone(user.getPhone());
        exists.setRole(user.getRole());
        return 1;
    }

    @Override
    public int updatePassword(Integer userId, String password) {
        User exists = MemoryStore.USERS.get(userId);
        if (exists == null) {
            return 0;
        }
        exists.setPassword(password);
        return 1;
    }

    @Override
    public int updateAuditStatus(Integer userId, Integer auditStatus) {
        User exists = MemoryStore.USERS.get(userId);
        if (exists == null) {
            return 0;
        }
        exists.setAuditStatus(auditStatus);
        return 1;
    }

    @Override
    public int updateLocked(Integer userId, Integer isLocked) {
        User exists = MemoryStore.USERS.get(userId);
        if (exists == null) {
            return 0;
        }
        exists.setIsLocked(isLocked);
        return 1;
    }

    @Override
    public int delete(Integer userId) {
        return MemoryStore.USERS.remove(userId) == null ? 0 : 1;
    }

    @Override
    public User findById(Integer userId) {
        return MemoryStore.USERS.get(userId);
    }

    @Override
    public User findByUsername(String username) {
        for (User user : MemoryStore.USERS.values()) {
            if (user.getUsername() != null && user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public User findByPhone(String phone) {
        for (User user : MemoryStore.USERS.values()) {
            if (user.getPhone() != null && user.getPhone().equals(phone)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public User findByStudentNo(String studentNo) {
        for (User user : MemoryStore.USERS.values()) {
            if (user.getStudentNo() != null && user.getStudentNo().equals(studentNo)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public List<User> findByRole(String role) {
        List<User> result = new ArrayList<User>();
        for (User user : MemoryStore.USERS.values()) {
            if (user.getRole() != null && user.getRole().equals(role)) {
                result.add(user);
            }
        }
        return result;
    }

    @Override
    public List<User> findByAuditStatus(Integer auditStatus) {
        List<User> result = new ArrayList<User>();
        for (User user : MemoryStore.USERS.values()) {
            if (auditStatus.equals(user.getAuditStatus())) {
                result.add(user);
            }
        }
        return result;
    }

    @Override
    public List<User> findAll() {
        List<User> result = new ArrayList<User>(MemoryStore.USERS.values());
        result.sort(new java.util.Comparator<User>() {
            @Override
            public int compare(User a, User b) {
                return a.getUserId().compareTo(b.getUserId());
            }
        });
        return result;
    }

    @Override
    public long countByRole(String role) {
        long count = 0;
        for (User user : MemoryStore.USERS.values()) {
            if (user.getRole() != null && user.getRole().equals(role)) {
                count++;
            }
        }
        return count;
    }
}
