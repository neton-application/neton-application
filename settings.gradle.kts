pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.name = "neton-application"

// 框架依赖（composite build）
includeBuild("../neton")
includeBuild("../geolite4k")

// Canonical sibling repositories are assembled as source subprojects here.
// Each repository keeps its own settings.gradle.kts and remains independently buildable.
// application 仅做装配 —— 所有模块（含 system/infra 核心）都是独立 canonical 仓。
include(":module-system")
project(":module-system").projectDir = file("../neton-application-module-system")
include(":module-infra")
project(":module-infra").projectDir = file("../neton-application-module-infra")
include(":module-member")
project(":module-member").projectDir = file("../neton-application-module-member")
include(":module-payment")
project(":module-payment").projectDir = file("../neton-application-module-payment")
include(":module-platform")
project(":module-platform").projectDir = file("../neton-application-module-platform")

// 应用入口
include(":application")
