package config

import neton.core.component.NetonContext
import neton.core.config.ConfigLoader
import neton.security.jwt.JwtAuthenticatorV1

private const val APP_CONFIG_PATH = "config"
private const val JWT_SECRET_PATH = "security.jwt.secretKey"
private const val JWT_HEADER_NAME_PATH = "security.jwt.headerName"
private const val JWT_TOKEN_PREFIX_PATH = "security.jwt.tokenPrefix"

data class JwtRuntimeConfig(
    val secretKey: String,
    val headerName: String,
    val tokenPrefix: String
)

fun loadJwtRuntimeConfig(ctx: NetonContext): JwtRuntimeConfig {
    val appConfig = ConfigLoader.loadApplicationConfig(
        configPath = APP_CONFIG_PATH,
        environment = ConfigLoader.resolveEnvironment(ctx.args),
        args = ctx.args
    )

    val secretKey = ConfigLoader.getString(appConfig, JWT_SECRET_PATH)?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: throw IllegalStateException(
            "Missing JWT secret. Configure security.jwt.secretKey or NETON__SECURITY__JWT__SECRET_KEY."
        )

    val headerName = ConfigLoader.getString(appConfig, JWT_HEADER_NAME_PATH)?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "Authorization"

    val tokenPrefix = ConfigLoader.getString(appConfig, JWT_TOKEN_PREFIX_PATH)?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "Bearer "

    return JwtRuntimeConfig(
        secretKey = secretKey,
        headerName = headerName,
        tokenPrefix = tokenPrefix
    )
}

fun buildJwtAuthenticator(ctx: NetonContext): JwtAuthenticatorV1 {
    val runtimeConfig = loadJwtRuntimeConfig(ctx)
    return JwtAuthenticatorV1(
        secretKey = runtimeConfig.secretKey,
        headerName = runtimeConfig.headerName,
        tokenPrefix = runtimeConfig.tokenPrefix
    )
}
