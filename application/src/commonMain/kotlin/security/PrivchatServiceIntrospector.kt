package security

import com.netonstream.privchat.application.module.privchat.client.PrivchatServiceClient
import neton.security.jwt.IntrospectionResult
import neton.security.jwt.UnifiedTokenIntrospector

/**
 * 把 [PrivchatServiceClient.introspectAuthToken] 适配成 [UnifiedTokenIntrospector]
 * （spec TOKEN_UNIFICATION_SPEC v1.3 §6.1）。
 *
 * Phase A `enabled=false` 默认 dormant 路径下本类不会被调用；当 ops 切 enabled=true
 * 后，[neton.security.jwt.UnifiedTokenAuthenticator] 在 cache miss / mismatch 时
 * 调本类做权威 introspect。
 *
 * **fail-closed 契约**：任何异常往上抛，由 [neton.security.jwt.UnifiedTokenAuthenticator]
 * 捕获映射成 [neton.security.jwt.AuthFailureReason.IntrospectFailed] →
 * [neton.security.jwt.AuthOutcome.Failure] → 框架 401。
 */
public class PrivchatServiceIntrospector(
    private val client: PrivchatServiceClient,
) : UnifiedTokenIntrospector {

    override suspend fun introspect(token: String): IntrospectionResult {
        val r = client.introspectAuthToken(token)
        return IntrospectionResult(
            active = r.active,
            userId = r.userId,
            deviceId = r.deviceId,
            sessionVersion = r.sessionVersion,
            scope = r.scope,
            audience = r.audience,
            expiresAt = r.expiresAt,
            jti = r.jti,
            reason = r.reason,
        )
    }
}
