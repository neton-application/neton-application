package controller.admin.file.dto

import kotlinx.serialization.Serializable

@Serializable
data class FileConfigVO(
    val id: Long = 0,
    val name: String? = null,
    val storage: Int? = null,
    val master: Int? = null,
    val remark: String? = null
)
