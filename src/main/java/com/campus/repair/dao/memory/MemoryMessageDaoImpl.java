package com.campus.repair.dao.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.campus.repair.dao.MessageDao;
import com.campus.repair.domain.Message;

/**
 * 消息通知数据访问内存实现。
 */
public class MemoryMessageDaoImpl implements MessageDao {

    @Override
    public int insert(Message message) {
        int id = MemoryStore.nextId(MemoryStore.MESSAGE_SEQ, message.getMsgId());
        message.setMsgId(Integer.valueOf(id));
        if (message.getCreateTime() == null) {
            message.setCreateTime(new Date());
        }
        if (message.getIsRead() == null) {
            message.setIsRead(Integer.valueOf(0));
        }
        MemoryStore.MESSAGES.put(Integer.valueOf(id), message);
        return 1;
    }

    @Override
    public int markRead(Integer msgId) {
        Message exists = MemoryStore.MESSAGES.get(msgId);
        if (exists == null) {
            return 0;
        }
        exists.setIsRead(Integer.valueOf(1));
        return 1;
    }

    @Override
    public int markAllRead(Integer userId) {
        int count = 0;
        for (Message message : MemoryStore.MESSAGES.values()) {
            if (userId.equals(message.getUserId()) && !message.isReadFlag()) {
                message.setIsRead(Integer.valueOf(1));
                count++;
            }
        }
        return count;
    }

    @Override
    public int delete(Integer msgId) {
        return MemoryStore.MESSAGES.remove(msgId) == null ? 0 : 1;
    }

    @Override
    public Message findById(Integer msgId) {
        return MemoryStore.MESSAGES.get(msgId);
    }

    @Override
    public List<Message> findByUser(Integer userId) {
        List<Message> result = new ArrayList<Message>();
        for (Message message : MemoryStore.MESSAGES.values()) {
            if (userId.equals(message.getUserId())) {
                result.add(message);
            }
        }
        return sortDesc(result);
    }

    @Override
    public List<Message> findUnreadByUser(Integer userId) {
        List<Message> result = new ArrayList<Message>();
        for (Message message : MemoryStore.MESSAGES.values()) {
            if (userId.equals(message.getUserId()) && !message.isReadFlag()) {
                result.add(message);
            }
        }
        return sortDesc(result);
    }

    @Override
    public long countUnread(Integer userId) {
        long count = 0;
        for (Message message : MemoryStore.MESSAGES.values()) {
            if (userId.equals(message.getUserId()) && !message.isReadFlag()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<Message> findAll() {
        return sortDesc(new ArrayList<Message>(MemoryStore.MESSAGES.values()));
    }

    private static List<Message> sortDesc(List<Message> source) {
        source.sort(new Comparator<Message>() {
            @Override
            public int compare(Message a, Message b) {
                return b.getMsgId().compareTo(a.getMsgId());
            }
        });
        return source;
    }
}
