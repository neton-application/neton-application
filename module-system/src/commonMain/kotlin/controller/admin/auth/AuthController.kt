package controller.admin.auth

import controller.admin.auth.dto.LoginRequest
import controller.admin.auth.dto.LoginResponse
import controller.admin.auth.dto.PermissionInfoVO
import controller.admin.auth.dto.SmsLoginRequest
import controller.admin.auth.dto.SendSmsCodeRequest
import controller.admin.auth.dto.ResetPasswordRequest
import controller.admin.auth.dto.SocialLoginRequest
import controller.admin.auth.dto.SocialRedirectVO
import logic.AuthLogic
import logic.PermissionLogic
import neton.core.annotations.*
import neton.core.interfaces.Identity

@Controller("/system/auth")
class AuthController(
    private val authLogic: AuthLogic,
    private val permissionLogic: PermissionLogic
) {

    @Post("/login")
    @AllowAnonymous
    suspend fun login(@Body request: LoginRequest): LoginResponse {
        return authLogic.login(request)
    }

    @Post("/logout")
    suspend fun logout(identity: Identity) {
        val userId = identity.id.toLong()
        authLogic.logout(userId)
    }

    @Post("/refresh-token")
    @AllowAnonymous
    suspend fun refreshToken(@Query refreshToken: String): LoginResponse {
        return authLogic.refreshToken(refreshToken)
    }

    @Get("/get-permission-info")
    suspend fun getPermissionInfo(identity: Identity): PermissionInfoVO {
        val userId = identity.id.toLong()
        return permissionLogic.getPermissionInfo(userId)
    }

    @Post("/sms-login")
    @AllowAnonymous
    suspend fun smsLogin(@Body request: SmsLoginRequest): LoginResponse {
        return authLogic.smsLogin(request.mobile, request.smsCode)
    }

    @Post("/send-sms-code")
    @AllowAnonymous
    suspend fun sendSmsCode(@Body request: SendSmsCodeRequest) {
        authLogic.sendSmsCode(request.mobile, request.scene)
    }

    @Post("/reset-password")
    @AllowAnonymous
    suspend fun resetPassword(@Body request: ResetPasswordRequest) {
        authLogic.resetPassword(request.mobile, request.smsCode, request.newPassword)
    }

    @Get("/social-auth-redirect")
    @AllowAnonymous
    suspend fun socialAuthRedirect(type: String, redirectUri: String? = null): SocialRedirectVO {
        val url = authLogic.socialAuthRedirect(type, redirectUri ?: "")
        return SocialRedirectVO(url = url)
    }

    @Post("/social-login")
    @AllowAnonymous
    suspend fun socialLogin(@Body request: SocialLoginRequest): LoginResponse {
        return authLogic.socialLogin(request.socialType, request.code, request.redirectUri ?: "")
    }
}
