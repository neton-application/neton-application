package logic

import controller.admin.menu.dto.MenuVO
import controller.admin.auth.dto.PermissionInfoVO
import controller.admin.auth.dto.UserInfoVO
import controller.admin.role.dto.RoleVO
import model.Menu
import model.Role
import model.RoleMenu
import model.UserRole
import table.MenuTable
import table.RoleMenuTable
import table.RoleTable
import table.UserRoleTable
import table.UserTable
import neton.core.http.NotFoundException
import neton.logging.Logger
import neton.database.dsl.*

private const val SUPER_ADMIN_ROLE = "super_admin"

class PermissionLogic(
    private val log: Logger
) {

    suspend fun getPermissionInfo(userId: Long): PermissionInfoVO {
        // Get user info
        val user = UserTable.get(userId) ?: throw NotFoundException("User not found")
        val userInfoVO = UserInfoVO(
            userId = user.id.toString(),
            username = user.username,
            nickname = user.nickname,
            avatar = user.avatar ?: ""
        )

        // Get user roles
        val userRoles = UserRoleTable.query {
            where { UserRole::userId eq userId }
        }.list()

        val roleIds = userRoles.map { it.roleId }
        val roles = if (roleIds.isNotEmpty()) {
            RoleTable.query {
                where { Role::id `in` roleIds }
            }.list().filter { it.status == 1 }
        } else emptyList()

        val roleCodes = roles.map { it.code }
        val isSuperAdmin = SUPER_ADMIN_ROLE in roleCodes

        // super_admin: return all menus and wildcard permission
        if (isSuperAdmin) {
            val allMenus = MenuTable.query {
                where { Menu::status eq 1 }
                orderBy(Menu::sort.asc())
            }.list()

            val menuTree = buildMenuTree(allMenus, 0)

            return PermissionInfoVO(
                user = userInfoVO,
                roles = roleCodes,
                permissions = listOf("*:*:*"),
                menus = menuTree
            )
        }

        // Normal role: get menus via role_menu mapping
        val permissions = mutableListOf<String>()
        val menuIds = mutableSetOf<Long>()

        if (roleIds.isNotEmpty()) {
            val roleMenus = RoleMenuTable.query {
                where { RoleMenu::roleId `in` roleIds }
            }.list()

            val allMenuIds = roleMenus.map { it.menuId }
            if (allMenuIds.isNotEmpty()) {
                val menus = MenuTable.query {
                    where {
                        and(
                            Menu::id `in` allMenuIds,
                            Menu::status eq 1
                        )
                    }
                }.list()

                menus.forEach { menu ->
                    menuIds.add(menu.id)
                    if (!menu.permission.isNullOrBlank()) {
                        if (!permissions.contains(menu.permission!!)) {
                            permissions.add(menu.permission!!)
                        }
                    }
                }
            }
        }

        // Build menu tree
        val allMenus = if (menuIds.isNotEmpty()) {
            MenuTable.query {
                where {
                    and(
                        Menu::id `in` menuIds.toList(),
                        Menu::status eq 1
                    )
                }
                orderBy(Menu::sort.asc())
            }.list()
        } else {
            emptyList()
        }

        val menuTree = buildMenuTree(allMenus, 0)

        return PermissionInfoVO(
            user = userInfoVO,
            roles = roleCodes,
            permissions = permissions,
            menus = menuTree
        )
    }

    suspend fun listRoleMenus(roleId: Long): List<Long> {
        val roleMenus = RoleMenuTable.query {
            where { RoleMenu::roleId eq roleId }
        }.list()

        return roleMenus.map { it.menuId }
    }

    suspend fun assignRoleMenu(roleId: Long, menuIds: List<Long>) {
        RoleTable.get(roleId)
            ?: throw NotFoundException("Role not found")

        // Remove existing mappings
        val existing = RoleMenuTable.query {
            where { RoleMenu::roleId eq roleId }
        }.list()
        existing.forEach { RoleMenuTable.destroy(it.id) }

        // Insert new mappings
        menuIds.forEach { menuId ->
            RoleMenuTable.insert(RoleMenu(roleId = roleId, menuId = menuId))
        }

        log.info("Assigned ${menuIds.size} menus to role $roleId")
    }

    suspend fun listUserRoles(userId: Long): List<Long> {
        val userRoles = UserRoleTable.query {
            where { UserRole::userId eq userId }
        }.list()

        return userRoles.map { it.roleId }
    }

    suspend fun assignUserRole(userId: Long, roleIds: List<Long>) {
        // Remove existing mappings
        val existing = UserRoleTable.query {
            where { UserRole::userId eq userId }
        }.list()
        existing.forEach { UserRoleTable.destroy(it.id) }

        // Insert new mappings
        roleIds.forEach { roleId ->
            UserRoleTable.insert(UserRole(userId = userId, roleId = roleId))
        }

        log.info("Assigned ${roleIds.size} roles to user $userId")
    }

    private fun buildMenuTree(menus: List<Menu>, parentId: Long): List<MenuVO> {
        return menus
            .filter { it.parentId == parentId && it.type != 3 }
            .map { menu ->
                val children = buildMenuTree(menus, menu.id)
                MenuVO(
                    id = menu.id,
                    name = menu.name,
                    permission = menu.permission,
                    type = menu.type,
                    parentId = menu.parentId,
                    path = menu.path,
                    component = menu.component,
                    icon = menu.icon,
                    sort = menu.sort,
                    status = menu.status,
                    visible = menu.status == 1,
                    keepAlive = true,
                    children = children.ifEmpty { null }
                )
            }
    }
}
