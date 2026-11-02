package com.phuketlow.bot.handlers

import com.google.gson.JsonObject
import com.phuketlow.bot.sections.MainMenu
import com.phuketlow.bot.telegram.TelegramClient
import org.slf4j.LoggerFactory

/**
 * Обработка текстовых сообщений от пользователя.
 */
object MessageHandler {

    private val logger = LoggerFactory.getLogger("MessageHandler")

    fun handle(message: JsonObject) {
        val chatId = message.get("chat")?.asJsonObject?.get("id")?.asLong ?: return
        val text = message.get("text")?.asString ?: return

        logger.info("Received message from chat $chatId: $text")

        // Custom Menu Button — идемпотентный, ставим на каждом сообщении
        TelegramClient.setChatMenuButton(chatId)

        when {
            text == "/start" || text == "/menu" || text == "Меню" ->
                MainMenu.show(chatId)

            else ->
                TelegramClient.sendMessage(
                    chatId,
                    "📝 Я понимаю только команды.\nОтправьте /start или /menu, чтобы открыть главное меню."
                )
        }
    }
}
