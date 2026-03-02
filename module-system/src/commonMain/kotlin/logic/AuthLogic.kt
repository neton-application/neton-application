package logic

import controller.admin.auth.dto.LoginRequest
import controller.admin.auth.dto.LoginResponse
import model.User
import table.UserTable
import table.UserRoleTable
import table.RoleTable
import model.UserRole
import model.Role
import neton.security.jwt.JwtAuthenticatorV1
import neton.core.http.BadRequestException
import neton.core.http.NotFoundException
import neton.logging.Logger
import infra.PasswordEncoder
import neton.database.dsl.*

import kotlin.time.Clock
import neton.security.identity.UserId

class AuthLogic(
    private val log: Logger,
    private val jwt: JwtAuthenticatorV1,
    private val messageSendLogic: MessageSendLogic? = null,
    private val socialUserLogic: SocialUserLogic? = null
) {

    companion object {
        const val ACCESS_TOKEN_EXPIRES = 7200L   // 2 hours in seconds
        const val REFRESH_TOKEN_EXPIRES = 604800L // 7 days in seconds
    }

    suspend fun login(request: LoginRequest): LoginResponse {
        val user = UserTable.oneWhere {
            User::username eq request.username
        } ?: throw BadRequestException("Invalid username or password")

        if (user.status != 0) {
            throw BadRequestException("User account is disabled")
        }

        val passwordValid = verifyPassword(request.password, user.passwordHash)
        if (!passwordValid) {
            throw BadRequestException("Invalid username or password")
        }

        // Fetch user roles for the token
        val userRoles = UserRoleTable.query {
            where { UserRole::userId eq user.id }
        }.list()
        val roleIds = userRoles.map { it.roleId }
        val roles = if (roleIds.isNotEmpty()) {
            RoleTable.query {
                where { Role::id `in` roleIds }
            }.list().map { it.code }.toSet()
        } else emptySet()

        val accessToken = jwt.createToken(
            userId = UserId(user.id.toULong()),
            roles = roles,
            permissions = emptySet(),
            expiresInSeconds = ACCESS_TOKEN_EXPIRES,
            extraClaims = mapOf("type" to "access", "username" to user.username)
        )
        val refreshToken = jwt.createToken(
            userId = UserId(user.id.toULong()),
            roles = emptySet(),
            permissions = emptySet(),
            expiresInSeconds = REFRESH_TOKEN_EXPIRES,
            extraClaims = mapOf("type" to "refresh", "username" to user.username)
        )

        log.info("User logged in: ${user.username}")

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user.id,
            expiresTime = Clock.System.now().toEpochMilliseconds() + ACCESS_TOKEN_EXPIRES * 1000,
            username = user.username,
            nickname = user.nickname
        )
    }

    suspend fun logout(userId: Long) {
        log.info("User logged out: $userId")
    }

    suspend fun refreshToken(refreshToken: String): LoginResponse {
        val userId = parseTokenUserId(refreshToken)
            ?: throw BadRequestException("Invalid or expired refresh token")

        val user = UserTable.get(userId)
            ?: throw NotFoundException("User not found")

        if (user.status != 0) {
            throw BadRequestException("User account is disabled")
        }

        // Fetch user roles
        val userRoles = UserRoleTable.query {
            where { UserRole::userId eq user.id }
        }.list()
        val roleIds = userRoles.map { it.roleId }
        val roles = if (roleIds.isNotEmpty()) {
            RoleTable.query {
                where { Role::id `in` roleIds }
            }.list().map { it.code }.toSet()
        } else emptySet()

        val newAccessToken = jwt.createToken(
            userId = UserId(user.id.toULong()),
            roles = roles,
            permissions = emptySet(),
            expiresInSeconds = ACCESS_TOKEN_EXPIRES,
            extraClaims = mapOf("type" to "access", "username" to user.username)
        )
        val newRefreshToken = jwt.createToken(
            userId = UserId(user.id.toULong()),
            roles = emptySet(),
            permissions = emptySet(),
            expiresInSeconds = REFRESH_TOKEN_EXPIRES,
            extraClaims = mapOf("type" to "refresh", "username" to user.username)
        )

        return LoginResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            userId = user.id,
            expiresTime = Clock.System.now().toEpochMilliseconds() + ACCESS_TOKEN_EXPIRES * 1000,
            username = user.username,
            nickname = user.nickname
        )
    }

    suspend fun smsLogin(mobile: String, smsCode: String): LoginResponse {
        val sendLogic = messageSendLogic
            ?: throw BadRequestException("SMS service not configured")

        if (!sendLogic.verifySmsCode(mobile, smsCode)) {
            throw BadRequestException("Invalid or expired SMS code")
        }

        // Find user by mobile
        val user = UserTable.oneWhere {
            User::mobile eq mobile
        } ?: throw NotFoundException("User not found with mobile: $mobile")

        if (user.status != 0) {
            throw BadRequestException("User account is disabled")
        }

        // Fetch user roles
        val userRoles = UserRoleTable.query {
            where { UserRole::userId eq user.id }
        }.list()
        val roleIds = userRoles.map { it.roleId }
        val roles = if (roleIds.isNotEmpty()) {
            RoleTable.query {
                where { Role::id `in` roleIds }
            }.list().map { it.code }.toSet()
        } else emptySet()

        val accessToken = jwt.createToken(
            userId = UserId(user.id.toULong()),
            roles = roles,
            permissions = emptySet(),
            expiresInSeconds = ACCESS_TOKEN_EXPIRES,
            extraClaims = mapOf("type" to "access", "username" to user.username)
        )
        val refreshToken = jwt.createToken(
            userId = UserId(user.id.toULong()),
            roles = emptySet(),
            permissions = emptySet(),
            expiresInSeconds = REFRESH_TOKEN_EXPIRES,
            extraClaims = mapOf("type" to "refresh", "username" to user.username)
        )

        log.info("User SMS logged in: ${user.username}")

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user.id,
            expiresTime = Clock.System.now().toEpochMilliseconds() + ACCESS_TOKEN_EXPIRES * 1000,
            username = user.username,
            nickname = user.nickname
        )
    }

    suspend fun sendSmsCode(mobile: String, scene: String) {
        val sendLogic = messageSendLogic
            ?: throw BadRequestException("SMS service not configured")
        sendLogic.sendVerificationCode(mobile, scene)
    }

    suspend fun resetPassword(mobile: String, smsCode: String, newPassword: String) {
        val sendLogic = messageSendLogic
            ?: throw BadRequestException("SMS service not configured")

        if (!sendLogic.verifySmsCode(mobile, smsCode)) {
            throw BadRequestException("Invalid or expired SMS code")
        }

        val user = UserTable.oneWhere {
            User::mobile eq mobile
        } ?: throw NotFoundException("User not found with mobile: $mobile")

        val hashedPassword = PasswordEncoder.encode(newPassword)
        UserTable.update(user.copy(passwordHash = hashedPassword))
        log.info("Password reset for user: ${user.username}")
    }

    /**
     * Get social auth redirect URL.
     */
    suspend fun socialAuthRedirect(socialType: String, redirectUri: String): String {
        val social = socialUserLogic
            ?: throw BadRequestException("Social login not configured")
        return social.getAuthRedirectUrl(socialType, redirectUri)
    }

    /**
     * Social login — exchange code for token. If the social account is bound
     * to an existing user, return their token. Otherwise throw error.
     */
    suspend fun socialLogin(socialType: String, code: String, redirectUri: String): LoginResponse {
        val social = socialUserLogic
            ?: throw BadRequestException("Social login not configured")

        val socialUser = social.socialLogin(socialType, code, redirectUri, userType = 1)

        if (socialUser.userId == 0L) {
            throw BadRequestException("Social account not bound to any admin user. Please bind first.")
        }

        val user = UserTable.get(socialUser.userId)
            ?: throw NotFoundException("Bound user not found")

        if (user.status != 0) {
            throw BadRequestException("User account is disabled")
        }

        // Fetch user roles
        val userRoles = UserRoleTable.query {
            where { UserRole::userId eq user.id }
        }.list()
        val roleIds = userRoles.map { it.roleId }
        val roles = if (roleIds.isNotEmpty()) {
            RoleTable.query {
                where { Role::id `in` roleIds }
            }.list().map { it.code }.toSet()
        } else emptySet()

        val accessToken = jwt.createToken(
            userId = UserId(user.id.toULong()),
            roles = roles,
            permissions = emptySet(),
            expiresInSeconds = ACCESS_TOKEN_EXPIRES,
            extraClaims = mapOf("type" to "access", "username" to user.username)
        )
        val refreshToken = jwt.createToken(
            userId = UserId(user.id.toULong()),
            roles = emptySet(),
            permissions = emptySet(),
            expiresInSeconds = REFRESH_TOKEN_EXPIRES,
            extraClaims = mapOf("type" to "refresh", "username" to user.username)
        )

        log.info("User social logged in: ${user.username} via $socialType")

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user.id,
            expiresTime = Clock.System.now().toEpochMilliseconds() + ACCESS_TOKEN_EXPIRES * 1000,
            username = user.username,
            nickname = user.nickname
        )
    }

    private fun verifyPassword(rawPassword: String, passwordHash: String): Boolean {
        return PasswordEncoder.matches(rawPassword, passwordHash)
    }

    private fun parseTokenUserId(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            val payload = parts[1]
            val decoded = decodeBase64Url(payload)
            val subRegex = """"sub"\s*:\s*"?(\d+)"?""".toRegex()
            val match = subRegex.find(decoded)
            match?.groupValues?.get(1)?.toLongOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeBase64Url(input: String): String {
        val padded = input.replace('-', '+').replace('_', '/')
        val padding = when (padded.length % 4) {
            2 -> "$padded=="
            3 -> "$padded="
            else -> padded
        }
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val bytes = mutableListOf<Byte>()
        var i = 0
        while (i < padding.length) {
            if (padding[i] == '=') break
            val a = chars.indexOf(padding[i])
            val b = if (i + 1 < padding.length) chars.indexOf(padding[i + 1]) else 0
            val c = if (i + 2 < padding.length && padding[i + 2] != '=') chars.indexOf(padding[i + 2]) else 0
            val d = if (i + 3 < padding.length && padding[i + 3] != '=') chars.indexOf(padding[i + 3]) else 0
            bytes.add(((a shl 2) or (b shr 4)).toByte())
            if (i + 2 < padding.length && padding[i + 2] != '=') {
                bytes.add((((b and 0xF) shl 4) or (c shr 2)).toByte())
            }
            if (i + 3 < padding.length && padding[i + 3] != '=') {
                bytes.add((((c and 0x3) shl 6) or d).toByte())
            }
            i += 4
        }
        return bytes.toByteArray().decodeToString()
    }
}
