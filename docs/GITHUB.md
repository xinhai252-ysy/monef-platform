# GitHub 上传与本地部署

## 上传内容

上传项目根目录中的前后端源码、Docker Compose、构建脚本、README、docs 和 GitHub Actions 配置。

`.gitignore` 已排除真实环境配置、数据库文件、依赖目录、日志、JAR 和生成的前端文件。`.env.example` 只有配置示例，可以上传。演示账号是公开的本地体验账号，不用于正式环境。

不要上传 `.env`、`backend/data/`，也不要将真实 API Key 或正式密码写入源码。上传前执行：

```powershell
git status --short
git diff --check
git check-ignore .env backend/data/monef.mv.db
```

本仓库提供 CI，推送或创建 PR 后会分别运行前端测试与构建、Java 17 后端测试与打包。CI 使用内存 H2，不能代替 MySQL / Redis 实际联调。

当前没有配置 Git 远程仓库。创建 GitHub 仓库后，再设置用户选定的地址；提交、推送前先确认仓库和公开范围。GitHub 只托管源码，不会自动运行 Java、MySQL 和 Redis。

## Windows 重启后继续 Docker 部署

Windows 的 Virtual Machine Platform 和 WSL 已于 2026-10-27 启用并完成重启。Docker Desktop 引擎已正常运行，四个 Compose 服务已成功构建和启动。

项目本机 `.env` 已创建，包含独立随机密码，不在 Git 跟踪范围。管理员邮箱是 `admin@monef.local`；密码查看本机 `.env` 中的 `MONEF_ADMIN_PASSWORD`，不要放入截图、提交或聊天记录。

在项目根目录执行：

```powershell
docker info
docker compose --env-file .env config --quiet
docker compose --env-file .env up --build -d
docker compose ps
```

打开 <http://127.0.0.1:8088>，使用 `.env` 中的管理员账号登录。

MySQL 在首次初始化时创建 `monef` 数据库，后端通过 Flyway 自动建表。Redis 保存会话。默认不生成示例流水，原 H2 数据不自动迁移，仍保留在原目录。

数据库仅在 Docker 内部网络提供访问，当前没有映射宿主机数据库端口。如需 IDEA / 数据库客户端连接，之后可增加仅绑定本机的端口映射。

普通 `docker compose down` 保留数据卷；不要使用 `down -v`，它会删除数据库和 Redis 的数据卷。首次建库后，修改 `.env` 不会自动修改已经存在的 MySQL 用户密码或应用管理员密码。

## 登录功能范围

2026-10-27 本地验证：前端 10 项测试通过、TypeScript 与生产构建通过；后端 Maven verify 通过，15 项测试无失败。通过运行中的 Vite 代理访问 H2 后端，验证错误密码返回 401、正确密码登录成功、Cookie 会话恢复成功、退出返回 204、退出后受保护接口返回 401。Docker Compose 配置解析通过；MySQL / Redis 容器联调已完成：正式管理员登录、错误密码拒绝、退出后受保护接口拒绝访问均通过。重启 MySQL、Redis、后端后，Redis 会话及 MySQL 账户数据均保留。尚未提交或推送 GitHub，未配置远程地址。

已有真实服务端邮箱密码登录、BCrypt 密码哈希、Cookie Session、CSRF、退出登录和企业数据隔离。浏览器刷新会向后端检查会话。前端已补充登录错误、连接失败、会话失效清理和重新取得 CSRF 令牌的处理。

静态预览使用模拟身份，不用于验证认证。正式构建通过后端认证；已支持注册独立工作区；尚未实现邀请用户、忘记密码或邮件验证。

Docker 首次启动后的验收：错误密码被拒绝；正确账号登录；刷新后保持会话；退出后业务接口返回未登录；创建账户后重启容器，账户仍存在。以上场景已于 2026-10-27 验证通过。验证留下一个零余额、已停用的「部署验证账户（已停用）」，不影响资金余额，审计记录保留。
