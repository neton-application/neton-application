package security

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import neton.redis.RedisClient
import neton.security.jwt.SessionVersionCache

/**
 * Redis 实现的设备级 `session_version` cache（spec TOKEN_UNIFICATION_SPEC v1.3 §7.4）。
 *
 * Key 格式：`session:{uid}:{device_id}:version`，value = 版本号字符串。
 *
 * **错误处理**：Redis 失败时直接抛异常往上传，由 [neton.security.jwt.UnifiedTokenAuthenticator]
 * 捕获并 fallback 到 introspect（spec §7.4 拍板第 8 条："Redis 不可用 → 绕过 cache，直接 introspect"）。
 * 不在本 adapter 里吞异常 —— 让 caller 的"cache miss / 异常 → introspect"统一逻辑处理。
 *
 * **TTL**：phase A 默认 30s（[neton.security.jwt.UnifiedTokenAuthenticator] 接收 ttlSeconds 参数）。
 */
public class RedisSessionVersionCache(
    private val redis: RedisClient,
) : SessionVersionCache {

    override suspend fun get(uid: Long, deviceId: String): Long? {
        return redis.getValue(keyOf(uid, deviceId))?.toLongOrNull()
    }

    override suspend fun set(
        uid: Long,
        deviceId: String,
        version: Long,
        ttlSeconds: Long,
    ) {
        val ttl: Duration = ttlSeconds.seconds
        redis.set(keyOf(uid, deviceId), version.toString(), ttl)
    }

    private fun keyOf(uid: Long, deviceId: String): String =
        "session:$uid:$deviceId:version"
}
