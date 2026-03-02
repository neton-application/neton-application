package controller.admin.message.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageTemplateVO(
    val id: Long = 0,
    val name: String,
    val code: String,
    val content: String,
    val params: String? = null,
    val channelId: Long = 0,
    val type: Int = 0,
    val status: Int = 0,
    val remark: String? = null,
    val createdAt: String? = null
)
