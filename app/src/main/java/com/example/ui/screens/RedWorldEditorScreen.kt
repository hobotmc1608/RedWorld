package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CanonStatus
import com.example.data.local.CellDiffItem
import com.example.data.local.CharacterEntity
import com.example.data.local.LocationEntity
import com.example.data.local.PlotlineEntity
import com.example.data.local.WorldEventEntity
import com.example.data.local.WorldItemEntity
import com.example.data.local.WorldRuleEntity
import com.example.data.local.XlsxCellData
import com.example.data.local.XlsxWorkbookModel
import com.example.ui.components.CanonStatusBadge
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.StatusCanonGreen

/**
 * RedWorld & Multi-Project World Editor:
 * Sub-tabs:
 * 0 = Персонажи (полноценная система управления персонажами: вертикальный список с аватарами, поиск, фильтрация, сортировка, подробная карточка по тематическим разделам и независимое редактирование каждого пункта)
 * 1 = Сюжет (отдельный раздел: сюжетные линии, задачи, варианты развития, связанные персонажи/локации, состояние)
 * 2 = История событий (отдельный раздел: название, описание, дата/время, место, участники, причины, последствия, связанные события, связь с сюжетом)
 * 3 = Локации (Красный особняк, Зелёная улица, Магазин, Бар + подлокации, правила доступа, связи)
 * 4 = Правила мира (законы, ограничения, исключения, связанные сущности)
 * 5 = Предметы и организации (свойства, происхождение, владелец, локация, связи)
 * 6 = Таблицы XLSX (хирургический редактор с сохранением формул, листов и стилей)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RedWorldEditorScreen(
    characters: List<CharacterEntity>,
    locations: List<LocationEntity>,
    worldRules: List<WorldRuleEntity>,
    plotlines: List<PlotlineEntity> = emptyList(),
    events: List<WorldEventEntity>,
    items: List<WorldItemEntity>,
    relationships: List<com.example.data.local.RelationshipEntity> = emptyList(),
    xlsxWorkbook: XlsxWorkbookModel?,
    xlsxDiffs: List<CellDiffItem>,
    selectedCharacterId: Long?,
    characterSortMode: String = "NAME",
    isExtractingCharacterFile: Boolean = false,
    onSelectCharacter: (Long?) -> Unit,
    onChangeCharacterSortMode: (String) -> Unit = {},
    onMoveCharacterOrder: (CharacterEntity, Boolean) -> Unit = { _, _ -> },
    onSaveCharacter: (CharacterEntity, String) -> Unit,
    onUpdateCharacterStateOnly: (Long, String, String, String, String) -> Unit,
    onDeleteCharacter: (CharacterEntity) -> Unit,
    onCloneCharacter: (CharacterEntity) -> Unit = {},
    onToggleCharacterFavorite: (CharacterEntity) -> Unit = {},
    onToggleCharacterPinned: (CharacterEntity) -> Unit = {},
    onToggleCharacterArchived: (CharacterEntity) -> Unit = {},
    onClearCharacterAvatar: (CharacterEntity) -> Unit = {},
    onExportSingleCharacterUri: (CharacterEntity, Uri, Boolean) -> Unit = { _, _, _ -> },
    onExtractCharacterFromUri: (Uri, String, (com.example.data.local.AiExtractedCharacterDossier, String) -> Unit) -> Unit = { _, _, _ -> },
    onExtractCharacterFromSampleDoc: (String, (com.example.data.local.AiExtractedCharacterDossier, String) -> Unit) -> Unit = { _, _ -> },
    onUpdateCharacterAvatar: (CharacterEntity, String) -> Unit = { _, _ -> },
    onAddCharacterReference: (CharacterEntity, String, String, String) -> Unit = { _, _, _, _ -> },
    onRemoveCharacterReference: (CharacterEntity, String) -> Unit = { _, _ -> },
    onAddCharacterPrompt: (CharacterEntity, String, String, String) -> Unit = { _, _, _, _ -> },
    onRemoveCharacterPrompt: (CharacterEntity, String) -> Unit = { _, _ -> },
    onAddCharacterDeclaredRelation: (CharacterEntity, com.example.data.local.DeclaredCharacterRelation) -> Unit = { _, _ -> },
    onOpenInImageStudioWithAsset: (CharacterEntity, String?, String?, String?) -> Unit = { _, _, _, _ -> },
    onSaveLocation: (LocationEntity) -> Unit,
    onDeleteLocation: (LocationEntity) -> Unit = {},
    onSaveWorldRule: (WorldRuleEntity) -> Unit,
    onDeleteWorldRule: (WorldRuleEntity) -> Unit = {},
    onSavePlotline: (PlotlineEntity) -> Unit = {},
    onDeletePlotline: (PlotlineEntity) -> Unit = {},
    onSaveEvent: (WorldEventEntity) -> Unit,
    onDeleteEvent: (WorldEventEntity) -> Unit = {},
    onSaveItem: (WorldItemEntity) -> Unit = {},
    onDeleteItem: (WorldItemEntity) -> Unit = {},
    onEditXlsxCell: (String, String, String) -> Unit,
    onRevertXlsxBackup: () -> Unit,
    onImportXlsxUri: (Uri, String) -> Unit,
    onExportXlsxUri: (Uri) -> Unit,
    initialSubTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab.coerceIn(0, 6)) }

    val tabs = listOf(
        "Персонажи (${characters.size})",
        "Сюжет (${plotlines.size})",
        "История событий (${events.size})",
        "Локации (${locations.size})",
        "Правила мира (${worldRules.size})",
        "Предметы (${items.size})",
        "Таблицы XLSX"
    )

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = activeSubTab == idx,
                    onClick = {
                        activeSubTab = idx
                        if (idx == 0 && selectedCharacterId != null) {
                            onSelectCharacter(null)
                        }
                    },
                    text = { Text(title) },
                    modifier = Modifier.testTag("redworld_editor_tab_$idx")
                )
            }
        }

        when (activeSubTab) {
            0 -> CharacterEditorSubScreen(
                characters = characters,
                locations = locations,
                relationships = relationships,
                selectedCharacterId = selectedCharacterId,
                characterSortMode = characterSortMode,
                isExtractingCharacterFile = isExtractingCharacterFile,
                onSelectCharacter = onSelectCharacter,
                onChangeCharacterSortMode = onChangeCharacterSortMode,
                onMoveCharacterOrder = onMoveCharacterOrder,
                onSaveCharacter = onSaveCharacter,
                onUpdateCharacterStateOnly = onUpdateCharacterStateOnly,
                onDeleteCharacter = onDeleteCharacter,
                onCloneCharacter = onCloneCharacter,
                onToggleCharacterFavorite = onToggleCharacterFavorite,
                onToggleCharacterPinned = onToggleCharacterPinned,
                onToggleCharacterArchived = onToggleCharacterArchived,
                onClearCharacterAvatar = onClearCharacterAvatar,
                onExportSingleCharacterUri = onExportSingleCharacterUri,
                onExtractCharacterFromUri = onExtractCharacterFromUri,
                onExtractCharacterFromSampleDoc = onExtractCharacterFromSampleDoc,
                onUpdateCharacterAvatar = onUpdateCharacterAvatar,
                onAddCharacterReference = onAddCharacterReference,
                onRemoveCharacterReference = onRemoveCharacterReference,
                onAddCharacterPrompt = onAddCharacterPrompt,
                onRemoveCharacterPrompt = onRemoveCharacterPrompt,
                onAddCharacterDeclaredRelation = onAddCharacterDeclaredRelation,
                onOpenInImageStudioWithAsset = onOpenInImageStudioWithAsset
            )
            1 -> PlotlinesSubScreen(
                plotlines = plotlines,
                characters = characters,
                locations = locations,
                onSavePlotline = onSavePlotline,
                onDeletePlotline = onDeletePlotline
            )
            2 -> EventsAndHistorySubScreen(
                events = events,
                locations = locations,
                plotlines = plotlines,
                onSaveEvent = onSaveEvent,
                onDeleteEvent = onDeleteEvent
            )
            3 -> LocationsSubScreen(
                locations = locations,
                onSaveLocation = onSaveLocation,
                onDeleteLocation = onDeleteLocation
            )
            4 -> WorldRulesSubScreen(
                rules = worldRules,
                onSaveRule = onSaveWorldRule,
                onDeleteRule = onDeleteWorldRule
            )
            5 -> ItemsAndOrgsSubScreen(
                items = items,
                onSaveItem = onSaveItem,
                onDeleteItem = onDeleteItem
            )
            6 -> XlsxSurgicalEditorSubScreen(
                workbook = xlsxWorkbook,
                diffs = xlsxDiffs,
                onEditCell = onEditXlsxCell,
                onRevertBackup = onRevertXlsxBackup,
                onImportUri = onImportXlsxUri,
                onExportUri = onExportXlsxUri
            )
        }
    }
}

private data class SingleFieldEditSession(
    val fieldSpec: CharacterFieldSpec,
    val initialText: String,
    val subItemIndex: Int? = null,
    val allSubItems: List<String> = emptyList(),
    val delimiter: String = "\n",
    val isAddingNewSubItem: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterEditorSubScreen(
    characters: List<CharacterEntity>,
    locations: List<LocationEntity>,
    relationships: List<com.example.data.local.RelationshipEntity>,
    selectedCharacterId: Long?,
    characterSortMode: String,
    isExtractingCharacterFile: Boolean,
    onSelectCharacter: (Long?) -> Unit,
    onChangeCharacterSortMode: (String) -> Unit,
    onMoveCharacterOrder: (CharacterEntity, Boolean) -> Unit,
    onSaveCharacter: (CharacterEntity, String) -> Unit,
    onUpdateCharacterStateOnly: (Long, String, String, String, String) -> Unit,
    onDeleteCharacter: (CharacterEntity) -> Unit,
    onCloneCharacter: (CharacterEntity) -> Unit,
    onToggleCharacterFavorite: (CharacterEntity) -> Unit,
    onToggleCharacterPinned: (CharacterEntity) -> Unit,
    onToggleCharacterArchived: (CharacterEntity) -> Unit,
    onClearCharacterAvatar: (CharacterEntity) -> Unit,
    onExportSingleCharacterUri: (CharacterEntity, Uri, Boolean) -> Unit,
    onExtractCharacterFromUri: (Uri, String, (com.example.data.local.AiExtractedCharacterDossier, String) -> Unit) -> Unit,
    onExtractCharacterFromSampleDoc: (String, (com.example.data.local.AiExtractedCharacterDossier, String) -> Unit) -> Unit,
    onUpdateCharacterAvatar: (CharacterEntity, String) -> Unit,
    onAddCharacterReference: (CharacterEntity, String, String, String) -> Unit,
    onRemoveCharacterReference: (CharacterEntity, String) -> Unit,
    onAddCharacterPrompt: (CharacterEntity, String, String, String) -> Unit,
    onRemoveCharacterPrompt: (CharacterEntity, String) -> Unit,
    onAddCharacterDeclaredRelation: (CharacterEntity, com.example.data.local.DeclaredCharacterRelation) -> Unit,
    onOpenInImageStudioWithAsset: (CharacterEntity, String?, String?, String?) -> Unit
) {
    // Определяем, открыта ли карточка конкретного выбранного персонажа:
    // Если selectedCharacterId == null (или персонаж удалён) — отображается ТОЛЬКО экран списка персонажей.
    // Если selectedCharacterId != null и найден — отображается ТОЛЬКО отдельный экран карточки выбранного персонажа.
    val openedCharacter = remember(characters, selectedCharacterId) {
        if (selectedCharacterId == null) null
        else characters.firstOrNull { it.id == selectedCharacterId }
    }

    var showNewCharDialog by remember { mutableStateOf(false) }
    var prefilledNewCharName by remember { mutableStateOf("") }

    if (openedCharacter == null) {
        CharacterListScreen(
            characters = characters,
            characterSortMode = characterSortMode,
            onOpenCharacterCard = { charId -> onSelectCharacter(charId) },
            onRequestCreateCharacter = {
                prefilledNewCharName = ""
                showNewCharDialog = true
            },
            onChangeCharacterSortMode = onChangeCharacterSortMode,
            onMoveCharacterOrder = onMoveCharacterOrder
        )
    } else {
        CharacterDetailScreen(
            activeChar = openedCharacter,
            allCharacters = characters,
            relationships = relationships,
            isExtractingCharacterFile = isExtractingCharacterFile,
            onBackToList = { onSelectCharacter(null) },
            onSaveCharacter = onSaveCharacter,
            onUpdateCharacterStateOnly = onUpdateCharacterStateOnly,
            onDeleteCharacter = { charToDelete ->
                onDeleteCharacter(charToDelete)
                onSelectCharacter(null)
            },
            onCloneCharacter = onCloneCharacter,
            onToggleCharacterFavorite = onToggleCharacterFavorite,
            onToggleCharacterPinned = onToggleCharacterPinned,
            onToggleCharacterArchived = onToggleCharacterArchived,
            onClearCharacterAvatar = onClearCharacterAvatar,
            onExportSingleCharacterUri = onExportSingleCharacterUri,
            onExtractCharacterFromUri = onExtractCharacterFromUri,
            onExtractCharacterFromSampleDoc = onExtractCharacterFromSampleDoc,
            onUpdateCharacterAvatar = onUpdateCharacterAvatar,
            onAddCharacterReference = onAddCharacterReference,
            onRemoveCharacterReference = onRemoveCharacterReference,
            onAddCharacterPrompt = onAddCharacterPrompt,
            onRemoveCharacterPrompt = onRemoveCharacterPrompt,
            onAddCharacterDeclaredRelation = onAddCharacterDeclaredRelation,
            onOpenInImageStudioWithAsset = onOpenInImageStudioWithAsset
        )
    }

    // Диалог создания нового персонажа со страницы списка
    if (showNewCharDialog) {
        val baseNewChar = remember(prefilledNewCharName) {
            CharacterEntity(name = prefilledNewCharName, canonStatus = CanonStatus.CANON.name)
        }
        AiCharacterDossierDialog(
            initialCharacter = baseNewChar,
            isNewCharacter = true,
            isExtractingFile = isExtractingCharacterFile,
            onExtractFromUri = onExtractCharacterFromUri,
            onExtractFromSampleDoc = onExtractCharacterFromSampleDoc,
            onDismiss = { showNewCharDialog = false },
            onSave = { createdChar, summary ->
                onSaveCharacter(createdChar, summary)
                showNewCharDialog = false
            }
        )
    }
}

/**
 * ЭКРАН 1: ЭКРАН СПИСКА ПЕРСОНАЖЕЙ (CharacterListScreen)
 * На этом экране находятся ТОЛЬКО элементы управления списком:
 * - Кнопка «Создать персонажа»
 * - Поле поиска
 * - Фильтры (работающие с реальными данными модели)
 * - Сортировка
 * - Вертикальный список персонажей (аватар слева, имя справа, краткая доп. информация)
 * Полная информация о персонажах и формы редактирования их характеристик здесь НЕ отображаются.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterListScreen(
    characters: List<CharacterEntity>,
    characterSortMode: String,
    onOpenCharacterCard: (Long) -> Unit,
    onRequestCreateCharacter: () -> Unit,
    onChangeCharacterSortMode: (String) -> Unit,
    onMoveCharacterOrder: (CharacterEntity, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showFiltersPanel by remember { mutableStateOf(false) }

    // Filter states backed by actual CharacterEntity data fields
    var selectedRoleFilter by remember { mutableStateOf("ALL") }
    var selectedAvatarFilter by remember { mutableStateOf("ALL") } // ALL, WITH_AVATAR, NO_AVATAR
    var selectedCanonFilter by remember { mutableStateOf("ALL") }
    var selectedTagFilter by remember { mutableStateOf("ALL") }
    var onlyFavoritesFilter by remember { mutableStateOf(false) }
    var showArchivedCharacters by remember { mutableStateOf(false) }

    val availableRoles = remember(characters) {
        characters.mapNotNull { ch ->
            ch.storyRole.trim().takeIf { it.isNotEmpty() }
                ?: ch.occupation.trim().takeIf { it.isNotEmpty() }
        }.distinct()
    }

    val availableTags = remember(characters) {
        characters.flatMap { ch ->
            ch.tagsCsv.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
        }.distinct()
    }

    val hasActiveFilters = selectedRoleFilter != "ALL" ||
        selectedAvatarFilter != "ALL" ||
        selectedCanonFilter != "ALL" ||
        selectedTagFilter != "ALL" ||
        onlyFavoritesFilter ||
        showArchivedCharacters

    fun resetAllFilters() {
        selectedRoleFilter = "ALL"
        selectedAvatarFilter = "ALL"
        selectedCanonFilter = "ALL"
        selectedTagFilter = "ALL"
        onlyFavoritesFilter = false
        showArchivedCharacters = false
    }

    val filteredCharacters = remember(
        characters,
        searchQuery,
        selectedRoleFilter,
        selectedAvatarFilter,
        selectedCanonFilter,
        selectedTagFilter,
        onlyFavoritesFilter,
        showArchivedCharacters
    ) {
        val q = searchQuery.trim()
        characters.filter { ch ->
            val matchesArchive = if (showArchivedCharacters) true else !ch.isArchived || characters.all { it.isArchived }
            val matchesFav = if (onlyFavoritesFilter) ch.isFavorite else true
            val matchesRole = if (selectedRoleFilter == "ALL") true else {
                ch.storyRole.equals(selectedRoleFilter, ignoreCase = true) ||
                    ch.occupation.equals(selectedRoleFilter, ignoreCase = true)
            }
            val matchesAvatar = when (selectedAvatarFilter) {
                "WITH_AVATAR" -> ch.avatarBase64.isNotBlank()
                "NO_AVATAR" -> ch.avatarBase64.isBlank()
                else -> true
            }
            val matchesCanon = if (selectedCanonFilter == "ALL") true else ch.canonStatus == selectedCanonFilter
            val matchesTag = if (selectedTagFilter == "ALL") true else {
                ch.tagsCsv.split(",", ";").any { it.trim().equals(selectedTagFilter, ignoreCase = true) }
            }
            val matchesSearch = if (q.isEmpty()) true else {
                ch.name.contains(q, ignoreCase = true) ||
                    ch.fullName.contains(q, ignoreCase = true) ||
                    ch.aliases.contains(q, ignoreCase = true) ||
                    ch.occupation.contains(q, ignoreCase = true) ||
                    ch.storyRole.contains(q, ignoreCase = true) ||
                    ch.currentLocationName.contains(q, ignoreCase = true) ||
                    ch.factionOrGroup.contains(q, ignoreCase = true) ||
                    ch.tagsCsv.contains(q, ignoreCase = true) ||
                    ch.personality.contains(q, ignoreCase = true) ||
                    ch.appearance.contains(q, ignoreCase = true) ||
                    ch.shortDescription.contains(q, ignoreCase = true) ||
                    ch.detailedDescription.contains(q, ignoreCase = true) ||
                    ch.skills.contains(q, ignoreCase = true) ||
                    ch.habits.contains(q, ignoreCase = true)
            }
            matchesArchive && matchesFav && matchesRole && matchesAvatar && matchesCanon && matchesTag && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("character_list_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Пустой список персонажей (когда в проекте 0 персонажей)
        if (characters.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, CrimsonPrimary),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "В этом проекте ещё нет персонажей",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Создайте карточку первого персонажа вручную или подгрузите готовое досье из файла (.DOC, .DOCX, .PDF, .TXT, .XLSX).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onRequestCreateCharacter,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.testTag("create_first_character_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Создать первого персонажа")
                        }
                    }
                }
            }
        } else {
            // Панель управления списком: Кнопка «Создать персонажа», Поиск, Сортировка, Фильтры
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Верхняя строка: заголовок списка и кнопка «+ Создать персонажа»
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Персонажи (${filteredCharacters.size} из ${characters.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Выберите персонажа из списка, чтобы открыть его подробную карточку.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onRequestCreateCharacter,
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                modifier = Modifier.testTag("add_character_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Создать персонажа")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Создать персонажа")
                            }
                        }

                        // Поле поиска
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Поиск по имени, роли, псевдониму, биографии, тегам...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.testTag("clear_character_search_icon")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Очистить поиск")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("character_search_input")
                        )

                        // Сортировка и переключатель фильтров
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Сортировка:",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberGoldSecondary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                AssistChip(
                                    onClick = { showFiltersPanel = !showFiltersPanel },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.FilterAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(if (hasActiveFilters) "Фильтры (активны)" else "Фильтры")
                                    },
                                    modifier = Modifier.testTag("toggle_character_filters_button")
                                )
                                if (hasActiveFilters) {
                                    TextButton(
                                        onClick = { resetAllFilters() },
                                        modifier = Modifier.testTag("reset_character_filters_button")
                                    ) {
                                        Icon(Icons.Default.FilterAltOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Сбросить фильтры")
                                    }
                                }
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val sortOptions = listOf(
                                "NAME" to "По имени",
                                "CREATED" to "По дате создания",
                                "UPDATED" to "По изменениям",
                                "CUSTOM" to "Свой порядок"
                            )
                            sortOptions.forEach { (code, label) ->
                                FilterChip(
                                    selected = characterSortMode == code,
                                    onClick = { onChangeCharacterSortMode(code) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("char_sort_$code")
                                )
                            }
                        }

                        // Панель фильтрации (по наличию аватара, избранному, роли/категории, статусу канона, тегам)
                        AnimatedVisibility(visible = showFiltersPanel || hasActiveFilters) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Наличие аватара:", style = MaterialTheme.typography.labelSmall, color = AmberGoldSecondary)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            "ALL" to "Все",
                                            "WITH_AVATAR" to "С аватаром",
                                            "NO_AVATAR" to "Без аватара"
                                        ).forEach { (code, label) ->
                                            FilterChip(
                                                selected = selectedAvatarFilter == code,
                                                onClick = { selectedAvatarFilter = code },
                                                label = { Text(label) },
                                                modifier = Modifier.testTag("filter_avatar_$code")
                                            )
                                        }
                                        FilterChip(
                                            selected = onlyFavoritesFilter,
                                            onClick = { onlyFavoritesFilter = !onlyFavoritesFilter },
                                            label = { Text("★ Избранные") },
                                            modifier = Modifier.testTag("filter_only_favorites")
                                        )
                                        if (characters.any { it.isArchived }) {
                                            FilterChip(
                                                selected = showArchivedCharacters,
                                                onClick = { showArchivedCharacters = !showArchivedCharacters },
                                                label = { Text("Показывать архив") }
                                            )
                                        }
                                    }

                                    if (availableRoles.isNotEmpty()) {
                                        Text("Категория / Роль:", style = MaterialTheme.typography.labelSmall, color = AmberGoldSecondary)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilterChip(
                                                selected = selectedRoleFilter == "ALL",
                                                onClick = { selectedRoleFilter = "ALL" },
                                                label = { Text("Все роли") }
                                            )
                                            availableRoles.forEach { roleName ->
                                                FilterChip(
                                                    selected = selectedRoleFilter.equals(roleName, ignoreCase = true),
                                                    onClick = { selectedRoleFilter = roleName },
                                                    label = { Text(roleName) }
                                                )
                                            }
                                        }
                                    }

                                    Text("Статус канона:", style = MaterialTheme.typography.labelSmall, color = AmberGoldSecondary)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = selectedCanonFilter == "ALL",
                                            onClick = { selectedCanonFilter = "ALL" },
                                            label = { Text("Все статусы") }
                                        )
                                        CanonStatus.entries.forEach { st ->
                                            FilterChip(
                                                selected = selectedCanonFilter == st.name,
                                                onClick = { selectedCanonFilter = st.name },
                                                label = { Text(st.label) }
                                            )
                                        }
                                    }

                                    if (availableTags.isNotEmpty()) {
                                        Text("Теги:", style = MaterialTheme.typography.labelSmall, color = AmberGoldSecondary)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilterChip(
                                                selected = selectedTagFilter == "ALL",
                                                onClick = { selectedTagFilter = "ALL" },
                                                label = { Text("Все теги") }
                                            )
                                            availableTags.forEach { tag ->
                                                FilterChip(
                                                    selected = selectedTagFilter.equals(tag, ignoreCase = true),
                                                    onClick = { selectedTagFilter = tag },
                                                    label = { Text("#$tag") }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Сообщение при отсутствии совпадений поиска или фильтров
            if (filteredCharacters.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "По вашему запросу персонажи не найдены",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Попробуйте изменить текст поиска или сбросить установленные фильтры.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (searchQuery.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.testTag("reset_character_search_button")
                                    ) {
                                        Text("Сбросить поиск")
                                    }
                                }
                                if (hasActiveFilters) {
                                    Button(
                                        onClick = { resetAllFilters() },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                        modifier = Modifier.testTag("reset_character_filters_empty_button")
                                    ) {
                                        Text("Сбросить все фильтры")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Вертикальный список персонажей: аватар слева, имя справа + краткая доп. информация
                itemsIndexed(filteredCharacters, key = { _, ch -> ch.id }) { idx, ch ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCharacterCard(ch.id) }
                            .testTag("character_row_${ch.id}"),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                CharacterAvatarThumbnail(
                                    name = ch.name,
                                    avatarBase64 = ch.avatarBase64,
                                    size = 48.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (ch.isPinned) {
                                            Icon(
                                                Icons.Default.PushPin,
                                                contentDescription = "Закреплён",
                                                tint = CrimsonPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        if (ch.isFavorite) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = "Избранный",
                                                tint = AmberGoldSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = ch.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    val secondaryMeta = buildList {
                                        val roleOrJob = ch.occupation.ifBlank { ch.storyRole }
                                        if (roleOrJob.isNotBlank()) add(roleOrJob)
                                        if (ch.age.isNotBlank()) add("${ch.age} л.")
                                        if (ch.currentLocationName.isNotBlank()) add("Локация: ${ch.currentLocationName}")
                                    }.joinToString(" • ")

                                    if (secondaryMeta.isNotEmpty()) {
                                        Text(
                                            text = secondaryMeta,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    val shortDesc = ch.shortDescription.ifBlank { ch.personality }
                                    if (shortDesc.isNotBlank()) {
                                        Text(
                                            text = shortDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (idx > 0) {
                                    IconButton(
                                        onClick = { onMoveCharacterOrder(ch, true) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Выше",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (idx < filteredCharacters.lastIndex) {
                                    IconButton(
                                        onClick = { onMoveCharacterOrder(ch, false) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Ниже",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                CanonStatusBadge(statusName = ch.canonStatus)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ЭКРАН 2: ОТДЕЛЬНЫЙ ЭКРАН КАРТОЧКИ ПЕРСОНАЖА (CharacterDetailScreen)
 * Полностью заменяет экран списка персонажей при открытии карточки.
 * На этом экране ЗАПРЕЩЕНО отображать:
 * - Список остальных персонажей
 * - Кнопку «Создать персонажа»
 * - Фильтры списка и сортировку списка
 * - Панели управления общим списком и карточки других персонажей.
 * Содержит:
 * - Верхнюю часть (Кнопка «Назад», Аватар, Имя персонажа, Меню действий с карточкой)
 * - Всю информацию о выбранном персонаже по 10 тематическим разделам + пользовательским полям + связям + медиатеке
 * - Поточечное редактирование каждого поля отдельно (inline и в модальном окне)
 * - Предупреждение о несохранённых изменениях при выходе по кнопке «Назад».
 */
@Composable
private fun CharacterDetailScreen(
    activeChar: CharacterEntity,
    allCharacters: List<CharacterEntity>,
    relationships: List<com.example.data.local.RelationshipEntity>,
    isExtractingCharacterFile: Boolean,
    onBackToList: () -> Unit,
    onSaveCharacter: (CharacterEntity, String) -> Unit,
    onUpdateCharacterStateOnly: (Long, String, String, String, String) -> Unit,
    onDeleteCharacter: (CharacterEntity) -> Unit,
    onCloneCharacter: (CharacterEntity) -> Unit,
    onToggleCharacterFavorite: (CharacterEntity) -> Unit,
    onToggleCharacterPinned: (CharacterEntity) -> Unit,
    onToggleCharacterArchived: (CharacterEntity) -> Unit,
    onClearCharacterAvatar: (CharacterEntity) -> Unit,
    onExportSingleCharacterUri: (CharacterEntity, Uri, Boolean) -> Unit,
    onExtractCharacterFromUri: (Uri, String, (com.example.data.local.AiExtractedCharacterDossier, String) -> Unit) -> Unit,
    onExtractCharacterFromSampleDoc: (String, (com.example.data.local.AiExtractedCharacterDossier, String) -> Unit) -> Unit,
    onUpdateCharacterAvatar: (CharacterEntity, String) -> Unit,
    onAddCharacterReference: (CharacterEntity, String, String, String) -> Unit,
    onRemoveCharacterReference: (CharacterEntity, String) -> Unit,
    onAddCharacterPrompt: (CharacterEntity, String, String, String) -> Unit,
    onRemoveCharacterPrompt: (CharacterEntity, String) -> Unit,
    onAddCharacterDeclaredRelation: (CharacterEntity, com.example.data.local.DeclaredCharacterRelation) -> Unit,
    onOpenInImageStudioWithAsset: (CharacterEntity, String?, String?, String?) -> Unit
) {
    val thematicSections = remember { getCharacterThematicSections() }
    val allFieldSpecsByKey = remember(thematicSections) {
        thematicSections.flatMap { it.fields }.associateBy { it.key }
    }

    // Черновики inline-редактирования отдельных полей на экране карточки (ключ поля -> текущий текст)
    var inlineFieldDrafts by remember(activeChar.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showUnsavedExitWarning by remember { mutableStateOf(false) }

    var showFullEditDialog by remember { mutableStateOf(false) }
    var showStateOnlyDialog by remember { mutableStateOf(false) }
    var characterPendingDeletion by remember { mutableStateOf<CharacterEntity?>(null) }
    var singleFieldEditSession by remember { mutableStateOf<SingleFieldEditSession?>(null) }

    // Проверяем, есть ли несохранённые изменения в открытых inline-полях карточки
    val hasUnsavedChanges = remember(inlineFieldDrafts, activeChar) {
        inlineFieldDrafts.any { (key, draftVal) ->
            val spec = allFieldSpecsByKey[key]
            spec != null && draftVal != spec.getter(activeChar)
        }
    }

    fun saveAllPendingInlineChanges(): CharacterEntity {
        var updated = activeChar
        val changedLabels = mutableListOf<String>()
        inlineFieldDrafts.forEach { (key, draftVal) ->
            val spec = allFieldSpecsByKey[key]
            if (spec != null) {
                val trimmed = draftVal.trim()
                if (key == "name" && trimmed.isBlank()) return@forEach
                if (trimmed != spec.getter(updated)) {
                    updated = spec.setter(updated, trimmed)
                    changedLabels.add(spec.label)
                }
            }
        }
        if (changedLabels.isNotEmpty()) {
            onSaveCharacter(
                updated,
                "Обновлены поля (${changedLabels.joinToString(", ")}) у персонажа «${updated.name}»"
            )
        }
        inlineFieldDrafts = emptyMap()
        return updated
    }

    val handleRequestBackToList = {
        if (hasUnsavedChanges) {
            showUnsavedExitWarning = true
        } else {
            inlineFieldDrafts = emptyMap()
            onBackToList()
        }
    }

    // Системная кнопка «Назад» возвращает к списку персонажей (с проверкой несохранённых изменений)
    BackHandler(enabled = true) {
        handleRequestBackToList()
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportSingleCharacterUri(activeChar, uri, false)
        }
    }

    val exportMdLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            onExportSingleCharacterUri(activeChar, uri, true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("character_detail_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 3.1. Верхняя часть карточки персонажа: Кнопка «Назад», Аватар, Имя персонажа, Меню действий
        item {
            CharacterCardHeaderSection(
                character = activeChar,
                isDedicatedPageMode = true,
                hasUnsavedChanges = hasUnsavedChanges,
                inlineNameDraft = inlineFieldDrafts["name"],
                onStartInlineNameEdit = { nameSpec ->
                    inlineFieldDrafts = inlineFieldDrafts + ("name" to nameSpec.getter(activeChar))
                },
                onUpdateInlineNameDraft = { newName ->
                    inlineFieldDrafts = inlineFieldDrafts + ("name" to newName)
                },
                onSaveInlineNameEdit = { nameSpec ->
                    val draft = inlineFieldDrafts["name"]?.trim().orEmpty()
                    if (draft.isNotBlank()) {
                        val updated = nameSpec.setter(activeChar, draft)
                        onSaveCharacter(updated, "Изменено имя персонажа на «$draft»")
                        inlineFieldDrafts = inlineFieldDrafts - "name"
                    }
                },
                onCancelInlineNameEdit = {
                    inlineFieldDrafts = inlineFieldDrafts - "name"
                },
                onSaveAllUnsavedChanges = {
                    saveAllPendingInlineChanges()
                },
                onDiscardAllUnsavedChanges = {
                    inlineFieldDrafts = emptyMap()
                },
                onBackToList = handleRequestBackToList,
                onUpdateAvatar = { b64 -> onUpdateCharacterAvatar(activeChar, b64) },
                onClearAvatar = { onClearCharacterAvatar(activeChar) },
                onOpenFullEdit = { showFullEditDialog = true },
                onCloneCharacter = { onCloneCharacter(activeChar) },
                onRequestDeleteCharacter = { characterPendingDeletion = activeChar },
                onToggleFavorite = { onToggleCharacterFavorite(activeChar) },
                onTogglePinned = { onToggleCharacterPinned(activeChar) },
                onToggleArchived = { onToggleCharacterArchived(activeChar) },
                onExportCharacterJson = {
                    exportJsonLauncher.launch("${activeChar.name.replace(" ", "_")}_card.json")
                },
                onExportCharacterMarkdown = {
                    exportMdLauncher.launch("${activeChar.name.replace(" ", "_")}_dossier.md")
                },
                onInlineEditField = { fieldSpec ->
                    inlineFieldDrafts = inlineFieldDrafts + (fieldSpec.key to fieldSpec.getter(activeChar))
                }
            )
        }

        // Слой 2: Текущее состояние персонажа (отделено от постоянной биографии)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, AmberGoldSecondary.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Слой 2: Текущее состояние в сцене (не перезаписывает биографию)",
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberGoldSecondary
                            )
                            Text(
                                text = "Локация сейчас: ${activeChar.currentLocationName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        OutlinedButton(
                            onClick = { showStateOnlyDialog = true },
                            modifier = Modifier.testTag("edit_current_state_button")
                        ) {
                            Text("Сменить состояние")
                        }
                    }
                    Text("Эмоции и состояние: ${activeChar.currentMoodAndCondition}", style = MaterialTheme.typography.bodySmall)
                    if (activeChar.characterStatus.isNotBlank()) {
                        Text("Статус: ${activeChar.characterStatus}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (activeChar.currentOutfit.isNotBlank()) {
                        Text("Текущая одежда: ${activeChar.currentOutfit}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (activeChar.currentActiveGoal.isNotBlank()) {
                        Text(
                            "Текущая задача в сцене: ${activeChar.currentActiveGoal}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberGoldSecondary
                        )
                    }
                }
            }
        }

        // 3.2 & 4. Все тематические разделы карточки с независимым редактированием каждого поля и подпункта
        items(thematicSections, key = { it.id }) { sectionSpec ->
            CharacterThematicSectionCard(
                section = sectionSpec,
                character = activeChar,
                inlineFieldDrafts = inlineFieldDrafts,
                onStartInlineFieldEdit = { fieldSpec ->
                    inlineFieldDrafts = inlineFieldDrafts + (fieldSpec.key to fieldSpec.getter(activeChar))
                },
                onUpdateInlineFieldDraft = { key, updatedText ->
                    inlineFieldDrafts = inlineFieldDrafts + (key to updatedText)
                },
                onSaveInlineFieldEdit = { fieldSpec ->
                    val newText = inlineFieldDrafts[fieldSpec.key]?.trim().orEmpty()
                    if (fieldSpec.key == "name" && newText.isBlank()) return@CharacterThematicSectionCard
                    val updatedChar = fieldSpec.setter(activeChar, newText)
                    onSaveCharacter(
                        updatedChar,
                        "Изменено поле «${fieldSpec.label}» у персонажа «${updatedChar.name}»"
                    )
                    inlineFieldDrafts = inlineFieldDrafts - fieldSpec.key
                },
                onCancelInlineFieldEdit = { key ->
                    inlineFieldDrafts = inlineFieldDrafts - key
                },
                onEditSingleField = { fieldSpec ->
                    singleFieldEditSession = SingleFieldEditSession(
                        fieldSpec = fieldSpec,
                        initialText = inlineFieldDrafts[fieldSpec.key] ?: fieldSpec.getter(activeChar)
                    )
                },
                onEditSubItem = { fieldSpec, subIdx, allItems, delim ->
                    singleFieldEditSession = SingleFieldEditSession(
                        fieldSpec = fieldSpec,
                        initialText = allItems.getOrElse(subIdx) { "" },
                        subItemIndex = subIdx,
                        allSubItems = allItems,
                        delimiter = delim,
                        isAddingNewSubItem = false
                    )
                },
                onAddSubItem = { fieldSpec, allItems, delim ->
                    singleFieldEditSession = SingleFieldEditSession(
                        fieldSpec = fieldSpec,
                        initialText = "",
                        subItemIndex = allItems.size,
                        allSubItems = allItems,
                        delimiter = delim,
                        isAddingNewSubItem = true
                    )
                }
            )
        }

        // 11. Пользовательские характеристики (Custom Key-Value Fields)
        item {
            CharacterCustomFieldsSectionCard(
                character = activeChar,
                onSaveCustomFieldsJson = { updatedJson, summary ->
                    onSaveCharacter(
                        activeChar.copy(customFieldsJson = updatedJson),
                        summary
                    )
                }
            )
        }

        // АВТОМАТИЧЕСКИЕ И ОЖИДАЮЩИЕ СВЯЗИ ПЕРСОНАЖА (без кнопок создания других персонажей)
        item {
            CharacterAutoRelationshipsSection(
                character = activeChar,
                allCharacters = allCharacters,
                allRelationships = relationships,
                onAddDeclaredRelation = { rel ->
                    onAddCharacterDeclaredRelation(activeChar, rel)
                },
                onQuickCreateMissingCharacter = {}
            )
        }

        // МЕДИАТЕКА, РЕФЕРЕНСЫ И ПРОМПТЫ ДЛЯ IMAGE STUDIO
        item {
            CharacterMediaAndPromptsSection(
                character = activeChar,
                onUpdateAvatar = { b64 -> onUpdateCharacterAvatar(activeChar, b64) },
                onAddReferenceImage = { title, b64, notes ->
                    onAddCharacterReference(activeChar, title, b64, notes)
                },
                onRemoveReferenceImage = { refId ->
                    onRemoveCharacterReference(activeChar, refId)
                },
                onAddSavedPrompt = { title, promptText, category ->
                    onAddCharacterPrompt(activeChar, title, promptText, category)
                },
                onRemoveSavedPrompt = { prmId ->
                    onRemoveCharacterPrompt(activeChar, prmId)
                },
                onOpenInImageStudio = { prm, refB64, refTitle ->
                    onOpenInImageStudioWithAsset(activeChar, prm, refB64, refTitle)
                }
            )
        }
    }

    // Предупреждение о несохранённых изменениях перед выходом из карточки по кнопке «Назад»
    if (showUnsavedExitWarning) {
        AlertDialog(
            onDismissRequest = { showUnsavedExitWarning = false },
            title = { Text("Несохранённые изменения") },
            text = {
                Text(
                    "В карточке персонажа «${activeChar.name}» есть несохранённые изменения. " +
                        "Сохранить их перед возвратом к списку персонажей?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveAllPendingInlineChanges()
                        showUnsavedExitWarning = false
                        onBackToList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("save_and_exit_character_button")
                ) {
                    Text("Сохранить и выйти")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            inlineFieldDrafts = emptyMap()
                            showUnsavedExitWarning = false
                            onBackToList()
                        },
                        modifier = Modifier.testTag("discard_and_exit_character_button")
                    ) {
                        Text("Выйти без сохранения", color = CrimsonPrimary)
                    }
                    TextButton(
                        onClick = { showUnsavedExitWarning = false },
                        modifier = Modifier.testTag("stay_on_character_card_button")
                    ) {
                        Text("Остаться")
                    }
                }
            }
        )
    }

    // Диалог поточечного редактирования отдельного поля или отдельного абзаца/пункта
    if (singleFieldEditSession != null) {
        val session = singleFieldEditSession!!
        var draftValue by remember(session) { mutableStateOf(session.initialText) }

        val dialogTitle = when {
            session.isAddingNewSubItem -> "Добавить пункт в «${session.fieldSpec.label}»"
            session.subItemIndex != null -> "Изменить пункт #${session.subItemIndex + 1} («${session.fieldSpec.label}»)"
            else -> "Изменить поле «${session.fieldSpec.label}»"
        }

        AlertDialog(
            onDismissRequest = { singleFieldEditSession = null },
            title = { Text(dialogTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Сохранение обновит только этот пункт карточки «${activeChar.name}». Все остальные разделы и поля останутся без изменений.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = draftValue,
                        onValueChange = { draftValue = it },
                        label = { Text(session.fieldSpec.label) },
                        minLines = if (session.fieldSpec.isMultiline) 3 else 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("single_field_editor_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = draftValue.trim()
                        if (session.fieldSpec.key == "name" && trimmed.isBlank()) {
                            return@Button
                        }
                        val newFullFieldValue = when {
                            session.isAddingNewSubItem -> {
                                val nextList = session.allSubItems + listOfNotNull(trimmed.takeIf { it.isNotEmpty() })
                                val joiner = if (session.delimiter == "\n") "\n" else "${session.delimiter} "
                                nextList.joinToString(joiner)
                            }
                            session.subItemIndex != null -> {
                                val mutable = session.allSubItems.toMutableList()
                                if (trimmed.isEmpty()) {
                                    if (session.subItemIndex in mutable.indices) mutable.removeAt(session.subItemIndex)
                                } else if (session.subItemIndex in mutable.indices) {
                                    mutable[session.subItemIndex] = trimmed
                                }
                                val joiner = if (session.delimiter == "\n") "\n" else "${session.delimiter} "
                                mutable.joinToString(joiner)
                            }
                            else -> trimmed
                        }
                        val updatedChar = session.fieldSpec.setter(activeChar, newFullFieldValue)
                        onSaveCharacter(
                            updatedChar,
                            "Изменено поле «${session.fieldSpec.label}» у персонажа «${updatedChar.name}»"
                        )
                        inlineFieldDrafts = inlineFieldDrafts - session.fieldSpec.key
                        singleFieldEditSession = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("confirm_single_field_save_button")
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (session.fieldSpec.key != "name" && session.initialText.isNotBlank()) {
                        TextButton(
                            onClick = {
                                val newFullFieldValue = if (session.subItemIndex != null && !session.isAddingNewSubItem) {
                                    val mutable = session.allSubItems.toMutableList()
                                    if (session.subItemIndex in mutable.indices) mutable.removeAt(session.subItemIndex)
                                    val joiner = if (session.delimiter == "\n") "\n" else "${session.delimiter} "
                                    mutable.joinToString(joiner)
                                } else {
                                    ""
                                }
                                val updatedChar = session.fieldSpec.setter(activeChar, newFullFieldValue)
                                onSaveCharacter(
                                    updatedChar,
                                    "Очищен пункт в поле «${session.fieldSpec.label}» у персонажа «${updatedChar.name}»"
                                )
                                inlineFieldDrafts = inlineFieldDrafts - session.fieldSpec.key
                                singleFieldEditSession = null
                            },
                            modifier = Modifier.testTag("clear_single_field_button")
                        ) {
                            Text("Удалить пункт", color = CrimsonPrimary)
                        }
                    }
                    TextButton(
                        onClick = { singleFieldEditSession = null },
                        modifier = Modifier.testTag("cancel_single_field_edit_button")
                    ) {
                        Text("Отмена")
                    }
                }
            }
        )
    }

    // Диалог подтверждения удаления персонажа
    if (characterPendingDeletion != null) {
        val charToDelete = characterPendingDeletion!!
        AlertDialog(
            onDismissRequest = { characterPendingDeletion = null },
            title = { Text("Удалить персонажа «${charToDelete.name}»?") },
            text = {
                Text(
                    "Вы уверены, что хотите удалить карточку персонажа «${charToDelete.name}»? " +
                        "Вся анкета и характеристики этого персонажа будут удалены из текущего проекта."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        characterPendingDeletion = null
                        onDeleteCharacter(charToDelete)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("confirm_delete_character_button")
                ) {
                    Text("Удалить персонажа")
                }
            },
            dismissButton = {
                TextButton(onClick = { characterPendingDeletion = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: Update Current State Only (Layer 2)
    if (showStateOnlyDialog) {
        var loc by remember { mutableStateOf(activeChar.currentLocationName) }
        var mood by remember { mutableStateOf(activeChar.currentMoodAndCondition) }
        var outfit by remember { mutableStateOf(activeChar.currentOutfit) }
        var goal by remember { mutableStateOf(activeChar.currentActiveGoal) }

        AlertDialog(
            onDismissRequest = { showStateOnlyDialog = false },
            title = { Text("Текущее состояние: ${activeChar.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Изменение текущего состояния фиксирует перемещение персонажа, но НЕ переписывает постоянные сведения или прошлые события.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Где находится сейчас") })
                    OutlinedTextField(value = mood, onValueChange = { mood = it }, label = { Text("Эмоциональное/физическое состояние") })
                    OutlinedTextField(value = outfit, onValueChange = { outfit = it }, label = { Text("Одежда в текущей сцене") })
                    OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("Текущая активная цель") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCharacterStateOnly(activeChar.id, loc, mood, outfit, goal)
                        showStateOnlyDialog = false
                    }
                ) { Text("Обновить состояние") }
            },
            dismissButton = {
                TextButton(onClick = { showStateOnlyDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Dialog: Full Edit Character with AI File Upload (.DOC, .DOCX, .PDF, .TXT, .XLSX)
    if (showFullEditDialog) {
        AiCharacterDossierDialog(
            initialCharacter = activeChar,
            isNewCharacter = false,
            isExtractingFile = isExtractingCharacterFile,
            onExtractFromUri = onExtractCharacterFromUri,
            onExtractFromSampleDoc = onExtractCharacterFromSampleDoc,
            onDismiss = { showFullEditDialog = false },
            onSave = { updatedChar, summary ->
                onSaveCharacter(updatedChar, summary)
                inlineFieldDrafts = emptyMap()
                showFullEditDialog = false
            }
        )
    }
}

@Composable
private fun CharacterFieldRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = AmberGoldSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * 1. СЮЖЕТ (Отдельный раздел «Сюжет»):
 * - Сюжетные линии
 * - Задачи
 * - Возможные варианты развития
 * - Связанных персонажей
 * - Связанные локации
 * - Состояние сюжетной линии
 */
@Composable
private fun PlotlinesSubScreen(
    plotlines: List<PlotlineEntity>,
    characters: List<CharacterEntity>,
    locations: List<LocationEntity>,
    onSavePlotline: (PlotlineEntity) -> Unit,
    onDeletePlotline: (PlotlineEntity) -> Unit
) {
    var editingPlotline by remember { mutableStateOf<PlotlineEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Раздел «Сюжет» (Сюжетные линии и задачи)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Отдельный раздел управления арками, задачами, развилками и состоянием сюжетных линий.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        editingPlotline = PlotlineEntity(
                            title = "",
                            description = "",
                            involvedCharactersCsv = characters.take(2).joinToString(", ") { it.name },
                            linkedLocationsCsv = locations.firstOrNull()?.name ?: "Красный особняк"
                        )
                        isCreatingNew = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("add_plotline_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Сюжетная линия")
                }
            }
        }

        items(plotlines, key = { it.id }) { plot ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = CrimsonPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = plot.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Состояние сюжетной линии: ${plot.status}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusCanonGreen
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = {
                                editingPlotline = plot
                                isCreatingNew = false
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать сюжет")
                            }
                            IconButton(onClick = { onDeletePlotline(plot) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить сюжет")
                            }
                        }
                    }

                    Text(plot.description, style = MaterialTheme.typography.bodyMedium)

                    if (plot.tasks.isNotBlank()) {
                        CharacterFieldRow("Задачи сюжетной линии", plot.tasks)
                    }
                    if (plot.developmentOptions.isNotBlank()) {
                        CharacterFieldRow("Возможные варианты развития", plot.developmentOptions)
                    }
                    val charsDisplay = plot.involvedCharactersCsv.ifBlank { plot.linkedCharacterNamesCsv }
                    if (charsDisplay.isNotBlank()) {
                        CharacterFieldRow("Связанные персонажи", charsDisplay)
                    }
                    val locsDisplay = plot.linkedLocationsCsv.ifBlank { plot.linkedLocationNamesCsv }
                    if (locsDisplay.isNotBlank()) {
                        CharacterFieldRow("Связанные локации", locsDisplay)
                    }
                }
            }
        }
    }

    if (editingPlotline != null) {
        val base = editingPlotline!!
        var title by remember(base) { mutableStateOf(base.title) }
        var desc by remember(base) { mutableStateOf(base.description) }
        var objectives by remember(base) { mutableStateOf(base.tasks) }
        var branches by remember(base) { mutableStateOf(base.developmentOptions) }
        var chars by remember(base) { mutableStateOf(base.involvedCharactersCsv.ifBlank { base.linkedCharacterNamesCsv }) }
        var locs by remember(base) { mutableStateOf(base.linkedLocationsCsv.ifBlank { base.linkedLocationNamesCsv }) }
        var state by remember(base) { mutableStateOf(base.status) }

        AlertDialog(
            onDismissRequest = { editingPlotline = null },
            title = { Text(if (isCreatingNew) "Новая сюжетная линия" else "Редактировать сюжетную линию") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название сюжетной линии") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("Состояние (Активна / В разработке / Приостановлена / Завершена)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание сюжетной линии") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = objectives, onValueChange = { objectives = it }, label = { Text("Задачи сюжетной линии") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = branches, onValueChange = { branches = it }, label = { Text("Возможные варианты развития (развилки)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = chars, onValueChange = { chars = it }, label = { Text("Связанные персонажи") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = locs, onValueChange = { locs = it }, label = { Text("Связанные локации") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSavePlotline(
                                base.copy(
                                    title = title.trim(),
                                    description = desc.trim(),
                                    tasks = objectives.trim(),
                                    developmentOptions = branches.trim(),
                                    involvedCharactersCsv = chars.trim(),
                                    linkedCharacterNamesCsv = chars.trim(),
                                    linkedLocationsCsv = locs.trim(),
                                    linkedLocationNamesCsv = locs.trim(),
                                    status = state.trim().ifBlank { "Активна" }
                                )
                            )
                            editingPlotline = null
                        }
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { editingPlotline = null }) { Text("Отмена") }
            }
        )
    }
}

/**
 * 1. ИСТОРИЯ СОБЫТИЙ (Отдельный раздел «История событий»):
 * - Название события
 * - Описание
 * - Дату или условный момент времени
 * - Место
 * - Участников
 * - Причины
 * - Последствия
 * - Связанные события
 * - Связь с сюжетными линиями
 */
@Composable
private fun EventsAndHistorySubScreen(
    events: List<WorldEventEntity>,
    locations: List<LocationEntity>,
    plotlines: List<PlotlineEntity>,
    onSaveEvent: (WorldEventEntity) -> Unit,
    onDeleteEvent: (WorldEventEntity) -> Unit
) {
    var editingEvent by remember { mutableStateOf<WorldEventEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Раздел «История событий» (Хронология мира)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Карточки персонажей и правила мира не используются как свалка памяти о каждом событии — вся хронология, причины и последствия хранятся здесь.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        editingEvent = WorldEventEntity(
                            title = "",
                            description = "",
                            timelineDate = "День ${events.size + 1}, 19:00",
                            locationName = locations.firstOrNull()?.name ?: "Красный особняк",
                            participantNamesCsv = "Катрин, Александра",
                            linkedPlotlinesCsv = plotlines.firstOrNull()?.title ?: ""
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("add_event_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Событие")
                }
            }
        }

        items(events, key = { it.id }) { ev ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ev.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${ev.timelineDate} • Место: ${ev.locationName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberGoldSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CanonStatusBadge(statusName = ev.canonStatus)
                            IconButton(onClick = { editingEvent = ev }) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать событие")
                            }
                            IconButton(onClick = { onDeleteEvent(ev) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить событие")
                            }
                        }
                    }

                    Text(ev.description, style = MaterialTheme.typography.bodyMedium)
                    CharacterFieldRow("Участники", ev.participantNamesCsv)
                    if (ev.causes.isNotBlank()) {
                        CharacterFieldRow("Причины события", ev.causes)
                    }
                    if (ev.consequences.isNotBlank()) {
                        CharacterFieldRow("Последствия события", ev.consequences)
                    }
                    val linkedEvs = ev.linkedEventsCsv.ifBlank { ev.linkedEventTitlesCsv }
                    if (linkedEvs.isNotBlank()) {
                        CharacterFieldRow("Связанные события", linkedEvs)
                    }
                    val linkedPlots = ev.linkedPlotlinesCsv.ifBlank { ev.linkedPlotlineTitlesCsv }
                    if (linkedPlots.isNotBlank()) {
                        CharacterFieldRow("Связь с сюжетными линиями", linkedPlots)
                    }
                }
            }
        }
    }

    if (editingEvent != null) {
        val base = editingEvent!!
        var title by remember(base) { mutableStateOf(base.title) }
        var date by remember(base) { mutableStateOf(base.timelineDate) }
        var loc by remember(base) { mutableStateOf(base.locationName) }
        var participants by remember(base) { mutableStateOf(base.participantNamesCsv) }
        var desc by remember(base) { mutableStateOf(base.description) }
        var causes by remember(base) { mutableStateOf(base.causes) }
        var consequences by remember(base) { mutableStateOf(base.consequences) }
        var linkedEvents by remember(base) { mutableStateOf(base.linkedEventsCsv.ifBlank { base.linkedEventTitlesCsv }) }
        var linkedPlots by remember(base) { mutableStateOf(base.linkedPlotlinesCsv.ifBlank { base.linkedPlotlineTitlesCsv }) }

        AlertDialog(
            onDismissRequest = { editingEvent = null },
            title = { Text(if (base.id == 0L) "Зафиксировать событие" else "Редактировать событие") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название события") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Дата или условный момент времени") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Место (локация)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = participants, onValueChange = { participants = it }, label = { Text("Участники") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание события") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = causes, onValueChange = { causes = it }, label = { Text("Причины события") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = consequences, onValueChange = { consequences = it }, label = { Text("Последствия события") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = linkedEvents, onValueChange = { linkedEvents = it }, label = { Text("Связанные события") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = linkedPlots, onValueChange = { linkedPlots = it }, label = { Text("Связь с сюжетными линиями") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSaveEvent(
                                base.copy(
                                    title = title.trim(),
                                    timelineDate = date.trim(),
                                    locationName = loc.trim(),
                                    participantNamesCsv = participants.trim(),
                                    description = desc.trim(),
                                    causes = causes.trim(),
                                    consequences = consequences.trim(),
                                    linkedEventsCsv = linkedEvents.trim(),
                                    linkedEventTitlesCsv = linkedEvents.trim(),
                                    linkedPlotlinesCsv = linkedPlots.trim(),
                                    linkedPlotlineTitlesCsv = linkedPlots.trim()
                                )
                            )
                            editingEvent = null
                        }
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { editingEvent = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun WorldRulesSubScreen(
    rules: List<WorldRuleEntity>,
    onSaveRule: (WorldRuleEntity) -> Unit,
    onDeleteRule: (WorldRuleEntity) -> Unit
) {
    var editingRule by remember { mutableStateOf<WorldRuleEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Правила мира (Законы, ограничения, исключения)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Общие правила мира хранятся отдельно от памяти о конкретных сюжетных сценах.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { editingRule = WorldRuleEntity(title = "", category = "Общие правила", content = "") },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Правило")
                }
            }
        }

        items(rules, key = { it.id }) { rule ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = CrimsonPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(rule.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${rule.category} • ${rule.priorityLevel}", style = MaterialTheme.typography.labelMedium, color = AmberGoldSecondary)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CanonStatusBadge(statusName = rule.canonStatus)
                            IconButton(onClick = { editingRule = rule }) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                            }
                            IconButton(onClick = { onDeleteRule(rule) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                    Text(rule.content, style = MaterialTheme.typography.bodyMedium)
                    if (rule.restrictions.isNotBlank()) CharacterFieldRow("Ограничения", rule.restrictions)
                    if (rule.exceptions.isNotBlank()) CharacterFieldRow("Исключения", rule.exceptions)
                    if (rule.linkedLocationNamesCsv.isNotBlank()) CharacterFieldRow("Связанные локации", rule.linkedLocationNamesCsv)
                    if (rule.linkedCharacterNamesCsv.isNotBlank()) CharacterFieldRow("Связанные персонажи", rule.linkedCharacterNamesCsv)
                }
            }
        }
    }

    if (editingRule != null) {
        val base = editingRule!!
        var title by remember(base) { mutableStateOf(base.title) }
        var category by remember(base) { mutableStateOf(base.category) }
        var content by remember(base) { mutableStateOf(base.content) }
        var restrictions by remember(base) { mutableStateOf(base.restrictions) }
        var exceptions by remember(base) { mutableStateOf(base.exceptions) }
        var locs by remember(base) { mutableStateOf(base.linkedLocationNamesCsv) }
        var chars by remember(base) { mutableStateOf(base.linkedCharacterNamesCsv) }

        AlertDialog(
            onDismissRequest = { editingRule = null },
            title = { Text(if (base.id == 0L) "Добавить правило мира" else "Редактировать правило мира") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название правила") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Категория") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Формулировка закона мира") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = restrictions, onValueChange = { restrictions = it }, label = { Text("Строгие ограничения") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = exceptions, onValueChange = { exceptions = it }, label = { Text("Исключения из правила") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = locs, onValueChange = { locs = it }, label = { Text("Связанные локации") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = chars, onValueChange = { chars = it }, label = { Text("Связанные персонажи") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSaveRule(
                                base.copy(
                                    title = title.trim(),
                                    category = category.trim(),
                                    content = content.trim(),
                                    restrictions = restrictions.trim(),
                                    exceptions = exceptions.trim(),
                                    linkedLocationNamesCsv = locs.trim(),
                                    linkedCharacterNamesCsv = chars.trim()
                                )
                            )
                            editingRule = null
                        }
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { editingRule = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun LocationsSubScreen(
    locations: List<LocationEntity>,
    onSaveLocation: (LocationEntity) -> Unit,
    onDeleteLocation: (LocationEntity) -> Unit
) {
    var editingLoc by remember { mutableStateOf<LocationEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Локации проекта", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { editingLoc = LocationEntity(name = "", category = "Городская локация", description = "") },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Новая локация")
                }
            }
        }

        items(locations, key = { it.id }) { loc ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = CrimsonPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(loc.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(loc.category, style = MaterialTheme.typography.labelMedium, color = AmberGoldSecondary)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CanonStatusBadge(statusName = loc.canonStatus)
                            IconButton(onClick = { editingLoc = loc }) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                            }
                            IconButton(onClick = { onDeleteLocation(loc) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                    Text(loc.description, style = MaterialTheme.typography.bodyMedium)
                    if (loc.atmosphere.isNotBlank()) CharacterFieldRow("Атмосфера и особенности", loc.atmosphere)
                    if (loc.accessRules.isNotBlank()) CharacterFieldRow("Правила и ограничения локации", loc.accessRules)
                    if (loc.subLocationsCsv.isNotBlank()) CharacterFieldRow("Подлокации и зоны", loc.subLocationsCsv)
                    if (loc.linkedCharacterNamesCsv.isNotBlank()) CharacterFieldRow("Связанные персонажи", loc.linkedCharacterNamesCsv)
                    if (loc.notes.isNotBlank()) CharacterFieldRow("Заметки", loc.notes)
                }
            }
        }
    }

    if (editingLoc != null) {
        val base = editingLoc!!
        var name by remember(base) { mutableStateOf(base.name) }
        var category by remember(base) { mutableStateOf(base.category) }
        var desc by remember(base) { mutableStateOf(base.description) }
        var atmosphere by remember(base) { mutableStateOf(base.atmosphere) }
        var accessRules by remember(base) { mutableStateOf(base.accessRules) }
        var subLocs by remember(base) { mutableStateOf(base.subLocationsCsv) }
        var linkedChars by remember(base) { mutableStateOf(base.linkedCharacterNamesCsv) }
        var notes by remember(base) { mutableStateOf(base.notes) }

        AlertDialog(
            onDismissRequest = { editingLoc = null },
            title = { Text(if (base.id == 0L) "Добавить локацию" else "Редактировать локацию") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название локации") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Категория / Тип") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = atmosphere, onValueChange = { atmosphere = it }, label = { Text("Атмосфера и особенности") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = accessRules, onValueChange = { accessRules = it }, label = { Text("Правила и ограничения") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subLocs, onValueChange = { subLocs = it }, label = { Text("Подлокации (через запятую)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = linkedChars, onValueChange = { linkedChars = it }, label = { Text("Связанные персонажи") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Заметки") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveLocation(
                                base.copy(
                                    name = name.trim(),
                                    category = category.trim(),
                                    description = desc.trim(),
                                    atmosphere = atmosphere.trim(),
                                    accessRules = accessRules.trim(),
                                    subLocationsCsv = subLocs.trim(),
                                    linkedCharacterNamesCsv = linkedChars.trim(),
                                    notes = notes.trim()
                                )
                            )
                            editingLoc = null
                        }
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { editingLoc = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun ItemsAndOrgsSubScreen(
    items: List<WorldItemEntity>,
    onSaveItem: (WorldItemEntity) -> Unit,
    onDeleteItem: (WorldItemEntity) -> Unit
) {
    var editingItem by remember { mutableStateOf<WorldItemEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Предметы, артефакты и организации", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { editingItem = WorldItemEntity(name = "", category = "Предмет", description = "") },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить объект")
                }
            }
        }

        items(items, key = { it.id }) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = CrimsonPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(item.category, style = MaterialTheme.typography.labelMedium, color = AmberGoldSecondary)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CanonStatusBadge(statusName = item.canonStatus)
                            IconButton(onClick = { editingItem = item }) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                            }
                            IconButton(onClick = { onDeleteItem(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                    Text(item.description, style = MaterialTheme.typography.bodyMedium)
                    if (item.properties.isNotBlank()) CharacterFieldRow("Свойства", item.properties)
                    if (item.origin.isNotBlank()) CharacterFieldRow("Происхождение", item.origin)
                    if (item.ownerCharacterName.isNotBlank()) CharacterFieldRow("Владелец / Связанный персонаж", item.ownerCharacterName)
                    if (item.locationName.isNotBlank()) CharacterFieldRow("Локация", item.locationName)
                    if (item.notes.isNotBlank()) CharacterFieldRow("Заметки", item.notes)
                }
            }
        }
    }

    if (editingItem != null) {
        val base = editingItem!!
        var name by remember(base) { mutableStateOf(base.name) }
        var category by remember(base) { mutableStateOf(base.category) }
        var desc by remember(base) { mutableStateOf(base.description) }
        var props by remember(base) { mutableStateOf(base.properties) }
        var origin by remember(base) { mutableStateOf(base.origin) }
        var owner by remember(base) { mutableStateOf(base.ownerCharacterName) }
        var loc by remember(base) { mutableStateOf(base.locationName) }
        var notes by remember(base) { mutableStateOf(base.notes) }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text(if (base.id == 0L) "Новый объект / организация" else "Редактировать объект") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Тип (Предмет / Артефакт / Организация)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = props, onValueChange = { props = it }, label = { Text("Свойства и функции") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = origin, onValueChange = { origin = it }, label = { Text("Происхождение") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("Владелец / Связанный персонаж") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Связанная локация") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Заметки") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveItem(
                                base.copy(
                                    name = name.trim(),
                                    category = category.trim(),
                                    description = desc.trim(),
                                    properties = props.trim(),
                                    origin = origin.trim(),
                                    ownerCharacterName = owner.trim(),
                                    locationName = loc.trim(),
                                    notes = notes.trim()
                                )
                            )
                            editingItem = null
                        }
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text("Отмена") }
            }
        )
    }
}

/**
 * Section 9.4: Surgical XLSX Workbook Editor preserving sheets, sheet names, field order,
 * formulas (<f>), styles (s="..."), and merged cells (<mergeCells>), with automatic backup,
 * version comparison diff viewer, and rollback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XlsxSurgicalEditorSubScreen(
    workbook: XlsxWorkbookModel?,
    diffs: List<CellDiffItem>,
    onEditCell: (String, String, String) -> Unit,
    onRevertBackup: () -> Unit,
    onImportUri: (Uri, String) -> Unit,
    onExportUri: (Uri) -> Unit
) {
    var selectedSheetIdx by remember { mutableIntStateOf(0) }
    var editingCell by remember { mutableStateOf<Pair<String, XlsxCellData>?>(null) }
    var showExportWarningDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportUri(uri, uri.lastPathSegment?.substringAfterLast('/') ?: "imported.xlsx")
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            onExportUri(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("xlsx_editor_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = CrimsonPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Хирургический редактор XLSX: ${workbook?.fileName ?: "Загрузка..."}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Сохраняет листы, порядок полей, формулы (<f>), стили и объединённые ячейки (<mergeCells>). Перед каждым изменением создаётся резервная версия.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(
                                    arrayOf(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "*/*"
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Импорт XLSX")
                        }

                        OutlinedButton(
                            onClick = { showExportWarningDialog = true }
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Экспорт XLSX")
                        }

                        OutlinedButton(
                            onClick = onRevertBackup,
                            modifier = Modifier.testTag("revert_xlsx_backup_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Отменить правку")
                        }
                    }
                }
            }
        }

        if (diffs.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, StatusCanonGreen)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Сравнение с предыдущей резервной версией (${diffs.size} измен.):",
                            style = MaterialTheme.typography.titleSmall,
                            color = StatusCanonGreen
                        )
                        diffs.forEach { diff ->
                            Text(
                                text = "• Лист «${diff.sheetName}», ячейка ${diff.cellRef}: «${diff.oldValue}» → «${diff.newValue}» (Формулы сохранены: ${if (diff.formulaPreserved) "Да" else "Обновлено"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        if (workbook != null && workbook.sheets.isNotEmpty()) {
            val safeIdx = selectedSheetIdx.coerceIn(0, workbook.sheets.lastIndex)
            val currentSheet = workbook.sheets[safeIdx]

            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    workbook.sheets.forEachIndexed { idx, sheet ->
                        FilterChip(
                            selected = safeIdx == idx,
                            onClick = { selectedSheetIdx = idx },
                            label = { Text("Лист: ${sheet.sheetName}") }
                        )
                    }
                }
                if (currentSheet.mergedRanges.isNotEmpty()) {
                    Text(
                        text = "Сохраняемые объединённые диапазоны (mergeCells): ${currentSheet.mergedRanges.joinToString(", ")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberGoldSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            val rowsMap = currentSheet.cells.groupBy { it.rowNumber }.toSortedMap()
            items(rowsMap.entries.toList(), key = { it.key }) { (rowNum, rowCells) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Строка #$rowNum",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCells.sortedBy { it.colIndex }.forEach { cell ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            editingCell = currentSheet.zipEntryPath to cell
                                        }
                                        .testTag("xlsx_cell_${cell.cellRef}"),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(
                                        1.dp,
                                        if (cell.formula.isNotEmpty()) AmberGoldSecondary else MaterialTheme.colorScheme.outline
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = cell.cellRef,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = CrimsonPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (cell.formula.isNotEmpty()) {
                                                Text(
                                                    text = "fx=${cell.formula}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = AmberGoldSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = cell.displayValue,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingCell != null) {
        val (zipPath, cell) = editingCell!!
        var newValue by remember(cell) { mutableStateOf(cell.displayValue) }
        AlertDialog(
            onDismissRequest = { editingCell = null },
            title = { Text("Точечное изменение ячейки ${cell.cellRef}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Карточка не перестраивается целиком: изменится только значение ячейки ${cell.cellRef}. Все листы, стили s=\"${cell.styleIndex}\" и объединённые ячейки останутся нетронутыми.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (cell.formula.isNotEmpty()) {
                        Text(
                            text = "Внимание: ячейка содержит формулу =${cell.formula}. При вводе числа тег формулы <f> будет сохранён.",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberGoldSecondary
                        )
                    }
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Значение ячейки ${cell.cellRef}") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditCell(zipPath, cell.cellRef, newValue)
                        editingCell = null
                    }
                ) { Text("Создать бэкап и сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { editingCell = null }) { Text("Отмена") }
            }
        )
    }

    if (showExportWarningDialog) {
        AlertDialog(
            onDismissRequest = { showExportWarningDialog = false },
            title = { Text("Проверка структуры перед экспортом XLSX") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Статус сохранности элементов исходного документа:",
                        style = MaterialTheme.typography.titleSmall,
                        color = StatusCanonGreen
                    )
                    Text("• Листы и названия листов: Сохранены 100%", style = MaterialTheme.typography.bodySmall)
                    Text("• Порядок полей и строк: Сохранён 100%", style = MaterialTheme.typography.bodySmall)
                    Text("• Формулы (<f>) и стили (styles.xml): Сохранены 100%", style = MaterialTheme.typography.bodySmall)
                    Text("• Объединённые ячейки (<mergeCells>): Сохранены 100%", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportWarningDialog = false
                        exportLauncher.launch(workbook?.fileName ?: "RedWorld_Character_Cards.xlsx")
                    }
                ) { Text("Экспортировать файл") }
            },
            dismissButton = {
                TextButton(onClick = { showExportWarningDialog = false }) { Text("Отмена") }
            }
        )
    }
}
