package com.campus.repair.dao.mybatis;

import java.util.List;

import org.apache.ibatis.session.SqlSession;

import com.campus.repair.dao.WorkerDao;
import com.campus.repair.dao.mapper.WorkerMapper;
import com.campus.repair.domain.Worker;

/**
 * 维修人员数据访问 MyBatis 实现（对应设计书 2.2.6 WorkerDao）。
 * SQL 见 {@code resources/mapper/WorkerMapper.xml}。
 */
public class MyBatisWorkerDaoImpl extends MyBatisDaoSupport implements WorkerDao {

    private WorkerMapper mapper(SqlSession session) {
        return session.getMapper(WorkerMapper.class);
    }

    @Override
    public int insert(Worker worker) {
        if (worker.getCurrentOrders() == null) {
            worker.setCurrentOrders(Integer.valueOf(0));
        }
        if (worker.getStatus() == null) {
            worker.setStatus(Integer.valueOf(1));
        }
        return mutate(session -> mapper(session).insert(worker));
    }

    @Override
    public int update(Worker worker) {
        return mutate(session -> mapper(session).update(worker));
    }

    @Override
    public int updateStatus(Integer workerId, Integer status) {
        return mutate(session -> mapper(session).updateStatus(workerId, status));
    }

    @Override
    public int updateLocation(Integer workerId, String location) {
        return mutate(session -> mapper(session).updateLocation(workerId, location));
    }

    @Override
    public int addCurrentOrders(Integer workerId, int delta) {
        return mutate(session -> mapper(session).addCurrentOrders(workerId, delta));
    }

    @Override
    public int delete(Integer workerId) {
        return mutate(session -> mapper(session).delete(workerId));
    }

    @Override
    public Worker findById(Integer workerId) {
        return query(session -> mapper(session).findById(workerId));
    }

    @Override
    public Worker findByUserId(Integer userId) {
        return query(session -> mapper(session).findByUserId(userId));
    }

    @Override
    public List<Worker> findAll() {
        return query(session -> mapper(session).findAll());
    }

    @Override
    public List<Worker> findAvailable() {
        return query(session -> mapper(session).findAvailable());
    }

    @Override
    public List<Worker> findBySkill(String skill) {
        if (skill == null || skill.trim().isEmpty()) {
            return findAll();
        }
        return query(session -> mapper(session).findBySkill(skill.trim()));
    }
}
