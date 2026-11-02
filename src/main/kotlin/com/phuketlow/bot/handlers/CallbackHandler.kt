package com.phuketlow.bot.handlers

import com.google.gson.JsonObject
import com.phuketlow.bot.sections.MainMenu
import com.phuketlow.bot.sections.RealEstate
import com.phuketlow.bot.telegram.Keyboard
import com.phuketlow.bot.telegram.TelegramClient
import org.slf4j.LoggerFactory

object CallbackHandler {

    private val logger = LoggerFactory.getLogger("CallbackHandler")

    fun handle(callback: JsonObject) {
        val callbackId = callback.get("id")?.asString ?: return
        val chatId = callback.get("message")?.asJsonObject
            ?.get("chat")?.asJsonObject?.get("id")?.asLong ?: return
        val data = callback.get("data")?.asString ?: return

        logger.info("Received callback from chat $chatId: $data")

        when {
            data == "menu" -> {
                MainMenu.show(chatId)
                TelegramClient.answerCallbackQuery(callbackId)
            }

            data.startsWith("villa_") -> {
                val id = data.removePrefix("villa_")
                TelegramClient.answerCallbackQuery(callbackId)
                TelegramClient.sendChatAction(chatId, "typing")
                RealEstate.showVillaCard(chatId, id)
            }

            data == "real_estate" ||
            data == "real_estate_rent" ||
            data == "real_estate_buy" -> {
                val response = RealEstate.handleCallback(data, chatId)
                TelegramClient.sendMessageWithMarkup(
                    chatId, response.text, response.keyboard
                )
                TelegramClient.answerCallbackQuery(callbackId)
            }

            else -> {
                val text = stubText(data)
                TelegramClient.sendMessageWithMarkup(
                    chatId, text, Keyboard.inline()
                )
                TelegramClient.answerCallbackQuery(callbackId)
            }
        }
    }

    private fun stubText(data: String): String = when (data) {
        "excursions" -> "🎭 *Экскурсии*\n\nПопулярные экскурсии на Пхукете:\n• Острова (Пхи-Пхи, Джеймс Бонд)\n• Водопады и храмы\n• Ночные рынки и шоу\n\n_Раздел в разработке_"
        "cars" -> "🚗 *Аренда авто*\n\nТранспорт на Пхукете:\n• Мотобайки и скутеры\n• Автомобили с водителем\n• Трансферы из аэропорта\n\n_Раздел в разработке_"
        "yachts" -> "🛥️ *Яхты*\n\nАренда яхт на Пхукете:\n• Частные яхты\n• Групповые поездки\n• Рыбалка и дайвинг\n\n_Раздел в разработке_"
        "currency" -> "💱 *Обмен валюты*\n\nКурсы валют на сегодня:\n\n_Раздел в разработке_"
        else -> "❓ Неизвестная команда"
    }
}
