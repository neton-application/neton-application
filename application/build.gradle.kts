plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

repositories {
    mavenCentral()
}

kotlin {
    listOf(macosArm64(), linuxX64(), linuxArm64(), mingwX64()).forEach { target ->
        target.binaries {
            executable {
                entryPoint = "main"
            }
        }
        val coreInterop = rootProject.file("../neton/neton-core/build/nativeInterop/${target.name}").absolutePath
        target.binaries.forEach { binary ->
            binary.linkerOpts.add("-L$coreInterop")
            binary.linkerOpts.add("-lenv")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                // 核心模块
                implementation(project(":module-system"))
                implementation(project(":module-infra"))

                // 独立扩展模块（按需注释/取消注释，通过 composite build 解析）
                implementation("com.netonstream.app:module-member")
                implementation("com.netonstream.app:module-payment")
                implementation("com.netonstream.app:module-platform")
                implementation("com.netonstream.app:module-game")
                implementation("com.netonstream.app:module-assistant")
                implementation("com.netonstream.privchat:main")

                // 框架依赖
                implementation("com.netonstream:neton-core")
                implementation("com.netonstream:neton-routing")
                implementation("com.netonstream:neton-security")
                implementation("com.netonstream:neton-http")
                implementation("com.netonstream:neton-database")
                implementation("com.netonstream:neton-logging")
                implementation("com.netonstream:neton-validation")
                implementation("com.netonstream:neton-redis")
                implementation("com.netonstream:neton-cache")
                implementation("com.netonstream:neton-storage")
                implementation("com.netonstream:neton-jobs")
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

dependencies {
    add("kspMacosArm64", "com.netonstream:neton-ksp")
    add("kspLinuxX64", "com.netonstream:neton-ksp")
    add("kspLinuxArm64", "com.netonstream:neton-ksp")
    add("kspMingwX64", "com.netonstream:neton-ksp")
}

// Ensure Kotlin compilation sees KSP-generated commonMain sources.
// Required because application Main references generated GeneratedNetonConfigRegistry —
// K2 metadata compilation needs it at the commonMain level.
afterEvaluate {
    val kspOut = file("build/generated/ksp/macosArm64/macosArm64Main/kotlin")
    kotlin.sourceSets.named("commonMain") {
        kotlin.srcDir(kspOut)
    }
    listOf("macosArm64Main", "linuxX64Main", "linuxArm64Main", "mingwX64Main").forEach { name ->
        kotlin.sourceSets.findByName(name)?.let { ss ->
            val filtered = ss.kotlin.srcDirs.filter { !it.path.contains("generated/ksp") }
            if (filtered.size < ss.kotlin.srcDirs.size) ss.kotlin.setSrcDirs(filtered)
        }
    }
}

tasks.matching { it.name == "compileCommonMainKotlinMetadata" }.configureEach {
    dependsOn("kspKotlinMacosArm64")
}
tasks.matching { it.name.matches(Regex("compileKotlin(MacosArm64|LinuxX64|LinuxArm64|MingwX64)")) }.configureEach {
    dependsOn("kspKotlinMacosArm64")
}
tasks.matching { it.name.matches(Regex("kspKotlin(LinuxX64|LinuxArm64|MingwX64)")) }.configureEach {
    dependsOn("kspKotlinMacosArm64")
}

tasks.matching { it.name.startsWith("linkDebugExecutable") }.configureEach {
    val targetName = when {
        name.contains("MacosArm64") -> "MacosArm64"
        name.contains("LinuxX64") -> "LinuxX64"
        name.contains("LinuxArm64") -> "LinuxArm64"
        name.contains("MingwX64") -> "MingwX64"
        else -> return@configureEach
    }
    dependsOn(gradle.includedBuild("neton").task(":neton-core:archivePosixEnv$targetName"))
}

// ============================================================
// copyModuleMigrations — build-time packaging only.
//
// Runtime migration sources 由各 ModuleInitializer.migrations() 声明,**不**走这里。
// 这里只把仓库里各模块的 sql/<dialect>/V*.sql 拷贝到 application 运行目录的
// `migrations/<moduleId>/<dialect>/`,让 application.kexe migrate 子命令在
// 当前 cwd 下能读到对应文件(SPEC §0.3 模块自管 SQL,application 不维护
// 运行期映射表)。
//
// 拍板 (2026-06-01 DB-MIG-7A):
//   - infra 短期持有 system+infra 合并 SQL (privchat-application/sql/postgresql)
//   - system 模块 migrations() = emptyList(),DB-MIG-7B 再拆
// ============================================================
val moduleMigrations: List<Pair<String, String>> = listOf(
    "infra"    to "../sql/postgresql",
    "member"   to "../../privchat-application-module-member/sql/postgresql",
    "payment"  to "../../privchat-application-module-payment/sql/postgresql",
    "platform" to "../../privchat-application-module-platform/sql/postgresql",
    "game"     to "../../privchat-application-module-game/sql/postgresql",
    "privchat" to "../../neton-application-module-privchat/sql/postgresql",
)

val copyModuleMigrations by tasks.registering(Copy::class) {
    group = "neton-migration"
    description = "Aggregate per-module sql/<dialect>/V*.sql into ./migrations/<moduleId>/<dialect>/ " +
        "for application.kexe migrate to scan at runtime."
    val outDir = file("migrations")
    outputs.dir(outDir)
    moduleMigrations.forEach { (moduleId, src) ->
        from(src) {
            include("V*.sql")
            into("$moduleId/postgresql")
        }
    }
    into(outDir)
    doFirst { outDir.deleteRecursively() }
}
