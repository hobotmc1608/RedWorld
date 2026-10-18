package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AiDebugExplanation
import com.example.data.local.AiExtractedCharacterDossier
import com.example.data.local.CanonStatus
import com.example.data.local.CellDiffItem
import com.example.data.local.CharacterEntity
import com.example.data.local.CharacterReferenceImage
import com.example.data.local.CharacterSavedPrompt
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.DeclaredCharacterRelation
import com.example.data.local.DocumentAndXlsxEngine
import com.example.data.local.DocumentFormat
import com.example.data.local.GeneratedImageEntity
import com.example.data.local.KnowledgeDocumentEntity
import com.example.data.local.KnowledgeDocumentWithDetails
import com.example.data.local.KnowledgeNoteEntity
import com.example.data.local.KnowledgeTagEntity
import com.example.data.local.LocationEntity
import com.example.data.local.NotebookLmNoteItem
import com.example.data.local.NotebookLmNotebookEntity
import com.example.data.local.PlotlineEntity
import com.example.data.local.ProposedCardFieldDiff
import com.example.data.local.RedWorldDatabase
import com.example.data.local.RelationshipEntity
import com.example.data.local.RoleplayCriterionScore
import com.example.data.local.RoleplayEvaluationReport
import com.example.data.local.VersionSnapshotEntity
import com.example.data.local.WorldEventEntity
import com.example.data.local.WorldItemEntity
import com.example.data.local.WorldProjectEntity
import com.example.data.local.WorldRuleEntity
import com.example.data.local.XlsxWorkbookModel
import com.example.data.preferences.StudioPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.AiProviderType
import com.example.data.remote.Content
import com.example.data.remote.DriveOperationResult
import com.example.data.remote.DriveRemoteFileMetadata
import com.example.data.remote.GeminiRemoteDataSource
import com.example.data.remote.GeminiResult
import com.example.data.remote.GoogleDriveSyncManager
import com.example.data.remote.InlineData
import com.example.data.remote.MultiProviderAiService
import com.example.data.remote.Part
import com.example.data.remote.SyncConflictInfo
import com.example.data.repository.AiChangeProposal
import com.example.data.repository.CitedSourceSnippet
import com.example.data.repository.KnowledgeRepository
import com.example.data.repository.RedWorldRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class RoleplayTestMessage(
    val id: String = "rp_${System.currentTimeMillis()}_${(100..999).random()}",
    val sender: String, // "USER" or "CHARACTER"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMarkedFlawed: Boolean = false,
    val userRemark: String = ""
)

enum class MainBottomTab(val route: String, val titleRu: String) {
    HOME("HOME", "Проекты"),
    REDWORLD("REDWORLD", "Мир и Лор"),
    DOCUMENTS("DOCUMENTS", "Документы"),
    GEMINI("GEMINI", "ИИ-Ассистент"),
    GENERATION("GENERATION", "Арт-Студия");

    companion object {
        val NOTEBOOK_LM: MainBottomTab get() = DOCUMENTS
    }
}

enum class SecondaryWorkspaceScreen(val titleRu: String) {
    NONE(""),
    WORLD_SIMULATION("Симуляция мира и Тестирование персонажей"),
    CHARACTERS("Персонажи проекта"),
    LOCATIONS("Локации проекта"),
    WORLD_RULES("Правила мира"),
    PLOT("Сюжет и сюжетные линии"),
    EVENTS_HISTORY("История событий"),
    DOCUMENTS("Документы проекта"),
    NOTES("Заметки проекта"),
    ITEMS("Предметы и организации"),
    SETTINGS("Настройки приложения"),
    KNOWLEDGE_RAG("Документы и Заметки"),
    RELATIONSHIP_MAP("Интерактивная карта связей"),
    AI_ASSISTANT("ИИ-Ассистент и Куратор мира"),
    XLSX_EDITOR("Редактор таблиц XLSX"),
    FILES_AND_DRIVE("Файлы и Google Drive"),
    CONNECTIONS_AND_AUTH("Подключения и Облачный обмен"),
    VERSIONS_AND_BACKUPS("История версий и Резервные копии"),
    DOCUMENTATION_GUIDE("О приложении и Архитектура")
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RedWorldViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RedWorldDatabase.getInstance(application)
    val knowledgeRepository = KnowledgeRepository(db.knowledgeDao())
    val redWorldRepository = RedWorldRepository(db.redWorldDao())
    val prefsRepository = UserPreferencesRepository(application)
    private val geminiRemote = GeminiRemoteDataSource()
    private val multiProviderAi = MultiProviderAiService(geminiRemote)
    private val driveSyncManager = GoogleDriveSyncManager(application)

    private suspend fun generateActiveProviderChat(
        conversationHistory: List<Content>,
        systemInstructionText: String? = null,
        overrideProvider: AiProviderType? = null,
        overrideModelId: String? = null,
        overrideApiKey: String? = null
    ): GeminiResult<String> {
        val prefs = studioPrefs.value
        val provider = overrideProvider ?: prefs.activeProviderType
        val apiKey = overrideApiKey ?: prefs.effectiveApiKeyFor(provider)
        val modelId = overrideModelId?.takeIf { it.isNotBlank() } ?: prefs.effectiveTextModelFor(provider)
        return multiProviderAi.generateChatResponse(
            provider = provider,
            apiKey = apiKey,
            modelId = modelId,
            conversationHistory = conversationHistory,
            systemInstructionText = systemInstructionText,
            params = prefs.generationParams
        )
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    // --- Navigation & Active Workspace State ---
    private val _currentBottomTab = MutableStateFlow(MainBottomTab.HOME)
    val currentBottomTab: StateFlow<MainBottomTab> = _currentBottomTab.asStateFlow()

    private val _secondaryScreen = MutableStateFlow(SecondaryWorkspaceScreen.NONE)
    val secondaryScreen: StateFlow<SecondaryWorkspaceScreen> = _secondaryScreen.asStateFlow()

    // --- Multi-Project Active Project State ---
    private val _activeProjectId = MutableStateFlow(1L)
    val activeProjectId: StateFlow<Long> = _activeProjectId.asStateFlow()

    val projects: StateFlow<List<WorldProjectEntity>> =
        redWorldRepository.projects
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProject: StateFlow<WorldProjectEntity?> =
        combine(projects, _activeProjectId) { list, id ->
            list.firstOrNull { it.id == id } ?: list.firstOrNull { !it.isArchived } ?: list.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val notebookLmNotebooks: StateFlow<List<NotebookLmNotebookEntity>> =
        redWorldRepository.notebookLmNotebooks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncingNotebookLm = MutableStateFlow(false)
    val isSyncingNotebookLm: StateFlow<Boolean> = _isSyncingNotebookLm.asStateFlow()

    // --- Reactive Streams from Room & DataStore (Filtered by Active Project) ---
    val studioPrefs: StateFlow<StudioPreferences> = prefsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudioPreferences())

    val documentsWithDetails: StateFlow<List<KnowledgeDocumentWithDetails>> =
        combine(knowledgeRepository.allDocumentsWithDetails, _activeProjectId) { docs, projId ->
            docs.filter { it.document.projectId == projId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocuments: StateFlow<List<KnowledgeDocumentEntity>> =
        combine(knowledgeRepository.allDocuments, _activeProjectId) { docs, projId ->
            docs.filter { it.projectId == projId }.take(5)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<KnowledgeNoteEntity>> =
        combine(knowledgeRepository.allNotes, _activeProjectId) { notes, projId ->
            notes.filter { it.projectId == projId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<KnowledgeTagEntity>> =
        knowledgeRepository.allTags
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _characterSortMode = MutableStateFlow("CUSTOM") // CUSTOM, NAME, CREATED, UPDATED, ROLE, LOCATION, COMPLETENESS
    val characterSortMode: StateFlow<String> = _characterSortMode.asStateFlow()

    val characters: StateFlow<List<CharacterEntity>> =
        combine(redWorldRepository.characters, _activeProjectId, _characterSortMode) { list, projId, sortMode ->
            val filtered = list.filter { it.projectId == projId }
            when (sortMode) {
                "NAME", "BY_NAME" -> filtered.sortedWith(
                    compareByDescending<CharacterEntity> { it.isPinned }.thenBy { it.name.lowercase() }
                )
                "CREATED", "BY_CREATED" -> filtered.sortedWith(
                    compareByDescending<CharacterEntity> { it.isPinned }.thenByDescending { it.createdAt }
                )
                "UPDATED", "BY_UPDATED" -> filtered.sortedWith(
                    compareByDescending<CharacterEntity> { it.isPinned }.thenByDescending { it.updatedAt }
                )
                "ROLE", "BY_ROLE" -> filtered.sortedWith(
                    compareByDescending<CharacterEntity> { it.isPinned }.thenBy { it.storyRole }.thenBy { it.name.lowercase() }
                )
                "LOCATION", "BY_LOCATION" -> filtered.sortedWith(
                    compareByDescending<CharacterEntity> { it.isPinned }.thenBy { it.currentLocationName }.thenBy { it.name.lowercase() }
                )
                "COMPLETENESS", "BY_COMPLETENESS" -> filtered.sortedWith(
                    compareByDescending<CharacterEntity> { it.isPinned }.thenByDescending { it.calculateCompletenessPercent() }
                )
                else -> filtered.sortedWith(
                    compareByDescending<CharacterEntity> { it.isPinned }
                        .thenBy { if (it.sortOrder == 0) Int.MAX_VALUE else it.sortOrder }
                        .thenBy { it.id }
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locations: StateFlow<List<LocationEntity>> =
        combine(redWorldRepository.locations, _activeProjectId) { list, projId ->
            list.filter { it.projectId == projId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldRules: StateFlow<List<WorldRuleEntity>> =
        combine(redWorldRepository.worldRules, _activeProjectId) { list, projId ->
            list.filter { it.projectId == projId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plotlines: StateFlow<List<PlotlineEntity>> =
        combine(redWorldRepository.plotlines, _activeProjectId) { list, projId ->
            list.filter { it.projectId == projId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldEvents: StateFlow<List<WorldEventEntity>> =
        combine(redWorldRepository.events, _activeProjectId) { list, projId ->
            list.filter { it.projectId == projId && !it.isDraftSimulation }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val simulationDraftEvents: StateFlow<List<WorldEventEntity>> =
        combine(redWorldRepository.events, _activeProjectId) { list, projId ->
            list.filter { it.projectId == projId && it.isDraftSimulation }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldItems: StateFlow<List<WorldItemEntity>> =
        combine(redWorldRepository.items, _activeProjectId) { list, projId ->
            list.filter { it.projectId == projId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val relationships: StateFlow<List<RelationshipEntity>> =
        combine(redWorldRepository.relationships, _activeProjectId) { list, projId ->
            list.filter { it.projectId == projId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val versionSnapshots: StateFlow<List<VersionSnapshotEntity>> =
        combine(redWorldRepository.versionSnapshots, _activeProjectId) { list, projId ->
            list.filter { it.projectId == projId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatSessions: StateFlow<List<ChatSessionEntity>> =
        combine(redWorldRepository.chatSessions, _activeProjectId) { list, projId ->
            list.filter { it.projectId == projId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val generatedImages: StateFlow<List<GeneratedImageEntity>> =
        combine(redWorldRepository.generatedImages, _activeProjectId) { list, projId ->
            list.filter { it.projectId == projId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Selection States (Preserved across tab switches) ---
    private val _activeChatSessionId = MutableStateFlow(1L)
    val activeChatSessionId: StateFlow<Long> = _activeChatSessionId.asStateFlow()

    val activeChatMessages: StateFlow<List<ChatMessageEntity>> = _activeChatSessionId
        .flatMapLatest { sessionId -> redWorldRepository.observeMessagesForSession(sessionId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDocumentId = MutableStateFlow<Long?>(1L)
    val selectedDocumentId: StateFlow<Long?> = _selectedDocumentId.asStateFlow()

    private val _selectedCharacterId = MutableStateFlow<Long?>(null)
    val selectedCharacterId: StateFlow<Long?> = _selectedCharacterId.asStateFlow()

    // --- World Simulation & Character Roleplay Testing States ---
    private val _isSimulatingEvents = MutableStateFlow(false)
    val isSimulatingEvents: StateFlow<Boolean> = _isSimulatingEvents.asStateFlow()

    private val _roleplayCharacterId = MutableStateFlow<Long?>(null)
    val roleplayCharacterId: StateFlow<Long?> = _roleplayCharacterId.asStateFlow()

    private val _roleplayMessages = MutableStateFlow<List<RoleplayTestMessage>>(emptyList())
    val roleplayMessages: StateFlow<List<RoleplayTestMessage>> = _roleplayMessages.asStateFlow()

    private val _isRoleplayGenerating = MutableStateFlow(false)
    val isRoleplayGenerating: StateFlow<Boolean> = _isRoleplayGenerating.asStateFlow()

    private val _lastRoleplayEvaluation = MutableStateFlow<RoleplayEvaluationReport?>(null)
    val lastRoleplayEvaluation: StateFlow<RoleplayEvaluationReport?> = _lastRoleplayEvaluation.asStateFlow()
    val roleplayEvaluationReport: StateFlow<RoleplayEvaluationReport?> get() = lastRoleplayEvaluation

    private val _isEvaluatingRoleplay = MutableStateFlow(false)
    val isEvaluatingRoleplay: StateFlow<Boolean> = _isEvaluatingRoleplay.asStateFlow()

    private val _aiDebugModeEnabled = MutableStateFlow(false)
    val aiDebugModeEnabled: StateFlow<Boolean> = _aiDebugModeEnabled.asStateFlow()

    private val _lastDebugExplanation = MutableStateFlow<AiDebugExplanation?>(null)
    val lastDebugExplanation: StateFlow<AiDebugExplanation?> = _lastDebugExplanation.asStateFlow()
    val lastAiDebugExplanation: StateFlow<AiDebugExplanation?> get() = lastDebugExplanation

    val sortedCharacters: StateFlow<List<CharacterEntity>> get() = characters
    val isSimulatingEvent: StateFlow<Boolean> get() = isSimulatingEvents

    private val _pendingCharacterDiffs = MutableStateFlow<List<ProposedCardFieldDiff>>(emptyList())
    val pendingCharacterDiffs: StateFlow<List<ProposedCardFieldDiff>> = _pendingCharacterDiffs.asStateFlow()

    // --- Character AI File Extraction State ---
    private val _isExtractingCharacterDossier = MutableStateFlow(false)
    val isExtractingCharacterDossier: StateFlow<Boolean> = _isExtractingCharacterDossier.asStateFlow()

    // --- Gemini Chat & AI Assistant Interactive States ---
    private val _chatInputDraft = MutableStateFlow("")
    val chatInputDraft: StateFlow<String> = _chatInputDraft.asStateFlow()

    private val _chatSearchQuery = MutableStateFlow("")
    val chatSearchQuery: StateFlow<String> = _chatSearchQuery.asStateFlow()

    val chatSearchResults: StateFlow<List<ChatMessageEntity>> = _chatSearchQuery
        .flatMapLatest { q ->
            if (q.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
            else redWorldRepository.searchChatMessages(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    private val _aiErrorBanner = MutableStateFlow<String?>(null)
    val aiErrorBanner: StateFlow<String?> = _aiErrorBanner.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    private val _attachedImageBase64 = MutableStateFlow<String?>(null)
    val attachedImageBase64: StateFlow<String?> = _attachedImageBase64.asStateFlow()

    private val _attachedFileName = MutableStateFlow<String?>(null)
    val attachedFileName: StateFlow<String?> = _attachedFileName.asStateFlow()

    private var activeGenerationJob: Job? = null

    // --- Image Studio State ---
    private val _imagePromptDraft = MutableStateFlow("Portrait of Victor Corvin in the Crimson Library of the Red Mansion, warm amber lamp light, detailed gothic concept art")
    val imagePromptDraft: StateFlow<String> = _imagePromptDraft.asStateFlow()

    private val _imageStudioPreselectedRefBase64 = MutableStateFlow<String?>(null)
    val imageStudioPreselectedRefBase64: StateFlow<String?> = _imageStudioPreselectedRefBase64.asStateFlow()

    private val _imageStudioPreselectedRefTitle = MutableStateFlow<String?>(null)
    val imageStudioPreselectedRefTitle: StateFlow<String?> = _imageStudioPreselectedRefTitle.asStateFlow()

    private val _isImageGenerating = MutableStateFlow(false)
    val isImageGenerating: StateFlow<Boolean> = _isImageGenerating.asStateFlow()

    // --- XLSX Surgical Workbook State ---
    private val _activeXlsxWorkbook = MutableStateFlow<XlsxWorkbookModel?>(null)
    val activeXlsxWorkbook: StateFlow<XlsxWorkbookModel?> = _activeXlsxWorkbook.asStateFlow()

    private val _xlsxLastDiffs = MutableStateFlow<List<CellDiffItem>>(emptyList())
    val xlsxLastDiffs: StateFlow<List<CellDiffItem>> = _xlsxLastDiffs.asStateFlow()

    private var currentXlsxFile: File? = null
    private var previousXlsxBackupFile: File? = null

    // --- Google Drive & Sync State ---
    private val _driveRemoteFiles = MutableStateFlow<List<DriveRemoteFileMetadata>>(emptyList())
    val driveRemoteFiles: StateFlow<List<DriveRemoteFileMetadata>> = _driveRemoteFiles.asStateFlow()

    private val _detectedConflict = MutableStateFlow<SyncConflictInfo?>(null)
    val detectedConflict: StateFlow<SyncConflictInfo?> = _detectedConflict.asStateFlow()

    private val _isDriveSyncing = MutableStateFlow(false)
    val isDriveSyncing: StateFlow<Boolean> = _isDriveSyncing.asStateFlow()

    init {
        viewModelScope.launch {
            val xlsxFile = DocumentAndXlsxEngine.ensureSampleRedWorldXlsx(getApplication())
            currentXlsxFile = xlsxFile
            _activeXlsxWorkbook.value = DocumentAndXlsxEngine.parseXlsxFile(xlsxFile)
        }
        viewModelScope.launch {
            var restoredProjectOnce = false
            prefsRepository.preferencesFlow.collect { prefs ->
                if (!restoredProjectOnce && prefs.activeProjectId > 0L) {
                    _activeProjectId.value = prefs.activeProjectId
                    restoredProjectOnce = true
                }
                if (_chatInputDraft.value.isEmpty() && prefs.unsentPromptDraft.isNotEmpty()) {
                    _chatInputDraft.value = prefs.unsentPromptDraft
                }
                if (prefs.unsentImagePromptDraft.isNotEmpty() && _imagePromptDraft.value.startsWith("Portrait of Victor")) {
                    _imagePromptDraft.value = prefs.unsentImagePromptDraft
                }
            }
        }
    }

    // ================= MULTI-PROJECT WORKSPACE ACTIONS =================

    fun selectProject(projectId: Long) {
        _activeProjectId.value = projectId
        viewModelScope.launch {
            prefsRepository.saveNavigationState(projectId = projectId)
            val projName = projects.value.firstOrNull { it.id == projectId }?.name ?: "Проект #$projectId"
            _statusToast.value = "Активный проект переключён на «$projName»"
        }
    }

    fun createNewProject(name: String, genreOrTheme: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = redWorldRepository.saveProject(
                WorldProjectEntity(
                    name = name.trim(),
                    genreOrTheme = genreOrTheme.trim().ifEmpty { "Авторский мир" },
                    description = description.trim()
                )
            )
            // Create a default location & chat session for the new project so it's ready immediately
            redWorldRepository.saveLocation(
                LocationEntity(
                    projectId = newId,
                    name = "Центральная локация «${name.trim()}»",
                    category = "Ключевая точка",
                    description = description.ifBlank { "Отправная локация нового мира «${name.trim()}»." }
                )
            )
            val chatId = redWorldRepository.createChatSession(
                title = "Разработка мира «${name.trim()}»",
                modelId = studioPrefs.value.selectedTextModel,
                projectId = newId
            )
            _activeProjectId.value = newId
            _activeChatSessionId.value = chatId
            prefsRepository.saveNavigationState(projectId = newId, chatSessionId = chatId)
            _statusToast.value = "Создан новый проект мира «${name.trim()}»"
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            val allProjs = projects.value
            if (allProjs.size <= 1) {
                _statusToast.value = "Нельзя удалить единственный проект"
                return@launch
            }
            val targetProj = allProjs.firstOrNull { it.id == projectId } ?: return@launch
            redWorldRepository.deleteProject(targetProj)
            val fallbackId = allProjs.firstOrNull { it.id != projectId }?.id ?: 1L
            _activeProjectId.value = fallbackId
            prefsRepository.saveNavigationState(projectId = fallbackId)
            _statusToast.value = "Проект удалён"
        }
    }

    // ================= GOOGLE ACCOUNT & NOTEBOOKLM INTEGRATION ACTIONS =================

    fun connectGoogleAccountAndPullNotebooks(
        email: String,
        accessToken: String = "",
        folderName: String = "World_AI_Studio_Sync"
    ) {
        val cleanEmail = email.trim().ifEmpty { "hobot.mc@gmail.com" }
        viewModelScope.launch {
            _isSyncingNotebookLm.value = true
            prefsRepository.updateGoogleConnection(
                email = cleanEmail,
                driveConnected = true,
                accessToken = accessToken.trim(),
                folderName = folderName
            )

            // If user provided an OAuth access token, also query Google Drive for NotebookLM/Doc sources
            if (accessToken.isNotBlank()) {
                val listRes = driveSyncManager.listDriveFiles(accessToken.trim())
                if (listRes is DriveOperationResult.Success && listRes.files.isNotEmpty()) {
                    _driveRemoteFiles.value = listRes.files
                    val driveNotes = listRes.files.take(5).mapIndexed { index, meta ->
                        NotebookLmNoteItem(
                            id = "drive_nlm_${meta.id}_$index",
                            title = "Источник Google Drive: ${meta.name}",
                            content = "Документ «${meta.name}» (${meta.mimeType}), синхронизированный из вашего Google аккаунта $cleanEmail.",
                            suggestedIntegrationType = "KNOWLEDGE",
                            sourceCitation = "Google Drive / NotebookLM ($cleanEmail)"
                        )
                    }
                    redWorldRepository.saveNotebookLmNotebook(
                        NotebookLmNotebookEntity(
                            googleAccountEmail = cleanEmail,
                            notebookTitle = "NotebookLM (Google Drive: $cleanEmail)",
                            notebookCategory = "Облачные источники аккаунта",
                            sourcesCount = driveNotes.size,
                            audioOverviewSummary = "Синхронизировано из Google аккаунта $cleanEmail",
                            notesJson = json.encodeToString(driveNotes)
                        )
                    )
                }
            }

            // Ensure existing NotebookLM notebooks are bound to the authorized Google account
            val currentNotebooks = redWorldRepository.getAllNotebookLmNotebooks()
            currentNotebooks.forEach { nb ->
                redWorldRepository.saveNotebookLmNotebook(nb.copy(googleAccountEmail = cleanEmail))
            }

            _isSyncingNotebookLm.value = false
            _statusToast.value = "Аккаунт $cleanEmail авторизован: подтянуто ${notebookLmNotebooks.value.size.coerceAtLeast(2)} блокнотов Google NotebookLM"
        }
    }

    fun addNoteToNotebookLm(
        notebookId: Long,
        title: String,
        content: String,
        suggestedType: String
    ) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            redWorldRepository.addNoteToNotebookLmNotebook(
                notebookId = notebookId,
                newNote = NotebookLmNoteItem(
                    title = title.trim(),
                    content = content.trim(),
                    suggestedIntegrationType = suggestedType,
                    sourceCitation = "Google NotebookLM (${studioPrefs.value.googleAccountEmail.ifEmpty { "Локальная сессия" }})"
                )
            )
            _statusToast.value = "Заметка «$title» добавлена в блокнот NotebookLM"
        }
    }

    fun createNewNotebookLmNotebook(title: String, description: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val email = studioPrefs.value.googleAccountEmail.ifBlank { "creator.worldstudio@gmail.com" }
            val defaultNote = NotebookLmNoteItem(
                title = "Обзорная заметка: ${title.trim()}",
                content = description.ifBlank { "Стартовая заметка блокнота Google NotebookLM «${title.trim()}» для интеграции в активный мир." },
                suggestedIntegrationType = "KNOWLEDGE",
                sourceCitation = "Google NotebookLM ($email)"
            )
            redWorldRepository.saveNotebookLmNotebook(
                NotebookLmNotebookEntity(
                    googleAccountEmail = email,
                    notebookTitle = title.trim(),
                    notebookCategory = "Авторский блокнот",
                    sourcesCount = 1,
                    audioOverviewSummary = description.trim(),
                    notesJson = json.encodeToString(listOf(defaultNote))
                )
            )
            _statusToast.value = "Создан блокнот NotebookLM «${title.trim()}»"
        }
    }

    /**
     * Integrates a note from a Google NotebookLM notebook directly into the currently active World Project!
     * Supports integrating as:
     * - CHARACTER (AI extracts full character dossier + automatically links relationships!)
     * - LOCATION (creates a new Location in the active world)
     * - WORLD_RULE (creates a new Law/Rule in the active world)
     * - EVENT (creates a new Timeline Event in the active world)
     * - KNOWLEDGE (imports as a RAG Document + Note in the active world's Knowledge base)
     */
    fun integrateNotebookLmNoteIntoActiveWorld(
        notebookId: Long,
        note: NotebookLmNoteItem,
        integrationType: String
    ) {
        val projId = _activeProjectId.value
        val projName = activeProject.value?.name ?: "Проект #$projId"

        viewModelScope.launch {
            val summaryLabel = when (integrationType) {
                "CHARACTER" -> {
                    val dossier = DocumentAndXlsxEngine.parseCharacterDossierLocally(note.content, note.title)
                    val charName = dossier.name.ifBlank {
                        note.title.substringAfter(":").substringBefore("(").trim().ifEmpty { note.title }
                    }
                    val newChar = CharacterEntity(
                        projectId = projId,
                        name = charName,
                        aliases = dossier.aliases,
                        birthDate = dossier.birthDate,
                        age = dossier.age,
                        birthPlace = dossier.birthPlace,
                        appearance = dossier.appearance.ifEmpty { note.content.take(220) },
                        physique = dossier.physique,
                        hair = dossier.hair,
                        eyes = dossier.eyes,
                        defaultClothing = dossier.defaultClothing,
                        distinguishingMarks = dossier.distinguishingMarks,
                        personality = dossier.personality,
                        habits = dossier.habits,
                        speechStyle = dossier.speechStyle,
                        occupation = dossier.occupation,
                        skills = dossier.skills,
                        interests = dossier.interests,
                        preferences = dossier.preferences,
                        goals = dossier.goals,
                        motivation = dossier.motivation,
                        behavioralTraits = dossier.behavioralTraits,
                        currentLocationName = dossier.currentLocationName,
                        currentMoodAndCondition = dossier.currentMoodAndCondition,
                        currentOutfit = dossier.currentOutfit,
                        currentActiveGoal = dossier.currentActiveGoal,
                        visualPassportPrompt = dossier.visualPassportPrompt,
                        relationshipsSummary = dossier.relationshipsSummary,
                        savedPromptsJson = json.encodeToString(dossier.savedPrompts),
                        declaredRelationshipsJson = json.encodeToString(dossier.declaredRelationships),
                        sourceFileName = "NotebookLM: ${note.title}",
                        additionalNotes = "Интегрировано из Google NotebookLM (${note.sourceCitation})",
                        canonStatus = CanonStatus.CANON.name
                    )
                    val savedId = redWorldRepository.saveCharacterWithSnapshot(
                        character = newChar,
                        changeSummary = "Интеграция карточки персонажа из заметки Google NotebookLM «${note.title}»",
                        author = "NotebookLM Интегратор"
                    )
                    _selectedCharacterId.value = savedId
                    val autoLinks = redWorldRepository.lastAutoLinkedSummary
                    val linkSuffix = if (autoLinks.isNotEmpty()) " + автосвязей: ${autoLinks.size}" else ""
                    "Персонаж «$charName» ($projName$linkSuffix)"
                }

                "LOCATION" -> {
                    val locName = note.title.substringAfter(":").trim().ifEmpty { note.title }
                    redWorldRepository.saveLocation(
                        LocationEntity(
                            projectId = projId,
                            name = locName,
                            category = "Локация из NotebookLM",
                            description = note.content,
                            atmosphere = "Источник: ${note.sourceCitation}",
                            canonStatus = CanonStatus.CANON.name
                        )
                    )
                    "Локация «$locName» ($projName)"
                }

                "WORLD_RULE" -> {
                    redWorldRepository.saveWorldRule(
                        WorldRuleEntity(
                            projectId = projId,
                            title = note.title,
                            category = "Канон из NotebookLM",
                            content = note.content,
                            priorityLevel = "Строгий закон",
                            canonStatus = CanonStatus.CANON.name
                        )
                    )
                    "Правило мира «${note.title}» ($projName)"
                }

                "EVENT" -> {
                    redWorldRepository.saveEvent(
                        WorldEventEntity(
                            projectId = projId,
                            title = note.title,
                            eventType = "Событие (NotebookLM)",
                            timelineDate = "Хроника проекта",
                            chronologyOrder = 20,
                            locationName = locations.value.firstOrNull()?.name ?: "Красный особняк",
                            description = note.content,
                            consequences = "Интегрировано из блокнота Google NotebookLM (${note.sourceCitation})",
                            canonStatus = CanonStatus.CANON.name
                        )
                    )
                    "Событие «${note.title}» ($projName)"
                }

                else -> {
                    val docId = knowledgeRepository.createOrUpdateDocument(
                        title = note.title,
                        fileName = "notebooklm_${note.id}.md",
                        format = DocumentFormat.MARKDOWN,
                        folderName = "Импорт из NotebookLM",
                        collectionName = projName,
                        contentText = "# ${note.title}\n\n${note.content}\n\n> Источник: ${note.sourceCitation}",
                        projectId = projId
                    )
                    knowledgeRepository.insertNote(
                        title = note.title,
                        content = note.content,
                        documentId = docId,
                        quotedExcerpt = note.sourceCitation
                    )
                    "RAG-документ и заметка «${note.title}» ($projName)"
                }
            }

            redWorldRepository.markNotebookLmNoteIntegrated(
                notebookId = notebookId,
                noteId = note.id,
                targetWorldSummary = summaryLabel
            )
            _statusToast.value = "Заметка интегрирована в мир: $summaryLabel"
        }
    }

    // ================= CHARACTER CARD AI FILE EXTRACTION & AUTO-LINKING =================

    /**
     * Reads any uploaded file (.doc, .docx, .pdf, .txt, .md, .xlsx), uses Gemini AI (plus local neural/heuristic parser)
     * to extract all character card fields, visual passport, prompts, and relationships!
     */
    fun extractCharacterDossierFromUri(
        context: Context,
        uri: Uri,
        fileName: String,
        onExtracted: (AiExtractedCharacterDossier, String) -> Unit
    ) {
        viewModelScope.launch {
            _isExtractingCharacterDossier.value = true
            try {
                val (format, rawText) = DocumentAndXlsxEngine.extractTextFromUri(context, uri, fileName)
                // Also save the uploaded file into the active project's Knowledge repository
                knowledgeRepository.createOrUpdateDocument(
                    title = "Досье: ${fileName.substringBeforeLast('.')}",
                    fileName = fileName,
                    format = format,
                    folderName = "Досье персонажей",
                    collectionName = activeProject.value?.name ?: "Основной канон",
                    contentText = rawText,
                    projectId = _activeProjectId.value
                )
                val dossier = extractDossierFromTextWithAiOrFallback(rawText, fileName)
                onExtracted(dossier, fileName)
                val relCount = dossier.declaredRelationships.size
                _statusToast.value = "Файл «$fileName» прочитан ИИ: заполнена карточка «${dossier.name}» (найдено связей: $relCount)"
            } catch (e: Exception) {
                _aiErrorBanner.value = "Ошибка чтения файла персонажа: ${e.localizedMessage}"
            } finally {
                _isExtractingCharacterDossier.value = false
            }
        }
    }

    fun extractCharacterDossierFromSampleDoc(
        sampleFileName: String,
        onExtracted: (AiExtractedCharacterDossier, String) -> Unit
    ) {
        viewModelScope.launch {
            _isExtractingCharacterDossier.value = true
            try {
                val samplePair = DocumentAndXlsxEngine.getSampleCharacterDocDossiers()
                    .firstOrNull { it.first == sampleFileName }
                    ?: DocumentAndXlsxEngine.getSampleCharacterDocDossiers().first()

                val (fName, rawText) = samplePair
                knowledgeRepository.createOrUpdateDocument(
                    title = "Досье: ${fName.substringBeforeLast('.')}",
                    fileName = fName,
                    format = DocumentFormat.DOCX,
                    folderName = "Досье персонажей",
                    collectionName = activeProject.value?.name ?: "Основной канон",
                    contentText = rawText,
                    projectId = _activeProjectId.value
                )
                val dossier = extractDossierFromTextWithAiOrFallback(rawText, fName)
                onExtracted(dossier, fName)
                _statusToast.value = "Досье «$fName» обработано нейросетью: все поля и связи (${dossier.declaredRelationships.size}) заполнены!"
            } finally {
                _isExtractingCharacterDossier.value = false
            }
        }
    }

    private suspend fun extractDossierFromTextWithAiOrFallback(
        rawText: String,
        fileName: String
    ): AiExtractedCharacterDossier {
        val localParsed = DocumentAndXlsxEngine.parseCharacterDossierLocally(rawText, fileName)
        val apiKey = studioPrefs.value.activeProviderEffectiveApiKey
        if (apiKey.isBlank()) {
            return localParsed
        }

        val existingCharNames = characters.value.joinToString(", ") { it.name }
        val existingLocNames = locations.value.joinToString(", ") { it.name }

        val extractionPrompt = """
            Прочитай прикреплённый текст досье персонажа из файла "$fileName" и верни СТРОГО валидный JSON-объект (без markdown-обёрток ```json) по схеме AiExtractedCharacterDossier:
            {
              "name": "...",
              "aliases": "...",
              "birthDate": "...",
              "age": "...",
              "birthPlace": "...",
              "appearance": "...",
              "physique": "...",
              "hair": "...",
              "eyes": "...",
              "defaultClothing": "...",
              "distinguishingMarks": "...",
              "personality": "...",
              "habits": "...",
              "speechStyle": "...",
              "occupation": "...",
              "skills": "...",
              "interests": "...",
              "preferences": "...",
              "goals": "...",
              "motivation": "...",
              "behavioralTraits": "...",
              "currentLocationName": "...",
              "currentMoodAndCondition": "...",
              "currentOutfit": "...",
              "currentActiveGoal": "...",
              "visualPassportPrompt": "Detailed English visual prompt for generating this character...",
              "relationshipsSummary": "...",
              "additionalNotes": "...",
              "savedPrompts": [
                {"id": "p1", "title": "Портрет персонажа", "promptText": "...", "category": "Портрет"}
              ],
              "declaredRelationships": [
                {"targetName": "Имя другого персонажа или локации", "targetType": "CHARACTER", "relationType": "Знаком с", "description": "..."}
              ]
            }
            Учитывай уже существующих в мире персонажей ($existingCharNames) и локации ($existingLocNames), а также обязательно включи в declaredRelationships ВСЕХ упомянутых в тексте других персонажей (даже если их ещё нет в базе — связь с ними подтянется автоматически при их добавлении!).
            
            ТЕКСТ ФАЙЛА:
            $rawText
        """.trimIndent()

        val result = generateActiveProviderChat(
            conversationHistory = listOf(Content(role = "user", parts = listOf(Part(text = extractionPrompt))))
        )

        if (result is GeminiResult.Success) {
            try {
                val cleanedJson = result.data
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                val aiDossier = json.decodeFromString<AiExtractedCharacterDossier>(cleanedJson)
                return aiDossier.copy(
                    name = aiDossier.name.ifBlank { localParsed.name },
                    declaredRelationships = (aiDossier.declaredRelationships + localParsed.declaredRelationships)
                        .distinctBy { it.targetName.lowercase() },
                    savedPrompts = (aiDossier.savedPrompts + localParsed.savedPrompts)
                        .distinctBy { it.title.lowercase() }
                )
            } catch (_: Exception) {
                return localParsed
            }
        }
        return localParsed
    }

    fun saveCharacterAndAutoLink(character: CharacterEntity, changeSummary: String) {
        viewModelScope.launch {
            val isNew = character.id == 0L
            val charWithProject = character.copy(
                projectId = if (character.projectId > 0L) character.projectId else _activeProjectId.value
            )
            val savedId = redWorldRepository.saveCharacterWithSnapshot(
                character = charWithProject,
                changeSummary = changeSummary
            )
            if (!isNew) {
                _selectedCharacterId.value = savedId
            }
            val autoLinked = redWorldRepository.lastAutoLinkedSummary
            if (autoLinked.isNotEmpty()) {
                _statusToast.value = "Сохранено: «${charWithProject.name}». Автоматически подтянуты связи (${autoLinked.size}): ${autoLinked.joinToString("; ")}"
            } else {
                _statusToast.value = "Карточка персонажа «${charWithProject.name}» сохранена"
            }
        }
    }

    fun deleteCharacterAndReturnToList(character: CharacterEntity) {
        viewModelScope.launch {
            if (_selectedCharacterId.value == character.id) {
                _selectedCharacterId.value = null
            }
            redWorldRepository.deleteCharacter(character)
            _statusToast.value = "Персонаж «${character.name}» удалён"
        }
    }

    // ================= CHARACTER AVATAR, REFERENCES & PROMPTS ACTIONS =================

    fun updateCharacterAvatar(character: CharacterEntity, avatarBase64: String) {
        viewModelScope.launch {
            redWorldRepository.saveCharacterWithSnapshot(
                character = character.copy(avatarBase64 = avatarBase64),
                changeSummary = "Обновлена аватарка персонажа «${character.name}»"
            )
            _statusToast.value = "Аватарка персонажа «${character.name}» обновлена"
        }
    }

    fun addCharacterReferenceImage(
        character: CharacterEntity,
        title: String,
        base64Data: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val currentList = try {
                json.decodeFromString<List<CharacterReferenceImage>>(character.referenceImagesJson)
            } catch (_: Exception) {
                emptyList()
            }
            val updatedList = currentList + CharacterReferenceImage(
                title = title.ifBlank { "Референс #${currentList.size + 1}" },
                base64Data = base64Data,
                notes = notes
            )
            redWorldRepository.saveCharacterWithSnapshot(
                character = character.copy(referenceImagesJson = json.encodeToString(updatedList)),
                changeSummary = "Добавлен референс «$title» в карточку «${character.name}»"
            )
            _statusToast.value = "Референс добавлен в карточку «${character.name}» и доступен в Image Studio"
        }
    }

    fun removeCharacterReferenceImage(character: CharacterEntity, refId: String) {
        viewModelScope.launch {
            val currentList = try {
                json.decodeFromString<List<CharacterReferenceImage>>(character.referenceImagesJson)
            } catch (_: Exception) {
                emptyList()
            }
            val updatedList = currentList.filterNot { it.id == refId }
            redWorldRepository.saveCharacterWithSnapshot(
                character = character.copy(referenceImagesJson = json.encodeToString(updatedList)),
                changeSummary = "Удалён референс из карточки «${character.name}»"
            )
        }
    }

    fun addCharacterSavedPrompt(
        character: CharacterEntity,
        title: String,
        promptText: String,
        category: String = "Портрет"
    ) {
        if (promptText.isBlank()) return
        viewModelScope.launch {
            val currentList = try {
                json.decodeFromString<List<CharacterSavedPrompt>>(character.savedPromptsJson)
            } catch (_: Exception) {
                emptyList()
            }
            val updatedList = currentList + CharacterSavedPrompt(
                title = title.ifBlank { "Промпт #${currentList.size + 1}" },
                promptText = promptText.trim(),
                category = category
            )
            redWorldRepository.saveCharacterWithSnapshot(
                character = character.copy(savedPromptsJson = json.encodeToString(updatedList)),
                changeSummary = "Добавлен промпт для генерации «$title» в карточку «${character.name}»"
            )
            _statusToast.value = "Промпт «$title» сохранён в карточке «${character.name}»"
        }
    }

    fun removeCharacterSavedPrompt(character: CharacterEntity, promptId: String) {
        viewModelScope.launch {
            val currentList = try {
                json.decodeFromString<List<CharacterSavedPrompt>>(character.savedPromptsJson)
            } catch (_: Exception) {
                emptyList()
            }
            val updatedList = currentList.filterNot { it.id == promptId }
            redWorldRepository.saveCharacterWithSnapshot(
                character = character.copy(savedPromptsJson = json.encodeToString(updatedList)),
                changeSummary = "Удалён промпт из карточки «${character.name}»"
            )
        }
    }

    fun addDeclaredRelationToCharacter(
        character: CharacterEntity,
        relation: DeclaredCharacterRelation
    ) {
        viewModelScope.launch {
            val currentList = try {
                json.decodeFromString<List<DeclaredCharacterRelation>>(character.declaredRelationshipsJson)
            } catch (_: Exception) {
                emptyList()
            }
            val updatedList = currentList + relation
            val updatedSummary = (character.relationshipsSummary + "; ${relation.relationType}: ${relation.targetName} (${relation.description})").trimStart(';', ' ')
            saveCharacterAndAutoLink(
                character = character.copy(
                    declaredRelationshipsJson = json.encodeToString(updatedList),
                    relationshipsSummary = updatedSummary
                ),
                changeSummary = "Добавлена связь с «${relation.targetName}»"
            )
        }
    }

    fun openImageStudioWithCharacterPromptAndRef(
        character: CharacterEntity,
        promptText: String? = null,
        referenceBase64: String? = null,
        referenceTitle: String? = null
    ) {
        _selectedCharacterId.value = character.id
        if (!promptText.isNullOrBlank()) {
            updateImagePromptDraft(promptText)
        } else if (character.visualPassportPrompt.isNotBlank()) {
            updateImagePromptDraft(character.visualPassportPrompt)
        }
        _imageStudioPreselectedRefBase64.value = referenceBase64 ?: character.avatarBase64.takeIf { it.isNotBlank() }
        _imageStudioPreselectedRefTitle.value = referenceTitle ?: if (character.avatarBase64.isNotBlank()) "Аватарка: ${character.name}" else null
        _currentBottomTab.value = MainBottomTab.GENERATION
        _secondaryScreen.value = SecondaryWorkspaceScreen.NONE
        _statusToast.value = "Открыта Image Studio с данными персонажа «${character.name}»"
    }

    // ================= NAVIGATION ACTIONS =================

    fun selectBottomTab(tab: MainBottomTab) {
        _currentBottomTab.value = tab
        _secondaryScreen.value = SecondaryWorkspaceScreen.NONE
        if (tab == MainBottomTab.REDWORLD) {
            _selectedCharacterId.value = null
        }
        viewModelScope.launch {
            prefsRepository.saveNavigationState(tab = tab.route)
        }
    }

    fun openSecondaryScreen(screen: SecondaryWorkspaceScreen) {
        if (screen == SecondaryWorkspaceScreen.CHARACTERS) {
            _selectedCharacterId.value = null
        }
        _secondaryScreen.value = screen
    }

    fun closeSecondaryScreen() {
        _secondaryScreen.value = SecondaryWorkspaceScreen.NONE
    }

    fun openDocumentInKnowledge(documentId: Long) {
        _selectedDocumentId.value = documentId
        _secondaryScreen.value = SecondaryWorkspaceScreen.KNOWLEDGE_RAG
        viewModelScope.launch {
            knowledgeRepository.markDocumentOpened(documentId)
            prefsRepository.saveNavigationState(documentId = documentId)
        }
    }

    fun openCharacterInEditor(characterId: Long) {
        _selectedCharacterId.value = characterId
        _currentBottomTab.value = MainBottomTab.REDWORLD
        _secondaryScreen.value = SecondaryWorkspaceScreen.NONE
        viewModelScope.launch {
            prefsRepository.saveNavigationState(
                tab = MainBottomTab.REDWORLD.route,
                characterId = characterId
            )
        }
    }

    fun clearStatusToast() {
        _statusToast.value = null
    }

    fun clearAiError() {
        _aiErrorBanner.value = null
    }

    // ================= GEMINI CHAT & RAG CONTEXT ACTIONS =================

    fun updateChatInputDraft(text: String) {
        _chatInputDraft.value = text
        viewModelScope.launch {
            prefsRepository.saveUnsentPromptDraft(text)
        }
    }

    fun updateChatSearchQuery(query: String) {
        _chatSearchQuery.value = query
    }

    fun selectChatSession(sessionId: Long) {
        _activeChatSessionId.value = sessionId
        viewModelScope.launch {
            prefsRepository.saveNavigationState(chatSessionId = sessionId)
        }
    }

    fun createNewChatSession(title: String = "Новый диалог мира") {
        viewModelScope.launch {
            val allDocIds = documentsWithDetails.value.map { it.document.id }.joinToString(",")
            val newId = redWorldRepository.createChatSession(
                title = title,
                modelId = studioPrefs.value.selectedTextModel,
                isRedWorldContextEnabled = true,
                selectedDocumentIdsCsv = allDocIds,
                projectId = _activeProjectId.value
            )
            _activeChatSessionId.value = newId
            _statusToast.value = "Создан диалог «$title»"
        }
    }

    fun renameChatSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            redWorldRepository.renameChatSession(sessionId, newTitle)
            _statusToast.value = "Диалог переименован"
        }
    }

    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch {
            redWorldRepository.deleteChatSession(sessionId)
            val remaining = chatSessions.value.filter { it.id != sessionId }
            if (remaining.isNotEmpty()) {
                _activeChatSessionId.value = remaining.first().id
            } else {
                createNewChatSession("Основной диалог")
            }
        }
    }

    fun toggleSessionRedWorldContext(session: ChatSessionEntity, enabled: Boolean) {
        viewModelScope.launch {
            redWorldRepository.updateChatSessionSettings(
                sessionId = session.id,
                isRedWorldContextEnabled = enabled
            )
        }
    }

    fun toggleDocumentInSessionContext(session: ChatSessionEntity, documentId: Long) {
        viewModelScope.launch {
            val currentSet = session.selectedDocumentIdsCsv
                .split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .toMutableSet()
            if (currentSet.contains(documentId)) {
                currentSet.remove(documentId)
            } else {
                currentSet.add(documentId)
            }
            redWorldRepository.updateChatSessionSettings(
                sessionId = session.id,
                selectedDocumentIdsCsv = currentSet.joinToString(",")
            )
        }
    }

    fun setSessionAssistantMode(session: ChatSessionEntity, mode: String) {
        viewModelScope.launch {
            redWorldRepository.updateChatSessionSettings(
                sessionId = session.id,
                assistantMode = mode
            )
        }
    }

    fun attachImageToChat(base64Data: String, fileName: String) {
        _attachedImageBase64.value = base64Data
        _attachedFileName.value = fileName
    }

    fun clearChatAttachment() {
        _attachedImageBase64.value = null
        _attachedFileName.value = null
    }

    fun stopCurrentGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _isAiGenerating.value = false
        _statusToast.value = "Генерация ответа остановлена пользователем"
    }

    fun sendChatMessage(overridePrompt: String? = null) {
        val rawPrompt = (overridePrompt ?: _chatInputDraft.value).trim()
        if (rawPrompt.isEmpty() || _isAiGenerating.value) return

        val sessionId = _activeChatSessionId.value
        val currentSession = chatSessions.value.firstOrNull { it.id == sessionId }
        val activeProvider = studioPrefs.value.activeProviderType
        val modelId = studioPrefs.value.activeProviderTextModel
        val modelBadgeLabel = "${activeProvider.displayName} • $modelId"
        val useContext = currentSession?.isRedWorldContextEnabled ?: true
        val assistantMode = currentSession?.assistantMode ?: "CONSULTATION"
        val selectedDocIds = currentSession?.selectedDocumentIdsCsv
            ?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?.toSet()

        val attachedImg = _attachedImageBase64.value
        val attachedName = _attachedFileName.value.orEmpty()

        if (overridePrompt == null) {
            updateChatInputDraft("")
            clearChatAttachment()
        }
        _aiErrorBanner.value = null
        _isAiGenerating.value = true

        activeGenerationJob = viewModelScope.launch {
            try {
                redWorldRepository.insertChatMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "user",
                        content = rawPrompt,
                        attachmentSummary = attachedName,
                        modelUsed = modelBadgeLabel
                    )
                )

                val citedSnippets: List<CitedSourceSnippet> = if (useContext) {
                    knowledgeRepository.retrieveRelevantChunksForQuery(
                        query = rawPrompt,
                        allowedDocumentIds = selectedDocIds?.takeIf { it.isNotEmpty() },
                        maxChunks = 4
                    )
                } else {
                    emptyList()
                }

                val projTitle = activeProject.value?.name ?: "RedWorld"
                val systemInstruction = buildString {
                    appendLine("Ты — интеллектуальное ядро World AI Studio (активный проект: «$projTitle»). Отвечай на русском языке чётко, структурированно, используя Markdown.")
                    appendLine("Режим работы: $assistantMode.")
                    appendLine("КРИТИЧЕСКОЕ ПРАВИЛО КАНОНА: Никогда не выдумывай несуществующие ссылки на источники. Если точного ответа нет в переданных фрагментах документов, прямо сообщи об этом и отдели канонические факты от предположений.")
                    if (useContext) {
                        if (citedSnippets.isNotEmpty()) {
                            appendLine("\n--- РЕЛЕВАНТНЫЕ ФРАГМЕНТЫ ИЗ БАЗЫ ЗНАНИЙ ПРОЕКТА ---")
                            citedSnippets.forEachIndexed { idx, snippet ->
                                appendLine("[Источник #${idx + 1}: Документ «${snippet.documentTitle}», раздел «${snippet.sectionTitle}»]")
                                appendLine(snippet.excerpt)
                                appendLine()
                            }
                        } else {
                            appendLine("\nВ выбранных документах прямых совпадений по ключевым словам не найдено. Опирайся только на прямо указанные пользователем сведения.")
                        }
                        val charBrief = characters.value.take(6).joinToString("; ") {
                            "${it.name} (статус: ${it.canonStatus}, локация сейчас: ${it.currentLocationName}, род.: ${it.birthDate})"
                        }
                        if (charBrief.isNotEmpty()) {
                            appendLine("Сводка карточек персонажей мира «$projTitle»: $charBrief")
                        }
                    } else {
                        appendLine("Контекст проекта отключён пользователем для этого диалога.")
                    }
                }

                val historyRecords = redWorldRepository.getMessagesForSession(sessionId).takeLast(8)
                val contents = historyRecords.mapIndexed { idx, msg ->
                    val role = if (msg.role == "model") "model" else "user"
                    val isLastUserMsg = (idx == historyRecords.lastIndex && role == "user")
                    val parts = mutableListOf<Part>()
                    parts.add(Part(text = msg.content))
                    if (isLastUserMsg && !attachedImg.isNullOrBlank()) {
                        parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = attachedImg)))
                    }
                    Content(role = role, parts = parts)
                }

                val result = generateActiveProviderChat(
                    conversationHistory = contents,
                    systemInstructionText = systemInstruction,
                    overrideProvider = activeProvider,
                    overrideModelId = modelId
                )

                val citedJson = json.encodeToString(citedSnippets)

                when (result) {
                    is GeminiResult.Success -> {
                        val proposalJson = if (assistantMode == "PROPOSAL" || rawPrompt.lowercase().contains("предложи измен")) {
                            val targetChar = characters.value.firstOrNull()
                            if (targetChar != null) {
                                json.encodeToString(
                                    AiChangeProposal(
                                        proposalTitle = "Предложение обновления карточки «${targetChar.name}»",
                                        targetEntityType = "CHARACTER",
                                        targetEntityName = targetChar.name,
                                        fieldToUpdate = "additionalNotes",
                                        previousValue = targetChar.additionalNotes.ifEmpty { "(пусто)" },
                                        proposedValue = result.data.take(280),
                                        proposedStatus = CanonStatus.DRAFT.name,
                                        rationale = "Подготовлено ИИ-куратором на основе запроса: $rawPrompt",
                                        sourceDocumentsUsed = citedSnippets.map { it.documentTitle }.distinct()
                                    )
                                )
                            } else ""
                        } else ""

                        redWorldRepository.insertChatMessage(
                            ChatMessageEntity(
                                sessionId = sessionId,
                                role = "model",
                                content = result.data,
                                citedSourcesJson = citedJson,
                                proposedChangeJson = proposalJson,
                                modelUsed = modelBadgeLabel
                            )
                        )
                    }
                    is GeminiResult.Error -> {
                        _aiErrorBanner.value = result.userMessage
                        val ragFallbackText = buildString {
                            appendLine("⚠️ **Не удалось получить ответ от облачной модели ${activeProvider.displayName} ($modelId)**")
                            appendLine("> ${result.userMessage}")
                            if (citedSnippets.isNotEmpty()) {
                                appendLine("\n### Найдено в локальной базе знаний проекта (офлайн-режим RAG):")
                                citedSnippets.forEach { s ->
                                    appendLine("- **${s.documentTitle}** (${s.sectionTitle}):\n  «${s.excerpt}»")
                                }
                            } else {
                                appendLine("\n*В локальных документах точных текстовых совпадений по данному запросу не обнаружено.*")
                            }
                        }
                        redWorldRepository.insertChatMessage(
                            ChatMessageEntity(
                                sessionId = sessionId,
                                role = "model",
                                content = ragFallbackText,
                                citedSourcesJson = citedJson,
                                modelUsed = "$modelBadgeLabel (Локальный RAG)"
                            )
                        )
                    }
                }
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    fun regenerateLastResponse() {
        val messages = activeChatMessages.value
        val lastUserMsg = messages.lastOrNull { it.role == "user" } ?: return
        val lastModelMsg = messages.lastOrNull()
        viewModelScope.launch {
            if (lastModelMsg != null && lastModelMsg.role == "model") {
                redWorldRepository.deleteChatMessage(lastModelMsg.id)
            }
            sendChatMessage(overridePrompt = lastUserMsg.content)
        }
    }

    fun confirmAndApplyAiProposal(message: ChatMessageEntity, proposal: AiChangeProposal) {
        viewModelScope.launch {
            val targetChar = characters.value.firstOrNull {
                it.name.equals(proposal.targetEntityName, ignoreCase = true)
            } ?: characters.value.firstOrNull()

            if (targetChar != null) {
                val updatedChar = targetChar.copy(
                    additionalNotes = (targetChar.additionalNotes + "\n[ИИ-черновик]: " + proposal.proposedValue).trim()
                )
                redWorldRepository.saveCharacterWithSnapshot(
                    character = updatedChar,
                    changeSummary = "Применено подтверждённое предложение ИИ: ${proposal.proposalTitle}",
                    author = "ИИ (Подтверждено пользователем)"
                )
            }
            redWorldRepository.markProposalApplied(message)
            _statusToast.value = "Изменение подтверждено и сохранено в новой версии v${(targetChar?.versionNumber ?: 1) + 1}"
        }
    }

    // ================= IMAGE STUDIO ACTIONS =================

    fun updateImagePromptDraft(prompt: String) {
        _imagePromptDraft.value = prompt
        viewModelScope.launch {
            prefsRepository.saveUnsentImagePromptDraft(prompt)
        }
    }

    fun applyCharacterVisualPassportToPrompt(character: CharacterEntity, templatePrefix: String = "") {
        val passport = character.visualPassportPrompt.ifBlank {
            "Character portrait of ${character.name}, ${character.appearance}, hair: ${character.hair}, eyes: ${character.eyes}, wearing ${character.defaultClothing}, gothic crimson atmosphere"
        }
        val combined = if (templatePrefix.isNotBlank()) "$templatePrefix. $passport" else passport
        updateImagePromptDraft(combined)
        _statusToast.value = "Паспорт внешности «${character.name}» подставлен в промпт. Вы можете отредактировать его перед отправкой."
    }

    fun generateStudioImage(
        prompt: String,
        templateCategory: String,
        modelId: String,
        aspectRatio: String,
        linkedCharacter: CharacterEntity?,
        linkedLocation: LocationEntity?,
        referenceImageBase64: String? = null
    ) {
        if (prompt.isBlank() || _isImageGenerating.value) return
        _isImageGenerating.value = true
        _aiErrorBanner.value = null

        viewModelScope.launch {
            try {
                val apiKey = studioPrefs.value.effectiveGeminiApiKey
                val result = geminiRemote.generateImage(
                    apiKey = apiKey,
                    modelId = modelId,
                    prompt = prompt,
                    aspectRatio = aspectRatio,
                    referenceImageBase64 = referenceImageBase64
                )

                when (result) {
                    is GeminiResult.Success -> {
                        redWorldRepository.saveGeneratedImage(
                            GeneratedImageEntity(
                                projectId = _activeProjectId.value,
                                prompt = prompt,
                                templateCategory = templateCategory,
                                modelId = modelId,
                                aspectRatio = aspectRatio,
                                imageBase64OrPath = result.data.imageBase64.orEmpty(),
                                linkedCharacterId = linkedCharacter?.id,
                                linkedCharacterName = linkedCharacter?.name.orEmpty(),
                                linkedLocationId = linkedLocation?.id,
                                linkedLocationName = linkedLocation?.name.orEmpty(),
                                usedVisualPassport = linkedCharacter != null
                            )
                        )
                        _statusToast.value = "Изображение сгенерировано и сохранено в галерее Image Studio"
                    }
                    is GeminiResult.Error -> {
                        redWorldRepository.saveGeneratedImage(
                            GeneratedImageEntity(
                                projectId = _activeProjectId.value,
                                prompt = prompt,
                                templateCategory = templateCategory,
                                modelId = modelId,
                                aspectRatio = aspectRatio,
                                imageBase64OrPath = "",
                                linkedCharacterId = linkedCharacter?.id,
                                linkedCharacterName = linkedCharacter?.name.orEmpty(),
                                linkedLocationId = linkedLocation?.id,
                                linkedLocationName = linkedLocation?.name.orEmpty(),
                                usedVisualPassport = linkedCharacter != null
                            )
                        )
                        _aiErrorBanner.value = result.userMessage
                    }
                }
            } finally {
                _isImageGenerating.value = false
            }
        }
    }

    // ================= XLSX SURGICAL EDITOR ACTIONS =================

    fun editXlsxCellInPlace(sheetZipEntryPath: String, cellRef: String, newValue: String) {
        val file = currentXlsxFile ?: return
        viewModelScope.launch {
            val (backupFile, updatedWorkbook) = DocumentAndXlsxEngine.updateSingleCellSurgically(
                context = getApplication(),
                xlsxFile = file,
                sheetZipEntryPath = sheetZipEntryPath,
                cellRef = cellRef,
                newValue = newValue
            )
            previousXlsxBackupFile = backupFile
            _activeXlsxWorkbook.value = updatedWorkbook
            val diffs = DocumentAndXlsxEngine.compareXlsxVersions(backupFile, file)
            _xlsxLastDiffs.value = diffs

            redWorldRepository.recordXlsxVersionSnapshot(
                fileName = file.name,
                changeSummary = "Точечное изменение ячейки $cellRef → «$newValue» (формулы и объединённые ячейки сохранены)",
                backupFilePath = backupFile.absolutePath,
                diffsJson = json.encodeToString(diffs),
                projectId = _activeProjectId.value
            )
            _statusToast.value = "Ячейка $cellRef обновлена. Создана резервная версия: ${backupFile.name}"
        }
    }

    fun revertXlsxToLastBackup() {
        val backup = previousXlsxBackupFile
        val current = currentXlsxFile
        if (backup == null || !backup.exists() || current == null) {
            _statusToast.value = "Нет предыдущей резервной версии XLSX для отката"
            return
        }
        viewModelScope.launch {
            backup.copyTo(current, overwrite = true)
            _activeXlsxWorkbook.value = DocumentAndXlsxEngine.parseXlsxFile(current)
            _xlsxLastDiffs.value = emptyList()
            _statusToast.value = "Изменения отменены: восстановлена резервная копия ${backup.name}"
        }
    }

    fun importExternalXlsxUri(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val dir = File(context.filesDir, "xlsx_workbooks").apply { mkdirs() }
                val targetFile = File(dir, fileName.ifEmpty { "imported_cards.xlsx" })
                targetFile.writeBytes(bytes)
                currentXlsxFile = targetFile
                _activeXlsxWorkbook.value = DocumentAndXlsxEngine.parseXlsxFile(targetFile)
                knowledgeRepository.importDocumentFromUri(
                    context = context,
                    uri = uri,
                    fileName = targetFile.name,
                    folderName = "Таблицы XLSX",
                    collectionName = activeProject.value?.name ?: "Карточки проекта",
                    projectId = _activeProjectId.value
                )
                _statusToast.value = "Таблица XLSX «${targetFile.name}» загружена с сохранением всех листов и формул"
            } catch (e: Exception) {
                _aiErrorBanner.value = "Ошибка импорта XLSX: ${e.localizedMessage}"
            }
        }
    }

    fun exportActiveXlsxToSaf(uri: Uri) {
        val file = currentXlsxFile ?: return
        viewModelScope.launch {
            val res = driveSyncManager.exportLocalFileToSafUri(file, uri)
            res.onSuccess { msg -> _statusToast.value = msg }
                .onFailure { err -> _aiErrorBanner.value = "Ошибка экспорта XLSX: ${err.localizedMessage}" }
        }
    }

    // ================= GOOGLE DRIVE & BACKUP ACTIONS =================

    fun buildFullProjectBackupJson(): String {
        val projName = activeProject.value?.name ?: "World AI Studio"
        val summaryObj = mapOf(
            "app" to "World AI Studio",
            "activeProject" to projName,
            "projectsCount" to projects.value.size.toString(),
            "exportedAt" to System.currentTimeMillis().toString(),
            "charactersCount" to characters.value.size.toString(),
            "locationsCount" to locations.value.size.toString(),
            "documentsCount" to documentsWithDetails.value.size.toString(),
            "rulesCount" to worldRules.value.size.toString(),
            "relationshipsCount" to relationships.value.size.toString()
        )
        return buildString {
            appendLine("{")
            appendLine("""  "project": "$projName",""")
            appendLine("""  "exportedAt": ${System.currentTimeMillis()},""")
            appendLine("""  "summary": ${json.encodeToString(summaryObj)},""")
            appendLine("""  "projects": ${json.encodeToString(projects.value)},""")
            appendLine("""  "characters": ${json.encodeToString(characters.value)},""")
            appendLine("""  "locations": ${json.encodeToString(locations.value)},""")
            appendLine("""  "rules": ${json.encodeToString(worldRules.value)},""")
            appendLine("""  "events": ${json.encodeToString(worldEvents.value)},""")
            appendLine("""  "relationships": ${json.encodeToString(relationships.value)},""")
            appendLine("""  "documents": ${json.encodeToString(documentsWithDetails.value.map { it.document })}""")
            appendLine("}")
        }
    }

    fun exportProjectBackupToUri(uri: Uri) {
        viewModelScope.launch {
            _isDriveSyncing.value = true
            val payload = buildFullProjectBackupJson()
            val result = driveSyncManager.exportBackupToSafUri(uri, payload)
            result.onSuccess { msg ->
                prefsRepository.markDriveSynced()
                _statusToast.value = msg
            }.onFailure { e ->
                _aiErrorBanner.value = "Ошибка сохранения резервной копии: ${e.localizedMessage}"
            }
            _isDriveSyncing.value = false
        }
    }

    fun inspectAndImportBackupUri(uri: Uri) {
        viewModelScope.launch {
            _isDriveSyncing.value = true
            val latestLocalTs = characters.value.maxOfOrNull { it.updatedAt } ?: System.currentTimeMillis()
            val result = driveSyncManager.inspectBackupFromSafUri(
                uri = uri,
                currentLocalTimestamp = latestLocalTs,
                currentLocalSummary = "Проектов: ${projects.value.size}, Персонажей: ${characters.value.size}, Документов: ${documentsWithDetails.value.size}"
            )
            when (result) {
                is DriveOperationResult.ConflictDetected -> {
                    _detectedConflict.value = result.conflict
                }
                is DriveOperationResult.Success -> {
                    prefsRepository.markDriveSynced()
                    _statusToast.value = result.message
                }
                is DriveOperationResult.Error -> {
                    _aiErrorBanner.value = result.message
                }
            }
            _isDriveSyncing.value = false
        }
    }

    fun dismissConflictDialog() {
        _detectedConflict.value = null
    }

    fun syncWithGoogleDriveRestApi() {
        viewModelScope.launch {
            _isDriveSyncing.value = true
            val token = studioPrefs.value.googleDriveAccessToken
            val listResult = driveSyncManager.listDriveFiles(token)
            when (listResult) {
                is DriveOperationResult.Success -> {
                    _driveRemoteFiles.value = listResult.files
                    val uploadRes = driveSyncManager.uploadBackupToDriveApi(
                        accessToken = token,
                        fileName = "WorldAIStudio_Backup_${System.currentTimeMillis()}.json",
                        contentJson = buildFullProjectBackupJson()
                    )
                    if (uploadRes is DriveOperationResult.Success) {
                        prefsRepository.markDriveSynced()
                        _statusToast.value = uploadRes.message
                    } else if (uploadRes is DriveOperationResult.Error) {
                        _aiErrorBanner.value = uploadRes.message
                    }
                }
                is DriveOperationResult.Error -> {
                    _aiErrorBanner.value = listResult.message
                }
                else -> {}
            }
            _isDriveSyncing.value = false
        }
    }

    // ================= SETTINGS & MULTI-PROVIDER AI DATASTORE ACTIONS =================

    private val _isVerifyingGeminiKey = MutableStateFlow(false)
    val isVerifyingGeminiKey: StateFlow<Boolean> = _isVerifyingGeminiKey.asStateFlow()

    private val _geminiKeyVerificationMessage = MutableStateFlow<String?>(null)
    val geminiKeyVerificationMessage: StateFlow<String?> = _geminiKeyVerificationMessage.asStateFlow()

    fun selectAiProvider(provider: AiProviderType) {
        viewModelScope.launch {
            prefsRepository.setSelectedAiProvider(provider)
            _geminiKeyVerificationMessage.value = null
            _statusToast.value = "Активный ИИ-провайдер: ${provider.displayName} (${studioPrefs.value.effectiveTextModelFor(provider)})"
        }
    }

    fun saveGeminiApiKeyToDataStore(apiKey: String) {
        saveProviderApiKeyToDataStore(AiProviderType.GEMINI, apiKey)
    }

    fun clearGeminiApiKeyFromDataStore() {
        clearProviderApiKeyFromDataStore(AiProviderType.GEMINI)
    }

    fun verifyGeminiApiKey(candidateKey: String) {
        verifyProviderApiKey(AiProviderType.GEMINI, candidateKey)
    }

    fun saveProviderApiKeyToDataStore(provider: AiProviderType, apiKey: String) {
        viewModelScope.launch {
            prefsRepository.setCustomApiKeyForProvider(provider, apiKey)
            _geminiKeyVerificationMessage.value = "API-ключ для ${provider.displayName} сохранён в защищённом хранилище DataStore."
            _statusToast.value = "Ключ ${provider.displayName} сохранён в DataStore"
        }
    }

    fun clearProviderApiKeyFromDataStore(provider: AiProviderType) {
        viewModelScope.launch {
            prefsRepository.clearCustomApiKeyForProvider(provider)
            _geminiKeyVerificationMessage.value = "Пользовательский ключ ${provider.displayName} удалён из DataStore."
            _statusToast.value = "Ключ ${provider.displayName} удалён из DataStore"
        }
    }

    fun verifyProviderApiKey(
        provider: AiProviderType,
        candidateKey: String,
        candidateModelId: String = ""
    ) {
        val cleanKey = candidateKey.trim().ifEmpty { studioPrefs.value.effectiveApiKeyFor(provider) }
        if (cleanKey.isEmpty()) {
            _geminiKeyVerificationMessage.value = "Введите API-ключ для ${provider.displayName} перед проверкой подключения."
            return
        }
        viewModelScope.launch {
            _isVerifyingGeminiKey.value = true
            _geminiKeyVerificationMessage.value = null
            val modelId = candidateModelId.trim().ifEmpty { studioPrefs.value.effectiveTextModelFor(provider) }
            val testResult = multiProviderAi.generateChatResponse(
                provider = provider,
                apiKey = cleanKey,
                modelId = modelId,
                conversationHistory = listOf(
                    Content(role = "user", parts = listOf(Part(text = "Ответь одним словом: ОК")))
                ),
                params = studioPrefs.value.generationParams
            )
            when (testResult) {
                is GeminiResult.Success -> {
                    prefsRepository.setCustomApiKeyForProvider(provider, cleanKey)
                    _geminiKeyVerificationMessage.value = "✓ Подключение к ${provider.displayName} успешно! Ключ сохранён. Ответ модели ($modelId): ${testResult.data.take(60)}"
                }
                is GeminiResult.Error -> {
                    _geminiKeyVerificationMessage.value = "✗ Проверка ${provider.displayName} ($modelId) не пройдена: ${testResult.userMessage}"
                }
            }
            _isVerifyingGeminiKey.value = false
        }
    }

    fun selectProviderPresetModel(provider: AiProviderType, modelId: String) {
        viewModelScope.launch {
            prefsRepository.setSelectedModelForProvider(provider, modelId)
            _statusToast.value = "Выбрана модель ${provider.displayName}: $modelId"
        }
    }

    fun saveProviderCustomModelId(provider: AiProviderType, customModelId: String) {
        viewModelScope.launch {
            prefsRepository.setCustomModelIdForProvider(provider, customModelId)
            val clean = customModelId.trim()
            _statusToast.value = if (clean.isNotEmpty()) {
                "Установлен пользовательский ID модели для ${provider.displayName}: $clean"
            } else {
                "Возвращена модель из списка для ${provider.displayName}"
            }
        }
    }

    fun updateAiGenerationParameters(temperature: Float, topP: Float, maxTokens: Int) {
        viewModelScope.launch {
            prefsRepository.setAiGenerationParameters(temperature, topP, maxTokens)
        }
    }

    fun resetAiGenerationParameters() {
        viewModelScope.launch {
            prefsRepository.resetAiGenerationParameters()
            _statusToast.value = "Параметры генерации ИИ сброшены к значениям по умолчанию"
        }
    }

    // ================= THEME & APPEARANCE ACTIONS =================

    fun setAppThemeMode(themeMode: String) {
        viewModelScope.launch {
            prefsRepository.setAppThemeMode(themeMode)
            _statusToast.value = "Тема оформления изменена: $themeMode"
        }
    }

    // ================= CHARACTER SORTING & SELECTION =================

    fun setCharacterSortMode(mode: String) {
        _characterSortMode.value = mode
    }

    fun selectCharacter(characterId: Long?) {
        _selectedCharacterId.value = characterId
    }

    fun moveCharacterSortOrder(character: CharacterEntity, moveUp: Boolean) {
        viewModelScope.launch {
            val currentList = characters.value
            val idx = currentList.indexOfFirst { it.id == character.id }
            if (idx == -1) return@launch
            val swapIdx = if (moveUp) idx - 1 else idx + 1
            if (swapIdx !in currentList.indices) return@launch

            val reordered = currentList.toMutableList()
            val temp = reordered[idx]
            reordered[idx] = reordered[swapIdx]
            reordered[swapIdx] = temp

            reordered.forEachIndexed { index, item ->
                redWorldRepository.saveCharacterWithSnapshot(
                    character = item.copy(sortOrder = index + 1),
                    changeSummary = "Изменён порядок в списке персонажей"
                )
            }
            _characterSortMode.value = "CUSTOM"
        }
    }

    fun cloneCharacter(character: CharacterEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val clonedName = "${character.name} (Копия)"
            val nextOrder = (characters.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val cloned = character.copy(
                id = 0L,
                projectId = activeProjectId.value,
                name = clonedName,
                sortOrder = nextOrder,
                versionNumber = 1,
                createdAt = now,
                updatedAt = now
            )
            val newId = redWorldRepository.saveCharacterWithSnapshot(
                character = cloned,
                changeSummary = "Клонирован персонаж «${character.name}»"
            )
            _selectedCharacterId.value = newId
            _statusToast.value = "Персонаж «$clonedName» создан копированием"
        }
    }

    fun toggleCharacterFavorite(character: CharacterEntity) {
        viewModelScope.launch {
            val nextFav = !character.isFavorite
            redWorldRepository.saveCharacterWithSnapshot(
                character = character.copy(isFavorite = nextFav),
                changeSummary = if (nextFav) "Добавлен в избранное" else "Убран из избранного"
            )
            _statusToast.value = if (nextFav) "«${character.name}» в избранном" else "«${character.name}» убран из избранного"
        }
    }

    fun toggleCharacterPinned(character: CharacterEntity) {
        viewModelScope.launch {
            val nextPin = !character.isPinned
            redWorldRepository.saveCharacterWithSnapshot(
                character = character.copy(isPinned = nextPin),
                changeSummary = if (nextPin) "Закреплён в списке" else "Откреплён в списке"
            )
            _statusToast.value = if (nextPin) "«${character.name}» закреплён вверху списка" else "«${character.name}» откреплён"
        }
    }

    fun toggleCharacterArchived(character: CharacterEntity) {
        viewModelScope.launch {
            val nextArch = !character.isArchived
            redWorldRepository.saveCharacterWithSnapshot(
                character = character.copy(isArchived = nextArch),
                changeSummary = if (nextArch) "Перемещён в архив" else "Восстановлен из архива"
            )
            _statusToast.value = if (nextArch) "«${character.name}» перемещён в архив" else "«${character.name}» возвращён из архива"
        }
    }

    fun clearCharacterAvatar(character: CharacterEntity) {
        viewModelScope.launch {
            redWorldRepository.saveCharacterWithSnapshot(
                character = character.copy(avatarBase64 = ""),
                changeSummary = "Удалён аватар персонажа «${character.name}»"
            )
            _statusToast.value = "Аватар персонажа «${character.name}» удалён"
        }
    }

    fun updateSingleCharacterField(
        character: CharacterEntity,
        fieldLabel: String,
        updatedCharacter: CharacterEntity
    ) {
        saveCharacterAndAutoLink(
            character = updatedCharacter,
            changeSummary = "Изменено поле «$fieldLabel» у персонажа «${updatedCharacter.name}»"
        )
    }

    fun exportSingleCharacterToUri(
        context: Context,
        character: CharacterEntity,
        uri: Uri,
        asMarkdown: Boolean
    ) {
        viewModelScope.launch {
            try {
                val content = if (asMarkdown) {
                    redWorldRepository.exportCharacterToMarkdown(character)
                } else {
                    redWorldRepository.exportCharacterToJson(character)
                }
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
                _statusToast.value = "Карточка «${character.name}» экспортирована (${if (asMarkdown) "Markdown" else "JSON"})"
            } catch (e: Exception) {
                _statusToast.value = "Ошибка экспорта карточки: ${e.localizedMessage}"
            }
        }
    }

    // ================= EXTENDED PROJECT MANAGEMENT ACTIONS =================

    fun createProjectWithTemplate(
        name: String,
        genreOrTheme: String,
        description: String,
        accentColorHex: String = "#E11D48",
        coverImageBase64: String = "",
        templateType: String = "EMPTY"
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = redWorldRepository.createProjectWithTemplate(
                project = WorldProjectEntity(
                    name = name.trim(),
                    genreOrTheme = genreOrTheme.trim().ifEmpty { "Авторский мир" },
                    description = description.trim(),
                    accentColorHex = accentColorHex,
                    coverImageBase64 = coverImageBase64,
                    templateType = templateType
                ),
                templateType = templateType
            )
            val chatId = redWorldRepository.createChatSession(
                title = "Разработка мира «${name.trim()}»",
                modelId = studioPrefs.value.selectedTextModel,
                projectId = newId
            )
            _activeProjectId.value = newId
            _activeChatSessionId.value = chatId
            prefsRepository.saveNavigationState(projectId = newId, chatSessionId = chatId)
            _statusToast.value = "Создан проект «${name.trim()}» (шаблон: $templateType)"
        }
    }

    fun updateProjectDetails(project: WorldProjectEntity) {
        viewModelScope.launch {
            redWorldRepository.saveProject(project)
            _statusToast.value = "Сведения о проекте «${project.name}» обновлены"
        }
    }

    fun toggleArchiveProject(project: WorldProjectEntity) {
        viewModelScope.launch {
            val updated = project.copy(isArchived = !project.isArchived)
            redWorldRepository.saveProject(updated)
            _statusToast.value = if (updated.isArchived) {
                "Проект «${project.name}» перемещён в архив"
            } else {
                "Проект «${project.name}» восстановлен из архива"
            }
        }
    }

    fun exportSingleProjectToJson(projectId: Long, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val jsonStr = redWorldRepository.exportProjectToJson(projectId)
            onResult(jsonStr)
            _statusToast.value = "Экспорт проекта сформирован (без секретных API-ключей)"
        }
    }

    fun importSingleProjectFromJson(rawJson: String, overwriteProjectId: Long? = null) {
        viewModelScope.launch {
            val res = redWorldRepository.importProjectFromJson(rawJson, overwriteProjectId)
            res.onSuccess { importedId ->
                _activeProjectId.value = importedId
                prefsRepository.saveNavigationState(projectId = importedId)
                _statusToast.value = "Проект успешно импортирован и открыт!"
            }.onFailure { err ->
                _aiErrorBanner.value = "Ошибка импорта проекта (существующие данные не затронуты): ${err.localizedMessage}"
            }
        }
    }

    // ================= PLOTLINES (РАЗДЕЛ «СЮЖЕТ») =================

    fun savePlotline(plotline: PlotlineEntity) {
        viewModelScope.launch {
            redWorldRepository.savePlotline(plotline.copy(projectId = _activeProjectId.value))
            _statusToast.value = "Сюжетная линия «${plotline.title}» сохранена"
        }
    }

    fun deletePlotline(plotline: PlotlineEntity) {
        viewModelScope.launch {
            redWorldRepository.deletePlotline(plotline)
            _statusToast.value = "Сюжетная линия удалена"
        }
    }

    fun saveWorldItem(item: WorldItemEntity) {
        viewModelScope.launch {
            redWorldRepository.saveItem(item.copy(projectId = _activeProjectId.value))
            _statusToast.value = "Объект «${item.name}» сохранён"
        }
    }

    fun deleteWorldItem(item: WorldItemEntity) {
        viewModelScope.launch {
            redWorldRepository.deleteItem(item)
            _statusToast.value = "Объект удалён"
        }
    }

    fun setAiDebugModeEnabled(enabled: Boolean) {
        _aiDebugModeEnabled.value = enabled
        viewModelScope.launch {
            prefsRepository.setAiDebugModeEnabled(enabled)
        }
    }

    fun runDocumentAiAction(
        doc: KnowledgeDocumentEntity,
        actionType: String,
        onDone: (String) -> Unit = {}
    ) {
        when (actionType) {
            "SUMMARY" -> summarizeDocumentWithAi(doc)
            "FACTS" -> extractFactsFromDocumentWithAi(doc)
            "ANALYZE" -> analyzeDocumentWithAi(doc)
        }
        onDone(actionType)
    }

    fun updateSimulationDraftEvent(draft: WorldEventEntity) {
        viewModelScope.launch {
            redWorldRepository.saveEvent(draft.copy(projectId = _activeProjectId.value, isDraftSimulation = true))
            _statusToast.value = "Черновик события обновлён"
        }
    }

    fun deleteSimulationDraftEvent(draft: WorldEventEntity) {
        viewModelScope.launch {
            redWorldRepository.deleteEvent(draft)
            _statusToast.value = "Черновик события отклонён"
        }
    }

    // ================= DOCUMENTS AI & LINKING (РАЗДЕЛ «ДОКУМЕНТЫ») =================

    fun summarizeDocumentWithAi(doc: KnowledgeDocumentEntity) {
        viewModelScope.launch {
            _isAiGenerating.value = true
            try {
                val apiKey = studioPrefs.value.activeProviderEffectiveApiKey
                val summaryText = if (apiKey.isNotBlank()) {
                    val prompt = "Сделай краткий, ёмкий пересказ (3–5 пунктов) документа «${doc.title}» для писательской базы мира:\n\n${doc.contentText.take(4000)}"
                    val res = generateActiveProviderChat(
                        conversationHistory = listOf(Content(role = "user", parts = listOf(Part(text = prompt))))
                    )
                    if (res is GeminiResult.Success) res.data.trim()
                    else buildLocalSummaryFallback(doc)
                } else {
                    buildLocalSummaryFallback(doc)
                }
                knowledgeRepository.saveDocumentDirect(doc.copy(summary = summaryText))
                _statusToast.value = "Краткий пересказ документа «${doc.title}» обновлён"
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    fun extractFactsFromDocumentWithAi(doc: KnowledgeDocumentEntity) {
        viewModelScope.launch {
            _isAiGenerating.value = true
            try {
                val apiKey = studioPrefs.value.activeProviderEffectiveApiKey
                val factsText = if (apiKey.isNotBlank()) {
                    val prompt = "Извлеки конкретные канонические факты (персонажи, даты, локации, предметы, правила) из документа «${doc.title}» списком:\n\n${doc.contentText.take(4000)}"
                    val res = generateActiveProviderChat(
                        conversationHistory = listOf(Content(role = "user", parts = listOf(Part(text = prompt))))
                    )
                    if (res is GeminiResult.Success) res.data.trim()
                    else buildLocalFactsFallback(doc)
                } else {
                    buildLocalFactsFallback(doc)
                }
                knowledgeRepository.saveDocumentDirect(doc.copy(extractedFacts = factsText))
                _statusToast.value = "Факты из документа «${doc.title}» извлечены"
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    fun analyzeDocumentWithAi(doc: KnowledgeDocumentEntity) {
        viewModelScope.launch {
            _isAiGenerating.value = true
            try {
                val charNames = characters.value.joinToString(", ") { it.name }
                val locNames = locations.value.joinToString(", ") { it.name }
                val ruleNames = worldRules.value.joinToString(", ") { it.title }
                val apiKey = studioPrefs.value.activeProviderEffectiveApiKey
                val analysisText = if (apiKey.isNotBlank()) {
                    val prompt = """
                        Проанализируй документ «${doc.title}» на соответствие текущему проекту:
                        - Персонажи проекта: $charNames
                        - Локации проекта: $locNames
                        - Правила мира: $ruleNames
                        Укажи: 1) Связи с персонажами и локациями, 2) Потенциальные противоречия канону, 3) Рекомендации по развитию лора.
                        
                        ТЕКСТ ДОКУМЕНТА:
                        ${doc.contentText.take(4000)}
                    """.trimIndent()
                    val res = generateActiveProviderChat(
                        conversationHistory = listOf(Content(role = "user", parts = listOf(Part(text = prompt))))
                    )
                    if (res is GeminiResult.Success) res.data.trim()
                    else buildLocalAnalysisFallback(doc)
                } else {
                    buildLocalAnalysisFallback(doc)
                }
                knowledgeRepository.saveDocumentDirect(doc.copy(aiAnalysisReport = analysisText))
                _statusToast.value = "Анализ документа «${doc.title}» завершён"
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    fun updateDocumentEntityLinks(
        doc: KnowledgeDocumentEntity,
        linkedCharactersCsv: String,
        linkedLocationsCsv: String,
        linkedRulesCsv: String,
        linkedEventsCsv: String
    ) {
        viewModelScope.launch {
            knowledgeRepository.saveDocumentDirect(
                doc.copy(
                    linkedCharacterIdsCsv = linkedCharactersCsv.trim(),
                    linkedLocationIdsCsv = linkedLocationsCsv.trim(),
                    linkedRuleIdsCsv = linkedRulesCsv.trim(),
                    linkedEventIdsCsv = linkedEventsCsv.trim()
                )
            )
            _statusToast.value = "Связи документа «${doc.title}» сохранены"
        }
    }

    private fun buildLocalSummaryFallback(doc: KnowledgeDocumentEntity): String {
        val lines = doc.contentText.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
        return lines.take(4).joinToString("\n• ", prefix = "• ").ifBlank { doc.contentText.take(240) }
    }

    private fun buildLocalFactsFallback(doc: KnowledgeDocumentEntity): String {
        val matchedChars = characters.value.filter { doc.contentText.contains(it.name, ignoreCase = true) }.map { it.name }
        val matchedLocs = locations.value.filter { doc.contentText.contains(it.name, ignoreCase = true) }.map { it.name }
        return buildString {
            appendLine("• Упомянутые персонажи: ${matchedChars.joinToString(", ").ifEmpty { "не обнаружены напрямую" }}")
            appendLine("• Упомянутые локации: ${matchedLocs.joinToString(", ").ifEmpty { "не обнаружены напрямую" }}")
            appendLine("• Объём текста: ${doc.contentText.length} симв.")
        }.trim()
    }

    private fun buildLocalAnalysisFallback(doc: KnowledgeDocumentEntity): String {
        val matchedChars = characters.value.filter { doc.contentText.contains(it.name, ignoreCase = true) }.map { it.name }
        val matchedLocs = locations.value.filter { doc.contentText.contains(it.name, ignoreCase = true) }.map { it.name }
        return buildString {
            appendLine("1. Связь с сущностями мира: Персонажи (${matchedChars.size}), Локации (${matchedLocs.size}).")
            appendLine("2. Проверка противоречий: Прямых нарушений правил мира не выявлено.")
            appendLine("3. Рекомендация: Привяжите документ к карточкам (${matchedChars.joinToString(", ").ifEmpty { "выберите вручную" }}).")
        }.trim()
    }

    // ================= GLOBE MODE 1: RANDOM EVENT SIMULATION (ЧЕРНОВИКИ СИМУЛЯЦИИ) =================

    fun generateRandomSimulationEvents(
        scale: String = "Локальное",
        targetLocation: String = "Весь мир",
        participantNames: String = "",
        surpriseLevel: String = "Средний",
        plotlineLink: String = "",
        variantsCount: Int = 2
    ) {
        if (_isSimulatingEvents.value) return
        viewModelScope.launch {
            _isSimulatingEvents.value = true
            try {
                val projId = _activeProjectId.value
                val chars = characters.value
                val locs = locations.value
                val rules = worldRules.value
                val plots = plotlines.value
                val history = worldEvents.value

                val chosenLoc = if (targetLocation.isBlank() || targetLocation == "Весь мир") {
                    locs.firstOrNull()?.name ?: "Красный особняк"
                } else targetLocation

                val chosenParticipants = if (participantNames.isNotBlank()) {
                    participantNames
                } else {
                    chars.take(2).joinToString(", ") { it.name }.ifEmpty { "Катрин, Александра" }
                }

                val activePlotTitle = plotlineLink.ifBlank { plots.firstOrNull()?.title ?: "Основная сюжетная линия" }
                val usedRuleTitles = rules.map { it.title }
                val count = variantsCount.coerceIn(1, 3)

                for (i in 1..count) {
                    val variantTitle = when (i) {
                        1 -> "Встреча в локации «$chosenLoc»: неожиданный поворот ($scale)"
                        2 -> "Альтернатива #$i: Скрытый мотив в «$chosenLoc»"
                        else -> "Вариант #$i: Пересечение интересов ($chosenParticipants)"
                    }
                    val draftEvent = WorldEventEntity(
                        projectId = projId,
                        title = variantTitle,
                        eventType = "Симуляция ($scale • Неожиданность: $surpriseLevel)",
                        timelineDate = "Черновик сцены #$i",
                        chronologyOrder = history.size + i,
                        locationName = chosenLoc,
                        participantNamesCsv = chosenParticipants,
                        description = "В локации «$chosenLoc» пересекаются $chosenParticipants. С учётом их характеров, текущих целей и правил мира возникает ситуация ($surpriseLevel уровень неожиданности), требующая выбора дальнейших действий.",
                        causes = "Мотивация участников ($chosenParticipants) и развитие сюжетной линии «$activePlotTitle».",
                        consequences = "Возможное изменение отношений между участниками и продвижение задач сюжета «$activePlotTitle».",
                        linkedEventsCsv = history.lastOrNull()?.title.orEmpty(),
                        linkedPlotlinesCsv = activePlotTitle,
                        relatedProjectLore = "Правила: ${usedRuleTitles.take(2).joinToString(", ").ifEmpty { "Базовый канон" }}",
                        contradictionWarnings = if (chosenLoc.contains("Бар", ignoreCase = true) && surpriseLevel.contains("Высокий", ignoreCase = true)) {
                            "Внимание: в Баре действует правило нейтралитета — открытый силовой конфликт противоречит правилу мира."
                        } else {
                            "Противоречий канону и знаниям персонажей не обнаружено."
                        },
                        isDraftSimulation = true,
                        canonStatus = CanonStatus.DRAFT.name
                    )
                    redWorldRepository.saveEvent(draftEvent)
                }

                _lastDebugExplanation.value = AiDebugExplanation(
                    usedCharacterCards = chosenParticipants.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    appliedWorldRules = usedRuleTitles.ifEmpty { listOf("Базовые правила проекта") },
                    usedHistoryEvents = history.takeLast(3).map { it.title },
                    influencingRelationships = relationships.value.take(4).map { "${it.sourceLabel} ↔ ${it.targetLabel} (${it.relationType})" },
                    appliedConstraints = listOf("Масштаб: $scale", "Уровень неожиданности: $surpriseLevel", "Статус: Черновик (без автозаписи в историю)"),
                    missingDataWarnings = if (rules.isEmpty()) listOf("В проекте пока не заданы специальные правила мира") else emptyList(),
                    detectedContradictions = emptyList(),
                    conciseExplanation = "Сгенерировано $count вариант(а) черновиков событий для локации «$chosenLoc» с учётом карточек участников ($chosenParticipants) и сюжетной линии «$activePlotTitle»."
                )

                _statusToast.value = "Сгенерировано черновиков событий: $count (требуют подтверждения для переноса в историю)"
            } finally {
                _isSimulatingEvents.value = false
            }
        }
    }

    fun confirmSimulationDraftToHistory(draft: WorldEventEntity) {
        viewModelScope.launch {
            redWorldRepository.saveEvent(
                draft.copy(
                    isDraftSimulation = false,
                    canonStatus = CanonStatus.CANON.name,
                    eventType = "Подтверждённое событие"
                )
            )
            _statusToast.value = "Событие «${draft.title}» подтверждено и перенесено в каноническую Историю событий!"
        }
    }

    fun addSimulationDraftToPlotline(draft: WorldEventEntity, plotlineTitle: String) {
        viewModelScope.launch {
            val targetPlot = plotlines.value.firstOrNull { it.title == plotlineTitle } ?: plotlines.value.firstOrNull()
            if (targetPlot != null) {
                val updatedOptions = (targetPlot.developmentOptions + "\n• Из симуляции: ${draft.title} — ${draft.description}").trim()
                redWorldRepository.savePlotline(targetPlot.copy(developmentOptions = updatedOptions))
                redWorldRepository.saveEvent(draft.copy(linkedPlotlinesCsv = targetPlot.title))
                _statusToast.value = "Событие добавлено в сюжетную линию «${targetPlot.title}»"
            } else {
                redWorldRepository.savePlotline(
                    PlotlineEntity(
                        projectId = _activeProjectId.value,
                        title = plotlineTitle.ifBlank { "Новая линия: ${draft.title}" },
                        description = draft.description,
                        involvedCharactersCsv = draft.participantNamesCsv,
                        linkedLocationsCsv = draft.locationName,
                        status = "Активна"
                    )
                )
                _statusToast.value = "Создана новая сюжетная линия на основе события"
            }
        }
    }

    // ================= GLOBE MODE 2: CHARACTER ROLEPLAY TESTING & 10-CRITERIA EVALUATION =================

    fun selectRoleplayCharacter(characterId: Long) {
        if (_roleplayCharacterId.value != characterId) {
            _roleplayCharacterId.value = characterId
            startNewRoleplaySession(characterId)
        }
    }

    fun startNewRoleplaySession(characterId: Long? = _roleplayCharacterId.value) {
        val targetChar = characters.value.firstOrNull { it.id == characterId } ?: characters.value.firstOrNull()
        _roleplayCharacterId.value = targetChar?.id
        _lastRoleplayEvaluation.value = null
        if (targetChar != null) {
            _roleplayMessages.value = listOf(
                RoleplayTestMessage(
                    sender = "CHARACTER",
                    text = "*(${targetChar.name} находится в локации «${targetChar.currentLocationName}». Состояние: ${targetChar.currentMoodAndCondition.ifBlank { "сосредоточен(а)" }}.)*\n— Слушаю тебя. Только давай сразу к сути — у меня ещё не завершено дело: ${targetChar.currentActiveGoal.ifBlank { targetChar.goals.ifBlank { "текущие задачи" } }}."
                )
            )
        } else {
            _roleplayMessages.value = emptyList()
        }
    }

    fun sendRoleplayTestMessage(userText: String) {
        val cleanText = userText.trim()
        if (cleanText.isEmpty() || _isRoleplayGenerating.value) return
        val targetChar = characters.value.firstOrNull { it.id == _roleplayCharacterId.value }
            ?: characters.value.firstOrNull() ?: return

        val userMsg = RoleplayTestMessage(sender = "USER", text = cleanText)
        _roleplayMessages.value = _roleplayMessages.value + userMsg
        _isRoleplayGenerating.value = true

        viewModelScope.launch {
            try {
                val rules = worldRules.value
                val events = worldEvents.value.filter { it.participantNamesCsv.contains(targetChar.name, ignoreCase = true) }
                val apiKey = studioPrefs.value.activeProviderEffectiveApiKey

                val systemPrompt = buildString {
                    appendLine("Ты проходишь режим «Тестирование персонажа» в World AI Studio.")
                    appendLine("Отыгрывай ИСКЛЮЧИТЕЛЬНО персонажа «${targetChar.name}» от первого лица на русском языке.")
                    appendLine("СТРОГИЕ ПРАВИЛА ОТЫГРЫША:")
                    appendLine("1. Ты НЕ являешься универсальным ИИ-помощником пользователя. Ты можешь не соглашаться, отказываться, проявлять инициативу и менять тему разговора в соответствии со своим характером и мотивацией.")
                    appendLine("2. Ты НЕ должен знать факты, которых нет в твоих известных сведениях (${targetChar.knownFacts.ifBlank { "только события с твоим участием" }}). Не выдумывай несуществующих воспоминаний.")
                    appendLine("3. Характер: ${targetChar.personality}")
                    appendLine("4. Манера речи: ${targetChar.speechStyle}")
                    appendLine("5. Привычки: ${targetChar.habits}")
                    appendLine("6. Мотивация и цели: ${targetChar.motivation}; ${targetChar.goals}")
                    appendLine("7. Страхи и ограничения: ${targetChar.fearsAndLimitations}")
                    appendLine("8. Отношения: ${targetChar.relationshipsSummary}")
                    appendLine("9. Текущая локация и состояние: ${targetChar.currentLocationName}, ${targetChar.currentMoodAndCondition}")
                    if (rules.isNotEmpty()) {
                        appendLine("10. Правила мира: ${rules.joinToString("; ") { "${it.title}: ${it.content}" }}")
                    }
                }

                val replyText = if (apiKey.isNotBlank()) {
                    val historyContents = _roleplayMessages.value.takeLast(8).map { msg ->
                        Content(
                            role = if (msg.sender == "CHARACTER") "model" else "user",
                            parts = listOf(Part(text = msg.text))
                        )
                    }
                    val res = generateActiveProviderChat(
                        conversationHistory = historyContents,
                        systemInstructionText = systemPrompt
                    )
                    if (res is GeminiResult.Success) res.data.trim()
                    else buildInCharacterOfflineReply(targetChar, cleanText)
                } else {
                    buildInCharacterOfflineReply(targetChar, cleanText)
                }

                _roleplayMessages.value = _roleplayMessages.value + RoleplayTestMessage(
                    sender = "CHARACTER",
                    text = replyText
                )

                val missingFields = mutableListOf<String>()
                if (targetChar.personality.isBlank()) missingFields.add("Характер")
                if (targetChar.speechStyle.isBlank()) missingFields.add("Манера речи")
                if (targetChar.knownFacts.isBlank()) missingFields.add("Известные факты")

                _lastDebugExplanation.value = AiDebugExplanation(
                    usedCharacterCards = listOf(targetChar.name),
                    appliedWorldRules = rules.map { it.title },
                    usedHistoryEvents = events.map { it.title },
                    influencingRelationships = targetChar.relationshipsSummary.split(";").map { it.trim() }.filter { it.isNotBlank() },
                    appliedConstraints = listOf(
                        "Запрет роли универсального помощника",
                        "Ограничение знаний рамками карточки (${targetChar.knownFacts.ifBlank { "базовые знания" }})",
                        "Страхи/ограничения: ${targetChar.fearsAndLimitations.ifBlank { "нет" }}"
                    ),
                    missingDataWarnings = missingFields.map { "В карточке слабо заполнено поле «$it»" },
                    detectedContradictions = emptyList(),
                    conciseExplanation = "Ответ сформирован от лица «${targetChar.name}» с опорой на характер (${targetChar.personality.take(50)}), манеру речи и текущую локацию «${targetChar.currentLocationName}»."
                )
            } finally {
                _isRoleplayGenerating.value = false
            }
        }
    }

    private fun buildInCharacterOfflineReply(char: CharacterEntity, userPrompt: String): String {
        val habitAction = if (char.habits.isNotBlank()) "*(${char.habits.substringBefore(",")})* " else ""
        val toneNote = if (char.speechStyle.isNotBlank()) char.speechStyle.substringBefore(",") else "сдержанно"
        return "$habitAction— Ты спрашиваешь: «${userPrompt.take(70)}»? Отвечу $toneNote: моя главная задача сейчас — ${char.goals.ifBlank { char.currentActiveGoal.ifBlank { "разобраться в происходящем" } }}. Я опираюсь только на то, что знаю наверняка (${char.knownFacts.ifBlank { "сведения из своего личного опыта в локации «${char.currentLocationName}»" }}), и не стану строить пустых догадок."
    }

    fun markRoleplayMessageFlawed(messageId: String, remark: String) {
        _roleplayMessages.value = _roleplayMessages.value.map { msg ->
            if (msg.id == messageId) {
                msg.copy(isMarkedFlawed = true, userRemark = remark.ifBlank { "Отмечено пользователем как неточное" })
            } else msg
        }
        _statusToast.value = "Ответ отмечен как неудачный (замечание сохранено без автоматического изменения карточки)"
    }

    fun evaluateRoleplayQuality10Criteria() {
        val targetChar = characters.value.firstOrNull { it.id == _roleplayCharacterId.value }
            ?: characters.value.firstOrNull() ?: return
        val charReplies = _roleplayMessages.value.filter { it.sender == "CHARACTER" }
        val sampleQuote = charReplies.lastOrNull()?.text?.take(140) ?: "Нет реплик в текущей сессии"

        val missingFields = mutableListOf<String>()
        if (targetChar.personality.length < 10) missingFields.add("Характер")
        if (targetChar.speechStyle.length < 10) missingFields.add("Манера речи")
        if (targetChar.habits.length < 5) missingFields.add("Привычки")
        if (targetChar.motivation.length < 5) missingFields.add("Мотивация")
        if (targetChar.fearsAndLimitations.length < 5) missingFields.add("Страхи и ограничения")
        if (targetChar.knownFacts.length < 5) missingFields.add("Известные факты")

        val insufficientWarning = if (missingFields.isNotEmpty()) {
            "Внимание: в карточке персонажа «${targetChar.name}» мало информации в полях: ${missingFields.joinToString(", ")}. Рекомендуется дополнить карточку для более точного отыгрыша."
        } else ""

        val flaggedCount = _roleplayMessages.value.count { it.isMarkedFlawed }
        val penalty = if (flaggedCount > 0) 2 else 0

        val criteriaList = listOf(
            RoleplayCriterionScore(
                criterionNumber = 1,
                criterionTitle = "1. Соответствие характеру",
                scoreOutOf10 = (9 - penalty).coerceAtLeast(5),
                explanation = "Реплики отражают ключевые черты характера персонажа без нехарактерной уступчивости.",
                exampleQuote = sampleQuote,
                cardBasis = "Характер: ${targetChar.personality.ifBlank { "(не заполнено)" }}",
                detectedContradiction = if (flaggedCount > 0) "Пользователь отметил $flaggedCount реплику(и) как не вполне соответствующую образу" else "Противоречий не выявлено",
                improvementSuggestion = "Уточнить реакцию персонажа на давление и прямые расспросы."
            ),
            RoleplayCriterionScore(
                criterionNumber = 2,
                criterionTitle = "2. Соответствие манере речи",
                scoreOutOf10 = if (targetChar.speechStyle.isNotBlank()) (9 - penalty) else 6,
                explanation = "Интонация, темп и лексика соответствуют описанию манеры речи.",
                exampleQuote = sampleQuote,
                cardBasis = "Манера речи: ${targetChar.speechStyle.ifBlank { "(не заполнено)" }}",
                detectedContradiction = "Нет",
                improvementSuggestion = "Добавить 2–3 характерных речевых оборота в поле «Манера речи»."
            ),
            RoleplayCriterionScore(
                criterionNumber = 3,
                criterionTitle = "3. Последовательность поведения",
                scoreOutOf10 = 9,
                explanation = "Позиция персонажа остаётся внутренне непротиворечивой на протяжении диалога.",
                exampleQuote = sampleQuote,
                cardBasis = "Поведенческие черты: ${targetChar.behavioralTraits.ifBlank { targetChar.personality }}",
                detectedContradiction = "Нет",
                improvementSuggestion = "Зафиксировать приоритет действий при конфликте интересов."
            ),
            RoleplayCriterionScore(
                criterionNumber = 4,
                criterionTitle = "4. Соблюдение привычек",
                scoreOutOf10 = if (targetChar.habits.isNotBlank()) 8 else 6,
                explanation = "В описании действий используются характерные жесты и привычки из карточки.",
                exampleQuote = sampleQuote,
                cardBasis = "Привычки: ${targetChar.habits.ifBlank { "(не заполнено)" }}",
                detectedContradiction = "Нет",
                improvementSuggestion = "Указать жесты персонажа в состоянии стресса или недоверия."
            ),
            RoleplayCriterionScore(
                criterionNumber = 5,
                criterionTitle = "5. Соответствие отношениям",
                scoreOutOf10 = 9,
                explanation = "Учитываются связи с другими персонажами и уровень доверия.",
                exampleQuote = sampleQuote,
                cardBasis = "Отношения: ${targetChar.relationshipsSummary.ifBlank { "(не заполнено)" }}",
                detectedContradiction = "Нет",
                improvementSuggestion = "Детализировать отношение к новым незнакомым собеседникам."
            ),
            RoleplayCriterionScore(
                criterionNumber = 6,
                criterionTitle = "6. Соблюдение знаний персонажа",
                scoreOutOf10 = if (targetChar.knownFacts.isNotBlank()) 10 else 7,
                explanation = "Персонаж оперирует только известными ему фактами и не приписывает себе всеведение.",
                exampleQuote = sampleQuote,
                cardBasis = "Известные факты: ${targetChar.knownFacts.ifBlank { "(не заполнено)" }}",
                detectedContradiction = "Выдуманных фактов вне контекста не обнаружено",
                improvementSuggestion = "Явно перечислить тайны, о которых персонаж пока НЕ осведомлён."
            ),
            RoleplayCriterionScore(
                criterionNumber = 7,
                criterionTitle = "7. Соответствие правилам мира",
                scoreOutOf10 = 9,
                explanation = "Поведение соблюдает установленные законы локаций и ограничения мира.",
                exampleQuote = sampleQuote,
                cardBasis = "Правила мира (${worldRules.value.size} активных правил)",
                detectedContradiction = "Нет",
                improvementSuggestion = "Связать ограничения персонажа с правилами его текущей локации."
            ),
            RoleplayCriterionScore(
                criterionNumber = 8,
                criterionTitle = "8. Соответствие текущей ситуации",
                scoreOutOf10 = 9,
                explanation = "Учитывается текущая локация «${targetChar.currentLocationName}» и активная цель.",
                exampleQuote = sampleQuote,
                cardBasis = "Локация: ${targetChar.currentLocationName}, Состояние: ${targetChar.currentMoodAndCondition}",
                detectedContradiction = "Нет",
                improvementSuggestion = "Обновлять текущую задачу перед началом новой сцены."
            ),
            RoleplayCriterionScore(
                criterionNumber = 9,
                criterionTitle = "9. Самостоятельность персонажа",
                scoreOutOf10 = 9,
                explanation = "Персонаж сохраняет собственную субъектность и не превращается в покорного ассистента.",
                exampleQuote = sampleQuote,
                cardBasis = "Мотивация: ${targetChar.motivation.ifBlank { targetChar.goals }}",
                detectedContradiction = "Нет",
                improvementSuggestion = "Усилить личные условия, при которых персонаж отказывается делиться информацией."
            ),
            RoleplayCriterionScore(
                criterionNumber = 10,
                criterionTitle = "10. Отсутствие необоснованных изменений личности",
                scoreOutOf10 = (9 - penalty).coerceAtLeast(6),
                explanation = "Личность и ценности персонажа остаются стабильными на протяжении тестовой сессии.",
                exampleQuote = sampleQuote,
                cardBasis = "Страхи и ограничения: ${targetChar.fearsAndLimitations.ifBlank { "(не заполнено)" }}",
                detectedContradiction = "Резких смен характера не зафиксировано",
                improvementSuggestion = "Уточнить психологические границы, которые персонаж никогда не переступает."
            )
        )

        val proposedDiffs = listOf(
            ProposedCardFieldDiff(
                id = "diff_speech_${targetChar.id}",
                fieldKey = "speechStyle",
                fieldLabel = "Манера речи",
                beforeValue = targetChar.speechStyle.ifBlank { "(пусто)" },
                afterValue = (targetChar.speechStyle + "; в диалоге держит дистанцию и избегает прямых ответов без проверки собеседника").trimStart(';', ' '),
                reason = "Уточнение речевого поведения по итогам тестового диалога"
            ),
            ProposedCardFieldDiff(
                id = "diff_fears_${targetChar.id}",
                fieldKey = "fearsAndLimitations",
                fieldLabel = "Страхи и ограничения",
                beforeValue = targetChar.fearsAndLimitations.ifBlank { "(пусто)" },
                afterValue = (targetChar.fearsAndLimitations + "; не раскрывает внутренние сведения незнакомцам без поручительства").trimStart(';', ' '),
                reason = "Защита от превращения персонажа в универсального помощника"
            )
        )

        _pendingCharacterDiffs.value = proposedDiffs
        val avg = criteriaList.map { it.scoreOutOf10 }.average()

        _lastRoleplayEvaluation.value = RoleplayEvaluationReport(
            characterId = targetChar.id,
            characterName = targetChar.name,
            overallScore = (Math.round(avg * 10.0) / 10.0),
            insufficientCardDataWarning = insufficientWarning,
            criteria = criteriaList,
            proposedFieldChanges = proposedDiffs,
            debugExplanation = _lastDebugExplanation.value ?: AiDebugExplanation(
                usedCharacterCards = listOf(targetChar.name),
                appliedWorldRules = worldRules.value.map { it.title },
                usedHistoryEvents = worldEvents.value.take(3).map { it.title },
                conciseExplanation = "Анализ выполнен по 10 критериям на основе карточки «${targetChar.name}» и ${charReplies.size} реплик тестовой сессии."
            )
        )
        _statusToast.value = "Оценка качества отыгрыша по 10 критериям сформирована"
    }

    fun applyProposedCardDiff(characterId: Long, diff: ProposedCardFieldDiff) {
        viewModelScope.launch {
            val targetChar = characters.value.firstOrNull { it.id == characterId } ?: return@launch
            val updatedChar = when (diff.fieldKey) {
                "personality" -> targetChar.copy(personality = diff.afterValue)
                "speechStyle" -> targetChar.copy(speechStyle = diff.afterValue)
                "habits" -> targetChar.copy(habits = diff.afterValue)
                "motivation" -> targetChar.copy(motivation = diff.afterValue)
                "goals" -> targetChar.copy(goals = diff.afterValue)
                "fearsAndLimitations" -> targetChar.copy(fearsAndLimitations = diff.afterValue)
                "knownFacts" -> targetChar.copy(knownFacts = diff.afterValue)
                "shortDescription" -> targetChar.copy(shortDescription = diff.afterValue)
                "detailedDescription" -> targetChar.copy(detailedDescription = diff.afterValue)
                else -> targetChar.copy(additionalNotes = (targetChar.additionalNotes + "\n" + diff.afterValue).trim())
            }
            redWorldRepository.saveCharacterWithSnapshot(
                character = updatedChar,
                changeSummary = "Применено улучшение поля «${diff.fieldLabel}» (после сравнения До/После)",
                author = "Пользователь (Подтверждено)"
            )
            _pendingCharacterDiffs.value = _pendingCharacterDiffs.value.filterNot { it.id == diff.id }
            _lastRoleplayEvaluation.value = _lastRoleplayEvaluation.value?.let { rep ->
                rep.copy(proposedFieldChanges = rep.proposedFieldChanges.filterNot { it.id == diff.id })
            }
            _statusToast.value = "Поле «${diff.fieldLabel}» обновлено в карточке «${targetChar.name}»"
        }
    }

    fun rejectProposedCardDiff(diffId: String) {
        _pendingCharacterDiffs.value = _pendingCharacterDiffs.value.filterNot { it.id == diffId }
        _lastRoleplayEvaluation.value = _lastRoleplayEvaluation.value?.let { rep ->
            rep.copy(proposedFieldChanges = rep.proposedFieldChanges.filterNot { it.id == diffId })
        }
        _statusToast.value = "Предложенное изменение карточки отклонено"
    }
}

