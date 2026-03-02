package controller.admin.menu.dto

import kotlinx.serialization.Serializable

@Serializable
data class MenuVO(
    val id: Long,
    val name: String,
    val permission: String? = null,
    val type: Int,
    val parentId: Long,
    val path: String? = null,
    val component: String? = null,
    val icon: String? = null,
    val sort: Int,
    val status: Int,
    val visible: Boolean,
    val keepAlive: Boolean,
    val children: List<MenuVO>? = null
)
