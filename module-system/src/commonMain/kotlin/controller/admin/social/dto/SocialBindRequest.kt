package controller.admin.social.dto

import kotlinx.serialization.Serializable

@Serializable
data class SocialBindRequest(
    val socialType: String,
    val code: String,
    val redirectUri: String? = null
)
