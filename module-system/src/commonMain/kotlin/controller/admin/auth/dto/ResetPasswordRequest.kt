package controller.admin.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class ResetPasswordRequest(
    val mobile: String,
    val smsCode: String,
    val newPassword: String
)
