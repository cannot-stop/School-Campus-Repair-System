package com.campus.repair.service;

import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.jdbc.Database;
import com.campus.repair.dao.mybatis.MyBatisSessionFactory;

/**
 * 事务模板（对应设计书 1.1.2.4"库存扣减需保证事务一致性"）。
 *
 * <p>按当前存储模式选择事务实现：</p>
 * <ul>
 *   <li>{@code mybatis}：{@link MyBatisSessionFactory} 的 SqlSession 事务
 *       （同一线程内复用同一 SqlSession 与数据库连接）；</li>
 *   <li>{@code jdbc}：{@link Database} 的 JDBC 事务；</li>
 *   <li>{@code memory}：空实现（演示与自测环境）。</li>
 * </ul>
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
        boolean mybatis = DaoFactory.isMyBatisMode();
        boolean jdbc = DaoFactory.isJdbcMode();
        if (mybatis) {
            MyBatisSessionFactory.begin();
        } else if (jdbc) {
            try {
                Database.begin();
            } catch (Exception e) {
                throw new IllegalStateException("事务开启失败：" + e.getMessage(), e);
            }
        }
        try {
            T result = action.run();
            if (mybatis) {
                MyBatisSessionFactory.commit();
            } else if (jdbc) {
                Database.commit();
            }
            return result;
        } catch (RuntimeException e) {
            rollback(mybatis, jdbc);
            throw e;
        } catch (Error e) {
            rollback(mybatis, jdbc);
            throw e;
        }
    }

    private static void rollback(boolean mybatis, boolean jdbc) {
        if (mybatis) {
            MyBatisSessionFactory.rollback();
        } else if (jdbc) {
            Database.rollback();
        }
    }
}
