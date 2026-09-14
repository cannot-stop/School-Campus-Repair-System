# 校园报修系统 · 交付验证报告

- **交付日期**：2026-09-14
- **依据文档**：《校园报修系统需求分析和系统详细设计书》（781 段、25 张表格、14 张图）
- **交付目录**：`D:\DeepSeek Harness\campus-repair-system`
- **运行环境**：JDK 25（`D:\Java`）+ MySQL 8.0.25（本地 `MySQL80` 服务）+ Windows PowerShell 5.1

---

## 一、验证结论

| 验证项 | 方式 | 结果 |
| --- | --- | --- |
| 源码编译 | `build.cmd -Clean -WithSelfTest`（javac，91 个源文件） | ✅ 编译通过（退出码 0） |
| 业务规则自测 | `build/selftest.log`（18 组用例） | ✅ **99 项断言全部通过** |
| 端到端接口验证（内置服务器·内存库） | `build/http-e2e.log`（16 组用例） | ✅ **97 项断言全部通过** |
| 端到端接口验证（内置服务器·MySQL） | `build/http-e2e-jdbc.log` | ✅ **97 项断言全部通过** |
| 端到端接口验证（Tomcat 11·非根上下文 `/campus_repair_system_war`） | `build/http-e2e-tomcat.log` | ✅ **97 项断言全部通过** |
| 前端脚本语法 | 全部 15 个 `.js` 经 `node --check` 校验 | ✅ 15/15 通过 |
| 数据库脚本 | `db/schema.sql` + `db/seed.sql` 在 MySQL 8.0.25 实际执行 | ✅ 建表 10 张、外键与唯一约束生效、演示数据导入成功 |
| 中文数据往返 | 表数据查询比对（楼栋、类别、技能标签、描述） | ✅ utf8mb4 全程无乱码 |
| 前端页面可访问性 | 15 个功能页面 + 登录页 HTTP 200（根上下文与 `/campus_repair_system_war` 均验证） | ✅ 全部可访问 |
| 容器部署启动自检 | Tomcat `logs/localhost.*.log` | ✅ 输出「存储模式=memory，可用账户数=8」 |
| 一键脚本 | `build.cmd`、`run.cmd` 实际执行 | ✅ 构建与服务启动均成功 |

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
| 7 | 前端接口调用写死根路径 `fetch('/api/...')` | 部署到非根上下文（如 `/campus_repair_system_war`）时请求全部打空 | 页面注入 `<base>` 与 `window.__CRS_BASE__`；`common.js` 新增 `App.url()` 统一拼接上下文；静态资源改相对路径；新增容器入口 `webapp/init.jsp` |
| 8 | 前端用 `document.cookie` 写 `CRS_TOKEN; path=/`，与容器下发的 `path=/上下文` 形成两个同名 Cookie | 登录成功但后续接口返回 1003「登录状态已失效」 | Cookie 按上下文路径写入并清理旧 Cookie；服务端与内置服务器均从候选令牌中挑选能对应有效会话的那个（已用"双 Cookie"场景实测） |
| 9 | `ApiServlet` 先 `getParameter()` 再读 `getInputStream()`（Servlet 规范下二选一） | Tomcat 下 JSON 请求体读取为参数失败 | 拆分为 JSON 体自行解析 / 表单参数交由容器解析两条互斥路径；新增 `Dispatcher.dispatchWithParams()` 供适配层复用 |
| 10 | `web.xml` 中 `db.password` 为空导致 jdbc 模式连接失败，但对外只表现为"用户名或密码错误" | 部署后登录失败原因不可见 | 支持环境变量覆盖（`CAMPUS_DB_PASSWORD`）；`CampusContextListener` 增加启动自检，明确打印存储模式、数据库连通性与可用账户数 |

---

## 五、环境适配说明

| 环境限制 | 处理方式 |
| --- | --- |
| 无 Maven、无外网（Maven 中央仓库不可达） | 提供 `scripts/build.ps1`：javac + 资源复制，零第三方依赖构建；`pom.xml` 供有网络环境打包 WAR |
| 无 Tomcat | 主形态使用 JDK 内置 HTTP 服务器运行；同时提供 `ApiServlet`/`CampusContextListener`/`web.xml`/JSP 视图层供容器部署 |
| 脚本平台为 Windows PowerShell 5.1（非 pwsh 7） | 所有 `.ps1` 保存为 UTF-8 with BOM；请求验证脚本采用 `WebSession` Cookie 方式鉴权 |
| 无管理员权限（无法启停 Windows 服务） | MySQL 服务已处于运行状态，直接使用 `mysql` 客户端执行脚本并验证；未修改系统服务配置 |

---

## 六、目录与产物

| 路径 | 内容 |
| --- | --- |
| `README.md` | 交付说明（启动、结构、需求对照、数据库、接口、差异说明、扩展点） |
| `db/schema.sql`、`db/seed.sql` | 建表脚本（10 张表）与演示数据 |
| `src/main/java` | 91 个 Java 文件（boot/common/config/domain/dao/service/util/web） |
| `src/servlet-adapter/java` | Tomcat 适配层（2 个类） |
| `src/test/java` | 6 个验证程序 |
| `webapp/` | 前端 33 个文件（16 个页面 + 15 个脚本 + 样式 + web.xml + JSP） |
| `scripts/build.ps1`、`scripts/http-e2e.ps1` | 构建脚本、端到端验证脚本 |
| `build/selftest.log` | 业务自测报告（99 项） |
| `build/http-e2e.log`、`build/http-e2e-jdbc.log` | 端到端验证报告（97 项 × 2 模式） |
| `build/category-trace2.log`、`build/jdbc-flow.log` | 定点验证报告 |
