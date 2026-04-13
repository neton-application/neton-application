package controller.admin.notice

import controller.admin.notice.dto.CreateNoticeRequest
import controller.admin.notice.dto.NoticeVO
import controller.admin.notice.dto.UpdateNoticeRequest
import dto.PageResponse
import model.Notice
import table.NoticeTable
import neton.core.annotations.*
import neton.core.http.NotFoundException
import neton.database.dsl.*


@Controller("/system/notice")
class NoticeController {

    @Get("/page")
    @Permission("system:notice:page")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query title: String? = null,
        @Query type: Int? = null,
        @Query status: Int? = null
    ): PageResponse<NoticeVO> {
        val result = NoticeTable.query {
            where {
                and(
                    whenNotBlank(title) { Notice::title like "%$it%" },
                    whenPresent(type) { Notice::type eq it },
                    whenPresent(status) { Notice::status eq it }
                )
            }
            orderBy(Notice::id.desc())
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

    @Get("/get/{id}")
    @Permission("system:notice:query")
    suspend fun get(@PathVariable id: Long): NoticeVO {
        val notice = NoticeTable.get(id)
            ?: throw NotFoundException("Notice not found")
        return notice.toVO()
    }

    @Post("/create")
    @Permission("system:notice:create")
    suspend fun create(@Body request: CreateNoticeRequest): Long {
        return NoticeTable.insert(
            Notice(
                title = request.title,
                content = request.content,
                type = request.type,
                status = request.status
            )
        ).id
    }

    @Put("/update")
    @Permission("system:notice:update")
    suspend fun update(@Body request: UpdateNoticeRequest) {
        NoticeTable.get(request.id)
            ?: throw NotFoundException("Notice not found")
        NoticeTable.update(
            Notice(
                id = request.id,
                title = request.title,
                content = request.content,
                type = request.type,
                status = request.status
            )
        )
    }

    @Delete("/delete/{id}")
    @Permission("system:notice:delete")
    suspend fun delete(@PathVariable id: Long) {
        NoticeTable.get(id)
            ?: throw NotFoundException("Notice not found")
        NoticeTable.destroy(id)
    }

    private fun Notice.toVO() = NoticeVO(
        id = id,
        title = title,
        content = content,
        type = type,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
