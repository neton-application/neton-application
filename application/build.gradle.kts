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

// KSP 生成代码加入各平台 sourceSet
for (targetName in listOf("MacosArm64", "LinuxX64", "LinuxArm64", "MingwX64")) {
    val lower = targetName.replaceFirstChar { it.lowercase() }
    kotlin.sourceSets.named("${lower}Main") {
        kotlin.srcDir("build/generated/ksp/$lower/${lower}Main/kotlin")
    }
}

// compile 依赖对应平台的 KSP 生成
tasks.matching { it.name.matches(Regex("compileKotlin(MacosArm64|LinuxX64|LinuxArm64|MingwX64)")) }.configureEach {
    val targetName = name.removePrefix("compileKotlin")
    dependsOn("kspKotlin$targetName")
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
