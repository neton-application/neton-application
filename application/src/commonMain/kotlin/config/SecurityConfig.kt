package config

import com.netonstream.privchat.application.module.privchat.client.PrivchatServiceClient
import neton.core.component.NetonContext
import neton.core.config.NetonConfig
import neton.core.config.NetonConfigurer
import neton.core.interfaces.SecurityBuilder
import neton.redis.RedisClient
import neton.security.jwt.DispatchingJwtAuthenticator
import neton.security.jwt.JwksHttpProvider
import neton.security.jwt.JwtAuthenticator
import neton.security.jwt.RsaJwtVerifier
import neton.security.jwt.SessionVersionCache
import neton.security.jwt.UnifiedTokenAuthenticator
import security.CoreAuthenticatorBridge
import security.JwksHttpFetcher
import security.PrivchatServiceIntrospector
import security.RedisSessionVersionCache

/**
 * 注册 application HTTP 鉴权认证器（spec TOKEN_UNIFICATION_SPEC v1.3 Phase A · §10）。
 *
 * **Phase A 默认行为**（`security.unified_token.enabled=false` + `verify_legacy_jwt=true`）：
 *
 * - dispatcher 内 unified 路径未构造（`unified=null`），不触发任何 unified 相关 IO
 *   （不预拉 JWKS、不查 introspect、不连 Redis 之 session cache）
 * - legacy `JwtAuthenticator` 直接处理所有请求；HS256 member token 行为不变
 * - 整个 application HTTP 与 Phase A 之前完全等价
 *
 * **切到 phase B**（ops 改 `enabled=true`）：
 *
 * 启动时会从 [NetonContext] 拉以下 binding（任一缺失即 fail-fast）：
 * - [PrivchatServiceClient]：用于 [JwksHttpFetcher] + [PrivchatServiceIntrospector]
 * - [RedisClient]：用于 [RedisSessionVersionCache]（session_version 30s 短缓存）
 *
 * 这些 binding 通常由 `PrivchatModuleInitializer` / Redis 模块在 init 阶段产生；
 * SecurityConfig `order=0` 较早执行，因此**只**在 enabled=true 时才尝试拉取，
 * 避免对其他模块的隐式启动顺序耦合。如果 ops 想 enable 但 lifecycle 顺序不对，
 * 会得到一个明确的 `IllegalStateException` 而不是运行成"所有请求 401"。
 */
@NetonConfig("security", order = 0)
class SecurityConfig : NetonConfigurer<SecurityBuilder> {
    override fun configure(ctx: NetonContext, target: SecurityBuilder) {
        val jwtConfig = loadJwtRuntimeConfig(ctx)
        val unifiedConfig = loadUnifiedTokenRuntimeConfig(ctx)

        // 1) Legacy HS256 verifier（始终构造，dispatcher 在 verifyLegacyJwt=true 时会调它）
        val legacy: JwtAuthenticator = JwtAuthenticator(
            secretKey = jwtConfig.secretKey,
            headerName = jwtConfig.headerName,
            tokenPrefix = jwtConfig.tokenPrefix,
        )

        // 2) Unified RS256 verifier（仅 enabled=true 时构造；默认 dormant 不触发）
        val unified: UnifiedTokenAuthenticator? = if (unifiedConfig.enabled) {
            val client = ctx.getOrNull(PrivchatServiceClient::class)
                ?: error(
                    "security.unified_token.enabled=true 需要 PrivchatServiceClient 已绑定到 NetonContext。" +
                        "通常由 PrivchatModuleInitializer 完成；检查模块加载顺序。",
                )
            val sessionCache: SessionVersionCache = run {
                val redis = ctx.getOrNull(RedisClient::class)
                    ?: error(
                        "security.unified_token.enabled=true 需要 RedisClient 已绑定到 NetonContext。" +
                            "确保 [cache.redis] 已配置且 redis 模块已加载。",
                    )
                RedisSessionVersionCache(redis)
            }
            val verifier = RsaJwtVerifier(
                keyProvider = JwksHttpProvider(
                    fetcher = JwksHttpFetcher(client),
                    cacheTtlMillis = unifiedConfig.jwksCacheTtlSeconds * 1000L,
                ),
            )
            UnifiedTokenAuthenticator(
                verifier = verifier,
                introspector = PrivchatServiceIntrospector(client),
                sessionVersionCache = sessionCache,
                sessionCacheTtlSeconds = unifiedConfig.sessionCacheTtlSeconds,
            )
        } else {
            null
        }

        // 3) Dispatcher 包住两者；默认 enabled=false + verifyLegacyJwt=true → 行为等价于纯 legacy
        val dispatcher = DispatchingJwtAuthenticator(
            legacy = legacy,
            unified = unified,
            unifiedEnabled = unifiedConfig.enabled,
            verifyLegacyJwt = unifiedConfig.verifyLegacyJwt,
            headerName = jwtConfig.headerName,
            tokenPrefix = jwtConfig.tokenPrefix,
        )
        // SecurityBuilder.registerAuthenticator 接收 neton.core.interfaces.Authenticator；
        // 我们的 dispatcher 实现的是 neton.security.Authenticator —— 通过 bridge 适配
        // （与 SecurityModule 内置的 JwtAuthenticatorAdapter 同模式）。
        target.registerAuthenticator(CoreAuthenticatorBridge(dispatcher))

        target.bindDefaultGuard()
        // super_admin 角色拥有所有权限
        target.setPermissionEvaluator { identity, permission, _ ->
            identity.hasRole("super_admin") || identity.hasPermission(permission)
        }
    }
}
