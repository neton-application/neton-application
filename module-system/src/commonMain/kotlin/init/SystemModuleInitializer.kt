package init

import infra.TableRegistryBuilder
import neton.core.component.NetonContext
import neton.core.module.ModuleInitializer
import neton.logging.LoggerFactory
import neton.security.jwt.JwtAuthenticatorV1
import config.buildJwtAuthenticator

// models
import model.*
// tables
import table.*
// logic
import logic.*
// providers
import logic.provider.*

object SystemModuleInitializer : ModuleInitializer {

    override val moduleId: String = "system"

    override fun initialize(ctx: NetonContext) {
        val loggerFactory = ctx.get(LoggerFactory::class)
        val registry = ctx.get(TableRegistryBuilder::class)

        // ===== 注册 Table =====
        registerTables(registry)

        // ===== 创建共享服务 =====
        val jwt = ctx.getOrNull(JwtAuthenticatorV1::class) ?: buildJwtAuthenticator(ctx)
        ctx.bind(JwtAuthenticatorV1::class, jwt)

        // ===== 创建 Provider =====
        val smsProvider = SmsProvider(loggerFactory.get("provider.sms"))
        val emailProvider = EmailProvider(loggerFactory.get("provider.email"))
        val messageProviders = mapOf<String, MessageProvider>("sms" to smsProvider, "email" to emailProvider)

        val googleProvider = GoogleSocialProvider(loggerFactory.get("provider.google"))
        val telegramProvider = TelegramSocialProvider(loggerFactory.get("provider.telegram"))
        val socialProviders = mapOf<String, SocialProvider>("google" to googleProvider, "telegram" to telegramProvider)

        // ===== 绑定 Logic =====
        val messageChannelLogic = MessageChannelLogic(loggerFactory.get("logic.message-channel"), messageProviders)
        val messageTemplateLogic = MessageTemplateLogic(loggerFactory.get("logic.message-template"))
        val messageSendLogic = MessageSendLogic(loggerFactory.get("logic.message-send"), messageChannelLogic, messageTemplateLogic)
        val socialUserLogic = SocialUserLogic(loggerFactory.get("logic.social-user"), socialProviders)
        val notificationTemplateLogic = NotificationTemplateLogic(loggerFactory.get("logic.notification-template"), messageSendLogic)

        ctx.bind(MessageChannelLogic::class, messageChannelLogic)
        ctx.bind(MessageTemplateLogic::class, messageTemplateLogic)
        ctx.bind(MessageSendLogic::class, messageSendLogic)
        ctx.bind(SocialUserLogic::class, socialUserLogic)
        ctx.bind(NotificationTemplateLogic::class, notificationTemplateLogic)

        ctx.bind(AuthLogic::class, AuthLogic(loggerFactory.get("logic.auth"), jwt, messageSendLogic, socialUserLogic))
        ctx.bind(UserLogic::class, UserLogic(loggerFactory.get("logic.user")))
        ctx.bind(RoleLogic::class, RoleLogic(loggerFactory.get("logic.role")))
        ctx.bind(MenuLogic::class, MenuLogic(loggerFactory.get("logic.menu")))
        ctx.bind(PermissionLogic::class, PermissionLogic(loggerFactory.get("logic.permission")))
        ctx.bind(DictLogic::class, DictLogic(loggerFactory.get("logic.dict"), infra.SimpleCache()))
        ctx.bind(LogLogic::class, LogLogic(loggerFactory.get("logic.log")))
        ctx.bind(DeptLogic::class, DeptLogic(loggerFactory.get("logic.dept")))
        ctx.bind(PostLogic::class, PostLogic(loggerFactory.get("logic.post")))

        // 注册 KSP 生成的路由
        neton.module.system.generated.SystemRouteInitializer.initialize(ctx)
    }

    private fun registerTables(registry: TableRegistryBuilder) {
        registry.register(User::class, UserTable)
        registry.register(Role::class, RoleTable)
        registry.register(Menu::class, MenuTable)
        registry.register(UserRole::class, UserRoleTable)
        registry.register(RoleMenu::class, RoleMenuTable)
        registry.register(Dept::class, DeptTable)
        registry.register(Post::class, PostTable)
        registry.register(DictType::class, DictTypeTable)
        registry.register(DictData::class, DictDataTable)
        registry.register(Notice::class, NoticeTable)
        registry.register(LoginLog::class, LoginLogTable)
        registry.register(OperateLog::class, OperateLogTable)
        // Provider tables
        registry.register(MessageChannel::class, MessageChannelTable)
        registry.register(MessageTemplate::class, MessageTemplateTable)
        registry.register(MessageLog::class, MessageLogTable)
        registry.register(SocialUser::class, SocialUserTable)
        registry.register(NotificationTemplate::class, NotificationTemplateTable)
        registry.register(NotifyMessage::class, NotifyMessageTable)
    }
}
