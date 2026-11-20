package com.phuketlow.bot.state

import com.phuketlow.bot.ai.AiClient.ChatMessage
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * История диалога с поддержкой, на каждый чат.
 * Ограничение: последние MAX_MESSAGES сообщений, TTL — 30 минут неактивности.
 */
object ConversationStore {

    private val logger = LoggerFactory.getLogger("ConversationStore")

    private const val MAX_MESSAGES = 20
    private const val TTL_MILLIS = 30 * 60 * 1000L

    private data class Conversation(
        val messages: ArrayDeque<ChatMessage> = ArrayDeque(),
        var lastActiveAt: Long = System.currentTimeMillis()
    )

    private val conversations = ConcurrentHashMap<Long, Conversation>()

    fun add(chatId: Long, message: ChatMessage) {
        val conv = conversations.computeIfAbsent(chatId) { Conversation() }
        synchronized(conv) {
            conv.messages.addLast(message)
            while (conv.messages.size > MAX_MESSAGES) conv.messages.removeFirst()
            conv.lastActiveAt = System.currentTimeMillis()
        }
    }

    fun history(chatId: Long): List<ChatMessage> {
        val conv = conversations[chatId] ?: return emptyList()
        return synchronized(conv) { conv.messages.toList() }
    }

    fun clear(chatId: Long) {
        conversations.remove(chatId)
    }

    /** Удаляет диалоги, неактивные дольше TTL. Вызывать периодически. */
    fun cleanup() {
        val now = System.currentTimeMillis()
        val expired = conversations.entries
            .filter { now - it.value.lastActiveAt > TTL_MILLIS }
            .map { it.key }
        expired.forEach { conversations.remove(it) }
        if (expired.isNotEmpty()) {
            logger.info("ConversationStore: cleaned up {} expired conversation(s)", expired.size)
        }
    }
}
