package com.phuketlow.bot.sections

import com.google.gson.JsonElement

/**
 * Ответ секции на callback: текст + клавиатура.
 */
data class SectionResponse(
    val text: String,
    val keyboard: JsonElement
)
