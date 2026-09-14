package com.campus.repair.dao.memory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.campus.repair.domain.Evaluation;
import com.campus.repair.domain.Material;
import com.campus.repair.domain.MaterialUsage;
import com.campus.repair.domain.Message;
import com.campus.repair.domain.Progress;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.RepairTask;
import com.campus.repair.domain.User;
import com.campus.repair.domain.Worker;

/**
 * 内存数据仓库：集中保存所有实体的内存副本，供各 MemoryXxxDaoImpl 共享。
 *
 * <p>用途：在未部署 MySQL 的环境中运行系统、执行自动化自测与演示；
 * 语义与 schema.sql 中的表结构一一对应（唯一约束、外键关联在 Dao 中实现）。</p>
 */
public final class MemoryStore {

    public static final Map<Integer, User> USERS = new ConcurrentHashMap<Integer, User>();
    public static final Map<Integer, Worker> WORKERS = new ConcurrentHashMap<Integer, Worker>();
    public static final Map<Integer, RepairOrder> ORDERS = new ConcurrentHashMap<Integer, RepairOrder>();
    public static final Map<Integer, RepairTask> TASKS = new ConcurrentHashMap<Integer, RepairTask>();
    public static final Map<Integer, Material> MATERIALS = new ConcurrentHashMap<Integer, Material>();
    public static final Map<Integer, MaterialUsage> USAGES = new ConcurrentHashMap<Integer, MaterialUsage>();
    public static final Map<Integer, Evaluation> EVALUATIONS = new ConcurrentHashMap<Integer, Evaluation>();
    public static final Map<Integer, Message> MESSAGES = new ConcurrentHashMap<Integer, Message>();
    public static final Map<Integer, Progress> PROGRESSES = new ConcurrentHashMap<Integer, Progress>();
    /** 基础数据：类型 → （值 → 排序号） */
    public static final Map<String, Map<String, Integer>> BASE_DATA =
            new ConcurrentHashMap<String, Map<String, Integer>>();

    public static final AtomicInteger USER_SEQ = new AtomicInteger(0);
    public static final AtomicInteger WORKER_SEQ = new AtomicInteger(0);
    public static final AtomicInteger ORDER_SEQ = new AtomicInteger(0);
    public static final AtomicInteger TASK_SEQ = new AtomicInteger(0);
    public static final AtomicInteger MATERIAL_SEQ = new AtomicInteger(0);
    public static final AtomicInteger USAGE_SEQ = new AtomicInteger(0);
    public static final AtomicInteger EVALUATION_SEQ = new AtomicInteger(0);
    public static final AtomicInteger MESSAGE_SEQ = new AtomicInteger(0);
    public static final AtomicInteger PROGRESS_SEQ = new AtomicInteger(0);

    private MemoryStore() {
    }

    /** 清空全部数据（自测重置使用） */
    public static void clear() {
        USERS.clear();
        WORKERS.clear();
        ORDERS.clear();
        TASKS.clear();
        MATERIALS.clear();
        USAGES.clear();
        EVALUATIONS.clear();
        MESSAGES.clear();
        PROGRESSES.clear();
        BASE_DATA.clear();
        USER_SEQ.set(0);
        WORKER_SEQ.set(0);
        ORDER_SEQ.set(0);
        TASK_SEQ.set(0);
        MATERIAL_SEQ.set(0);
        USAGE_SEQ.set(0);
        EVALUATION_SEQ.set(0);
        MESSAGE_SEQ.set(0);
        PROGRESS_SEQ.set(0);
    }

    /** 生成主键：指定 ID 则沿用并推进序列，否则自增 */
    public static int nextId(AtomicInteger seq, Integer specified) {
        if (specified != null) {
            int id = specified.intValue();
            if (id > seq.get()) {
                seq.set(id);
            }
            return id;
        }
        return seq.incrementAndGet();
    }

    /** 基础数据：新增 */
    public static void addBaseData(String type, String value, int sortNo) {
        Map<String, Integer> values = BASE_DATA.get(type);
        if (values == null) {
            values = new LinkedHashMap<String, Integer>();
            BASE_DATA.put(type, values);
        }
        values.put(value, Integer.valueOf(sortNo));
    }

    /** 基础数据：删除 */
    public static boolean removeBaseData(String type, String value) {
        Map<String, Integer> values = BASE_DATA.get(type);
        return values != null && values.remove(value) != null;
    }
}
