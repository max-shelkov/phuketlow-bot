package com.phuketlow.bot.model

/**
 * Критерии фильтрации каталога аренды.
 * Значение «0» / «null» означает «без ограничения» по этому критерию.
 *
 * @param bedrooms количество спален: 0 = любое, 1/2/3 = точное, 4 = «4 и более»
 * @param beach    пляж, точное совпадение без учёта регистра (null = любой)
 * @param priceFrom нижняя граница цены «от» (THB/мес), 0 = без нижней границы
 * @param priceTo   верхняя граница цены «от» (THB/мес), 0 = без верхней границы
 */
data class VillaFilter(
    val bedrooms: Int = 0,
    val beach: String? = null,
    val priceFrom: Int = 0,
    val priceTo: Int = 0
) {
    val isEmpty: Boolean
        get() = bedrooms == 0 && beach == null && priceFrom == 0 && priceTo == 0
}
