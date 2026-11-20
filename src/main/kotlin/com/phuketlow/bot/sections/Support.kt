package com.phuketlow.bot.sections

import com.phuketlow.bot.ai.AiClient
import com.phuketlow.bot.ai.AiClient.ChatMessage
import com.phuketlow.bot.knowledge.KnowledgeBase
import com.phuketlow.bot.state.ConversationStore
import com.phuketlow.bot.state.SupportMode
import com.phuketlow.bot.telegram.Keyboard
import com.phuketlow.bot.telegram.TelegramClient
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Секция «Поддержка»: ИИ-консультант на базе знаний.
 *
 * Правила:
 *  - модель отвечает ИСКЛЮЧИТЕЛЬНО на основе файла базы знаний;
 *  - если ответа в базе нет — модель отвечает маркером [[NO_ANSWER]],
 *    бот отправляет вежливый отказ и записывает вопрос в data/unanswered_questions.log.
 */
object Support {

    private val logger = LoggerFactory.getLogger("Support")

    private const val NO_ANSWER_MARKER = "[[NO_ANSWER]]"

    private val LOG_FILE = File("data/unanswered_questions.log")
    private val LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    // ─── System prompt ───────────────────────────────────────────────────────

    private fun systemPrompt(): String {
        val knowledge = KnowledgeBase.content()
        return buildString {
            appendLine("Ты — виртуальный ассистент поддержки сервиса PhuketLow (Пхукет, Таиланд).")
            appendLine("Отвечай на русском языке, кратко и по делу, дружелюбно.")
            appendLine()
            appendLine("ВАЖНОЕ ПРАВИЛО: отвечай ИСКЛЮЧИТЕЛЬНО на основе базы знаний ниже.")
            appendLine("Не используй собственные знания, не выдумывай цены, условия и контакты.")
            appendLine("Если в базе знаний НЕТ ответа на вопрос пользователя — ответь ОДНОЙ строкой ровно так: $NO_ANSWER_MARKER")
            appendLine("Ничего не добавляй до и после маркера.")
            appendLine()
            appendLine("=== БАЗА ЗНАНИЙ ===")
            appendLine(knowledge.ifBlank { "(база знаний пуста)" })
            appendLine("=== КОНЕЦ БАЗЫ ЗНАНИЙ ===")
        }
    }

    // ─── Вход / выход из режима ──────────────────────────────────────────────

    fun enter(chatId: Long) {
        SupportMode.enter(chatId)
        val text = """
💬 *Режим поддержки*

Здравствуйте! Я ИИ-консультант PhuketLow.
Спросите меня о виллах, экскурсиях, аренде авто, яхтах или обмене валюты.

Я отвечаю только по своей базе знаний — если чего-то не знаю, честно скажу.

_Для выхода отправьте «Меню» или нажмите кнопку ниже._
        """.trimIndent()
        TelegramClient.sendMessageWithMarkup(chatId, text, Keyboard.inline())
    }

    fun exit(chatId: Long) {
        SupportMode.exit(chatId)
        ConversationStore.clear(chatId)
    }

    // ─── Вопрос → ответ ──────────────────────────────────────────────────────

    fun ask(chatId: Long, userText: String) {
        if (!KnowledgeBase.isLoaded) {
            TelegramClient.sendMessage(
                chatId,
                "⚠️ База знаний временно недоступна. Попробуйте позже или напишите менеджеру.",
                parseMode = "HTML"
            )
            return
        }

        TelegramClient.sendChatAction(chatId, "typing")

        val messages = mutableListOf(ChatMessage("system", systemPrompt()))
        messages.addAll(ConversationStore.history(chatId))
        messages.add(ChatMessage("user", userText))

        val rawAnswer: String = try {
            AiClient.chat(messages)
        } catch (e: AiClient.AiException) {
            logger.error("LLM error for chat $chatId: ${e.message}")
            TelegramClient.sendMessage(
                chatId,
                "⚠️ Извините, сейчас не получается связаться с ИИ-ассистентом. Попробуйте ещё раз чуть позже.",
                parseMode = "HTML"
            )
            return
        }

        // Сохраняем в историю «чистые» сообщения (без маркера)
        val isNoAnswer = rawAnswer.contains(NO_ANSWER_MARKER)
        val cleanAnswer = rawAnswer.replace(NO_ANSWER_MARKER, "").trim()

        if (isNoAnswer) {
            logUnansweredQuestion(chatId, userText)
            ConversationStore.add(chatId, ChatMessage("user", userText))
            ConversationStore.add(chatId, ChatMessage("assistant", "Не могу ответить на этот вопрос."))
            TelegramClient.sendMessage(
                chatId,
                "😔 К сожалению, у меня нет информации по этому вопросу. " +
                    "Я записал его — менеджер ответит лично. " +
                    "Попробуйте спросить о виллах, экскурсиях, авто, яхтах или валюте.",
                parseMode = "HTML"
            )
            return
        }

        ConversationStore.add(chatId, ChatMessage("user", userText))
        ConversationStore.add(chatId, ChatMessage("assistant", cleanAnswer))

        // Ответы LLM отправляем БЕЗ parse_mode: в тексте могут быть *, _, [ —
        // Markdown/HTML сломали бы отправку (400 от Telegram).
        TelegramClient.sendMessagePlain(chatId, cleanAnswer)
    }

    // ─── Лог неизвестных вопросов ────────────────────────────────────────────

    private fun logUnansweredQuestion(chatId: Long, question: String) {
        try {
            LOG_FILE.parentFile?.mkdirs()
            val timestamp = LocalDateTime.now().format(LOG_TIME_FORMAT)
            val safeQuestion = question.replace(Regex("\r?\n"), " ⏎ ")
            synchronized(LOG_FILE) {
                java.nio.file.Files.write(
                    LOG_FILE.toPath(),
                    "$timestamp | chat=$chatId | $safeQuestion\n".toByteArray(Charsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
                )
            }
            logger.info("Unanswered question logged (chat $chatId): $question")
        } catch (e: Exception) {
            logger.error("Failed to log unanswered question", e)
        }
    }
}
