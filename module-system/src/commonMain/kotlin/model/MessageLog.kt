package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.CreatedAt

@Serializable
@Table("system_message_logs")
data class MessageLog(
    @Id
    val id: Long = 0,
    val channelId: Long,
    val templateId: Long? = null,
    val templateCode: String? = null,
    val receiver: String,
    val content: String? = null,
    val params: String? = null,
    val sendStatus: Int = 0,
    val sendTime: String? = null,
    val errorMessage: String? = null,
    val userId: Long? = null,
    val userType: Int = 0,
    @CreatedAt
    val createdAt: String? = null
)
