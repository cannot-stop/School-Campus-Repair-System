package com.campus.repair.dao.mybatis;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import com.campus.repair.common.PageResult;
import com.campus.repair.dao.RepairOrderDao;
import com.campus.repair.dao.mapper.RepairOrderMapper;
import com.campus.repair.dao.mapper.RepairOrderQuery;
import com.campus.repair.domain.OrderQuery;
import com.campus.repair.domain.RepairOrder;

/**
 * 报修单数据访问 MyBatis 实现（对应设计书 2.2.6 RepairOrderDao 与表 2.11）。
 *
 * <p>SQL 见 {@code resources/mapper/RepairOrderMapper.xml}，其中：
 * <ul>
 *   <li>{@code findById} 通过 LEFT JOIN 带出报修人、当前维修任务、维修人员与评价；</li>
 *   <li>{@code findByCondition} / {@code countByCondition} 对应设计书 2.2.3.2
 *       的按条件分页查询，条件由 {@code <where>+<if>} 动态拼接，分页由 LIMIT/OFFSET 完成；</li>
 *   <li>{@code updateSelective} 仅更新非空字段，保证"审核只调整优先级"不会清空其他业务字段。</li>
 * </ul>
 */
public class MyBatisRepairOrderDaoImpl extends MyBatisDaoSupport implements RepairOrderDao {

    private RepairOrderMapper mapper(SqlSession session) {
        return session.getMapper(RepairOrderMapper.class);
    }

    @Override
    public int insert(RepairOrder order) {
        if (order.getCreateTime() == null) {
            order.setCreateTime(new Date());
        }
        if (order.getStatus() == null) {
            order.setStatus(Integer.valueOf(0));
        }
        if (order.getPriority() == null) {
            order.setPriority(Integer.valueOf(0));
        }
        if (order.getUrgeCount() == null) {
            order.setUrgeCount(Integer.valueOf(0));
        }
        return mutate(session -> mapper(session).insert(order));
    }

    @Override
    public int update(RepairOrder order) {
        return mutate(session -> mapper(session).updateSelective(order));
    }

    @Override
    public int updateStatus(Integer orderId, Integer status) {
        return mutate(session -> mapper(session).updateStatus(orderId, status));
    }

    @Override
    public int updateReject(Integer orderId, Integer status, String rejectReason) {
        return mutate(session -> mapper(session).updateReject(orderId, status, rejectReason));
    }

    @Override
    public int increaseUrgeCount(Integer orderId) {
        return mutate(session -> mapper(session).increaseUrgeCount(orderId));
    }

    @Override
    public int delete(Integer orderId) {
        return mutate(session -> mapper(session).delete(orderId));
    }

    @Override
    public RepairOrder findById(Integer orderId) {
        return query(session -> mapper(session).findById(orderId));
    }

    @Override
    public List<RepairOrder> findByCondition(OrderQuery query) {
        RepairOrderQuery params = toMapperQuery(query);
        return query(session -> mapper(session).findByCondition(params));
    }

    @Override
    public long countByCondition(OrderQuery query) {
        RepairOrderQuery params = toMapperQuery(query);
        Long count = query(session -> Long.valueOf(mapper(session).countByCondition(params)));
        return count == null ? 0L : count.longValue();
    }

    @Override
    public PageResult<RepairOrder> pageByCondition(OrderQuery query) {
        long total = countByCondition(query);
        int pageNum = query == null ? 1 : Math.max(query.getPageNum(), 1);
        int pageSize = query == null ? 10 : Math.max(query.getPageSize(), 1);
        List<RepairOrder> rows = total == 0 ? new ArrayList<RepairOrder>() : findByCondition(query);
        return new PageResult<RepairOrder>(rows, total, pageNum, pageSize);
    }

    @Override
    public List<RepairOrder> findByWorker(Integer workerId, Integer status) {
        return query(session -> mapper(session).findByWorker(workerId, status));
    }

    @Override
    public List<RepairOrder> findByUser(Integer userId) {
        return query(session -> mapper(session).findByUser(userId));
    }

    @Override
    public long countByStatus(Integer status) {
        Long value = query(session -> Long.valueOf(mapper(session).countByStatus(status)));
        return value == null ? 0L : value.longValue();
    }

    @Override
    public long countAll() {
        Long value = query(session -> Long.valueOf(mapper(session).countAll()));
        return value == null ? 0L : value.longValue();
    }

    /** 把 Service 层的 OrderQuery 转换为 Mapper 的查询参数对象（含分页折算） */
    private RepairOrderQuery toMapperQuery(OrderQuery query) {
        RepairOrderQuery target = new RepairOrderQuery();
        if (query == null) {
            target.setLimit(Integer.valueOf(10));
            target.setOffset(Integer.valueOf(0));
            return target;
        }
        target.setOrderId(query.getOrderId());
        target.setUserId(query.getUserId());
        target.setStatus(query.getStatus());
        target.setCategory(query.getCategory());
        target.setBuilding(query.getBuilding());
        target.setPriority(query.getPriority());
        target.setKeyword(query.getKeyword());
        target.setBeginTime(query.getBeginTime());
        target.setEndTime(query.getEndTime());
        target.setWorkerId(query.getWorkerId());
        target.setLimit(Integer.valueOf(Math.max(query.getPageSize(), 1)));
        target.setOffset(Integer.valueOf(query.getOffset()));
        return target;
    }
}
