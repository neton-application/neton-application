# Neton Application

A general-purpose admin backend built on the [Neton](../neton) framework and compiled to a native binary with Kotlin/Native — fast startup, small memory footprint, single-file deployment, no JVM at runtime.

This repository is the **base distribution**: it only assembles modules (application entry, configuration, CI). All business code lives in independent module repositories.

> Specifications (source of truth) live in `neton-docs`:
> [SPEC.md](../neton-docs/docs/zh-hans/spec/application/SPEC.md) ·
> [Identity realms & sessions](../neton-docs/docs/zh-hans/spec/application/identity-realm.md) ·
> [Auth abuse protection](../neton-docs/docs/zh-hans/spec/application/auth-abuse-protection-spec.md)
>
> Engineering rules: [docs/ENGINEERING_RULES.md](./docs/ENGINEERING_RULES.md) ·
> Module template: [docs/module-template-spec.md](./docs/module-template-spec.md)

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language | Kotlin 2.4.20 (Multiplatform) |
| Framework | Neton 1.0.0-beta1 |
| Compilation | Kotlin/Native → native binary (`application.kexe`) |
| Database | **PostgreSQL only** |
| Cache / rate limiting / verification codes | Redis |
| Authentication | Realm-isolated JWT (HS256) + server-side sessions |
| Build | Gradle 8.14 + KSP |
| Logging | Structured JSON, multiple sinks |

Runtime: cold start ~150 ms (debug) / ~3 ms (release), ~20 MB memory.

## Repositories

Multi-repo + Gradle Composite Build. One repository per module; this distribution only assembles them.

| Repository | Contents |
|------------|----------|
| `neton` | Framework (core, http, database, security, …) |
| `neton-application` | This repo: application entry, config, CI. No business module code |
| `neton-application-module-system` | Admin identity, authentication & sessions, RBAC, MFA, verification codes, audit, settings, health probes |
| `neton-application-module-infra` | Files, scheduled jobs, API logs, Redis monitoring, domain events |
| `neton-application-module-account` | End-user accounts: registration/login, profile, mobile/email/password, account sessions |
| `neton-application-module-member` | Member growth: levels, points, check-in, invites, addresses, groups/tags (depends on account) |
| `neton-application-module-payment` | Payment: channels, orders, refunds, wallets, transfers, withdrawals, bank cards |
| `neton-application-module-platform` | Open platform: clients, API catalog, billing, call logs and statistics |
| `neton-application-module-cs` | Customer service (talks to PrivChat via `privchat-service-client`) |
| `neton-application-front` (+ `neton-application-front-*`) | Admin frontend (React 19 + Next.js) |
| `neton-application-client` (+ `neton-application-client-*`) | End-user console (React 19 + Next.js) |

Workspace layout (paths in `settings.gradle.kts` are relative to it):

```
projects/
├── Neton/
│   ├── neton/                               # Framework
│   ├── geolite4k/
│   ├── neton-application/                   # This repo
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
    └── privchat-service-client/             # Used by module-cs
```

The exact repositories and pinned commits for a clean checkout are listed in `scripts/ci/dependencies.lock`; `scripts/ci/checkout-dependencies.sh <workspace-root>` checks them out (CI uses this).

This repo:

```
neton-application/
├── application/
│   ├── src/commonMain/kotlin/Main.kt   # Entry point (assembles modules and infrastructure)
│   └── config/                         # Runtime config (TOML)
├── scripts/ci/                         # Dependency lock & checkout scripts
├── tests/security/                     # Identity realm & session E2E
├── build.gradle.kts / settings.gradle.kts
```

## Architecture

### Layers

```
Controller  ──  HTTP routing, parameter binding, @Permission
     │
   Logic    ──  Business logic, transaction orchestration
     │
   Table    ──  Data access (KSP-generated CRUD)
     │
   DB       ──  PostgreSQL
```

### Route Groups

Defined in `application/config/routing.conf`:

| Group | Mount | Purpose | requireAuth | Accepted tokens |
|-------|-------|---------|-------------|-----------------|
| `admin` | `/admin` | Admin API | true | Admin realm only |
| `app` | `/app` | End-user / mobile API | true | Account realm only |
| `manage` | `/manage` | Operator management center | true | Account realm only |
| `open` | `/platform` | Open platform API | as needed | No application tokens (uses its own authenticator) |

### Module Dependencies

```
application
├── module-system        ← Core
├── module-infra         ← system
├── module-account       ← system, infra
├── module-member        ← account
├── module-payment       ← system, account
├── module-platform      ← system
└── module-cs            ← account, payment
```

The module list has a single source: `ksp { arg("neton.modules", ...) }` in `application/build.gradle.kts`. It drives the `migrate` command, the startup precheck and the server.

## Security

### JWT secret

The signing key is supplied **only** via the environment variable `NETON_SECURITY__JWT__SECRETKEY`. It must be at least 32 characters. If it is missing, still the placeholder (`REPLACE_ME…`) or too short, the application refuses to start — in every environment, including `dev`.

```bash
export NETON_SECURITY__JWT__SECRETKEY="$(openssl rand -base64 48)"
```

### Identity realms and sessions

- Admin tokens and account tokens are separate realms. The `admin` group accepts only admin tokens; `app` and `manage` accept only account tokens. A token from the wrong realm gets 401.
- Refresh tokens cannot be used as access tokens. Refresh tokens are single-use (admin and the built-in account realm); reuse beyond a 30-second concurrency window revokes the whole session.
- `POST …/auth/logout` revokes only the current session. `POST /admin/system/auth/logout-all` and `POST /app/auth/logout-all` revoke all sessions of that user.
- Password change and permission revocation take effect immediately, without waiting for token expiry.
- The admin frontend uses an HttpOnly session cookie; cookie-authenticated write requests must send `X-Neton-Csrf: 1`.
- **After upgrading to this version, all previously issued tokens are rejected** (they carry no `realm` claim). Users must log in again.

Details: [identity-realm.md](../neton-docs/docs/zh-hans/spec/application/identity-realm.md).

### Verification codes (SMS / email)

Configured under `[security.verification]` in `application.conf`:

| Key | Default | Meaning |
|-----|---------|---------|
| `simulate` | `false` | Store codes without sending. **Local development only** |
| `resend_cooldown_seconds` | `60` | Minimum interval per recipient |
| `daily_limit_per_recipient` | `10` | Daily cap per recipient |
| `daily_limit_global` | `0` (unlimited) | Site-wide daily cap per channel. **Set this in production** according to your SMS budget |

Without a configured SMS/email channel, sending a code returns an error. For local development:

```toml
[security.verification]
simulate = true
```

Login failures are throttled per realm + account + client IP (5 failures → 15-minute cooldown, HTTP 429 with `Retry-After`).

### Admin network allowlist

`[security.network.admin]` / `[security.network.app]` in `application.conf` restrict a route group to IP/CIDR lists (checked before authentication). Disabled by default.

## Quick Start

### Prerequisites

- **JDK 17+** (build only; not needed at runtime)
- **PostgreSQL** (the only supported database)
- **Redis** (default `127.0.0.1:6379`)
- All sibling repositories checked out as shown above

### 1. Create the database and configure the connection

Create an empty PostgreSQL database, then edit `application/config/database.conf` (keep `[database]` in `application.conf` consistent):

```toml
[default]
driver = "POSTGRESQL"
uri = "postgresql://user:password@localhost:5432/neton-application"

[migration]
history_table = "neton_schema_history"
```

Any other driver is rejected by module-system at startup.

### 2. Build

```bash
./gradlew :application:linkDebugExecutableMacosArm64   # macOS Apple Silicon
./gradlew :application:linkDebugExecutableLinuxX64     # Linux x64
./gradlew :application:linkDebugExecutableLinuxArm64   # Linux ARM64
./gradlew :application:linkDebugExecutableMingwX64     # Windows x64
```

For release builds replace `linkDebugExecutable` with `linkReleaseExecutable`. Output: `application/build/bin/<target>/<debug|release>Executable/application.kexe`.

### 3. Run migrations

Schema creation and upgrades go **only** through the `migrate` command. SQL is compiled into the binary; there is no need (and it is not allowed) to import SQL files with `psql` by hand. Startup never migrates automatically: if migrations are pending, failed or mismatched, startup aborts.

```bash
cd application
export NETON_SECURITY__JWT__SECRETKEY="$(openssl rand -base64 48)"
./build/bin/macosArm64/debugExecutable/application.kexe migrate up
```

Subcommands: `migrate status` (exits 1 if anything is pending), `migrate up`, `migrate verify`. Applied scripts are checksummed and must not be modified.

### 4. Start

Run from the `application` directory so `config/` is found:

```bash
cd application
./build/bin/macosArm64/debugExecutable/application.kexe
```

Listens on `http://0.0.0.0:8080` by default.

Notes:
- `module-cs` requires `config/privchat.conf` (PrivChat service API URL and master key); startup fails without it. Do not commit real keys.
- Default admin account (seeded by migrations): `admin` / `admin123`. **Change the password immediately.**

### 5. Verify

```bash
curl http://localhost:8080/admin/system/health/live    # process is up, touches no dependencies
curl http://localhost:8080/admin/system/health/ready   # probes PostgreSQL and Redis; 503 if any is unavailable
```

Both are anonymous. Use `live` for restart policies and `ready` for load balancing. The root path `/` only returns a static `ok` and does not reflect dependency health.

### 6. Frontends

- **Admin console**: `neton-application-front` (React 19 + Next.js) is the official admin frontend; `neton-application-admin` (Vue) is the legacy version during the dual-track period and is being phased out.
- **End-user console**: `neton-application-client` (React 19 + Next.js), using the `/app` group.

```bash
cd ../neton-application-front && pnpm install && pnpm dev
```

## Configuration

Files in `application/config/`:

| File | Purpose |
|------|---------|
| `application.conf` | Server, JWT, unified token, network allowlist, logging |
| `database.conf` | PostgreSQL connection and migration history table |
| `routing.conf` | Route groups |
| `storage.conf` | File storage backends (local by default; S3 template included) |
| `privchat.conf` | PrivChat service API used by module-cs |
| `geolite.conf` | Path to the GeoLite2 City database (obtain under the MaxMind license) |

Priority: CLI args > environment variables > environment config file > base config file > framework defaults.
Environment variables use the `NETON_` prefix with `__` as the level separator (e.g. `NETON_SECURITY__JWT__SECRETKEY` → `security.jwt.secretKey`).

`./application.kexe --env=prod` loads environment-specific overrides such as `application.prod.conf`.

CORS is configured in `Main.kt` (`http { cors { … } }`) and allows all origins by default for development; tighten it for production.

## Testing

```bash
# Module unit tests
./gradlew :module-system:macosArm64Test :module-account:macosArm64Test :module-member:macosArm64Test
```

### Security regression E2E

`tests/security/realm-session-e2e.sh` runs the real binary against a real PostgreSQL and an isolated Redis, and asserts: placeholder key refuses to start, realm cross matrix, CSRF, single-use refresh tokens, logout revokes only the current session, password change revokes all sessions, immediate permission revocation, login-failure cooldown, logout-all and login audit.

Requirements: local PostgreSQL (`createdb`/`dropdb`/`psql` on `PATH`, user allowed to create databases), `redis-server` and `redis-cli`, `curl`, `python3`. Build the debug binary first.

```bash
./gradlew :application:linkDebugExecutableMacosArm64
tests/security/realm-session-e2e.sh
```

| Variable | Default | Meaning |
|----------|---------|---------|
| `APP_BIN` | `application/build/bin/macosArm64/debugExecutable/application.kexe` | Binary under test |
| `PGHOST` / `PGPORT` / `PGUSER` / `PGPASSWORD` | `localhost` / `5432` / current user / empty | PostgreSQL connection |
| `E2E_PORT` | `18090` | Application port |
| `E2E_REDIS_PORT` | `6398` | Port of the temporary Redis |

The script creates a temporary database and Redis instance and removes them on exit.

### CI

`.github/workflows/backend-ci.yml`: checks out locked dependencies, runs module unit tests and links the binary (macOS), runs the security E2E on PostgreSQL 16 + Redis, builds the Linux x64 release binary, and runs the outbox PostgreSQL contract tests.

## Development Guide

### Adding a Controller

```kotlin
// <module>/src/commonMain/kotlin/controller/admin/example/ExampleController.kt
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

KSP generates route registration. The route group is inferred from the package (`controller.admin.*` → `/admin`, `controller.app.*` → `/app`, `controller.manage.*` → `/manage`). Paginated endpoints return `dto.PageResponse<T>`.

### Adding a Model + Table

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

Schema changes go into a new `sql/postgresql/V*.sql` in the module repository and are applied with `migrate up`.

### Build commands

```bash
./gradlew :application:compileKotlinMacosArm64          # compile only
./gradlew :application:linkDebugExecutableMacosArm64    # compile + link
./gradlew clean :application:linkDebugExecutableMacosArm64
```

If you see "Unresolved reference *TableImpl" after changing a model, delete the module's `build/generated/ksp` and `build/kspCaches` and rebuild.

## License

Private - All Rights Reserved
