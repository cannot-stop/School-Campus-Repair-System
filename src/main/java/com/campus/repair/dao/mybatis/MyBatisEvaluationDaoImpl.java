package com.campus.repair.dao.mybatis;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import com.campus.repair.dao.EvaluationDao;
import com.campus.repair.dao.mapper.EvaluationMapper;
import com.campus.repair.domain.Evaluation;

/**
 * 评价数据访问 MyBatis 实现（对应设计书 2.2.6 EvaluationDao 与表 2.16）。
 * SQL 见 {@code resources/mapper/EvaluationMapper.xml}。
 */
public class MyBatisEvaluationDaoImpl extends MyBatisDaoSupport implements EvaluationDao {

    private EvaluationMapper mapper(SqlSession session) {
        return session.getMapper(EvaluationMapper.class);
    }

    @Override
    public int insert(Evaluation evaluation) {
        if (evaluation.getCreateTime() == null) {
            evaluation.setCreateTime(new Date());
        }
        if (evaluation.getAuditStatus() == null) {
            evaluation.setAuditStatus(Integer.valueOf(0));
        }
        return mutate(session -> mapper(session).insert(evaluation));
    }

    @Override
    public int update(Evaluation evaluation) {
        return mutate(session -> mapper(session).update(evaluation));
    }

    @Override
    public int updateReply(Integer evalId, String reply) {
        return mutate(session -> mapper(session).updateReply(evalId, reply));
    }

    @Override
    public int updateAuditStatus(Integer evalId, Integer auditStatus) {
        return mutate(session -> mapper(session).updateAuditStatus(evalId, auditStatus));
    }

    @Override
    public int delete(Integer evalId) {
        return mutate(session -> mapper(session).delete(evalId));
    }

    @Override
    public Evaluation findById(Integer evalId) {
        return query(session -> mapper(session).findById(evalId));
    }

    @Override
    public Evaluation findByOrder(Integer orderId) {
        return query(session -> mapper(session).findByOrder(orderId));
    }

    @Override
    public List<Evaluation> findAll() {
        return query(session -> mapper(session).findAll());
    }

    @Override
    public List<Evaluation> findByAuditStatus(Integer auditStatus) {
        return query(session -> mapper(session).findByAuditStatus(auditStatus));
    }

    @Override
    public List<Evaluation> findByWorker(Integer workerId) {
        return query(session -> mapper(session).findByWorker(workerId));
    }

    /** 平均评分：SQL 无数据时返回 null，此处统一转为 0（与内存库实现语义一致） */
    @Override
    public double avgScore() {
        Double value = query(session -> mapper(session).avgScore());
        return value == null ? 0d : value.doubleValue();
    }

    @Override
    public double avgScoreByWorker(Integer workerId) {
        Double value = query(session -> mapper(session).avgScoreByWorker(workerId));
        return value == null ? 0d : value.doubleValue();
    }

    @Override
    public long countAll() {
        Long value = query(session -> Long.valueOf(mapper(session).countAll()));
        return value == null ? 0L : value.longValue();
    }
}
