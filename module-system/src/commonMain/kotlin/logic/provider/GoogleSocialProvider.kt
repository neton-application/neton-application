package logic.provider

import neton.logging.Logger

/**
 * Google OAuth2 social login provider.
 * In v1, provides the structure but returns placeholder data.
 * In production, parse config JSON for client_id/client_secret and
 * call Google OAuth2 APIs.
 */
class GoogleSocialProvider(
    private val log: Logger
) : SocialProvider {

    override val type: String = "google"

    override suspend fun getAuthRedirectUrl(config: String, redirectUri: String): String {
        // TODO: Parse config for client_id, build Google OAuth2 authorize URL
        // https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=...&response_type=code&scope=openid+profile+email
        log.info("social.google.redirect.requested")
        return "https://accounts.google.com/o/oauth2/v2/auth?redirect_uri=$redirectUri&response_type=code&scope=openid+profile+email"
    }

    override suspend fun getUserInfo(config: String, code: String, redirectUri: String): SocialUserInfo {
        // TODO: Exchange code for token via https://oauth2.googleapis.com/token
        // Then fetch user info from https://www.googleapis.com/oauth2/v3/userinfo
        log.info("social.google.code_exchange.requested")
        return SocialUserInfo(
            openId = "",
            nickname = null,
            avatar = null,
            rawUserInfo = null,
            rawTokenInfo = null
        )
    }
}
