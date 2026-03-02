package controller.admin.message

import controller.admin.message.dto.NotificationTemplateVO
import dto.PageResponse
import controller.admin.message.dto.SendMessageRequest
import logic.NotificationTemplateLogic
import model.NotificationTemplate
import neton.core.annotations.*

@Controller("/system/notify-template")
class NotifyTemplateController(
    private val notificationTemplateLogic: NotificationTemplateLogic
) {

    @Get("/page")
    @Permission("system:notify-template:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query code: String? = null,
        @Query status: Int? = null
    ): PageResponse<NotificationTemplateVO> {
        return notificationTemplateLogic.page(pageNo, pageSize, name, code, status)
    }

    @Get("/get/{id}")
    @Permission("system:notify-template:query")
    suspend fun get(@PathVariable id: Long): NotificationTemplateVO {
        return notificationTemplateLogic.getById(id)
    }

    @Post("/create")
    @Permission("system:notify-template:create")
    suspend fun create(@Body template: NotificationTemplate): Long {
        return notificationTemplateLogic.create(template)
    }

    @Put("/update")
    @Permission("system:notify-template:update")
    suspend fun update(@Body template: NotificationTemplate) {
        notificationTemplateLogic.update(template)
    }

    @Delete("/delete/{id}")
    @Permission("system:notify-template:delete")
    suspend fun delete(@PathVariable id: Long) {
        notificationTemplateLogic.delete(id)
    }

    @Post("/send-notify")
    @Permission("system:notify-template:update")
    suspend fun sendNotify(@Body request: SendMessageRequest): Boolean {
        return notificationTemplateLogic.send(
            notificationCode = request.templateCode,
            receiver = request.receiver,
            params = request.params
        )
    }
}
