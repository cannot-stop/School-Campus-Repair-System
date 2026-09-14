package com.campus.repair.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.campus.repair.domain.Worker;

/**
 * 维修人员 Mapper 接口（对应设计书 2.2.6 WorkerDao）。
 */
public interface WorkerMapper {

    int insert(Worker worker);

    int update(Worker worker);

    int updateStatus(@Param("workerId") Integer workerId, @Param("status") Integer status);

    int updateLocation(@Param("workerId") Integer workerId, @Param("location") String location);

    /** 在单量增减（delta 为 +1 / -1），SQL 使用 GREATEST 保证不为负 */
    int addCurrentOrders(@Param("workerId") Integer workerId, @Param("delta") int delta);

    int delete(@Param("workerId") Integer workerId);

    Worker findById(@Param("workerId") Integer workerId);

    Worker findByUserId(@Param("userId") Integer userId);

    List<Worker> findAll();

    List<Worker> findAvailable();

    List<Worker> findBySkill(@Param("skill") String skill);
}
