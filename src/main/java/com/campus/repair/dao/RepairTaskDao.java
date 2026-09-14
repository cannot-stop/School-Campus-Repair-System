package com.campus.repair.dao;

import java.util.List;

import com.campus.repair.domain.RepairTask;

/**
 * 维修任务数据访问接口（对应设计书 2.2.6 RepairTaskDao）。
 */
public interface RepairTaskDao {

    int insert(RepairTask task);

    int update(RepairTask task);

    int updateStatus(Integer taskId, Integer status);

    /** 完成任务登记（状态、结果、备注、完成时间） */
    int updateFinish(Integer taskId, Integer status, String result, String remark);

    int delete(Integer taskId);

    RepairTask findById(Integer taskId);

    /** 报修单当前有效任务（最新一条非转单/退单任务） */
    RepairTask findActiveByOrder(Integer orderId);

    List<RepairTask> findByOrder(Integer orderId);

    List<RepairTask> findByWorker(Integer workerId);

    List<RepairTask> findByWorkerAndStatus(Integer workerId, Integer status);

    List<RepairTask> findByStatus(Integer status);

    List<RepairTask> findAll();

    /** 维修工进行中的任务数量（用于校正在单量） */
    long countActiveByWorker(Integer workerId);

    long countByStatus(Integer status);
}
