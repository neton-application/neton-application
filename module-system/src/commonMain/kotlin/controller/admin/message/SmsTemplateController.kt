package controller.admin.message

import controller.admin.message.dto.MessageTemplateVO
import controller.admin.message.dto.SendMessageRequest
import dto.PageResponse
import logic.MessageSendLogic
import logic.MessageTemplateLogic
import model.MessageTemplate
import neton.core.annotations.*

@Controller("/system/sms-template")
class SmsTemplateController(
    private val messageTemplateLogic: MessageTemplateLogic,
    private val messageSendLogic: MessageSendLogic
) {

    @Get("/page")
    @Permission("system:sms-template:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query code: String? = null,
        @Query status: Int? = null
    ): PageResponse<MessageTemplateVO> {
        return messageTemplateLogic.pageByChannelType(pageNo, pageSize, "sms", name, code, status)
    }

    @Get("/get/{id}")
    @Permission("system:sms-template:query")
    suspend fun get(@PathVariable id: Long): MessageTemplateVO {
        return messageTemplateLogic.getById(id)
    }

    @Post("/create")
    @Permission("system:sms-template:create")
    suspend fun create(@Body template: MessageTemplate): Long {
        return messageTemplateLogic.create(template)
    }

    @Put("/update")
    @Permission("system:sms-template:update")
    suspend fun update(@Body template: MessageTemplate) {
        messageTemplateLogic.update(template)
    }

    @Delete("/delete/{id}")
    @Permission("system:sms-template:delete")
    suspend fun delete(@PathVariable id: Long) {
        messageTemplateLogic.delete(id)
    }

    @Post("/send-sms")
    @Permission("system:sms-template:update")
    suspend fun sendSms(@Body request: SendMessageRequest): Boolean {
        return messageSendLogic.sendByTemplate(
            templateCode = request.templateCode,
            receiver = request.receiver,
            params = request.params
        )
    }
}
