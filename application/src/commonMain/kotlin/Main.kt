import com.netonstream.privchat.application.module.privchat.client.PrivchatServiceClient
import com.netonstream.privchat.application.module.privchat.client.PrivchatServiceClientImpl
import infra.TableRegistryBuilder
import neton.core.Neton
import neton.core.config.ConfigLoader
import neton.core.generated.GeneratedNetonConfigRegistry
import neton.database.database
import neton.http.http
import neton.redis.redis
import neton.routing.routing
import neton.security.security
import neton.storage.storage
import security.WildcardPermissionEvaluator

import init.SystemModuleInitializer
import init.InfraModuleInitializer
import init.MemberModuleInitializer
import init.PaymentModuleInitializer
import init.PlatformModuleInitializer
import init.PrivchatModuleInitializer

fun main(args: Array<String>) {
    val tableRegistryBuilder = TableRegistryBuilder()

    // 预构造 PrivchatServiceClient 并通过 DSL bind 提前绑定到 ctx，确保
    // SecurityConfig（在 SecurityComponent.start 阶段触发，先于 module init）能拿到。
    // 重复绑定由 PrivchatModuleInitializer 兜底（用 bindIfAbsent 语义）。
    val privchatServiceClient = buildPrivchatServiceClient(args)

    Neton.run(args) {
        configRegistry(GeneratedNetonConfigRegistry)

        // 预绑定 TableRegistryBuilder，供各模块 initialize() 中注册 Table
        bind(TableRegistryBuilder::class, tableRegistryBuilder)
        // 预绑定 PrivchatServiceClient（spec MODULE_PRIVCHAT §5）
        bind(PrivchatServiceClient::class, privchatServiceClient)

        http { }

        // database 启动只做连接探活与 tableRegistry 注入。
        // 严禁在此或任何 ModuleInitializer 中调用 ensureTable()/ALTER 等 schema 变更逻辑。
        // schema 演进的唯一权威路径是手动 SQL 脚本（sql/{dialect}/V*.sql）。
        // 详见架构边界：neton-docs/docs/spec/migration.md
        database {
            tableRegistry = tableRegistryBuilder.build()
        }

        security {
            // 注入应用脚手架的通配权限评估器（rbac-spec §4.2）。
            // 框架默认 PermissionEvaluator 是精确匹配，不支持 *:*:* 等通配；
            // 必须由脚手架显式覆盖。
            setPermissionEvaluator(WildcardPermissionEvaluator())
        }

        // Redis 装载（SMS code 暂存、限流、token 黑名单等都需要）。
        // 配置文件 config/redis.conf；缺省 host=127.0.0.1 port=6379。
        redis { }

        // 文件存储：用户态上传统一走 default source（spec
        // MODULE_MEMBER_PROFILE_SPEC §4.2）。配置在 config/storage.conf；
        // 第一版 local，后续切 S3 / OSS 不需要业务代码改动。
        storage { }

        // routing { } 已内置限流能力（Redis 优先，无 Redis 降级本地内存）
        // @RateLimit 注解标注的接口将自动生效
        routing { }

        modules(
            SystemModuleInitializer,
            InfraModuleInitializer,
            PrivchatModuleInitializer,
            MemberModuleInitializer,
            PaymentModuleInitializer,
            PlatformModuleInitializer,
        )
    }
}

/**
 * 直接读 `config/privchat.conf` 构造 PrivchatServiceClient（与 PrivchatModuleInitializer
 * 内部一致），用于在 Neton DSL 阶段预绑定。
 */
private fun buildPrivchatServiceClient(args: Array<String>): PrivchatServiceClient {
    val config = ConfigLoader.loadModuleConfig(
        "privchat",
        configPath = "config",
        environment = ConfigLoader.resolveEnvironment(args),
        args = args,
    ) ?: error("Missing privchat.conf — application 必须提供 [server] 配置")
    val baseUrl = ConfigLoader.getString(config, "server.service_api_base_url")
        ?.trim()?.takeIf { it.isNotEmpty() }
        ?: error("privchat.conf: server.service_api_base_url 必填")
    val serviceKey = ConfigLoader.getString(config, "server.service_master_key")
        ?.trim()?.takeIf { it.isNotEmpty() }
        ?: error("privchat.conf: server.service_master_key 必填（NETON_PRIVCHAT__SERVER__SERVICE_MASTER_KEY 可覆盖）")
    val businessSystemId = ConfigLoader.getString(config, "server.business_system_id")
        ?.trim()?.takeIf { it.isNotEmpty() }
    return PrivchatServiceClientImpl(
        baseUrl = baseUrl.trimEnd('/'),
        serviceMasterKey = serviceKey,
        businessSystemId = businessSystemId,
    )
}
