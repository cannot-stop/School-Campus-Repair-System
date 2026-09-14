package com.campus.repair.dao;

import java.util.List;

import com.campus.repair.domain.Evaluation;

/**
 * 评价数据访问接口（对应设计书 2.2.6 EvaluationDao）。
 */
public interface EvaluationDao {

    int insert(Evaluation evaluation);

    int update(Evaluation evaluation);

    /** 管理员回复 */
    int updateReply(Integer evalId, String reply);

    /** 评价审核 */
    int updateAuditStatus(Integer evalId, Integer auditStatus);

    int delete(Integer evalId);

    Evaluation findById(Integer evalId);

    Evaluation findByOrder(Integer orderId);

    List<Evaluation> findAll();

    List<Evaluation> findByAuditStatus(Integer auditStatus);

    List<Evaluation> findByWorker(Integer workerId);

    /** 平均评分（无评价返回 0） */
    double avgScore();

    /** 平均评分（指定维修工） */
    double avgScoreByWorker(Integer workerId);

    long countAll();
}
