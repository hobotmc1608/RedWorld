package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainBottomTab
import com.example.ui.RedWorldViewModel
import com.example.ui.SecondaryWorkspaceScreen
import com.example.ui.screens.ConnectionsAndDriveScreen
import com.example.ui.screens.GeminiWorkspaceScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.NotebookLmTabScreen
import com.example.ui.screens.RedWorldEditorScreen
import com.example.ui.screens.RedWorldKnowledgeScreen
import com.example.ui.screens.VersionsAndGuideScreen
import com.example.ui.screens.VisualRelationshipMapScreen
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.BurgundyCardBorder
import com.example.ui.theme.BurgundySurfaceContainer
import com.example.ui.theme.BurgundySurfaceElevated
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.RedWorldTheme
import com.example.ui.theme.TextIvoryPrimary
import com.example.ui.theme.TextMutedRose
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RedWorldTheme {
                RedWorldAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedWorldAppRoot(
    viewModel: RedWorldViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val currentTab by viewModel.currentBottomTab.collectAsStateWithLifecycle()
    val secondaryScreen by viewModel.secondaryScreen.collectAsStateWithLifecycle()
    val prefs by viewModel.studioPrefs.collectAsStateWithLifecycle()

    val documentsWithDetails by viewModel.documentsWithDetails.collectAsStateWithLifecycle()
    val recentDocuments by viewModel.recentDocuments.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()

    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val worldRules by viewModel.worldRules.collectAsStateWithLifecycle()
    val worldEvents by viewModel.worldEvents.collectAsStateWithLifecycle()
    val worldItems by viewModel.worldItems.collectAsStateWithLifecycle()
    val relationships by viewModel.relationships.collectAsStateWithLifecycle()
    val versionSnapshots by viewModel.versionSnapshots.collectAsStateWithLifecycle()

    val chatSessions by viewModel.chatSessions.collectAsStateWithLifecycle()
    val activeChatSessionId by viewModel.activeChatSessionId.collectAsStateWithLifecycle()
    val activeChatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val chatInputDraft by viewModel.chatInputDraft.collectAsStateWithLifecycle()
    val chatSearchQuery by viewModel.chatSearchQuery.collectAsStateWithLifecycle()
    val chatSearchResults by viewModel.chatSearchResults.collectAsStateWithLifecycle()
    val isAiGenerating by viewModel.isAiGenerating.collectAsStateWithLifecycle()
    val aiErrorBanner by viewModel.aiErrorBanner.collectAsStateWithLifecycle()
    val attachedFileName by viewModel.attachedFileName.collectAsStateWithLifecycle()

    val generatedImages by viewModel.generatedImages.collectAsStateWithLifecycle()
    val imagePromptDraft by viewModel.imagePromptDraft.collectAsStateWithLifecycle()
    val isImageGenerating by viewModel.isImageGenerating.collectAsStateWithLifecycle()

    val selectedDocumentId by viewModel.selectedDocumentId.collectAsStateWithLifecycle()
    val selectedCharacterId by viewModel.selectedCharacterId.collectAsStateWithLifecycle()
    val activeXlsxWorkbook by viewModel.activeXlsxWorkbook.collectAsStateWithLifecycle()
    val xlsxLastDiffs by viewModel.xlsxLastDiffs.collectAsStateWithLifecycle()

    val driveRemoteFiles by viewModel.driveRemoteFiles.collectAsStateWithLifecycle()
    val detectedConflict by viewModel.detectedConflict.collectAsStateWithLifecycle()
    val isDriveSyncing by viewModel.isDriveSyncing.collectAsStateWithLifecycle()
    val statusToast by viewModel.statusToast.collectAsStateWithLifecycle()

    LaunchedEffect(statusToast) {
        val msg = statusToast
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusToast()
        }
    }

    // BackHandler for secondary screens and non-home tabs
    BackHandler(enabled = secondaryScreen != SecondaryWorkspaceScreen.NONE || currentTab != MainBottomTab.HOME) {
        if (secondaryScreen != SecondaryWorkspaceScreen.NONE) {
            viewModel.closeSecondaryScreen()
        } else {
            viewModel.selectBottomTab(MainBottomTab.HOME)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = BurgundySurfaceContainer,
                drawerContentColor = TextIvoryPrimary
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "RedWorld AI Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = CrimsonPrimary
                    )
                    Text(
                        text = "ИИ, База знаний RAG и Архив канона",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                }
                HorizontalDivider(color = BurgundyCardBorder)
                Spacer(modifier = Modifier.height(6.dp))

                val drawerItems = listOf(
                    Triple(SecondaryWorkspaceScreen.KNOWLEDGE_RAG, "RedWorld Knowledge (RAG)", Icons.Default.MenuBook),
                    Triple(SecondaryWorkspaceScreen.RELATIONSHIP_MAP, "Карта связей объектов", Icons.Default.AccountTree),
                    Triple(SecondaryWorkspaceScreen.AI_ASSISTANT, "AI-Помощник RedWorld", Icons.Default.AutoAwesome),
                    Triple(SecondaryWorkspaceScreen.XLSX_EDITOR, "Редактор таблиц XLSX", Icons.Default.TableChart),
                    Triple(SecondaryWorkspaceScreen.FILES_AND_DRIVE, "Файлы и Google Drive", Icons.Default.CloudSync),
                    Triple(SecondaryWorkspaceScreen.CONNECTIONS_AND_AUTH, "Подключения и API-ключи", Icons.Default.Key),
                    Triple(SecondaryWorkspaceScreen.VERSIONS_AND_BACKUPS, "История версий и Бэкапы", Icons.Default.History)
                )

                drawerItems.forEach { (screen, label, icon) ->
                    NavigationDrawerItem(
                        label = { Text(label) },
                        selected = secondaryScreen == screen,
                        onClick = {
                            viewModel.openSecondaryScreen(screen)
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = CrimsonPrimary.copy(alpha = 0.22f),
                            selectedTextColor = TextIvoryPrimary,
                            selectedIconColor = CrimsonPrimary,
                            unselectedTextColor = TextIvoryPrimary,
                            unselectedIconColor = AmberGoldSecondary
                        ),
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                            .testTag("drawer_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBackground,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        val headerTitle = if (secondaryScreen != SecondaryWorkspaceScreen.NONE) {
                            secondaryScreen.titleRu
                        } else {
                            when (currentTab) {
                                MainBottomTab.HOME -> "RedWorld AI Studio"
                                MainBottomTab.GEMINI -> "Gemini AI и Контекст RedWorld"
                                MainBottomTab.NOTEBOOK_LM -> "Google NotebookLM"
                                MainBottomTab.GENERATION -> "Image Studio (Генерация)"
                                MainBottomTab.REDWORLD -> "Редактор мира RedWorld"
                            }
                        }
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (secondaryScreen != SecondaryWorkspaceScreen.NONE) {
                            IconButton(
                                onClick = { viewModel.closeSecondaryScreen() },
                                modifier = Modifier.testTag("top_bar_back_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Назад",
                                    tint = TextIvoryPrimary
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("open_drawer_button")
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Меню разделов",
                                    tint = CrimsonPrimary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.KNOWLEDGE_RAG) },
                            modifier = Modifier.testTag("top_bar_knowledge_button")
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = "RedWorld Knowledge",
                                tint = AmberGoldSecondary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.RELATIONSHIP_MAP) },
                            modifier = Modifier.testTag("top_bar_map_button")
                        ) {
                            Icon(
                                Icons.Default.AccountTree,
                                contentDescription = "Карта связей",
                                tint = CrimsonPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BurgundySurfaceContainer,
                        titleContentColor = TextIvoryPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = BurgundySurfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_bottom_navigation")
                ) {
                    val navItems = listOf(
                        Triple(MainBottomTab.HOME, "Главная", Icons.Default.Home),
                        Triple(MainBottomTab.GEMINI, "Gemini", Icons.AutoMirrored.Filled.Chat),
                        Triple(MainBottomTab.NOTEBOOK_LM, "NotebookLM", Icons.Default.Book),
                        Triple(MainBottomTab.GENERATION, "Генерация", Icons.Default.Image),
                        Triple(MainBottomTab.REDWORLD, "RedWorld", Icons.Default.Public)
                    )
                    navItems.forEach { (tab, label, icon) ->
                        val isSelected = secondaryScreen == SecondaryWorkspaceScreen.NONE && currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectBottomTab(tab) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TextIvoryPrimary,
                                selectedTextColor = CrimsonPrimary,
                                indicatorColor = CrimsonPrimary.copy(alpha = 0.28f),
                                unselectedIconColor = TextMutedRose,
                                unselectedTextColor = TextMutedRose
                            ),
                            modifier = Modifier.testTag("bottom_nav_${tab.route.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (secondaryScreen) {
                    SecondaryWorkspaceScreen.KNOWLEDGE_RAG -> {
                        RedWorldKnowledgeScreen(
                            documents = documentsWithDetails,
                            notes = allNotes,
                            tags = allTags,
                            selectedDocumentId = selectedDocumentId,
                            onSelectDocument = { id -> viewModel.openDocumentInKnowledge(id) },
                            onCreateDocument = { title, fileName, format, folder, collection, content, tagIds ->
                                scope.launch {
                                    viewModel.knowledgeRepository.createOrUpdateDocument(
                                        title = title,
                                        fileName = fileName,
                                        format = format,
                                        folderName = folder,
                                        collectionName = collection,
                                        contentText = content,
                                        tagIds = tagIds
                                    )
                                }
                            },
                            onImportDocumentUri = { uri, fileName, folder, collection ->
                                scope.launch {
                                    viewModel.knowledgeRepository.importDocumentFromUri(
                                        context, uri, fileName, folder, collection
                                    )
                                }
                            },
                            onDeleteDocument = { docId ->
                                scope.launch { viewModel.knowledgeRepository.deleteDocument(docId) }
                            },
                            onCreateNote = { title, content, docId, quote, tagIds ->
                                scope.launch {
                                    viewModel.knowledgeRepository.insertNote(
                                        title = title,
                                        content = content,
                                        documentId = docId,
                                        quotedExcerpt = quote,
                                        tagIds = tagIds
                                    )
                                }
                            },
                            onDeleteNote = { noteId ->
                                scope.launch { viewModel.knowledgeRepository.deleteNote(noteId) }
                            },
                            onCreateTag = { name, category ->
                                scope.launch {
                                    viewModel.knowledgeRepository.createTag(name = name, category = category)
                                }
                            },
                            onDeleteTag = { tagId ->
                                scope.launch { viewModel.knowledgeRepository.deleteTag(tagId) }
                            },
                            onAttachTagToDoc = { docId, tagId ->
                                scope.launch { viewModel.knowledgeRepository.attachTagToDocument(docId, tagId) }
                            },
                            onAskGeminiAboutDocument = { question ->
                                viewModel.selectBottomTab(MainBottomTab.GEMINI)
                                viewModel.sendChatMessage(question)
                            }
                        )
                    }

                    SecondaryWorkspaceScreen.RELATIONSHIP_MAP -> {
                        VisualRelationshipMapScreen(
                            relationships = relationships,
                            characters = characters,
                            locations = locations,
                            onAddRelationship = { rel ->
                                scope.launch { viewModel.redWorldRepository.saveRelationship(rel) }
                            },
                            onDeleteRelationship = { id ->
                                scope.launch { viewModel.redWorldRepository.deleteRelationship(id) }
                            },
                            onOpenCharacterCard = { charId ->
                                viewModel.openCharacterInEditor(charId)
                            },
                            onOpenDocumentCard = { docId ->
                                viewModel.openDocumentInKnowledge(docId)
                            }
                        )
                    }

                    SecondaryWorkspaceScreen.AI_ASSISTANT -> {
                        GeminiWorkspaceScreen(
                            prefs = prefs,
                            sessions = chatSessions,
                            activeSessionId = activeChatSessionId,
                            messages = activeChatMessages,
                            allDocuments = documentsWithDetails,
                            inputDraft = chatInputDraft,
                            searchQuery = chatSearchQuery,
                            searchResults = chatSearchResults,
                            isGenerating = isAiGenerating,
                            errorBanner = aiErrorBanner,
                            attachedFileName = attachedFileName,
                            onInputDraftChange = viewModel::updateChatInputDraft,
                            onSearchQueryChange = viewModel::updateChatSearchQuery,
                            onSelectSession = viewModel::selectChatSession,
                            onCreateSession = viewModel::createNewChatSession,
                            onRenameSession = viewModel::renameChatSession,
                            onDeleteSession = viewModel::deleteChatSession,
                            onSelectModel = { m -> scope.launch { viewModel.prefsRepository.setSelectedTextModel(m) } },
                            onToggleRedWorldContext = viewModel::toggleSessionRedWorldContext,
                            onToggleDocInContext = viewModel::toggleDocumentInSessionContext,
                            onSetAssistantMode = viewModel::setSessionAssistantMode,
                            onAttachImage = viewModel::attachImageToChat,
                            onClearAttachment = viewModel::clearChatAttachment,
                            onSendMessage = viewModel::sendChatMessage,
                            onStopGeneration = viewModel::stopCurrentGeneration,
                            onRegenerateLast = viewModel::regenerateLastResponse,
                            onConfirmAiProposal = viewModel::confirmAndApplyAiProposal,
                            onOpenCitedDocument = viewModel::openDocumentInKnowledge,
                            onOpenApiKeySettings = {
                                viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.CONNECTIONS_AND_AUTH)
                            }
                        )
                    }

                    SecondaryWorkspaceScreen.XLSX_EDITOR -> {
                        RedWorldEditorScreen(
                            characters = characters,
                            locations = locations,
                            worldRules = worldRules,
                            events = worldEvents,
                            items = worldItems,
                            xlsxWorkbook = activeXlsxWorkbook,
                            xlsxDiffs = xlsxLastDiffs,
                            selectedCharacterId = selectedCharacterId,
                            onSelectCharacter = { id -> viewModel.openCharacterInEditor(id) },
                            onSaveCharacter = { ch, summary ->
                                scope.launch { viewModel.redWorldRepository.saveCharacterWithSnapshot(ch, summary) }
                            },
                            onUpdateCharacterStateOnly = { id, loc, mood, outfit, goal ->
                                scope.launch {
                                    viewModel.redWorldRepository.updateCharacterCurrentStateOnly(id, loc, mood, outfit, goal)
                                }
                            },
                            onDeleteCharacter = { ch ->
                                scope.launch { viewModel.redWorldRepository.deleteCharacter(ch) }
                            },
                            onSaveLocation = { loc ->
                                scope.launch { viewModel.redWorldRepository.saveLocation(loc) }
                            },
                            onSaveWorldRule = { rule ->
                                scope.launch { viewModel.redWorldRepository.saveWorldRule(rule) }
                            },
                            onSaveEvent = { ev ->
                                scope.launch { viewModel.redWorldRepository.saveEvent(ev) }
                            },
                            onEditXlsxCell = viewModel::editXlsxCellInPlace,
                            onRevertXlsxBackup = viewModel::revertXlsxToLastBackup,
                            onImportXlsxUri = { uri, name -> viewModel.importExternalXlsxUri(context, uri, name) },
                            onExportXlsxUri = viewModel::exportActiveXlsxToSaf,
                            initialSubTab = 4
                        )
                    }

                    SecondaryWorkspaceScreen.FILES_AND_DRIVE,
                    SecondaryWorkspaceScreen.CONNECTIONS_AND_AUTH -> {
                        ConnectionsAndDriveScreen(
                            prefs = prefs,
                            remoteFiles = driveRemoteFiles,
                            conflictInfo = detectedConflict,
                            isSyncing = isDriveSyncing,
                            onSaveCustomGeminiKey = { key ->
                                scope.launch { viewModel.prefsRepository.setCustomGeminiApiKey(key) }
                            },
                            onSaveGoogleConnection = { email, connected, token, folder ->
                                scope.launch {
                                    viewModel.prefsRepository.updateGoogleConnection(email, connected, token, folder)
                                }
                            },
                            onDisconnectGoogle = {
                                scope.launch { viewModel.prefsRepository.disconnectGoogleAccount() }
                            },
                            onSyncDriveRestApi = viewModel::syncWithGoogleDriveRestApi,
                            onExportProjectSafUri = viewModel::exportProjectBackupToUri,
                            onInspectAndImportSafUri = viewModel::inspectAndImportBackupUri,
                            onDismissConflict = viewModel::dismissConflictDialog
                        )
                    }

                    SecondaryWorkspaceScreen.VERSIONS_AND_BACKUPS,
                    SecondaryWorkspaceScreen.DOCUMENTATION_GUIDE -> {
                        VersionsAndGuideScreen(
                            snapshots = versionSnapshots,
                            onRestoreSnapshot = { snap ->
                                scope.launch { viewModel.redWorldRepository.restoreCharacterFromSnapshot(snap) }
                            }
                        )
                    }

                    SecondaryWorkspaceScreen.NONE -> {
                        when (currentTab) {
                            MainBottomTab.HOME -> {
                                HomeDashboardScreen(
                                    prefs = prefs,
                                    recentChats = chatSessions,
                                    recentDocs = recentDocuments,
                                    allDocsWithDetails = documentsWithDetails,
                                    characters = characters,
                                    generatedImages = generatedImages,
                                    onSelectBottomTab = viewModel::selectBottomTab,
                                    onOpenSecondaryScreen = viewModel::openSecondaryScreen,
                                    onOpenChatSession = viewModel::selectChatSession,
                                    onOpenDocument = viewModel::openDocumentInKnowledge,
                                    onOpenCharacter = viewModel::openCharacterInEditor
                                )
                            }

                            MainBottomTab.GEMINI -> {
                                GeminiWorkspaceScreen(
                                    prefs = prefs,
                                    sessions = chatSessions,
                                    activeSessionId = activeChatSessionId,
                                    messages = activeChatMessages,
                                    allDocuments = documentsWithDetails,
                                    inputDraft = chatInputDraft,
                                    searchQuery = chatSearchQuery,
                                    searchResults = chatSearchResults,
                                    isGenerating = isAiGenerating,
                                    errorBanner = aiErrorBanner,
                                    attachedFileName = attachedFileName,
                                    onInputDraftChange = viewModel::updateChatInputDraft,
                                    onSearchQueryChange = viewModel::updateChatSearchQuery,
                                    onSelectSession = viewModel::selectChatSession,
                                    onCreateSession = viewModel::createNewChatSession,
                                    onRenameSession = viewModel::renameChatSession,
                                    onDeleteSession = viewModel::deleteChatSession,
                                    onSelectModel = { m -> scope.launch { viewModel.prefsRepository.setSelectedTextModel(m) } },
                                    onToggleRedWorldContext = viewModel::toggleSessionRedWorldContext,
                                    onToggleDocInContext = viewModel::toggleDocumentInSessionContext,
                                    onSetAssistantMode = viewModel::setSessionAssistantMode,
                                    onAttachImage = viewModel::attachImageToChat,
                                    onClearAttachment = viewModel::clearChatAttachment,
                                    onSendMessage = viewModel::sendChatMessage,
                                    onStopGeneration = viewModel::stopCurrentGeneration,
                                    onRegenerateLast = viewModel::regenerateLastResponse,
                                    onConfirmAiProposal = viewModel::confirmAndApplyAiProposal,
                                    onOpenCitedDocument = viewModel::openDocumentInKnowledge,
                                    onOpenApiKeySettings = {
                                        viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.CONNECTIONS_AND_AUTH)
                                    }
                                )
                            }

                            MainBottomTab.NOTEBOOK_LM -> {
                                NotebookLmTabScreen(
                                    onSwitchToKnowledgeLibrary = {
                                        viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.KNOWLEDGE_RAG)
                                    },
                                    onSwitchToGeminiChat = {
                                        viewModel.selectBottomTab(MainBottomTab.GEMINI)
                                    }
                                )
                            }

                            MainBottomTab.GENERATION -> {
                                ImageStudioScreen(
                                    prefs = prefs,
                                    characters = characters,
                                    locations = locations,
                                    generatedImages = generatedImages,
                                    promptDraft = imagePromptDraft,
                                    isGenerating = isImageGenerating,
                                    onPromptDraftChange = viewModel::updateImagePromptDraft,
                                    onSelectImageModel = { m ->
                                        scope.launch { viewModel.prefsRepository.setSelectedImageModel(m) }
                                    },
                                    onApplyVisualPassport = viewModel::applyCharacterVisualPassportToPrompt,
                                    onGenerateImage = viewModel::generateStudioImage,
                                    onDeleteImage = { img ->
                                        scope.launch { viewModel.redWorldRepository.deleteGeneratedImage(img) }
                                    }
                                )
                            }

                            MainBottomTab.REDWORLD -> {
                                RedWorldEditorScreen(
                                    characters = characters,
                                    locations = locations,
                                    worldRules = worldRules,
                                    events = worldEvents,
                                    items = worldItems,
                                    xlsxWorkbook = activeXlsxWorkbook,
                                    xlsxDiffs = xlsxLastDiffs,
                                    selectedCharacterId = selectedCharacterId,
                                    onSelectCharacter = { id -> viewModel.openCharacterInEditor(id) },
                                    onSaveCharacter = { ch, summary ->
                                        scope.launch { viewModel.redWorldRepository.saveCharacterWithSnapshot(ch, summary) }
                                    },
                                    onUpdateCharacterStateOnly = { id, loc, mood, outfit, goal ->
                                        scope.launch {
                                            viewModel.redWorldRepository.updateCharacterCurrentStateOnly(id, loc, mood, outfit, goal)
                                        }
                                    },
                                    onDeleteCharacter = { ch ->
                                        scope.launch { viewModel.redWorldRepository.deleteCharacter(ch) }
                                    },
                                    onSaveLocation = { loc ->
                                        scope.launch { viewModel.redWorldRepository.saveLocation(loc) }
                                    },
                                    onSaveWorldRule = { rule ->
                                        scope.launch { viewModel.redWorldRepository.saveWorldRule(rule) }
                                    },
                                    onSaveEvent = { ev ->
                                        scope.launch { viewModel.redWorldRepository.saveEvent(ev) }
                                    },
                                    onEditXlsxCell = viewModel::editXlsxCellInPlace,
                                    onRevertXlsxBackup = viewModel::revertXlsxToLastBackup,
                                    onImportXlsxUri = { uri, name -> viewModel.importExternalXlsxUri(context, uri, name) },
                                    onExportXlsxUri = viewModel::exportActiveXlsxToSaf
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
