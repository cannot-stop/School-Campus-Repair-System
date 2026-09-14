package com.campus.repair.dao;

import java.util.List;

import com.campus.repair.domain.Worker;

/**
 * 维修人员数据访问接口（对应设计书 2.2.6 WorkerDao）。
 */
public interface WorkerDao {

    int insert(Worker worker);

    int update(Worker worker);

    int updateStatus(Integer workerId, Integer status);

    int updateLocation(Integer workerId, String location);

    /** 在单量增减（delta 为 +1 / -1），返回影响行数 */
    int addCurrentOrders(Integer workerId, int delta);

    int delete(Integer workerId);

    Worker findById(Integer workerId);

    Worker findByUserId(Integer userId);

    List<Worker> findAll();

    /** 可派单的维修人员（在线），按在单量升序 */
    List<Worker> findAvailable();

    List<Worker> findBySkill(String skill);
}
