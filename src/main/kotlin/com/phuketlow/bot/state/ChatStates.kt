package com.phuketlow.bot.state

import com.phuketlow.bot.model.VillaFilter
import java.util.concurrent.ConcurrentHashMap

/**
 * Состояние диалога аренды для одного чата: текущий фильтр и страница.
 * Хранится в памяти процесса — при рестарте бота сбрасывается (допустимо).
 */
data class RentalState(
    val filter: VillaFilter = VillaFilter(),
    val page: Int = 0,
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun touch() {
        updatedAt = System.currentTimeMillis()
    }
}

/**
 * Реестр состояний по chatId.
 *
 * Состояние не кодируется в callback_data (лимит Telegram — 64 байта),
 * поэтому живёт здесь, а в кнопках — только короткие ключи
 * (`f_bed_2`, `rent_page_3` и т.п.).
 *
 * Записи старше [TTL_MS] удаляются лениво — при каждом обращении.
 */
object ChatStates {

    private const val TTL_MS = 30 * 60 * 1000L // 30 минут

    private val rental = ConcurrentHashMap<Long, RentalState>()

    /** Возвращает актуальное состояние (или новое), обновляет TTL. */
    fun get(chatId: Long): RentalState {
        purge()
        val s = rental.getOrPut(chatId) { RentalState() }
        s.touch()
        return s
    }

    fun update(chatId: Long, block: (RentalState) -> RentalState) {
        val s = block(get(chatId))
        s.touch()
        rental[chatId] = s
    }

    fun clear(chatId: Long) {
        rental.remove(chatId)
    }

    /** Ленивая очистка протухших записей. */
    private fun purge() {
        val now = System.currentTimeMillis()
        rental.entries.removeIf { (now - it.value.updatedAt) > TTL_MS }
    }
}
