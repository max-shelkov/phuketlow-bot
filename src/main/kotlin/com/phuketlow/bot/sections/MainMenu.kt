package com.phuketlow.bot.sections

import com.google.gson.JsonObject
import com.phuketlow.bot.telegram.Keyboard
import com.phuketlow.bot.telegram.TelegramClient

/**
 * Главное меню бота.
 */
object MainMenu {

    fun show(chatId: Long) {
        val text = """
🏝️ *Добро пожаловать в PhuketLow!*

Ваш ассистент на Пхукете. Выберите раздел:
        """.trimIndent()

        val keyboard = Keyboard.inline(
            buttons = listOf(
                "🏠 Недвижимость" to "real_estate",
                "🎭 Экскурсии"    to "excursions",
                "🚗 Аренда авто" to "cars",
                "🛥️ Яхты"        to "yachts",
                "💱 Обмен валюты" to "currency"
            ),
            withMenuButton = false // в главном меню кнопка «Меню» не нужна
        )

        TelegramClient.sendMessageWithMarkup(chatId, text, keyboard)
    }
}
