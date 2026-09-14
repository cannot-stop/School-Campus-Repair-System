package com.campus.repair.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.campus.repair.domain.Evaluation;

/**
 * 评价 Mapper 接口（对应设计书 2.2.6 EvaluationDao 与表 2.16）。
 */
public interface EvaluationMapper {

    int insert(Evaluation evaluation);

    int update(Evaluation evaluation);

    int updateReply(@Param("evalId") Integer evalId, @Param("reply") String reply);

    int updateAuditStatus(@Param("evalId") Integer evalId, @Param("auditStatus") Integer auditStatus);

    int delete(@Param("evalId") Integer evalId);

    Evaluation findById(@Param("evalId") Integer evalId);

    Evaluation findByOrder(@Param("orderId") Integer orderId);

    List<Evaluation> findAll();

    List<Evaluation> findByAuditStatus(@Param("auditStatus") Integer auditStatus);

    List<Evaluation> findByWorker(@Param("workerId") Integer workerId);

    /** 平均评分（仅统计审核通过的评价值，无数据返回 null） */
    Double avgScore();

    Double avgScoreByWorker(@Param("workerId") Integer workerId);

    long countAll();
}
