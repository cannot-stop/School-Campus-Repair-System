package com.campus.repair.dao.mybatis;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import com.campus.repair.dao.MessageDao;
import com.campus.repair.dao.mapper.MessageMapper;
import com.campus.repair.domain.Message;

/**
 * 消息通知数据访问 MyBatis 实现（对应设计书 2.2.6 MessageDao 与表 2.17）。
 * SQL 见 {@code resources/mapper/MessageMapper.xml}。
 */
public class MyBatisMessageDaoImpl extends MyBatisDaoSupport implements MessageDao {

    private MessageMapper mapper(SqlSession session) {
        return session.getMapper(MessageMapper.class);
    }

    @Override
    public int insert(Message message) {
        if (message.getCreateTime() == null) {
            message.setCreateTime(new Date());
        }
        if (message.getIsRead() == null) {
            message.setIsRead(Integer.valueOf(0));
        }
        return mutate(session -> mapper(session).insert(message));
    }

    @Override
    public int markRead(Integer msgId) {
        return mutate(session -> mapper(session).markRead(msgId));
    }

    @Override
    public int markAllRead(Integer userId) {
        return mutate(session -> mapper(session).markAllRead(userId));
    }

    @Override
    public int delete(Integer msgId) {
        return mutate(session -> mapper(session).delete(msgId));
    }

    @Override
    public Message findById(Integer msgId) {
        return query(session -> mapper(session).findById(msgId));
    }

    @Override
    public List<Message> findByUser(Integer userId) {
        return query(session -> mapper(session).findByUser(userId));
    }

    @Override
    public List<Message> findUnreadByUser(Integer userId) {
        return query(session -> mapper(session).findUnreadByUser(userId));
    }

    @Override
    public long countUnread(Integer userId) {
        Long count = query(session -> Long.valueOf(mapper(session).countUnread(userId)));
        return count == null ? 0L : count.longValue();
    }

    @Override
    public List<Message> findAll() {
        return query(session -> mapper(session).findAll());
    }
}
