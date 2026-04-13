package controller.admin.job

import controller.admin.job.dto.CreateJobRequest
import controller.admin.job.dto.JobVO
import controller.admin.job.dto.UpdateJobRequest
import logic.JobLogic
import model.Job
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Put
import neton.core.annotations.Delete
import neton.core.annotations.Permission
import neton.core.annotations.Query
import neton.core.annotations.PathVariable
import neton.core.annotations.Body
import neton.core.http.NotFoundException
import neton.logging.Logger

@Controller("/infra/job")
class JobController(
    private val log: Logger,
    private val jobLogic: JobLogic = JobLogic(log)
) {

    @Post("/create")
    @Permission("infra:job:create")
    suspend fun create(@Body request: CreateJobRequest): Long {
        val job = Job(
            name = request.name,
            handlerName = request.handlerName,
            handlerParam = request.handlerParam,
            cronExpression = request.cronExpression,
            status = request.status
        )
        return jobLogic.createJob(job)
    }

    @Put("/update")
    @Permission("infra:job:update")
    suspend fun update(@Body request: UpdateJobRequest) {
        val existing = jobLogic.getJob(request.id) ?: throw NotFoundException("Job not found")
        val job = existing.copy(
            name = request.name,
            handlerName = request.handlerName,
            handlerParam = request.handlerParam,
            cronExpression = request.cronExpression,
            status = request.status
        )
        jobLogic.updateJob(job)
    }

    @Delete("/delete/{id}")
    @Permission("infra:job:delete")
    suspend fun delete(@PathVariable id: Long) {
        jobLogic.deleteJob(id)
    }

    @Delete("/delete-list")
    @Permission("infra:job:delete")
    suspend fun deleteList(@Query ids: String) {
        ids.split(",").mapNotNull { it.trim().toLongOrNull() }.forEach { jobLogic.deleteJob(it) }
    }

    @Get("/get/{id}")
    @Permission("infra:job:query")
    suspend fun get(@PathVariable id: Long): Job? {
        return jobLogic.getJob(id)
    }

    @Get("/page")
    @Permission("infra:job:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query status: Int? = null,
        @Query handlerName: String? = null
    ) = jobLogic.pageJob(pageNo, pageSize, name, status, handlerName)

    @Put("/update-status/{id}")
    @Permission("infra:job:update")
    suspend fun updateStatus(
        @PathVariable id: Long,
        @Query status: Int
    ) {
        jobLogic.updateJobStatus(id, status)
    }

    @Put("/trigger/{id}")
    @Permission("infra:job:trigger")
    suspend fun trigger(@PathVariable id: Long) {
        jobLogic.triggerJob(id)
    }

    @Get("/get_next_times/{id}")
    @Permission("infra:job:query")
    suspend fun getNextTimes(@PathVariable id: Long): List<String> {
        return jobLogic.getJobNextTimes(id)
    }
}
