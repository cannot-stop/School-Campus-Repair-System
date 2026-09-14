package com.campus.repair.dao.mybatis;

import org.apache.ibatis.session.SqlSession;

/**
 * MyBatis Dao 实现的公共基类。
 *
 * <p>统一提供会话获取与事务语义：DAO 自身不持有数据库连接，
 * 事务范围由 {@link MyBatisSessionFactory} 的 begin/commit/rollback
 * （配合 Service 层的 TxTemplate）决定；无事务时读写操作各自独立提交。</p>
 */
public abstract class MyBatisDaoSupport {

    /** 读取操作：无事务时自动关闭会话 */
    protected <T> T query(MyBatisSessionFactory.SessionCallback<T> callback) {
        return MyBatisSessionFactory.query(callback);
    }

    /** 写入操作：无事务时自动提交、异常自动回滚，最后关闭会话 */
    protected <T> T mutate(MyBatisSessionFactory.SessionCallback<T> callback) {
        return MyBatisSessionFactory.mutate(callback);
    }

    /** 当前会话（需要直接获取 Mapper 时使用） */
    protected SqlSession session() {
        return MyBatisSessionFactory.session();
    }
}
