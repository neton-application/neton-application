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

        database {
            tableRegistry = tableRegistryBuilder.build()
        }

        security { }

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
