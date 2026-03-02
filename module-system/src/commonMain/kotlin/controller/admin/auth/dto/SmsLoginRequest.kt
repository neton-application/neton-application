package controller.admin.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class SmsLoginRequest(
    val mobile: String,
    val smsCode: String
)
