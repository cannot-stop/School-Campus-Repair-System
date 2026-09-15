package com.campus.repair.service;

import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.mybatis.MyBatisSessionFactory;

/**
 * 事务模板（对应设计书 1.1.2.4"库存扣减需保证事务一致性"）。
 *
 * <p>按当前存储模式选择事务实现：</p>
 * <ul>
 *   <li>{@code mybatis}：{@link MyBatisSessionFactory} 的 SqlSession 事务
 *       （同一线程内复用同一 SqlSession 与数据库连接）；</li>
 *   <li>{@code memory}：空实现（演示与自测环境，内存库无事务语义）。</li>
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
        if (!DaoFactory.isMyBatisMode()) {
            // 内存库模式：没有事务语义，直接执行
            return action.run();
        }
        MyBatisSessionFactory.begin();
        try {
            T result = action.run();
            MyBatisSessionFactory.commit();
            return result;
        } catch (RuntimeException e) {
            MyBatisSessionFactory.rollback();
            throw e;
        } catch (Error e) {
            MyBatisSessionFactory.rollback();
            throw e;
        }
    }
}
