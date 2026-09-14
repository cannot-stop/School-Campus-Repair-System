package com.campus.repair.dao.jdbc;

import java.util.List;

import com.campus.repair.dao.MessageDao;
import com.campus.repair.domain.Message;

/**
 * 消息通知数据访问 JDBC 实现（对应设计书 2.2.6 MessageDao）。
 */
public class JdbcMessageDaoImpl implements MessageDao {

    private static final String COLUMNS = "msg_id, user_id, content, msg_type, is_read, create_time";

    @Override
    public int insert(Message message) {
        String sql = "INSERT INTO t_message (user_id, content, msg_type, is_read, create_time) VALUES (?, ?, ?, ?, ?)";
        int id = JdbcTemplate.insertReturnKey(sql, message.getUserId(), message.getContent(), message.getMsgType(),
                message.getIsRead() == null ? Integer.valueOf(0) : message.getIsRead(),
                new java.sql.Timestamp(message.getCreateTime() == null
                        ? System.currentTimeMillis() : message.getCreateTime().getTime()));
        message.setMsgId(Integer.valueOf(id));
        return id > 0 ? 1 : 0;
    }

    @Override
    public int markRead(Integer msgId) {
        return JdbcTemplate.update("UPDATE t_message SET is_read = 1 WHERE msg_id = ?", msgId);
    }

    @Override
    public int markAllRead(Integer userId) {
        return JdbcTemplate.update("UPDATE t_message SET is_read = 1 WHERE user_id = ? AND is_read = 0", userId);
    }

    @Override
    public int delete(Integer msgId) {
        return JdbcTemplate.update("DELETE FROM t_message WHERE msg_id = ?", msgId);
    }

    @Override
    public Message findById(Integer msgId) {
        return JdbcTemplate.queryOne("SELECT " + COLUMNS + " FROM t_message WHERE msg_id = ?", Message.class, msgId);
    }

    @Override
    public List<Message> findByUser(Integer userId) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_message WHERE user_id = ? ORDER BY create_time DESC, msg_id DESC",
                Message.class, userId);
    }

    @Override
    public List<Message> findUnreadByUser(Integer userId) {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_message WHERE user_id = ? AND is_read = 0"
                + " ORDER BY create_time DESC, msg_id DESC", Message.class, userId);
    }

    @Override
    public long countUnread(Integer userId) {
        return JdbcTemplate.queryCount("SELECT COUNT(*) FROM t_message WHERE user_id = ? AND is_read = 0", userId);
    }

    @Override
    public List<Message> findAll() {
        return JdbcTemplate.queryList("SELECT " + COLUMNS + " FROM t_message ORDER BY msg_id DESC", Message.class);
    }
}
