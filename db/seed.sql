-- ============================================================================
--  校园报修系统 · 演示数据脚本
--  依赖 schema.sql（先执行 schema.sql，再执行本脚本）
--
--  演示账号（密码均为对应明文，采用 SHA-256(盐 + 明文) 存储，盐值见 PasswordUtil.SALT）：
--    学生报修人   student01  / 123456
--    教师报修人   teacher01  / 123456
--    维修工       worker01   / worker123   （水电）
--    维修工       worker02   / worker123   （木工）
--    维修工       worker03   / worker123   （设备）
--    维修管理员   manager01  / manager123
--    系统管理员   admin      / admin123
-- ============================================================================
USE campus_repair;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE t_progress;
TRUNCATE TABLE t_message;
TRUNCATE TABLE t_evaluation;
TRUNCATE TABLE t_material_usage;
TRUNCATE TABLE t_repair_task;
TRUNCATE TABLE t_repair_order;
TRUNCATE TABLE t_worker;
TRUNCATE TABLE t_material;
TRUNCATE TABLE t_user;
TRUNCATE TABLE t_base_data;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------- 基础数据 -------------------------------------
INSERT INTO t_base_data (data_type, data_value, sort_no) VALUES
('building', '1号教学楼', 1),
('building', '2号教学楼', 2),
('building', '实验楼', 3),
('building', '图书馆', 4),
('building', '学生公寓1栋', 5),
('building', '学生公寓2栋', 6),
('building', '行政楼', 7),
('building', '体育馆', 8),
('category', '水电', 1),
('category', '木工', 2),
('category', '设备', 3),
('category', '门窗', 4),
('category', '网络', 5),
('category', '其他', 6),
('skill', '水电', 1),
('skill', '木工', 2),
('skill', '设备', 3),
('skill', '门窗', 4),
('skill', '网络', 5);

-- ---------------------------- 用户 -----------------------------------------
-- 说明：账号命名与内存库演示数据（DemoDataLoader）保持一致，便于两种模式使用同一套演示脚本
INSERT INTO t_user (user_id, username, password, real_name, student_no, phone, role, audit_status, is_locked, create_time) VALUES
(1, 'student',   '249a485f9f77dbf3841a437bdab6135f97db7430a2871e150fc8b1f712083ed9', '李明',   '2021010101', '13800000001', 'reporter', 1, 0, '2024-03-01 09:00:00'),
(2, 'teacher',   '249a485f9f77dbf3841a437bdab6135f97db7430a2871e150fc8b1f712083ed9', '王芳',   'T20190101',  '13800000002', 'reporter', 1, 0, '2024-03-01 09:05:00'),
(3, 'worker01',  'c93dbbb51de50b24bcbedea4f1ab6e3c049c7ab0ddb1f8a12b9136492ae804f8', '赵强',   'W2020001',   '13800000003', 'worker',   1, 0, '2024-03-01 09:10:00'),
(4, 'worker02',  'c93dbbb51de50b24bcbedea4f1ab6e3c049c7ab0ddb1f8a12b9136492ae804f8', '孙勇',   'W2020002',   '13800000004', 'worker',   1, 0, '2024-03-01 09:12:00'),
(5, 'worker03',  'c93dbbb51de50b24bcbedea4f1ab6e3c049c7ab0ddb1f8a12b9136492ae804f8', '周涛',   'W2020003',   '13800000005', 'worker',   1, 0, '2024-03-01 09:14:00'),
(6, 'manager',   'de0e497342d82d9d1985e84284ea35b197a5a53fc694dfc54d2252149f13b1ca', '陈静',   'M2018001',   '13800000006', 'manager',  1, 0, '2024-03-01 09:20:00'),
(7, 'admin',     'e1d9ca5b4a2eb2adb332bb044c55bda231e40f6b8cdbeab871bb5b767a1e4695', '系统管理员', 'A0001',  '13800000007', 'admin',    1, 0, '2024-03-01 09:30:00'),
(8, 'student02', '249a485f9f77dbf3841a437bdab6135f97db7430a2871e150fc8b1f712083ed9', '张伟',   '2021010102', '13800000008', 'reporter', 0, 0, '2024-03-02 10:00:00');

-- ---------------------------- 维修人员 -------------------------------------
INSERT INTO t_worker (worker_id, user_id, name, phone, skill_tags, current_orders, location, status) VALUES
(1, 3, '赵强', '13800000003', '水电,设备', 0, '1号教学楼值班室', 1),
(2, 4, '孙勇', '13800000004', '木工,门窗', 0, '维修间', 1),
(3, 5, '周涛', '13800000005', '设备,网络', 0, '实验楼', 1);

-- ---------------------------- 耗材 -----------------------------------------
INSERT INTO t_material (mat_id, mat_name, spec, stock, unit_price) VALUES
(1, 'LED灯管', 'T8 18W', 120, 15.50),
(2, '水龙头', 'DN15 单冷', 40, 32.00),
(3, '三角阀', 'DN15', 60, 18.00),
(4, 'PVC水管', 'DN25', 80, 9.80),
(5, '门锁芯', '通用型', 25, 45.00),
(6, '合页', '4寸不锈钢', 100, 6.50),
(7, '网线', '超六类（米）', 500, 2.20),
(8, '网络模块', 'RJ45 千兆', 30, 12.00),
(9, '空气开关', 'C63 2P', 20, 28.00),
(10, '密封胶', '中性硅酮（支）', 50, 14.00);

-- ---------------------------- 报修单 ---------------------------------------
-- status: 0待审核 1待派单 2已派单 3维修中 4待确认 5已完成 6已撤销 7已驳回
INSERT INTO t_repair_order (order_id, user_id, building, floor, room, category, description, images, status, priority, urge_count, reject_reason, create_time, update_time) VALUES
(1, 1, '学生公寓1栋', '3', '312', '水电', '卫生间水龙头持续漏水，关不紧，地面已积水。', NULL, 5, 0, 0, NULL, '2024-04-01 08:30:00', '2024-04-01 15:20:00'),
(2, 2, '1号教学楼', '2', '205', '设备', '多媒体讲台投影仪无法开机，指示灯不亮。', NULL, 5, 1, 0, NULL, '2024-04-02 09:10:00', '2024-04-02 16:40:00'),
(3, 1, '学生公寓2栋', '5', '501', '水电', '走廊声控灯不亮，晚上出入不便。', NULL, 4, 0, 0, NULL, '2024-04-05 19:20:00', '2024-04-06 10:05:00'),
(4, 2, '实验楼', '3', '308', '木工', '实验台抽屉滑轨损坏，抽屉卡住无法拉开。', NULL, 3, 0, 1, NULL, '2024-04-06 10:40:00', '2024-04-06 14:00:00'),
(5, 1, '图书馆', '4', '自习区', '网络', '自习区无线网络信号弱，频繁掉线。', NULL, 1, 0, 0, NULL, '2024-04-07 11:00:00', '2024-04-07 11:00:00'),
(6, 2, '行政楼', '1', '101', '门窗', '办公室窗户把手松动，关不严。', NULL, 0, 0, 0, NULL, '2024-04-08 08:50:00', NULL),
(7, 1, '体育馆', '1', '器材室', '设备', '跑步机显示屏黑屏，无法启动。', NULL, 7, 0, 0, '该设备已报废，请走资产报损流程。', '2024-04-08 09:30:00', '2024-04-08 10:20:00');

-- ---------------------------- 维修任务 -------------------------------------
-- status: 0待接单 1维修中 2待确认 3已完成 4转单 5退单
INSERT INTO t_repair_task (task_id, order_id, worker_id, dispatch_time, accept_time, start_time, finish_time, status, result, remark) VALUES
(1, 1, 1, '2024-04-01 09:00:00', '2024-04-01 09:12:00', '2024-04-01 10:00:00', '2024-04-01 11:30:00', 3, '更换水龙头与三角阀，试水无渗漏。', NULL),
(2, 2, 3, '2024-04-02 09:40:00', '2024-04-02 09:55:00', '2024-04-02 10:20:00', '2024-04-02 11:50:00', 3, '更换电源模块，投影仪恢复正常。', NULL),
(3, 3, 1, '2024-04-05 20:00:00', '2024-04-05 20:10:00', '2024-04-06 08:30:00', '2024-04-06 10:05:00', 2, '更换声控开关与灯管，测试正常。', NULL),
(4, 4, 2, '2024-04-06 11:20:00', '2024-04-06 11:35:00', '2024-04-06 13:00:00', NULL, 1, NULL, '原滑轨型号缺货，预计延期1天。');

-- ---------------------------- 耗材使用 -------------------------------------
INSERT INTO t_material_usage (use_id, task_id, mat_id, use_count, use_time) VALUES
(1, 1, 2, 1, '2024-04-01 11:20:00'),
(2, 1, 3, 1, '2024-04-01 11:20:00'),
(3, 2, 9, 1, '2024-04-02 11:40:00'),
(4, 3, 1, 2, '2024-04-06 09:50:00');

-- ---------------------------- 评价 -----------------------------------------
INSERT INTO t_evaluation (eval_id, order_id, score, comment, reply, audit_status, create_time) VALUES
(1, 1, 5, '师傅来得很快，维修干净利落，赞！', '感谢反馈，我们会继续努力。', 1, '2024-04-01 16:00:00'),
(2, 2, 4, '修好了，希望下次能再快一点。', NULL, 1, '2024-04-02 17:10:00');

-- ---------------------------- 进度反馈 -------------------------------------
INSERT INTO t_progress (progress_id, task_id, content, create_time) VALUES
(1, 1, '已到现场，确认需更换水龙头。', '2024-04-01 10:05:00'),
(2, 2, '检测为电源模块故障，需更换空气开关。', '2024-04-02 10:30:00'),
(3, 4, '原滑轨型号缺货，预计延期1天。', '2024-04-06 14:00:00');

-- ---------------------------- 消息通知 -------------------------------------
INSERT INTO t_message (msg_id, user_id, content, msg_type, is_read, create_time) VALUES
(1, 6, '您有 1 条新的报修申请待审核（报修单号 6）。', '报修', 0, '2024-04-08 08:50:00'),
(2, 1, '您的报修单（报修单号 5）已通过审核，等待派单。', '审核', 1, '2024-04-07 11:10:00'),
(3, 5, '您有一条新的维修任务待接收（报修单号 5）。', '派单', 0, '2024-04-07 11:20:00'),
(4, 2, '您的报修单（报修单号 7）未通过审核：该设备已报废，请走资产报损流程。', '审核', 0, '2024-04-08 10:20:00');

-- ---------------------------- 校正自增 -------------------------------------
ALTER TABLE t_user AUTO_INCREMENT = 100;
ALTER TABLE t_worker AUTO_INCREMENT = 100;
ALTER TABLE t_repair_order AUTO_INCREMENT = 100;
ALTER TABLE t_repair_task AUTO_INCREMENT = 100;
ALTER TABLE t_material AUTO_INCREMENT = 100;
ALTER TABLE t_material_usage AUTO_INCREMENT = 100;
ALTER TABLE t_evaluation AUTO_INCREMENT = 100;
ALTER TABLE t_message AUTO_INCREMENT = 100;
ALTER TABLE t_progress AUTO_INCREMENT = 100;
