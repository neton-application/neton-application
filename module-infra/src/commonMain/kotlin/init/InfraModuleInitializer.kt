package init

import infra.DbAccessLogWriter
import infra.DbErrorLogWriter
import infra.TableRegistryBuilder
import neton.core.component.NetonContext
import neton.core.interfaces.AccessLogWriter
import neton.core.interfaces.ErrorLogWriter
import neton.core.module.ModuleInitializer
import neton.logging.LoggerFactory

import model.*
import table.*
import logic.*

object InfraModuleInitializer : ModuleInitializer {

    override val moduleId: String = "infra"
    override val dependsOn: List<String> = listOf("system")

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

        // 注册 API 日志写入器
        ctx.bind(AccessLogWriter::class, DbAccessLogWriter())
        ctx.bind(ErrorLogWriter::class, DbErrorLogWriter())

        // 注册 KSP 生成的路由
        neton.module.infra.generated.InfraRouteInitializer.initialize(ctx)
    }
}
