package init

import infra.DbAccessLogWriter
import infra.DbErrorLogWriter
import infra.TableRegistryBuilder
import neton.core.component.NetonContext
import neton.core.interfaces.AccessLogWriter
import neton.core.interfaces.ErrorLogWriter
import neton.core.module.MigrationDialect
import neton.core.module.MigrationSource
import neton.core.module.ModuleInitializer
import neton.logging.LoggerFactory
import neton.storage.StorageOperator

import model.*
import table.*
import logic.*

object InfraModuleInitializer : ModuleInitializer {

    override val moduleId: String = "infra"
    override val dependsOn: List<String> = listOf("system")

    /**
     * SQL 资源路径指向部署后的 `application/migrations/infra/postgresql/`。
     * 构建期由 `application/build.gradle.kts` 的 `copyModuleMigrations` task 把
     * 源仓库 `privchat-application/sql/postgresql/V*.sql` 拷贝到该目录。
     *
     * 短期 infra 持有 system+infra 合并的 SQL(DB-MIG-7A 拍板)。
     * TODO(DB-MIG-7B): split system_* tables out into module-system owned migrations,
     * 把 system_* 表从 `privchat-application/sql/postgresql/` 拆到
     * `privchat-application/module-system/sql/postgresql/`,再让 SystemModuleInitializer
     * 声明自己的 migrations()。
     */
    override fun migrations(): List<MigrationSource> = listOf(
        MigrationSource(
            moduleId = moduleId,
            dialect = MigrationDialect.POSTGRESQL,
            resourcePath = "migrations/$moduleId/postgresql",
        )
    )

    override fun initialize(ctx: NetonContext) {
        val loggerFactory = ctx.get(LoggerFactory::class)
        val registry = ctx.get(TableRegistryBuilder::class)

        // 注册 Table
        registry.register(Config::class, ConfigTable)
        registry.register(FileInfo::class, FileInfoTable)
        registry.register(FileConfig::class, FileConfigTable)
        registry.register(ApiAccessLog::class, ApiAccessLogTable)
        registry.register(ApiErrorLog::class, ApiErrorLogTable)
        registry.register(Job::class, JobTable)
        registry.register(JobLog::class, JobLogTable)

        // 绑定 Logic
        ctx.bind(ConfigLogic::class, ConfigLogic(loggerFactory.get("logic.config")))
        ctx.bind(FileLogic::class, FileLogic(loggerFactory.get("logic.file")))
        ctx.bind(JobLogic::class, JobLogic(loggerFactory.get("logic.job")))

        // 应用唯一用户态文件入口（spec MODULE_MEMBER_PROFILE_SPEC §4.2）。
        // 依赖 default StorageOperator（neton-storage）+ FileBusinessPolicyRegistry。
        // 注：StorageOperator 由 storage { } DSL 在 Main.kt 装配；如未装配此处会拿到 null。
        val storage = ctx.get(StorageOperator::class)
        val policyRegistry = FileBusinessPolicyRegistry.default()
        ctx.bind(FileBusinessPolicyRegistry::class, policyRegistry)
        ctx.bind(
            AppFileLogic::class,
            AppFileLogic(
                log = loggerFactory.get("logic.app-file"),
                storage = storage,
                policyRegistry = policyRegistry,
            ),
        )

        // 注册 API 日志写入器
        ctx.bind(AccessLogWriter::class, DbAccessLogWriter())
        ctx.bind(ErrorLogWriter::class, DbErrorLogWriter())

        // 注册 KSP 生成的路由
        neton.module.infra.generated.InfraRouteInitializer.initialize(ctx)
    }
}
