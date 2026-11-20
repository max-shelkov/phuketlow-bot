package com.phuketlow.bot.knowledge

import org.slf4j.LoggerFactory
import java.io.File

/**
 * База знаний ИИ-поддержки.
 * Читает `src/main/knowledge/support_knowledge.md` один раз при старте бота.
 * Содержимое подставляется в system prompt: модель отвечает
 * исключительно на основе этих данных.
 */
object KnowledgeBase {

    private val logger = LoggerFactory.getLogger("KnowledgeBase")

    /** Пути к файлу базы знаний: локальный запуск и деплой (/opt/phuketlow-bot). */
    private val CANDIDATE_PATHS = listOf(
        "src/main/knowledge/support_knowledge.md",
        "knowledge/support_knowledge.md"
    )

    private var content: String = ""

    /** Загружает базу знаний в память. Вызывается один раз при старте. */
    fun load() {
        for (path in CANDIDATE_PATHS) {
            val file = File(path)
            if (file.exists()) {
                content = file.readText()
                logger.info("KnowledgeBase loaded: $path (${content.length} chars)")
                return
            }
        }
        content = ""
        logger.warn("KnowledgeBase: support_knowledge.md not found (tried: $CANDIDATE_PATHS) — support will refuse all questions")
    }

    /** Текст базы знаний (пустая строка, если файл не найден). */
    fun content(): String = content

    val isLoaded: Boolean
        get() = content.isNotBlank()
}
