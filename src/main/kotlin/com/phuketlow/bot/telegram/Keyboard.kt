package com.phuketlow.bot.telegram

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Построение inline-клавиатур (reply_markup) для Telegram.
 * Чистая логика без I/O — легко тестировать.
 */
object Keyboard {

    /**
     * Создаёт inline-клавиатуру из списка кнопок.
     * Каждая кнопка — отдельная строка.
     *
     * @param buttons список пар (текст, callback_data)
     * @param withMenuButton добавить ли кнопку «⬅️ Меню» в самом низу
     */
    fun inline(
        buttons: List<Pair<String, String>> = emptyList(),
        withMenuButton: Boolean = true
    ): JsonObject {
        val inlineKeyboard = JsonArray()

        buttons.forEach { (label, callbackData) ->
            inlineKeyboard.add(buttonRow(label, callbackData))
        }

        if (withMenuButton) {
            inlineKeyboard.add(buttonRow("⬅️ Меню", "menu"))
        }

        return JsonObject().apply {
            add("inline_keyboard", inlineKeyboard)
        }
    }

    /** Одна строка с одной кнопкой. */
    private fun buttonRow(text: String, callbackData: String): JsonArray {
        return JsonArray().apply {
            add(JsonObject().apply {
                addProperty("text", text)
                addProperty("callback_data", callbackData)
            })
        }
    }
}
