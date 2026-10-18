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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timeline
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
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.screens.RedWorldEditorScreen
import com.example.ui.screens.RedWorldKnowledgeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VersionsAndGuideScreen
import com.example.ui.screens.VisualRelationshipMapScreen
import com.example.ui.screens.WorldSimulationAndRoleplayScreen
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.RedWorldTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: RedWorldViewModel = viewModel()
            val prefs by viewModel.studioPrefs.collectAsStateWithLifecycle()
            RedWorldTheme(themeMode = prefs.appThemeMode) {
                RedWorldAppRoot(viewModel = viewModel)
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

    val sortedCharacters by viewModel.sortedCharacters.collectAsStateWithLifecycle()
    val characterSortMode by viewModel.characterSortMode.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val worldRules by viewModel.worldRules.collectAsStateWithLifecycle()
    val plotlines by viewModel.plotlines.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()

    val worldEvents by viewModel.worldEvents.collectAsStateWithLifecycle()
    val worldItems by viewModel.worldItems.collectAsStateWithLifecycle()
    val relationships by viewModel.relationships.collectAsStateWithLifecycle()
    val versionSnapshots by viewModel.versionSnapshots.collectAsStateWithLifecycle()

    val simulationDraftEvents by viewModel.simulationDraftEvents.collectAsStateWithLifecycle()
    val isSimulatingEvent by viewModel.isSimulatingEvent.collectAsStateWithLifecycle()
    val roleplayCharacterId by viewModel.roleplayCharacterId.collectAsStateWithLifecycle()
    val roleplayMessages by viewModel.roleplayMessages.collectAsStateWithLifecycle()
    val isRoleplayGenerating by viewModel.isRoleplayGenerating.collectAsStateWithLifecycle()
    val roleplayEvaluationReport by viewModel.roleplayEvaluationReport.collectAsStateWithLifecycle()
    val isEvaluatingRoleplay by viewModel.isEvaluatingRoleplay.collectAsStateWithLifecycle()
    val aiDebugModeEnabled by viewModel.aiDebugModeEnabled.collectAsStateWithLifecycle()
    val lastAiDebugExplanation by viewModel.lastAiDebugExplanation.collectAsStateWithLifecycle()

    val isExtractingCharacterFile by viewModel.isExtractingCharacterDossier.collectAsStateWithLifecycle()
    val preselectedRefBase64 by viewModel.imageStudioPreselectedRefBase64.collectAsStateWithLifecycle()
    val preselectedRefTitle by viewModel.imageStudioPreselectedRefTitle.collectAsStateWithLifecycle()

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
    val isVerifyingGeminiKey by viewModel.isVerifyingGeminiKey.collectAsStateWithLifecycle()
    val geminiKeyVerificationMessage by viewModel.geminiKeyVerificationMessage.collectAsStateWithLifecycle()
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
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "World AI Studio",
                            style = MaterialTheme.typography.headlineMedium,
                            color = CrimsonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Активный проект: ${activeProject?.name ?: "RedWorld: Багровый Горизонт"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberGoldSecondary
                        )
                        Text(
                            text = "Творческая среда миров, персонажей, сюжета, документов и ИИ-симуляции",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Разделы активного проекта",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberGoldSecondary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                    )

                    val projectDrawerItems = listOf(
                        Triple(SecondaryWorkspaceScreen.CHARACTERS, "Персонажи (${sortedCharacters.size})", Icons.Default.Person),
                        Triple(SecondaryWorkspaceScreen.LOCATIONS, "Локации (${locations.size})", Icons.Default.Place),
                        Triple(SecondaryWorkspaceScreen.WORLD_RULES, "Правила мира (${worldRules.size})", Icons.Default.Gavel),
                        Triple(SecondaryWorkspaceScreen.PLOT, "Сюжет (${plotlines.size})", Icons.Default.Timeline),
                        Triple(SecondaryWorkspaceScreen.EVENTS_HISTORY, "История событий (${worldEvents.size})", Icons.Default.History),
                        Triple(SecondaryWorkspaceScreen.DOCUMENTS, "Документы (${documentsWithDetails.size})", Icons.Default.Description),
                        Triple(SecondaryWorkspaceScreen.NOTES, "Заметки (${allNotes.size})", Icons.Default.Bookmark),
                        Triple(SecondaryWorkspaceScreen.ITEMS, "Предметы (${worldItems.size})", Icons.Default.Category),
                        Triple(SecondaryWorkspaceScreen.RELATIONSHIP_MAP, "Связи (${relationships.size})", Icons.Default.AccountTree),
                        Triple(SecondaryWorkspaceScreen.WORLD_SIMULATION, "Симуляция и Тест (Глобус)", Icons.Default.Public),
                        Triple(SecondaryWorkspaceScreen.XLSX_EDITOR, "Таблицы XLSX", Icons.Default.TableChart),
                        Triple(SecondaryWorkspaceScreen.VERSIONS_AND_BACKUPS, "Версии и Бэкапы", Icons.Default.CloudSync)
                    )

                    projectDrawerItems.forEach { (screen, label, icon) ->
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
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                selectedIconColor = CrimsonPrimary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = AmberGoldSecondary
                            ),
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                                .testTag("drawer_item_${screen.name.lowercase()}")
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // 11. НАСТРОЙКИ ПРИЛОЖЕНИЯ (значок шестерёнки в левом меню)
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = "Настройки приложения",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        selected = secondaryScreen == SecondaryWorkspaceScreen.SETTINGS,
                        onClick = {
                            viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.SETTINGS)
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Настройки приложения"
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = CrimsonPrimary.copy(alpha = 0.22f),
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            selectedIconColor = CrimsonPrimary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = CrimsonPrimary
                        ),
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("drawer_item_settings")
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        val projectSuffix = activeProject?.name?.let { " • $it" } ?: ""
                        val headerTitle = if (secondaryScreen != SecondaryWorkspaceScreen.NONE) {
                            secondaryScreen.titleRu
                        } else {
                            when (currentTab) {
                                MainBottomTab.HOME -> "World AI Studio$projectSuffix"
                                MainBottomTab.GEMINI -> "${prefs.activeProviderType.displayName}$projectSuffix"
                                MainBottomTab.DOCUMENTS -> "Документы$projectSuffix"
                                MainBottomTab.GENERATION -> "Image Studio$projectSuffix"
                                MainBottomTab.REDWORLD -> "Редактор мира$projectSuffix"
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
                                    tint = MaterialTheme.colorScheme.onSurface
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
                        // Кнопка с изображением глобуса (Симуляция событий и Тестирование персонажа)
                        IconButton(
                            onClick = { viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.WORLD_SIMULATION) },
                            modifier = Modifier.testTag("top_bar_globe_simulation_button")
                        ) {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = "Глобус: Симуляция мира и Тестирование персонажа",
                                tint = CrimsonPrimary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.DOCUMENTS) },
                            modifier = Modifier.testTag("top_bar_knowledge_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Документы проекта",
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
                        IconButton(
                            onClick = { viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.SETTINGS) },
                            modifier = Modifier.testTag("top_bar_settings_button")
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Настройки приложения",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_bottom_navigation")
                ) {
                    val navItems = listOf(
                        Triple(MainBottomTab.HOME, "Главная", Icons.Default.Home),
                        Triple(MainBottomTab.GEMINI, "Gemini", Icons.AutoMirrored.Filled.Chat),
                        Triple(MainBottomTab.DOCUMENTS, "Документы", Icons.Default.Description),
                        Triple(MainBottomTab.GENERATION, "Генерация", Icons.Default.Image),
                        Triple(MainBottomTab.REDWORLD, "Мир", Icons.Default.Public)
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
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = CrimsonPrimary,
                                indicatorColor = CrimsonPrimary.copy(alpha = 0.25f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                @Composable
                fun renderWorldEditor(subTab: Int) {
                    RedWorldEditorScreen(
                        characters = sortedCharacters,
                        locations = locations,
                        worldRules = worldRules,
                        plotlines = plotlines,
                        events = worldEvents,
                        items = worldItems,
                        relationships = relationships,
                        xlsxWorkbook = activeXlsxWorkbook,
                        xlsxDiffs = xlsxLastDiffs,
                        selectedCharacterId = selectedCharacterId,
                        characterSortMode = characterSortMode,
                        isExtractingCharacterFile = isExtractingCharacterFile,
                        onSelectCharacter = { id -> viewModel.selectCharacter(id) },
                        onChangeCharacterSortMode = { mode ->
                            viewModel.setCharacterSortMode(mode)
                            scope.launch { viewModel.prefsRepository.setCharacterSortMode(mode) }
                        },
                        onMoveCharacterOrder = viewModel::moveCharacterSortOrder,
                        onSaveCharacter = viewModel::saveCharacterAndAutoLink,
                        onUpdateCharacterStateOnly = { id, loc, mood, outfit, goal ->
                            scope.launch {
                                viewModel.redWorldRepository.updateCharacterCurrentStateOnly(id, loc, mood, outfit, goal)
                            }
                        },
                        onDeleteCharacter = viewModel::deleteCharacterAndReturnToList,
                        onCloneCharacter = viewModel::cloneCharacter,
                        onToggleCharacterFavorite = viewModel::toggleCharacterFavorite,
                        onToggleCharacterPinned = viewModel::toggleCharacterPinned,
                        onToggleCharacterArchived = viewModel::toggleCharacterArchived,
                        onClearCharacterAvatar = viewModel::clearCharacterAvatar,
                        onExportSingleCharacterUri = { ch, uri, asMd ->
                            viewModel.exportSingleCharacterToUri(context, ch, uri, asMd)
                        },
                        onExtractCharacterFromUri = { uri, name, onExtracted ->
                            viewModel.extractCharacterDossierFromUri(context, uri, name, onExtracted)
                        },
                        onExtractCharacterFromSampleDoc = viewModel::extractCharacterDossierFromSampleDoc,
                        onUpdateCharacterAvatar = viewModel::updateCharacterAvatar,
                        onAddCharacterReference = viewModel::addCharacterReferenceImage,
                        onRemoveCharacterReference = viewModel::removeCharacterReferenceImage,
                        onAddCharacterPrompt = viewModel::addCharacterSavedPrompt,
                        onRemoveCharacterPrompt = viewModel::removeCharacterSavedPrompt,
                        onAddCharacterDeclaredRelation = viewModel::addDeclaredRelationToCharacter,
                        onOpenInImageStudioWithAsset = viewModel::openImageStudioWithCharacterPromptAndRef,
                        onSaveLocation = { loc ->
                            scope.launch {
                                viewModel.redWorldRepository.saveLocation(
                                    loc.copy(projectId = activeProject?.id ?: 1L)
                                )
                            }
                        },
                        onDeleteLocation = { loc ->
                            scope.launch { viewModel.redWorldRepository.deleteLocation(loc) }
                        },
                        onSaveWorldRule = { rule ->
                            scope.launch {
                                viewModel.redWorldRepository.saveWorldRule(
                                    rule.copy(projectId = activeProject?.id ?: 1L)
                                )
                            }
                        },
                        onDeleteWorldRule = { rule ->
                            scope.launch { viewModel.redWorldRepository.deleteWorldRule(rule) }
                        },
                        onSavePlotline = viewModel::savePlotline,
                        onDeletePlotline = viewModel::deletePlotline,
                        onSaveEvent = { ev ->
                            scope.launch {
                                viewModel.redWorldRepository.saveEvent(
                                    ev.copy(projectId = activeProject?.id ?: 1L)
                                )
                            }
                        },
                        onDeleteEvent = { ev ->
                            scope.launch { viewModel.redWorldRepository.deleteEvent(ev) }
                        },
                        onSaveItem = viewModel::saveWorldItem,
                        onDeleteItem = viewModel::deleteWorldItem,
                        onEditXlsxCell = viewModel::editXlsxCellInPlace,
                        onRevertXlsxBackup = viewModel::revertXlsxToLastBackup,
                        onImportXlsxUri = { uri, name -> viewModel.importExternalXlsxUri(context, uri, name) },
                        onExportXlsxUri = viewModel::exportActiveXlsxToSaf,
                        initialSubTab = subTab
                    )
                }

                @Composable
                fun renderDocumentsAndNotes(subTab: Int = 0) {
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
                                    tagIds = tagIds,
                                    projectId = activeProject?.id ?: 1L
                                )
                            }
                        },
                        onImportDocumentUri = { uri, fileName, folder, collection ->
                            scope.launch {
                                viewModel.knowledgeRepository.importDocumentFromUri(
                                    context = context,
                                    uri = uri,
                                    fileName = fileName,
                                    folderName = folder,
                                    collectionName = collection,
                                    projectId = activeProject?.id ?: 1L
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
                                    tagIds = tagIds,
                                    projectId = activeProject?.id ?: 1L
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
                        },
                        onRunDocumentAiAction = { doc, actionType ->
                            viewModel.runDocumentAiAction(doc, actionType) {}
                        },
                        onUpdateDocumentEntityLinks = viewModel::updateDocumentEntityLinks,
                        initialSubTab = subTab
                    )
                }

                when (secondaryScreen) {
                    SecondaryWorkspaceScreen.SETTINGS -> {
                        SettingsScreen(
                            prefs = prefs,
                            isVerifyingKey = isVerifyingGeminiKey,
                            verificationResultMessage = geminiKeyVerificationMessage,
                            onSaveGeminiApiKey = viewModel::saveGeminiApiKeyToDataStore,
                            onClearGeminiApiKey = viewModel::clearGeminiApiKeyFromDataStore,
                            onVerifyGeminiApiKey = viewModel::verifyGeminiApiKey,
                            onSelectDefaultTextModel = { modelId ->
                                scope.launch { viewModel.prefsRepository.setSelectedTextModel(modelId) }
                            },
                            onSelectDefaultImageModel = { modelId ->
                                scope.launch { viewModel.prefsRepository.setSelectedImageModel(modelId) }
                            },
                            onToggleTransmissionNotice = { enabled ->
                                scope.launch { viewModel.prefsRepository.setShowTransmissionNotice(enabled) }
                            },
                            onSelectAiProvider = viewModel::selectAiProvider,
                            onSaveProviderApiKey = viewModel::saveProviderApiKeyToDataStore,
                            onClearProviderApiKey = viewModel::clearProviderApiKeyFromDataStore,
                            onVerifyProviderApiKey = viewModel::verifyProviderApiKey,
                            onSelectProviderPresetModel = viewModel::selectProviderPresetModel,
                            onSaveProviderCustomModelId = viewModel::saveProviderCustomModelId,
                            onUpdateAiGenerationParams = viewModel::updateAiGenerationParameters,
                            onResetAiGenerationParams = viewModel::resetAiGenerationParameters,
                            onSelectAppThemeMode = viewModel::setAppThemeMode,
                            onToggleAiDebugMode = { enabled ->
                                viewModel.setAiDebugModeEnabled(enabled)
                                scope.launch { viewModel.prefsRepository.setAiDebugModeEnabled(enabled) }
                            },
                            onExportActiveProjectJson = { onResult ->
                                viewModel.exportSingleProjectToJson(activeProject?.id ?: 1L, onResult)
                            },
                            onImportProjectJson = { rawJson, overwrite ->
                                viewModel.importSingleProjectFromJson(
                                    rawJson = rawJson,
                                    overwriteProjectId = if (overwrite) (activeProject?.id ?: 1L) else null
                                )
                            },
                            onExportFullBackupUri = viewModel::exportProjectBackupToUri,
                            onImportFullBackupUri = viewModel::inspectAndImportBackupUri,
                            onOpenConnectionsAndDrive = {
                                viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.CONNECTIONS_AND_AUTH)
                            },
                            onOpenVersionsAndBackups = {
                                viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.VERSIONS_AND_BACKUPS)
                            }
                        )
                    }

                    SecondaryWorkspaceScreen.WORLD_SIMULATION -> {
                        WorldSimulationAndRoleplayScreen(
                            characters = sortedCharacters,
                            locations = locations,
                            worldRules = worldRules,
                            plotlines = plotlines,
                            simulationDrafts = simulationDraftEvents,
                            isSimulatingEvents = isSimulatingEvent,
                            roleplayCharacterId = roleplayCharacterId,
                            roleplayMessages = roleplayMessages,
                            isRoleplayGenerating = isRoleplayGenerating,
                            lastEvaluationReport = roleplayEvaluationReport,
                            lastDebugExplanation = lastAiDebugExplanation,
                            onGenerateSimulationEvents = viewModel::generateRandomSimulationEvents,
                            onConfirmDraftToHistory = viewModel::confirmSimulationDraftToHistory,
                            onDeleteDraftEvent = viewModel::deleteSimulationDraftEvent,
                            onUpdateDraftEvent = viewModel::updateSimulationDraftEvent,
                            onAddDraftToPlotline = viewModel::addSimulationDraftToPlotline,
                            onSelectRoleplayCharacter = viewModel::selectRoleplayCharacter,
                            onStartNewRoleplaySession = { viewModel.startNewRoleplaySession() },
                            onSendRoleplayMessage = viewModel::sendRoleplayTestMessage,
                            onMarkMessageFlawed = viewModel::markRoleplayMessageFlawed,
                            onEvaluateRoleplay10Criteria = viewModel::evaluateRoleplayQuality10Criteria,
                            onApplyProposedDiff = viewModel::applyProposedCardDiff,
                            onRejectProposedDiff = viewModel::rejectProposedCardDiff
                        )
                    }

                    SecondaryWorkspaceScreen.CHARACTERS -> renderWorldEditor(0)
                    SecondaryWorkspaceScreen.PLOT -> renderWorldEditor(1)
                    SecondaryWorkspaceScreen.EVENTS_HISTORY -> renderWorldEditor(2)
                    SecondaryWorkspaceScreen.LOCATIONS -> renderWorldEditor(3)
                    SecondaryWorkspaceScreen.WORLD_RULES -> renderWorldEditor(4)
                    SecondaryWorkspaceScreen.ITEMS -> renderWorldEditor(5)
                    SecondaryWorkspaceScreen.XLSX_EDITOR -> renderWorldEditor(6)

                    SecondaryWorkspaceScreen.DOCUMENTS,
                    SecondaryWorkspaceScreen.KNOWLEDGE_RAG -> renderDocumentsAndNotes(0)

                    SecondaryWorkspaceScreen.NOTES -> renderDocumentsAndNotes(2)

                    SecondaryWorkspaceScreen.RELATIONSHIP_MAP -> {
                        VisualRelationshipMapScreen(
                            relationships = relationships,
                            characters = sortedCharacters,
                            locations = locations,
                            onAddRelationship = { rel ->
                                scope.launch {
                                    viewModel.redWorldRepository.saveRelationship(
                                        rel.copy(projectId = activeProject?.id ?: 1L)
                                    )
                                }
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
                            onSelectModel = { m -> viewModel.selectProviderPresetModel(prefs.activeProviderType, m) },
                            onSelectProvider = viewModel::selectAiProvider,
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
                                viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.SETTINGS)
                            }
                        )
                    }

                    SecondaryWorkspaceScreen.FILES_AND_DRIVE,
                    SecondaryWorkspaceScreen.CONNECTIONS_AND_AUTH -> {
                        ConnectionsAndDriveScreen(
                            prefs = prefs,
                            remoteFiles = driveRemoteFiles,
                            conflictInfo = detectedConflict,
                            isSyncing = isDriveSyncing,
                            onSaveCustomGeminiKey = viewModel::saveGeminiApiKeyToDataStore,
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
                                    projects = projects,
                                    activeProject = activeProject,
                                    recentChats = chatSessions,
                                    recentDocs = recentDocuments,
                                    allDocsWithDetails = documentsWithDetails,
                                    characters = sortedCharacters,
                                    generatedImages = generatedImages,
                                    onSelectProject = viewModel::selectProject,
                                    onCreateProjectWithTemplate = { name, genre, desc, template ->
                                        viewModel.createProjectWithTemplate(
                                            name = name,
                                            genreOrTheme = genre,
                                            description = desc,
                                            templateType = template
                                        )
                                    },
                                    onToggleArchiveProject = viewModel::toggleArchiveProject,
                                    onDeleteProject = viewModel::deleteProject,
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
                                    onSelectModel = { m -> viewModel.selectProviderPresetModel(prefs.activeProviderType, m) },
                                    onSelectProvider = viewModel::selectAiProvider,
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
                                        viewModel.openSecondaryScreen(SecondaryWorkspaceScreen.SETTINGS)
                                    }
                                )
                            }

                            MainBottomTab.DOCUMENTS -> renderDocumentsAndNotes(0)

                            MainBottomTab.GENERATION -> {
                                ImageStudioScreen(
                                    prefs = prefs,
                                    characters = sortedCharacters,
                                    locations = locations,
                                    generatedImages = generatedImages,
                                    promptDraft = imagePromptDraft,
                                    preselectedCharacterId = selectedCharacterId,
                                    preselectedRefBase64 = preselectedRefBase64,
                                    preselectedRefTitle = preselectedRefTitle,
                                    isGenerating = isImageGenerating,
                                    onPromptDraftChange = viewModel::updateImagePromptDraft,
                                    onSelectImageModel = { m ->
                                        scope.launch { viewModel.prefsRepository.setSelectedImageModel(m) }
                                    },
                                    onApplyVisualPassport = viewModel::applyCharacterVisualPassportToPrompt,
                                    onSavePromptToCharacterCard = viewModel::addCharacterSavedPrompt,
                                    onSetCharacterAvatar = viewModel::updateCharacterAvatar,
                                    onAddCharacterReference = viewModel::addCharacterReferenceImage,
                                    onGenerateImage = viewModel::generateStudioImage,
                                    onDeleteImage = { img ->
                                        scope.launch { viewModel.redWorldRepository.deleteGeneratedImage(img) }
                                    }
                                )
                            }

                            MainBottomTab.REDWORLD -> renderWorldEditor(0)
                        }
                    }
                }
            }
        }
    }
}
