package com.phuketlow.bot.telegram

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.phuketlow.bot.config.Config
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Единая точка доступа к Telegram Bot API.
 * Все HTTP-запросы проходят через этот объект.
 */
object TelegramClient {

    private val logger = LoggerFactory.getLogger("TelegramClient")
    private val gson = Gson()

    private const val POLL_TIMEOUT_SECONDS = 30

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout((POLL_TIMEOUT_SECONDS + 30).toLong(), TimeUnit.SECONDS)
        .build()

    val apiBase: String
        get() = "https://api.telegram.org/bot${Config.TOKEN}"

    // ─── Низкоуровневый запрос ───────────────────────────────────────────────

    fun sendRequest(method: String, body: String?): JsonElement? {
        val url = "$apiBase/$method"
        return try {
            val request = if (body != null) {
                Request.Builder()
                    .url(url)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
            } else {
                Request.Builder().url(url).get().build()
            }

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: return null
                if (response.isSuccessful) {
                    gson.fromJson(responseBody, JsonElement::class.java)
                } else {
                    logger.warn("API error: ${response.code} — ${responseBody.take(500)}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Request failed: $url", e)
            null
        }
    }

    // ─── Высокоуровневые методы ──────────────────────────────────────────────

    fun getMe(): String? {
        val result = sendRequest("getMe", null)
        return result?.asJsonObject?.get("result")?.asJsonObject?.get("username")?.asString
    }

    fun getUpdates(offset: Int, timeout: Int = POLL_TIMEOUT_SECONDS): JsonElement? {
        val body = com.google.gson.JsonObject().apply {
            addProperty("offset", offset)
            addProperty("timeout", timeout)
        }
        return sendRequest("getUpdates", body.toString())
    }

    fun sendMessage(chatId: Long, text: String, parseMode: String = "Markdown") {
        val body = com.google.gson.JsonObject().apply {
            addProperty("chat_id", chatId)
            addProperty("text", text)
            addProperty("parse_mode", parseMode)
        }
        sendRequest("sendMessage", body.toString())
    }

    fun sendMessageWithMarkup(chatId: Long, text: String, replyMarkup: com.google.gson.JsonElement, parseMode: String = "Markdown") {
        val body = com.google.gson.JsonObject().apply {
            addProperty("chat_id", chatId)
            addProperty("text", text)
            addProperty("parse_mode", parseMode)
            add("reply_markup", replyMarkup)
        }
        sendRequest("sendMessage", body.toString())
    }

    fun sendMediaGroup(chatId: Long, media: com.google.gson.JsonArray) {
        val body = com.google.gson.JsonObject().apply {
            addProperty("chat_id", chatId)
            add("media", media)
        }
        val result = sendRequest("sendMediaGroup", body.toString())
        if (result != null && result.asJsonObject.get("ok").asBoolean) {
            logger.info("sendMediaGroup OK: ${media.size()} item(s)")
        } else {
            logger.warn("sendMediaGroup FAILED: ${result?.toString()?.take(400)}")
        }
    }

    fun sendChatAction(chatId: Long, action: String = "typing") {
        val body = com.google.gson.JsonObject().apply {
            addProperty("chat_id", chatId)
            addProperty("action", action)
        }
        try {
            sendRequest("sendChatAction", body.toString())
        } catch (_: Exception) {
            // Не критично — просто индикатор
        }
    }

    fun answerCallbackQuery(callbackId: String) {
        val body = com.google.gson.JsonObject().apply {
            addProperty("callback_query_id", callbackId)
        }
        sendRequest("answerCallbackQuery", body.toString())
    }

    fun setChatMenuButton(chatId: Long, text: String = "Меню") {
        val body = com.google.gson.JsonObject().apply {
            addProperty("chat_id", chatId)
            add("menu_button", com.google.gson.JsonObject().apply {
                addProperty("type", "text")
                addProperty("text", text)
            })
        }
        val result = sendRequest("setChatMenuButton", body.toString())
        if (result != null && !result.asJsonObject.get("ok").asBoolean) {
            logger.warn("Failed to set menu button for chat $chatId: $result")
        }
    }
}
