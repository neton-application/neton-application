package controller.admin.message.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import model.MessageChannel

@Serializable
data class MailAccountVO(
    val id: Long = 0,
    val mail: String = "",
    val username: String = "",
    val password: String = "",
    val host: String = "",
    val port: Int = 465,
    val sslEnable: Boolean = true,
    val starttlsEnable: Boolean = false,
    val status: Int = 1,
    val remark: String? = null,
    val createTime: String? = null
) {
    fun toChannel(): MessageChannel {
        val config = buildJsonObject {
            put("mail", mail)
            put("username", username)
            put("password", password)
            put("host", host)
            put("port", port)
            put("sslEnable", sslEnable)
            put("starttlsEnable", starttlsEnable)
        }.toString()
        return MessageChannel(
            id = id,
            name = mail.ifBlank { username },
            code = mail.replace("@", "_at_").replace(".", "_"),
            type = "email",
            config = config,
            status = status,
            remark = remark
        )
    }

    companion object {
        fun fromChannel(channel: MessageChannel): MailAccountVO {
            return try {
                val json = Json { ignoreUnknownKeys = true }
                val obj = json.parseToJsonElement(channel.config ?: "{}").let {
                    it as? kotlinx.serialization.json.JsonObject ?: kotlinx.serialization.json.JsonObject(emptyMap())
                }
                MailAccountVO(
                    id = channel.id,
                    mail = obj["mail"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        ?: channel.name,
                    username = obj["username"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        ?: "",
                    password = obj["password"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        ?: "",
                    host = obj["host"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: "",
                    port = obj["port"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() }
                        ?: 465,
                    sslEnable = obj["sslEnable"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBoolean() }
                        ?: true,
                    starttlsEnable = obj["starttlsEnable"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBoolean() }
                        ?: false,
                    status = channel.status,
                    remark = channel.remark,
                    createTime = channel.createdAt
                )
            } catch (_: Exception) {
                MailAccountVO(
                    id = channel.id,
                    mail = channel.name,
                    status = channel.status,
                    remark = channel.remark,
                    createTime = channel.createdAt
                )
            }
        }
    }
}
