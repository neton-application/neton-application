package infra

import neton.database.api.Table
import kotlin.reflect.KClass

/**
 * Table 注册表构建器
 *
 * 各模块在 ModuleInitializer.initialize(ctx) 中通过 ctx.get(TableRegistryBuilder::class)
 * 注册自己的 Table 映射。Main.kt 在 database { } 中调用 build() 获取 TableRegistry 函数。
 *
 * build() 返回的闭包捕获了 entries 引用，模块后续注册的 Table 在查询时自动可见。
 * 这是因为 database { } 在 install 阶段执行（早于模块初始化），而模块初始化在
 * initializeInfrastructure 阶段执行——闭包捕获保证了时序正确。
 */
class TableRegistryBuilder {
    private val entries = mutableMapOf<KClass<*>, Table<*, *>>()

    fun register(clazz: KClass<*>, table: Table<*, *>) {
        entries[clazz] = table
    }

    /**
     * 构建 TableRegistry 函数。
     * 返回的闭包捕获了 entries 引用，后续 register 的 Table 自动生效。
     */
    fun build(): (KClass<*>) -> Table<*, *>? = { clazz ->
        @Suppress("UNCHECKED_CAST")
        entries[clazz]
    }
}
