package controller.admin.config.dto

import kotlinx.serialization.Serializable

@Serializable
data class ConfigVO(
    val id: Long = 0,
    val category: String? = null,
    val configKey: String? = null,
    val value: String? = null,
    val type: Int? = null,
    val name: String? = null,
    val remark: String? = null
)
