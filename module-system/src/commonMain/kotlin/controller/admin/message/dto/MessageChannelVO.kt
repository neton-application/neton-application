package controller.admin.message.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageChannelVO(
    val id: Long = 0,
    val name: String,
    val code: String,
    val type: String,
    val config: String? = null,
    val status: Int = 0,
    val remark: String? = null,
    val createdAt: String? = null
)
