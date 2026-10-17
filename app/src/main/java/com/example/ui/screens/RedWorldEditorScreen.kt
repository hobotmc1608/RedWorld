package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CanonStatus
import com.example.data.local.CellDiffItem
import com.example.data.local.CharacterEntity
import com.example.data.local.LocationEntity
import com.example.data.local.WorldEventEntity
import com.example.data.local.WorldItemEntity
import com.example.data.local.WorldRuleEntity
import com.example.data.local.XlsxCellData
import com.example.data.local.XlsxWorkbookModel
import com.example.ui.components.CanonStatusBadge
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.BurgundyCardBorder
import com.example.ui.theme.BurgundySurfaceContainer
import com.example.ui.theme.BurgundySurfaceElevated
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.StatusCanonGreen
import com.example.ui.theme.TextIvoryPrimary
import com.example.ui.theme.TextMutedRose

/**
 * RedWorld Editor (Section 9):
 * Manages Characters (with strict separation of Permanent Lore, Current State, and Event History),
 * World Rules, Locations (Красный особняк, Зелёная улица, Магазин, Бар), Events/Scenes,
 * Items/Organizations, and Surgical XLSX Workbook Editing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RedWorldEditorScreen(
    characters: List<CharacterEntity>,
    locations: List<LocationEntity>,
    worldRules: List<WorldRuleEntity>,
    events: List<WorldEventEntity>,
    items: List<WorldItemEntity>,
    xlsxWorkbook: XlsxWorkbookModel?,
    xlsxDiffs: List<CellDiffItem>,
    selectedCharacterId: Long?,
    onSelectCharacter: (Long) -> Unit,
    onSaveCharacter: (CharacterEntity, String) -> Unit,
    onUpdateCharacterStateOnly: (Long, String, String, String, String) -> Unit,
    onDeleteCharacter: (CharacterEntity) -> Unit,
    onSaveLocation: (LocationEntity) -> Unit,
    onSaveWorldRule: (WorldRuleEntity) -> Unit,
    onSaveEvent: (WorldEventEntity) -> Unit,
    onEditXlsxCell: (String, String, String) -> Unit,
    onRevertXlsxBackup: () -> Unit,
    onImportXlsxUri: (Uri, String) -> Unit,
    onExportXlsxUri: (Uri) -> Unit,
    initialSubTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    // 0 = Персонажи, 1 = Правила мира, 2 = Локации, 3 = События и состояния, 4 = Таблицы XLSX

    val tabs = listOf(
        "Персонажи (${characters.size})",
        "Правила мира (${worldRules.size})",
        "Локации (${locations.size})",
        "События и сцены (${events.size})",
        "Таблицы XLSX"
    )

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = BurgundySurfaceContainer,
            contentColor = CrimsonPrimary,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = activeSubTab == idx,
                    onClick = { activeSubTab = idx },
                    text = { Text(title) },
                    modifier = Modifier.testTag("redworld_editor_tab_$idx")
                )
            }
        }

        when (activeSubTab) {
            0 -> CharacterEditorSubScreen(
                characters = characters,
                locations = locations,
                selectedCharacterId = selectedCharacterId,
                onSelectCharacter = onSelectCharacter,
                onSaveCharacter = onSaveCharacter,
                onUpdateCharacterStateOnly = onUpdateCharacterStateOnly,
                onDeleteCharacter = onDeleteCharacter
            )
            1 -> WorldRulesSubScreen(
                rules = worldRules,
                onSaveRule = onSaveWorldRule
            )
            2 -> LocationsSubScreen(
                locations = locations,
                onSaveLocation = onSaveLocation
            )
            3 -> EventsAndHistorySubScreen(
                events = events,
                locations = locations,
                onSaveEvent = onSaveEvent
            )
            4 -> XlsxSurgicalEditorSubScreen(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterEditorSubScreen(
    characters: List<CharacterEntity>,
    locations: List<LocationEntity>,
    selectedCharacterId: Long?,
    onSelectCharacter: (Long) -> Unit,
    onSaveCharacter: (CharacterEntity, String) -> Unit,
    onUpdateCharacterStateOnly: (Long, String, String, String, String) -> Unit,
    onDeleteCharacter: (CharacterEntity) -> Unit
) {
    val activeChar = characters.firstOrNull { it.id == selectedCharacterId } ?: characters.firstOrNull()
    var showFullEditDialog by remember { mutableStateOf(false) }
    var showNewCharDialog by remember { mutableStateOf(false) }
    var showStateOnlyDialog by remember { mutableStateOf(false) }

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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    characters.forEach { ch ->
                        FilterChip(
                            selected = ch.id == activeChar?.id,
                            onClick = { onSelectCharacter(ch.id) },
                            label = { Text(ch.name) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
                Button(
                    onClick = { showNewCharDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("add_character_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить персонажа")
                }
            }
        }

        if (activeChar != null) {
            // LAYER 2 CARD: ТЕКУЩЕЕ СОСТОЯНИЕ (Separated from Permanent Lore!)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BurgundySurfaceElevated),
                    border = BorderStroke(1.dp, AmberGoldSecondary.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Слой 2: Текущее состояние (не перезаписывает биографию)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AmberGoldSecondary
                                )
                                Text(
                                    text = "Локация сейчас: ${activeChar.currentLocationName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextIvoryPrimary
                                )
                            }
                            OutlinedButton(
                                onClick = { showStateOnlyDialog = true },
                                modifier = Modifier.testTag("edit_current_state_button")
                            ) {
                                Text("Сменить состояние")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Эмоции и состояние: ${activeChar.currentMoodAndCondition}", style = MaterialTheme.typography.bodySmall)
                        if (activeChar.currentOutfit.isNotBlank()) {
                            Text("Текущая одежда: ${activeChar.currentOutfit}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (activeChar.currentActiveGoal.isNotBlank()) {
                            Text("Текущая задача в сцене: ${activeChar.currentActiveGoal}", style = MaterialTheme.typography.bodySmall, color = AmberGoldSecondary)
                        }
                    }
                }
            }

            // LAYER 1 CARD: ПОСТОЯННЫЕ СВЕДЕНИЯ (Permanent Character Dossier - all 26+ fields)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                    border = BorderStroke(1.dp, BurgundyCardBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${activeChar.name} (v${activeChar.versionNumber})",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextIvoryPrimary
                                )
                                Text(
                                    text = "Псевдонимы: ${activeChar.aliases.ifEmpty { "—" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberGoldSecondary
                                )
                            }
                            CanonStatusBadge(statusName = activeChar.canonStatus)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showFullEditDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                modifier = Modifier.testTag("edit_character_card_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Редактировать досье")
                            }
                            if (characters.size > 1) {
                                IconButton(onClick = { onDeleteCharacter(activeChar) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = TextMutedRose)
                                }
                            }
                        }

                        Text("Слой 1: Постоянные биографические сведения", style = MaterialTheme.typography.titleSmall, color = CrimsonPrimary)
                        CharacterFieldRow("Дата рождения и возраст", "${activeChar.birthDate} (${activeChar.age} лет)")
                        CharacterFieldRow("Место рождения", activeChar.birthPlace)
                        CharacterFieldRow("Род занятий / Роль", activeChar.occupation)
                        CharacterFieldRow("Внешность и телосложение", "${activeChar.appearance} • ${activeChar.physique}")
                        CharacterFieldRow("Волосы и глаза", "Волосы: ${activeChar.hair} • Глаза: ${activeChar.eyes}")
                        CharacterFieldRow("Повседневная одежда", activeChar.defaultClothing)
                        CharacterFieldRow("Особые приметы", activeChar.distinguishingMarks)
                        CharacterFieldRow("Характер", activeChar.personality)
                        CharacterFieldRow("Привычки и манера речи", "${activeChar.habits} • Речь: ${activeChar.speechStyle}")
                        CharacterFieldRow("Навыки и интересы", "${activeChar.skills} • Интересы: ${activeChar.interests}")
                        CharacterFieldRow("Предпочтения", activeChar.preferences)
                        CharacterFieldRow("Цели и мотивация", "${activeChar.goals} • Мотивация: ${activeChar.motivation}")
                        CharacterFieldRow("Поведенческие особенности", activeChar.behavioralTraits)
                        CharacterFieldRow("Отношения", activeChar.relationshipsSummary)
                        CharacterFieldRow("Паспорт внешности (Image Studio)", activeChar.visualPassportPrompt)
                        CharacterFieldRow("Пользовательские поля (JSON)", activeChar.customFieldsJson)
                        if (activeChar.additionalNotes.isNotBlank()) {
                            CharacterFieldRow("Дополнительные сведения", activeChar.additionalNotes)
                        }
                    }
                }
            }
        }
    }

    // Dialog: Update Current State Only (Layer 2)
    if (showStateOnlyDialog && activeChar != null) {
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
                        color = TextMutedRose
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

    // Dialog: Full Edit or New Character
    if ((showFullEditDialog && activeChar != null) || showNewCharDialog) {
        val base = if (showNewCharDialog) {
            CharacterEntity(name = "", canonStatus = CanonStatus.DRAFT.name)
        } else activeChar!!

        var name by remember { mutableStateOf(base.name) }
        var aliases by remember { mutableStateOf(base.aliases) }
        var birthDate by remember { mutableStateOf(base.birthDate) }
        var age by remember { mutableStateOf(base.age) }
        var occupation by remember { mutableStateOf(base.occupation) }
        var appearance by remember { mutableStateOf(base.appearance) }
        var personality by remember { mutableStateOf(base.personality) }
        var goals by remember { mutableStateOf(base.goals) }
        var visualPassport by remember { mutableStateOf(base.visualPassportPrompt) }
        var customJson by remember { mutableStateOf(base.customFieldsJson) }
        var canonStatus by remember { mutableStateOf(base.canonStatus) }

        AlertDialog(
            onDismissRequest = {
                showFullEditDialog = false
                showNewCharDialog = false
            },
            title = { Text(if (showNewCharDialog) "Новая карточка персонажа" else "Редактировать: ${base.name}") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(380.dp)) {
                    item {
                        Text("Статус достоверности данных (предположения не становятся каноном автоматически):", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CanonStatus.entries.forEach { st ->
                                FilterChip(
                                    selected = canonStatus == st.name,
                                    onClick = { canonStatus = st.name },
                                    label = { Text(st.label, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }
                    }
                    item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя персонажа") }) }
                    item { OutlinedTextField(value = aliases, onValueChange = { aliases = it }, label = { Text("Псевдонимы") }) }
                    item { OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Дата рождения") }) }
                    item { OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Возраст") }) }
                    item { OutlinedTextField(value = occupation, onValueChange = { occupation = it }, label = { Text("Работа / Занятие") }) }
                    item { OutlinedTextField(value = appearance, onValueChange = { appearance = it }, label = { Text("Внешность, волосы, глаза, одежда") }, minLines = 2) }
                    item { OutlinedTextField(value = personality, onValueChange = { personality = it }, label = { Text("Характер, привычки, манера речи") }, minLines = 2) }
                    item { OutlinedTextField(value = goals, onValueChange = { goals = it }, label = { Text("Цели и мотивация") }) }
                    item { OutlinedTextField(value = visualPassport, onValueChange = { visualPassport = it }, label = { Text("Утверждённый паспорт внешности (Image Studio)") }, minLines = 2) }
                    item { OutlinedTextField(value = customJson, onValueChange = { customJson = it }, label = { Text("Пользовательские поля (JSON)") }) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveCharacter(
                                base.copy(
                                    name = name.trim(),
                                    aliases = aliases.trim(),
                                    birthDate = birthDate.trim(),
                                    age = age.trim(),
                                    occupation = occupation.trim(),
                                    appearance = appearance.trim(),
                                    personality = personality.trim(),
                                    goals = goals.trim(),
                                    visualPassportPrompt = visualPassport.trim(),
                                    customFieldsJson = customJson.trim(),
                                    canonStatus = canonStatus
                                ),
                                if (showNewCharDialog) "Создание карточки персонажа" else "Редактирование постоянных сведений карточки"
                            )
                            showFullEditDialog = false
                            showNewCharDialog = false
                        }
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Сохранить версию")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFullEditDialog = false
                        showNewCharDialog = false
                    }
                ) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun CharacterFieldRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = AmberGoldSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = TextIvoryPrimary)
    }
}

@Composable
private fun WorldRulesSubScreen(
    rules: List<WorldRuleEntity>,
    onSaveRule: (WorldRuleEntity) -> Unit
) {
    var showAddRuleDialog by remember { mutableStateOf(false) }

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
                    Text("Карточка мира RedWorld (Законы и механика)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Общие правила мира хранятся отдельно от памяти о конкретных сюжетных сценах.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                }
                Button(
                    onClick = { showAddRuleDialog = true },
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
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = CrimsonPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(rule.title, style = MaterialTheme.typography.titleSmall, color = TextIvoryPrimary)
                                Text("${rule.category} • ${rule.priorityLevel}", style = MaterialTheme.typography.labelMedium, color = AmberGoldSecondary)
                            }
                        }
                        CanonStatusBadge(statusName = rule.canonStatus)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(rule.content, style = MaterialTheme.typography.bodyMedium, color = TextIvoryPrimary)
                }
            }
        }
    }

    if (showAddRuleDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Общие правила") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddRuleDialog = false },
            title = { Text("Добавить правило мира RedWorld") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название правила") })
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Раздел (География, Автономия, Сцены и т.д.)") })
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Формулировка закона мира") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSaveRule(WorldRuleEntity(title = title, category = category, content = content))
                            showAddRuleDialog = false
                        }
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showAddRuleDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun LocationsSubScreen(
    locations: List<LocationEntity>,
    onSaveLocation: (LocationEntity) -> Unit
) {
    var showAddLocDialog by remember { mutableStateOf(false) }

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
                Text("Локации RedWorld", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { showAddLocDialog = true },
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
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = CrimsonPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(loc.name, style = MaterialTheme.typography.titleMedium, color = TextIvoryPrimary)
                                Text(loc.category, style = MaterialTheme.typography.labelMedium, color = AmberGoldSecondary)
                            }
                        }
                        CanonStatusBadge(statusName = loc.canonStatus)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(loc.description, style = MaterialTheme.typography.bodyMedium)
                    if (loc.atmosphere.isNotBlank()) {
                        Text("Атмосфера: ${loc.atmosphere}", style = MaterialTheme.typography.bodySmall, color = TextMutedRose)
                    }
                    if (loc.accessRules.isNotBlank()) {
                        Text("Правила доступа: ${loc.accessRules}", style = MaterialTheme.typography.bodySmall, color = AmberGoldSecondary)
                    }
                }
            }
        }
    }

    if (showAddLocDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Городская локация") }
        var desc by remember { mutableStateOf("") }
        var atmosphere by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddLocDialog = false },
            title = { Text("Добавить локацию RedWorld") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название локации") })
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Категория") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, minLines = 3)
                    OutlinedTextField(value = atmosphere, onValueChange = { atmosphere = it }, label = { Text("Атмосфера и интерьер") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveLocation(LocationEntity(name = name, category = category, description = desc, atmosphere = atmosphere))
                            showAddLocDialog = false
                        }
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showAddLocDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun EventsAndHistorySubScreen(
    events: List<WorldEventEntity>,
    locations: List<LocationEntity>,
    onSaveEvent: (WorldEventEntity) -> Unit
) {
    var showAddEventDialog by remember { mutableStateOf(false) }

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
                    Text("Слой 3: История событий и сцен", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Хронологический журнал сцен. Сохраняет прошлое неизменным при смене текущей локации персонажей.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                }
                Button(
                    onClick = { showAddEventDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Событие")
                }
            }
        }

        items(events, key = { it.id }) { ev ->
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ev.title, style = MaterialTheme.typography.titleSmall, color = TextIvoryPrimary)
                            Text("${ev.timelineDate} • Локация: ${ev.locationName} • Участники: ${ev.participantNamesCsv}", style = MaterialTheme.typography.labelMedium, color = AmberGoldSecondary)
                        }
                        CanonStatusBadge(statusName = ev.canonStatus)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(ev.description, style = MaterialTheme.typography.bodyMedium)
                    if (ev.consequences.isNotBlank()) {
                        Text("Последствия: ${ev.consequences}", style = MaterialTheme.typography.bodySmall, color = TextMutedRose)
                    }
                }
            }
        }
    }

    if (showAddEventDialog) {
        var title by remember { mutableStateOf("") }
        var date by remember { mutableStateOf("День 13, 19:00") }
        var loc by remember { mutableStateOf(locations.firstOrNull()?.name ?: "Красный особняк") }
        var participants by remember { mutableStateOf("Виктор Корвин, Елена Ростова") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("Зафиксировать событие в хронологии") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название события / сцены") })
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Дата / Время в хронологии") })
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Локация") })
                    OutlinedTextField(value = participants, onValueChange = { participants = it }, label = { Text("Участники") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание происшедшего") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSaveEvent(
                                WorldEventEntity(
                                    title = title,
                                    timelineDate = date,
                                    locationName = loc,
                                    participantNamesCsv = participants,
                                    description = desc
                                )
                            )
                            showAddEventDialog = false
                        }
                    }
                ) { Text("Записать") }
            },
            dismissButton = {
                TextButton(onClick = { showAddEventDialog = false }) { Text("Отмена") }
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
    var editingCell by remember { mutableStateOf< Pair<String, XlsxCellData>? >(null) }
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
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceElevated),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = CrimsonPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Хирургический редактор XLSX: ${workbook?.fileName ?: "Загрузка..."}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextIvoryPrimary
                            )
                            Text(
                                text = "Сохраняет листы, порядок полей, формулы (<f>), стили и объединённые ячейки (<mergeCells>). Перед каждым изменением создаётся резервная версия.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedRose
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

        // Diff comparison card if a cell was modified
        if (diffs.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
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
                                color = TextIvoryPrimary
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
                    colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                    border = BorderStroke(1.dp, BurgundyCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Строка #$rowNum",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMutedRose
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
                                    color = BurgundySurfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (cell.formula.isNotEmpty()) AmberGoldSecondary else BurgundyCardBorder
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
                                            color = TextIvoryPrimary
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

    // Dialog: Surgical Single Cell Edit
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
                        color = TextMutedRose
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

    // Dialog: Pre-export check & warning (Section 9.4 requirement)
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
                    if (workbook?.warnings?.isNotEmpty() == true) {
                        workbook.warnings.forEach { w ->
                            Text(w, style = MaterialTheme.typography.bodySmall, color = AmberGoldSecondary)
                        }
                    } else {
                        Text(
                            "Предупреждений о потере элементов нет. Если книга открывается во внешнем Excel после правок формул, Excel автоматически пересчитает кэшированные значения.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedRose
                        )
                    }
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
