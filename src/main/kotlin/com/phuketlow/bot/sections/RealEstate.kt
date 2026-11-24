package com.phuketlow.bot.sections

import com.google.gson.JsonArray
import com.phuketlow.bot.data.VillaStore
import com.phuketlow.bot.model.Villa
import com.phuketlow.bot.model.VillaFilter
import com.phuketlow.bot.state.ChatStates
import com.phuketlow.bot.telegram.Keyboard
import com.phuketlow.bot.telegram.TelegramClient
import org.slf4j.LoggerFactory

/**
 * Секция «Недвижимость»: аренда и покупка.
 * Данные — из real_estate.xlsx (лист rental).
 * Фото — по публичным URL (GitHub), Telegram скачивает сам.
 *
 * Аренда: экран фильтров (спальни / пляж / цена) → список страницами по 10.
 * Состояние диалога — в [ChatStates] (callback_data ограничен 64 байтами).
 */
object RealEstate {

    private val logger = LoggerFactory.getLogger("RealEstate")

    private const val PAGE_SIZE = 10

    /** Варианты цены (нижняя, верхняя) THB/мес для экрана фильтров. */
    private val PRICE_OPTIONS = listOf(
        0 to 30_000,        // до 30к
        30_000 to 50_000,   // от 30к до 50к
        50_000 to 100_000,  // от 50к до 100к
        100_000 to 0        // 100к+
    )

    /** Варианты спален: 1, 2, 3, 4+ (4 = «4 и более»). */
    private val BED_OPTIONS = listOf(1, 2, 3, 4)

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
                if (VillaStore.all().isEmpty()) {
                    SectionResponse(
                        """
🔑 *Аренда недвижимости*

Список пока пуст. Заполните `src/main/xlsx/real_estate.xlsx` (лист `rental`).
                        """.trimIndent(),
                        Keyboard.inline()
                    )
                } else {
                    // Вход в аренду — сразу экран фильтров
                    showFilters(chatId)
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

    // ─── Экран фильтров ──────────────────────────────────────────────────────

    fun showFilters(chatId: Long): SectionResponse {
        val state = ChatStates.get(chatId)
        val f = state.filter
        val beaches = VillaStore.uniqueBeaches()
        val count = VillaStore.filter(f).size

        val text = buildString {
            appendLine("🔑 *Аренда: фильтры*")
            appendLine()
            appendLine("🛏️ *Спален:* ${bedLabel(f.bedrooms)}")
            appendLine("🏖️ *Пляж:* ${f.beach ?: "любой"}")
            appendLine("💰 *Цена:* ${priceLabel(f.priceFrom, f.priceTo)}${if (f.priceFrom == 0 && f.priceTo == 0) "" else " THB/мес"}")
            appendLine()
            append("Найдено: *$count* объект(ов)")
        }

        val rows = mutableListOf<List<Pair<String, String>>>()

        // Спальни: Любое | 1 | 2 | 3 | 4+
        val bedRow = mutableListOf("Любое" to "f_bed_0")
        bedRow.addAll(BED_OPTIONS.map { n -> (if (n == 4) "4+" else n.toString()) to "f_bed_$n" })
        rows += bedRow

        // Пляж: Любой | <уникальные пляжи>
        val beachRow = mutableListOf("Любой" to "f_beach_-1")
        beaches.forEachIndexed { i, b -> beachRow += b to "f_beach_$i" }
        rows += beachRow

        // Цена: Любая | до 30к | 30–50к | 50–100к | 100к+
        val priceRow = mutableListOf("Любая" to "f_price_-1")
        PRICE_OPTIONS.forEachIndexed { i, (lo, hi) ->
            priceRow.add(priceRangeLabel(lo, hi) to "f_price_$i")
        }
        rows += priceRow

        // Действия
        rows += listOf("✅ Показать ($count)" to "rent_list")
        rows += listOf("↩️ Сбросить" to "rent_reset")

        return SectionResponse(text, Keyboard.rows(rows))
    }

    /** Применяет один критерий фильтра и перерисовывает экран фильтров. */
    fun applyFilterOption(chatId: Long, data: String): SectionResponse {
        val state = ChatStates.get(chatId)
        val f = state.filter
        val beaches = VillaStore.uniqueBeaches()

        val newFilter = when {
            data.startsWith("f_bed_") -> {
                val n = data.removePrefix("f_bed_").toIntOrNull() ?: 0
                f.copy(bedrooms = n)
            }
            data.startsWith("f_beach_") -> {
                val idx = data.removePrefix("f_beach_").toIntOrNull() ?: -1
                f.copy(beach = beaches.getOrNull(idx))
            }
            data.startsWith("f_price_") -> {
                val idx = data.removePrefix("f_price_").toIntOrNull() ?: -1
                val (lo, hi) = PRICE_OPTIONS.getOrNull(idx) ?: (0 to 0)
                f.copy(priceFrom = lo, priceTo = hi)
            }
            else -> f
        }

        ChatStates.update(chatId) { it.copy(filter = newFilter, page = 0) }
        return showFilters(chatId)
    }

    // ─── Список (пагинация) ──────────────────────────────────────────────────

    fun showList(chatId: Long, page: Int = 0): SectionResponse {
        val state = ChatStates.get(chatId)
        val filtered = VillaStore.filter(state.filter)

        if (filtered.isEmpty()) {
            return SectionResponse(
                """
🔑 *Аренда: ничего не найдено*

Попробуйте изменить или сбросить фильтры.
                """.trimIndent(),
                Keyboard.rows(
                    listOf(
                        listOf("🔍 Фильтры" to "rent_filters"),
                        listOf("↩️ Сбросить" to "rent_reset")
                    )
                )
            )
        }

        val pages = (filtered.size + PAGE_SIZE - 1) / PAGE_SIZE
        val p = page.coerceIn(0, pages - 1)
        val slice = filtered.subList(p * PAGE_SIZE, minOf((p + 1) * PAGE_SIZE, filtered.size))

        val text = buildString {
            appendLine("🔑 *Аренда: ${filtered.size} объект(ов)*")
            appendLine()
            if (!state.filter.isEmpty) {
                appendLine("Фильтры: ${filterSummary(state.filter)}")
                appendLine()
            }
            append("Стр. ${p + 1} из $pages")
        }

        val rows = slice.map { v ->
            listOf(villaButton(v) to "villa_${v.id}")
        }.toMutableList()

        // Навигация
        val nav = mutableListOf<Pair<String, String>>()
        if (p > 0) nav += "⬅️ Назад" to "rent_page_${p - 1}"
        if (p < pages - 1) nav += "Вперёд ➡️" to "rent_page_${p + 1}"
        if (nav.isNotEmpty()) rows += nav

        rows += listOf("🔍 Фильтры" to "rent_filters")

        return SectionResponse(text, Keyboard.rows(rows))
    }

    fun resetFilters(chatId: Long): SectionResponse {
        ChatStates.clear(chatId)
        return showFilters(chatId)
    }

    // ─── Помощники ───────────────────────────────────────────────────────────

    private fun villaButton(v: Villa): String {
        val price = if (v.priceFrom > 0) " · от ${v.priceFrom} THB/мес" else ""
        return "• ${v.name} · ${v.bedrooms} сп.$price"
    }

    private fun bedLabel(n: Int): String = when (n) {
        0 -> "любое"
        4 -> "4+"
        else -> n.toString()
    }

    private fun priceLabel(lo: Int, hi: Int): String =
        if (lo == 0 && hi == 0) "любая" else priceRangeLabel(lo, hi)

    private fun priceRangeLabel(lo: Int, hi: Int): String = when {
        lo == 0 && hi > 0 -> "до ${hi / 1000}к"
        hi == 0 -> "${lo / 1000}к+"
        else -> "${lo / 1000}–${hi / 1000}к"
    }

    private fun filterSummary(f: VillaFilter): String = buildList {
        if (f.bedrooms > 0) add("спален ${bedLabel(f.bedrooms)}")
        if (f.beach != null) add("пляж ${f.beach}")
        if (f.priceFrom > 0 || f.priceTo > 0) add("${priceLabel(f.priceFrom, f.priceTo)} THB")
    }.joinToString(", ")

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
            appendLine("🏖️ Пляж: ${v.beaches.joinToString(", ")}")
            if (v.priceFrom > 0) {
                appendLine("💰 От ${v.priceFrom} THB за месяц")
                appendLine("_Точная стоимость зависит от месяца — уточнит менеджер_")
            }
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
