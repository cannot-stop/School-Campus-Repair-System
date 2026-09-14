package com.campus.repair.service;

import java.util.Date;
import java.util.List;

import com.campus.repair.common.BusinessException;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.MessageDao;
import com.campus.repair.dao.UserDao;
import com.campus.repair.domain.Message;
import com.campus.repair.domain.MessageType;
import com.campus.repair.domain.Role;
import com.campus.repair.domain.User;

/**
 * 消息通知服务（对应设计书 2.2.6 MessageDao 与各功能点的"通知相关人员"要求）。
 */
public class MessageService {

    private final MessageDao messageDao = DaoFactory.messageDao();
    private final UserDao userDao = DaoFactory.userDao();

    /** 推送一条消息 */
    public Message send(Integer userId, String content, String msgType) {
        if (userId == null || content == null || content.trim().isEmpty()) {
            return null;
        }
        Message message = new Message();
        message.setUserId(userId);
        message.setContent(content);
        message.setMsgType(msgType == null ? MessageType.SYSTEM : msgType);
        message.setIsRead(Integer.valueOf(0));
        message.setCreateTime(new Date());
        messageDao.insert(message);
        return message;
    }

    /** 按角色群发（如通知所有维修管理员） */
    public int sendToRole(String roleCode, String content, String msgType) {
        int count = 0;
        for (User user : userDao.findByRole(roleCode)) {
            send(user.getUserId(), content, msgType);
            count++;
        }
        return count;
    }

    /** 通知全部维修管理员与系统管理员 */
    public int sendToManagers(String content, String msgType) {
        int count = sendToRole(Role.MANAGER.getCode(), content, msgType);
        count += sendToRole(Role.ADMIN.getCode(), content, msgType);
        return count;
    }

    /** 我的消息（最多最近 200 条） */
    public List<Message> myMessages(Integer userId) {
        List<Message> list = messageDao.findByUser(userId);
        if (list.size() > 200) {
            return list.subList(0, 200);
        }
        return list;
    }

    /** 未读消息数 */
    public long unreadCount(Integer userId) {
        return messageDao.countUnread(userId);
    }

    /** 标记已读 */
    public void markRead(Integer userId, Integer msgId) {
        Message message = messageDao.findById(msgId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        if (!userId.equals(message.getUserId())) {
            throw new BusinessException("只能操作本人的消息");
        }
        messageDao.markRead(msgId);
    }

    /** 全部标记已读 */
    public int markAllRead(Integer userId) {
        return messageDao.markAllRead(userId);
    }
}
