package config

import neton.core.component.NetonContext
import neton.core.config.NetonConfig
import neton.core.config.NetonConfigurer
import neton.core.interfaces.SecurityBuilder

@NetonConfig("security", order = 0)
class SecurityConfig : NetonConfigurer<SecurityBuilder> {
    override fun configure(ctx: NetonContext, target: SecurityBuilder) {
        val jwtConfig = loadJwtRuntimeConfig(ctx)
        target.registerJwtAuthenticator(
            secretKey = jwtConfig.secretKey,
            headerName = jwtConfig.headerName,
            tokenPrefix = jwtConfig.tokenPrefix
        )
        target.bindDefaultGuard()
        // super_admin 角色拥有所有权限
        target.setPermissionEvaluator { identity, permission, _ ->
            identity.hasRole("super_admin") || identity.hasPermission(permission)
        }
    }
}
