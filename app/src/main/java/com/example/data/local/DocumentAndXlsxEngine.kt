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
     * Extracts readable text from URI based on detected format (TXT, MD, DOCX, XLSX, PDF).
     */
    suspend fun extractTextFromUri(
        context: Context,
        uri: Uri,
        fileName: String
    ): Pair<DocumentFormat, String> = withContext(Dispatchers.IO) {
        val lower = fileName.lowercase()
        val format = when {
            lower.endsWith(".pdf") -> DocumentFormat.PDF
            lower.endsWith(".xlsx") -> DocumentFormat.XLSX
            lower.endsWith(".docx") -> DocumentFormat.DOCX
            lower.endsWith(".md") || lower.endsWith(".markdown") -> DocumentFormat.MARKDOWN
            else -> DocumentFormat.TXT
        }

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext format to ""

        val text = when (format) {
            DocumentFormat.TXT, DocumentFormat.MARKDOWN -> {
                String(bytes, StandardCharsets.UTF_8)
            }
            DocumentFormat.DOCX -> {
                extractTextFromDocxBytes(bytes)
            }
            DocumentFormat.XLSX -> {
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
            DocumentFormat.PDF -> {
                extractTextOrMetadataFromPdf(context, bytes, fileName)
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
            return "Ошибка чтения DOCX: ${e.localizedMessage}"
        }
        return sb.toString().trim()
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
