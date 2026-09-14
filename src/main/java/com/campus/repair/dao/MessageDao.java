package com.campus.repair.dao;

import java.util.List;

import com.campus.repair.domain.Message;

/**
 * 消息通知数据访问接口（对应设计书 2.2.6 MessageDao）。
 */
public interface MessageDao {

    int insert(Message message);

    int markRead(Integer msgId);

    int markAllRead(Integer userId);

    int delete(Integer msgId);

    Message findById(Integer msgId);

    List<Message> findByUser(Integer userId);

    List<Message> findUnreadByUser(Integer userId);

    /** 未读消息数 */
    long countUnread(Integer userId);

    List<Message> findAll();
}
