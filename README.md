# 校园报修系统（Campus Repair Service System）

依据《**校园报修系统需求分析和系统详细设计书**》实现的完整可运行系统。
采用设计书 2.1 节规定的 **MVC 四层架构**：表示层（View）/ 控制层（Controller）/
业务逻辑层（Service）/ 数据访问层（DAO）+ 辅助类，覆盖设计书第 1 章的全部功能需求与
第 2 章的全部设计内容（软件架构、功能模块、类设计、数据库逻辑与物理结构）。

| 项目 | 说明 |
| --- | --- |
| 技术栈 | Java 17+（已在 JDK 25 上验证）、Jakarta Servlet（Tomcat 10/11）、**MyBatis 3.5 Mapper**、MySQL 8.0 |
| 架构 | MVC 分层：8 个功能 Controller + 1 个前端控制器、8 个 Service、10 个 DAO 接口（**MyBatis** / 手写 SQL / 内存库 三套可切换实现） |
| 持久化 | **MyBatis 3.5.16**：10 个 Mapper 接口 + 10 个 Mapper XML、99 条映射语句，SQL 全部在 `src/main/resources/mapper/*.xml` |
| 数据库 | MySQL 8.0，`campus_repair` 库，10 张表（设计书 8 张业务表 + 2 张支撑表） |
| 接口 | 65 个 REST 接口，统一 `Result{code,message,data}` 返回结构 |
| 前端 | 17 个页面（HTML + 原生 JS + CSS），覆盖四类角色的全部操作 |
| 验证 | 业务自测 99 项、MyBatis 定点验证 39 项、端到端接口验证 97 项，全部通过 |

---

## 一、快速启动（IDEA + Tomcat）

本项目的运行方式为 **IDEA + Tomcat 10/11 部署**（Tomcat 会自行编译 Java 源码，无需 Maven）。

### 1. 配置 Artifact 与 Tomcat（一次性，约 1 分钟）

1. IDEA → `File → Project Structure → Artifacts`：新增 *Web Application: Exploded*（输出目录自定）：
   - `Web resource directory` 指向模块内的 **`webapp`** 目录；
   - `Available Elements` 中把 **模块的 compile output** 加入 `WEB-INF/classes`；
   - 把 `lib/` 下 **4 个 jar** 全部加入 `WEB-INF/lib`：
     `mybatis-3.5.16.jar`、`mysql-connector-j-8.3.0.jar`、`slf4j-api-1.7.36.jar`、`slf4j-simple-1.7.36.jar`
     （MyBatis 与 MySQL 驱动必需，slf4j 供 MyBatis 打印日志）。
2. IDEA → `Run → Edit Configurations → Tomcat Server → Local`：
   - `Deployment` 标签页 `+` 选择该 artifact；`Application context` 填 `/` 或任意名称（如 `/campus_repair_system_war`）；
   - `Server` 标签页确认 JDK 为 17 及以上（本项目在 JDK 25 上验证）。
3. 点 Run 启动，浏览器访问 `http://localhost:8080/`（或 `http://localhost:8080/<上下文>/`）。

> 模块的 Web Facet（`campus-repair-system.iml`）已指向 `webapp` 与 `WEB-INF/web.xml`，
> 通常 IDEA 会自动识别；若 Artifact 列表为空，按上面第 1 步手工添加即可。

> **运行资源不需要手工添加**：MyBatis 的 `mybatis-config.xml`、`mapper/*.xml` 与 `config.properties`
> 已随 Web 根一起提供——项目把它们镜像在 `webapp/WEB-INF/classes/` 下（`src/main/resources/` 是源，
> `build.cmd` 每次编译都会自动重新同步）。原因是 IDEA 打包 Artifact 时一定会把 `webapp/` 原样复制进 WAR，
> 所以即使 IDEA 那次只是增量构建、没有把 `src/main/resources` 拷进模块输出目录，这两个文件在部署后
> 也一定位于类路径上。若该目录为空或提示与源文件不一致，执行一次 `build.cmd` 即可补齐。

### 2. 数据源：MyBatis + MySQL（默认）或内存库演示

`webapp/WEB-INF/web.xml` 中 `storage.mode=mybatis`（默认）表示 **MyBatis Mapper + MySQL**，
需先初始化数据库；若只想快速看界面，把 `storage.mode` 改为 `memory` 即可（内置演示数据，无需数据库）。

```bat
:: ① 建库建表 + 导入演示数据
mysql -u root -p --default-character-set=utf8mb4 -e "source db/schema.sql"
mysql -u root -p --default-character-set=utf8mb4 -e "source db/seed.sql"

:: ② 确认 webapp/WEB-INF/web.xml：storage.mode=mybatis，db.password 改成你的 MySQL 密码
::    或（推荐）用环境变量传密码，避免明文入库：
::    IDEA → Tomcat 运行配置 → Environment variables 增加 CAMPUS_DB_PASSWORD=你的密码
```

启动后自检日志会明确写出实际生效的数据访问实现，例如：

```
[CampusRepair] 校园报修系统启动完成：存储模式=mybatis，数据访问=MyBatis Mapper（resources/mapper/*.xml），
数据库=OK: MySQL 8.0.25（MyBatis MySQL Connector/J 连接池），可用账户数=8
```

### 3. 编译校验与验证命令（可选，Tomcat 会自行编译）

```bat
build.cmd                                          :: 用 javac 编译全部源码（含 Servlet 适配层与 MyBatis Dao）
build.cmd -WithSelfTest                            :: 编译并运行业务自测（99 项断言，报告 build/selftest.log）
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\http-e2e.ps1 -Base http://127.0.0.1:8080/campus_repair_system_war   :: 端到端接口验证（97 项断言）
```

### 4. 演示账号

| 角色 | 用户名 | 密码 | 说明 |
| --- | --- | --- | --- |
| 报修人（学生） | `student` | `123456` | 提交报修、查询、催办、撤销、确认结果、评价 |
| 报修人（教职工） | `teacher` | `123456` | 同上 |
| 维修人员 | `worker01` `worker02` `worker03` | `worker123` | 技能分别为 水电/设备、木工/门窗、设备/网络 |
| 维修管理员 | `manager` | `manager123` | 审核、人工/智能派单、监督、统计、评价管理、耗材 |
| 系统管理员 | `admin` | `admin123` | 用户审核与锁定、基础数据维护、统计分析 |
| 待审核账户 | `student02` | `123456` | 用于演示「未审核账户无法登录 → 管理员审核通过」流程 |

---

## 二、目录结构

```
campus-repair-system/
├─ build.cmd                        编译校验脚本入口（javac + 业务自测，无需 Maven 与外网）
├─ README.md / DELIVERY-VERIFICATION.md  交付说明与交付验证报告
├─ .gitignore                       忽略编译产物与 IDEA 本地配置
├─ campus-repair-system.iml         IDEA 模块文件（Web Facet 指向 webapp 与 web.xml）
├─ scripts/
│   ├─ build.ps1                    编译校验与自测脚本（自动定位 JDK 与 Tomcat）
│   └─ http-e2e.ps1                 端到端接口验证脚本（97 项断言）
├─ db/
│   ├─ schema.sql                   建库建表脚本（10 张表 + 外键、唯一约束、索引）
│   └─ seed.sql                     演示数据（8 账户、7 报修单、4 任务等）
├─ lib/                             第三方依赖：mybatis-3.5.16.jar、mysql-connector-j-8.3.0.jar、
│                                   slf4j-api-1.7.36.jar、slf4j-simple-1.7.36.jar（需加入 WEB-INF/lib）
└─ webapp/                          前端资源与 Web 配置（即 IDEA Artifact 的 Web 根）
│   ├─ index.html register.html     登录 / 注册
│   ├─ home.html                    工作台（按角色自适应）
│   ├─ report*.html task.html       报修查询/提交/详情、维修任务处理
│   ├─ dispatch.html audit.html     派单管理（含智能派单匹配明细）、报修审核
│   ├─ monitor.html statistics.html 维修监督、统计分析
│   ├─ evaluation.html material.html 评价管理、耗材管理
│   ├─ admin.html base-data.html    用户与审核、基础数据维护
│   ├─ messages.html 404.html       消息通知、错误页
│   ├─ js/  css/                    前端脚本与样式（common.js 为公共库，含上下文路径适配）
│   └─ WEB-INF/
│       ├─ web.xml                  部署描述（servlet 映射、上下文参数、会话配置）
│       ├─ classes/                 运行资源镜像：mybatis-config.xml、mapper/*.xml、config.properties
│       │                           （由 build.cmd 从 src/main/resources 同步，随 Web 根一起打进 WAR）
│       └─ (jsp/)                   可选视图目录，当前交付未使用（见下方说明）
└─ src/
    ├─ main/java/com/campus/repair/
    │   ├─ boot/        DemoDataLoader（内存库演示数据装载，由启动监听器调用）
    │   ├─ common/      Result / PageResult / BusinessException / Validate
    │   ├─ config/      AppConfig 配置管理（支持 CAMPUS_* 环境变量覆盖）
    │   ├─ domain/      8 个实体 + 枚举 + 派单评分模型 + 统计行
    │   ├─ dao/
    │   │   ├─ mapper/  MyBatis Mapper 接口（10 个）+ RepairOrderQuery（分页查询入参）
    │   │   ├─ mybatis/ MyBatis Dao 实现 + MyBatisSessionFactory（会话与事务）
    │   │   ├─ jdbc/    手写 SQL 的对照实现（JdbcTemplate、Database 连接池）
    │   │   └─ memory/  内存库实现（无数据库演示）
    │   ├─ dal/         MyBatisCapabilityProbe（数据访问层自检：报告实际使用的 DAO 实现）
    │   ├─ service/     8 个业务服务 + 事务模板
    │   ├─ util/        PasswordUtil / DateUtil / JsonUtil
    │   └─ web/         前端控制器 Dispatcher、路由注解、会话、8 个 Controller
    ├─ main/resources/  config.properties、mybatis-config.xml、mapper/*.xml（MyBatis SQL 源文件，
    │                   由 build.cmd 同步一份到 webapp/WEB-INF/classes 随 WAR 部署）
    ├─ servlet-adapter/ ApiServlet（/api/* 前端控制器）、CampusContextListener（启动初始化与自检）
    └─ test/java/       验证程序（业务自测、MyBatis 定点验证、参数解析、派单与 JDBC 定点验证）
```

> **视图层说明**：本交付的视图层由 `webapp/*.html` + `webapp/js/`（静态页面 + JSON 接口）承担，
> 页面数量与功能见第四章；工程内**不含 `.jsp` 文件**。`web.xml` 仍配置了 `jsp-config`（UTF-8 编码）
> 与 `/WEB-INF/jsp/*` 访问保护，若需要 JSP 形态的视图，直接在该目录下新增即可，后端接口无需改动。

---

## 三、需求与设计的实现对照

### 3.1 第 1 章 系统需求

| 设计书条目 | 实现位置 |
| --- | --- |
| 1.1.1 四类角色（报修人/维修人员/维修管理员/系统管理员）及其职责 | `domain/Role`、`web/Route` 角色权限、各角色工作台页面 |
| 1.1.2.1 账户注册（表 1.2） | `AccountService.register()`：用户名唯一、手机号格式、密码加密、状态置待审核 |
| 1.1.2.1 登录（表 1.3） | `AccountService.login()`：密码校验、审核与锁定校验、会话建立、失败 5 次锁定 |
| 1.1.2.1 注销/信息维护/消息配置/账户审核（表 1.4） | `logout`、`modifyInfo/modifyPassword`（校验原密码）、`auditUser` |
| 1.1.2.2 提交报修（表 1.5） | `ReportService.submit()`：地点/类别/描述校验、楼栋取自基础数据、状态待审核、通知管理员 |
| 1.1.2.2 报修查询与撤销（表 1.6） | `ReportService.query()`（多条件 + 分页）、`cancel()`（仅待审核/待派单可撤销） |
| 1.1.2.2 报修催办（表 1.1） | `ReportService.urge()`：累加催办次数并通知管理员与维修人员 |
| 1.1.2.3 报修审核（表 1.8） | `ReportService.audit()`：受理/驳回，不受理必须填写退回原因并通知报修人 |
| 1.1.2.3 人工派单 / 接单 / 退单（表 1.8） | `DispatchService.manualDispatch()`、`accept()`（状态转维修中）、`refuse()`（退回派单池） |
| 1.1.2.3 派单时可查看在单量与技能标签 | `DispatchService.rankCandidates()` 返回候选与推荐理由，派单页面展示 |
| 1.1.2.3 接单时限 2 小时、超时自动提醒 | `AppConfig.acceptDeadlineMinutes`、`DispatchService.remindOverdueAccept()` + 定时任务 |
| 1.1.2.4 开始维修 / 进度反馈（表 1.10） | `RepairService.start()`、`feedback()`（延期原因必填、超时提醒管理员） |
| 1.1.2.4 耗材登记（表 1.10） | `RepairService.registerMaterial()`：超出库存拒绝；支持"先登记后补库" |
| 1.1.2.4 维修完成登记（表 1.9） | `RepairService.finish()`：结果必填、扣减库存并写入耗材使用记录（同一事务） |
| 1.1.2.4 结果确认（表 1.10） | `RepairService.confirm()`：确认通过归档；不通过退回并**回滚耗材库存** |
| 1.1.3 八个业务实体 | `domain/` 下 8 个实体与 `db/schema.sql` 的 8 张业务表一一对应 |
| 1.2 非功能性需求（表 1.13） | 分层低耦合、DAO 可替换、业务规则可配置（`config.properties`）、分页与索引优化 |

### 3.2 第 2 章 系统设计

| 设计书条目 | 实现位置 |
| --- | --- |
| 2.1 基于 MVC 的软件架构（图 2.1） | `web`（表示/控制）、`service`（业务）、`dao`（数据访问）、`common/util/config`（辅助类） |
| 2.2.1 五个功能模块（表 2.1） | 账户管理、报修管理、派单管理、维修管理、评价与统计分析，模块划分与设计书一致 |
| 2.2.2 账户注册/登录 MVC 资源表（表 2.2、2.3） | `AccountController` → `AccountService` → `UserDao`，页面 `index.html`、`register.html` |
| 2.2.3 提交报修 MVC 资源表（表 2.4） | `ReportController` → `ReportService` → `RepairOrderDao`，页面 `report-submit.html` |
| 2.2.4 人工派单、接单功能点 | `DispatchController.manualDispatch()/accept()`、`dispatch.html`、`task.html` |
| 2.2.5 维修完成登记 MVC 资源表（表 2.6） | `RepairController` → `RepairService` → `RepairTaskDao/MaterialDao/MaterialUsageDao` |
| 2.2.6 系统类设计（表 2.8） | 5 个 Controller、5 个 Service、8 个 Dao 接口、8 个实体类，命名与职责与方法名一致 |
| 2.2.6 DispatchService.calcMatchScore() | `domain/DispatchCandidate.score()`：技能 40 + 在单量 30 + 在线 15 + 紧急响应 15 = 100 分 |
| 2.2.6 StatController / StatService | `StatController`（8 个统计接口）、`StatService`（总览、类别、楼栋、绩效、月度、耗材、评价） |
| 2.3.1 逻辑结构（表 2.9、图 2.13） | 8 张业务表 + 外键关系，见 `db/schema.sql` |
| 2.3.2 物理结构（表 2.10–2.17） | 字段名、类型、约束、注释与设计书完全一致（差异见第六节） |

---

## 四、数据库设计

### 4.1 表清单

| 表名 | 说明 | 对应设计书 |
| --- | --- | --- |
| `t_user` | 用户表（角色、审核状态、锁定状态） | 表 2.10 |
| `t_repair_order` | 报修单表（地点、类别、描述、状态、优先级、催办次数） | 表 2.11 |
| `t_repair_task` | 维修任务表（派单/接单/开始/完成时间、结果、备注） | 表 2.12 |
| `t_worker` | 维修人员表（技能标签、在单量、位置、在线状态） | 表 2.13 |
| `t_material` | 耗材表（库存、单价） | 表 2.14 |
| `t_material_usage` | 耗材使用表 | 表 2.15 |
| `t_evaluation` | 评价表（评分、评语、管理员回复） | 表 2.16 |
| `t_message` | 消息通知表 | 表 2.17 |
| `t_base_data` | 基础数据表（楼栋 / 报修类别 / 维修工种） | 1.1.1 系统管理员职责 |
| `t_progress` | 维修进度反馈表（进度说明与延期原因留痕） | 表 1.10 进度反馈 |

### 4.2 状态机

**报修单 `t_repair_order.status`**

```
0 待审核 ──受理──> 1 待派单 ──派单──> 2 已派单 ──接单──> 3 维修中 ──完成登记──> 4 待确认
   │                   │                                                       ├─确认通过─> 5 已完成
   ├─报修人撤销─────────┴───────────────────────────────> 6 已撤销                 └─确认不通过─> 3 维修中（回滚耗材）
   └─审核驳回───────────────────────────────────────────> 7 已驳回
```

**维修任务 `t_repair_task.status`**

```
0 待接单 ──接单──> 1 维修中 ──完成登记──> 2 待确认 ──确认通过──> 3 已完成
                     │                                     （确认不通过 → 回到 1 维修中）
                     ├─申请转单──> 6 已退回派单池（报修单回到 1 待派单）
                     └─申请退单──> 5 已退单（报修单回到 1 待派单）
```

### 4.3 关键一致性设计

- **耗材事务一致性**：`RepairService.finish()` 中「保存维修结果 + 扣减库存 + 写入使用记录 + 更新报修单状态」
  在 `TxTemplate` 事务内完成；库存扣减使用条件更新 `UPDATE t_material SET stock = stock - ? WHERE mat_id = ? AND stock >= ?`，
  并发下不会出现负库存。
- **耗材回滚**：结果确认不通过时调用 `MaterialService.restoreByTask()` 恢复库存并删除使用记录，
  保证「退回重做」不产生物料账目错误。
- **在单量维护**：派单 +1、接单不变、退单/转单/完成 -1，并提供 `recalculateLoad()` 依据进行中任务数校正。
- **唯一约束**：用户名、手机号、学号/工号唯一；同一报修单仅一条评价；同一任务同一耗材仅一条使用记录。

---

## 五、接口与权限

### 5.1 统一返回结构

```json
{ "code": 0, "message": "操作成功", "data": { } , "success": true }
```

状态码：`0` 成功、`1001` 参数校验失败、`1002` 业务规则校验失败、`1003` 未登录、`1004` 无权限、`1005` 数据不存在、`5000` 系统错误。

### 5.2 接口分组（共 65 个）

| 模块 | 接口前缀 | 主要接口 | 允许角色 |
| --- | --- | --- | --- |
| 账户 | `/api/account/*` | register、login、logout、profile、modifyInfo、modifyPassword、list、pending、audit、lock、baseData、saveBaseData | 公开/登录/admin |
| 系统 | `/api/system/info` | 系统与运行模式信息 | 公开 |
| 报修 | `/api/report/*` | submit、list、detail、cancel、urge、audit、pendingAudit、statistics、options、overdue | reporter / manager / admin |
| 派单 | `/api/dispatch/*` | pending、candidates、auto、manual、accept、transfer、refuse、myTasks、workers、recalculate、remind | manager / worker |
| 维修 | `/api/repair/*` | start、feedback、material、finish、confirm、detail、statistics、status | worker / reporter / manager |
| 耗材 | `/api/material/*` | list、save、delete、addStock | 登录 / manager / admin |
| 评价 | `/api/eval/*` | submit、list、reply、audit、pending、statistics | reporter / manager / admin |
| 消息 | `/api/message/*` | list、unread、read、readAll | 登录 |
| 统计 | `/api/stat/*` | overview、category、building、worker、monthly、material、report、auditEval | manager / admin |

### 5.3 鉴权与会话

- 登录成功后创建会话并下发 `CRS_TOKEN` Cookie（前端同时保存于 localStorage 以便页面携带）。
- 每个接口通过 `@Route(roles = {...})` 声明访问角色，`Dispatcher` 统一校验：未登录返回 `1003`，
  角色不符返回 `1004`；`publicRoute = true` 的接口（登录、注册、系统信息）无需登录。
- 会话空闲 2 小时失效；重复登录不影响其他会话；注销即失效并同步维修工离线状态。
- 登录失败 5 次自动锁定账户，需系统管理员解锁。

---

## 六、与设计书的差异与补充说明

系统在实现设计书要求的基础上，对设计书中未展开或存在冲突的部分做了明确取舍，均已在代码注释中标注：

| 序号 | 设计书内容 | 实现处理 | 原因 |
| --- | --- | --- | --- |
| 1 | 表 2.11 `t_repair_order.status` 仅定义到 `6 已撤销` | 补充 `7 已驳回` 与 `reject_reason` 字段 | 设计书 1.1.2.3 要求"不受理的报修单需填写退回原因并通知报修人"，原状态枚举无法承载 |
| 2 | 智能派单仅出现在类设计（`smartDispatch()`/`calcMatchScore()`）与方法表，无需求与设计正文 | 补充可解释的加权评分算法：技能 40 + 在单量 30 + 在线 15 + 紧急响应 15；保留人工派单兜底 | 使类设计中的方法具备可实现、可验证的语义 |
| 3 | 接单规则写"状态由待接单变为维修中"，而"开始维修"前置条件写"待接单或维修中" | 接单 → `维修中`；"开始维修"语义为到场确认并记录开始时间/进度，可在维修中重复调用 | 消除状态机歧义，保留两个功能点 |
| 4 | 转单只在类设计 `DispatchController.transfer()` 出现 | 完整实现转单：退回派单池 + 通知管理员，可指定接手人重派 | 覆盖表 1.1"申请转单或退单"的需求 |
| 5 | 表 2.15 耗材使用表仅 5 个字段 | 表结构保持一致，通过 `t_material.unit_price` 关联计算金额（`MaterialUsage.getAmount()`）用于成本统计 | 不改变设计书物理结构，同时在统计模块满足成本核算需求 |
| 6 | 维修进度反馈无对应表 | 新增 `t_progress` 表记录进度与延期原因 | 表 1.10 要求"进度反馈""延期原因必填"，需持久化留痕 |
| 7 | 基础数据（楼栋/设备类型/工种）维护无对应表 | 新增 `t_base_data` 表 | 1.1.1 明确系统管理员需维护这些基础数据 |
| 8 | 数据访问层提到 MyBatis/Hibernate | 采用 **MyBatis 3.5.16 正式实现**：10 个 Mapper 接口 + 10 个 Mapper XML（99 条语句），SQL 位于 `src/main/resources/mapper/*.xml`；另保留手写 SQL 的 `dao/jdbc/` 与 `dao/memory/` 作为对照实现，通过 `storage.mode` 一步切换 | 完全对齐设计书"数据访问层由 MyBatis 实现"的描述；保留等价实现便于对照教学与无数据库环境演示 |
| 9 | 表 2.15 无唯一约束 | 增加 `uk_usage_task_mat` 唯一键，登记同一耗材时累加数量 | 防止同一任务同一耗材出现重复记录 |
| 10 | 界面形态为 JSP 页面 | 前端为静态页面 + JSON 接口（同一套接口契约），视图层由 `webapp/*.html` + `js/` 承担；工程内未放置 `.jsp` 文件 | 静态页面便于自动化验证与团队分工；`web.xml` 已保留 `jsp-config` 与 `/WEB-INF/jsp/*` 访问保护，如需 JSP 形态可直接在 `webapp/WEB-INF/jsp/` 下补充视图，后端接口无需改动 |

---

## 七、验证证据

### 7.1 业务自测（`scripts/build.ps1 -WithSelfTest`）

18 组用例、**99 项断言全部通过**，报告输出至 `build/selftest.log`。覆盖：

账户注册与唯一性校验、实名审核与登录控制、登录失败锁定阈值、报修提交与必填校验、
楼栋基础数据校验、报修审核（受理/驳回必须填原因）、**智能派单匹配度算法**、
派单/接单/退单/转单状态机、维修开始与进度反馈、**耗材超库存拦截与事务一致性**、
维修完成登记、**结果确认不通过后的耗材回滚**、评价与审核回复、撤销与催办规则、
统计指标（完成率/平均分/成本/趋势）、消息通知与在线状态、接单超时提醒、
**审核局部更新不清空业务字段的回归校验**、在单量与进行中任务数一致性。

### 7.2 端到端接口验证（`scripts/http-e2e.ps1`）

16 个分组、**97 项断言全部通过**，报告输出至 `build/http-e2e.log`。覆盖：

静态资源与 15 个功能页面可访问性、五种角色登录、错误密码与不存在用户拦截、
未登录与越权访问拦截（4 类）、提交报修与业务规则校验、报修审核、
智能派单匹配度明细、派单与接单、非本人任务操作拦截、维修处理与进度反馈、
耗材超库存拦截与库存扣减、维修完成登记与状态流转、结果确认与耗材回滚、
评价与评价审核回复、统计报表全部指标、撤销与催办规则、报修人数据隔离、
账户审核/锁定/解锁/基础数据维护、账户信息与密码维护、消息已读、超时监督与耗材管理。

**各验证形态均已通过**：

| 验证形态 | 运行方式 | 自测 | 端到端 |
| --- | --- | --- | --- |
| Tomcat 11 · 根上下文 `/` | IDEA/`startup.bat` 部署，`storage.mode=memory` | 99/99 通过 | 97/97 通过 |
| Tomcat 11 · 非根上下文 `/campus_repair_system_war` | 同上（前端已做上下文自适应） | — | 97/97 通过 |
| **Tomcat 11 + MyBatis Mapper + MySQL**（当前部署形态） | `web.xml` 设 `storage.mode=mybatis` | 99/99 通过 | **97/97 通过**（`build/http-e2e.log`） |
| MyBatis Mapper 层定点验证 | `test/.../MyBatisCheck.java`（在**部署目录的类路径**上运行） | **39/39 通过**（`build/mybatis-check.log`） | — |

> 说明：项目运行方式为"IDEA + Tomcat"；MyBatis 一行是在 Tomcat + MySQL 真实环境下测得的，
> 启动自检确认 `存储模式=mybatis，数据访问=MyBatis Mapper（resources/mapper/*.xml），
> 数据库=OK: MySQL 8.0.25（MyBatis MySQL Connector/J 连接池）`。
> 其中 `MyBatisCheck` 特意只把**部署目录**（`WEB-INF/classes` + `WEB-INF/lib/*.jar`）放进类路径运行，
> 用于证明 MyBatis 配置确实随 WAR 一起部署生效，而不是靠源码目录里的 `src/main/resources` 兜底。

### 7.3 定点验证程序

| 程序 | 用途 |
| --- | --- |
| `test/.../SelfTestRunner.java` | 主业务自测（99 项断言，容器无关的 Service/DAO 层） |
| `test/.../MyBatisCheck.java` | **MyBatis Mapper 层定点验证（39 项断言）** |
| `test/.../ParamMapTest.java` | 请求参数解析（JSON/表单/中文/数组） |
| `test/.../DispatchCheck.java` | 智能派单得分与演示数据技能匹配核对 |
| `test/.../CategoryTrace.java` | 报修单类别在「提交→审核→派单」链路上不被清空的回归验证 |
| `test/.../JdbcFlowCheck.java` | JDBC 模式下派单→接单→开始维修的时间字段持久化核对 |
| `test/.../ConfigProbe.java` | 配置优先级（`-D` 参数覆盖 `config.properties`）核对 |

### 7.4 实施过程中发现并修复的真实缺陷

1. **审核局部更新清空业务字段**：`RepairOrderDao.update()` 为整行覆盖式更新，而审核时只设置优先级，
   导致报修类别、地点、描述被写成 NULL，进而使智能派单技能匹配失效（技能得分 0）。已改为
   「仅更新非空字段」，并新增回归断言。
2. **MySQL 时间字段读取为空**：MySQL Connector/J 8.x 默认把 `DATETIME` 映射为 `java.time.LocalDateTime`，
   原映射器仅识别 `java.util.Date`，导致 JDBC 模式下派单时间、开始时间、完成时间、注册时间读取为 null
   （表现为超时监督与月度趋势统计无数据）。已在 `RowMapper` 中统一转换。
3. **耗材使用与耗材信息未关联**：JDBC 实现未带出耗材名称与单价，导致成本统计恒为 0。已补 LEFT JOIN。
4. **DDL 建表顺序错误**：`t_repair_task` 的外键指向尚未创建的 `t_worker`，脚本执行失败。已调整顺序。
5. **Tomcat 部署下所有接口 404（"点了没反应"的直接原因）**：`ApiServlet` 把含上下文路径的
   `request.getRequestURI()`（如 `/campus_repair_system_war/api/account/login`）直接交给路由表，
   而路由表以 `/api/...` 为基准，导致全部接口返回"接口不存在"。已改为按 `getContextPath()` 剥离前缀。
6. **前端接口路径写死为根路径**：所有页面用 `fetch('/api/...')`，部署到非根上下文时请求打到根上下文。
   已改为上下文自适应：页面注入 `<base>` 与 `window.__CRS_BASE__`，`common.js` 的 `App.url()` 统一拼接前缀，
   静态资源改为相对路径。
7. **Cookie 路径冲突导致"登录后立即未登录"**：前端曾以 `document.cookie` 写 `CRS_TOKEN; path=/`，
   与容器下发的 `path=/应用上下文` 形成两个同名 Cookie，服务端可能取到过期/无效的那个。
   已改为按上下文路径写入，且服务端会从候选令牌中挑选能对应有效会话的那个。
8. **Servlet 参数读取冲突**：`ApiServlet` 先 `getParameter()` 再读 `getInputStream()`，
   在 Tomcat 下会拿不到 JSON 请求体。已拆分为「JSON 体自行解析 / 表单参数交由容器解析」两条互斥路径，
   并新增 `Dispatcher.dispatchWithParams()` 供容器适配层复用（避免重复读体）。
9. **配置口令为空的静默失败**：`web.xml` 的 `db.password` 为空时，jdbc 模式连接失败却只表现为"用户名或密码错误"；
   已支持环境变量覆盖（`CAMPUS_DB_PASSWORD`），并在 `CampusContextListener` 增加启动自检，
   日志明确打印存储模式、数据库连通性与**可用账户数**，账号为 0 时给出明确处置提示。
10. **IDEA 模块残留错误 Web Facet**：`campus-repair-system.iml` 中曾同时配置 `webapp` 与已删除的
   `src/main/webapp` 两个 Web Facet，会导致 Artifact 选错 Web 根。已清理为单一 Facet（仅 `webapp`）。
11. **页面跳转链接写成根路径绝对地址（"无法注册"的直接原因）**：登录页的「立即注册」链接是
   `/register.html`，在 `/campus_repair_system_war` 上下文下会被解析成上下文之外的地址而 404；
   同类问题共 10 处（登录↔注册互跳、404 页返回按钮、侧边栏导航、列表里的详情/处理链接、
   登录成功后跳首页、注销后跳登录页）。已全部改为**相对路径**（`register.html`、`task.html?taskId=…` 等），
   配合页面顶部注入的 `<base>` 在任意上下文下都能正确解析；`App.url()` 也加了防重复拼接保护。
12. **IDEA 打包 WAR 时丢掉 MyBatis 资源配置（"数据库连接失败"的直接原因）**：那次构建是**增量构建**
   （JPS 日志为 `No changes found since last build`、`Affected build targets count: 0`），没有把
   `src/main/resources` 拷进模块输出目录，于是 artifact 的 `WEB-INF/classes` 下只有 `com/`，
   缺少 `mybatis-config.xml` 与 `mapper/*.xml`；`MyBatisSessionFactory.init()` 抛出
   `MyBatis 配置加载失败：mybatis-config.xml`，而启动自检把它笼统归因为"数据库连接失败"，
   让人误以为要去查数据库账号密码。已做两处修复：
   （a）把运行资源镜像到 `webapp/WEB-INF/classes/`（`build.cmd` 每次编译自动同步）——IDEA 打包时
   必定原样复制 Web 根，资源因此不再依赖增量构建是否拷贝了 `src/main/resources`；
   （b）启动自检区分"配置未进入类路径"与"数据库连不上"两种失败，不再给出误导性提示。
13. **两处只能跑一次的验证用例**：`MyBatisCheck` 用绝对值断言"派单后在单量 == 1、确认后 == 0"，
   `http-e2e.ps1` 把资料修改用的手机号写死为 `13911110009`；只要这两个脚本在此之前跑过一次，
   第二次就必然失败（分别表现为在单量不是 1、`该手机号已被其他账户使用`）。已分别改为按增量断言、
   按运行时刻生成手机号，两个验证脚本现在都可重复执行。

---

## 八、部署与扩展

### 8.1 运行方式：IDEA + Tomcat（唯一运行方式）

项目已按"容器部署"收敛：**不存在内置服务器启动类**，运行入口只有两个 Servlet 组件。

**适配层文件**

| 文件 | 作用 |
| --- | --- |
| `src/servlet-adapter/java/.../ApiServlet.java` | 前端控制器 Servlet，映射 `/api/*`，负责参数适配（JSON/表单）、上下文路径剥离、调度与结果渲染 |
| `src/servlet-adapter/java/.../CampusContextListener.java` | 读取 `web.xml` 上下文参数、初始化数据源、装载内存库演示数据、启动自检并打印可用账户数 |
| `src/main/java/.../boot/DemoDataLoader.java` | 内存库演示数据（与 `db/seed.sql` 一致），由上面的监听器调用 |
| `webapp/WEB-INF/web.xml` | 部署描述文件（servlet 映射、上下文参数、会话配置、JSP 视图保护） |
| `webapp/WEB-INF/classes/` | 运行资源镜像（`mybatis-config.xml`、`mapper/*.xml`、`config.properties`），随 Web 根一起打进 WAR |

### 8.2 部署步骤（IDEA + Tomcat 10/11，已实测通过）

1. IDEA → `File → Project Structure → Artifacts`：新增 *Web Application: Exploded*，
   输出目录自定（如 `target/campus-repair`）：
   - `Web resource directory` 指向项目内的 **`webapp`** 目录；
   - `Available Elements` 中把 **`campus-repair-system compile output`** 加入
     `WEB-INF/classes`；
   - 把 `lib/` 下 **4 个 jar** 全部加入 `WEB-INF/lib`：
     `mybatis-3.5.16.jar`、`mysql-connector-j-8.3.0.jar`、`slf4j-api-1.7.36.jar`、`slf4j-simple-1.7.36.jar`
     （mybatis 模式下缺任何一个都会在启动自检里报错）。
   - 其余无需手工添加：`webapp/WEB-INF/classes/` 下已带 `mybatis-config.xml`、`mapper/*.xml`
     与 `config.properties`（由 `build.cmd` 从 `src/main/resources` 同步）。
2. IDEA → `Run → Edit Configurations → Tomcat Server → Local`：
   - `Deployment` 标签页 `+` 选择上面的 artifact，**Application context 建议填 `/`**
     （填 `/campus_repair_system_war` 也能正常工作，前端已做上下文路径自适应）；
   - 确认 JDK 为 17 及以上（本项目已在 JDK 25 验证）。
3. 启动后浏览器访问 `http://localhost:8080/`（根上下文）或
   `http://localhost:8080/<你的上下文>/`，用 `student / 123456` 登录。
4. 启动日志（IDEA 控制台与 `logs/localhost.*.log`）会出现自检信息，务必确认这一行的形态：
   ```
   :: mybatis / jdbc 模式（已连上数据库）
   [CampusRepair] 校园报修系统启动完成：存储模式=mybatis，数据访问=MyBatis Mapper（resources/mapper/*.xml），
   数据库=OK: MySQL 8.0.25（MyBatis MySQL Connector/J 连接池），可用账户数=8

   :: memory 模式
   [CampusRepair] 校园报修系统启动完成：存储模式=memory，数据库=（内存库模式，未连接数据库），可用账户数=8
   ```
   `可用账户数` 为 0 或负数、`数据库=FAILED…`，都表示**数据访问层没准备好**，登录必然失败：
   - `FAILED: MyBatis 配置未进入类路径…` → 部署包的 `WEB-INF/classes` 下少了 `mybatis-config.xml` 或 `mapper/*.xml`
     （累计执行一次 `build.cmd` 重新同步后再构建 artifact，详见 8.6 排查表）；
   - `FAILED` 其他信息 → 数据库账号、库名或 MySQL 服务本身的问题。

**命令行方式（不依赖 IDEA，可选）**

```bat
:: 1) 编译全部源码（build.ps1 会自动把 Tomcat 的 servlet-api/jsp-api 加入 classpath）
build.cmd

:: 2) 组装部署目录（<APP> 为 Tomcat webapps 下的应用目录）
::    webapp\* 已包含 WEB-INF\classes 下的 MyBatis 配置与 mapper，无需另拷
xcopy /E /Y webapp\*              <APP>\
xcopy /E /Y build\classes\com     <APP>\WEB-INF\classes\com\
copy /Y lib\*.jar                 <APP>\WEB-INF\lib\

:: 3) 启动
set JAVA_HOME=D:\Java
<Tomcat>\bin\startup.bat
```

### 8.3 数据访问层：MyBatis Mapper（设计书 2.2.6 的 MyBatis 实现）

**存储模式（`storage.mode`）**

| 取值 | 数据访问实现 | 用途 |
| --- | --- | --- |
| `mybatis`（**默认/正式部署**） | `dao/mybatis/MyBatisXxxDaoImpl` + `resources/mapper/*.xml` | MyBatis Mapper + MySQL，SQL 集中管理 |
| `jdbc` | `dao/jdbc/JdbcXxxDaoImpl`（手写 SQL） | 对照实现，便于比较两种写法 |
| `memory` | `dao/memory/MemoryXxxDaoImpl` | 无数据库演示（内置演示数据） |

**文件构成**

| 位置 | 内容 |
| --- | --- |
| `src/main/resources/mybatis-config.xml` | 全局配置：POOLED 数据源（连接参数来自 `db.*`）、JDBC 事务、10 个 Mapper 注册、类型别名 |
| `src/main/resources/mapper/*.xml` | 10 个 Mapper XML，共 **99 条映射语句**（`select`/`insert`/`update`/`delete`） |
| `src/main/java/.../dao/mapper/*.java` | 10 个 Mapper 接口 + `RepairOrderQuery`（分页查询入参对象） |
| `src/main/java/.../dao/mybatis/MyBatisSessionFactory.java` | SqlSessionFactory 初始化、SqlSession 获取、**事务 begin/commit/rollback**、连接自检 |
| `src/main/java/.../dao/mybatis/MyBatisDaoSupport.java` | DAO 公共基类：`query()` / `mutate()` 包装会话与自动提交 |
| `src/main/java/.../dao/mybatis/MyBatisXxxDaoImpl.java` | 10 个 DAO 实现：仅「取 Mapper → 调方法 → 转换返回值」，**不含任何 SQL 字符串** |
| `src/main/java/.../dal/MyBatisCapabilityProbe.java` | 自检工具：报告实际使用的 DAO 实现类与已注册 Mapper |

**与设计书对应的关键语句**

- `RepairOrderMapper.findByCondition / countByCondition`：对应设计书 2.2.3.2 的
  `findByCondition()` 按条件分页查询，条件用 `<where>` + `<if>` 动态拼接，分页用 `LIMIT/OFFSET`；
  查询通过 LEFT JOIN 一次性带出报修人、当前有效维修任务、维修人员与评价信息。
- `RepairOrderMapper.updateSelective`：局部更新（仅覆盖非空字段），
  保证"审核只调整优先级"不会清空类别/地点/描述等其他业务字段。
- `MaterialMapper.deductStock`：条件更新 `SET stock = stock - ? WHERE mat_id = ? AND stock >= ?`，
  库存不足时影响 0 行，由 Service 抛出"库存不足"业务异常，并发下不会出现负库存。
- `RepairTaskMapper.countActiveByWorker`、`EvaluationMapper.avgScore` 等聚合语句支撑统计模块。
- 事务：`MyBatisSessionFactory.begin/commit/rollback` 配合 `TxTemplate`，
  使「保存维修结果 + 扣减库存 + 写入耗材使用记录 + 更新报修单状态」在同一 SqlSession 事务内完成
  （满足设计书"库存扣减与耗材使用记录的写入需在同一个事务中完成"）。

**部署所需 jar（放入 `WEB-INF/lib`）**

```
mybatis-3.5.16.jar        MyBatis 核心
mysql-connector-j-8.3.0.jar  MySQL 驱动
slf4j-api-1.7.36.jar      MyBatis 日志 API
slf4j-simple-1.7.36.jar   简易日志实现（可选，便于在控制台看到执行的 SQL）
```

> 说明：`src` 全量编译（含 MyBatis 实现）需要这些 jar 在 classpath 上，
> `scripts/build.ps1` 会自动把 `lib/*.jar` 加入编译与运行 classpath。

**启动自检会明确报告实际生效的实现**，例如：

```
[CampusRepair] 校园报修系统启动完成：存储模式=mybatis，数据访问=MyBatis Mapper（resources/mapper/*.xml），
数据库=OK: MySQL 8.0.25（MyBatis MySQL Connector/J 连接池），可用账户数=8
```

### 8.4 配置项与生效位置

| 配置项 | 取值位置（Tomcat 部署） | 说明 |
| --- | --- | --- |
| `storage.mode` | `WEB-INF/web.xml` 的 `context-param`（当前为 `mybatis`） | `mybatis` / `jdbc` / `memory` |
| `db.url` / `db.username` / `db.password` | `WEB-INF/web.xml` 的 `context-param` | MyBatis 数据源连接参数 |
| `dispatch.acceptDeadlineMinutes` | `WEB-INF/web.xml` 的 `context-param` | 接单时限（表 1.8：2 小时） |
| `order.confirmDeadlineHours` | `WEB-INF/web.xml` 的 `context-param` | 结果确认时限（表 1.9：24 小时） |
| 监听端口 / 上下文路径 | Tomcat 的 `conf/server.xml` 与 IDEA 的运行配置 | 与本项目配置无关 |

**配置优先级（高 → 低）**：环境变量（如 `CAMPUS_DB_PASSWORD`）> `web.xml` 上下文参数（会被注入为系统属性）>
classpath 下 `config.properties`（仅作默认值）> 内置默认值。

推荐用环境变量传库密码，避免明文写进仓库（IDEA：Tomcat 运行配置 → `Environment variables`）：

```bat
set CAMPUS_DB_PASSWORD=你的密码
```

（环境变量名规则：`CAMPUS_` + 配置项大写并把 `.` 换成 `_`，例如 `db.password` → `CAMPUS_DB_PASSWORD`。）

### 8.5 扩展点

- **替换数据访问框架**：`dao/` 下 10 个 DAO 接口已有 MyBatis、手写 SQL、内存三套实现，
  只需改 `storage.mode` 或在 `DaoFactory` 中切换实现类，Service 层无需改动
  （设计书 1.2 可维护性要求）。新增实体只需补 Mapper 接口 + Mapper XML，并在
  `mybatis-config.xml` 的 `<mappers>` 中注册。
- **替换会话存储**：`web/SessionManager` 可改为 Redis 实现，支持多实例部署。
- **新增功能模块**：新增 `XxxController` + `@Route` 注解方法即可自动注册路由，
  无需修改 `Dispatcher`。

### 8.6 部署排查表（"点了没反应"逐项对照）

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 登录页能打开，点「登录」无任何反应（右上角仅有短暂红色提示） | 接口请求 404，`/api/*` 未部署或路径不对 | 确认 `WEB-INF/classes` 下有 `com/campus/repair/web/servlet/ApiServlet.class`；打开浏览器 F12 → Network，看 `/api/account/login` 的状态码与响应体 |
| 接口返回 `{"code":1005,"message":"接口不存在：/应用上下文/api/..."}` | 上下文路径未被剥离（旧版本缺陷，已修复） | 使用当前版本代码；`ApiServlet` 已按 `request.getContextPath()` 剥离前缀 |
| 接口返回 `{"code":1002,"message":"用户名或密码错误"}` | 账号数据源为空：内存库演示数据未装载，或 jdbc 模式未执行建库脚本 / 库密码不对 | 看启动自检日志的 `可用账户数`；为 0 时：改用 `storage.mode=memory` 快速验证，或执行 `db/schema.sql` + `db/seed.sql` 并核对 `db.password` |
| 启动日志出现 `数据库=FAILED: MyBatis 配置未进入类路径…`，后跟一条 `[错误] 数据库连接失败…` | MyBatis 的 XML 配置没被打进 WAR：`WEB-INF/classes` 下缺 `mybatis-config.xml` 或 `mapper/*.xml`（IDEA 增量构建没拷贝 `src/main/resources` 时会出现） | 与数据库账号密码无关。执行一次 `build.cmd`（会把 `src/main/resources` 同步到 `webapp/WEB-INF/classes`），确认该目录下有 `mybatis-config.xml` 与 `mapper/` 后再重新构建 artifact 并重启 |
| 启动日志出现 `数据库=FAILED: …` 且提示的是连接或认证失败 | `db.url/db.username/db.password` 与本机 MySQL 不一致 | 修正 `web.xml` 的 context-param，或设置环境变量 `CAMPUS_DB_PASSWORD` |
| `数据库=OK` 但 `可用账户数=0`（或为负数） | 建库脚本没执行，或连到了另一个空库 | 执行 `db/schema.sql` + `db/seed.sql`，并核对 `db.url` 中的库名 |
| 接口返回 500 且日志有 `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | `WEB-INF/lib` 缺少 MySQL 驱动 | 把 `lib/mysql-connector-j-8.3.0.jar` 复制进 `WEB-INF/lib` |
| 登录成功但后续接口返回 `1003 登录状态已失效` | 浏览器同时携带两个同名 `CRS_TOKEN` Cookie（旧版本以 `path=/` 写入所致） | 使用当前版本（Cookie 按上下文路径写入，且服务端会挑选有效令牌）；清一次浏览器 Cookie 即可 |
| 应用启动失败，日志提示 servlet 映射冲突 | 同一 Tomcat 中其他应用（如 `SecondPractice_041_war`）自身配置有误，与本项目无关 | 在 IDEA 的 Tomcat 配置中移除该应用的 Deployment，或单独为它排查 |
| IDEA 里 Artifact 的 Web 根指向一个不存在的目录 | 模块曾有多个 Web Facet（历史遗留 `src/main/webapp`） | 已清理为单一 Facet（仅 `webapp`）；若仍异常，删掉模块 Facet 后重新添加 Web Facet 指向 `webapp` 即可 |
| 点「还没有账户？立即注册」跳到 404（或登录成功后停在 404） | 页面里的跳转链接写成根路径绝对地址（如 `/register.html`），在 `/应用上下文` 部署下会跳到上下文之外 | 已修复：页面跳转与静态资源全部改为**相对路径**（配合页面顶部注入的 `<base>`，任意上下文均可解析）；若你自行加链接，请不要用 `/xxx.html` 这种根路径写法 |
