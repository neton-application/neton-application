package security

import com.netonstream.privchat.application.module.privchat.client.PrivchatServiceClient
import neton.security.jwt.JwkRsaMaterial
import neton.security.jwt.JwksFetcher

/**
 * 把 [PrivchatServiceClient.fetchJwks] 适配成 [JwksFetcher]
 * （spec TOKEN_UNIFICATION_SPEC v1.3 §6.1 / §11.1）。
 *
 * client 已负责：
 * - HTTP（GET `/api/service/auth/jwks`，公开端点）
 * - JSON 解码（`{ "keys": [...] }`）
 * - 503 / 解码失败的异常映射（[com.netonstream.privchat.application.module.privchat.client.error.PrivchatServiceException]）
 *
 * 本类仅做字段映射：JWKS DTO → [JwkRsaMaterial]。
 *
 * **fail-fast 契约**：任何异常往上抛，[neton.security.jwt.JwksHttpProvider] 收到后会
 * 保持既有 cache 不破坏（首次启动如果 fetch 失败，整个 provider 没有 cache，
 * 后续 unified token 验签都会因 UnknownKid 被拒，这是预期行为）。
 */
public class JwksHttpFetcher(
    private val client: PrivchatServiceClient,
) : JwksFetcher {

    override suspend fun fetch(): List<JwkRsaMaterial> {
        val jwks = client.fetchJwks()
        return jwks.keys.map { k ->
            JwkRsaMaterial(
                kid = k.kid,
                n = k.n,
                e = k.e,
                alg = k.alg,
                kty = k.kty,
                use = k.use_,
            )
        }
    }
}
