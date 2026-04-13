package controller.admin.post

import dto.PageResponse
import controller.admin.post.dto.CreatePostRequest
import controller.admin.post.dto.PostVO
import controller.admin.post.dto.UpdatePostRequest
import model.Post
import table.PostTable
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Put
import neton.core.annotations.Delete
import neton.core.annotations.Permission
import neton.core.annotations.PathVariable
import neton.core.annotations.Query
import neton.core.annotations.Body
import neton.core.http.BadRequestException
import neton.core.http.NotFoundException
import neton.database.dsl.*


@Controller("/system/post")
class PostController {

    @Get("/page")
    @Permission("system:post:page")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query code: String? = null,
        @Query status: Int? = null
    ): PageResponse<PostVO> {
        val result = PostTable.query {
            where {
                and(
                    whenNotBlank(name) { Post::name like "%$it%" },
                    whenNotBlank(code) { Post::code like "%$it%" },
                    whenPresent(status) { Post::status eq it }
                )
            }
            orderBy(Post::sort.asc())
        }.page(pageNo, pageSize)

        val items = result.items.map { it.toVO() }
        return PageResponse(
            list = items,
            total = result.total,
            page = pageNo,
            size = pageSize,
            totalPages = ((result.total + pageSize - 1) / pageSize).toInt()
        )
    }

    @Get("/simple-list")
    @Permission("system:post:list")
    suspend fun listAllSimple(): List<PostVO> {
        val posts = PostTable.query {
            where { Post::status eq 0 }
            orderBy(Post::sort.asc())
        }.list()
        return posts.map { it.toVO() }
    }

    @Get("/get/{id}")
    @Permission("system:post:query")
    suspend fun get(@PathVariable id: Long): PostVO {
        val post = PostTable.get(id)
            ?: throw NotFoundException("Post not found")
        return post.toVO()
    }

    @neton.core.annotations.Post("/create")
    @Permission("system:post:create")
    suspend fun create(@Body request: CreatePostRequest): Long {
        val existing = PostTable.oneWhere {
            Post::code eq request.code
        }

        if (existing != null) {
            throw BadRequestException("Post code already exists")
        }

        return PostTable.insert(
            Post(
                code = request.code,
                name = request.name,
                sort = request.sort,
                status = request.status
            )
        ).id
    }

    @Put("/update")
    @Permission("system:post:update")
    suspend fun update(@Body request: UpdatePostRequest) {
        PostTable.get(request.id)
            ?: throw NotFoundException("Post not found")
        PostTable.update(
            Post(
                id = request.id,
                code = request.code,
                name = request.name,
                sort = request.sort,
                status = request.status
            )
        )
    }

    @Delete("/delete/{id}")
    @Permission("system:post:delete")
    suspend fun delete(@PathVariable id: Long) {
        PostTable.get(id)
            ?: throw NotFoundException("Post not found")
        PostTable.destroy(id)
    }

    private fun Post.toVO() = PostVO(
        id = id,
        code = code,
        name = name,
        sort = sort,
        status = status
    )
}
