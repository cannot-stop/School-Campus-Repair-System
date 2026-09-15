package com.campus.repair.dao.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.campus.repair.common.PageResult;
import com.campus.repair.domain.OrderQuery;
import com.campus.repair.domain.RepairOrder;

/**
 * 内存查询辅助器：对内存中的实体集合执行与 MySQL SQL 等价的过滤、排序与分页。
 *
 * <p>内存实现与 JDBC 实现共享同一份查询语义（例如"报修人只能查询本人报修单""列表分页展示"），
 * 避免两套 DAO 出现行为差异。</p>
 */
public final class MemoryQuery {

    private MemoryQuery() {
    }

    /** 按条件过滤报修单（与 RepairOrderMapper 的 WHERE 条件语义一致） */
    public static List<RepairOrder> filterOrders(List<RepairOrder> source, OrderQuery query) {
        List<RepairOrder> result = new ArrayList<RepairOrder>();
        if (source == null) {
            return result;
        }
        for (RepairOrder order : source) {
            if (query == null) {
                result.add(order);
                continue;
            }
            if (query.getOrderId() != null && !query.getOrderId().equals(order.getOrderId())) {
                continue;
            }
            if (query.getUserId() != null && !query.getUserId().equals(order.getUserId())) {
                continue;
            }
            if (query.getStatus() != null && !query.getStatus().equals(order.getStatus())) {
                continue;
            }
            if (!blank(query.getCategory()) && !query.getCategory().equals(order.getCategory())) {
                continue;
            }
            if (!blank(query.getBuilding()) && !query.getBuilding().equals(order.getBuilding())) {
                continue;
            }
            if (query.getPriority() != null && !query.getPriority().equals(order.getPriority())) {
                continue;
            }
            if (!blank(query.getKeyword())) {
                String keyword = query.getKeyword().trim();
                String description = order.getDescription() == null ? "" : order.getDescription();
                String room = order.getRoom() == null ? "" : order.getRoom();
                String location = order.getLocation();
                if (!description.contains(keyword) && !room.contains(keyword) && !location.contains(keyword)) {
                    continue;
                }
            }
            if (query.getBeginTime() != null && (order.getCreateTime() == null
                    || order.getCreateTime().before(query.getBeginTime()))) {
                continue;
            }
            if (query.getEndTime() != null && (order.getCreateTime() == null
                    || order.getCreateTime().after(query.getEndTime()))) {
                continue;
            }
            result.add(order);
        }
        result.sort(new Comparator<RepairOrder>() {
            @Override
            public int compare(RepairOrder a, RepairOrder b) {
                int pa = a.getPriority() == null ? 0 : a.getPriority().intValue();
                int pb = b.getPriority() == null ? 0 : b.getPriority().intValue();
                if (pa != pb) {
                    return pb - pa;
                }
                Date ta = a.getCreateTime();
                Date tb = b.getCreateTime();
                if (ta == null || tb == null) {
                    return 0;
                }
                return tb.compareTo(ta);
            }
        });
        return result;
    }

    /** 分页 */
    public static <T> PageResult<T> page(List<T> source, int pageNum, int pageSize) {
        int size = Math.max(pageSize, 1);
        int num = Math.max(pageNum, 1);
        int from = Math.min((num - 1) * size, source.size());
        int to = Math.min(from + size, source.size());
        return new PageResult<T>(new ArrayList<T>(source.subList(from, to)), source.size(), num, size);
    }

    /** 过滤 */
    public static <T> List<T> filter(List<T> source, Predicate<T> predicate) {
        List<T> result = new ArrayList<T>();
        if (source == null) {
            return result;
        }
        for (T item : source) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** 简单判定接口 */
    public interface Predicate<T> {
        boolean test(T item);
    }
}
