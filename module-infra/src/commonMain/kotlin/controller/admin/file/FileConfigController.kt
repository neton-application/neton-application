package controller.admin.file

import controller.admin.file.dto.FileConfigVO
import logic.FileLogic
import model.FileConfig
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

@Controller("/infra/file-config")
class FileConfigController(
    private val log: Logger,
    private val fileLogic: FileLogic = FileLogic(log)
) {

    @Post("/create")
    @Permission("infra:file-config:create")
    suspend fun create(@Body vo: FileConfigVO): Long {
        val fileConfig = FileConfig(
            name = vo.name ?: "",
            storage = vo.storage ?: 0,
            config = "",
            master = vo.master ?: 0,
            remark = vo.remark
        )
        return fileLogic.createFileConfig(fileConfig)
    }

    @Put("/update")
    @Permission("infra:file-config:update")
    suspend fun update(@Body vo: FileConfigVO) {
        val existing = fileLogic.getFileConfig(vo.id) ?: throw NotFoundException("File config not found")
        val fileConfig = existing.copy(
            name = vo.name ?: existing.name,
            storage = vo.storage ?: existing.storage,
            master = vo.master ?: existing.master,
            remark = vo.remark ?: existing.remark
        )
        fileLogic.updateFileConfig(fileConfig)
    }

    @Delete("/delete/{id}")
    @Permission("infra:file-config:delete")
    suspend fun delete(@PathVariable id: Long) {
        fileLogic.deleteFileConfig(id)
    }

    @Delete("/delete-list")
    @Permission("infra:file-config:delete")
    suspend fun deleteList(@Query ids: String) {
        ids.split(",").mapNotNull { it.trim().toLongOrNull() }.forEach { fileLogic.deleteFileConfig(it) }
    }

    @Get("/get/{id}")
    @Permission("infra:file-config:query")
    suspend fun get(@PathVariable id: Long): FileConfig? {
        return fileLogic.getFileConfig(id)
    }

    @Get("/page")
    @Permission("infra:file-config:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query storage: Int? = null
    ) = fileLogic.pageFileConfig(pageNo, pageSize, name, storage)

    @Put("/update-master/{id}")
    @Permission("infra:file-config:update")
    suspend fun updateMaster(@PathVariable id: Long) {
        fileLogic.updateMaster(id)
    }

    @Get("/test/{id}")
    @Permission("infra:file-config:query")
    suspend fun test(@PathVariable id: Long): Boolean {
        return fileLogic.testFileConfig(id)
    }
}
