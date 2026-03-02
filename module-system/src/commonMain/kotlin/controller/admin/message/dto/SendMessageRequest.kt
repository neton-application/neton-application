package controller.admin.message.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    val templateCode: String,
    val receiver: String,
    val params: Map<String, String> = emptyMap()
)
