package logic

import model.MessageChannel
import model.MessageLog
import table.MessageChannelTable
import table.MessageLogTable
import neton.core.http.NotFoundException
import neton.logging.Logger
import neton.database.dsl.*

import neton.redis.RedisClient
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class MessageSendLogic(
    private val log: Logger,
    private val messageChannelLogic: MessageChannelLogic,
    private val messageTemplateLogic: MessageTemplateLogic,
    private val redis: RedisClient? = null
) {

    companion object {
        private const val SMS_CODE_PREFIX = "sms:code:"
        private const val SMS_CODE_TTL_SECONDS = 300L  // 5 minutes
    }

    private fun maskMobile(mobile: String): String {
        return if (mobile.length < 7) "***" else "${mobile.take(3)}****${mobile.takeLast(4)}"
    }

    /**
     * Send a message using a template code and parameters.
     * Flow: lookup template → lookup channel → render content → send via provider → log result
     */
    suspend fun sendByTemplate(
        templateCode: String,
        receiver: String,
        params: Map<String, String>,
        userId: Long? = null,
        userType: Int = 0
    ): Boolean {
        val template = messageTemplateLogic.getByCode(templateCode)
            ?: throw NotFoundException("Message template not found: $templateCode")

        val channel = MessageChannelTable.get(template.channelId)
            ?: throw NotFoundException("Message channel not found for template: $templateCode")

        val content = messageTemplateLogic.renderContent(template.content, params)

        val provider = messageChannelLogic.getProvider(channel.type)
            ?: throw NotFoundException("No provider for channel type: ${channel.type}")

        val success = try {
            provider.send(receiver, content, channel.config ?: "{}")
        } catch (e: Exception) {
            log.error("Failed to send message via ${channel.type}: ${e.message}")
            // Log failure
            logMessage(channel.id, template.id, templateCode, receiver, content,
                params.toString(), 1, e.message, userId, userType)
            return false
        }

        // Log success
        logMessage(channel.id, template.id, templateCode, receiver, content,
            params.toString(), if (success) 0 else 1, null, userId, userType)

        return success
    }

    /**
     * Send a verification code via SMS. Generates a 6-digit code,
     * stores it in Redis with TTL, and sends via the SMS channel.
     */
    suspend fun sendVerificationCode(mobile: String, scene: String = "login"): Boolean {
        val code = Random.nextInt(100000, 999999).toString()

        // Store code in Redis
        if (redis != null) {
            redis.set("$SMS_CODE_PREFIX$mobile", code, SMS_CODE_TTL_SECONDS.seconds)
            log.info("sms.code.generated", mapOf("mobile" to maskMobile(mobile), "scene" to scene))
        } else {
            log.warn("sms.code.redis_unavailable", mapOf("mobile" to maskMobile(mobile), "scene" to scene))
        }

        // Try to send via template if configured
        try {
            val template = messageTemplateLogic.getByCode("sms_verification_$scene")
            if (template != null) {
                return sendByTemplate(
                    templateCode = "sms_verification_$scene",
                    receiver = mobile,
                    params = mapOf("code" to code)
                )
            }
        } catch (e: Exception) {
            log.warn("No SMS template configured for scene=$scene, code stored in Redis only")
        }

        return true
    }

    /**
     * Verify a SMS code from Redis.
     */
    suspend fun verifySmsCode(mobile: String, code: String): Boolean {
        if (redis == null) {
            log.warn("Redis not available, cannot verify SMS code")
            return false
        }
        val storedCode = redis.getValue("$SMS_CODE_PREFIX$mobile")
        if (storedCode == null || storedCode != code) {
            return false
        }
        // Delete code after successful verification
        redis.delete("$SMS_CODE_PREFIX$mobile")
        return true
    }

    private suspend fun logMessage(
        channelId: Long,
        templateId: Long?,
        templateCode: String?,
        receiver: String,
        content: String?,
        params: String?,
        sendStatus: Int,
        errorMessage: String?,
        userId: Long?,
        userType: Int
    ) {
        val messageLog = MessageLog(
            channelId = channelId,
            templateId = templateId,
            templateCode = templateCode,
            receiver = receiver,
            content = content,
            params = params,
            sendStatus = sendStatus,
            sendTime = Clock.System.now().toString(),
            errorMessage = errorMessage,
            userId = userId,
            userType = userType
        )
        try {
            MessageLogTable.insert(messageLog)
        } catch (e: Exception) {
            log.error("Failed to log message: ${e.message}")
        }
    }
}
