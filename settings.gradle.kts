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

// 核心模块
include(":module-system")
include(":module-infra")

// Canonical sibling repositories are assembled as source subprojects here.
// Each repository keeps its own settings.gradle.kts and remains independently buildable.
include(":module-member")
project(":module-member").projectDir = file("../neton-application-module-member")
include(":module-payment")
project(":module-payment").projectDir = file("../neton-application-module-payment")
include(":module-platform")
project(":module-platform").projectDir = file("../neton-application-module-platform")

// 应用入口
include(":application")
