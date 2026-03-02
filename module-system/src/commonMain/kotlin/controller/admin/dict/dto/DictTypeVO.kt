package controller.admin.dict.dto

import kotlinx.serialization.Serializable

@Serializable
data class DictTypeVO(
    val id: Long,
    val name: String,
    val type: String,
    val status: Int,
    val remark: String? = null
)
