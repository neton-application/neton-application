package controller.admin.message

import controller.admin.message.dto.MessageTemplateVO
import dto.PageResponse
import controller.admin.message.dto.SendMessageRequest
import logic.MessageSendLogic
import logic.MessageTemplateLogic
import model.MessageTemplate
import neton.core.annotations.*

@Controller("/system/message-template")
class MessageTemplateController(
    private val messageTemplateLogic: MessageTemplateLogic,
    private val messageSendLogic: MessageSendLogic
) {

    @Get("/page")
    @Permission("system:message-template:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query code: String? = null,
        @Query type: Int? = null,
        @Query status: Int? = null
    ): PageResponse<MessageTemplateVO> {
        return messageTemplateLogic.page(pageNo, pageSize, name, code, type, status)
    }

    @Get("/get/{id}")
    @Permission("system:message-template:query")
    suspend fun get(@PathVariable id: Long): MessageTemplateVO {
        return messageTemplateLogic.getById(id)
    }

    @Post("/create")
    @Permission("system:message-template:create")
    suspend fun create(@Body template: MessageTemplate): Long {
        return messageTemplateLogic.create(template)
    }

    @Put("/update")
    @Permission("system:message-template:update")
    suspend fun update(@Body template: MessageTemplate) {
        messageTemplateLogic.update(template)
    }

    @Delete("/delete/{id}")
    @Permission("system:message-template:delete")
    suspend fun delete(@PathVariable id: Long) {
        messageTemplateLogic.delete(id)
    }

    @Post("/send")
    @Permission("system:message-template:update")
    suspend fun send(@Body request: SendMessageRequest): Boolean {
        return messageSendLogic.sendByTemplate(
            templateCode = request.templateCode,
            receiver = request.receiver,
            params = request.params
        )
    }
}
