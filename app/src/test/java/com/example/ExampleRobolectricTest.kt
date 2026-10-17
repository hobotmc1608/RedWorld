package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.DocumentAndXlsxEngine
import com.example.data.local.DocumentFormat
import com.example.data.local.DocumentTagCrossRef
import com.example.data.local.KnowledgeDocumentEntity
import com.example.data.local.KnowledgeNoteEntity
import com.example.data.local.KnowledgeTagEntity
import com.example.data.local.RedWorldDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: RedWorldDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = RedWorldDatabase.createInMemory(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app_name from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("RedWorld AI", appName)
    }

    @Test
    fun `knowledge dao inserts queries and deletes documents notes and tags`() = runBlocking {
        val dao = db.knowledgeDao()

        // 1. Insert Document
        val docId = dao.insertDocument(
            KnowledgeDocumentEntity(
                title = "Тестовый канон Красного особняка",
                fileName = "test_mansion.md",
                format = DocumentFormat.MARKDOWN.name,
                folderName = "Правила мира",
                collectionName = "Основной канон",
                contentText = "# Красный особняк\nОписание резиденции Виктора Корвина."
            )
        )
        assertTrue(docId > 0L)

        // 2. Insert Tag and link to Document
        val tagId = dao.insertTag(KnowledgeTagEntity(name = "Особняк", category = "Локация"))
        dao.insertDocumentTagCrossRef(DocumentTagCrossRef(documentId = docId, tagId = tagId))

        // 3. Insert Note attached to Document
        val noteId = dao.insertNote(
            KnowledgeNoteEntity(
                documentId = docId,
                title = "Заметка архивариуса",
                content = "Проверить расписание библиотеки."
            )
        )
        assertTrue(noteId > 0L)

        // 4. Query Document with Details
        val withDetails = dao.observeDocumentWithDetailsById(docId).first()
        assertNotNull(withDetails)
        assertEquals("Тестовый канон Красного особняка", withDetails?.document?.title)
        assertEquals(1, withDetails?.tags?.size)
        assertEquals("Особняк", withDetails?.tags?.first()?.name)
        assertEquals(1, withDetails?.notes?.size)
        assertEquals("Заметка архивариуса", withDetails?.notes?.first()?.title)

        // 5. Delete Note, Tag, and Document
        dao.deleteNoteById(noteId)
        assertEquals(0, dao.observeAllNotes().first().size)

        dao.deleteTagById(tagId)
        assertEquals(0, dao.observeAllTags().first().size)

        dao.deleteDocumentById(docId)
        assertEquals(0, dao.observeAllDocuments().first().size)
    }

    @Test
    fun `gemini chat session and message history persistence`() = runBlocking {
        val rwDao = db.redWorldDao()
        val sessionId = rwDao.insertChatSession(
            ChatSessionEntity(
                title = "Тестовый чат Gemini",
                modelId = "gemini-3.5-flash"
            )
        )
        rwDao.insertChatMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = "Где находится Виктор Корвин?"
            )
        )
        rwDao.insertChatMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "model",
                content = "Виктор Корвин находится в Красном особняке."
            )
        )

        val messages = rwDao.getMessagesForSession(sessionId)
        assertEquals(2, messages.size)
        assertEquals("user", messages[0].role)
        assertEquals("model", messages[1].role)
    }

    @Test
    fun `surgical xlsx engine preserves sheets formulas and merged cells`() = runBlocking {
        val xlsxFile = DocumentAndXlsxEngine.ensureSampleRedWorldXlsx(context)
        val initialWb = DocumentAndXlsxEngine.parseXlsxFile(xlsxFile)
        assertEquals(2, initialWb.sheets.size)
        val sheet1 = initialWb.sheets[0]
        assertTrue(sheet1.mergedRanges.contains("B7:C7"))
        val formulaCell = sheet1.cells.firstOrNull { it.cellRef == "B7" }
        assertEquals("AVERAGE(B4:C4)", formulaCell?.formula)

        // Edit single cell B5 (current location of Victor) surgically
        val (backupFile, updatedWb) = DocumentAndXlsxEngine.updateSingleCellSurgically(
            context = context,
            xlsxFile = xlsxFile,
            sheetZipEntryPath = sheet1.zipEntryPath,
            cellRef = "B5",
            newValue = "Зелёная улица"
        )
        assertTrue(backupFile.exists())
        val updatedB5 = updatedWb.sheets[0].cells.firstOrNull { it.cellRef == "B5" }
        assertEquals("Зелёная улица", updatedB5?.displayValue)
        // Verify formula in B7 and merged range B7:C7 are still intact
        val preservedB7 = updatedWb.sheets[0].cells.firstOrNull { it.cellRef == "B7" }
        assertEquals("AVERAGE(B4:C4)", preservedB7?.formula)
        assertTrue(updatedWb.sheets[0].mergedRanges.contains("B7:C7"))
    }
}
