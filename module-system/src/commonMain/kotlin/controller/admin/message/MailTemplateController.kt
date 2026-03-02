package controller.admin.message

import controller.admin.message.dto.MessageTemplateVO
import controller.admin.message.dto.SendMessageRequest
import dto.PageResponse
import logic.MessageSendLogic
import logic.MessageTemplateLogic
import model.MessageTemplate
import kotlinx.serialization.Serializable
import neton.core.annotations.*

@Serializable
data class MailSendRequest(
    val templateCode: String,
    val toMails: List<String>,
    val ccMails: List<String> = emptyList(),
    val bccMails: List<String> = emptyList(),
    val templateParams: Map<String, String> = emptyMap()
)

@Controller("/system/mail-template")
class MailTemplateController(
    private val messageTemplateLogic: MessageTemplateLogic,
    private val messageSendLogic: MessageSendLogic
) {

    @Get("/page")
    @Permission("system:mail-template:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query code: String? = null,
        @Query status: Int? = null
    ): PageResponse<MessageTemplateVO> {
        return messageTemplateLogic.pageByChannelType(pageNo, pageSize, "email", name, code, status)
    }

    @Get("/get/{id}")
    @Permission("system:mail-template:query")
    suspend fun get(@PathVariable id: Long): MessageTemplateVO {
        return messageTemplateLogic.getById(id)
    }

    @Post("/create")
    @Permission("system:mail-template:create")
    suspend fun create(@Body template: MessageTemplate): Long {
        return messageTemplateLogic.create(template)
    }

    @Put("/update")
    @Permission("system:mail-template:update")
    suspend fun update(@Body template: MessageTemplate) {
        messageTemplateLogic.update(template)
    }

    @Delete("/delete/{id}")
    @Permission("system:mail-template:delete")
    suspend fun delete(@PathVariable id: Long) {
        messageTemplateLogic.delete(id)
    }

    @Post("/send-mail")
    @Permission("system:mail-template:update")
    suspend fun sendMail(@Body request: MailSendRequest): Boolean {
        return messageSendLogic.sendByTemplate(
            templateCode = request.templateCode,
            receiver = request.toMails.joinToString(","),
            params = request.templateParams
        )
    }
}
