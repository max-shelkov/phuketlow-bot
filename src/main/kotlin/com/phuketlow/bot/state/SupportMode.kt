package com.phuketlow.bot.state

import java.util.concurrent.ConcurrentHashMap

/**
 * Отслеживает, какие чаты находятся в режиме ИИ-поддержки.
 * В режиме поддержки любой текст пользователя уходит в LLM,
 * а /start, /menu, «Меню» — выход в главное меню.
 */
object SupportMode {

    private val activeChats = ConcurrentHashMap<Long, Boolean>()

    fun enter(chatId: Long) {
        activeChats[chatId] = true
    }

    fun exit(chatId: Long) {
        activeChats.remove(chatId)
    }

    fun isActive(chatId: Long): Boolean = activeChats[chatId] == true
}
