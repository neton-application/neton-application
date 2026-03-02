package controller.admin.message

import controller.admin.message.dto.MessageChannelVO
import dto.PageResponse
import logic.MessageChannelLogic
import model.MessageChannel
import kotlinx.serialization.Serializable
import neton.core.annotations.*

@Serializable
data class ChannelSendTestRequest(
    val channelId: Long,
    val receiver: String,
    val content: String
)

@Controller("/system/message-channel")
class MessageChannelController(
    private val messageChannelLogic: MessageChannelLogic
) {

    @Get("/page")
    @Permission("system:message-channel:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query type: String? = null,
        @Query status: Int? = null
    ): PageResponse<MessageChannelVO> {
        return messageChannelLogic.page(pageNo, pageSize, name, type, status)
    }

    @Get("/get/{id}")
    @Permission("system:message-channel:query")
    suspend fun get(@PathVariable id: Long): MessageChannelVO {
        return messageChannelLogic.getById(id)
    }

    @Get("/simple-list")
    @Permission("system:message-channel:query")
    suspend fun listAllSimple(): List<MessageChannelVO> {
        return messageChannelLogic.listAllSimple()
    }

    @Post("/create")
    @Permission("system:message-channel:create")
    suspend fun create(@Body channel: MessageChannel): Long {
        return messageChannelLogic.create(channel)
    }

    @Put("/update")
    @Permission("system:message-channel:update")
    suspend fun update(@Body channel: MessageChannel) {
        messageChannelLogic.update(channel)
    }

    @Delete("/delete/{id}")
    @Permission("system:message-channel:delete")
    suspend fun delete(@PathVariable id: Long) {
        messageChannelLogic.delete(id)
    }

    @Post("/send-test")
    @Permission("system:message-channel:update")
    suspend fun sendTest(@Body request: ChannelSendTestRequest): Boolean {
        return messageChannelLogic.sendTest(request.channelId, request.receiver, request.content)
    }
}
