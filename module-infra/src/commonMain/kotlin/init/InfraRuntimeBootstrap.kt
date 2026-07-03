package init

import infra.DbAccessLogWriter
import infra.DbErrorLogWriter
import neton.core.component.NetonContext
import neton.core.interfaces.AccessLogWriter
import neton.core.interfaces.ErrorLogWriter
import neton.logging.LoggerFactory
import neton.storage.StorageOperator

import logic.*

// MANIFEST-P3: 手写 runtime bootstrap。ConfigLogic / FileLogic / JobLogic 已标 @Logic
// → 生成的 InfraLogicInitializer 装配; moduleId/dependsOn/migrations/路由 由 KSP manifest。
// 这里留: FileUploadLogic (storage + inline policyRegistry) + 日志写入器。
object InfraRuntimeBootstrap {
    fun initialize(ctx: NetonContext) {
        val loggerFactory = ctx.get(LoggerFactory::class)
        // 应用唯一用户态文件入口（spec MODULE_MEMBER_PROFILE_SPEC §4.2）。
        // storage 由 storage { } DSL 在 Main.kt 装配; policyRegistry inline 构造 → 非 @Logic。
        val storage = ctx.get(StorageOperator::class)
        val policyRegistry = FileBusinessPolicyRegistry.default()
        ctx.bind(FileBusinessPolicyRegistry::class, policyRegistry)
        ctx.bind(
            FileUploadLogic::class,
            FileUploadLogic(
                log = loggerFactory.get("logic.app-file"),
                storage = storage,
                policyRegistry = policyRegistry,
            ),
        )

        // 注册 API 日志写入器
        ctx.bind(AccessLogWriter::class, DbAccessLogWriter())
        ctx.bind(ErrorLogWriter::class, DbErrorLogWriter())
    }
}
