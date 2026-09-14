package com.campus.repair.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.campus.repair.domain.RepairTask;

/**
 * 维修任务 Mapper 接口（对应设计书 2.2.6 RepairTaskDao 与表 2.12）。
 */
public interface RepairTaskMapper {

    int insert(RepairTask task);

    int update(RepairTask task);

    int updateStatus(@Param("taskId") Integer taskId, @Param("status") Integer status);

    int updateFinish(@Param("taskId") Integer taskId, @Param("status") Integer status,
                     @Param("result") String result, @Param("remark") String remark);

    int delete(@Param("taskId") Integer taskId);

    RepairTask findById(@Param("taskId") Integer taskId);

    /** 报修单当前有效任务（最新一条非转单/退单任务） */
    RepairTask findActiveByOrder(@Param("orderId") Integer orderId);

    List<RepairTask> findByOrder(@Param("orderId") Integer orderId);

    List<RepairTask> findByWorker(@Param("workerId") Integer workerId);

    List<RepairTask> findByWorkerAndStatus(@Param("workerId") Integer workerId, @Param("status") Integer status);

    List<RepairTask> findByStatus(@Param("status") Integer status);

    List<RepairTask> findAll();

    /** 维修工进行中的任务数量（含待接单/维修中/待确认/转单，用于在单量校正） */
    long countActiveByWorker(@Param("workerId") Integer workerId);

    long countByStatus(@Param("status") Integer status);
}
