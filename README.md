# Monef · 企业智能财务分析与资金管理平台

按 Monef 组件设计重建的全栈项目，包含可操作的 Vue 3 界面、Java 17 后端、数据库迁移、Docker Compose、测试与启动包。

界面参考：[One Collective Design / One Week Wonders · Monef](https://dribbble.com/shots/27389006-UI-elements-for-Monef-Corporate-Finance-Dashboard)。这是独立实现，不是原作品源码；未打包参考设计中的摄影、手机渲染或素材。

## 立即运行

已构建的 `release/monef-api-1.0.0.jar` 内含正式 API 模式的前端。安装 Java 17 后，在项目目录运行：

```powershell
.\scripts\start-demo.ps1
```

macOS / Linux：

```bash
sh scripts/start-demo.sh
```

浏览器访问 **http://127.0.0.1:8080**。

- 邮箱：`demo@monef.local`
- 密码：`MonefDemo!2026`
- 演示模式：H2 文件数据库、240 笔自动生成的示例流水、3 个币种账户、固定演示汇率。
- 数据保存在 `backend/data/`，重启不丢失；Ctrl+C 停止服务。
- 演示账户与固定汇率仅用于本地体验。此模式与正式 MySQL / Redis 模式有明确区分。

若本地 PowerShell 策略禁止运行脚本，直接执行：

```powershell
cd backend
java -jar ../release/monef-api-1.0.0.jar --spring.profiles.active=demo
```

在线交互演示是独立的浏览器示例模式，变更仅存储在访问者本机，不能代替后端部署。正常构建默认 `VITE_PREVIEW_MODE=false`，API 失败时不会悄悄切换为示例数据。

## 已实现功能

1. **财务数据管理**：创建账户、启用 / 停用账户、期初余额、收入 / 支出入账、分页、关键词 / 账户 / 状态 / 类型 / 月份筛选、交易详情和 CSV 导出。原流水不可篡改；纠错采用作废后重新入账，保留审计记录。
2. **财务统计**：数据库按月聚合、12 个月补零序列、收入 / 支出 / 净现金流 / 收支结余率、环比、支出分类，前端可切换 6 / 12 个月和净现金流视图。
3. **多币种**：USD、EUR、GBP、CNY、JPY、SGD、HKD、AUD、CAD、CHF；Frankfurter 日汇率查询、历史日期查询、Redis 缓存、MySQL 历史记录、金额换算。页面显示汇率日期、来源与缓存状态。
4. **Spring AI 助手**：模型只能访问当前企业、选定月份的月度摘要和最多 20 条流水；没有 SQL、写库、转账或跨企业工具。结果显示分析月份与数据来源。
5. **基础工程能力**：Spring Security 会话登录、BCrypt、CSRF、ADMIN / ANALYST 权限、租户隔离、交易幂等、防并发重复入账、操作审计、Redis 登录 / AI 限流、参数验证、统一错误处理、Flyway、健康检查。

## 技术版本

- Java 17；Spring Boot 3.5.16；Spring AI 1.1.8。
- Spring Data JPA、Spring Security、Spring Session Redis、Flyway。
- MySQL 8.4、Redis 7.4。
- Vue 3.5.22、TypeScript、Vite 7.1.7、Lucide、decimal.js。
- Maven 3.9+；Node.js 22+；pnpm 11.25.0。

版本已固定。Spring AI 1.1.x 对应 Spring Boot 3.5.x，满足 Java 17 要求。

## MySQL + Redis 完整部署

需要已正常运行的 Docker Engine / Docker Desktop。

```powershell
Copy-Item .env.example .env
# 编辑 .env，设置独立的数据库、Redis、管理员密码和管理员邮箱
docker compose --env-file .env up --build -d
docker compose ps
```

访问 **http://127.0.0.1:8088**，使用 `.env` 中的管理员账号登录。

默认不写入示例财务数据，也不启用 AI。只有新建、空数据库才会按管理员环境变量创建初始账号；更改环境变量不会重置现有账号密码。后续应通过受控运维流程管理用户，当前版本未提供用户邀请 / 密码重置界面。

需要体验容器内示例数据时，在一次性演示环境设置 `SEED_DEMO=true` 和 `FX_DEMO=true`。正式环境保持为 `false`。

数据库与 Redis 不映射宿主机端口；前端默认只监听 `127.0.0.1:8088`。对外提供服务时，在受控网络配置 HTTPS 反向代理并设置 `COOKIE_SECURE=true`。Compose 数据卷持久化数据库与 Redis；普通 `docker compose down` 不删除数据卷。

## 启用 Spring AI

在服务器 `.env` 设置：

```dotenv
AI_ENABLED=true
AI_PROVIDER=openai
OPENAI_API_KEY=your-server-side-api-key
OPENAI_BASE_URL=https://api.openai.com
AI_MODEL=gpt-4o-mini
```

然后重新创建后端容器：

```bash
docker compose up -d --force-recreate backend
```

也可使用兼容 OpenAI 工具调用协议的模型服务。API Key 只在后端环境变量中读取，不进入前端构建和浏览器存储。启用后，当前月份摘要与被工具选中的流水会发送给所配置的模型供应商；应使用符合企业数据治理要求的服务。

未启用模型时返回 `DATA_SUMMARY`，明确标注为固定数据摘要；不会声称执行了模型推理。启用后供应商出错返回 502 / 503，不伪造分析成功。AI 内部工具循环最多 4 轮模型请求、6 次查询（含初始摘要），每次最多 20 条交易；每用户每分钟最多 10 次分析。

## 本地开发与构建

后端：

```bash
cd backend
mvn spring-boot:run
```

前端另开终端：

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

访问 `http://127.0.0.1:5173`；Vite 将 `/api` 代理到 8080。本地开发登录页预填演示账号，正式构建不预填。

重新构建包含前端的启动包：

```powershell
.\scripts\build-release.ps1
```

或 `sh scripts/build-release.sh`。构建脚本先跑前端测试与类型检查，再打包前端并运行后端验证；生成的 JAR 放入 `release/`。

只构建在线交互演示：在前端使用 `VITE_PREVIEW_MODE=true` 构建；项目根目录的 `scripts/build-preview.mjs` 用于 Sites 静态打包。这不会部署 Java 服务。

## 金额与统计规则

- 金额用 Java `BigDecimal` / SQL `DECIMAL`；JSON 中金额与汇率输出为字符串，前端用 decimal.js 展示，避免大金额精度丢失。
- 原币金额最多两位小数，入账时按 HALF_UP 舍入 USD 折算结果到两位小数。图表绘制使用数值坐标，账本金额不依赖浮点计算。
- 余额 = 期初余额 + 有效收入 − 有效支出；允许账面负余额，不代表银行可用余额。
- Dashboard 的统一币种为 USD；跨币种交易按入账汇率快照汇总，不使用今天汇率重算历史。
- 净现金流 = 收入 − 支出；这是收付口径，不等于权责发生制净利润。
- 收入为零时结余率为空；上月为零时环比为空。环比分母使用上月绝对值。
- 当前月份尚未结束时，环比比较本月至今与完整上月。日期和服务器统计按 UTC。
- FX 历史记录是实际查询得到的汇率观察值，不是自动填充的完整日线。节假日由供应商返回最近有效报价日；页面保留这个真实日期。
- 参考汇率不是银行成交价；换算不执行资金兑换或转账。

## 验证结果与范围

见 [验收记录](docs/VERIFICATION.md)、[接口文档](docs/API.md) 和 [架构说明](docs/ARCHITECTURE.md)。

当前版本实现本次列出的业务闭环，并具备权限、隔离、精度与审计基础。未覆盖银行直连、复式记账总账、预算审批、发票税务、SSO/MFA、备份恢复演练、压力测试与生产监控告警，不应把这些未实现能力视为已交付。

## 参考

- [Spring AI 1.1 工具调用](https://docs.spring.io/spring-ai/reference/1.1/api/tools.html)
- [Spring Boot 3.5 / Java 17 兼容性](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Frankfurter 汇率接口](https://frankfurter.dev/)
