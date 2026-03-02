package controller.admin.post.dto

import kotlinx.serialization.Serializable

@Serializable
data class PostVO(
    val id: Long,
    val code: String,
    val name: String,
    val sort: Int,
    val status: Int
)
