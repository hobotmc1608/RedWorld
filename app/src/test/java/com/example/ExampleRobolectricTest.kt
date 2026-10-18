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
import com.example.data.preferences.UserPreferencesRepository
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
        assertEquals("World AI Studio", appName)
    }

    @Test
    fun `datastore saves masks and clears gemini api key`() = runBlocking {
        val prefsRepo = UserPreferencesRepository(context)
        val sampleKey = "AIzaSyTestRedWorldKey9876"

        prefsRepo.setCustomGeminiApiKey(sampleKey)
        val savedPrefs = prefsRepo.preferencesFlow.first()
        assertEquals(sampleKey, savedPrefs.customGeminiApiKey)
        assertEquals(sampleKey, savedPrefs.effectiveGeminiApiKey)
        assertTrue(savedPrefs.isUsingCustomDataStoreKey)
        assertEquals("AIza••••••••••••9876", savedPrefs.maskedGeminiApiKey)

        prefsRepo.clearCustomGeminiApiKey()
        val clearedPrefs = prefsRepo.preferencesFlow.first()
        assertEquals("", clearedPrefs.customGeminiApiKey)
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
    fun `character dossier extraction and automatic relationship linking on new character addition`() = runBlocking {
        val repo = com.example.data.repository.RedWorldRepository(db.redWorldDao())

        // 1. Parse sample .doc character dossier (Маркус Вейн) which declares relationships with Виктор Корвин and Лидия Морган
        val samplePair = DocumentAndXlsxEngine.getSampleCharacterDocDossiers().first()
        val dossier = DocumentAndXlsxEngine.parseCharacterDossierLocally(samplePair.second, samplePair.first)
        assertEquals("Маркус Вейн", dossier.name)
        assertTrue(dossier.declaredRelationships.any { it.targetName.contains("Лидия Морган") })
        assertTrue(dossier.savedPrompts.isNotEmpty())

        // 2. Save Маркус Вейн first (Лидия Морган does not exist in DB yet!)
        val json = kotlinx.serialization.json.Json { encodeDefaults = true; ignoreUnknownKeys = true }
        repo.saveCharacterWithSnapshot(
            com.example.data.local.CharacterEntity(
                projectId = 1L,
                name = dossier.name,
                declaredRelationshipsJson = json.encodeToString(dossier.declaredRelationships),
                savedPromptsJson = json.encodeToString(dossier.savedPrompts)
            ),
            changeSummary = "Добавлен Маркус Вейн из .doc файла"
        )

        // 3. Now add Лидия Морган and verify the pending relationship from Маркус Вейн is linked automatically!
        val lidiaSample = DocumentAndXlsxEngine.getSampleCharacterDocDossiers()[1]
        val lidiaDossier = DocumentAndXlsxEngine.parseCharacterDossierLocally(lidiaSample.second, lidiaSample.first)
        repo.saveCharacterWithSnapshot(
            com.example.data.local.CharacterEntity(
                projectId = 1L,
                name = lidiaDossier.name,
                declaredRelationshipsJson = json.encodeToString(lidiaDossier.declaredRelationships)
            ),
            changeSummary = "Добавлена Лидия Морган"
        )

        val allRels = db.redWorldDao().getAllRelationships()
        assertTrue(
            "Expected automatic relationship link between Маркус Вейн and Лидия Морган",
            allRels.any {
                (it.sourceLabel.contains("Маркус Вейн") && it.targetLabel.contains("Лидия Морган")) ||
                    (it.sourceLabel.contains("Лидия Морган") && it.targetLabel.contains("Маркус Вейн"))
            }
        )
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

    @Test
    fun `plotlines event history and safe project export import without api keys`() = runBlocking {
        val repo = com.example.data.repository.RedWorldRepository(db.redWorldDao())

        val projectId = repo.createProjectWithTemplate(
            project = com.example.data.local.WorldProjectEntity(
                name = "RedWorld Test",
                genreOrTheme = "Драма",
                description = "Тестовый проект"
            ),
            templateType = "DEMO_REDWORLD"
        )
        assertTrue(projectId > 0L)

        val plotId = repo.savePlotline(
            com.example.data.local.PlotlineEntity(
                projectId = projectId,
                title = "Тайна Красного особняка",
                description = "Расследование старого договора",
                tasks = "Найти архив",
                developmentOptions = "Союз с Александрой / Тайный ход Нины",
                involvedCharactersCsv = "Катрин, Александра, Нина, Лола",
                linkedLocationsCsv = "Красный особняк",
                status = "Активна"
            )
        )
        assertTrue(plotId > 0L)

        val exportedJson = repo.exportProjectToJson(projectId)
        assertTrue(exportedJson.contains("Тайна Красного особняка"))
        assertTrue(!exportedJson.contains("AIzaSy"))

        // Malformed import must fail gracefully without deleting existing project data
        val badImport = repo.importProjectFromJson("{broken_json", overwriteExistingProjectId = projectId)
        assertTrue(badImport.isFailure)
        val remainingPlotlines = db.redWorldDao().getAllPlotlines().filter { it.projectId == projectId }
        assertTrue(remainingPlotlines.isNotEmpty())
    }

    @Test
    fun `character management granular field update preserves all other fields and supports export`() = runBlocking {
        val repo = com.example.data.repository.RedWorldRepository(db.redWorldDao())

        val charId = repo.saveCharacterWithSnapshot(
            com.example.data.local.CharacterEntity(
                projectId = 1L,
                name = "Катрин Вейн",
                age = "24",
                storyRole = "Главный герой",
                appearance = "Высокая, строгий взгляд.\nПредпочитает тёмные плащи.",
                detailedDescription = "Выросла в северном поместье.",
                habits = "Проверяет замки; Пьёт крепкий чай"
            ),
            changeSummary = "Создание персонажа Катрин Вейн"
        )
        assertTrue(charId > 0L)

        val saved = db.redWorldDao().getCharacterById(charId)
        assertNotNull(saved)

        // Update ONLY age using granular field spec without touching detailedDescription, appearance, or habits
        val ageSpec = com.example.ui.screens.getCharacterThematicSections()
            .flatMap { it.fields }
            .first { it.key == "age" }
        val updatedOnlyAge = ageSpec.setter(saved!!, "25")
        repo.saveCharacterWithSnapshot(updatedOnlyAge, "Изменено поле «Возраст»")

        val afterAgeUpdate = db.redWorldDao().getCharacterById(charId)!!
        assertEquals("25", afterAgeUpdate.age)
        assertEquals("Выросла в северном поместье.", afterAgeUpdate.detailedDescription)
        assertEquals("Проверяет замки; Пьёт крепкий чай", afterAgeUpdate.habits)
        assertEquals(2, afterAgeUpdate.versionNumber)

        // Export to Markdown and JSON
        val md = repo.exportCharacterToMarkdown(afterAgeUpdate)
        assertTrue(md.contains("# Досье персонажа: Катрин Вейн"))
        assertTrue(md.contains("Выросла в северном поместье."))

        val jsonStr = repo.exportCharacterToJson(afterAgeUpdate)
        val importedResult = repo.importCharacterCardFromJson(jsonStr, targetProjectId = 1L)
        assertTrue(importedResult.isSuccess)
    }

    @Test
    fun `multi-provider ai settings persist keys models custom ids and generation parameters independently`() = runBlocking {
        val prefsRepo = UserPreferencesRepository(context)

        // 1. Default provider is Google Gemini
        val initial = prefsRepo.preferencesFlow.first()
        assertEquals(com.example.data.remote.AiProviderType.GEMINI, initial.activeProviderType)
        assertEquals("gemini-3.5-flash", initial.activeProviderTextModel)

        // 2. Configure OpenAI GPT key, model, and custom model ID
        prefsRepo.setCustomApiKeyForProvider(com.example.data.remote.AiProviderType.OPENAI, "sk-proj-OpenAiTestKey1234")
        prefsRepo.setSelectedAiProvider(com.example.data.remote.AiProviderType.OPENAI)
        prefsRepo.setSelectedModelForProvider(com.example.data.remote.AiProviderType.OPENAI, "gpt-4.1")

        val openAiState = prefsRepo.preferencesFlow.first()
        assertEquals(com.example.data.remote.AiProviderType.OPENAI, openAiState.activeProviderType)
        assertEquals("sk-proj-OpenAiTestKey1234", openAiState.activeProviderEffectiveApiKey)
        assertEquals("sk-p••••••••••••1234", openAiState.maskedApiKeyFor(com.example.data.remote.AiProviderType.OPENAI))
        assertEquals("gpt-4.1", openAiState.activeProviderTextModel)

        // Set custom model ID for OpenAI
        prefsRepo.setCustomModelIdForProvider(com.example.data.remote.AiProviderType.OPENAI, "gpt-4o-custom-2025")
        val openAiCustomModelState = prefsRepo.preferencesFlow.first()
        assertEquals("gpt-4o-custom-2025", openAiCustomModelState.activeProviderTextModel)

        // 3. Switch to xAI Grok and verify OpenAI settings remain intact
        prefsRepo.setCustomApiKeyForProvider(com.example.data.remote.AiProviderType.XAI, "xai-GrokSecretKey5678")
        prefsRepo.setSelectedAiProvider(com.example.data.remote.AiProviderType.XAI)
        prefsRepo.setSelectedModelForProvider(com.example.data.remote.AiProviderType.XAI, "grok-3-mini")
        prefsRepo.setAiGenerationParameters(temperature = 0.9f, topP = 0.85f, maxTokens = 4096)

        val xaiState = prefsRepo.preferencesFlow.first()
        assertEquals(com.example.data.remote.AiProviderType.XAI, xaiState.activeProviderType)
        assertEquals("xai-GrokSecretKey5678", xaiState.activeProviderEffectiveApiKey)
        assertEquals("grok-3-mini", xaiState.activeProviderTextModel)
        assertEquals(0.9f, xaiState.aiTemperature, 0.001f)
        assertEquals(0.85f, xaiState.aiTopP, 0.001f)
        assertEquals(4096, xaiState.aiMaxTokens)
        // OpenAI key and custom model are preserved when XAI is active
        assertEquals("sk-proj-OpenAiTestKey1234", xaiState.effectiveOpenAiApiKey)
        assertEquals("gpt-4o-custom-2025", xaiState.effectiveTextModelFor(com.example.data.remote.AiProviderType.OPENAI))

        // 4. Reset generation parameters and clear keys
        prefsRepo.resetAiGenerationParameters()
        prefsRepo.clearCustomApiKeyForProvider(com.example.data.remote.AiProviderType.OPENAI)
        prefsRepo.clearCustomApiKeyForProvider(com.example.data.remote.AiProviderType.XAI)
        prefsRepo.setSelectedAiProvider(com.example.data.remote.AiProviderType.GEMINI)

        val finalState = prefsRepo.preferencesFlow.first()
        assertEquals(com.example.data.remote.AiProviderType.GEMINI, finalState.activeProviderType)
        assertEquals(0.7f, finalState.aiTemperature, 0.001f)
        assertEquals(2048, finalState.aiMaxTokens)
        assertEquals("", finalState.customOpenAiApiKey)
        assertEquals("", finalState.customXaiApiKey)
    }
}



