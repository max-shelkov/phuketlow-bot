package com.phuketlow.bot.sections

import com.google.gson.JsonArray
import com.phuketlow.bot.data.VillaStore
import com.phuketlow.bot.model.Villa
import com.phuketlow.bot.telegram.Keyboard
import com.phuketlow.bot.telegram.TelegramClient
import org.slf4j.LoggerFactory

/**
 * Секция «Недвижимость»: аренда и покупка.
 * Данные — из real_estate.xlsx (лист rental).
 * Фото — по публичным URL (GitHub), Telegram скачивает сам.
 */
object RealEstate {

    private val logger = LoggerFactory.getLogger("RealEstate")

    // ─── Обработка callback'ей секции ────────────────────────────────────────

    fun handleCallback(data: String, chatId: Long): SectionResponse {
        return when (data) {
            "real_estate" -> SectionResponse(
                """
🏠 *Недвижимость на Пхукете*

Выберите, что вы хотите сделать:
                """.trimIndent(),
                Keyboard.inline(
                    listOf(
                        "🔑 Арендовать" to "real_estate_rent",
                        "💰 Купить"     to "real_estate_buy"
                    )
                )
            )

            "real_estate_rent" -> {
                val list = VillaStore.all()
                if (list.isEmpty()) {
                    SectionResponse(
                        """
🔑 *Аренда недвижимости*

Список пока пуст. Заполните `src/main/xlsx/real_estate.xlsx` (лист `rental`).
                        """.trimIndent(),
                        Keyboard.inline()
                    )
                } else {
                    SectionResponse(
                        """
🔑 *Аренда: виллы и квартиры*

Выберите объект:
                        """.trimIndent(),
                        Keyboard.inline(
                            list.map { v ->
                                "• ${v.name} · ${v.area} · ${v.bedrooms} сп." to "villa_${v.id}"
                            }
                        )
                    )
                }
            }

            "real_estate_buy" -> SectionResponse(
                """
💰 *Покупка недвижимости*

_Каталог в разработке…_
                """.trimIndent(),
                Keyboard.inline()
            )

            else -> SectionResponse("❓ Неизвестная команда", Keyboard.inline())
        }
    }

    // ─── Карточка виллы (альбом фото + описание) ────────────────────────────

    fun showVillaCard(chatId: Long, villaId: String) {
        val v = VillaStore.findById(villaId)
        if (v == null) {
            TelegramClient.sendMessageWithMarkup(
                chatId, "❌ Вилла не найдена (id=$villaId)", Keyboard.inline()
            )
            return
        }

        // Фото — по публичным ссылкам (GitHub). Telegram скачивает их сам.
        val urls = v.photos.map { name -> VillaStore.photoUrl(name) }
        logger.info("Villa ${v.name}: ${v.photos.size} photos in xlsx, ${urls.size} URL(s) ready")

        // Подпись для альбома (Markdown)
        val caption = buildString {
            appendLine("🏝️ *${v.name}*")
            appendLine()
            if (v.description.isNotBlank()) {
                appendLine(v.description)
                appendLine()
            }
            appendLine("🏷️ Категория: ${v.category}")
            appendLine("🛏️ Спален: ${v.bedrooms}")
            appendLine("📍 Район: ${v.area}")
            appendLine()
            append("📷 Фото: ${urls.size}")
        }

        if (urls.isNotEmpty()) {
            TelegramClient.sendChatAction(chatId, "upload_photo")
            sendPhotoAlbum(chatId, urls, caption)
        } else {
            TelegramClient.sendMessage(chatId, caption)
        }
    }

    // ─── Отправка альбома по URL ─────────────────────────────────────────────

    private fun sendPhotoAlbum(chatId: Long, urls: List<String>, caption: String) {
        val photos = urls.take(10)
        logger.info("sendMediaGroup (URL): sending ${photos.size} photo(s) to chat $chatId")

        val array = JsonArray()
        photos.forEachIndexed { i, url ->
            val obj = com.google.gson.JsonObject()
            obj.addProperty("type", "photo")
            obj.addProperty("media", url)
            if (i == 0 && caption.isNotEmpty()) obj.addProperty("caption", caption)
            array.add(obj)
        }

        TelegramClient.sendMediaGroup(chatId, array)
    }
}
