# HTTP API

基础路径 `/api`，JSON 编码 UTF-8，金额字段使用十进制字符串。日期 `YYYY-MM-DD`，月份 `YYYY-MM`，时间为 ISO-8601 UTC。

## 登录与 CSRF

1. `GET /api/auth/csrf`，保存响应 Cookie，以及响应中的 `headerName` 与 `token`。
2. `POST /api/auth/login`，带上步骤 1 的 Cookie 和 CSRF Header。
3. 登录后使用同一 Cookie Jar；每次非 GET 请求均带 CSRF Header。前端自动重新取得登录后令牌。

登录请求：

```json
{"email":"demo@monef.local","password":"MonefDemo!2026"}
```

返回 `email`、`name`、`role`、`tenantId`、`demo`。`GET /auth/me` 查询会话，`POST /auth/logout` 注销。

企业身份只能来自已认证用户；API 不接受客户端覆盖 `tenantId`。会话超时 30 分钟。生产会话存储在 Redis；演示环境使用进程内 HTTP Session。

## 账户

- `GET /accounts`：当前企业账户，包含原币余额。
- `POST /accounts`：管理员新建。
- `PATCH /accounts/{id}/status`：管理员提交 `{"active":false}` 停用，或 `true` 启用。

新建示例：

```json
{
  "name":"美元运营账户",
  "institution":"Mercury ···· 4829",
  "currency":"USD",
  "type":"BANK",
  "openingBalance":"10000.00"
}
```

`type` 为 `BANK`、`WALLET`、`CASH`。不直接修改期初余额或已入账历史。

## 流水

`GET /transactions` 支持：

- `page` 从 0 开始，默认 0；`size` 1–100，默认 10。
- `q` 描述关键词（按字面匹配 `%` / `_`），最多 100 字符。
- `kind`：`INCOME` / `EXPENSE`；`status`：`POSTED` / `VOID`。
- `accountId`；`start` 与 `end`（均包含边界）。

响应：`content`、`page`、`size`、`totalElements`、`totalPages`。稳定排序：记账日、创建时间、ID，均降序。

`POST /transactions` 管理员入账：

```json
{
  "accountId":"returned-account-id",
  "kind":"EXPENSE",
  "amount":"128.50",
  "description":"软件订阅",
  "category":"软件订阅",
  "bookedOn":"2026-10-26",
  "clientRef":"a-unique-request-id-001"
}
```

金额币种由账户决定，客户端不能覆盖。`clientRef` 为 8–64 位字母、数字、下划线、连字符。同企业同编号同内容重试返回原交易，不重复入账；同编号不同内容返回 409。并发写入使用账户悲观锁与数据库唯一约束。

交易返回包含 `baseAmount`（USD）、`fxRate`、`fxDate`、`fxSource`，供核对历史换算依据。

`POST /transactions/{id}/void` 将状态设为 `VOID`，重复作废是幂等操作。作废排除统计和余额，原始行仍存在。

`GET /categories` 和 `GET /currencies` 获取允许的分类 / 币种。收入与支出分类必须匹配。

## 仪表盘

`GET /dashboard?month=2026-10` 返回：

- `income`、`expense`、`netCashFlow`：`value` 与 `change`（环比百分比，可为 null）。
- `surplusMargin`（百分比，可为 null）、`transactionCount`。
- `series`：截至所选月的 12 个月，每个月有 `month`、`income`、`expense`，空月补零。
- `categories`：支出分类、USD 金额与笔数，按金额降序。
- `baseCurrency=USD`、`generatedAt`。

仅聚合已入账流水。界面切换 6 个月时只截取返回序列，不改变服务端统计口径。

## 汇率

- `GET /fx/rate?base=USD&quote=EUR`：最新日参考汇率。
- 添加 `date=2026-10-23`：指定历史日期。
- `GET /fx/convert?base=USD&quote=EUR&amount=1200.00`：金额换算，支持同样的可选日期。
- `GET /fx/history?base=USD&quote=EUR&page=0`：每页 20 条已保存的观察记录。

汇率返回 `base`、`quote`、`rate`、`date`、`source`、`cached`、`fetchedAt`。`date` 为供应商实际报价日，可能早于请求日期。来源 `FRANKFURTER`、`DEMO` 或 `IDENTITY`（同币种）。

实时缓存 TTL 一小时；历史记录持久化。Redis 不可用时汇率可尝试数据库 / 供应商，供应商失败返回 503，不虚构汇率或隐式返回陈旧数据。

## AI

`POST /ai/chat`：

```json
{"message":"分析本月主要支出与现金流变化","month":"2026-10"}
```

问题最大 1,000 字符。响应 `answer`、`mode`、`month`、`currency`、`transactionCount`、`tools`、`generatedAt`。

- `SPRING_AI`：已配置模型生成的回答。
- `DATA_SUMMARY`：未配置模型的确定性数据摘要，明确标识限制。

每次请求创建独立工具对象。工具中企业与月份由服务端封闭捕获，模型无参数可修改；月度摘要不暴露完整年度交易数据，流水查询最多 20 条，最多 6 次查询。内部工具执行关闭，应用显式驱动最多 4 轮模型调用。没有任意 SQL、HTTP、文件操作或写入工具。

## 审计与健康

- `GET /audit?page=0`：管理员查询当前企业审计，每页 20 条。
- `GET /actuator/health`：存活 / 依赖健康，未认证可读取状态，不返回详细信息。

审计记录账户状态、交易入账 / 作废和 AI 分析元数据；不保存原始聊天问题或模型 API Key。

## 错误

错误格式：`{"message":"可读错误信息","timestamp":"ISO-8601"}`。安全过滤器的 401 / 403 只包含 `message`。

- 400：参数 / 金额 / 日期 / 分类无效。
- 401：未登录、会话失效或凭据错误。
- 403：权限不足或 CSRF 令牌无效。
- 404：当前企业范围内对象不存在。
- 409：幂等编号冲突、账户已停用或数据库约束冲突。
- 429：达到登录 / AI 调用限额。
- 502 / 503：模型、汇率、限流依赖不可用。

事务失败时账本写入回滚。前端保留同一表单请求编号以便网络故障后安全重试。
