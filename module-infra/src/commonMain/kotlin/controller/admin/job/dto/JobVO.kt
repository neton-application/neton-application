package controller.admin.job.dto

import kotlinx.serialization.Serializable

@Serializable
data class JobVO(
    val id: Long = 0,
    val name: String? = null,
    val handlerName: String? = null,
    val handlerParam: String? = null,
    val cronExpression: String? = null,
    val status: Int? = null
)
