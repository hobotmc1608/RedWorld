package com.example.data.local

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Represents a parsed cell inside an XLSX sheet, preserving cell coordinate (e.g. A1),
 * raw value, formula (if any), and style attribute index.
 */
@Serializable
data class XlsxCellData(
    val cellRef: String,      // e.g., "A1", "B2"
    val rowNumber: Int,
    val colIndex: Int,
    val displayValue: String,
    val formula: String = "", // Preserved <f> tag
    val styleIndex: String = "" // Preserved s="..." attribute
)

/**
 * Represents a worksheet inside an XLSX file, preserving its original name, sheetId,
 * entry path in the ZIP archive, merged cell ranges, and rows.
 */
@Serializable
data class XlsxSheetData(
    val sheetName: String,
    val sheetIndex: Int,
    val zipEntryPath: String,
    val mergedRanges: List<String>,
    val cells: List<XlsxCellData>,
    val hasComplexDrawingsOrMacros: Boolean = false
)

/**
 * Represents a full XLSX workbook loaded in memory with its raw ZIP bytes preserved
 * for surgical in-place cell editing without losing formatting, formulas, or merged cells.
 */
data class XlsxWorkbookModel(
    val fileName: String,
    val localFilePath: String,
    val sheets: List<XlsxSheetData>,
    val warnings: List<String>
)

@Serializable
data class CellDiffItem(
    val sheetName: String,
    val cellRef: String,
    val oldValue: String,
    val newValue: String,
    val formulaPreserved: Boolean
)

/**
 * Handles document text extraction (PDF, TXT, MD, DOCX, XLSX), RAG chunking,
 * and surgical XLSX read/write operations with backup & diff verification.
 */
object DocumentAndXlsxEngine {

    /**
     * Splits document text into overlapping semantic chunks for RAG retrieval.
     */
    fun chunkDocumentText(
        documentId: Long,
        rawText: String,
        targetChunkChars: Int = 550
    ): List<DocumentChunkEntity> {
        if (rawText.isBlank()) return emptyList()
        val lines = rawText.lines()
        val chunks = mutableListOf<DocumentChunkEntity>()
        var currentHeading = "Введение"
        val buffer = StringBuilder()
        var chunkIdx = 0
        var offsetCursor = 0
        var chunkStartOffset = 0

        fun flushBuffer() {
            val text = buffer.toString().trim()
            if (text.isNotEmpty()) {
                chunks.add(
                    DocumentChunkEntity(
                        documentId = documentId,
                        chunkIndex = chunkIdx++,
                        sectionTitle = currentHeading,
                        chunkText = text,
                        startOffset = chunkStartOffset,
                        endOffset = offsetCursor
                    )
                )
            }
            buffer.clear()
            chunkStartOffset = offsetCursor
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#")) {
                flushBuffer()
                currentHeading = trimmed.trimStart('#').trim().ifEmpty { currentHeading }
            }
            buffer.appendLine(line)
            offsetCursor += line.length + 1
            if (buffer.length >= targetChunkChars) {
                flushBuffer()
            }
        }
        flushBuffer()
        return chunks
    }

    /**
     * Extracts readable text from URI based on detected format (DOC, DOCX, PDF, TXT, MD, XLSX, RTF, or any text/binary document).
     */
    suspend fun extractTextFromUri(
        context: Context,
        uri: Uri,
        fileName: String
    ): Pair<DocumentFormat, String> = withContext(Dispatchers.IO) {
        val lower = fileName.lowercase()
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext DocumentFormat.TXT to ""

        val isZipHeader = bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

        val format = when {
            lower.endsWith(".pdf") -> DocumentFormat.PDF
            lower.endsWith(".xlsx") -> DocumentFormat.XLSX
            lower.endsWith(".docx") || lower.endsWith(".doc") -> DocumentFormat.DOCX
            lower.endsWith(".md") || lower.endsWith(".markdown") -> DocumentFormat.MARKDOWN
            else -> DocumentFormat.TXT
        }

        val text = when {
            format == DocumentFormat.PDF -> {
                extractTextOrMetadataFromPdf(context, bytes, fileName)
            }
            format == DocumentFormat.XLSX -> {
                val destFile = File(context.filesDir, "imported_${System.currentTimeMillis()}_$fileName")
                destFile.writeBytes(bytes)
                val workbook = parseXlsxFile(destFile)
                buildString {
                    appendLine("# Таблица XLSX: $fileName")
                    workbook.sheets.forEach { sheet ->
                        appendLine("\n## Лист: ${sheet.sheetName}")
                        if (sheet.mergedRanges.isNotEmpty()) {
                            appendLine("Объединённые диапазоны: ${sheet.mergedRanges.joinToString(", ")}")
                        }
                        val rows = sheet.cells.groupBy { it.rowNumber }.toSortedMap()
                        rows.forEach { (rowNum, cells) ->
                            val line = cells.sortedBy { it.colIndex }.joinToString(" | ") { cell ->
                                if (cell.formula.isNotEmpty()) "${cell.cellRef}: ${cell.displayValue} [=${cell.formula}]"
                                else "${cell.cellRef}: ${cell.displayValue}"
                            }
                            appendLine("Строка $rowNum: $line")
                        }
                    }
                }
            }
            lower.endsWith(".docx") || (lower.endsWith(".doc") && isZipHeader) -> {
                extractTextFromDocxBytes(bytes)
            }
            lower.endsWith(".doc") || lower.endsWith(".rtf") -> {
                extractTextFromLegacyDocOrRtfBytes(bytes)
            }
            else -> {
                String(bytes, StandardCharsets.UTF_8)
            }
        }
        format to text
    }

    private fun extractTextFromDocxBytes(bytes: ByteArray): String {
        val sb = StringBuilder()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    if (entry?.name == "word/document.xml") {
                        val parser = XmlPullParserFactory.newInstance().newPullParser()
                        parser.setInput(InputStreamReader(zis, StandardCharsets.UTF_8))
                        var eventType = parser.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG && parser.name == "t") {
                                sb.append(parser.nextText())
                            } else if (eventType == XmlPullParser.END_TAG && parser.name == "p") {
                                sb.append("\n")
                            }
                            eventType = parser.next()
                        }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            return extractTextFromLegacyDocOrRtfBytes(bytes)
        }
        val result = sb.toString().trim()
        return if (result.isNotEmpty()) result else extractTextFromLegacyDocOrRtfBytes(bytes)
    }

    /**
     * Extracts readable Russian/English text from legacy MS Word .doc (OLE2 binary, Word XML, RTF, or plain text with .doc extension).
     */
    private fun extractTextFromLegacyDocOrRtfBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val utf8Candidate = String(bytes, StandardCharsets.UTF_8)
        // Check if it is plain UTF-8 text or Markdown saved with .doc extension
        val printableRatio = utf8Candidate.count { it.isLetterOrDigit() || it.isWhitespace() || it in ".,:;!?-—()«»\"'/#" }.toFloat() /
            utf8Candidate.length.coerceAtLeast(1)
        if (printableRatio > 0.82f && !utf8Candidate.startsWith("{\\rtf")) {
            return utf8Candidate.trim()
        }
        // Check if RTF
        if (utf8Candidate.startsWith("{\\rtf")) {
            return utf8Candidate
                .replace(Regex("""\\par\b"""), "\n")
                .replace(Regex("""\\[a-zA-Z]+\d*\s?"""), "")
                .replace(Regex("""[{}]"""), "")
                .trim()
        }
        // Binary .doc (OLE2) often stores Unicode text in UTF-16LE and/or UTF-8 chunks
        val utf16Candidate = try {
            String(bytes, StandardCharsets.UTF_16LE)
        } catch (_: Exception) {
            ""
        }
        val runs16 = Regex("""[А-Яа-яЁёA-Za-z0-9\s.,:;!?\-—()«»"'\n]{6,}""")
            .findAll(utf16Candidate)
            .map { it.value.trim() }
            .filter { run -> run.count { it.isLetter() } >= 4 }
            .toList()
        if (runs16.sumOf { it.length } > 40) {
            return runs16.joinToString("\n")
        }
        val runs8 = Regex("""[А-Яа-яЁёA-Za-z0-9\s.,:;!?\-—()«»"'\n]{6,}""")
            .findAll(utf8Candidate)
            .map { it.value.trim() }
            .filter { run -> run.count { it.isLetter() } >= 4 }
            .toList()
        return runs8.joinToString("\n").trim()
    }

    /**
     * Built-in sample .DOC character dossiers so the user can test 1-click AI extraction and
     * automatic relationship linking (e.g. Marcus Vane <-> Victor Corvin & Elena Rostova & Lidia Morgan).
     */
    fun getSampleCharacterDocDossiers(): List<Pair<String, String>> = listOf(
        "Маркус_Вейн_Досье.doc" to """
            ДОСЬЕ ПЕРСОНАЖА REDWORLD (Формат DOC)
            Имя: Маркус Вейн
            Полное имя: Маркус Эдвард Вейн
            Псевдонимы: Часовщик Зелёной улицы, Старый Маркус
            Роль в сюжете: Союзник
            Статус персонажа: Активен
            Фракция: Гильдия независимых мастеров Зелёной улицы
            Теги: Магазин, Артефакты, Часовщик, Нейтралитет, Информатор
            Пол: Мужской
            Происхождение: Человек
            Краткое описание: Владелец антикварного и технического Магазина на Зелёной улице, главный эксперт по механизмам и старинным артефактам.
            Подробная биография: Потомственный часовщик и инженер из Северного порта. Перебрался на Зелёную улицу двадцать лет назад и превратил свой антикварный магазин в нейтральную гавань для искателей и хранителей тайн.
            Дата рождения: 22 августа 1974
            Возраст: 51
            Место рождения: Северный порт
            Работа: Владелец антикварного и технического Магазина на Зелёной улице
            Внешность: Коренастый мужчина с седыми висками, внимательным прищуром и спокойной осанкой мастера.
            Телосложение: Плотное, крепкое
            Волосы: Тёмно-русые с густой благородной сединой
            Глаза: Изумрудно-зелёные за тонкими линзами очков
            Голос: Глуховатый баритон с лёгким металлическим тембром и размеренными паузами
            Одежда: Твидовый жилет цвета тёмной бронзы, кожаный фартук часовщика, латунный хронометр на цепочке.
            Особые приметы: Следы тонких ожогов от пайки на пальцах правой руки, антикварный перстень с рубиновой гранью.
            Характер: Прагматичный, рассудительный, ценит точность сделок и хранит чужие секреты.
            Архетип: Мудрый ремесленник / Хранитель артефактов
            Привычки: Протирает оптические линзы бархатной салфеткой во время сложных переговоров.
            Манера речи: Неторопливая, с использованием технических и часовых метафор.
            Характерные фразы: «У каждого механизма свой запас хода»; «На Зелёной улице платят точностью, а не обещаниями».
            Навыки: Реставрация редких механизмов, экспертиза артефактов, дешифровка старых носителей данных.
            Сильные стороны: Безупречная память на клейма мастеров, хладнокровие, широкая сеть поставщиков.
            Слабые стороны: Старая травма колена, недоверие к быстрым сделкам без залога.
            Инвентарь: Латунный хронометр, набор ювелирных отмычек, журнал залогов, часовая лупа с рубиновым фильтром.
            Интересы: Довоенная картография, акустические пластинки, редкие сорта чая.
            Предпочтения: Улун в фарфоровой пиале, порядок на верстаке в задней комнате Магазина.
            Не любит: Шумных визитёров, поддельные печати и попытки торговаться после удара по рукам.
            Цели: Восстановить утраченный механизм башенных часов Красного особняка и защитить нейтралитет Зелёной улицы.
            Мотивация: Сохранение ремесленного наследия старого квартала.
            Внутренний конфликт: Знает, что его старый чертёж может открыть нижний свод архива, но опасается последствий.
            Страхи и ограничения: Опасается пожара в мастерской; связан клятвой гильдии не раскрывать имена заказчиков.
            Известные факты: Знает устройство замков Красного особняка и тайные поставки между Магазином и Баром.
            Личные тайны: Хранит в двойном дне сейфа дубликат шестерни башенных часов Виктора Корвина.
            Чего персонаж не знает: Не знает истинного содержания зашифрованного дневника Елены Ростовой.
            Поведение: Никогда не продаёт опасные артефакты случайным посетителям без проверки через архив.
            Локация сейчас: Магазин
            Состояние сейчас: Разбирает шифрованный тубус в мастерской Магазина
            Здоровье сейчас: В норме
            Текущая одежда: Рабочий жилет и часовая лупа
            Активная цель: Передать расшифрованную схему Виктору Корвину и предупредить Елену Ростову
            Паспорт внешности: Portrait of Marcus Vane, 51 year old master watchmaker and antique shop owner, silver-streaked dark hair, emerald green eyes behind thin brass spectacles, wearing a dark bronze tweed vest and leather craftsman apron, warm antique shop lighting with brass gears and ruby glass reflections, RedWorld gothic noir concept art.
            Промпт 1 [Портрет | Портрет в антикварной лавке]: Portrait of Marcus Vane behind the glass counter of the Green Street Antique Shop, brass spectacles, emerald eyes, holding an intricate ruby chronometer, moody crimson and amber lighting, 8k concept art
            Промпт 2 [Иллюстрации игровых сцен | Тайная экспертиза в мастерской]: Cinematic scene of Marcus Vane in the backroom workshop of the Shop on Green Street, examining a cipher cylinder under a magnifying lamp, shelves of rare artifacts
            Связь: Виктор Корвин | CHARACTER | Знаком с | Давний партнёр и консультант Хранителя архива Красного особняка по старинным механизмам
            Связь: Елена Ростова | CHARACTER | Друг | Снабжает Елену редким снаряжением, плёнкой и сведениями о посетителях Зелёной улицы
            Связь: Лидия Морган | CHARACTER | Коллега | Поставляет редкие музыкальные пластинки и закрытые письма для хозяйки Бара «Алый Бархат»
            Связь: Магазин | LOCATION | Работает в | Владелец и главный мастер антикварного магазина на Зелёной улице
        """.trimIndent(),
        "Лидия_Морган_Досье.doc" to """
            ДОСЬЕ ПЕРСОНАЖА REDWORLD (Формат DOC)
            Имя: Лидия Морган
            Полное имя: Лидия Виктория Морган
            Псевдонимы: Хозяйка «Алого Бархата», Мадам Лидия
            Роль в сюжете: Ключевой персонаж
            Статус персонажа: Активен
            Фракция: Клуб «Алый Бархат» (Зелёная улица)
            Теги: Бар, Дипломатия, Нейтралитет, Информационная сеть
            Пол: Женский
            Происхождение: Человек
            Краткое описание: Владелица и управляющая ночного Бара «Алый Бархат» на Зелёной улице, хранительница негласного перемирия.
            Подробная биография: Выросла в Южном квартале, начинала как певица и организатор закрытых вечеров, а затем выкупила Бар на Зелёной улице, сделав его главной дипломатической площадкой города.
            Дата рождения: 19 февраля 1990
            Возраст: 35
            Место рождения: Южный квартал
            Работа: Владелица и управляющая ночного Бара «Алый Бархат» на Зелёной улице
            Внешность: Элегантная высокая женщина с аристократичной осанкой и бархатным голосом.
            Телосложение: Стройное, грациозное
            Волосы: Тёмно-каштановые, уложенные мягкими волнами
            Глаза: Тёмно-карие с золотистой искрой
            Голос: Грудной, мягкий, с певучими джазовыми интонациями
            Одежда: Вечернее платье цвета тёмного бордо, серебряный браслет и брошь с гранатом.
            Особые приметы: Родинка у левого уголка губ, гранатовая брошь-ключ.
            Характер: Дипломатичная, проницательная, умеет гасить конфликты одним взглядом.
            Архетип: Хозяйка салона / Теневой дипломат
            Привычки: Ставит джазовый винил перед закрытием Бара, ведёт тайный реестр гостей.
            Манера речи: Мягкая, обволакивающая, но не допускающая возражений при нарушении правил Бара.
            Характерные фразы: «Оружие и обиды остаются у гардероба»; «В Алом Бархате каждый бокал помнит своего гостя».
            Навыки: Дипломатия, сбор неформальной информации, управление заведением, психология.
            Сильные стороны: Чтение невербальных сигналов, абсолютный авторитет среди завсегдатаев Бара.
            Слабые стороны: Зависимость безопасности Бара от соблюдения перемирия между фракциями.
            Инвентарь: Гранатовая брошь-ключ от VIP-ложи, книга резервов в кожаном переплёте, коллекционный винил.
            Интересы: Джазовые пластинки, редкие коктейли, история семей Красного особняка.
            Предпочтения: Гранатовый аперитив, полумрак угловой ложи Бара.
            Не любит: Нарушителей кодекса нейтралитета и непрошеных соглядатаев.
            Цели: Удержать правило абсолютного нейтралитета в Баре «Алый Бархат».
            Мотивация: Независимость и защита информационной сети Зелёной улицы.
            Внутренний конфликт: Вынуждена балансировать между интересами Красного особняка и независимыми жителями улицы.
            Страхи и ограничения: Не может открыто занять ничью сторону, иначе Бар потеряет статус нейтральной территории.
            Известные факты: Знает расписание закрытых встреч в VIP-ложе и должников Зелёной улицы.
            Личные тайны: Хранит записи разговоров из третьей кабинки за последние три года.
            Чего персонаж не знает: Не знает, кто именно отправил анонимное приглашение в Красный особняк.
            Поведение: Защищает гостей своего заведения, но строго наказывает за нарушение перемирия.
            Локация сейчас: Бар
            Состояние сейчас: Встречает вечерних гостей у стойки Бара «Алый Бархат»
            Здоровье сейчас: В норме
            Текущая одежда: Бордовое бархатное платье с гранатовой брошью
            Активная цель: Обсудить с Маркусом Вейном и Еленой Ростовой появление незнакомцев на Зелёной улице
            Паспорт внешности: Portrait of Lidia Morgan, 35 year old owner of the Crimson Velvet Bar, dark wavy chestnut hair, warm dark brown eyes with golden flecks, wearing an elegant deep burgundy velvet evening dress and garnet brooch, moody jazz bar lighting, noir concept art.
            Промпт 1 [Портрет | Хозяйка Бара Алый Бархат]: Portrait of Lidia Morgan standing by a vintage vinyl turntable in the Crimson Velvet Bar, deep burgundy velvet dress, garnet brooch, warm amber and crimson neon reflections
            Связь: Маркус Вейн | CHARACTER | Коллега | Обменивается редкими заказами и новостями Зелёной улицы с владельцем Магазина
            Связь: Елена Ростова | CHARACTER | Знаком с | Предоставляет Елене звукоизолированную кабинку в Баре для встреч с информаторами
            Связь: Виктор Корвин | CHARACTER | Знаком с | Соблюдает негласный пакт о нейтралитете с Хранителем Красного особняка
            Связь: Бар | LOCATION | Работает в | Хозяйка и хранительница нейтралитета Бара «Алый Бархат»
        """.trimIndent()
    )

    /**
     * Parses any raw document text (from .doc, .docx, .pdf, .txt, .md, .xlsx) into a structured
     * [AiExtractedCharacterDossier] including permanent lore, current state, visual passport,
     * saved prompts, and declared relationships for automatic linking.
     */
    fun parseCharacterDossierLocally(rawText: String, fileName: String): AiExtractedCharacterDossier {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        fun findValue(vararg keys: String): String {
            for (line in lines) {
                for (k in keys) {
                    val regex = Regex("""^[-*•]?\s*${Regex.escape(k)}\s*[:—\-]\s*(.+)$""", RegexOption.IGNORE_CASE)
                    val m = regex.find(line)
                    if (m != null) return m.groupValues[1].trim()
                }
            }
            return ""
        }

        val extractedName = findValue("Имя", "Имя персонажа", "ФИО", "Персонаж", "Name")
            .ifEmpty {
                lines.firstOrNull { !it.contains("ДОСЬЕ", ignoreCase = true) && !it.startsWith("#") && it.length in 3..50 }
                    ?: fileName.substringBeforeLast('.').replace('_', ' ')
            }

        val savedPrompts = mutableListOf<CharacterSavedPrompt>()
        val declaredRels = mutableListOf<DeclaredCharacterRelation>()

        for (line in lines) {
            if (line.startsWith("Промпт", ignoreCase = true) || line.startsWith("Prompt", ignoreCase = true)) {
                val afterColon = line.substringAfter(':', "").trim()
                val bracketMatch = Regex("""\[(.*?)\]""").find(line)
                val meta = bracketMatch?.groupValues?.get(1).orEmpty()
                val category = meta.substringBefore('|').trim().ifEmpty { "Портрет" }
                val title = meta.substringAfter('|', "").trim().ifEmpty { "Промпт из досье" }
                if (afterColon.isNotEmpty()) {
                    savedPrompts.add(
                        CharacterSavedPrompt(
                            id = "prm_${System.currentTimeMillis()}_${savedPrompts.size}",
                            title = title,
                            promptText = afterColon,
                            category = category
                        )
                    )
                }
            } else if (line.startsWith("Связь", ignoreCase = true) || line.startsWith("Отношение", ignoreCase = true)) {
                val payload = line.substringAfter(':', "").trim()
                val parts = payload.split("|").map { it.trim() }
                if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
                    val targetName = parts[0]
                    val targetType = parts.getOrNull(1)?.takeIf { it == "CHARACTER" || it == "LOCATION" } ?: "CHARACTER"
                    val relType = parts.getOrNull(2)?.ifEmpty { "Знаком с" } ?: "Знаком с"
                    val desc = parts.getOrNull(3).orEmpty()
                    declaredRels.add(
                        DeclaredCharacterRelation(
                            targetName = targetName,
                            targetType = targetType,
                            relationType = relType,
                            description = desc
                        )
                    )
                }
            }
        }

        val appearance = findValue("Внешность", "Описание внешности", "Appearance")
        val hair = findValue("Волосы", "Цвет волос", "Hair")
        val eyes = findValue("Глаза", "Цвет глаз", "Eyes")
        val clothing = findValue("Одежда", "Повседневная одежда", "Clothing")
        val visualPassport = findValue("Паспорт внешности", "Visual Passport", "Визуальный паспорт").ifEmpty {
            buildString {
                append("Character portrait of $extractedName")
                if (appearance.isNotBlank()) append(", $appearance")
                if (hair.isNotBlank()) append(", hair: $hair")
                if (eyes.isNotBlank()) append(", eyes: $eyes")
                if (clothing.isNotBlank()) append(", wearing $clothing")
                append(", RedWorld gothic crimson atmosphere, detailed concept art")
            }
        }

        if (savedPrompts.isEmpty() && visualPassport.isNotBlank()) {
            savedPrompts.add(
                CharacterSavedPrompt(
                    id = "prm_auto_${System.currentTimeMillis()}",
                    title = "Портрет: $extractedName",
                    promptText = visualPassport,
                    category = "Портрет"
                )
            )
        }

        val relSummary = findValue("Отношения", "Связи", "Relationships").ifEmpty {
            declaredRels.joinToString("; ") { "${it.relationType}: ${it.targetName} (${it.description})" }
        }

        val occupation = findValue("Работа", "Род занятий", "Профессия", "Должность", "Occupation")
        val shortDesc = findValue("Краткое описание", "Кратко", "Описание", "Short Description").ifEmpty {
            listOf(occupation, appearance).filter { it.isNotBlank() }.joinToString(". ")
        }

        return AiExtractedCharacterDossier(
            name = extractedName,
            fullName = findValue("Полное имя", "ФИО полностью", "Full Name").ifEmpty { extractedName },
            aliases = findValue("Псевдонимы", "Псевдоним", "Титул", "Прозвище", "Aliases"),
            storyRole = findValue("Роль в сюжете", "Сюжетная роль", "Тип персонажа", "Role").ifEmpty { "Ключевой персонаж" },
            characterStatus = findValue("Статус персонажа", "Статус", "Status").ifEmpty { "Активен" },
            factionOrGroup = findValue("Фракция", "Организация", "Группа", "Дом", "Орден", "Faction"),
            tagsCsv = findValue("Теги", "Метки", "Ключевые слова", "Tags"),
            gender = findValue("Пол", "Gender"),
            speciesOrOrigin = findValue("Происхождение", "Раса", "Вид", "Народ", "Origin").ifEmpty { "Человек" },
            shortDescription = shortDesc,
            detailedDescription = findValue("Подробная биография", "Биография", "История жизни", "Предыстория", "Biography"),
            birthDate = findValue("Дата рождения", "Родился", "Родилась", "Birth Date"),
            age = findValue("Возраст", "Лет", "Age"),
            birthPlace = findValue("Место рождения", "Родной город", "Birth Place"),
            appearance = appearance,
            physique = findValue("Телосложение", "Комплекция", "Physique"),
            hair = hair,
            eyes = eyes,
            voiceDescription = findValue("Голос", "Тембр голоса", "Voice"),
            defaultClothing = clothing,
            distinguishingMarks = findValue("Особые приметы", "Приметы", "Шрамы", "Distinguishing Marks"),
            personality = findValue("Характер", "Личность", "Темперамент", "Personality"),
            archetype = findValue("Архетип", "Психотип", "Archetype"),
            habits = findValue("Привычки", "Привычка", "Habits"),
            speechStyle = findValue("Манера речи", "Речь", "Стиль речи", "Speech Style"),
            catchphrases = findValue("Характерные фразы", "Любимые фразы", "Цитаты", "Catchphrases"),
            occupation = occupation,
            skills = findValue("Навыки", "Умения", "Способности", "Skills"),
            strengths = findValue("Сильные стороны", "Преимущества", "Strengths"),
            weaknesses = findValue("Слабые стороны", "Уязвимости", "Слабости", "Weaknesses"),
            inventorySummary = findValue("Инвентарь", "Личные вещи", "Снаряжение", "Предметы", "Inventory"),
            interests = findValue("Интересы", "Хобби", "Увлечения", "Interests"),
            preferences = findValue("Предпочтения", "Любит", "Вкусы", "Preferences"),
            dislikes = findValue("Не любит", "Антипатии", "Раздражает", "Dislikes"),
            goals = findValue("Цели", "Цель", "Goals"),
            motivation = findValue("Мотивация", "Мотив", "Motivation"),
            internalConflict = findValue("Внутренний конфликт", "Дилемма", "Internal Conflict"),
            fearsAndLimitations = findValue("Страхи и ограничения", "Страхи", "Табу", "Ограничения", "Fears"),
            knownFacts = findValue("Известные факты", "Знания", "Что знает", "Known Facts"),
            personalSecrets = findValue("Личные тайны", "Секреты", "Тайна", "Secrets"),
            unknownFactsRestriction = findValue("Чего персонаж не знает", "Не знает", "Ограничения знаний", "Unknown Facts"),
            behavioralTraits = findValue("Поведение", "Поведенческие особенности", "Behavior"),
            currentLocationName = findValue("Локация сейчас", "Текущая локация", "Где находится", "Локация").ifEmpty { "Красный особняк" },
            currentMoodAndCondition = findValue("Состояние сейчас", "Текущее состояние", "Эмоции").ifEmpty { "Спокоен, сосредоточен" },
            currentHealthStatus = findValue("Здоровье сейчас", "Здоровье", "Физическое состояние").ifEmpty { "В норме" },
            currentOutfit = findValue("Текущая одежда", "Одежда в сцене"),
            currentActiveGoal = findValue("Активная цель", "Текущая задача", "Задача в сцене"),
            visualPassportPrompt = visualPassport,
            relationshipsSummary = relSummary,
            additionalNotes = "Заполнено нейросетью на основе файла: $fileName",
            savedPrompts = savedPrompts,
            declaredRelationships = declaredRels
        )
    }

    private fun extractTextOrMetadataFromPdf(
        context: Context,
        bytes: ByteArray,
        fileName: String
    ): String {
        val tempFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
        return try {
            tempFile.writeBytes(bytes)
            var pageCount = 0
            var pageDimensions = ""
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    pageCount = renderer.pageCount
                    if (pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            pageDimensions = "${page.width}x${page.height} pt"
                        }
                    }
                }
            }
            // Extract readable ASCII/UTF-8 text literals from PDF stream where present
            val rawAscii = String(bytes, StandardCharsets.ISO_8859_1)
            val extractedLiterals = Regex("""\(([^\(\)\\]{3,120})\)""")
                .findAll(rawAscii)
                .map { it.groupValues[1] }
                .filter { s -> s.count { it.isLetterOrDigit() || it.isWhitespace() } > s.length * 0.75 }
                .take(150)
                .joinToString(" ")

            buildString {
                appendLine("# PDF Документ: $fileName")
                appendLine("- Количество страниц: $pageCount ($pageDimensions)")
                appendLine("- Размер файла: ${bytes.size / 1024} КБ")
                if (extractedLiterals.isNotBlank()) {
                    appendLine("\n## Извлечённый текстовый слой")
                    appendLine(extractedLiterals)
                } else {
                    appendLine("\nДокумент импортирован в локальное хранилище RedWorld Knowledge и доступен для просмотра страниц.")
                }
            }
        } catch (e: Exception) {
            "PDF Документ ($fileName, ${bytes.size} байт). Метаданные сохранены."
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Generates a canonical RedWorld Character & World XLSX file on disk so the user can
     * immediately inspect, edit cells, test formula/merge preservation, and export to Google Drive.
     */
    suspend fun ensureSampleRedWorldXlsx(context: Context): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "xlsx_workbooks").apply { mkdirs() }
        val file = File(dir, "RedWorld_Character_Cards.xlsx")
        if (!file.exists()) {
            writeSampleXlsxFile(file)
        }
        file
    }

    private fun writeSampleXlsxFile(targetFile: File) {
        ZipOutputStream(FileOutputStream(targetFile)).use { zos ->
            fun putXml(path: String, xml: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(xml.trimIndent().toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }

            putXml(
                "[Content_Types].xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
                """
            )

            putXml(
                "_rels/.rels",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """
            )

            putXml(
                "xl/workbook.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets>
                        <sheet name="Карточки персонажей" sheetId="1" r:id="rId1"/>
                        <sheet name="Локации и статус" sheetId="2" r:id="rId2"/>
                    </sheets>
                </workbook>
                """
            )

            putXml(
                "xl/_rels/workbook.xml.rels",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                    <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """
            )

            putXml(
                "xl/styles.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <fonts count="2">
                        <font><sz val="11"/><name val="Calibri"/></font>
                        <font><b/><sz val="12"/><name val="Calibri"/></font>
                    </fonts>
                    <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
                    <borders count="1"><border/></borders>
                    <cellXfs count="2">
                        <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
                        <xf numFmtId="0" fontId="1" fillId="0" borderId="0" applyFont="1"/>
                    </cellXfs>
                </styleSheet>
                """
            )

            putXml(
                "xl/worksheets/sheet1.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <sheetData>
                        <row r="1">
                            <c r="A1" s="1" t="inlineStr"><is><t>Поле карточки RedWorld</t></is></c>
                            <c r="B1" s="1" t="inlineStr"><is><t>Виктор Корвин</t></is></c>
                            <c r="C1" s="1" t="inlineStr"><is><t>Елена Ростова</t></is></c>
                        </row>
                        <row r="2">
                            <c r="A2" t="inlineStr"><is><t>Псевдоним / Титул</t></is></c>
                            <c r="B2" t="inlineStr"><is><t>Хранитель Архива</t></is></c>
                            <c r="C2" t="inlineStr"><is><t>Тень Зелёной улицы</t></is></c>
                        </row>
                        <row r="3">
                            <c r="A3" t="inlineStr"><is><t>Дата рождения (Постоянно)</t></is></c>
                            <c r="B3" t="inlineStr"><is><t>14.11.1988</t></is></c>
                            <c r="C3" t="inlineStr"><is><t>03.04.1998</t></is></c>
                        </row>
                        <row r="4">
                            <c r="A4" t="inlineStr"><is><t>Возраст (лет)</t></is></c>
                            <c r="B4" t="n"><v>37</v></c>
                            <c r="C4" t="n"><v>27</v></c>
                        </row>
                        <row r="5">
                            <c r="A5" t="inlineStr"><is><t>Текущая локация (Состояние)</t></is></c>
                            <c r="B5" t="inlineStr"><is><t>Красный особняк</t></is></c>
                            <c r="C5" t="inlineStr"><is><t>Бар</t></is></c>
                        </row>
                        <row r="6">
                            <c r="A6" t="inlineStr"><is><t>Статус канона</t></is></c>
                            <c r="B6" t="inlineStr"><is><t>Канон</t></is></c>
                            <c r="C6" t="inlineStr"><is><t>Канон</t></is></c>
                        </row>
                        <row r="7">
                            <c r="A7" s="1" t="inlineStr"><is><t>Средний возраст группы (Формула)</t></is></c>
                            <c r="B7" t="n"><f>AVERAGE(B4:C4)</f><v>32</v></c>
                            <c r="C7" t="inlineStr"><is><t>Авторасчёт</t></is></c>
                        </row>
                    </sheetData>
                    <mergeCells count="1">
                        <mergeCell ref="B7:C7"/>
                    </mergeCells>
                </worksheet>
                """
            )

            putXml(
                "xl/worksheets/sheet2.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <sheetData>
                        <row r="1">
                            <c r="A1" s="1" t="inlineStr"><is><t>Локация</t></is></c>
                            <c r="B1" s="1" t="inlineStr"><is><t>Категория</t></is></c>
                            <c r="C1" s="1" t="inlineStr"><is><t>Режим доступа</t></is></c>
                        </row>
                        <row r="2">
                            <c r="A2" t="inlineStr"><is><t>Красный особняк</t></is></c>
                            <c r="B2" t="inlineStr"><is><t>Резиденция / Архив</t></is></c>
                            <c r="C2" t="inlineStr"><is><t>Закрытый (по допуску)</t></is></c>
                        </row>
                        <row r="3">
                            <c r="A3" t="inlineStr"><is><t>Зелёная улица</t></is></c>
                            <c r="B3" t="inlineStr"><is><t>Городская артерия</t></is></c>
                            <c r="C3" t="inlineStr"><is><t>Открытый</t></is></c>
                        </row>
                        <row r="4">
                            <c r="A4" t="inlineStr"><is><t>Магазин</t></is></c>
                            <c r="B4" t="inlineStr"><is><t>Торговая точка</t></is></c>
                            <c r="C4" t="inlineStr"><is><t>10:00 - 21:00</t></is></c>
                        </row>
                        <row r="5">
                            <c r="A5" t="inlineStr"><is><t>Бар «Алый Бархат»</t></is></c>
                            <c r="B5" t="inlineStr"><is><t>Социальный узел</t></is></c>
                            <c r="C5" t="inlineStr"><is><t>Вечерний / Нейтралитет</t></is></c>
                        </row>
                    </sheetData>
                </worksheet>
                """
            )
        }
    }

    /**
     * Parses an XLSX file on disk without losing its original structure.
     * Reads `xl/sharedStrings.xml`, `xl/workbook.xml`, and each `xl/worksheets/sheet*.xml`.
     */
    suspend fun parseXlsxFile(file: File): XlsxWorkbookModel = withContext(Dispatchers.IO) {
        val entries = mutableMapOf<String, ByteArray>()
        val warnings = mutableListOf<String>()

        ZipInputStream(FileInputStream(file)).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val e = entry ?: continue
                if (!e.isDirectory) {
                    entries[e.name] = zis.readBytes()
                }
            }
        }

        if (entries.keys.any { it.contains("vbaProject") || it.contains("drawings/") }) {
            warnings.add("Внимание: книга содержит макросы VBA или встроенные векторные объекты. При точечном редактировании ячеек они сохраняются в архиве, но рекомендуется проверить их перед внешним экспортом.")
        }

        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
        val sheetDefs = parseWorkbookSheetNames(entries["xl/workbook.xml"])

        val parsedSheets = sheetDefs.mapIndexed { idx, (sheetName, _) ->
            val zipPath = "xl/worksheets/sheet${idx + 1}.xml"
            val sheetBytes = entries[zipPath]
            if (sheetBytes != null) {
                parseWorksheetXml(
                    sheetName = sheetName,
                    sheetIndex = idx,
                    zipEntryPath = zipPath,
                    xmlBytes = sheetBytes,
                    sharedStrings = sharedStrings
                )
            } else {
                XlsxSheetData(
                    sheetName = sheetName,
                    sheetIndex = idx,
                    zipEntryPath = zipPath,
                    mergedRanges = emptyList(),
                    cells = emptyList()
                )
            }
        }

        XlsxWorkbookModel(
            fileName = file.name,
            localFilePath = file.absolutePath,
            sheets = parsedSheets,
            warnings = warnings
        )
    }

    private fun parseSharedStrings(xmlBytes: ByteArray?): List<String> {
        if (xmlBytes == null) return emptyList()
        val list = mutableListOf<String>()
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(InputStreamReader(ByteArrayInputStream(xmlBytes), StandardCharsets.UTF_8))
            var event = parser.eventType
            var currentSi = StringBuilder()
            var insideSi = false
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "si") {
                            insideSi = true
                            currentSi = StringBuilder()
                        } else if (insideSi && parser.name == "t") {
                            currentSi.append(parser.nextText())
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "si" && insideSi) {
                            list.add(currentSi.toString())
                            insideSi = false
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun parseWorkbookSheetNames(xmlBytes: ByteArray?): List<Pair<String, String>> {
        if (xmlBytes == null) return listOf("Лист 1" to "1")
        val result = mutableListOf<Pair<String, String>>()
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(InputStreamReader(ByteArrayInputStream(xmlBytes), StandardCharsets.UTF_8))
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "sheet") {
                    val name = parser.getAttributeValue(null, "name") ?: "Sheet"
                    val sheetId = parser.getAttributeValue(null, "sheetId") ?: "1"
                    result.add(name to sheetId)
                }
                event = parser.next()
            }
        } catch (_: Exception) {
        }
        return result.ifEmpty { listOf("Лист 1" to "1") }
    }

    private fun parseWorksheetXml(
        sheetName: String,
        sheetIndex: Int,
        zipEntryPath: String,
        xmlBytes: ByteArray,
        sharedStrings: List<String>
    ): XlsxSheetData {
        val cells = mutableListOf<XlsxCellData>()
        val mergedRanges = mutableListOf<String>()

        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(InputStreamReader(ByteArrayInputStream(xmlBytes), StandardCharsets.UTF_8))
            var event = parser.eventType

            var currentCellRef = ""
            var currentCellType = ""
            var currentStyleIdx = ""
            var currentFormula = ""
            var currentVal = ""
            var insideCell = false

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "c" -> {
                                insideCell = true
                                currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                                currentCellType = parser.getAttributeValue(null, "t") ?: ""
                                currentStyleIdx = parser.getAttributeValue(null, "s") ?: ""
                                currentFormula = ""
                                currentVal = ""
                            }
                            "f" -> if (insideCell) {
                                currentFormula = parser.nextText()
                            }
                            "v" -> if (insideCell) {
                                val rawV = parser.nextText()
                                currentVal = if (currentCellType == "s") {
                                    val idx = rawV.toIntOrNull() ?: -1
                                    sharedStrings.getOrElse(idx) { rawV }
                                } else {
                                    rawV
                                }
                            }
                            "t" -> if (insideCell) {
                                currentVal = parser.nextText()
                            }
                            "mergeCell" -> {
                                val ref = parser.getAttributeValue(null, "ref")
                                if (!ref.isNullOrBlank()) mergedRanges.add(ref)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "c" && insideCell) {
                            val (rowNum, colIdx) = splitCellRef(currentCellRef)
                            cells.add(
                                XlsxCellData(
                                    cellRef = currentCellRef,
                                    rowNumber = rowNum,
                                    colIndex = colIdx,
                                    displayValue = currentVal,
                                    formula = currentFormula,
                                    styleIndex = currentStyleIdx
                                )
                            )
                            insideCell = false
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
        }

        return XlsxSheetData(
            sheetName = sheetName,
            sheetIndex = sheetIndex,
            zipEntryPath = zipEntryPath,
            mergedRanges = mergedRanges,
            cells = cells
        )
    }

    private fun splitCellRef(cellRef: String): Pair<Int, Int> {
        val letters = cellRef.takeWhile { it.isLetter() }.uppercase()
        val digits = cellRef.dropWhile { it.isLetter() }.toIntOrNull() ?: 1
        var col = 0
        for (ch in letters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return digits to (col - 1).coerceAtLeast(0)
    }

    /**
     * Surgically updates a single cell in an XLSX file while preserving all other sheets,
     * styles, mergedCells, formulas, and ZIP entries. Creates a timestamped backup file first.
     */
    suspend fun updateSingleCellSurgically(
        context: Context,
        xlsxFile: File,
        sheetZipEntryPath: String,
        cellRef: String,
        newValue: String
    ): Pair<File, XlsxWorkbookModel> = withContext(Dispatchers.IO) {
        // 1. Create backup copy before modifying
        val backupDir = File(context.filesDir, "xlsx_backups").apply { mkdirs() }
        val backupFile = File(backupDir, "${xlsxFile.nameWithoutExtension}_v_${System.currentTimeMillis()}.xlsx")
        xlsxFile.copyTo(backupFile, overwrite = true)

        // 2. Read all ZIP entries in memory
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(FileInputStream(xlsxFile)).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val e = entry ?: continue
                if (!e.isDirectory) {
                    entries[e.name] = zis.readBytes()
                }
            }
        }

        // 3. Modify only the target <c r="cellRef" ...> node inside the target sheet XML
        val sheetBytes = entries[sheetZipEntryPath]
        if (sheetBytes != null) {
            val xmlString = String(sheetBytes, StandardCharsets.UTF_8)
            val escapedVal = newValue
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")

            val cellPattern = Regex("""<c\b([^>]*\br="$cellRef"[^>]*)>(.*?)</c>""", RegexOption.DOT_MATCHES_ALL)
            val match = cellPattern.find(xmlString)
            if (match != null) {
                val attrs = match.groupValues[1]
                val innerXml = match.groupValues[2]
                // Preserve <f>...</f> if present and preserve s="..." attribute!
                val formulaMatch = Regex("""<f[^>]*>.*?</f>""", RegexOption.DOT_MATCHES_ALL).find(innerXml)
                val styleMatch = Regex("""\bs="([^"]+)"""").find(attrs)
                val styleAttr = styleMatch?.value?.let { " $it" } ?: ""
                val isNumeric = newValue.toDoubleOrNull() != null

                val replacementCellXml = if (formulaMatch != null && isNumeric) {
                    """<c r="$cellRef"$styleAttr t="n">${formulaMatch.value}<v>$escapedVal</v></c>"""
                } else if (isNumeric) {
                    """<c r="$cellRef"$styleAttr t="n"><v>$escapedVal</v></c>"""
                } else {
                    """<c r="$cellRef"$styleAttr t="inlineStr"><is><t>$escapedVal</t></is></c>"""
                }

                val updatedXml = xmlString.replaceRange(match.range, replacementCellXml)
                entries[sheetZipEntryPath] = updatedXml.toByteArray(StandardCharsets.UTF_8)
            }
        }

        // 4. Write all entries back to xlsxFile
        val tempOut = ByteArrayOutputStream()
        ZipOutputStream(tempOut).use { zos ->
            for ((path, bytes) in entries) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        xlsxFile.writeBytes(tempOut.toByteArray())

        backupFile to parseXlsxFile(xlsxFile)
    }

    /**
     * Compares two XLSX files cell-by-cell and returns a list of differences.
     */
    suspend fun compareXlsxVersions(oldFile: File, currentFile: File): List<CellDiffItem> =
        withContext(Dispatchers.IO) {
            if (!oldFile.exists() || !currentFile.exists()) return@withContext emptyList()
            val oldWb = parseXlsxFile(oldFile)
            val newWb = parseXlsxFile(currentFile)
            val diffs = mutableListOf<CellDiffItem>()

            for (newSheet in newWb.sheets) {
                val oldSheet = oldWb.sheets.firstOrNull { it.sheetName == newSheet.sheetName } ?: continue
                val oldMap = oldSheet.cells.associateBy { it.cellRef }
                for (newCell in newSheet.cells) {
                    val oldCell = oldMap[newCell.cellRef]
                    if (oldCell != null && oldCell.displayValue != newCell.displayValue) {
                        diffs.add(
                            CellDiffItem(
                                sheetName = newSheet.sheetName,
                                cellRef = newCell.cellRef,
                                oldValue = oldCell.displayValue,
                                newValue = newCell.displayValue,
                                formulaPreserved = oldCell.formula == newCell.formula
                            )
                        )
                    }
                }
            }
            diffs
        }
}
