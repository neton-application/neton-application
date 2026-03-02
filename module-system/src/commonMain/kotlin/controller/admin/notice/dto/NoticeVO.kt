package controller.admin.notice.dto

import kotlinx.serialization.Serializable

@Serializable
data class NoticeVO(
    val id: Long,
    val title: String,
    val content: String,
    val type: Int,
    val status: Int,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
