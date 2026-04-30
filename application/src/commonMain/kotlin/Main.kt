import infra.TableRegistryBuilder
import neton.core.Neton
import neton.core.generated.GeneratedNetonConfigRegistry
import neton.database.database
import neton.http.http
import neton.redis.redis
import neton.routing.routing
import neton.security.security
import security.WildcardPermissionEvaluator

import init.SystemModuleInitializer
import init.InfraModuleInitializer
import init.MemberModuleInitializer
import init.PaymentModuleInitializer
import init.PlatformModuleInitializer
import init.PrivchatModuleInitializer

fun main(args: Array<String>) {
    val tableRegistryBuilder = TableRegistryBuilder()

    Neton.run(args) {
        configRegistry(GeneratedNetonConfigRegistry)

        // 预绑定 TableRegistryBuilder，供各模块 initialize() 中注册 Table
        bind(TableRegistryBuilder::class, tableRegistryBuilder)

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
