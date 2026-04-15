package controller.admin.auth.dto

import kotlinx.serialization.Serializable
import neton.core.annotations.NotBlank
import neton.core.annotations.Size

@Serializable
data class RefreshTokenRequest(
    @property:NotBlank
    @property:Size(min = 32, max = 4096)
    val refreshToken: String
)
