package com.campus.repair.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.campus.repair.domain.User;

/**
 * 会话管理器（内存实现；生产环境可替换为 Redis 等集中式存储）。
 */
public final class SessionManager {

    /** 会话空闲超时（毫秒）：2 小时无操作自动失效 */
    private static final long IDLE_TIMEOUT = 2 * 3600 * 1000L;

    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<String, Session>();

    private SessionManager() {
    }

    /** 创建会话 */
    public static Session create(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Session session = new Session(token, user);
        SESSIONS.put(token, session);
        return session;
    }

    /** 获取会话（自动清理过期会话） */
    public static Session get(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        Session session = SESSIONS.get(token);
        if (session == null) {
            return null;
        }
        if (session.getIdleMillis() > IDLE_TIMEOUT) {
            SESSIONS.remove(token);
            return null;
        }
        session.touch();
        return session;
    }

    /** 注销会话 */
    public static void invalidate(String token) {
        if (token != null) {
            SESSIONS.remove(token);
        }
    }

    /** 清理超时会话 */
    public static int cleanExpired() {
        int count = 0;
        Iterator<Map.Entry<String, Session>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Session> entry = iterator.next();
            if (entry.getValue().getIdleMillis() > IDLE_TIMEOUT) {
                iterator.remove();
                count++;
            }
        }
        return count;
    }

    /** 当前在线会话数 */
    public static int onlineCount() {
        cleanExpired();
        return SESSIONS.size();
    }

    /** 在线用户列表（管理员查看） */
    public static List<Session> onlineSessions() {
        cleanExpired();
        return new ArrayList<Session>(SESSIONS.values());
    }

    /** 清空（自测使用） */
    public static void clear() {
        SESSIONS.clear();
    }
}
