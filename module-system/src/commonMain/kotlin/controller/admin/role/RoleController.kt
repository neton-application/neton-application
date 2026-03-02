package controller.admin.role

import dto.PageResponse
import controller.admin.role.dto.RoleVO
import logic.RoleLogic
import model.Role
import neton.core.annotations.*

@Controller("/system/role")
class RoleController(
    private val roleLogic: RoleLogic
) {

    @Get("/page")
    @Permission("system:role:page")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query code: String? = null,
        @Query status: Int? = null
    ): PageResponse<RoleVO> {
        return roleLogic.page(pageNo, pageSize, name, code, status)
    }

    @Get("/simple-list")
    @Permission("system:role:list")
    suspend fun listAllSimple(): List<RoleVO> {
        return roleLogic.listAllSimple()
    }

    @Get("/get/{id}")
    @Permission("system:role:query")
    suspend fun get(@PathVariable id: Long): RoleVO {
        return roleLogic.getById(id)
    }

    @Post("/create")
    @Permission("system:role:create")
    suspend fun create(@Body role: Role): Long {
        return roleLogic.create(role)
    }

    @Put("/update")
    @Permission("system:role:update")
    suspend fun update(@Body role: Role) {
        roleLogic.update(role)
    }

    @Delete("/delete/{id}")
    @Permission("system:role:delete")
    suspend fun delete(@PathVariable id: Long) {
        roleLogic.delete(id)
    }
}
