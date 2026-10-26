# 实现说明

## 分层

浏览器 Vue 3 通过同源 `/api` 请求 Spring Boot。开发时由 Vite 代理；容器部署时由 Nginx 代理；独立 JAR 内置同一套前端静态资源。控制器完成认证上下文读取与参数验证，服务层实现账本、统计、汇率和 AI 边界，Spring Data JPA 访问数据库。

正常业务页面不会使用浏览器示例账本。只有显式编译 `VITE_PREVIEW_MODE=true` 时，才加载独立的 `demo.ts` 交互演示模块。在线 Sites 仅托管此演示，Java / MySQL / Redis 在用户环境部署。

## 主要文件

- `backend/src/main/java/com/monef/FinanceService.java`：账户与不可变流水、幂等、作废、余额。
- `DashboardService.java`：数据库聚合、完整月序列、KPI 与支出分类。
- `FxService.java`：Frankfurter 请求、历史持久化、Redis / 演示内存缓存。
- `FinanceTools.java`：不带可控租户参数的两项只读能力。
- `AiService.java`：受限模型循环、确定性摘要、审计和限流。
- `SecurityConfig.java` / `TenantAccess.java`：会话、CSRF、权限、服务端企业身份。
- `RateLimiter.java`：Redis Lua 原子计数与过期；演示使用本地窗口。
- `frontend/src/App.vue`：六个工作区视图、表单、筛选与导出。
- `frontend/src/components/`：趋势、换汇、助手组件。

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
