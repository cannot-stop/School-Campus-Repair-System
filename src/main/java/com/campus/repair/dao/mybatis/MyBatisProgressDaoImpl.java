package com.campus.repair.dao.mybatis;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import com.campus.repair.dao.ProgressDao;
import com.campus.repair.dao.mapper.ProgressMapper;
import com.campus.repair.domain.Progress;

/**
 * 维修进度反馈数据访问 MyBatis 实现。
 * SQL 见 {@code resources/mapper/ProgressMapper.xml}。
 */
public class MyBatisProgressDaoImpl extends MyBatisDaoSupport implements ProgressDao {

    private ProgressMapper mapper(SqlSession session) {
        return session.getMapper(ProgressMapper.class);
    }

    @Override
    public int insert(Progress progress) {
        if (progress.getCreateTime() == null) {
            progress.setCreateTime(new Date());
        }
        return mutate(session -> mapper(session).insert(progress));
    }

    @Override
    public int deleteByTask(Integer taskId) {
        return mutate(session -> mapper(session).deleteByTask(taskId));
    }

    @Override
    public List<Progress> findByTask(Integer taskId) {
        return query(session -> mapper(session).findByTask(taskId));
    }

    @Override
    public List<Progress> findAll() {
        return query(session -> mapper(session).findAll());
    }
}
