package com.phuketlow.bot.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.phuketlow.bot.config.Config
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Клиент LLM через OpenAI-совместимый API (new-api).
 * POST {AI_BASE_URL}/v1/chat/completions
 */
object AiClient {

    private val logger = LoggerFactory.getLogger("AiClient")
    private val gson = Gson()

    // LLM может думать долго — отдельный клиент с большим read timeout
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    data class ChatMessage(val role: String, val content: String)

    /**
     * Отправляет историю диалога модели и возвращает её ответ.
     * @throws AiException при ошибке API или сети
     */
    fun chat(messages: List<ChatMessage>): String {
        val apiKey = Config.AI_API_KEY
        if (apiKey.isNullOrBlank()) {
            throw AiException("AI_API_KEY не задан")
        }

        val body = JsonObject().apply {
            addProperty("model", Config.AI_MODEL)
            add("messages", JsonArray().apply {
                messages.forEach { m ->
                    add(JsonObject().apply {
                        addProperty("role", m.role)
                        addProperty("content", m.content)
                    })
                }
            })
        }

        val url = Config.AI_BASE_URL.trimEnd('/') + "/v1/chat/completions"
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $apiKey")
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: throw AiException("Пустой ответ от LLM API")
            if (!response.isSuccessful) {
                logger.error("LLM API error: ${response.code} — ${responseBody.take(500)}")
                throw AiException("LLM API вернул ${response.code}")
            }
            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val content = json.getAsJsonArray("choices")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("message")
                ?.get("content")?.asString
            if (content.isNullOrBlank()) {
                throw AiException("LLM вернула пустой ответ")
            }
            return content.trim()
        }
    }

    class AiException(message: String) : Exception(message)
}
