package logic

import controller.admin.menu.dto.MenuVO
import model.Menu
import table.MenuTable
import neton.core.http.BadRequestException
import neton.core.http.NotFoundException
import neton.logging.Logger
import neton.database.dsl.*


class MenuLogic(
    private val log: Logger
) {

    suspend fun listFlat(): List<MenuVO> {
        val menus = MenuTable.query {
            orderBy(Menu::sort.asc())
        }.list()

        return menus.map { it.toVO(children = null) }
    }

    suspend fun listTree(): List<MenuVO> {
        val menus = MenuTable.query {
            orderBy(Menu::sort.asc())
        }.list()

        return buildTree(menus, 0)
    }

    suspend fun listAllSimple(): List<MenuVO> {
        val menus = MenuTable.query {
            where {
                Menu::status eq 0
            }
            orderBy(Menu::sort.asc())
        }.list()

        return menus.map { it.toVO(children = null) }
    }

    suspend fun getById(id: Long): MenuVO {
        val menu = MenuTable.get(id)
            ?: throw NotFoundException("Menu not found")
        return menu.toVO(children = null)
    }

    suspend fun create(menu: Menu): Long {
        return MenuTable.insert(menu).id
    }

    suspend fun update(menu: Menu) {
        MenuTable.get(menu.id)
            ?: throw NotFoundException("Menu not found")

        if (menu.parentId == menu.id) {
            throw BadRequestException("Parent menu cannot be itself")
        }

        MenuTable.update(menu)
    }

    suspend fun delete(id: Long) {
        // Check if menu has children
        val children = MenuTable.query {
            where { Menu::parentId eq id }
        }.list()

        if (children.isNotEmpty()) {
            throw BadRequestException("Cannot delete menu with children, please delete children first")
        }

        MenuTable.destroy(id)
    }

    private fun buildTree(menus: List<Menu>, parentId: Long): List<MenuVO> {
        return menus
            .filter { it.parentId == parentId }
            .map { menu ->
                val children = buildTree(menus, menu.id)
                menu.toVO(children = children.ifEmpty { null })
            }
    }

    private fun Menu.toVO(children: List<MenuVO>?) = MenuVO(
        id = id,
        name = name,
        permission = permission,
        type = type,
        parentId = parentId,
        path = path,
        component = component,
        icon = icon,
        sort = sort,
        status = status,
        visible = status == 0,
        keepAlive = true,
        children = children
    )
}
