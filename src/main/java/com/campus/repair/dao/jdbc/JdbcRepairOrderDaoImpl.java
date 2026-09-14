package com.campus.repair.dao.jdbc;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.campus.repair.common.PageResult;
import com.campus.repair.dao.RepairOrderDao;
import com.campus.repair.domain.OrderQuery;
import com.campus.repair.domain.RepairOrder;

/**
 * 报修单数据访问 JDBC 实现（对应设计书 2.2.6 RepairOrderDao）。
 *
 * <p>查询使用 LEFT JOIN 一次性带出报修人、当前维修任务、维修人员与评价信息，
 * 避免视图层多次查询（对应设计书 2.2.3.2 findByCondition 的按条件分页查询）。</p>
 */
public class JdbcRepairOrderDaoImpl implements RepairOrderDao {

    private static final String COLUMNS =
            "o.order_id, o.user_id, o.building, o.floor, o.room, o.category, o.description, o.images,"
                    + " o.status, o.priority, o.urge_count, o.reject_reason, o.create_time, o.update_time,"
                    + " u.username AS reporter_username, u.real_name AS reporter_name, u.phone AS reporter_phone,"
                    + " t.task_id, t.worker_id, w.name AS worker_name, w.phone AS worker_phone,"
                    + " t.status AS task_status, t.dispatch_time, t.accept_time, t.finish_time,"
                    + " t.result, t.remark,"
                    + " e.score AS eval_score, e.comment AS eval_comment, e.reply AS eval_reply";

    private static final String FROM =
            " FROM t_repair_order o"
                    + " LEFT JOIN t_user u ON u.user_id = o.user_id"
                    + " LEFT JOIN t_repair_task t ON t.task_id = ("
                    + "     SELECT t2.task_id FROM t_repair_task t2 WHERE t2.order_id = o.order_id"
                    + "     AND t2.status NOT IN (4, 5, 6) ORDER BY t2.task_id DESC LIMIT 1)"
                    + " LEFT JOIN t_worker w ON w.worker_id = t.worker_id"
                    + " LEFT JOIN t_evaluation e ON e.order_id = o.order_id";

    @Override
    public int insert(RepairOrder order) {
        String sql = "INSERT INTO t_repair_order (user_id, building, floor, room, category, description, images,"
                + " status, priority, urge_count, create_time, update_time)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int id = JdbcTemplate.insertReturnKey(sql, order.getUserId(), order.getBuilding(), order.getFloor(),
                order.getRoom(), order.getCategory(), order.getDescription(), order.getImages(),
                order.getStatus() == null ? Integer.valueOf(0) : order.getStatus(),
                order.getPriority() == null ? Integer.valueOf(0) : order.getPriority(),
                order.getUrgeCount() == null ? Integer.valueOf(0) : order.getUrgeCount(),
                stamp(order.getCreateTime(), true), stamp(order.getUpdateTime(), false));
        order.setOrderId(Integer.valueOf(id));
        return id > 0 ? 1 : 0;
    }

    @Override
    public int update(RepairOrder order) {
        // 仅更新非空字段，支持 Service 层按需更新（如只调整优先级）
        StringBuilder sql = new StringBuilder("UPDATE t_repair_order SET update_time = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(stamp(new Date(), true));
        if (order.getBuilding() != null) {
            sql.append(", building = ?");
            params.add(order.getBuilding());
        }
        if (order.getFloor() != null) {
            sql.append(", floor = ?");
            params.add(order.getFloor());
        }
        if (order.getRoom() != null) {
            sql.append(", room = ?");
            params.add(order.getRoom());
        }
        if (order.getCategory() != null) {
            sql.append(", category = ?");
            params.add(order.getCategory());
        }
        if (order.getDescription() != null) {
            sql.append(", description = ?");
            params.add(order.getDescription());
        }
        if (order.getImages() != null) {
            sql.append(", images = ?");
            params.add(order.getImages());
        }
        if (order.getStatus() != null) {
            sql.append(", status = ?");
            params.add(order.getStatus());
        }
        if (order.getPriority() != null) {
            sql.append(", priority = ?");
            params.add(order.getPriority());
        }
        sql.append(" WHERE order_id = ?");
        params.add(order.getOrderId());
        return JdbcTemplate.update(sql.toString(), params.toArray());
    }

    @Override
    public int updateStatus(Integer orderId, Integer status) {
        return JdbcTemplate.update("UPDATE t_repair_order SET status = ?, update_time = ? WHERE order_id = ?",
                status, stamp(new Date(), true), orderId);
    }

    @Override
    public int updateReject(Integer orderId, Integer status, String rejectReason) {
        return JdbcTemplate.update("UPDATE t_repair_order SET status = ?, reject_reason = ?, update_time = ?"
                + " WHERE order_id = ?", status, rejectReason, stamp(new Date(), true), orderId);
    }

    @Override
    public int increaseUrgeCount(Integer orderId) {
        return JdbcTemplate.update("UPDATE t_repair_order SET urge_count = COALESCE(urge_count, 0) + 1,"
                + " update_time = ? WHERE order_id = ?", stamp(new Date(), true), orderId);
    }

    @Override
    public int delete(Integer orderId) {
        return JdbcTemplate.update("DELETE FROM t_repair_order WHERE order_id = ?", orderId);
    }

    @Override
    public RepairOrder findById(Integer orderId) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + FROM + " WHERE o.order_id = ?", RepairOrder.class, orderId);
    }

    @Override
    public List<RepairOrder> findByCondition(OrderQuery query) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(FROM).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<Object>();
        buildWhere(query, sql, params, false);
        sql.append(" ORDER BY o.priority DESC, o.create_time DESC");
        if (query != null) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(Integer.valueOf(Math.max(query.getPageSize(), 1)));
            params.add(Integer.valueOf(query.getOffset()));
        }
        return JdbcTemplate.queryList(sql.toString(), RepairOrder.class, params.toArray());
    }

    @Override
    public long countByCondition(OrderQuery query) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*)" + FROM + " WHERE 1 = 1");
        List<Object> params = new ArrayList<Object>();
        buildWhere(query, sql, params, true);
        return JdbcTemplate.queryCount(sql.toString(), params.toArray());
    }

    @Override
    public PageResult<RepairOrder> pageByCondition(OrderQuery query) {
        long total = countByCondition(query);
        List<RepairOrder> rows = total == 0 ? new ArrayList<RepairOrder>() : findByCondition(query);
        int pageNum = query == null ? 1 : Math.max(query.getPageNum(), 1);
        int pageSize = query == null ? 10 : Math.max(query.getPageSize(), 1);
        return new PageResult<RepairOrder>(rows, total, pageNum, pageSize);
    }

    @Override
    public List<RepairOrder> findByWorker(Integer workerId, Integer status) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(FROM)
                .append(" WHERE t.worker_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(workerId);
        if (status != null) {
            sql.append(" AND o.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY o.priority DESC, t.dispatch_time DESC");
        return JdbcTemplate.queryList(sql.toString(), RepairOrder.class, params.toArray());
    }

    @Override
    public List<RepairOrder> findByUser(Integer userId) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + FROM + " WHERE o.user_id = ?"
                + " ORDER BY o.create_time DESC", RepairOrder.class, userId);
    }

    @Override
    public long countByStatus(Integer status) {
        return JdbcTemplate.queryCount("SELECT COUNT(*) FROM t_repair_order WHERE status = ?", status);
    }

    @Override
    public long countAll() {
        return JdbcTemplate.queryCount("SELECT COUNT(*) FROM t_repair_order");
    }

    private void buildWhere(OrderQuery query, StringBuilder sql, List<Object> params, boolean countMode) {
        if (query == null) {
            return;
        }
        if (query.getOrderId() != null) {
            sql.append(" AND o.order_id = ?");
            params.add(query.getOrderId());
        }
        if (query.getUserId() != null) {
            sql.append(" AND o.user_id = ?");
            params.add(query.getUserId());
        }
        if (query.getStatus() != null) {
            sql.append(" AND o.status = ?");
            params.add(query.getStatus());
        }
        if (query.getCategory() != null && !query.getCategory().trim().isEmpty()) {
            sql.append(" AND o.category = ?");
            params.add(query.getCategory().trim());
        }
        if (query.getBuilding() != null && !query.getBuilding().trim().isEmpty()) {
            sql.append(" AND o.building = ?");
            params.add(query.getBuilding().trim());
        }
        if (query.getPriority() != null) {
            sql.append(" AND o.priority = ?");
            params.add(query.getPriority());
        }
        if (query.getWorkerId() != null) {
            sql.append(" AND t.worker_id = ?");
            params.add(query.getWorkerId());
        }
        if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) {
            sql.append(" AND (o.description LIKE ? OR o.room LIKE ? OR o.building LIKE ?)");
            String like = "%" + query.getKeyword().trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (query.getBeginTime() != null) {
            sql.append(" AND o.create_time >= ?");
            params.add(stamp(query.getBeginTime(), true));
        }
        if (query.getEndTime() != null) {
            sql.append(" AND o.create_time <= ?");
            params.add(stamp(query.getEndTime(), true));
        }
    }

    private static Timestamp stamp(Date value, boolean nowIfNull) {
        if (value == null) {
            return nowIfNull ? new Timestamp(System.currentTimeMillis()) : null;
        }
        return new Timestamp(value.getTime());
    }
}
