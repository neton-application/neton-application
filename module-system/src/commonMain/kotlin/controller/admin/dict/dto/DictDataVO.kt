package controller.admin.dict.dto

import kotlinx.serialization.Serializable

@Serializable
data class DictDataVO(
    val id: Long,
    val dictType: String,
    val label: String,
    val value: String,
    val sort: Int,
    val status: Int,
    val remark: String? = null
)
