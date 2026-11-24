package com.phuketlow.bot.model

/**
 * Одна вилла / квартира из таблицы real_estate.xlsx (лист rental).
 *
 * @param id         идентификатор из колонки `id` (используется в callback_data)
 * @param name       название из колонки `name` (например, «Sea House»)
 * @param category   колонка `category` («Вилла» / «Квартира»)
 * @param bedrooms   колонка `bedrooms` (количество спален/спальных мест)
 * @param beaches    колонка `beach` — один или несколько пляжей,
 *                   разделённых запятыми (например, «Патонг, Карон»)
 * @param priceFrom  колонка `price_from` — минимальная цена (в месяц),
 *                   0 = не указана. Точная цена зависит от месяца —
 *                   уточняется менеджером.
 * @param photos     имена файлов из колонок `photo1..photo10` (порядок сохранён),
 *                   пустые ячейки пропускаются
 */
data class Villa(
    val id: String,
    val name: String,
    val category: String,
    val bedrooms: Int,
    val beaches: List<String>,
    val priceFrom: Int,
    val description: String,
    val photos: List<String>
)
