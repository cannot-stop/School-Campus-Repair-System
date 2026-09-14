package com.campus.repair.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.ResultCode;
import com.campus.repair.common.Validate;
import com.campus.repair.dao.BaseDataDao;
import com.campus.repair.dao.DaoFactory;
import com.campus.repair.dao.MessageDao;
import com.campus.repair.dao.UserDao;
import com.campus.repair.dao.WorkerDao;
import com.campus.repair.domain.AuditStatus;
import com.campus.repair.domain.Message;
import com.campus.repair.domain.MessageType;
import com.campus.repair.domain.Role;
import com.campus.repair.domain.User;
import com.campus.repair.domain.Worker;
import com.campus.repair.util.PasswordUtil;

/**
 * 账户业务服务（对应设计书 2.2.6 AccountService 与 2.2.2 账户管理模块设计）。
 *
 * <p>职责：账户注册、登录、注销、账户信息维护（手机号/密码）、消息配置、账户审核。</p>
 */
public class AccountService {

    /** 登录失败次数上限，超过则锁定账户（设计书 2.2.2.2"达到阈值后锁定账户"） */
    private static final int MAX_LOGIN_FAIL = 5;

    private final UserDao userDao = DaoFactory.userDao();
    private final WorkerDao workerDao = DaoFactory.workerDao();
    private final MessageDao messageDao = DaoFactory.messageDao();
    private final BaseDataDao baseDataDao = DaoFactory.baseDataDao();

    /** 登录失败计数（内存态，进程级） */
    private static final Map<String, Integer> FAIL_COUNTER = new HashMap<String, Integer>();

    // ------------------------------------------------------------------ 注册

    /**
     * 账户注册（对应设计书表 1.2 账户注册功能点）。
     *
     * <p>主事件流：校验必填项 → 校验用户名唯一性、手机号格式 → 密码加密保存 → 状态置为待审核 → 提示注册成功。</p>
     */
    public User register(Map<String, String> form) {
        return doRegister(form, true);
    }

    /** 注册（仅自测使用，可跳过密码强度校验以构造历史弱密码数据） */
    public User registerForTest(Map<String, String> form) {
        return doRegister(form, false);
    }

    private User doRegister(Map<String, String> form, boolean checkStrength) {
        String username = Validate.trim(form.get("username"));
        String password = form.get("password");
        String realName = Validate.trim(form.get("realName"));
        String studentNo = Validate.trim(form.get("studentNo"));
        String phone = Validate.trim(form.get("phone"));
        String role = Validate.trim(form.get("role"));
        String skillTags = Validate.trim(form.get("skillTags"));

        Validate.create()
                .required("username", "用户名", username)
                .maxLength("username", "用户名", username, 50)
                .required("password", "密码", password)
                .required("realName", "真实姓名", realName)
                .required("studentNo", "学号/工号", studentNo)
                .required("phone", "手机号", phone)
                .phone("phone", phone)
                .required("role", "用户角色", role)
                .throwIfInvalid();

        if (checkStrength && PasswordUtil.isWeak(password)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, PasswordUtil.strengthTip());
        }
        Role userRole = Role.of(role);
        if (userRole == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户角色不合法");
        }
        if (userRole == Role.ADMIN) {
            throw new BusinessException("系统管理员账户由系统预置，不能自助注册");
        }
        if (userRole == Role.WORKER && Validate.isBlank(skillTags)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "维修人员注册需填写技能标签，如：水电,木工");
        }
        if (userDao.findByUsername(username) != null) {
            throw new BusinessException("用户名已存在，请更换后重试");
        }
        if (userDao.findByPhone(phone) != null) {
            throw new BusinessException("该手机号已注册，同一手机号只能注册一个账户");
        }
        if (userDao.findByStudentNo(studentNo) != null) {
            throw new BusinessException("该学号/工号已注册，同一学号/工号只能注册一个账户");
        }

        return TxTemplate.execute(new TxTemplate.Action<User>() {
            @Override
            public User run() {
                User user = new User();
                user.setUsername(username);
                user.setPassword(PasswordUtil.encrypt(password));
                user.setRealName(realName);
                user.setStudentNo(studentNo);
                user.setPhone(phone);
                user.setRole(role);
                user.setAuditStatus(Integer.valueOf(AuditStatus.PENDING.getCode()));
                user.setIsLocked(Integer.valueOf(0));
                user.setCreateTime(new Date());
                userDao.insert(user);

                // 维修人员同时建立档案，便于派单时按技能与在单量匹配
                if (userRole == Role.WORKER) {
                    Worker worker = new Worker();
                    worker.setUserId(user.getUserId());
                    worker.setName(realName);
                    worker.setPhone(phone);
                    worker.setSkillTags(skillTags);
                    worker.setCurrentOrders(Integer.valueOf(0));
                    worker.setLocation("维修间");
                    worker.setStatus(Integer.valueOf(1));
                    workerDao.insert(worker);
                }

                // 通知系统管理员审核（表 1.2：系统管理员人工审核用户注册信息）
                List<User> admins = userDao.findByRole(Role.ADMIN.getCode());
                for (User admin : admins) {
                    sendMessage(admin.getUserId(), "用户「" + username + "（" + realName + "）提交了实名审核申请，请及时审核。",
                            MessageType.AUDIT);
                }
                return user;
            }
        });
    }

    // ------------------------------------------------------------------ 登录

    /**
     * 登录（对应设计书表 1.3 登录功能点）。
     *
     * <p>主事件流：校验用户名密码 → 校验审核状态与锁定状态 → 创建会话并返回用户。</p>
     */
    public User login(String username, String password) {
        Validate.create()
                .required("username", "用户名", username)
                .required("password", "密码", password)
                .throwIfInvalid();

        User user = userDao.findByUsername(username.trim());
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            int fails = recordFail(username.trim());
            int remain = Math.max(MAX_LOGIN_FAIL - fails, 0);
            if (fails >= MAX_LOGIN_FAIL && user != null) {
                userDao.updateLocked(user.getUserId(), Integer.valueOf(1));
                throw new BusinessException("密码错误次数过多，账户已锁定，请联系系统管理员解锁");
            }
            throw new BusinessException("用户名或密码错误" + (remain > 0 ? "（剩余尝试次数 " + remain + "）" : ""));
        }
        if (user.isLockedFlag()) {
            throw new BusinessException("账户已被锁定，请联系系统管理员解锁");
        }
        if (!user.isAudited()) {
            if (Integer.valueOf(AuditStatus.REJECTED.getCode()).equals(user.getAuditStatus())) {
                throw new BusinessException("账户审核未通过，请联系系统管理员核实实名信息");
            }
            throw new BusinessException("账户正在审核中，审核通过后方可登录");
        }
        FAIL_COUNTER.remove(username.trim());
        return user;
    }

    /** 注销（设计书表 1.4：退出当前登录账户，结束会话） */
    public void logout(Integer userId) {
        if (userId == null) {
            return;
        }
        User user = userDao.findById(userId);
        if (user != null && Role.WORKER.getCode().equals(user.getRole())) {
            Worker worker = workerDao.findByUserId(userId);
            if (worker != null) {
                workerDao.updateStatus(worker.getWorkerId(), Integer.valueOf(0));
            }
        }
    }

    /** 登录成功后同步维修工在线状态与位置（对应 t_worker.status/location） */
    public void markOnline(Integer userId, String location) {
        User user = userDao.findById(userId);
        if (user == null || !Role.WORKER.getCode().equals(user.getRole())) {
            return;
        }
        Worker worker = workerDao.findByUserId(userId);
        if (worker == null) {
            return;
        }
        workerDao.updateStatus(worker.getWorkerId(), Integer.valueOf(1));
        if (location != null && !location.trim().isEmpty()) {
            workerDao.updateLocation(worker.getWorkerId(), location.trim());
        }
    }

    // ------------------------------------------------------ 账户信息维护

    /** 修改手机号 */
    public User modifyInfo(Integer userId, String phone, String realName) {
        User user = requireUser(userId);
        Validate.create()
                .required("phone", "手机号", phone)
                .phone("phone", phone)
                .required("realName", "真实姓名", realName)
                .throwIfInvalid();
        User other = userDao.findByPhone(phone.trim());
        if (other != null && !other.getUserId().equals(userId)) {
            throw new BusinessException("该手机号已被其他账户使用");
        }
        user.setPhone(phone.trim());
        user.setRealName(realName.trim());
        userDao.update(user);
        return userDao.findById(userId);
    }

    /** 修改密码（表 1.4：修改密码需验证原密码） */
    public void modifyPassword(Integer userId, String oldPassword, String newPassword, String confirmPassword) {
        User user = requireUser(userId);
        Validate.create()
                .required("oldPassword", "原密码", oldPassword)
                .required("newPassword", "新密码", newPassword)
                .throwIfInvalid();
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (PasswordUtil.isWeak(newPassword)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, PasswordUtil.strengthTip());
        }
        if (confirmPassword != null && !newPassword.equals(confirmPassword)) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        if (PasswordUtil.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        userDao.updatePassword(userId, PasswordUtil.encrypt(newPassword));
    }

    // ---------------------------------------------------------- 账户审核

    /** 待审核账户列表 */
    public List<User> listPendingAudit() {
        return userDao.findByAuditStatus(Integer.valueOf(AuditStatus.PENDING.getCode()));
    }

    /** 全部账户列表（可按角色过滤） */
    public List<User> listUsers(String role) {
        if (Validate.isBlank(role)) {
            return userDao.findAll();
        }
        return userDao.findByRole(role.trim());
    }

    /**
     * 账户实名审核（对应设计书 2.2.2.3 账户审核功能点）。
     *
     * @param auditStatus 1 通过 / 2 驳回
     */
    public User auditUser(Integer operatorId, Integer userId, Integer auditStatus, String reason) {
        requireAdmin(operatorId);
        User user = requireUser(userId);
        if (auditStatus == null
                || (auditStatus.intValue() != AuditStatus.PASSED.getCode()
                && auditStatus.intValue() != AuditStatus.REJECTED.getCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审核结果只能为通过或驳回");
        }
        if (auditStatus.intValue() == AuditStatus.REJECTED.getCode() && Validate.isBlank(reason)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "驳回时必须填写驳回原因");
        }
        userDao.updateAuditStatus(userId, auditStatus);
        String text = auditStatus.intValue() == AuditStatus.PASSED.getCode()
                ? "您的账户实名审核已通过，现在可以登录校园报修系统。"
                : "您的账户实名审核未通过：" + reason;
        sendMessage(userId, text, MessageType.AUDIT);
        return userDao.findById(userId);
    }

    /** 锁定/解锁账户 */
    public User lockUser(Integer operatorId, Integer userId, boolean locked) {
        requireAdmin(operatorId);
        User user = requireUser(userId);
        if (Role.ADMIN.getCode().equals(user.getRole())) {
            throw new BusinessException("系统管理员账户不允许锁定");
        }
        userDao.updateLocked(userId, Integer.valueOf(locked ? 1 : 0));
        sendMessage(userId, locked ? "您的账户已被管理员锁定，请联系系统管理员。" : "您的账户已解锁，可以正常登录。",
                MessageType.SYSTEM);
        return userDao.findById(userId);
    }

    /** 用户列表统计（管理员工作台） */
    public Map<String, Object> userStatistics() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("total", Integer.valueOf(userDao.findAll().size()));
        data.put("reporter", Integer.valueOf(userDao.findByRole(Role.REPORTER.getCode()).size()));
        data.put("worker", Integer.valueOf(userDao.findByRole(Role.WORKER.getCode()).size()));
        data.put("manager", Integer.valueOf(userDao.findByRole(Role.MANAGER.getCode()).size()));
        data.put("admin", Integer.valueOf(userDao.findByRole(Role.ADMIN.getCode()).size()));
        data.put("pending", Integer.valueOf(listPendingAudit().size()));
        return data;
    }

    /** 当前登录用户信息（含维修工档案） */
    public Map<String, Object> profile(Integer userId) {
        User user = requireUser(userId);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("user", user);
        data.put("roleText", Role.textOf(user.getRole()));
        data.put("auditStatusText", user.getAuditStatusText());
        if (Role.WORKER.getCode().equals(user.getRole())) {
            Worker worker = workerDao.findByUserId(userId);
            data.put("worker", worker);
        }
        data.put("unreadMessages", Long.valueOf(messageDao.countUnread(userId)));
        return data;
    }

    // ------------------------------------------------------ 基础数据维护

    /** 基础数据：楼栋/类别/工种的查询 */
    public List<String> baseData(String type) {
        return baseDataDao.findValues(type);
    }

    /** 基础数据维护（系统管理员） */
    public void saveBaseData(Integer operatorId, String type, String value, String sortNo, boolean delete) {
        requireAdmin(operatorId);
        Validate.create()
                .required("type", "基础数据类型", type)
                .required("value", "基础数据值", value)
                .throwIfInvalid();
        if (!"building".equals(type) && !"category".equals(type) && !"skill".equals(type)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "基础数据类型仅支持 building（楼栋）、category（报修类别）、skill（维修工种）");
        }
        if (delete) {
            baseDataDao.delete(type, value.trim());
            return;
        }
        int sort = 100;
        try {
            if (sortNo != null && !sortNo.trim().isEmpty()) {
                sort = Integer.parseInt(sortNo.trim());
            }
        } catch (NumberFormatException ignored) {
            // 排序号非法时使用默认值
        }
        if (baseDataDao.insert(type, value.trim(), sort) == 0) {
            throw new BusinessException("该基础数据已存在");
        }
    }

    // ------------------------------------------------------------ 内部方法

    private User requireUser(Integer userId) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void requireAdmin(Integer operatorId) {
        User operator = requireUser(operatorId);
        if (!Role.ADMIN.getCode().equals(operator.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅系统管理员可执行该操作");
        }
    }

    private void sendMessage(Integer userId, String content, String type) {
        Message message = new Message();
        message.setUserId(userId);
        message.setContent(content);
        message.setMsgType(type);
        message.setIsRead(Integer.valueOf(0));
        message.setCreateTime(new Date());
        messageDao.insert(message);
    }

    private int recordFail(String username) {
        Integer count = FAIL_COUNTER.get(username);
        int value = count == null ? 1 : count.intValue() + 1;
        FAIL_COUNTER.put(username, Integer.valueOf(value));
        return value;
    }

    /** 查询维修工档案列表（派单页面使用） */
    public List<Worker> listWorkers() {
        List<Worker> workers = new ArrayList<Worker>(workerDao.findAll());
        return workers;
    }
}
