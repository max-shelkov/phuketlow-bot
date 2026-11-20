package com.phuketlow.bot.handlers

import com.google.gson.JsonObject
import com.phuketlow.bot.sections.MainMenu
import com.phuketlow.bot.sections.Support
import com.phuketlow.bot.state.SupportMode
import com.phuketlow.bot.telegram.TelegramClient
import org.slf4j.LoggerFactory

/**
 * Обработка текстовых сообщений от пользователя.
 */
object MessageHandler {

    private val logger = LoggerFactory.getLogger("MessageHandler")

    private val EXIT_COMMANDS = setOf("/start", "/menu", "Меню")

    fun handle(message: JsonObject) {
        val chatId = message.get("chat")?.asJsonObject?.get("id")?.asLong ?: return
        val text = message.get("text")?.asString ?: return

        logger.info("Received message from chat $chatId: $text")

        // Custom Menu Button — идемпотентный, ставим на каждом сообщении
        TelegramClient.setChatMenuButton(chatId)

        // Команды выхода из режима поддержки — всегда работают
        if (text in EXIT_COMMANDS) {
            if (SupportMode.isActive(chatId)) {
                Support.exit(chatId)
            }
            MainMenu.show(chatId)
            return
        }

        when {
            // Режим поддержки активен — любой текст уходит в ИИ
            SupportMode.isActive(chatId) ->
                Support.ask(chatId, text)

            else ->
                TelegramClient.sendMessage(
                    chatId,
                    "📝 Я понимаю только команды.\nОтправьте /start или /menu, чтобы открыть главное меню."
                )
        }
    }
}
