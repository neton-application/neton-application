package migration

import neton.core.module.MigrationDialect
import neton.core.module.MigrationSource
import neton.core.module.ModuleInitializer

/**
 * 共用 collection 逻辑: 轮询所有 ModuleInitializer 拿 migration sources。
 *
 * 同时被 `migrate` 子命令(执行迁移)和 `startup precheck`(检测 pending fail-fast)使用。
 *
 * 设计原则(2026-06-01 拍板):
 *   - **模块自管 SQL**: framework / application **不**维护 `moduleId → path` 的集中表
 *   - 每个模块的 `ModuleInitializer.migrations()` 自己声明 `MigrationSource`,application
 *     在这里只做拓扑排序 + flatMap + dialect 过滤,**不**改写 resourcePath
 *   - 这层独立放 application,不进 neton-database / neton-core(framework 不感知 modules)
 */
internal object ApplicationMigrationSources {

    /**
     * 收集所有模块声明的 [MigrationSource],按以下规则排序与过滤:
     *
     *   1. 模块按 [ModuleInitializer.dependsOn] 拓扑序(Kahn 稳定)
     *   2. 每个模块内的 sources 顺序按 `migrations()` 返回顺序(模块自己负责)
     *   3. 过滤 `source.dialect == dialect` —— application 当前链接的 sqlx4k driver
     *      决定可执行的 dialect(NETON-DB-VARIANT 单 driver 约束)
     *
     * @return 已排序 + 已过滤的 sources;cross-module 间按拓扑序,同一模块内按声明序
     */
    fun collect(modules: List<ModuleInitializer>, dialect: MigrationDialect): List<MigrationSource> {
        val sorted = topologicalSort(modules)
        return sorted.flatMap { it.migrations() }.filter { it.dialect == dialect }
    }

    /**
     * Kahn 拓扑排序模块。与 `Neton.kt` 内部 sort 等价,独立实现以避免 migrate 路径
     * 依赖 Neton.run 主启动流程。fail-fast: duplicate / missing dep / cycle 直接抛。
     */
    private fun topologicalSort(modules: List<ModuleInitializer>): List<ModuleInitializer> {
        if (modules.all { it.dependsOn.isEmpty() }) return modules

        val byId = mutableMapOf<String, ModuleInitializer>()
        for (m in modules) {
            require(m.moduleId !in byId) { "duplicate moduleId '${m.moduleId}'" }
            byId[m.moduleId] = m
        }
        for (m in modules) {
            for (dep in m.dependsOn) {
                require(dep in byId) { "module '${m.moduleId}' dependsOn '$dep' (not registered)" }
            }
        }

        val inDeg = mutableMapOf<String, Int>()
        for (m in modules) inDeg[m.moduleId] = m.dependsOn.size

        val queue = ArrayDeque<String>()
        for (m in modules) if (inDeg[m.moduleId] == 0) queue.addLast(m.moduleId)

        val result = mutableListOf<ModuleInitializer>()
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            result += byId[id]!!
            for (m in modules) {
                if (id in m.dependsOn) {
                    val deg = (inDeg[m.moduleId] ?: 1) - 1
                    inDeg[m.moduleId] = deg
                    if (deg == 0) queue.addLast(m.moduleId)
                }
            }
        }

        check(result.size == modules.size) {
            val unresolved = modules.map { it.moduleId }.filter { id -> result.none { it.moduleId == id } }
            "circular module dependency among: $unresolved"
        }
        return result
    }
}
