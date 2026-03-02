package controller.admin.permission.dto

import kotlinx.serialization.Serializable

@Serializable
data class AssignRoleMenuRequest(
    val roleId: Long,
    val menuIds: List<Long>
)

@Serializable
data class AssignUserRoleRequest(
    val userId: Long,
    val roleIds: List<Long>
)
