package logic.provider

import neton.logging.Logger

/**
 * Email message provider. In v1, logs the email content instead of
 * actually sending via SMTP or HTTP email API.
 */
class EmailProvider(
    private val log: Logger
) : MessageProvider {

    override val type: String = "email"

    override suspend fun send(receiver: String, content: String, config: String): Boolean {
        // v1: Log the email content. In production, parse `config` JSON for
        // SMTP settings or HTTP API credentials (e.g., SendGrid, AWS SES).
        log.info("Email to $receiver: $content")
        // TODO: Parse config JSON and send via SMTP/HTTP API
        return true
    }
}
