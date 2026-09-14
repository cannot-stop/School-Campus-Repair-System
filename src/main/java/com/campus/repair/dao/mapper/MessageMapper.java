package com.campus.repair.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.campus.repair.domain.Message;

/**
 * 消息通知 Mapper 接口（对应设计书 2.2.6 MessageDao 与表 2.17）。
 */
public interface MessageMapper {

    int insert(Message message);

    int markRead(@Param("msgId") Integer msgId);

    int markAllRead(@Param("userId") Integer userId);

    int delete(@Param("msgId") Integer msgId);

    Message findById(@Param("msgId") Integer msgId);

    List<Message> findByUser(@Param("userId") Integer userId);

    List<Message> findUnreadByUser(@Param("userId") Integer userId);

    long countUnread(@Param("userId") Integer userId);

    List<Message> findAll();
}
