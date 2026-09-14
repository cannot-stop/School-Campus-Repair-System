package com.campus.repair.dao.mybatis;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import com.campus.repair.dao.RepairTaskDao;
import com.campus.repair.dao.mapper.RepairTaskMapper;
import com.campus.repair.domain.RepairTask;

/**
 * 维修任务数据访问 MyBatis 实现（对应设计书 2.2.6 RepairTaskDao 与表 2.12）。
 * SQL 见 {@code resources/mapper/RepairTaskMapper.xml}。
 */
public class MyBatisRepairTaskDaoImpl extends MyBatisDaoSupport implements RepairTaskDao {

    private RepairTaskMapper mapper(SqlSession session) {
        return session.getMapper(RepairTaskMapper.class);
    }

    @Override
    public int insert(RepairTask task) {
        if (task.getDispatchTime() == null) {
            task.setDispatchTime(new Date());
        }
        if (task.getStatus() == null) {
            task.setStatus(Integer.valueOf(0));
        }
        return mutate(session -> mapper(session).insert(task));
    }

    @Override
    public int update(RepairTask task) {
        return mutate(session -> mapper(session).update(task));
    }

    @Override
    public int updateStatus(Integer taskId, Integer status) {
        return mutate(session -> mapper(session).updateStatus(taskId, status));
    }

    @Override
    public int updateFinish(Integer taskId, Integer status, String result, String remark) {
        return mutate(session -> mapper(session).updateFinish(taskId, status, result, remark));
    }

    @Override
    public int delete(Integer taskId) {
        return mutate(session -> mapper(session).delete(taskId));
    }

    @Override
    public RepairTask findById(Integer taskId) {
        return query(session -> mapper(session).findById(taskId));
    }

    @Override
    public RepairTask findActiveByOrder(Integer orderId) {
        return query(session -> mapper(session).findActiveByOrder(orderId));
    }

    @Override
    public List<RepairTask> findByOrder(Integer orderId) {
        return query(session -> mapper(session).findByOrder(orderId));
    }

    @Override
    public List<RepairTask> findByWorker(Integer workerId) {
        return query(session -> mapper(session).findByWorker(workerId));
    }

    @Override
    public List<RepairTask> findByWorkerAndStatus(Integer workerId, Integer status) {
        return query(session -> mapper(session).findByWorkerAndStatus(workerId, status));
    }

    @Override
    public List<RepairTask> findByStatus(Integer status) {
        return query(session -> mapper(session).findByStatus(status));
    }

    @Override
    public List<RepairTask> findAll() {
        return query(session -> mapper(session).findAll());
    }

    @Override
    public long countActiveByWorker(Integer workerId) {
        Long value = query(session -> Long.valueOf(mapper(session).countActiveByWorker(workerId)));
        return value == null ? 0L : value.longValue();
    }

    @Override
    public long countByStatus(Integer status) {
        Long value = query(session -> Long.valueOf(mapper(session).countByStatus(status)));
        return value == null ? 0L : value.longValue();
    }
}
