package migration

import kotlinx.coroutines.runBlocking
import neton.core.config.getEnv
import neton.core.module.ModuleInitializer
import neton.database.adapter.sqlx.SqlxDatabase
import neton.database.adapter.sqlx.SqlxDbContext
import neton.database.migration.MigrationCommand
import neton.database.migration.MigrationEngine
import neton.database.migration.MigrationResult

/**
 * 正常启动 (`./application.kexe` 无 args) 之前跑一次的 migration 状态检查。
 *
 * SPEC §0.6 红线: serve 模式**绝不**执行 DDL,只检查 pending → fail-fast。
 *
 * 复用同一个 [ApplicationMigrationSources.collect] —— 跟 `migrate` 子命令一模一样的
 * 模块轮询逻辑,只差最后一步:这里跑 [MigrationCommand.STATUS](read-only),migrate 跑
 * UP/STATUS/VERIFY。
 *
 * 红线允许做的事:
 *   - 连数据库
 *   - 读 history 表(如果存在),按 SPEC §6.4 状态模型决定 pending/failed/mismatch
 *   - 打印诊断信息
 *   - exit 非 0
 *
 * 红线禁止做的事:
 *   - 创建 history 表(history 不存在 = schema 没初始化 = fail-fast)
 *   - 执行 V*.sql
 *   - 建任何业务表
 *   - 写 history
 *
 * **MigrationEngine 的 STATUS 路径不写表**(参 [neton.database.migration.MigrationEngine.runStatus]):
 * 只 `history.exists()` 探测,不 `ensureExists()`。如果 history 表不存在,返回
 * `Status(historyExists=false, scripts=[全部 PENDING])`,我们 fail-fast。
 */
internal object MigrationStartupPrecheck {

    /**
     * @return null = OK(可以继续启动);非空 String = 应 fail-fast 的原因
     *
     * **Smoke-only bypass**: 当环境变量 `NETON_MIGRATION_PRECHECK_STRICT=false` 时,
     * precheck 失败(包括抛异常 / 返回非空 reason)降级为 warn,允许进程继续启动。
     * 默认行为(env 未设置或为其他值)仍保持严格 fail-fast。
     *
     * 此 bypass 仅用于本地 smoke,用于绕过遗留 DB 的 `neton_schema_history_*` 旧 schema
     * 与新 framework 的 schema 失配(NETON-DB-MIGRATION-HISTORY-UPGRADE 待办)。
     */
    fun check(args: Array<String>, modules: List<ModuleInitializer>): String? {
        val strict = getEnv("NETON_MIGRATION_PRECHECK_STRICT") != "false"
        return try {
            val reason = doCheck(args, modules)
            if (reason != null && !strict) {
                println(
                    "WARN MigrationStartupPrecheck bypassed: current DB likely uses legacy " +
                        "neton_schema_history_* schema. This is a SMOKE-ONLY bypass " +
                        "(NETON_MIGRATION_PRECHECK_STRICT=false). " +
                        "TODO NETON-DB-MIGRATION-HISTORY-UPGRADE — proper fix is to upgrade 5 " +
                        "history tables to new (module_id, installed_at, execution_ms, success bool) " +
                        "schema. reason=${reason.lineSequence().firstOrNull() ?: reason}"
                )
                null
            } else {
                reason
            }
        } catch (e: Throwable) {
            if (strict) throw e
            println(
                "WARN MigrationStartupPrecheck bypassed: current DB likely uses legacy " +
                    "neton_schema_history_* schema. This is a SMOKE-ONLY bypass " +
                    "(NETON_MIGRATION_PRECHECK_STRICT=false). " +
                    "TODO NETON-DB-MIGRATION-HISTORY-UPGRADE — proper fix is to upgrade 5 " +
                    "history tables to new (module_id, installed_at, execution_ms, success bool) " +
                    "schema. error=${e::class.simpleName}: ${e.message}"
            )
            null
        }
    }

    private fun doCheck(args: Array<String>, modules: List<ModuleInitializer>): String? {
        val cfg = when (val r = MigrationConfigLoader.load(args)) {
            is MigrationConfigLoader.Result.Ok -> r
            is MigrationConfigLoader.Result.Failure ->
                return "migration config invalid: ${r.message}"
        }

        // 初始化 DB — 这里直接复用全局 SqlxDatabase.initialize, Neton.run 主流程后续会再 init
        // 一次(sqlx4k 允许覆盖). 这是 dev 便利路径; 生产建议:
        //   先 `application.kexe migrate up` 跑完, 再 `application.kexe` 起服务.
        // 这种部署节奏下 precheck 只是兜底, 实际命中也只是 fail-fast 拒绝启动.
        try {
            SqlxDatabase.initialize(cfg.database)
        } catch (e: Throwable) {
            return "database connect failed: ${e.message}"
        }

        val sources = ApplicationMigrationSources.collect(modules, cfg.migration.dialect)
        if (sources.isEmpty()) {
            // 没有模块声明当前 dialect 的 SQL = 没有 schema 要管。早退,不打扰启动。
            // (典型场景: MEMORY driver dev 路径, modules 全部只声明 POSTGRESQL)
            return null
        }

        val engine = MigrationEngine(SqlxDbContext, cfg.migration)
        val result = runBlocking { engine.run(MigrationCommand.STATUS, sources) }

        return when (result) {
            is MigrationResult.Aborted ->
                "migration precheck aborted: ${result.reason}\n" +
                    "please run: ./application.kexe migrate up"

            is MigrationResult.Status -> when {
                // history 表不存在 → schema 未初始化
                !result.historyExists -> buildString {
                    appendLine("schema not initialized: migration history table '${cfg.migration.historyTable}' does not exist.")
                    appendLine("please run: ./application.kexe migrate up")
                    if (result.pendingCount > 0) {
                        appendLine("pending scripts: ${result.pendingCount}")
                        result.scripts.take(10).forEach {
                            appendLine("  [${it.moduleId}] V${it.version}  ${it.description}")
                        }
                        if (result.scripts.size > 10) appendLine("  ... ${result.scripts.size - 10} more")
                    }
                }

                // FAILED rows — manual intervention
                result.failedCount > 0 -> buildString {
                    appendLine("migration history has ${result.failedCount} FAILED row(s); manual intervention required:")
                    result.scripts.filter { it.state == MigrationResult.ScriptState.FAILED }.forEach {
                        appendLine("  [${it.moduleId}] V${it.version}  err=${it.errorMessage}")
                    }
                    appendLine("see migration spec §6.6 (recovery from FAILED).")
                }

                // checksum mismatch — 磁盘 vs history 不一致
                result.mismatchCount > 0 -> buildString {
                    appendLine("migration history checksum mismatch on ${result.mismatchCount} script(s):")
                    result.scripts.filter { it.state == MigrationResult.ScriptState.CHECKSUM_MISMATCH }.forEach {
                        appendLine(
                            "  [${it.moduleId}] V${it.version}: " +
                                "disk=${it.diskChecksum?.take(8)} history=${it.historyChecksum?.take(8)}"
                        )
                    }
                    appendLine("revert SQL or create a new V*.sql to evolve; never edit applied scripts.")
                }

                // pending — 有未应用脚本
                result.pendingCount > 0 -> buildString {
                    appendLine("${result.pendingCount} pending migration(s) detected. please run:")
                    appendLine("  ./application.kexe migrate up")
                    result.scripts.filter { it.state == MigrationResult.ScriptState.PENDING }.take(10).forEach {
                        appendLine("  [${it.moduleId}] V${it.version}  ${it.description}")
                    }
                }

                else -> null // OK
            }

            // STATUS 命令不应返回 Up / Verify, 安全起见兜底
            else -> "migration precheck unexpected result type: $result"
        }
    }
}
