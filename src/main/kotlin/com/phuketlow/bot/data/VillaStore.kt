package com.phuketlow.bot.data

import com.phuketlow.bot.model.Villa
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream

/**
 * Загружает данные о виллах из `real_estate.xlsx` (лист `rental`)
 * и отдаёт их боту из памяти.
 *
 * Таблица читается **по заголовкам** первой строки, поэтому перестановка
 * колонок не ломает бота. Ожидаемые заголовки:
 * `id`, `name`, `category`, `bedrooms`, `area`, `photo1`…`photo10`.
 */
object VillaStore {

    private val logger = LoggerFactory.getLogger("VillaStore")

    /** Относительный путь к xlsx (относительно каталога запуска — корня проекта). */
    private const val XLSX_PATH = "src/main/xlsx/real_estate.xlsx"

    /** Лист в workbook. */
    private const val SHEET_NAME = "rental"

    /**
     * Базовый URL публичного хостинга фото (GitHub raw).
     * Бот отправляет альбом по ссылкам — Telegram скачивает фото сам,
     * без загрузки с вашего ПК.
     */
    const val PHOTO_BASE_URL = "https://raw.githubusercontent.com/max-shelkov/phuketlow-bot-photos/main/"

    private const val MAX_PHOTOS = 10

    private var villas: List<Villa> = emptyList()

    /**
     * Публичная ссылка на фото по имени файла (из xlsx).
     * На хостинге все файлы .jpg — расширение нормализуем.
     */
    fun photoUrl(fileName: String): String {
        val normalized = if (fileName.endsWith(".png", ignoreCase = true))
            fileName.substringBeforeLast('.') + ".jpg" else fileName
        return PHOTO_BASE_URL + normalized
    }

    /** Инициализация: читает xlsx и сохраняет список в памяти. */
    fun init() {
        val xlsx = File(XLSX_PATH)
        if (!xlsx.exists()) {
            logger.warn("XLSX not found at $XLSX_PATH — section «Аренда» will be empty")
            return
        }

        try {
            FileInputStream(xlsx).use { stream ->
                WorkbookFactory.create(stream).use { wb ->
                    val sheet = wb.getSheet(SHEET_NAME)
                        ?: throw IllegalArgumentException("Лист «$SHEET_NAME» не найден в $XLSX_PATH")

                    val list = parseSheet(sheet)
                    villas = list
                    logger.info("Loaded ${list.size} villa(s) from $XLSX_PATH (sheet: $SHEET_NAME)")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to load villas from $XLSX_PATH", e)
            villas = emptyList()
        }
    }

    private fun parseSheet(sheet: org.apache.poi.ss.usermodel.Sheet): List<Villa> {
        val formatter = DataFormatter()

        // 1 строка — заголовки. Собираем map: header -> columnIndex
        val headerRow = sheet.getRow(0) ?: return emptyList()
        val headerIndex = mutableMapOf<String, Int>()
        for (c in 0 until headerRow.lastCellNum) {
            val cell = headerRow.getCell(c) ?: continue
            val header = formatter.formatCellValue(cell).trim()
            if (header.isNotEmpty()) headerIndex[header.lowercase()] = c
        }

        val idCol          = headerIndex["id"]       ?: return emptyList<Villa>().also { logger.warn("Нет колонки «id»") }
        val nameCol        = headerIndex["name"]     ?: 0
        val categoryCol    = headerIndex["category"] ?: 0
        val bedroomsCol    = headerIndex["bedrooms"] ?: 0
        val areaCol        = headerIndex["area"]     ?: 0
        val descriptionCol = headerIndex["description"] ?: -1
        val photoCols      = (1..MAX_PHOTOS).mapNotNull { headerIndex["photo$it"] }

        val result = mutableListOf<Villa>()
        val lastRow = sheet.lastRowNum

        for (r in 1..lastRow) {
            val row = sheet.getRow(r) ?: continue

            val id = text(row, idCol, formatter)
            if (id.isEmpty()) continue // пустая строка

            val name        = text(row, nameCol, formatter).ifEmpty { id }
            val category    = text(row, categoryCol, formatter).ifEmpty { "Вилла" }
            val bedrooms    = int(row, bedroomsCol, formatter)
            val area        = text(row, areaCol, formatter).ifEmpty { "Пхукет" }
            val description = if (descriptionCol >= 0) text(row, descriptionCol, formatter) else ""
            val photos      = photoCols
                .map { col -> text(row, col, formatter) }
                .filter { it.isNotBlank() }
                .take(MAX_PHOTOS)

            result += Villa(id, name, category, bedrooms, area, description, photos)
        }

        return result
    }

    // --- helpers ---

    private fun text(row: Row, col: Int, fmt: DataFormatter): String =
        cell(row, col)?.let { fmt.formatCellValue(it).trim() } ?: ""

    private fun int(row: Row, col: Int, fmt: DataFormatter): Int =
        cell(row, col)?.let {
            val type = it.cellType
            when (type) {
                CellType.NUMERIC -> it.getNumericCellValue().toInt()
                CellType.STRING -> fmt.formatCellValue(it).trim().toIntOrNull() ?: 0
                else -> fmt.formatCellValue(it).trim().toIntOrNull() ?: 0
            }
        } ?: 0

    private fun cell(row: Row, col: Int): Cell? = row.getCell(col)

    // --- API для бота ---

    fun all(): List<Villa> = villas

    fun findById(id: String): Villa? = villas.find { it.id == id }
}
