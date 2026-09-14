package com.campus.repair.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.PageResult;
import com.campus.repair.common.ResultCode;
import com.campus.repair.common.Validate;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.EvaluationDao;
import com.campus.repair.dao.RepairOrderDao;
import com.campus.repair.domain.AuditStatus;
import com.campus.repair.domain.Evaluation;
import com.campus.repair.domain.OrderStatus;
import com.campus.repair.domain.RepairOrder;
import com.campus.repair.domain.Role;
import com.campus.repair.domain.User;

/**
 * 评价服务（对应设计书 2.2.1 评价与统计分析模块中"报修评价、评价审核回复"）。
 */
public class EvaluationService {

    private final EvaluationDao evaluationDao = DaoFactory.evaluationDao();
    private final RepairOrderDao orderDao = DaoFactory.repairOrderDao();

    /** 提交评价（设计书表 1.1：对维修结果进行评价；评分 1～5 分） */
    public Evaluation submit(Integer userId, Integer orderId, String scoreText, String comment) {
        RepairOrder order = orderDao.findById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报修单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能评价本人提交的报修单");
        }
        if (order.getStatus() == null || order.getStatus().intValue() != OrderStatus.FINISHED.getCode()) {
            throw new BusinessException("仅已完成的报修单可以评价，当前状态：" + order.getStatusText());
        }
        if (evaluationDao.findByOrder(orderId) != null) {
            throw new BusinessException("该报修单已评价，不能重复评价");
        }
        Validate.create()
                .required("score", "评分", scoreText)
                .maxLength("comment", "评价内容", comment, 500)
                .throwIfInvalid();
        int score;
        try {
            score = Integer.parseInt(scoreText.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "评分必须为 1～5 的整数");
        }
        if (score < 1 || score > 5) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "评分必须在 1～5 之间");
        }
        Evaluation evaluation = new Evaluation();
        evaluation.setOrderId(orderId);
        evaluation.setScore(Integer.valueOf(score));
        evaluation.setComment(Validate.trim(comment));
        evaluation.setAuditStatus(Integer.valueOf(AuditStatus.PENDING.getCode()));
        evaluation.setCreateTime(new Date());
        evaluationDao.insert(evaluation);
        new MessageService().sendToManagers("报修单（" + orderId + "）收到新的服务评价（" + score + " 分），请审核。", "评价");
        return evaluationDao.findById(evaluation.getEvalId());
    }

    /** 评价列表（管理员：全部；报修人：本人的；维修工：本人的） */
    public PageResult<Evaluation> query(Integer userId, Map<String, String> params) {
        User user = DaoFactory.userDao().findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态失效，请重新登录");
        }
        List<Evaluation> all;
        if (Role.REPORTER.getCode().equals(user.getRole())) {
            all = new ArrayList<Evaluation>();
            for (RepairOrder order : orderDao.findByUser(userId)) {
                Evaluation evaluation = evaluationDao.findByOrder(order.getOrderId());
                if (evaluation != null) {
                    all.add(evaluation);
                }
            }
        } else if (Role.WORKER.getCode().equals(user.getRole())) {
            com.campus.repair.domain.Worker worker = DaoFactory.workerDao().findByUserId(userId);
            all = worker == null ? new ArrayList<Evaluation>() : evaluationDao.findByWorker(worker.getWorkerId());
        } else {
            all = evaluationDao.findAll();
        }
        // 过滤
        List<Evaluation> filtered = new ArrayList<Evaluation>();
        String auditStatus = params == null ? null : params.get("auditStatus");
        Integer filterStatus = parseInt(auditStatus);
        for (Evaluation evaluation : all) {
            if (filterStatus != null && !filterStatus.equals(evaluation.getAuditStatus())) {
                continue;
            }
            filtered.add(evaluation);
        }
        int pageNum = params == null ? 1 : intOrDefault(params.get("pageNum"), 1);
        int pageSize = params == null ? 10 : intOrDefault(params.get("pageSize"), 10);
        int from = Math.min(Math.max(pageNum - 1, 0) * Math.max(pageSize, 1), filtered.size());
        int to = Math.min(from + Math.max(pageSize, 1), filtered.size());
        return new PageResult<Evaluation>(new ArrayList<Evaluation>(filtered.subList(from, to)),
                filtered.size(), pageNum, pageSize);
    }

    /** 管理员回复评价 */
    public Evaluation reply(Integer operatorId, Integer evalId, String reply) {
        requireManager(operatorId);
        Evaluation evaluation = requireEvaluation(evalId);
        Validate.create()
                .required("reply", "回复内容", reply)
                .maxLength("reply", "回复内容", reply, 500)
                .throwIfInvalid();
        evaluationDao.updateReply(evalId, reply.trim());
        RepairOrder order = orderDao.findById(evaluation.getOrderId());
        if (order != null) {
            new MessageService().send(order.getUserId(), "您对报修单（" + order.getOrderId()
                    + "）的评价已收到管理员回复：" + reply.trim(), "评价");
        }
        return evaluationDao.findById(evalId);
    }

    /** 评价审核（对应设计书 StatController.auditEval()） */
    public Evaluation audit(Integer operatorId, Integer evalId, Integer auditStatus, String reason) {
        requireManager(operatorId);
        Evaluation evaluation = requireEvaluation(evalId);
        if (auditStatus == null || (auditStatus.intValue() != AuditStatus.PASSED.getCode()
                && auditStatus.intValue() != AuditStatus.REJECTED.getCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审核结果只能为通过或驳回");
        }
        if (auditStatus.intValue() == AuditStatus.REJECTED.getCode() && Validate.isBlank(reason)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "驳回评价时必须填写原因");
        }
        evaluationDao.updateAuditStatus(evalId, auditStatus);
        RepairOrder order = orderDao.findById(evaluation.getOrderId());
        if (order != null) {
            String text = auditStatus.intValue() == AuditStatus.PASSED.getCode()
                    ? "您对报修单（" + order.getOrderId() + "）的评价已通过审核。"
                    : "您对报修单（" + order.getOrderId() + "）的评价未通过审核：" + reason;
            new MessageService().send(order.getUserId(), text, "评价");
        }
        return evaluationDao.findById(evalId);
    }

    /** 待审核评价列表 */
    public List<Evaluation> pendingAudit() {
        return evaluationDao.findByAuditStatus(Integer.valueOf(AuditStatus.PENDING.getCode()));
    }

    /** 评价统计 */
    public Map<String, Object> statistics() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("total", Long.valueOf(evaluationDao.countAll()));
        data.put("avgScore", Double.valueOf(evaluationDao.avgScore()));
        List<Evaluation> all = evaluationDao.findAll();
        int[] distribution = new int[5];
        for (Evaluation evaluation : all) {
            if (evaluation.getScore() != null && Integer.valueOf(1).equals(evaluation.getAuditStatus())) {
                int score = evaluation.getScore().intValue();
                if (score >= 1 && score <= 5) {
                    distribution[score - 1]++;
                }
            }
        }
        List<Map<String, Object>> dist = new ArrayList<Map<String, Object>>();
        for (int i = 5; i >= 1; i--) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("score", Integer.valueOf(i));
            row.put("count", Integer.valueOf(distribution[i - 1]));
            dist.add(row);
        }
        data.put("distribution", dist);
        data.put("pendingAudit", Integer.valueOf(pendingAudit().size()));
        return data;
    }

    private Evaluation requireEvaluation(Integer evalId) {
        Evaluation evaluation = evaluationDao.findById(evalId);
        if (evaluation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        return evaluation;
    }

    private void requireManager(Integer operatorId) {
        User operator = DaoFactory.userDao().findById(operatorId);
        if (operator == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态失效，请重新登录");
        }
        if (!Role.MANAGER.getCode().equals(operator.getRole()) && !Role.ADMIN.getCode().equals(operator.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅维修管理员可执行该操作");
        }
    }

    private Integer parseInt(String value) {
        if (Validate.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int intOrDefault(String value, int defaultValue) {
        Integer parsed = parseInt(value);
        return parsed == null ? defaultValue : parsed.intValue();
    }
}
