package controller.admin.config

import controller.admin.config.dto.ConfigVO
import logic.ConfigLogic
import model.Config
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

@Controller("/infra/config")
class ConfigController(
    private val log: Logger,
    private val configLogic: ConfigLogic = ConfigLogic(log)
) {

    @Post("/create")
    @Permission("infra:config:create")
    suspend fun create(@Body vo: ConfigVO): Long {
        val config = Config(
            category = vo.category ?: "",
            configKey = vo.configKey ?: "",
            value = vo.value ?: "",
            type = vo.type ?: 0,
            name = vo.name ?: "",
            remark = vo.remark
        )
        return configLogic.create(config)
    }

    @Put("/update")
    @Permission("infra:config:update")
    suspend fun update(@Body vo: ConfigVO) {
        val existing = configLogic.get(vo.id) ?: throw NotFoundException("Config not found")
        val config = existing.copy(
            category = vo.category ?: existing.category,
            configKey = vo.configKey ?: existing.configKey,
            value = vo.value ?: existing.value,
            type = vo.type ?: existing.type,
            name = vo.name ?: existing.name,
            remark = vo.remark ?: existing.remark
        )
        configLogic.update(config)
    }

    @Delete("/delete/{id}")
    @Permission("infra:config:delete")
    suspend fun delete(@PathVariable id: Long) {
        configLogic.delete(id)
    }

    @Delete("/delete-list")
    @Permission("infra:config:delete")
    suspend fun deleteList(@Query ids: String) {
        ids.split(",").mapNotNull { it.trim().toLongOrNull() }.forEach { configLogic.delete(it) }
    }

    @Get("/get/{id}")
    @Permission("infra:config:query")
    suspend fun get(@PathVariable id: Long): Config? {
        return configLogic.get(id)
    }

    @Get("/page")
    @Permission("infra:config:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query category: String? = null,
        @Query name: String? = null,
        @Query configKey: String? = null,
        @Query type: Int? = null
    ) = configLogic.page(pageNo, pageSize, category, name, configKey, type)

    @Get("/get-value-by-key")
    @Permission("infra:config:query")
    suspend fun getValueByKey(@Query key: String): String? {
        val config = configLogic.getByKey(key)
        return config?.value
    }
}
