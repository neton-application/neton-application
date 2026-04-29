pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.name = "privchat-application"

// 框架依赖（composite build）
includeBuild("../neton")

// 独立扩展模块（按需引入，注释即可去掉）
includeBuild("../privchat-application-module-member")
includeBuild("../privchat-application-module-payment")
includeBuild("../privchat-application-module-platform")

// PrivChat 官方开发的 Neton Application 独立模块（无 Neton 上游）
// 详见 spec/07-application/MODULE_PRIVCHAT_SPEC.md
includeBuild("../neton-application-module-privchat")

// 核心模块
include(":module-system")
include(":module-infra")

// 应用入口
include(":application")
