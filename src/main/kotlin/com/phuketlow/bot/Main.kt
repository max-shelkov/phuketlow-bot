package com.phuketlow.bot

import com.phuketlow.bot.config.Config
import com.phuketlow.bot.data.VillaStore
import com.phuketlow.bot.handlers.CallbackHandler
import com.phuketlow.bot.handlers.MessageHandler
import com.phuketlow.bot.telegram.TelegramClient
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("PhuketLowBot")

fun main() {
    logger.info("Starting PhuketLow Telegram Bot...")

    if (Config.TOKEN.isNullOrBlank()) {
        logger.error("ERROR: Bot token not configured!")
        logger.error("Set the environment variable: TELEGRAM_BOT_TOKEN")
        return
    }

    // Test connection
    try {
        val botName = TelegramClient.getMe()
        if (botName != null) {
            logger.info("Bot connected! Username: @$botName")
        }
    } catch (e: Exception) {
        logger.error("Failed to connect to Telegram API", e)
        return
    }

    // Load data
    VillaStore.init()

    logger.info("PhuketLow Bot is running!")
    logger.info("Wait for incoming messages... (Press Ctrl+C to stop)")

    // Long polling
    var offset = 0
    while (true) {
        try {
            val updates = TelegramClient.getUpdates(offset)
            if (updates != null && updates.asJsonObject.get("ok").asBoolean) {
                val result = updates.asJsonObject.getAsJsonArray("result")
                result.forEach { updateElement ->
                    val update = updateElement.asJsonObject
                    val message = update.getAsJsonObject("message")
                    val callbackQuery = update.getAsJsonObject("callback_query")

                    if (message != null) {
                        MessageHandler.handle(message)
                        offset = update.get("update_id").asInt + 1
                    } else if (callbackQuery != null) {
                        CallbackHandler.handle(callbackQuery)
                        offset = update.get("update_id").asInt + 1
                    }
                }
            }
            Thread.sleep(100)
        } catch (e: java.net.SocketTimeoutException) {
            logger.debug("Long polling window closed without updates, retrying...")
        } catch (e: Exception) {
            logger.warn("Error in polling loop: $e")
            Thread.sleep(5000)
        }
    }
}
