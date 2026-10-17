package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CanonStatus
import com.example.data.local.CellDiffItem
import com.example.data.local.CharacterEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.DocumentAndXlsxEngine
import com.example.data.local.DocumentFormat
import com.example.data.local.GeneratedImageEntity
import com.example.data.local.KnowledgeDocumentEntity
import com.example.data.local.KnowledgeDocumentWithDetails
import com.example.data.local.KnowledgeNoteEntity
import com.example.data.local.KnowledgeTagEntity
import com.example.data.local.LocationEntity
import com.example.data.local.RedWorldDatabase
import com.example.data.local.RelationshipEntity
import com.example.data.local.VersionSnapshotEntity
import com.example.data.local.WorldEventEntity
import com.example.data.local.WorldItemEntity
import com.example.data.local.WorldRuleEntity
import com.example.data.local.XlsxWorkbookModel
import com.example.data.preferences.StudioPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.Content
import com.example.data.remote.DriveOperationResult
import com.example.data.remote.DriveRemoteFileMetadata
import com.example.data.remote.GeminiRemoteDataSource
import com.example.data.remote.GeminiResult
import com.example.data.remote.GoogleDriveSyncManager
import com.example.data.remote.InlineData
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

enum class MainBottomTab(val route: String, val titleRu: String) {
    HOME("HOME", "Главная"),
    GEMINI("GEMINI", "Gemini"),
    NOTEBOOK_LM("NOTEBOOK_LM", "NotebookLM"),
    GENERATION("GENERATION", "Генерация"),
    REDWORLD("REDWORLD", "RedWorld")
}

enum class SecondaryWorkspaceScreen(val titleRu: String) {
    NONE(""),
    KNOWLEDGE_RAG("RedWorld Knowledge (Документы и RAG)"),
    RELATIONSHIP_MAP("Интерактивная карта связей"),
    AI_ASSISTANT("AI-Помощник и Куратор RedWorld"),
    XLSX_EDITOR("Редактор таблиц XLSX"),
    FILES_AND_DRIVE("Файлы и Google Drive"),
    CONNECTIONS_AND_AUTH("Подключения, OAuth и API-ключи"),
    VERSIONS_AND_BACKUPS("История версий и Бэкапы"),
    DOCUMENTATION_GUIDE("Справка, Архитектура и APK")
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RedWorldViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RedWorldDatabase.getInstance(application)
    val knowledgeRepository = KnowledgeRepository(db.knowledgeDao())
    val redWorldRepository = RedWorldRepository(db.redWorldDao())
    val prefsRepository = UserPreferencesRepository(application)
    private val geminiRemote = GeminiRemoteDataSource()
    private val driveSyncManager = GoogleDriveSyncManager(application)

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    // --- Navigation & Active Workspace State ---
    private val _currentBottomTab = MutableStateFlow(MainBottomTab.HOME)
    val currentBottomTab: StateFlow<MainBottomTab> = _currentBottomTab.asStateFlow()

    private val _secondaryScreen = MutableStateFlow(SecondaryWorkspaceScreen.NONE)
    val secondaryScreen: StateFlow<SecondaryWorkspaceScreen> = _secondaryScreen.asStateFlow()

    // --- Reactive Streams from Room & DataStore ---
    val studioPrefs: StateFlow<StudioPreferences> = prefsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudioPreferences())

    val documentsWithDetails: StateFlow<List<KnowledgeDocumentWithDetails>> =
        knowledgeRepository.allDocumentsWithDetails
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocuments: StateFlow<List<KnowledgeDocumentEntity>> =
        knowledgeRepository.recentDocuments
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<KnowledgeNoteEntity>> =
        knowledgeRepository.allNotes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<KnowledgeTagEntity>> =
        knowledgeRepository.allTags
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val characters: StateFlow<List<CharacterEntity>> =
        redWorldRepository.characters
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locations: StateFlow<List<LocationEntity>> =
        redWorldRepository.locations
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldRules: StateFlow<List<WorldRuleEntity>> =
        redWorldRepository.worldRules
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldEvents: StateFlow<List<WorldEventEntity>> =
        redWorldRepository.events
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldItems: StateFlow<List<WorldItemEntity>> =
        redWorldRepository.items
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val relationships: StateFlow<List<RelationshipEntity>> =
        redWorldRepository.relationships
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val versionSnapshots: StateFlow<List<VersionSnapshotEntity>> =
        redWorldRepository.versionSnapshots
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatSessions: StateFlow<List<ChatSessionEntity>> =
        redWorldRepository.chatSessions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val generatedImages: StateFlow<List<GeneratedImageEntity>> =
        redWorldRepository.generatedImages
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Selection States (Preserved across tab switches) ---
    private val _activeChatSessionId = MutableStateFlow(1L)
    val activeChatSessionId: StateFlow<Long> = _activeChatSessionId.asStateFlow()

    val activeChatMessages: StateFlow<List<ChatMessageEntity>> = _activeChatSessionId
        .flatMapLatest { sessionId -> redWorldRepository.observeMessagesForSession(sessionId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDocumentId = MutableStateFlow<Long?>(1L)
    val selectedDocumentId: StateFlow<Long?> = _selectedDocumentId.asStateFlow()

    private val _selectedCharacterId = MutableStateFlow<Long?>(1L)
    val selectedCharacterId: StateFlow<Long?> = _selectedCharacterId.asStateFlow()

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
            // Load initial XLSX sample workbook
            val xlsxFile = DocumentAndXlsxEngine.ensureSampleRedWorldXlsx(getApplication())
            currentXlsxFile = xlsxFile
            _activeXlsxWorkbook.value = DocumentAndXlsxEngine.parseXlsxFile(xlsxFile)
        }
        viewModelScope.launch {
            // Restore persisted navigation & drafts on first emission
            prefsRepository.preferencesFlow.collect { prefs ->
                if (_chatInputDraft.value.isEmpty() && prefs.unsentPromptDraft.isNotEmpty()) {
                    _chatInputDraft.value = prefs.unsentPromptDraft
                }
                if (prefs.unsentImagePromptDraft.isNotEmpty() && _imagePromptDraft.value.startsWith("Portrait of Victor")) {
                    _imagePromptDraft.value = prefs.unsentImagePromptDraft
                }
            }
        }
    }

    // ================= NAVIGATION ACTIONS =================

    fun selectBottomTab(tab: MainBottomTab) {
        _currentBottomTab.value = tab
        _secondaryScreen.value = SecondaryWorkspaceScreen.NONE
        viewModelScope.launch {
            prefsRepository.saveNavigationState(tab = tab.route)
        }
    }

    fun openSecondaryScreen(screen: SecondaryWorkspaceScreen) {
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

    fun createNewChatSession(title: String = "Новый диалог RedWorld") {
        viewModelScope.launch {
            val allDocIds = documentsWithDetails.value.map { it.document.id }.joinToString(",")
            val newId = redWorldRepository.createChatSession(
                title = title,
                modelId = studioPrefs.value.selectedTextModel,
                isRedWorldContextEnabled = true,
                selectedDocumentIdsCsv = allDocIds
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
        val modelId = currentSession?.modelId ?: studioPrefs.value.selectedTextModel
        val useContext = currentSession?.isRedWorldContextEnabled ?: true
        val assistantMode = currentSession?.assistantMode ?: "CONSULTATION"
        val selectedDocIds = currentSession?.selectedDocumentIdsCsv
            ?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?.toSet()

        val attachedImg = _attachedImageBase64.value
        val attachedName = _attachedFileName.value.orEmpty()

        // Clear input draft immediately
        if (overridePrompt == null) {
            updateChatInputDraft("")
            clearChatAttachment()
        }
        _aiErrorBanner.value = null
        _isAiGenerating.value = true

        activeGenerationJob = viewModelScope.launch {
            try {
                // 1. Insert user message into Room
                redWorldRepository.insertChatMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "user",
                        content = rawPrompt,
                        attachmentSummary = attachedName,
                        modelUsed = modelId
                    )
                )

                // 2. Retrieve selective RAG context (NEVER sending all project files blindly)
                val citedSnippets: List<CitedSourceSnippet> = if (useContext) {
                    knowledgeRepository.retrieveRelevantChunksForQuery(
                        query = rawPrompt,
                        allowedDocumentIds = selectedDocIds?.takeIf { it.isNotEmpty() },
                        maxChunks = 4
                    )
                } else {
                    emptyList()
                }

                // 3. Build transparent System Instruction with retrieved fragments & Canon rules
                val systemInstruction = buildString {
                    appendLine("Ты — интеллектуальное ядро RedWorld AI Studio. Отвечай на русском языке чётко, структурированно, используя Markdown.")
                    appendLine("Режим работы: $assistantMode.")
                    appendLine("КРИТИЧЕСКОЕ ПРАВИЛО КАНОНА: Никогда не выдумывай несуществующие ссылки на источники. Если точного ответа нет в переданных фрагментах документов, прямо сообщи об этом и отдели канонические факты от предположений.")
                    if (useContext) {
                        if (citedSnippets.isNotEmpty()) {
                            appendLine("\n--- РЕЛЕВАНТНЫЕ ФРАГМЕНТЫ ИЗ БАЗЫ ЗНАНИЙ REDWORLD ---")
                            citedSnippets.forEachIndexed { idx, snippet ->
                                appendLine("[Источник #${idx + 1}: Документ «${snippet.documentTitle}», раздел «${snippet.sectionTitle}»]")
                                appendLine(snippet.excerpt)
                                appendLine()
                            }
                        } else {
                            appendLine("\nВ выбранных документах прямых совпадений по ключевым словам не найдено. Опирайся только на прямо указанные пользователем сведения.")
                        }
                        // Also include compact summary of characters & locations
                        val charBrief = characters.value.take(5).joinToString("; ") {
                            "${it.name} (статус: ${it.canonStatus}, локация сейчас: ${it.currentLocationName}, род.: ${it.birthDate})"
                        }
                        if (charBrief.isNotEmpty()) {
                            appendLine("Сводка карточек персонажей: $charBrief")
                        }
                    } else {
                        appendLine("Контекст проекта RedWorld отключён пользователем для этого диалога.")
                    }
                }

                // 4. Build multi-turn history (last 8 messages to respect token limits)
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

                // 5. Call Gemini API
                val apiKey = studioPrefs.value.effectiveGeminiApiKey
                val result = geminiRemote.generateChatResponse(
                    apiKey = apiKey,
                    modelId = modelId,
                    conversationHistory = contents,
                    systemInstructionText = systemInstruction
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
                                modelUsed = modelId
                            )
                        )
                    }
                    is GeminiResult.Error -> {
                        _aiErrorBanner.value = result.userMessage
                        // Provide local RAG fallback insight so the user still sees real matched sources offline/without key
                        val ragFallbackText = buildString {
                            appendLine("⚠️ **Не удалось получить ответ от облачной модели Gemini ($modelId)**")
                            appendLine("> ${result.userMessage}")
                            if (citedSnippets.isNotEmpty()) {
                                appendLine("\n### Найдено в локальной базе RedWorld Knowledge (офлайн-режим RAG):")
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
                                modelUsed = "$modelId (Локальный RAG)"
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

    /**
     * Applies an AI-proposed change ONLY after explicit user confirmation, recording a new Version Snapshot.
     */
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
            "Character portrait of ${character.name}, ${character.appearance}, hair: ${character.hair}, eyes: ${character.eyes}, wearing ${character.defaultClothing}, RedWorld gothic crimson atmosphere"
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
                        // Still save prompt history record in Image Studio and inform user honestly
                        redWorldRepository.saveGeneratedImage(
                            GeneratedImageEntity(
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
                diffsJson = json.encodeToString(diffs)
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
                // Also index in Knowledge
                knowledgeRepository.importDocumentFromUri(
                    context = context,
                    uri = uri,
                    fileName = targetFile.name,
                    folderName = "Таблицы XLSX",
                    collectionName = "Карточки проекта"
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
        val summaryObj = mapOf(
            "project" to "RedWorld AI Studio",
            "exportedAt" to System.currentTimeMillis().toString(),
            "charactersCount" to characters.value.size.toString(),
            "locationsCount" to locations.value.size.toString(),
            "documentsCount" to documentsWithDetails.value.size.toString(),
            "rulesCount" to worldRules.value.size.toString(),
            "relationshipsCount" to relationships.value.size.toString()
        )
        return buildString {
            appendLine("{")
            appendLine("""  "project": "RedWorld AI Studio",""")
            appendLine("""  "exportedAt": ${System.currentTimeMillis()},""")
            appendLine("""  "summary": ${json.encodeToString(summaryObj)},""")
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
                currentLocalSummary = "Персонажей: ${characters.value.size}, Документов: ${documentsWithDetails.value.size}"
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
                        fileName = "RedWorld_Backup_${System.currentTimeMillis()}.json",
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
}
