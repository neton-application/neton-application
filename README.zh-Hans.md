# Neton Application

基于 [Neton](../neton) 框架构建的通用后台服务，用 Kotlin/Native 编译为原生二进制：启动快、内存低、单文件部署，运行时不需要 JVM。

本仓库是**通用发行版（base distribution）**，只负责装配模块（启动入口、配置、CI），业务代码都在独立的模块仓库里。

> 规范（以此为准）在 `neton-docs`：
> [SPEC.md](../neton-docs/docs/zh-hans/spec/application/SPEC.md) ·
> [身份域与会话边界](../neton-docs/docs/zh-hans/spec/application/identity-realm.md) ·
> [认证与短信防滥用](../neton-docs/docs/zh-hans/spec/application/auth-abuse-protection-spec.md)
>
> 开发硬规范：[docs/ENGINEERING_RULES.md](./docs/ENGINEERING_RULES.md) ·
> 模块模板规范：[docs/module-template-spec.md](./docs/module-template-spec.md)

## 技术栈

| 项目 | 选型 |
|------|------|
| 语言 | Kotlin 2.4.20 (Multiplatform) |
| 框架 | Neton 1.0.0-beta1 |
| 编译 | Kotlin/Native → 原生二进制（`application.kexe`） |
| 数据库 | **仅 PostgreSQL** |
| 缓存 / 限流 / 验证码 | Redis |
| 认证 | 按身份域隔离的 JWT（HS256）+ 服务端会话 |
| 构建 | Gradle 8.14 + KSP |
| 日志 | 结构化 JSON，多 Sink |

运行指标：冷启动约 150ms（debug）/ 约 3ms（release），内存约 20MB。

## 仓库结构

多仓库 + Gradle Composite Build，每个模块一个仓库，本发行版只负责装配。

| 仓库 | 内容 |
|------|------|
| `neton` | 框架（core、http、database、security 等） |
| `neton-application` | 本仓库：启动入口、配置、CI，不含业务模块代码 |
| `neton-application-module-system` | 管理员身份、认证与会话、RBAC、MFA、验证码、审计、系统设置、健康探针 |
| `neton-application-module-infra` | 文件、定时任务、API 日志、Redis 监控、领域事件 |
| `neton-application-module-account` | 终端用户账号：注册登录、资料、手机 / 邮箱 / 密码、账号会话 |
| `neton-application-module-member` | 会员成长：等级、积分、签到、邀请、收货地址、分组标签（依赖 account） |
| `neton-application-module-payment` | 支付：渠道、订单、退款、钱包、转账、提现、银行卡 |
| `neton-application-module-platform` | 开放平台：客户端、API 目录、计费、调用日志与统计 |
| `neton-application-module-cs` | 客服（经 `privchat-service-client` 接入 PrivChat） |
| `neton-application-front`（及 `neton-application-front-*`） | 管理端前端（React 19 + Next.js） |
| `neton-application-client`（及 `neton-application-client-*`） | 用户中台 / C 端控制台（React 19 + Next.js） |

工作区布局（`settings.gradle.kts` 中的相对路径按此布局编写）：

```
projects/
├── Neton/
│   ├── neton/                               # 框架
│   ├── geolite4k/
│   ├── neton-application/                   # 本仓库
│   ├── neton-application-module-system/
│   ├── neton-application-module-infra/
│   ├── neton-application-module-account/
│   ├── neton-application-module-member/
│   ├── neton-application-module-payment/
│   ├── neton-application-module-platform/
│   ├── neton-application-module-cs/
│   ├── neton-application-front/
│   └── neton-application-client/
└── privchat/
    └── privchat-service-client/             # module-cs 使用
```

干净环境需要检出的仓库及锁定的提交号见 `scripts/ci/dependencies.lock`；`scripts/ci/checkout-dependencies.sh <工作区根目录>` 按它检出（CI 即用此脚本）。

本仓库：

```
neton-application/
├── application/
│   ├── src/commonMain/kotlin/Main.kt   # 启动入口（装配模块与基础设施）
│   └── config/                         # 运行时配置（TOML）
├── scripts/ci/                         # 依赖锁定与检出脚本
├── tests/security/                     # 身份域与会话 E2E
├── build.gradle.kts / settings.gradle.kts
```

## 架构设计

### 分层架构

```
Controller  ──  HTTP 路由、参数绑定、@Permission
     │
   Logic    ──  业务逻辑、事务编排
     │
   Table    ──  数据访问（KSP 生成 CRUD）
     │
   DB       ──  PostgreSQL
```

### 路由组

由 `application/config/routing.conf` 定义：

| 路由组 | 挂载路径 | 用途 | requireAuth | 接受的令牌 |
|--------|----------|------|-------------|-----------|
| `admin` | `/admin` | 管理后台 API | true | 仅管理员域 |
| `app` | `/app` | C 端 / 移动端 API | true | 仅账号域 |
| `manage` | `/manage` | 经营者管理中心 | true | 仅账号域 |
| `open` | `/platform` | 开放平台 API | 按需 | 不接受本应用令牌（自行装配认证器） |

### 模块依赖关系

```
application
├── module-system        ← 核心
├── module-infra         ← system
├── module-account       ← system、infra
├── module-member        ← account
├── module-payment       ← system、account
├── module-platform      ← system
└── module-cs            ← account、payment
```

模块列表只有一个来源：`application/build.gradle.kts` 中的 `ksp { arg("neton.modules", ...) }`，`migrate` 命令、启动预检与服务启动共用它。

## 安全

### JWT 密钥

签名密钥**只能**通过环境变量 `NETON_SECURITY__JWT__SECRETKEY` 提供，长度至少 32 个字符。缺失、仍是占位值（`REPLACE_ME…`）或过短时拒绝启动——任何环境都一样，包括 `dev`。

```bash
export NETON_SECURITY__JWT__SECRETKEY="$(openssl rand -base64 48)"
```

### 身份域与会话

- 管理员令牌与账号令牌分属不同身份域。`admin` 组只认管理员令牌，`app`、`manage` 只认账号令牌，用错域的令牌返回 401。
- refresh 令牌不能当 access 令牌访问接口；refresh 令牌只能用一次（管理员与内置账号域），超出 30 秒并发窗口的重复使用会作废整个会话。
- `POST …/auth/logout` 只吊销当前会话；`POST /admin/system/auth/logout-all` 与 `POST /app/auth/logout-all` 吊销该用户的全部会话。
- 改密、撤销权限立即生效，不必等令牌过期。
- 管理端前端使用 HttpOnly 会话 Cookie；凭 Cookie 的写请求必须带 `X-Neton-Csrf: 1`。
- **升级到本版本后，此前签发的令牌全部失效**（不带 `realm` 声明），用户需要重新登录。

详见 [identity-realm.md](../neton-docs/docs/zh-hans/spec/application/identity-realm.md)。

### 验证码（短信 / 邮件）

在 `application.conf` 的 `[security.verification]` 下配置：

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `simulate` | `false` | 只存不发，**仅限本地开发** |
| `resend_cooldown_seconds` | `60` | 同一接收方两次发码的最短间隔 |
| `daily_limit_per_recipient` | `10` | 同一接收方每日上限 |
| `daily_limit_global` | `0`（不限） | 每个通道全站每日上限，**生产必须按短信预算设置** |

未配置短信 / 邮件发送通道时，发码会报错。本地开发可设置：

```toml
[security.verification]
simulate = true
```

登录失败按「身份域 + 账号 + 客户端 IP」节流：失败 5 次冷却 15 分钟，返回 429 并带 `Retry-After`。

### 网络准入

`application.conf` 中的 `[security.network.admin]` / `[security.network.app]` 可按 IP / CIDR 白名单限制路由组（先于认证检查），默认关闭。

## 快速开始

### 环境要求

- **JDK 17+**（仅构建需要，运行时不需要）
- **PostgreSQL**（唯一支持的数据库）
- **Redis**（默认 `127.0.0.1:6379`）
- 按上文布局检出全部兄弟仓库

### 1. 建库并配置连接

先创建一个空的 PostgreSQL 数据库，再修改 `application/config/database.conf`（`application.conf` 里的 `[database]` 保持一致）：

```toml
[default]
driver = "POSTGRESQL"
uri = "postgresql://user:password@localhost:5432/neton-application"

[migration]
history_table = "neton_schema_history"
```

配置成其他驱动时，module-system 会在启动时拒绝。

### 2. 编译

```bash
./gradlew :application:linkDebugExecutableMacosArm64   # macOS Apple Silicon
./gradlew :application:linkDebugExecutableLinuxX64     # Linux x64
./gradlew :application:linkDebugExecutableLinuxArm64   # Linux ARM64
./gradlew :application:linkDebugExecutableMingwX64     # Windows x64
```

Release 构建把 `linkDebugExecutable` 换成 `linkReleaseExecutable`。产物：`application/build/bin/<target>/<debug|release>Executable/application.kexe`。

### 3. 执行迁移

建表与升级**只走** `migrate` 命令。SQL 已编译进二进制，不需要也不允许用 `psql` 手工导入 SQL。启动绝不自动迁移：存在待执行、执行失败或校验和不一致的迁移时，启动直接中止。

```bash
cd application
export NETON_SECURITY__JWT__SECRETKEY="$(openssl rand -base64 48)"
./build/bin/macosArm64/debugExecutable/application.kexe migrate up
```

子命令：`migrate status`（有待执行项时退出码为 1）、`migrate up`、`migrate verify`。已执行的脚本有校验和，禁止修改。

### 4. 启动

必须在 `application` 目录下启动，才能找到 `config/`：

```bash
cd application
./build/bin/macosArm64/debugExecutable/application.kexe
```

默认监听 `http://0.0.0.0:8080`。

注意：
- `module-cs` 需要 `config/privchat.conf`（PrivChat service API 地址与 master key），缺失时启动失败。不要把真实密钥提交进仓库。
- 默认管理员账号（由迁移写入）：`admin` / `admin123`，**请立即修改密码**。

### 5. 验证

```bash
curl http://localhost:8080/admin/system/health/live    # 进程存活，不探测任何依赖
curl http://localhost:8080/admin/system/health/ready   # 探测 PostgreSQL 与 Redis，任一不可用返回 503
```

两者都可匿名访问。重启策略用 `live`，负载均衡用 `ready`。根路径 `/` 只返回固定的 `ok`，不反映依赖状态。

### 6. 前端

- **管理后台**：`neton-application-front`（React 19 + Next.js）为官方管理端；`neton-application-admin`（Vue）为双轨期旧版，逐步下线。
- **用户中台**：`neton-application-client`（React 19 + Next.js），对接 `/app` 组。

```bash
cd ../neton-application-front && pnpm install && pnpm dev
```

## 配置说明

`application/config/` 下的文件：

| 文件 | 用途 |
|------|------|
| `application.conf` | 服务、JWT、统一令牌、网络准入、日志 |
| `database.conf` | PostgreSQL 连接与迁移历史表 |
| `routing.conf` | 路由组 |
| `storage.conf` | 文件存储后端（默认本地，附 S3 模板） |
| `privchat.conf` | module-cs 使用的 PrivChat service API |
| `geolite.conf` | GeoLite2 City 数据库路径（需按 MaxMind 许可自行获取） |

优先级：CLI 参数 > 环境变量 > 环境配置文件 > 基础配置文件 > 框架默认值。
环境变量以 `NETON_` 为前缀、`__` 表示层级（如 `NETON_SECURITY__JWT__SECRETKEY` → `security.jwt.secretKey`）。

`./application.kexe --env=prod` 会加载 `application.prod.conf` 等环境覆盖文件。

CORS 在 `Main.kt` 的 `http { cors { … } }` 中配置，开发期默认放开所有来源，生产须收紧。

## 测试

```bash
# 模块单元测试
./gradlew :module-system:macosArm64Test :module-account:macosArm64Test :module-member:macosArm64Test
```

### 安全回归 E2E

`tests/security/realm-session-e2e.sh` 用真实二进制 + 真实 PostgreSQL + 隔离的 Redis 运行，断言：占位密钥拒绝启动、身份域交叉矩阵、CSRF、refresh 令牌单次使用、退出只吊销当前会话、改密作废全部会话、撤权即时生效、登录失败冷却、全端退出与登录审计。

依赖：本机 PostgreSQL（`createdb` / `dropdb` / `psql` 在 `PATH` 中，且用户有建库权限）、`redis-server` 与 `redis-cli`、`curl`、`python3`。先编译 debug 二进制：

```bash
./gradlew :application:linkDebugExecutableMacosArm64
tests/security/realm-session-e2e.sh
```

| 环境变量 | 默认值 | 含义 |
|----------|--------|------|
| `APP_BIN` | `application/build/bin/macosArm64/debugExecutable/application.kexe` | 被测二进制 |
| `PGHOST` / `PGPORT` / `PGUSER` / `PGPASSWORD` | `localhost` / `5432` / 当前用户 / 空 | PostgreSQL 连接 |
| `E2E_PORT` | `18090` | 应用端口 |
| `E2E_REDIS_PORT` | `6398` | 临时 Redis 端口 |

脚本会创建临时数据库与 Redis 实例，退出时清理。

### CI

`.github/workflows/backend-ci.yml`：按锁定版本检出依赖，跑模块单测并链接二进制（macOS），在 PostgreSQL 16 + Redis 上跑安全 E2E，构建 Linux x64 release 二进制，并跑 outbox 的 PostgreSQL 契约测试。

## 开发指南

### 新增 Controller

```kotlin
// <模块>/src/commonMain/kotlin/controller/admin/example/ExampleController.kt
package controller.admin.example

@Controller("/example")
class ExampleController(private val logic: ExampleLogic) {

    @Get("/list")
    @Permission("system:example:list")
    suspend fun list(page: Int = 1, size: Int = 20) = logic.page(page, size)

    @Post("/create")
    @Permission("system:example:create")
    suspend fun create(request: CreateExampleRequest): Long = logic.create(request)
}
```

KSP 自动生成路由注册。路由组按包名推断（`controller.admin.*` → `/admin`，`controller.app.*` → `/app`，`controller.manage.*` → `/manage`）。分页接口返回 `dto.PageResponse<T>`。

### 新增 Model + Table

```kotlin
@Serializable
@Table("system_examples")
data class Example(
    @Id val id: Long = 0,
    val name: String,
    val status: Int = 0,
    @SoftDelete val deleted: Int = 0,
    @CreatedAt val createdAt: String? = null,
    @UpdatedAt val updatedAt: String? = null
)

object ExampleTable : Table<Example, Long> by ExampleTableImpl
```

表结构变更写在模块仓库新的 `sql/postgresql/V*.sql` 中，通过 `migrate up` 执行。

### 构建命令

```bash
./gradlew :application:compileKotlinMacosArm64          # 仅编译
./gradlew :application:linkDebugExecutableMacosArm64    # 编译 + 链接
./gradlew clean :application:linkDebugExecutableMacosArm64
```

修改 Model 后出现 “Unresolved reference *TableImpl” 时，删除该模块的 `build/generated/ksp` 与 `build/kspCaches` 后重新编译。

## License

Private - All Rights Reserved
