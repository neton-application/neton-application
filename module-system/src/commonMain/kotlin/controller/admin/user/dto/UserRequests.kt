package controller.admin.user.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val username: String,
    val password: String,
    val nickname: String,
    val deptId: Long? = null,
    val postIds: List<Long>? = null,
    val email: String? = null,
    val mobile: String? = null,
    val sex: Int = 0,
    val status: Int = 1,
    val remark: String? = null
)

@Serializable
data class UpdateUserRequest(
    val id: Long,
    val username: String? = null,
    val nickname: String? = null,
    val deptId: Long? = null,
    val postIds: List<Long>? = null,
    val email: String? = null,
    val mobile: String? = null,
    val sex: Int? = null,
    val status: Int? = null,
    val remark: String? = null
)

@Serializable
data class UpdatePasswordRequest(
    val id: Long,
    val newPassword: String
)

@Serializable
data class UpdateStatusRequest(
    val status: Int
)

@Serializable
data class ProfileUpdatePasswordRequest(
    val newPassword: String
)
