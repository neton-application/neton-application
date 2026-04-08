package controller.admin.message.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationTemplateVO(
    val id: Long = 0,
    val name: String,
    val code: String,
    val type: Int = 0,
    val messageTemplateId: Long = 0,
    val params: String? = null,
    val status: Int = 1,
    val remark: String? = null,
    val createdAt: String? = null
)
