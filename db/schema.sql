-- ============================================================================
--  校园报修系统 · 数据库建表脚本
--  对应《校园报修系统需求分析和系统详细设计书》2.3 数据库设计（表 2.10 ~ 表 2.17）
--  数据库：MySQL 8.0
--  字符集：utf8mb4 / utf8mb4_general_ci
--
--  说明：设计书物理结构设计中 t_repair_order.status 缺少"已驳回(7)"状态，
--        而 1.1.2.3 明确要求"不受理的报修单需填写退回原因并通知报修人"，
--        故此处补充状态 7=已驳回 与 reject_reason 字段（详见 README 差异说明）。
--        其余字段名、类型、约束与设计书完全一致。
-- ============================================================================

DROP DATABASE IF EXISTS campus_repair;
CREATE DATABASE campus_repair DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE campus_repair;

-- ---------------------------------------------------------------------------
-- 表 2.10 用户表 t_user
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    user_id         INT             NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键',
    username        VARCHAR(50)     NOT NULL                COMMENT '用户名',
    password        VARCHAR(128)    NOT NULL                COMMENT '密码（加盐加密存储）',
    real_name       VARCHAR(50)     NOT NULL                COMMENT '真实姓名',
    student_no      VARCHAR(30)     NOT NULL                COMMENT '学号/工号',
    phone           VARCHAR(20)     NOT NULL                COMMENT '手机号',
    role            VARCHAR(20)     NOT NULL                COMMENT '角色（reporter/worker/manager/admin）',
    audit_status    TINYINT         NOT NULL DEFAULT 0      COMMENT '审核状态（0待审/1通过/2驳回）',
    is_locked       TINYINT         DEFAULT 0               COMMENT '是否锁定（0否/1是）',
    create_time     DATETIME        NOT NULL                COMMENT '注册时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_phone (phone),
    UNIQUE KEY uk_user_student_no (student_no),
    KEY idx_user_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ---------------------------------------------------------------------------
-- 表 2.11 报修单表 t_repair_order
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS t_repair_order;
CREATE TABLE t_repair_order (
    order_id        INT             NOT NULL AUTO_INCREMENT COMMENT '报修单ID，主键',
    user_id         INT             NOT NULL                COMMENT '报修人ID，外键',
    building        VARCHAR(50)     NOT NULL                COMMENT '楼栋',
    floor           VARCHAR(20)     NOT NULL                COMMENT '楼层',
    room            VARCHAR(20)     NOT NULL                COMMENT '房间号',
    category        VARCHAR(30)     NOT NULL                COMMENT '报修类别（水电/木工/设备等）',
    description     TEXT            NOT NULL                COMMENT '故障描述',
    images          VARCHAR(255)    NULL                    COMMENT '现场照片URL（逗号分隔）',
    status          TINYINT         NOT NULL DEFAULT 0      COMMENT '状态（0待审核/1待派单/2已派单/3维修中/4待确认/5已完成/6已撤销/7已驳回）',
    priority        TINYINT         DEFAULT 0               COMMENT '优先级（0普通/1紧急）',
    urge_count      INT             NOT NULL DEFAULT 0      COMMENT '催办次数',
    reject_reason   VARCHAR(255)    NULL                    COMMENT '驳回原因（status=7 时必填）',
    create_time     DATETIME        NOT NULL                COMMENT '提交时间',
    update_time     DATETIME        NULL                    COMMENT '更新时间',
    PRIMARY KEY (order_id),
    KEY idx_order_user (user_id),
    KEY idx_order_status (status),
    KEY idx_order_create_time (create_time),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES t_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '报修单表';

-- ---------------------------------------------------------------------------
-- 表 2.13 维修人员表 t_worker
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS t_worker;
CREATE TABLE t_worker (
    worker_id       INT             NOT NULL AUTO_INCREMENT COMMENT '维修人员ID，主键',
    user_id         INT             NOT NULL                COMMENT '关联用户ID，外键',
    name            VARCHAR(50)     NOT NULL                COMMENT '姓名',
    phone           VARCHAR(20)     NOT NULL                COMMENT '手机号',
    skill_tags      VARCHAR(255)    NOT NULL                COMMENT '技能标签（如水电,木工,设备）',
    current_orders  INT             DEFAULT 0               COMMENT '当前在单量',
    location        VARCHAR(100)    NULL                    COMMENT '当前位置信息',
    status          TINYINT         DEFAULT 1               COMMENT '状态（0离线/1在线）',
    PRIMARY KEY (worker_id),
    UNIQUE KEY uk_worker_user (user_id),
    KEY idx_worker_status (status),
    CONSTRAINT fk_worker_user FOREIGN KEY (user_id) REFERENCES t_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '维修人员表';


-- ---------------------------------------------------------------------------
-- 表 2.12 维修任务表 t_repair_task
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS t_repair_task;
CREATE TABLE t_repair_task (
    task_id         INT             NOT NULL AUTO_INCREMENT COMMENT '任务ID，主键',
    order_id        INT             NOT NULL                COMMENT '报修单ID，外键',
    worker_id       INT             NOT NULL                COMMENT '维修人员ID，外键',
    dispatch_time   DATETIME        NOT NULL                COMMENT '派单时间',
    accept_time     DATETIME        NULL                    COMMENT '接单时间',
    start_time      DATETIME        NULL                    COMMENT '开始维修时间',
    finish_time     DATETIME        NULL                    COMMENT '完成登记时间',
    status          TINYINT         NOT NULL DEFAULT 0      COMMENT '状态（0待接单/1维修中/2待确认/3已完成/4转单/5退单）',
    result          VARCHAR(255)    NULL                    COMMENT '维修结果',
    remark          VARCHAR(255)    NULL                    COMMENT '备注/延期原因',
    PRIMARY KEY (task_id),
    KEY idx_task_order (order_id),
    KEY idx_task_worker (worker_id),
    KEY idx_task_status (status),
    CONSTRAINT fk_task_order FOREIGN KEY (order_id) REFERENCES t_repair_order (order_id),
    CONSTRAINT fk_task_worker FOREIGN KEY (worker_id) REFERENCES t_worker (worker_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '维修任务表';

-- ---------------------------------------------------------------------------
-- 表 2.14 耗材表 t_material
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS t_material;
CREATE TABLE t_material (
    mat_id          INT             NOT NULL AUTO_INCREMENT COMMENT '耗材ID，主键',
    mat_name        VARCHAR(100)    NOT NULL                COMMENT '耗材名称',
    spec            VARCHAR(100)    NULL                    COMMENT '规格型号',
    stock           INT             NOT NULL DEFAULT 0      COMMENT '库存数量',
    unit_price      DECIMAL(10,2)   NOT NULL                COMMENT '单价',
    PRIMARY KEY (mat_id),
    UNIQUE KEY uk_material_name_spec (mat_name, spec)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '耗材表';

-- ---------------------------------------------------------------------------
-- 表 2.15 耗材使用表 t_material_usage
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS t_material_usage;
CREATE TABLE t_material_usage (
    use_id          INT             NOT NULL AUTO_INCREMENT COMMENT '使用记录ID，主键',
    task_id         INT             NOT NULL                COMMENT '维修任务ID，外键',
    mat_id          INT             NOT NULL                COMMENT '耗材ID，外键',
    use_count       INT             NOT NULL                COMMENT '使用数量',
    use_time        DATETIME        NOT NULL                COMMENT '使用时间',
    PRIMARY KEY (use_id),
    UNIQUE KEY uk_usage_task_mat (task_id, mat_id),
    CONSTRAINT fk_usage_task FOREIGN KEY (task_id) REFERENCES t_repair_task (task_id),
    CONSTRAINT fk_usage_material FOREIGN KEY (mat_id) REFERENCES t_material (mat_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '耗材使用表';

-- ---------------------------------------------------------------------------
-- 表 2.16 评价表 t_evaluation
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS t_evaluation;
CREATE TABLE t_evaluation (
    eval_id         INT             NOT NULL AUTO_INCREMENT COMMENT '评价ID，主键',
    order_id        INT             NOT NULL                COMMENT '报修单ID，外键',
    score           TINYINT         NOT NULL                COMMENT '评分（1～5分）',
    comment         VARCHAR(500)    NULL                    COMMENT '文字评价',
    reply           VARCHAR(500)    NULL                    COMMENT '管理员回复',
    audit_status    TINYINT         NOT NULL DEFAULT 0      COMMENT '评价审核状态（0待审/1通过/2驳回）',
    create_time     DATETIME        NOT NULL                COMMENT '评价时间',
    PRIMARY KEY (eval_id),
    UNIQUE KEY uk_eval_order (order_id),
    CONSTRAINT fk_eval_order FOREIGN KEY (order_id) REFERENCES t_repair_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评价表';

-- ---------------------------------------------------------------------------
-- 表 2.17 消息通知表 t_message
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS t_message;
CREATE TABLE t_message (
    msg_id          INT             NOT NULL AUTO_INCREMENT COMMENT '消息ID，主键',
    user_id         INT             NOT NULL                COMMENT '接收用户ID，外键',
    content         VARCHAR(500)    NOT NULL                COMMENT '消息内容',
    msg_type        VARCHAR(30)     NOT NULL                COMMENT '消息类型（报修/派单/催办/审核等）',
    is_read         TINYINT         DEFAULT 0               COMMENT '是否已读（0未读/1已读）',
    create_time     DATETIME        NOT NULL                COMMENT '推送时间',
    PRIMARY KEY (msg_id),
    KEY idx_message_user_read (user_id, is_read),
    CONSTRAINT fk_message_user FOREIGN KEY (user_id) REFERENCES t_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '消息通知表';

-- ---------------------------------------------------------------------------
-- 基础数据表（设计书 1.1.1 要求维护"楼栋、设备类型、维修工种"等基础数据）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS t_base_data;
CREATE TABLE t_base_data (
    data_id         INT             NOT NULL AUTO_INCREMENT COMMENT '基础数据ID，主键',
    data_type       VARCHAR(30)     NOT NULL                COMMENT '类型（building/category/skill）',
    data_value      VARCHAR(100)    NOT NULL                COMMENT '值',
    sort_no         INT             DEFAULT 0               COMMENT '排序号',
    PRIMARY KEY (data_id),
    UNIQUE KEY uk_base_type_value (data_type, data_value)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '基础数据表';

-- ---------------------------------------------------------------------------
-- 业务过程留痕：维修进度反馈（设计书表 1.10/2.8 要求"进度反馈"与"延期原因"记录）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS t_progress;
CREATE TABLE t_progress (
    progress_id     INT             NOT NULL AUTO_INCREMENT COMMENT '进度ID，主键',
    task_id         INT             NOT NULL                COMMENT '维修任务ID，外键',
    content         VARCHAR(255)    NOT NULL                COMMENT '进度说明（延期原因必填）',
    create_time     DATETIME        NOT NULL                COMMENT '反馈时间',
    PRIMARY KEY (progress_id),
    KEY idx_progress_task (task_id),
    CONSTRAINT fk_progress_task FOREIGN KEY (task_id) REFERENCES t_repair_task (task_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '维修进度反馈表';
