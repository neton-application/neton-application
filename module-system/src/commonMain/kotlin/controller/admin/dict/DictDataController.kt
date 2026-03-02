package controller.admin.dict

import controller.admin.dict.dto.DictDataVO
import dto.PageResponse
import logic.DictLogic
import model.DictData
import neton.core.annotations.*

@Controller("/system/dict-data")
class DictDataController(
    private val dictLogic: DictLogic
) {

    @Get("/page")
    @Permission("system:dict:page")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query dictType: String? = null,
        @Query label: String? = null,
        @Query status: Int? = null
    ): PageResponse<DictDataVO> {
        return dictLogic.pageDictData(pageNo, pageSize, dictType, label, status)
    }

    @Get("/simple-list")
    @AllowAnonymous
    suspend fun simpleList(): List<DictDataVO> {
        return dictLogic.listAllSimpleDictData()
    }

    @Get("/get/{id}")
    @Permission("system:dict:query")
    suspend fun get(@PathVariable id: Long): DictDataVO {
        return dictLogic.getDictDataById(id)
    }

    @Get("/type")
    @Permission("system:dict:query")
    suspend fun getByType(@Query type: String): List<DictDataVO> {
        return dictLogic.getDataByType(type)
    }

    @Post("/create")
    @Permission("system:dict:create")
    suspend fun create(@Body dictData: DictData): Long {
        return dictLogic.createDictData(dictData)
    }

    @Put("/update")
    @Permission("system:dict:update")
    suspend fun update(@Body dictData: DictData) {
        dictLogic.updateDictData(dictData)
    }

    @Delete("/delete/{id}")
    @Permission("system:dict:delete")
    suspend fun delete(@PathVariable id: Long) {
        dictLogic.deleteDictData(id)
    }

    @Delete("/delete-list")
    @Permission("system:dict:delete")
    suspend fun deleteList(@Query ids: String) {
        ids.split(",").mapNotNull { it.trim().toLongOrNull() }.forEach { dictLogic.deleteDictData(it) }
    }
}
