package logic.provider

import neton.logging.Logger

/**
 * Telegram social login provider (v2 placeholder).
 * Telegram uses a widget-based login, not a standard OAuth2 flow.
 */
class TelegramSocialProvider(
    private val log: Logger
) : SocialProvider {

    override val type: String = "telegram"

    override suspend fun getAuthRedirectUrl(config: String, redirectUri: String): String {
        // Telegram login widget doesn't use a standard redirect URL.
        // In v2, return the widget configuration parameters.
        log.info("Telegram social login redirect requested")
        return ""
    }

    override suspend fun getUserInfo(config: String, code: String, redirectUri: String): SocialUserInfo {
        // TODO: Validate Telegram login widget hash and extract user info
        log.info("Telegram social login code exchange requested")
        return SocialUserInfo(
            openId = "",
            nickname = null,
            avatar = null,
            rawUserInfo = null,
            rawTokenInfo = null
        )
    }
}
