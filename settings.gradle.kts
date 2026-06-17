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

// 通用 base distribution 默认模块（system/infra in-tree + member/payment/platform）。
// R5-B: game/assistant 属 PrivChat 产品模块、module-privchat 属可选 IM 模块，
// 均移出通用 base，由 privchat-application 产品发行版 fork 自行 include。
includeBuild("../privchat-application-module-member")
includeBuild("../privchat-application-module-payment")
includeBuild("../privchat-application-module-platform")

// 核心模块
include(":module-system")
include(":module-infra")

// 应用入口
include(":application")
