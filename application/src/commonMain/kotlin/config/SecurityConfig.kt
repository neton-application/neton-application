package config

import neton.core.component.NetonContext
import neton.core.config.NetonConfig
import neton.core.config.NetonConfigurer
import neton.core.interfaces.SecurityBuilder
import neton.security.jwt.DispatchingJwtAuthenticator
import neton.security.jwt.JwtAuthenticator
import neton.security.jwt.UnifiedTokenAuthenticator
import security.CoreAuthenticatorBridge

/**
 * 通用 application HTTP 鉴权骨架（spec TOKEN_UNIFICATION_SPEC v1.3）。
 *
 * # 边界（CONSOL Step 2）
 *
 * 本 configurer 是 **通用 shell**，只认识框架类型：legacy HS256 [JwtAuthenticator] +
 * [DispatchingJwtAuthenticator] + 可选的 [UnifiedTokenAuthenticator]。
 * **不 import PrivchatServiceClient、不 hardcode privchat-server**。
 *
 * RS256/JWKS unified verifier 由 `module-privchat` 的 `PrivchatSecurityConfig`
 * （`order = -10`，先于本 configurer）构造并 bind 到 ctx；本 configurer 只 `getOrNull`
 * 取出框架类型 [UnifiedTokenAuthenticator] 组进 dispatcher。privchat 不加载 / 未 enable
 * 时 unified=null，dispatcher 退化为纯 legacy。
 *
 * # 默认行为
 *
 * `security.unified_token.enabled=false`（默认）→ unified=null，纯 legacy HS256，行为不变。
 */
@NetonConfig("security", order = 0)
class SecurityConfig : NetonConfigurer<SecurityBuilder> {
    override fun configure(ctx: NetonContext, target: SecurityBuilder) {
        val jwtConfig = loadJwtRuntimeConfig(ctx)
        val unifiedConfig = loadUnifiedTokenRuntimeConfig(ctx)

        // 1) Legacy HS256 verifier（始终构造）
        val legacy: JwtAuthenticator = JwtAuthenticator(
            secretKey = jwtConfig.secretKey,
            headerName = jwtConfig.headerName,
            tokenPrefix = jwtConfig.tokenPrefix,
        )

        // 2) Unified verifier —— 由 module-privchat PrivchatSecurityConfig (order=-10) 构造并 bind。
        //    shell 只取框架类型，不认识它来自 privchat。
        val unified: UnifiedTokenAuthenticator? = ctx.getOrNull(UnifiedTokenAuthenticator::class)
        if (unifiedConfig.enabled && unified == null) {
            error(
                "security.unified_token.enabled=true 但 UnifiedTokenAuthenticator 未绑定到 NetonContext。" +
                    "应由 module-privchat 的 PrivchatSecurityConfig 在 SecurityComponent.start (order=-10) " +
                    "构造并 bind；检查 privchat 模块是否在 neton.modules 中、configurer order 是否正确。",
            )
        }

        // 3) Dispatcher 包住两者；enabled=false 时 unified=null → 行为等价于纯 legacy
        val dispatcher = DispatchingJwtAuthenticator(
            legacy = legacy,
            unified = unified,
            unifiedEnabled = unifiedConfig.enabled,
            verifyLegacyJwt = unifiedConfig.verifyLegacyJwt,
            headerName = jwtConfig.headerName,
            tokenPrefix = jwtConfig.tokenPrefix,
        )
        target.registerAuthenticator(CoreAuthenticatorBridge(dispatcher))

        target.bindDefaultGuard()
        // super_admin 角色拥有所有权限
        target.setPermissionEvaluator { identity, permission, _ ->
            identity.hasRole("super_admin") || identity.hasPermission(permission)
        }
    }
}
