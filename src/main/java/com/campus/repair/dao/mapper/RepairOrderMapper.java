package com.campus.repair.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.campus.repair.domain.RepairOrder;

/**
 * 报修单 Mapper 接口（对应设计书 2.2.6 RepairOrderDao 与表 2.11）。
 *
 * <p>其中 {@link #findByCondition} / {@link #countByCondition} 对应设计书 2.2.3.2 的
 * {@code findByCondition()} 按条件分页查询；查询语句通过 LEFT JOIN 一次性带出报修人、
 * 当前维修任务、维修人员与评价信息。</p>
 */
public interface RepairOrderMapper {

    int insert(RepairOrder order);

    /** 仅更新非空字段，支持"审核只调整优先级"这类局部更新 */
    int updateSelective(RepairOrder order);

    int updateStatus(@Param("orderId") Integer orderId, @Param("status") Integer status);

    int updateReject(@Param("orderId") Integer orderId, @Param("status") Integer status,
                     @Param("rejectReason") String rejectReason);

    int increaseUrgeCount(@Param("orderId") Integer orderId);

    int delete(@Param("orderId") Integer orderId);

    RepairOrder findById(@Param("orderId") Integer orderId);

    List<RepairOrder> findByCondition(RepairOrderQuery query);

    long countByCondition(RepairOrderQuery query);

    List<RepairOrder> findByWorker(@Param("workerId") Integer workerId, @Param("status") Integer status);

    List<RepairOrder> findByUser(@Param("userId") Integer userId);

    long countByStatus(@Param("status") Integer status);

    long countAll();
}
