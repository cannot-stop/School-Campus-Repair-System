package com.campus.repair.web.controller;

import java.util.HashMap;
import java.util.Map;

import com.campus.repair.web.RequestContext;
import com.campus.repair.web.Route;
import com.campus.repair.web.View;

/**
 * 消息通知控制器（对应 t_message 与各功能点"通知相关人员"要求）。
 */
public class MessageController extends BaseController {

    /** 我的消息 */
    @Route("/api/message/list")
    public View list(RequestContext ctx) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("messages", messageService.myMessages(ctx.getUserId()));
        data.put("unread", Long.valueOf(messageService.unreadCount(ctx.getUserId())));
        return View.ok(data);
    }

    /** 未读消息数 */
    @Route("/api/message/unread")
    public View unread(RequestContext ctx) {
        return View.ok(Long.valueOf(messageService.unreadCount(ctx.getUserId())));
    }

    /** 标记已读 */
    @Route("/api/message/read")
    public View read(RequestContext ctx) {
        messageService.markRead(ctx.getUserId(), ctx.requireIntParam("msgId"));
        return success("已标记为已读", null);
    }

    /** 全部标记已读 */
    @Route("/api/message/readAll")
    public View readAll(RequestContext ctx) {
        int count = messageService.markAllRead(ctx.getUserId());
        return success("已将 " + count + " 条消息标记为已读", Integer.valueOf(count));
    }
}
