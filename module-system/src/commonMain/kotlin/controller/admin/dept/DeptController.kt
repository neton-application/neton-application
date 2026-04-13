package controller.admin.dept

import controller.admin.dept.dto.CreateDeptRequest
import controller.admin.dept.dto.DeptVO
import controller.admin.dept.dto.UpdateDeptRequest
import model.Dept
import table.DeptTable
import neton.core.annotations.*
import neton.core.http.BadRequestException
import neton.core.http.NotFoundException
import neton.database.dsl.*


@Controller("/system/dept")
class DeptController {

    @Get("/list")
    @Permission("system:dept:list")
    suspend fun list(): List<DeptVO> {
        val depts = DeptTable.query {
            orderBy(Dept::sort.asc())
        }.list()
        return buildTree(depts, 0)
    }

    @Get("/simple-list")
    @Permission("system:dept:list")
    suspend fun listAllSimple(): List<DeptVO> {
        val depts = DeptTable.query {
            where { Dept::status eq 0 }
            orderBy(Dept::sort.asc())
        }.list()
        return depts.map { it.toVO(children = null) }
    }

    @Get("/get/{id}")
    @Permission("system:dept:query")
    suspend fun get(@PathVariable id: Long): DeptVO {
        val dept = DeptTable.get(id)
            ?: throw NotFoundException("Department not found")
        return dept.toVO(children = null)
    }

    @Post("/create")
    @Permission("system:dept:create")
    suspend fun create(@Body request: CreateDeptRequest): Long {
        return DeptTable.insert(
            Dept(
                name = request.name,
                parentId = request.parentId,
                sort = request.sort,
                leaderUserId = request.leaderUserId,
                status = request.status
            )
        ).id
    }

    @Put("/update")
    @Permission("system:dept:update")
    suspend fun update(@Body request: UpdateDeptRequest) {
        DeptTable.get(request.id)
            ?: throw NotFoundException("Department not found")

        if (request.parentId == request.id) {
            throw BadRequestException("Parent department cannot be itself")
        }

        DeptTable.update(
            Dept(
                id = request.id,
                name = request.name,
                parentId = request.parentId,
                sort = request.sort,
                leaderUserId = request.leaderUserId,
                status = request.status
            )
        )
    }

    @Delete("/delete/{id}")
    @Permission("system:dept:delete")
    suspend fun delete(@PathVariable id: Long) {
        val children = DeptTable.query {
            where { Dept::parentId eq id }
        }.list()

        if (children.isNotEmpty()) {
            throw BadRequestException("Cannot delete department with children")
        }

        DeptTable.destroy(id)
    }

    @Delete("/delete-list")
    @Permission("system:dept:delete")
    suspend fun deleteList(@Query ids: String) {
        ids.split(",").mapNotNull { it.trim().toLongOrNull() }.forEach { DeptTable.destroy(it) }
    }

    private fun buildTree(depts: List<Dept>, parentId: Long): List<DeptVO> {
        return depts
            .filter { it.parentId == parentId }
            .map { dept ->
                val children = buildTree(depts, dept.id)
                dept.toVO(children = children.ifEmpty { null })
            }
    }

    private fun Dept.toVO(children: List<DeptVO>?) = DeptVO(
        id = id,
        name = name,
        parentId = parentId,
        sort = sort,
        status = status,
        children = children
    )
}
