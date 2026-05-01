package security

import neton.core.interfaces.Identity
import neton.security.Authenticator as SecurityAuthenticator
import neton.core.interfaces.Authenticator as CoreAuthenticator
import neton.core.interfaces.RequestContext as CoreRequestContext
import neton.security.RequestContext as SecurityRequestContext
import neton.security.identity.AuthenticationException

/**
 * Bridge：把一个 [neton.security.Authenticator] 包成 [neton.core.interfaces.Authenticator]。
 *
 * 镜像 [neton.security.SecurityModule] 里的 `JwtAuthenticatorAdapter` 模式（spec
 * Phase A 注释说明：core 与 security 是两个独立的 RequestContext / Authenticator
 * 接口体系，现网由 adapter 在 SecurityBuilder 注册口做适配）。
 *
 * 功能：
 * 1. 把 core RequestContext 映射成 security RequestContext（path / method / headers /
 *    routeGroup 直接转；query / body / session / remote 在 application HTTP 通道上
 *    用不到，统一返 null/empty）
 * 2. 捕获 [AuthenticationException]（legacy `JwtAuthenticator` 在 token 错误时抛
 *    这种异常）→ 返回 null（按 [Authenticator] 契约：凭据无效用 null 表达）
 *
 * Phase A 默认行为下，本 adapter 包的是 [neton.security.jwt.DispatchingJwtAuthenticator]，
 * 后者本身永远不抛 [AuthenticationException]（dispatcher 只 init 时抛
 * IllegalArgumentException 表示 misconfiguration）；catch 这条仍然保留以兼容下层
 * legacy `JwtAuthenticator` 的"token 错抛异常"老行为。
 */
public class CoreAuthenticatorBridge(
    private val delegate: SecurityAuthenticator,
) : CoreAuthenticator {

    override val name: String = delegate.name

    override suspend fun authenticate(context: CoreRequestContext): Identity? {
        val sec = object : SecurityRequestContext {
            override val path = context.path
            override val method = context.method
            override val headers = context.headers
            override val routeGroup = context.routeGroup
            override fun getQueryParameter(name: String): String? = null
            override fun getQueryParameters(): Map<String, List<String>> = emptyMap()
            override suspend fun getBodyAsString(): String? = null
            override fun getSessionId(): String? = null
            override fun getRemoteAddress(): String? = null
        }
        return try {
            delegate.authenticate(sec)
        } catch (_: AuthenticationException) {
            null
        }
    }
}
