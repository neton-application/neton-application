plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

subprojects {
    group = "com.netonstream.app"
    version = "1.0.0"

    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("com.netonstream.app:module-system")).using(project(":module-system"))
            substitute(module("com.netonstream.app:module-infra")).using(project(":module-infra"))
            substitute(module("com.netonstream.app:module-member")).using(project(":module-member"))
            substitute(module("com.netonstream.app:module-payment")).using(project(":module-payment"))
            substitute(module("com.netonstream.app:module-platform")).using(project(":module-platform"))
            substitute(module("com.netonstream.app:module-cs")).using(project(":module-cs"))
        }
    }
}
