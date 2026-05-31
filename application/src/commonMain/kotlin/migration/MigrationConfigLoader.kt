package migration

import neton.core.config.ConfigLoader
import neton.core.module.MigrationDialect
import neton.database.config.DatabaseConfig
import neton.database.config.DatabaseDriver
import neton.database.migration.MigrationConfig
import neton.database.migration.fromDriver

/**
 * 从 `config/database.conf` 解析 application migration 配置:
 *   - `[default].driver` / `[default].uri` → [DatabaseConfig]
 *   - `[migration].history_table`(可选,缺省 [MigrationConfig.DEFAULT_HISTORY_TABLE])
 *
 * 解析失败返回 [Result.Failure] 而不是抛异常,caller 决定 exit code(SPEC §5.4)。
 */
internal object MigrationConfigLoader {

    sealed class Result {
        data class Ok(
            val database: DatabaseConfig,
            val migration: MigrationConfig,
        ) : Result()

        data class Failure(val message: String) : Result()
    }

    fun load(args: Array<String>): Result {
        val env = ConfigLoader.resolveEnvironment(args)
        val dbConf = ConfigLoader.loadModuleConfig(
            moduleName = "database",
            configPath = "config",
            environment = env,
            args = args,
        ) ?: return Result.Failure("missing config/database.conf")

        @Suppress("UNCHECKED_CAST")
        val defaultSection = dbConf["default"] as? Map<String, Any?>
            ?: return Result.Failure("config/database.conf: missing [default] section")

        val driverStr = defaultSection["driver"] as? String
            ?: return Result.Failure("config/database.conf: [default].driver required")
        val uri = defaultSection["uri"] as? String
            ?: return Result.Failure("config/database.conf: [default].uri required")

        val driver = try {
            DatabaseDriver.valueOf(driverStr.uppercase())
        } catch (e: Exception) {
            return Result.Failure(
                "config/database.conf: invalid driver '$driverStr' " +
                    "(expected: POSTGRESQL | MYSQL | SQLITE | MEMORY)"
            )
        }

        @Suppress("UNCHECKED_CAST")
        val migSection = dbConf["migration"] as? Map<String, Any?>
        val historyTable = (migSection?.get("history_table") as? String)
            ?: MigrationConfig.DEFAULT_HISTORY_TABLE

        return Result.Ok(
            database = DatabaseConfig(driver = driver, uri = uri),
            migration = MigrationConfig(
                dialect = MigrationDialect.fromDriver(driver),
                historyTable = historyTable,
            ),
        )
    }
}
