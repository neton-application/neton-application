package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.CreatedAt

@Serializable
@Table("infra_files")
data class FileInfo(
    @Id
    val id: Long = 0,
    val configId: Long? = null,
    val name: String,
    val path: String,
    val url: String? = null,
    val mimeType: String? = null,
    val size: Long = 0,
    @CreatedAt
    val createdAt: String? = null
)
