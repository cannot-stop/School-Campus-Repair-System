# 校园报修系统 · 交付验证报告

- **交付日期**：2026-09-14
- **依据文档**：《校园报修系统需求分析和系统详细设计书》（781 段、25 张表格、14 张图）
- **交付目录**：`D:\DeepSeek Harness\campus-repair-system`
- **运行环境**：JDK 25（`D:\Java`）+ MySQL 8.0.25（本地 `MySQL80` 服务）+ Windows PowerShell 5.1

---

## 一、验证结论

| 验证项 | 方式 | 结果 |
| --- | --- | --- |
| 源码编译 | `build.cmd -Clean -WithSelfTest`（javac，115 个源文件，含 Servlet 适配层与 MyBatis Dao） | ✅ 编译通过（退出码 0） |
| 业务规则自测 | `build/selftest.log`（18 组用例，容器无关的 Service/DAO 层） | ✅ **99 项断言全部通过** |
| **MyBatis Mapper 层定点验证** | `build/mybatis-check.log`（`test/.../MyBatisCheck.java`） | ✅ **39 项断言全部通过** |
| **端到端接口验证（Tomcat 11 + MyBatis + MySQL，当前部署形态）** | `build/http-e2e.log`（16 组用例，`scripts/http-e2e.ps1`） | ✅ **97 项断言全部通过** |
| 端到端接口验证（Tomcat 11 · 根上下文 + 内存库） | 同一脚本的内存库形态运行 | ✅ **97 项断言全部通过** |
| 端到端接口验证（Tomcat 11 · 非根上下文 `/campus_repair_system_war`） | 同一脚本，`-Base` 指向该上下文 | ✅ **97 项断言全部通过** |
| **MyBatis 配置随 WAR 部署生效** | 只把**部署目录**（`WEB-INF/classes` + `WEB-INF/lib/*.jar`）加入类路径运行 `MyBatisCheck` | ✅ 39/39 通过，自检输出「已加载 mybatis-config.xml / OK: MySQL 8.0.25」 |
| 前端脚本语法 | 全部 15 个 `.js` 经 `node --check` 校验 | ✅ 15/15 通过 |
| 数据库脚本 | `db/schema.sql` + `db/seed.sql` 在 MySQL 8.0.25 实际执行 | ✅ 建表 10 张、外键与唯一约束生效、演示数据导入成功 |
| 中文数据往返 | 表数据查询比对（楼栋、类别、技能标签、描述） | ✅ utf8mb4 全程无乱码 |
| 前端页面可访问性 | 15 个功能页面 + 登录页 HTTP 200（根上下文与 `/campus_repair_system_war` 均验证） | ✅ 全部可访问 |
| 容器部署启动自检 | Tomcat `logs/localhost.*.log` | ✅ 输出「存储模式=mybatis，数据访问=MyBatis Mapper，数据库=OK: MySQL 8.0.25，可用账户数=8」 |
| 编译校验脚本 | `build.cmd`、`build.cmd -WithSelfTest` 实际执行 | ✅ 编译与自测均成功 |

**MyBatis Mapper 层验证明细（39 项）**：10 个 DAO 实现类均为 MyBatis 版本；10 个 Mapper 接口全部注册；
用户/维修工/基础数据/技能模糊查询可用；提交报修（含主键回填）、LEFT JOIN 带出报修人、
`updateSelective` 局部更新（审核只改优先级不清空其他字段）、按条件分页查询（`<where>+<if>+LIMIT`）、
人工派单、在单量增减、接单与开始维修的时间字段写入回读、条件更新扣减库存、
耗材使用带出名称与单价、完成登记（SQL `NOW()`）、结果确认归档、
评价增改与平均分聚合、统计总览/类别/绩效/成本/月度趋势、
超库存登记被拒且库存不变（MyBatis SqlSession 事务）、`update_time` 由 SQL 维护。

---

## 二、端到端验证覆盖明细（97 项）

| 分组 | 断言数 | 关键验证点 |
| --- | --- | --- |
| 1. 系统信息与静态资源 | 5 | 运行模式、登录页、前端脚本/样式、15 个功能页面可访问 |
| 2. 角色登录 | 7 | 5 类角色登录、错误密码、不存在用户 |
| 3. 未登录与越权拦截 | 4 | 未登录 1003、报修人/维修工/管理员跨角色 1004 |
| 4. 提交报修 | 5 | 表单选项加载、提交成功、状态待审核、非法楼栋、必填校验 |
| 5. 报修审核 | 2 | 越权拦截、受理后转待派单 |
| 6. 智能派单匹配度 | 4 | 候选排序、技能 40 分、总分上限、推荐理由 |
| 7. 派单与接单 | 7 | 智能派单、任务创建、状态流转、重复派单拦截、接单、越权拦截 |
| 8. 维修处理与耗材 | 9 | 开始维修、进度/延期反馈、超库存拦截、库存不变、扣减、结果必填、完成登记 |
| 9. 结果确认与回滚 | 6 | 不通过退回、**库存回滚**、重登记、再次完成、确认归档、任务列表 |
| 10. 评价与审核 | 4 | 提交、重复拦截、审核通过、回复 |
| 11. 统计分析 | 8 | 总览、类别、绩效、耗材成本、状态分布、楼栋、月度趋势、评价统计 |
| 12. 报修人业务规则 | 6 | 不可催办状态、可撤销、已完成不可撤销、越权撤销、数据隔离 |
| 13. 系统管理员 | 15 | 账户列表、在线会话、注册、重复拦截、待审核列表、未审核不能登录、审核、登录、通知、锁定/解锁、基础数据增删 |
| 14. 账户信息维护 | 6 | 信息查询、手机号校验、修改信息、原密码校验、改密、新密码登录 |
| 15. 消息通知 | 3 | 消息列表、全部已读、未读数归零 |
| 16. 维修监督与耗材管理 | 7 | 超时监督、在单量校正、超时提醒、耗材增/补/删、维修工状态位置 |

## 三、业务自测覆盖明细（99 项，18 组用例）

注册与唯一性（4）、实名审核与登录（2）、登录失败锁定阈值（1）、基础数据与维修工档案（1）、
提交报修（3+2 拦截）、报修审核与驳回（2+2 拦截）、智能派单算法（7）、派单接单状态机（6+3 拦截）、
维修处理（4+1 拦截）、耗材事务一致性（5+1 拦截）、结果确认与回滚（3）、评价（2+3 拦截）、
催办与撤销（3+2 拦截）、驳回与退单转单（5+3 拦截）、统计分析（8）、消息与在线状态（3）、
账户维护与权限（3+4 拦截）、数据一致性（1）、审核不清空业务字段回归（3）。

---

## 四、实施过程中发现并修复的真实缺陷

| # | 缺陷 | 影响 | 修复 |
| --- | --- | --- | --- |
| 1 | `RepairOrderDao.update()` 整行覆盖，审核时只设优先级导致类别/地点/描述被清空 | 智能派单技能匹配恒为 0，报修单信息丢失 | 改为仅更新非空字段；`SelfTestRunner` 增加 3 项回归断言，`CategoryTrace` 定点验证 |
| 2 | MySQL Connector/J 8.x 将 DATETIME 映射为 `java.time.LocalDateTime`，原映射器只识别 `java.util.Date` | JDBC 模式下派单/开始/完成/注册时间为 null，超时监督、月度趋势统计无数据 | `RowMapper` 增加 `LocalDateTime/LocalDate/LocalTime` 转换；`JdbcFlowCheck` 定点验证 |
| 3 | JDBC 耗材使用查询未关联耗材表 | 耗材成本统计恒为 0 | 补 `LEFT JOIN t_material`，带出名称、规格、单价 |
| 4 | `schema.sql` 中 `t_repair_task` 外键指向尚未创建的 `t_worker` | 建表脚本执行失败 | 调整建表顺序（t_worker 先于 t_repair_task） |
| 5 | 演示账号在 SQL 与内存数据中命名不一致 | 同一套演示脚本无法跨模式使用 | 统一为 `student`/`teacher`/`worker01-03`/`manager`/`admin` |
| 6 | `ApiServlet` 把含上下文路径的 `requestURI` 当作路由路径（Tomcat 部署后所有接口 404/"接口不存在"） | 界面能打开但任何操作都无反应——**用户实际遇到的问题** | 按 `request.getContextPath()` 剥离前缀后交给路由；Tomcat 下 97 项端到端断言验证通过 |
| 7 | 前端接口调用写死根路径 `fetch('/api/...')` | 部署到非根上下文（如 `/campus_repair_system_war`）时请求全部打空 | 接口调用统一经 `common.js` 的 `App.url()` 拼接上下文前缀；页面顶部注入 `<base>`，页面跳转与静态资源全部改为相对路径 |
| 8 | 前端用 `document.cookie` 写 `CRS_TOKEN; path=/`，与容器下发的 `path=/上下文` 形成两个同名 Cookie | 登录成功但后续接口返回 1003「登录状态已失效」 | Cookie 按上下文路径写入并清理旧 Cookie；服务端从候选令牌中挑选能对应有效会话的那个（已用"双 Cookie"场景实测） |
| 9 | `ApiServlet` 先 `getParameter()` 再读 `getInputStream()`（Servlet 规范下二选一） | Tomcat 下 JSON 请求体读取为参数失败 | 拆分为 JSON 体自行解析 / 表单参数交由容器解析两条互斥路径；新增 `Dispatcher.dispatchWithParams()` 供适配层复用 |
| 10 | `web.xml` 中 `db.password` 为空导致 jdbc 模式连接失败，但对外只表现为"用户名或密码错误" | 部署后登录失败原因不可见 | 支持环境变量覆盖（`CAMPUS_DB_PASSWORD`）；`CampusContextListener` 增加启动自检，明确打印存储模式、数据库连通性与可用账户数 |
| 11 | 页面跳转链接写成根路径绝对地址（登录页 `/register.html` 等共 10 处） | 在非根上下文部署下点击「立即注册」跳到 404——**"可以登录但无法注册"的原因** | 全部改为相对路径（`register.html`、`task.html?taskId=…`）；`App.url()` 增加防重复拼接保护；已用「登录页→注册页→提交注册→管理员审核→登录→提交报修」整链路实测 |
| 12 | IDEA 那次是**增量构建**（`No changes found since last build`），没把 `src/main/resources` 拷进模块输出目录，artifact 的 `WEB-INF/classes` 只有 `com/`，缺 `mybatis-config.xml` 与 `mapper/*.xml`；而启动自检把它笼统报成"数据库连接失败" | 界面能打开但登录失败，`MyBatisSessionFactory` 抛 `MyBatis 配置加载失败：mybatis-config.xml`——**用户实际遇到的"数据库连接失败"**（与库账号密码无关） | （a）把运行资源镜像到 `webapp/WEB-INF/classes/` 并由 `build.cmd` 每次编译自动同步：IDEA 打包时必定原样复制 Web 根，资源不再依赖增量构建；（b）`MyBatisSessionFactory` 与启动自检区分"配置未进入类路径"与"数据库连不上"两种失败 |
| 13 | `MyBatisCheck` 用绝对值断言在单量（`== 1` / `== 0`），`http-e2e.ps1` 把资料修改用的手机号写死为 `13911110009` | 两个验证脚本都只能跑一次，第二次必然失败（表现为"在单量不是 1"、"该手机号已被其他账户使用"），无法重复复验 | `MyBatisCheck` 改为按"派单前在单量"的增量断言；`http-e2e.ps1` 改为按运行时刻生成手机号（与注册手机号分号段）；两个脚本现均可重复执行 |

---

## 五、环境适配说明

| 环境限制 | 处理方式 |
| --- | --- |
| 需要 MyBatis、MySQL 驱动与 slf4j 三个依赖 | 统一放在项目根 `lib/` 目录（4 个 jar）；`scripts/build.ps1` 自动加入编译/运行 classpath，部署时把 `lib/*.jar` 加入 `WEB-INF/lib` 即可 |
| 脚本平台为 Windows PowerShell 5.1（非 pwsh 7） | 所有 `.ps1` 保存为 UTF-8 with BOM；请求验证脚本采用 `WebSession` Cookie 方式鉴权 |
| 无管理员权限（无法启停 Windows 服务） | MySQL 服务已处于运行状态，直接使用 `mysql` 客户端执行脚本并验证；未修改系统服务配置 |
| 中文 Windows 控制台代码页 936（GBK） | 日志输出统一改为显式 UTF-8 流，重定向日志文件始终为 UTF-8 |
| `cmd.exe` 按"打开 .cmd 时的控制台代码页"解析批处理，UTF-8 中文注释会被误解码、甚至把注释行截断成非法命令（报 `'xxx' is not recognized` 且退出码为 1） | `build.cmd` 的注释改为纯 ASCII，中文提示一律由 `build.ps1` 以 UTF-8 输出；`chcp 65001` 仅用于让控制台正确显示这些中文 |

---

## 六、数据访问层：MyBatis Mapper 实现（本轮新增）

按交付要求，把数据访问层从"手写 SQL"改写为**真正的 MyBatis Mapper 配置**：

| 项目 | 内容 |
| --- | --- |
| MyBatis 版本 | 3.5.16（`lib/mybatis-3.5.16.jar`） |
| Mapper 接口 | 10 个，位于 `src/main/java/com/campus/repair/dao/mapper/` |
| Mapper XML | 10 个，位于 `src/main/resources/mapper/`，共 **99 条映射语句** |
| 运行资源位置 | 源文件在 `src/main/resources/`；部署镜像在 `webapp/WEB-INF/classes/`（`mybatis-config.xml`、`mapper/*.xml`、`config.properties`），由 `build.cmd` 每次编译自动同步。镜像随 Web 根必定进入 WAR，因此不依赖 IDEA 增量构建是否把资源根拷进模块输出目录 |
| 全局配置 | `src/main/resources/mybatis-config.xml`：POOLED 数据源（参数来自 `db.*`）、`JDBC` 事务管理器、类型别名、10 个 Mapper 注册 |
| Dao 实现 | 10 个 `MyBatisXxxDaoImpl`，只做「取 Mapper → 调方法 → 转换返回值」，**不含任何 SQL 字符串** |
| 会话与事务 | `MyBatisSessionFactory`：SqlSessionFactory 初始化、ThreadLocal 事务会话、`begin/commit/rollback`、连接自检 |
| 事务接入 | `TxTemplate` 按 `storage.mode` 分派：mybatis → SqlSession 事务；jdbc → Database 事务；memory → 空实现 |
| 三种实现可切换 | `storage.mode=mybatis`（默认）/ `jdbc`（手写 SQL 对照）/ `memory`（内存库演示） |
| 自检工具 | `dal/MyBatisCapabilityProbe`：报告各 DAO 实际实现类、已注册 Mapper、映射语句数 |

**关键 SQL 的设计要点**：`findByCondition/countByCondition` 用 `<where>+<if>` 动态拼条件 + `LIMIT/OFFSET` 分页；
`updateSelective` 用 `<set>` 只更新非空字段（避免审核时清空业务字段）；
`deductStock` 用条件更新保证并发下无负库存；聚合语句（`avgScore`、`countActiveByWorker` 等）支撑统计模块。

**该层的验证**：`MyBatisCheck` 39 项断言全部通过（见第一节），
其中包含"10 个 DAO 实现类均为 MyBatis 版本""10 个 Mapper 全部注册"的显式断言，
以及完整业务闭环在 MySQL 上的逐项核对。

---

## 七、运行方式收敛（仅保留 Tomcat 部署）

按交付要求，项目已移除一切"仅内置服务器使用"的内容，运行入口只保留 Servlet 组件：

| 已删除 | 原因 |
| --- | --- |
| `run.cmd` | 内置服务器启动脚本 |
| `src/main/java/.../boot/CampusRepairApplication.java` | JDK 内置 HTTP 服务器入口（Tomcat 不需要） |
| `src/main/java/.../web/StaticFileHandler.java` | 静态资源处理器（Tomcat 自行托管静态资源） |
| `webapp/init.jsp`、`webapp/forward.jsp` | 登录页改用静态 `webapp/index.html` |
| `pom.xml` | 不再打 WAR（IDEA Artifact 直接指向 `webapp`） |
| `build/`、`target/` | 旧编译产物、过时 class 与旧日志（已加入 `.gitignore`） |

保留内容（Tomcat 运行与验证必需）：

| 保留 | 用途 |
| --- | --- |
| `src/main/java`（101 个文件） | Controller / Service / DAO 接口 / **MyBatis Dao** / 手写 SQL Dao / 内存 Dao / 实体 / 工具 |
| `src/servlet-adapter/java`（2 个文件） | `ApiServlet` + `CampusContextListener`，Tomcat 运行入口 |
| `src/main/java/.../boot/DemoDataLoader.java` | 内存库演示数据，由 `CampusContextListener` 调用 |
| `src/main/resources/mybatis-config.xml` | MyBatis 全局配置（POOLED 数据源、JDBC 事务、10 个 Mapper 注册） |
| `src/main/resources/mapper/*.xml`（10 个文件，729 行） | **99 条映射语句**，设计书 2.2.6 对应的全部 SQL |
| `src/main/java/.../dao/mapper/*.java`（11 个文件） | Mapper 接口（10 个）+ `RepairOrderQuery` 分页查询入参 |
| `webapp/`（46 个文件） | Web 根：17 个页面 + 15 个脚本 + 样式 + `WEB-INF/web.xml` + `WEB-INF/classes` 下 12 个运行资源镜像（MyBatis 配置与 mapper） |
| `lib/`（4 个 jar） | MyBatis、MySQL 驱动、slf4j（需加入 `WEB-INF/lib`） |
| `db/schema.sql`、`db/seed.sql` | MySQL 建表与演示数据 |
| `scripts/build.ps1`、`scripts/http-e2e.ps1` | 编译校验/自测与端到端验证（可选） |
| `src/test/java`（7 个程序） | 业务自测、**MyBatis 定点验证**与其它定点验证（可选） |

清理后复验：`build.cmd -WithSelfTest` 编译 **115 个源文件**成功、业务自测 **99/99 通过**；
Tomcat 11 + MyBatis + MySQL 部署端到端 **97/97 通过**（`build/http-e2e.log`）；
源码内无对已删除类的残留引用。

---

## 八、目录与产物

| 路径 | 内容 |
| --- | --- |
| `README.md` | 交付说明（部署、结构、需求对照、数据库、接口、MyBatis 层说明、差异说明、排查表） |
| `DELIVERY-VERIFICATION.md` | 本报告 |
| `db/schema.sql`、`db/seed.sql` | 建表脚本（10 张表）与演示数据 |
| `src/main/java` | 113 个 Java 文件（boot/common/config/domain/dao/mapper/dao-mybatis/dao-jdbc/dao-memory/dal/service/util/web） |
| `src/main/resources/mapper/*.xml` | 10 个 MyBatis Mapper XML（99 条映射语句） |
| `src/main/resources/mybatis-config.xml` | MyBatis 全局配置 |
| `src/servlet-adapter/java` | Tomcat 运行入口（ApiServlet、CampusContextListener） |
| `src/test/java` | 7 个验证程序（含 MyBatisCheck） |
| `lib/` | MyBatis 3.5.16、MySQL 驱动、slf4j（部署时加入 `WEB-INF/lib`） |
| `webapp/` | Web 根：17 个页面 + 15 个脚本 + 样式 + `WEB-INF/web.xml` + `WEB-INF/classes` 下 12 个运行资源镜像（MyBatis 配置与 mapper） |
| `scripts/build.ps1`、`scripts/http-e2e.ps1` | 编译校验/自测脚本、端到端验证脚本 |
| `build/selftest.log` | 业务自测报告（99 项） |
| `build/mybatis-check.log` | MyBatis Mapper 层定点验证报告（39 项） |
| `build/http-e2e.log` | 端到端验证报告（97 项；内存库形态与 MyBatis 形态使用同一文件名，每次运行覆盖） |
