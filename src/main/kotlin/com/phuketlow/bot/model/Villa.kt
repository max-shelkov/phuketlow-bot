package com.phuketlow.bot.model

/**
 * Одна вилла / квартира из таблицы real_estate.xlsx (лист rental).
 *
 * @param id         идентификатор из колонки `id` (используется в callback_data)
 * @param name       название из колонки `name` (например, «Sea House»)
 * @param category   колонка `category` («Вилла» / «Квартира»)
 * @param bedrooms   колонка `bedrooms` (количество спален/спальных мест)
 * @param area       колонка `area` (район Пхукета)
 * @param photos     имена файлов из колонок `photo1..photo10` (порядок сохранён),
 *                   пустые ячейки пропускаются
 */
data class Villa(
    val id: String,
    val name: String,
    val category: String,
    val bedrooms: Int,
    val area: String,
    val description: String,
    val photos: List<String>
)
