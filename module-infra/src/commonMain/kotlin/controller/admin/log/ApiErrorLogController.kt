package controller.admin.log

import controller.admin.log.dto.ApiErrorLogVO
import dto.PageResponse
import model.ApiErrorLog
import table.ApiErrorLogTable
import neton.database.dsl.*

import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Put
import neton.core.annotations.Permission
import neton.core.annotations.Query
import neton.core.http.NotFoundException
import neton.logging.Logger
import kotlin.time.Clock

@Controller("/infra/api-error-log")
class ApiErrorLogController(
    private val log: Logger
) {

    @Get("/page")
    @Permission("infra:api-error-log:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query userId: Long? = null,
        @Query applicationName: String? = null,
        @Query requestUrl: String? = null,
        @Query processStatus: Int? = null
    ): PageResponse<ApiErrorLogVO> {
        val result = ApiErrorLogTable.query {
            where {
                and(
                    whenPresent(userId) { ApiErrorLog::userId eq it },
                    whenNotBlank(applicationName) { ApiErrorLog::applicationName like "%$it%" },
                    whenNotBlank(requestUrl) { ApiErrorLog::requestUrl like "%$it%" },
                    whenPresent(processStatus) { ApiErrorLog::processStatus eq it }
                )
            }
            orderBy(ApiErrorLog::id.desc())
        }.page(pageNo, pageSize)

        return PageResponse(
            list = result.items.map { it.toVO() },
            total = result.total,
            page = pageNo,
            size = pageSize,
            totalPages = if (pageSize > 0) ((result.total + pageSize - 1) / pageSize).toInt() else 0
        )
    }

    @Get("/get")
    @Permission("infra:api-error-log:query")
    suspend fun get(@Query id: Long): ApiErrorLog? {
        return ApiErrorLogTable.get(id)
    }

    @Put("/update-status")
    @Permission("infra:api-error-log:update")
    suspend fun updateStatus(
        @Query id: Long,
        @Query processStatus: Int,
        @Query processUserId: Long
    ) {
        val errorLog = ApiErrorLogTable.get(id)
            ?: throw NotFoundException("Error log not found")
        val updated = errorLog.copy(
            processStatus = processStatus,
            processUserId = processUserId,
            processTime = Clock.System.now().toEpochMilliseconds()
        )
        ApiErrorLogTable.update(updated)
    }

    private fun ApiErrorLog.toVO() = ApiErrorLogVO(
        id = id,
        userId = userId,
        requestMethod = requestMethod,
        requestUrl = requestUrl,
        exceptionName = exceptionName,
        processStatus = processStatus,
        createdAt = createdAt
    )
}
