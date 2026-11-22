# 实现说明

## 分层

浏览器 Vue 3 通过同源 `/api` 请求 Spring Boot。开发时由 Vite 代理；容器部署时由 Nginx 代理；独立 JAR 内置同一套前端静态资源。控制器完成认证上下文读取与参数验证，服务层实现账本、统计、汇率和 AI 边界，Spring Data JPA 访问数据库。

正常业务页面不会使用浏览器示例账本。只有显式编译 `VITE_PREVIEW_MODE=true` 时，才加载独立的 `demo.ts` 交互演示模块。在线 Sites 仅托管此演示，Java / MySQL / Redis 在用户环境部署。

## 后端目录与依赖方向

后端按 `controller → service → repository → entity` 分层。Repository 使用 Spring Data JPA，承担 DAO 的职责，不另外增加一层只转发方法的 DAO 实现。

- `backend/src/main/java/com/monef/controller/`：认证、账户、交易、总览、汇率、AI、审计七个控制器。负责 HTTP 参数、校验、访问权限和调用服务。
- `service/FinanceService.java`：账户与流水业务、幂等、作废、余额和事务。
- `service/DashboardService.java`：数据库聚合、完整月序列、KPI 与支出分类。
- `service/FxService.java`：Frankfurter 请求、历史持久化、Redis / 演示内存缓存。
- `service/AuditService.java`：当前企业的审计分页和响应转换，控制器不直接访问数据库。
- `service/FinanceTools.java`、`service/AiService.java`：受限只读工具、模型循环、数据摘要及审计。
- `repository/`：每个 JPA Repository 独立文件，保留原有租户条件、行锁和聚合查询。
- `entity/`：五个实体独立文件；字段私有，通过访问方法读取和修改；主键与乐观锁版本不提供外部设置方法。
- `dto/`：独立的请求和响应记录，避免 HTTP 接口直接暴露数据库实体。
- `config/`：Spring Security、JSON 金额序列化和 HTTP 客户端配置。
- `security/`：服务端企业身份读取、Redis 原子限流和演示限流。
- `exception/`：业务异常与统一 HTTP 错误响应。
- `bootstrap/`：初始化管理员和可选演示数据。

控制器不执行账本 SQL 或仓储查询，业务事务保留在服务层；认证控制器处理 HTTP 会话和安全上下文。此次结构调整没有改数据库迁移、表名、JSON 字段或 API 地址。

## 前端目录与状态归属

- `frontend/src/App.vue`：工作区布局、导航出口及页面组合。
- `views/`：登录、总览、交易、账户和审计页面。
- `components/layout/`：侧栏与工作区弹窗。
- `components/`：趋势图、汇率换算与 AI 面板。
- `composables/useSession.ts`：登录、会话恢复、退出、过期事件及卸载时的监听清理。
- `composables/useFinanceWorkspace.ts`：当前工作区的数据加载、筛选、业务操作和表单状态。
- `composables/useDashboardMetrics.ts`：从服务端统计数据计算展示指标和迷你图坐标。
- `composables/workspaceContext.ts`：提供有类型的工作区上下文，页面共享当前工作区的响应式状态，不自行创建第二套账本状态。
- `api.ts`：统一网络请求、Cookie、CSRF、超时和错误处理；`demo.ts` 仅用于显式开启的浏览器演示。

页面负责展示与事件，组合式函数负责可复用状态和行为。工作区由根组件创建，其生命周期内只存在一个实例。

## 代码格式

项目根目录固定 Prettier 和 Java 插件版本，使用 `.prettierrc.json`。Java 使用四空格缩进，Vue / TypeScript 使用两空格；不压缩源代码。运行 `pnpm format` 统一格式，`pnpm format:check` 只检查。构建产物、数据库和依赖目录不参与格式化。

## 数据库

Flyway `V1__finance_schema.sql` 创建五张表：

1. `app_user`：登录邮箱、BCrypt 密码、角色与企业编号。
2. `finance_account`：账户币种、期初余额、启用状态、乐观版本。
3. `ledger_entry`：收入 / 支出、原币金额、USD 金额、汇率快照、业务日期、状态和幂等编号。以 `(tenant_id, client_ref)` 唯一，账户外键约束。
4. `fx_rate`：币种对、报价日、精确汇率、来源与获取时间，以币种对 / 日期 / 来源唯一。
5. `audit_event`：操作人、行为、对象、摘要与时间。

账本索引覆盖企业 + 业务日期 + 状态，以及企业 + 账户 + 状态。SQL 聚合在数据库中执行，不把全量交易加载到前端计算。

## 一致性与并发

创建交易时先按企业范围锁定账户，查同一请求编号，验证有效账户与金额，再固定入账汇率并插入账本 / 审计；整段服务操作在事务中完成。唯一约束兜底不同账户间同请求编号冲突。

账户余额通过有效流水求和，不维护易漂移的冗余余额。作废采用行锁，重复作废不重复写审计。当前实现没有复式分录、科目余额或银行清算语义。

## 权限

ADMIN 可以创建 / 停用账户、记账 / 作废并查看审计；ANALYST 可以查看和分析。租户由数据库用户记录决定，查询和写入均带企业约束。用户名称 / 描述采用 Vue 文本插值，模型响应不通过 `v-html` 渲染。

所有非 GET API 启用 CSRF。Cookie 为 HttpOnly、SameSite=Strict；生产 HTTPS 下启用 Secure。登录变更 Session ID。Redis 生产会话支持多实例共享；Redis 限流不可用时登录和 AI 请求返回 503。

## AI 证据与边界

每次分析先读取当前企业月度摘要，将其作为证据输入模型。模型可再次查询摘要或请求指定类型的少量流水。工具对象封闭捕获租户与月份，防止模型从问题文本生成任意租户或时间范围。

业务描述被视为不可信文本；系统提示要求仅使用查询结果回答。提示本身不能保证模型不会产生错误，因此回答依旧需要人审。技术边界由只读工具、最大行数、查询次数、模型轮次和请求限流保证，不能调用写入服务。

## 后续生产工作

应在实际部署环境进行 MySQL / Redis 集成验收、模型供应商连通性测试、数据备份恢复与容量测试。按组织要求接入 SSO/MFA、用户生命周期、密钥管理、集中日志、监控告警与审批流程。项目并未实现这些组织级能力。
