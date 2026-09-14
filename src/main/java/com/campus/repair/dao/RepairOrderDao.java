package com.campus.repair.dao;

import java.util.List;

import com.campus.repair.common.PageResult;
import com.campus.repair.domain.OrderQuery;
import com.campus.repair.domain.RepairOrder;

/**
 * 报修单数据访问接口（对应设计书 2.2.6 RepairOrderDao）。
 */
public interface RepairOrderDao {

    int insert(RepairOrder order);

    int update(RepairOrder order);

    int updateStatus(Integer orderId, Integer status);

    int updateReject(Integer orderId, Integer status, String rejectReason);

    /** 催办次数 +1 */
    int increaseUrgeCount(Integer orderId);

    int delete(Integer orderId);

    RepairOrder findById(Integer orderId);

    /** 按条件分页查询（设计书 2.2.3.2 的 findByCondition） */
    List<RepairOrder> findByCondition(OrderQuery query);

    long countByCondition(OrderQuery query);

    PageResult<RepairOrder> pageByCondition(OrderQuery query);

    /** 维修人员名下任务对应的报修单 */
    List<RepairOrder> findByWorker(Integer workerId, Integer status);

    List<RepairOrder> findByUser(Integer userId);

    long countByStatus(Integer status);

    long countAll();
}
