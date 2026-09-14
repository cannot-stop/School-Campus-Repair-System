package com.campus.repair.service;

import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.jdbc.Database;

/**
 * 事务模板（对应设计书 1.1.2.4"库存扣减需保证事务一致性"）。
 *
 * <p>storage.mode=jdbc 时开启真实数据库事务；内存模式（演示/自测）下为空实现，
 * 业务代码无需区分环境。</p>
 */
public final class TxTemplate {

    private TxTemplate() {
    }

    /** 事务回调 */
    public interface Action<T> {
        T run();
    }

    /** 在事务中执行 */
    public static <T> T execute(Action<T> action) {
        boolean jdbc = !DaoFactory.isMemoryMode();
        if (jdbc) {
            try {
                Database.begin();
            } catch (Exception e) {
                throw new IllegalStateException("事务开启失败：" + e.getMessage(), e);
            }
        }
        try {
            T result = action.run();
            if (jdbc) {
                Database.commit();
            }
            return result;
        } catch (RuntimeException e) {
            if (jdbc) {
                Database.rollback();
            }
            throw e;
        } catch (Error e) {
            if (jdbc) {
                Database.rollback();
            }
            throw e;
        }
    }
}
