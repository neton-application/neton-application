package controller.admin.dict

import controller.admin.dict.dto.DictTypeVO
import dto.PageResponse
import logic.DictLogic
import model.DictType
import neton.core.annotations.*

@Controller("/system/dict-type")
class DictTypeController(
    private val dictLogic: DictLogic
) {

    @Get("/page")
    @Permission("system:dict:page")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query type: String? = null,
        @Query status: Int? = null
    ): PageResponse<DictTypeVO> {
        return dictLogic.pageDictTypes(pageNo, pageSize, name, type, status)
    }

    @Get("/simple-list")
    @Permission("system:dict:query")
    suspend fun simpleList(): List<DictTypeVO> {
        return dictLogic.listAllDictTypesSimple()
    }

    @Get("/get/{id}")
    @Permission("system:dict:query")
    suspend fun get(@PathVariable id: Long): DictTypeVO {
        return dictLogic.getDictTypeById(id)
    }

    @Post("/create")
    @Permission("system:dict:create")
    suspend fun create(@Body dictType: DictType): Long {
        return dictLogic.createDictType(dictType)
    }

    @Put("/update")
    @Permission("system:dict:update")
    suspend fun update(@Body dictType: DictType) {
        dictLogic.updateDictType(dictType)
    }

    @Delete("/delete/{id}")
    @Permission("system:dict:delete")
    suspend fun delete(@PathVariable id: Long) {
        dictLogic.deleteDictType(id)
    }

    @Delete("/delete-list")
    @Permission("system:dict:delete")
    suspend fun deleteList(@Query ids: String) {
        ids.split(",").mapNotNull { it.trim().toLongOrNull() }.forEach { dictLogic.deleteDictType(it) }
    }
}
