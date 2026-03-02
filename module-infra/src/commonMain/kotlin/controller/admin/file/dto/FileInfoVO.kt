package controller.admin.file.dto

import kotlinx.serialization.Serializable

@Serializable
data class FileInfoVO(
    val id: Long = 0,
    val configId: Long? = null,
    val name: String? = null,
    val path: String? = null,
    val url: String? = null,
    val mimeType: String? = null,
    val size: Long = 0,
    val createdAt: String? = null
)
