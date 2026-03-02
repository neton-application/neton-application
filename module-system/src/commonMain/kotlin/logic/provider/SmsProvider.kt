package logic.provider

import neton.logging.Logger

/**
 * SMS message provider. In v1, logs the message content instead of calling
 * an actual SMS API. The channel config would contain API credentials for
 * providers like Aliyun SMS, Twilio, etc.
 */
class SmsProvider(
    private val log: Logger
) : MessageProvider {

    override val type: String = "sms"

    override suspend fun send(receiver: String, content: String, config: String): Boolean {
        // v1: Log the SMS content. In production, parse `config` JSON for
        // API credentials and call the SMS API (e.g., Aliyun SMS, Twilio).
        log.info("SMS to $receiver: $content")
        // TODO: Parse config JSON and call actual SMS HTTP API
        return true
    }
}
