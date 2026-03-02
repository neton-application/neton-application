package config

import neton.core.component.NetonContext
import neton.core.config.NetonConfig
import neton.core.config.NetonConfigurer
import neton.core.interfaces.SecurityBuilder

const val JWT_SECRET = "neton-backend-v1-secret-key-change-in-production"

@NetonConfig("security", order = 0)
class SecurityConfig : NetonConfigurer<SecurityBuilder> {
    override fun configure(ctx: NetonContext, target: SecurityBuilder) {
        target.registerJwtAuthenticator(
            secretKey = JWT_SECRET,
            headerName = "Authorization",
            tokenPrefix = "Bearer "
        )
        target.bindDefaultGuard()
        // super_admin 角色拥有所有权限
        target.setPermissionEvaluator { identity, permission, _ ->
            identity.hasRole("super_admin") || identity.hasPermission(permission)
        }
    }
}
