import infra.TableRegistryBuilder
import neton.core.Neton
import neton.core.generated.GeneratedNetonConfigRegistry
import neton.database.database
import neton.http.http
import neton.routing.routing
import neton.security.security

import init.SystemModuleInitializer
import init.InfraModuleInitializer
import init.MemberModuleInitializer
import init.PaymentModuleInitializer
import init.PlatformModuleInitializer

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

        security { }

        // routing { } 已内置限流能力（Redis 优先，无 Redis 降级本地内存）
        // @RateLimit 注解标注的接口将自动生效
        routing { }

        modules(
            SystemModuleInitializer,
            InfraModuleInitializer,
            MemberModuleInitializer,
            PaymentModuleInitializer,
            PlatformModuleInitializer,
        )
    }
}
