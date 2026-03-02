package controller.admin.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)
