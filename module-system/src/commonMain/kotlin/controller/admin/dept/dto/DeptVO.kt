package controller.admin.dept.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeptVO(
    val id: Long,
    val name: String,
    val parentId: Long,
    val sort: Int,
    val status: Int,
    val children: List<DeptVO>? = null
)
