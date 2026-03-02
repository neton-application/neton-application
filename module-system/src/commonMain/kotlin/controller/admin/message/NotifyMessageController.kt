package controller.admin.message

import controller.admin.message.dto.NotifyMessageVO
import dto.PageResponse
import model.NotifyMessage
import table.NotifyMessageTable
import neton.core.annotations.*
import neton.core.interfaces.Identity
import neton.database.dsl.*

@Controller("/system/notify-message")
class NotifyMessageController {

    @Get("/page")
    @Permission("system:notify-message:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query userId: Long? = null,
        @Query templateCode: String? = null,
        @Query templateType: Int? = null,
        @Query readStatus: Int? = null
    ): PageResponse<NotifyMessageVO> {
        val result = NotifyMessageTable.query {
            where {
                and(
                    whenPresent(userId) { NotifyMessage::userId eq it },
                    whenNotBlank(templateCode) { NotifyMessage::templateCode eq it },
                    whenPresent(templateType) { NotifyMessage::templateType eq it },
                    whenPresent(readStatus) { NotifyMessage::readStatus eq it }
                )
            }
            orderBy(NotifyMessage::id.desc())
        }.page(pageNo, pageSize)

        val items = result.items.map { it.toVO() }
        return PageResponse(
            list = items,
            total = result.total,
            page = pageNo,
            size = pageSize,
            totalPages = ((result.total + pageSize - 1) / pageSize).toInt()
        )
    }

    @Get("/my-page")
    @Permission("system:notify-message:query")
    suspend fun myPage(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query readStatus: Int? = null,
        identity: Identity
    ): PageResponse<NotifyMessageVO> {
        val userId = identity.id.toLong()
        val result = NotifyMessageTable.query {
            where {
                and(
                    NotifyMessage::userId eq userId,
                    whenPresent(readStatus) { NotifyMessage::readStatus eq it }
                )
            }
            orderBy(NotifyMessage::id.desc())
        }.page(pageNo, pageSize)

        val items = result.items.map { it.toVO() }
        return PageResponse(
            list = items,
            total = result.total,
            page = pageNo,
            size = pageSize,
            totalPages = ((result.total + pageSize - 1) / pageSize).toInt()
        )
    }

    @Put("/update-read")
    @Permission("system:notify-message:update")
    suspend fun updateRead(@Query ids: String) {
        val idList = ids.split(",").mapNotNull { it.trim().toLongOrNull() }
        for (id in idList) {
            val msg = NotifyMessageTable.get(id) ?: continue
            NotifyMessageTable.update(msg.copy(readStatus = 1))
        }
    }

    @Put("/update-all-read")
    @Permission("system:notify-message:update")
    suspend fun updateAllRead(identity: Identity) {
        val userId = identity.id.toLong()
        val unread = NotifyMessageTable.query {
            where {
                and(
                    NotifyMessage::userId eq userId,
                    NotifyMessage::readStatus eq 0
                )
            }
        }.list()
        for (msg in unread) {
            NotifyMessageTable.update(msg.copy(readStatus = 1))
        }
    }

    @Get("/get-unread-list")
    @Permission("system:notify-message:query")
    suspend fun getUnreadList(identity: Identity): List<NotifyMessageVO> {
        val userId = identity.id.toLong()
        return NotifyMessageTable.query {
            where {
                and(
                    NotifyMessage::userId eq userId,
                    NotifyMessage::readStatus eq 0
                )
            }
            orderBy(NotifyMessage::id.desc())
            limitOffset(10, 0)
        }.list().map { it.toVO() }
    }

    @Get("/get-unread-count")
    @Permission("system:notify-message:query")
    suspend fun getUnreadCount(identity: Identity): Long {
        val userId = identity.id.toLong()
        return NotifyMessageTable.query {
            where {
                and(
                    NotifyMessage::userId eq userId,
                    NotifyMessage::readStatus eq 0
                )
            }
        }.count()
    }

    private fun NotifyMessage.toVO() = NotifyMessageVO(
        id = id,
        userId = userId,
        userType = userType,
        templateId = templateId,
        templateCode = templateCode,
        templateType = templateType,
        templateNickname = templateNickname,
        templateContent = templateContent,
        templateParams = templateParams,
        readStatus = readStatus,
        readTime = readTime,
        createdAt = createdAt
    )
}
