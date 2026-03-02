package controller.admin.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendSmsCodeRequest(
    val mobile: String,
    val scene: String = "login"  // login, reset-password, etc.
)
