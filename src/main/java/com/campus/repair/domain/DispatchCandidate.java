package com.campus.repair.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 派单匹配度评分结果（对应设计书 2.2.6 DispatchService.calcMatchScore() 与
 * 设计书 1.1.2.3"人工派单时可查看维修人员当前在单量与技能标签"）。
 *
 * <p>评分模型（总分 100 分）：</p>
 * <ul>
 *   <li>技能匹配 40 分：维修工技能标签包含报修类别得满分，部分匹配（同工种大类）得 20 分；</li>
 *   <li>在单量 30 分：在单量越少得分越高，(10 - 在单量) / 10 × 30；</li>
 *   <li>在线状态 15 分：在线得满分，离线不得分；</li>
 *   <li>紧急单响应 15 分：紧急单且当前无在单任务得满分，紧急单有在单得 8 分，普通单得 10 分。</li>
 * </ul>
 */
public class DispatchCandidate implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MAX_SKILL_SCORE = 40;
    private static final int MAX_LOAD_SCORE = 30;
    private static final int MAX_ONLINE_SCORE = 15;
    private static final int MAX_URGENT_SCORE = 15;
    private static final int LOAD_BASE = 10;

    /** 候选维修人员 */
    private Worker worker;
    /** 总分 */
    private int totalScore;
    /** 技能匹配得分 */
    private int skillScore;
    /** 在单量得分 */
    private int loadScore;
    /** 在线状态得分 */
    private int onlineScore;
    /** 紧急单响应得分 */
    private int urgentScore;
    /** 评分说明（供派单页面展示推荐理由） */
    private List<String> reasons = new ArrayList<String>();

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getSkillScore() {
        return skillScore;
    }

    public void setSkillScore(int skillScore) {
        this.skillScore = skillScore;
    }

    public int getLoadScore() {
        return loadScore;
    }

    public void setLoadScore(int loadScore) {
        this.loadScore = loadScore;
    }

    public int getOnlineScore() {
        return onlineScore;
    }

    public void setOnlineScore(int onlineScore) {
        this.onlineScore = onlineScore;
    }

    public int getUrgentScore() {
        return urgentScore;
    }

    public void setUrgentScore(int urgentScore) {
        this.urgentScore = urgentScore;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    /** 推荐等级文本 */
    public String getLevelText() {
        if (totalScore >= 85) {
            return "强烈推荐";
        }
        if (totalScore >= 70) {
            return "推荐";
        }
        if (totalScore >= 55) {
            return "可用";
        }
        return "不建议";
    }

    /**
     * 计算匹配度得分（纯函数，便于单元自测）。
     *
     * @param worker   候选维修人员
     * @param category 报修类别
     * @param priority 优先级（0普通/1紧急）
     */
    public static DispatchCandidate score(Worker worker, String category, Integer priority) {
        if (worker == null) {
            throw new IllegalArgumentException("候选维修人员不能为空");
        }
        DispatchCandidate candidate = new DispatchCandidate();
        candidate.worker = worker;

        // 1. 技能匹配
        String skills = worker.getSkillTags() == null ? "" : worker.getSkillTags();
        String target = category == null ? "" : category;
        boolean exact = !target.isEmpty() && contains(skills, target);
        boolean partial = !exact && partialMatch(skills, target);
        int skillScore = exact ? MAX_SKILL_SCORE : (partial ? MAX_SKILL_SCORE / 2 : 0);
        candidate.skillScore = skillScore;
        if (exact) {
            candidate.reasons.add("技能完全匹配（" + target + "）");
        } else if (partial) {
            candidate.reasons.add("技能部分匹配（" + skills + "）");
        } else {
            candidate.reasons.add("无匹配技能标签");
        }

        // 2. 在单量
        int current = worker.currentOrderCount();
        int loadScore = (int) Math.round(Math.max(0, LOAD_BASE - current) * MAX_LOAD_SCORE / (double) LOAD_BASE);
        candidate.loadScore = loadScore;
        candidate.reasons.add("当前在单量 " + current + " 单");

        // 3. 在线状态
        candidate.onlineScore = worker.isOnline() ? MAX_ONLINE_SCORE : 0;
        candidate.reasons.add(worker.isOnline() ? "处于在线状态" : "当前离线");

        // 4. 紧急单响应
        boolean urgent = priority != null && priority.intValue() == 1;
        int urgentScore;
        if (urgent) {
            urgentScore = current == 0 ? MAX_URGENT_SCORE : MAX_URGENT_SCORE / 2;
        } else {
            urgentScore = 10;
        }
        candidate.urgentScore = urgentScore;
        if (urgent) {
            candidate.reasons.add("紧急单，需优先响应");
        }

        candidate.totalScore = skillScore + loadScore + candidate.onlineScore + urgentScore;
        return candidate;
    }

    /** 技能标签是否精确包含类别 */
    private static boolean contains(String skillTags, String category) {
        for (String item : skillTags.split(",")) {
            if (item.trim().equals(category)) {
                return true;
            }
        }
        return false;
    }

    /** 部分匹配：类别与技能存在包含关系（如"水电维修"与"水电"） */
    private static boolean partialMatch(String skillTags, String category) {
        for (String item : skillTags.split(",")) {
            String skill = item.trim();
            if (skill.isEmpty() || category.isEmpty()) {
                continue;
            }
            if (skill.contains(category) || category.contains(skill)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "DispatchCandidate{worker=" + (worker == null ? null : worker.getName())
                + ", totalScore=" + totalScore + "}";
    }
}
