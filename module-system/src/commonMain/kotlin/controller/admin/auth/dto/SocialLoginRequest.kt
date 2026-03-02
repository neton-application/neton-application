package controller.admin.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginRequest(
    val socialType: String,  // "google", "telegram"
    val code: String,
    val redirectUri: String? = null
)
