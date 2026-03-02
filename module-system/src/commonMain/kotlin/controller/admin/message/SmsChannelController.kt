package controller.admin.message

import controller.admin.message.dto.MessageChannelVO
import dto.PageResponse
import logic.MessageChannelLogic
import model.MessageChannel
import kotlinx.serialization.Serializable
import neton.core.annotations.*

@Serializable
data class SmsChannelSendTestRequest(
    val channelId: Long,
    val receiver: String,
    val content: String
)

@Controller("/system/sms-channel")
class SmsChannelController(
    private val messageChannelLogic: MessageChannelLogic
) {

    @Get("/page")
    @Permission("system:sms-channel:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query status: Int? = null
    ): PageResponse<MessageChannelVO> {
        return messageChannelLogic.page(pageNo, pageSize, name, type = "sms", status)
    }

    @Get("/get/{id}")
    @Permission("system:sms-channel:query")
    suspend fun get(@PathVariable id: Long): MessageChannelVO {
        return messageChannelLogic.getById(id)
    }

    @Get("/simple-list")
    @Permission("system:sms-channel:query")
    suspend fun simpleList(): List<MessageChannelVO> {
        return messageChannelLogic.listByType("sms")
    }

    @Post("/create")
    @Permission("system:sms-channel:create")
    suspend fun create(@Body channel: MessageChannel): Long {
        return messageChannelLogic.create(channel.copy(type = "sms"))
    }

    @Put("/update")
    @Permission("system:sms-channel:update")
    suspend fun update(@Body channel: MessageChannel) {
        messageChannelLogic.update(channel)
    }

    @Delete("/delete/{id}")
    @Permission("system:sms-channel:delete")
    suspend fun delete(@PathVariable id: Long) {
        messageChannelLogic.delete(id)
    }

    @Post("/send-test")
    @Permission("system:sms-channel:update")
    suspend fun sendTest(@Body request: SmsChannelSendTestRequest): Boolean {
        return messageChannelLogic.sendTest(request.channelId, request.receiver, request.content)
    }
}
