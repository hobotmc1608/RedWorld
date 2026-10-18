package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CanonStatus
import com.example.data.local.CharacterEntity
import com.example.data.local.LocationEntity
import com.example.data.local.WorldEventEntity
import com.example.data.local.WorldRuleEntity
import com.example.ui.components.CanonStatusBadge
import com.example.ui.components.StudioEmptyState
import com.example.ui.components.StudioErrorBanner
import com.example.ui.components.StudioLoadingState
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioSecondaryButton
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.BurgundyCardBorder
import com.example.ui.theme.BurgundySurfaceContainer
import com.example.ui.theme.BurgundySurfaceElevated
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.StatusAssumptionAmber
import com.example.ui.theme.StatusCanonGreen
import com.example.ui.theme.StatusDraftIndigo
import com.example.ui.theme.StatusReviewRed
import com.example.ui.theme.TextIvoryPrimary
import com.example.ui.theme.TextMutedRose
import kotlinx.serialization.Serializable

@Serializable
data class SimulatedEventDraft(
    val id: String = "sim_evt_${System.currentTimeMillis()}_${(100..999).random()}",
    val title: String,
    val description: String,
    val locationName: String,
    val participantsCsv: String,
    val causes: String,
    val possibleConsequences: String,
    val relatedProjectContext: String,
    val contradictionWarnings: String = "",
    val linkedStorylineTitle: String = "",
    val scale: String = "Локальная сцена",
    val surpriseLevel: String = "Средний",
    val status: String = CanonStatus.DRAFT.name
)

@Serializable
data class RoleplayChatMessage(
    val id: String = "rp_msg_${System.currentTimeMillis()}_${(100..999).random()}",
    val senderRole: String, // "USER" or "CHARACTER"
    val senderName: String,
    val text: String,
    val isMarkedUnsuccessful: Boolean = false,
    val userRemark: String = ""
)

@Serializable
data class RoleplayCriterionScore(
    val index: Int,
    val criterionTitle: String,
    val scoreOutOfTen: Int,
    val explanation: String,
    val exampleQuote: String,
    val cardBasis: String,
    val detectedContradiction: String,
    val improvementSuggestion: String
)

@Serializable
data class RoleplayEvaluationReport(
    val characterName: String,
    val overallScore: Double,
    val sparseCardWarning: String = "",
    val criteria: List<RoleplayCriterionScore>,
    val proposedCardField: String = "behavioralTraits",
    val proposedCardOldValue: String = "",
    val proposedCardNewValue: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Globe Button Destination — World Simulation Screen (Sections 15, 16, 17, 18):
 * Mode 0: Случайные события (Random Event Simulation with Draft workflow)
 * Mode 1: Тестирование персонажа и Оценка качества отыгрыша (10 criteria)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorldSimulationScreen(
    projectName: String,
    characters: List<CharacterEntity>,
    locations: List<LocationEntity>,
    worldRules: List<WorldRuleEntity>,
    events: List<WorldEventEntity>,
    simulatedDrafts: List<SimulatedEventDraft>,
    isGeneratingSimulation: Boolean,
    simulationError: String?,
    roleplayMessages: List<RoleplayChatMessage>,
    selectedRoleplayCharacterId: Long?,
    isRoleplayGenerating: Boolean,
    latestEvaluationReport: RoleplayEvaluationReport?,
    previousEvaluationReport: RoleplayEvaluationReport?,
    onSelectRoleplayCharacter: (Long) -> Unit,
    onGenerateSimulatedEvents: (
        scale: String,
        locationScope: String,
        participantsCsv: String,
        surpriseLevel: String,
        storylineTitle: String,
        variantsCount: Int
    ) -> Unit,
    onUpdateSimulatedDraft: (SimulatedEventDraft) -> Unit,
    onRejectSimulatedDraft: (String) -> Unit,
    onAcceptDraftToHistory: (SimulatedEventDraft) -> Unit,
    onAddDraftToStoryline: (SimulatedEventDraft, String) -> Unit,
    onSendRoleplayMessage: (CharacterEntity, String) -> Unit,
    onStartNewRoleplaySession: (CharacterEntity) -> Unit,
    onToggleMessageUnsuccessful: (String, String) -> Unit,
    onEvaluateRoleplayQuality: (CharacterEntity) -> Unit,
    onApplyCharacterCardProposal: (CharacterEntity, String, String) -> Unit,
    onDismissError: () -> Unit,
    initialModeTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var activeTab by rememberSaveable(initialModeTab) { mutableIntStateOf(initialModeTab) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = BurgundySurfaceContainer,
            contentColor = CrimsonPrimary
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("1. Случайные события") },
                icon = { Icon(Icons.Default.Casino, contentDescription = null) },
                modifier = Modifier.testTag("sim_tab_random_events")
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("2. Тестирование персонажа") },
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.testTag("sim_tab_character_test")
            )
        }

        if (!simulationError.isNullOrBlank()) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                StudioErrorBanner(
                    message = simulationError,
                    onDismiss = onDismissError
                )
            }
        }

        when (activeTab) {
            0 -> RandomEventsSimulationTab(
                projectName = projectName,
                characters = characters,
                locations = locations,
                worldRules = worldRules,
                events = events,
                drafts = simulatedDrafts,
                isGenerating = isGeneratingSimulation,
                onGenerate = onGenerateSimulatedEvents,
                onUpdateDraft = onUpdateSimulatedDraft,
                onRejectDraft = onRejectSimulatedDraft,
                onAcceptToHistory = onAcceptDraftToHistory,
                onAddToStoryline = onAddDraftToStoryline
            )
            1 -> CharacterRoleplayAndEvaluationTab(
                characters = characters,
                worldRules = worldRules,
                selectedCharacterId = selectedRoleplayCharacterId,
                messages = roleplayMessages,
                isGenerating = isRoleplayGenerating,
                latestReport = latestEvaluationReport,
                previousReport = previousEvaluationReport,
                onSelectCharacter = onSelectRoleplayCharacter,
                onSendMessage = onSendRoleplayMessage,
                onNewSession = onStartNewRoleplaySession,
                onToggleUnsuccessful = onToggleMessageUnsuccessful,
                onEvaluateQuality = onEvaluateRoleplayQuality,
                onApplyCardProposal = onApplyCharacterCardProposal
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RandomEventsSimulationTab(
    projectName: String,
    characters: List<CharacterEntity>,
    locations: List<LocationEntity>,
    worldRules: List<WorldRuleEntity>,
    events: List<WorldEventEntity>,
    drafts: List<SimulatedEventDraft>,
    isGenerating: Boolean,
    onGenerate: (String, String, String, String, String, Int) -> Unit,
    onUpdateDraft: (SimulatedEventDraft) -> Unit,
    onRejectDraft: (String) -> Unit,
    onAcceptToHistory: (SimulatedEventDraft) -> Unit,
    onAddToStoryline: (SimulatedEventDraft, String) -> Unit
) {
    var scale by rememberSaveable { mutableStateOf("Локальная сцена") }
    var locationScope by rememberSaveable { mutableStateOf(locations.firstOrNull()?.name ?: "Весь мир") }
    val selectedParticipants = remember {
        mutableStateListOf<String>().apply {
            characters.take(2).forEach { add(it.name) }
        }
    }
    var surpriseLevel by rememberSaveable { mutableStateOf("Средний (Интрига)") }
    val storylines = events.filter { it.eventType == "Сюжетная линия" }
    var selectedStoryline by rememberSaveable {
        mutableStateOf(storylines.firstOrNull()?.title ?: "Независимая сцена / Новая линия")
    }
    var variantsCount by rememberSaveable { mutableIntStateOf(2) }
    var draftBeingEdited by remember { mutableStateOf<SimulatedEventDraft?>(null) }

    val scaleOptions = listOf("Бытовой эпизод", "Локальная сцена", "Важное событие", "Масштабный поворот")
    val surpriseOptions = listOf("Низкий (Строго по канону)", "Средний (Интрига)", "Высокий (Неожиданный поворот)")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("random_events_simulation_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = CrimsonPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Генератор случайных событий мира «$projectName»",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextIvoryPrimary
                            )
                            Text(
                                text = "События генерируются по запросу со статусом «Черновик» с учётом правил мира (${worldRules.size}), характеров, мотивации и знаний персонажей. Черновики не записываются в каноническую историю без вашего подтверждения.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedRose
                            )
                        }
                    }

                    Text("1. Масштаб события:", style = MaterialTheme.typography.labelLarge, color = AmberGoldSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        scaleOptions.forEach { opt ->
                            FilterChip(
                                selected = scale == opt,
                                onClick = { scale = opt },
                                label = { Text(opt) }
                            )
                        }
                    }

                    Text("2. Локация или весь мир:", style = MaterialTheme.typography.labelLarge, color = AmberGoldSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = locationScope == "Весь мир",
                            onClick = { locationScope = "Весь мир" },
                            label = { Text("Весь мир") }
                        )
                        locations.forEach { loc ->
                            FilterChip(
                                selected = locationScope == loc.name,
                                onClick = { locationScope = loc.name },
                                label = { Text(loc.name) }
                            )
                        }
                    }

                    Text("3. Участники сцены:", style = MaterialTheme.typography.labelLarge, color = AmberGoldSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        characters.forEach { ch ->
                            val isChosen = selectedParticipants.contains(ch.name)
                            FilterChip(
                                selected = isChosen,
                                onClick = {
                                    if (isChosen) selectedParticipants.remove(ch.name)
                                    else selectedParticipants.add(ch.name)
                                },
                                label = { Text(ch.name) }
                            )
                        }
                    }

                    Text("4. Уровень неожиданности:", style = MaterialTheme.typography.labelLarge, color = AmberGoldSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        surpriseOptions.forEach { opt ->
                            FilterChip(
                                selected = surpriseLevel == opt,
                                onClick = { surpriseLevel = opt },
                                label = { Text(opt) }
                            )
                        }
                    }

                    Text("5. Связь с сюжетом и число вариантов:", style = MaterialTheme.typography.labelLarge, color = AmberGoldSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedStoryline == "Независимая сцена / Новая линия",
                            onClick = { selectedStoryline = "Независимая сцена / Новая линия" },
                            label = { Text("Новая / Независимая линия") }
                        )
                        storylines.forEach { sl ->
                            FilterChip(
                                selected = selectedStoryline == sl.title,
                                onClick = { selectedStoryline = sl.title },
                                label = { Text(sl.title) }
                            )
                        }
                        listOf(1, 2, 3).forEach { count ->
                            FilterChip(
                                selected = variantsCount == count,
                                onClick = { variantsCount = count },
                                label = { Text("$count вар.") }
                            )
                        }
                    }

                    StudioPrimaryButton(
                        text = "Сгенерировать черновики событий ($variantsCount)",
                        icon = Icons.Default.AutoAwesome,
                        isLoading = isGenerating,
                        onClick = {
                            val parts = if (selectedParticipants.isEmpty()) {
                                characters.take(2).joinToString(", ") { it.name }
                            } else {
                                selectedParticipants.joinToString(", ")
                            }
                            onGenerate(scale, locationScope, parts, surpriseLevel, selectedStoryline, variantsCount)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_simulated_events_button")
                    )
                }
            }
        }

        if (isGenerating) {
            item {
                StudioLoadingState(
                    title = "Симуляция вариантов развития событий...",
                    subtitle = "ИИ проверяет правила мира, мотивацию участников и хронологию проекта"
                )
            }
        }

        if (drafts.isEmpty() && !isGenerating) {
            item {
                StudioEmptyState(
                    title = "Нет активных черновиков симуляции",
                    description = "Задайте масштаб, локацию и участников выше и нажмите «Сгенерировать черновики событий», чтобы получить варианты сцен.",
                    icon = Icons.Default.Casino
                )
            }
        }

        items(drafts, key = { it.id }) { draft ->
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceElevated),
                border = BorderStroke(1.dp, StatusDraftIndigo),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = draft.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextIvoryPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Место: ${draft.locationName} • Масштаб: ${draft.scale}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberGoldSecondary
                            )
                        }
                        CanonStatusBadge(statusName = CanonStatus.DRAFT.name)
                    }

                    Text(
                        text = draft.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextIvoryPrimary
                    )

                    HorizontalDivider(color = BurgundyCardBorder)

                    Text("Участники: ${draft.participantsCsv}", style = MaterialTheme.typography.bodySmall, color = TextIvoryPrimary)
                    Text("Причины возникновения: ${draft.causes}", style = MaterialTheme.typography.bodySmall, color = TextMutedRose)
                    Text("Возможные последствия: ${draft.possibleConsequences}", style = MaterialTheme.typography.bodySmall, color = AmberGoldSecondary)
                    Text("Учтённые сведения проекта: ${draft.relatedProjectContext}", style = MaterialTheme.typography.labelMedium, color = StatusCanonGreen)

                    if (draft.contradictionWarnings.isNotBlank()) {
                        Surface(
                            color = StatusAssumptionAmber.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StatusAssumptionAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = StatusAssumptionAmber, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Проверка противоречий: ${draft.contradictionWarnings}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextIvoryPrimary
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onAcceptToHistory(draft) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCanonGreen),
                            modifier = Modifier.testTag("accept_draft_to_history_${draft.id}")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Принять в историю")
                        }
                        OutlinedButton(
                            onClick = { onAddToStoryline(draft, selectedStoryline) }
                        ) {
                            Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("В сюжет")
                        }
                        OutlinedButton(
                            onClick = { draftBeingEdited = draft }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Редактировать")
                        }
                        OutlinedButton(
                            onClick = {
                                onGenerate(
                                    draft.scale,
                                    draft.locationName,
                                    draft.participantsCsv,
                                    draft.surpriseLevel,
                                    draft.linkedStorylineTitle.ifBlank { selectedStoryline },
                                    2
                                )
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Альтернативы")
                        }
                        TextButton(
                            onClick = { onRejectDraft(draft.id) }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = StatusReviewRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Отклонить", color = StatusReviewRed)
                        }
                    }
                }
            }
        }
    }

    draftBeingEdited?.let { target ->
        var title by remember { mutableStateOf(target.title) }
        var desc by remember { mutableStateOf(target.description) }
        var loc by remember { mutableStateOf(target.locationName) }
        var parts by remember { mutableStateOf(target.participantsCsv) }
        var causes by remember { mutableStateOf(target.causes) }
        var cons by remember { mutableStateOf(target.possibleConsequences) }

        AlertDialog(
            onDismissRequest = { draftBeingEdited = null },
            title = { Text("Редактировать черновик события") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") })
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Место") })
                    OutlinedTextField(value = parts, onValueChange = { parts = it }, label = { Text("Участники") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, minLines = 3)
                    OutlinedTextField(value = causes, onValueChange = { causes = it }, label = { Text("Причины") })
                    OutlinedTextField(value = cons, onValueChange = { cons = it }, label = { Text("Последствия") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDraft(
                            target.copy(
                                title = title,
                                locationName = loc,
                                participantsCsv = parts,
                                description = desc,
                                causes = causes,
                                possibleConsequences = cons
                            )
                        )
                        draftBeingEdited = null
                    }
                ) { Text("Сохранить черновик") }
            },
            dismissButton = {
                TextButton(onClick = { draftBeingEdited = null }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterRoleplayAndEvaluationTab(
    characters: List<CharacterEntity>,
    worldRules: List<WorldRuleEntity>,
    selectedCharacterId: Long?,
    messages: List<RoleplayChatMessage>,
    isGenerating: Boolean,
    latestReport: RoleplayEvaluationReport?,
    previousReport: RoleplayEvaluationReport?,
    onSelectCharacter: (Long) -> Unit,
    onSendMessage: (CharacterEntity, String) -> Unit,
    onNewSession: (CharacterEntity) -> Unit,
    onToggleUnsuccessful: (String, String) -> Unit,
    onEvaluateQuality: (CharacterEntity) -> Unit,
    onApplyCardProposal: (CharacterEntity, String, String) -> Unit
) {
    val activeChar = characters.firstOrNull { it.id == selectedCharacterId } ?: characters.firstOrNull()
    var messageInput by rememberSaveable { mutableStateOf("") }
    var showProposalDiffDialog by remember { mutableStateOf(false) }
    var showComparisonDialog by remember { mutableStateOf(false) }
    var messageForRemark by remember { mutableStateOf<RoleplayChatMessage?>(null) }

    if (activeChar == null) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            StudioEmptyState(
                title = "В проекте пока нет персонажей",
                description = "Создайте хотя бы одного персонажа в разделе «Персонажи», чтобы протестировать диалог и оценить качество отыгрыша.",
                icon = Icons.Default.Person
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("character_roleplay_test_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Character Selector & Active Persona Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Выберите персонажа для тестового диалога:",
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberGoldSecondary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        characters.forEach { ch ->
                            FilterChip(
                                selected = ch.id == activeChar.id,
                                onClick = { onSelectCharacter(ch.id) },
                                label = { Text(ch.name) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CharacterAvatarCircle(
                            name = activeChar.name,
                            avatarBase64 = activeChar.avatarBase64,
                            sizeDp = 54
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeChar.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextIvoryPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeChar.shortDescription.ifBlank { activeChar.occupation }.ifBlank { "Персонаж мира" },
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberGoldSecondary
                            )
                            Text(
                                text = "Характер: ${activeChar.personality.ifBlank { "не указан" }} • Речь: ${activeChar.speechStyle.ifBlank { "не указана" }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMutedRose
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioSecondaryButton(
                            text = "Новая сессия",
                            icon = Icons.Default.Refresh,
                            onClick = { onNewSession(activeChar) },
                            modifier = Modifier.weight(1f)
                        )
                        StudioPrimaryButton(
                            text = "Анализ отыгрыша (10 крит.)",
                            icon = Icons.Default.Analytics,
                            isLoading = isGenerating,
                            onClick = { onEvaluateQuality(activeChar) },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("evaluate_roleplay_quality_button")
                        )
                    }
                }
            }
        }

        // Test Dialogue History
        item {
            Text(
                text = "История тестового диалога (не изменяет каноническую историю мира):",
                style = MaterialTheme.typography.labelLarge,
                color = TextMutedRose
            )
        }

        if (messages.isEmpty()) {
            item {
                StudioEmptyState(
                    title = "Начните тестовый диалог с «${activeChar.name}»",
                    description = "Напишите реплику или вопрос ниже. Персонаж будет отвечать в соответствии со своим характером, манерой речи, привычками, известными ему фактами и ограничениями.",
                    icon = Icons.Default.Person
                )
            }
        } else {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.senderRole == "USER"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) CrimsonPrimary.copy(alpha = 0.16f) else BurgundySurfaceElevated
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (msg.isMarkedUnsuccessful) StatusReviewRed
                        else if (isUser) CrimsonPrimary.copy(alpha = 0.5f)
                        else BurgundyCardBorder
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg.senderName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isUser) CrimsonPrimary else AmberGoldSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            if (!isUser) {
                                AssistChip(
                                    onClick = { messageForRemark = msg },
                                    label = {
                                        Text(
                                            if (msg.isMarkedUnsuccessful) "Отмечен как неудачный" else "Замечание / Оценка",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = if (msg.isMarkedUnsuccessful) StatusReviewRed else TextMutedRose,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextIvoryPrimary
                        )
                        if (msg.userRemark.isNotBlank()) {
                            Text(
                                text = "Замечание автора: ${msg.userRemark}",
                                style = MaterialTheme.typography.labelMedium,
                                color = StatusReviewRed
                            )
                        }
                    }
                }
            }
        }

        // Message Input Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Реплика для ${activeChar.name}...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roleplay_message_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                onSendMessage(activeChar, messageInput.trim())
                                messageInput = ""
                            }
                        },
                        enabled = messageInput.isNotBlank() && !isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("roleplay_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                    }
                }
            }
        }

        // 10-Criteria Roleplay Quality Evaluation Report (Section 18)
        latestReport?.let { report ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                    border = BorderStroke(1.5.dp, AmberGoldSecondary),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roleplay_evaluation_report_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Оценка качества отыгрыша: ${report.characterName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextIvoryPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Анализ ИИ носит рекомендательный характер и помогает автору уточнить карточку персонажа.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMutedRose
                                )
                            }
                            Surface(
                                color = StatusCanonGreen.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, StatusCanonGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", report.overallScore)} / 10",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StatusCanonGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (report.sparseCardWarning.isNotBlank()) {
                            Surface(
                                color = StatusAssumptionAmber.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, StatusAssumptionAmber),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ Мало данных в карточке: ${report.sparseCardWarning}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextIvoryPrimary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showProposalDiffDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                modifier = Modifier.testTag("open_card_proposal_diff_button")
                            ) {
                                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Предложить изменение карточки («До и после»)")
                            }
                            OutlinedButton(onClick = { onEvaluateQuality(activeChar) }) {
                                Text("Повторить анализ")
                            }
                            if (previousReport != null) {
                                OutlinedButton(onClick = { showComparisonDialog = true }) {
                                    Text("Сравнить с прошлым тестом")
                                }
                            }
                        }

                        HorizontalDivider(color = BurgundyCardBorder)

                        report.criteria.forEach { crit ->
                            Surface(
                                color = BurgundySurfaceElevated,
                                border = BorderStroke(1.dp, BurgundyCardBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${crit.index}. ${crit.criterionTitle}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextIvoryPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${crit.scoreOutOfTen}/10",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (crit.scoreOutOfTen >= 8) StatusCanonGreen else AmberGoldSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text("Объяснение: ${crit.explanation}", style = MaterialTheme.typography.bodySmall, color = TextIvoryPrimary)
                                    Text("Пример ответа: «${crit.exampleQuote}»", style = MaterialTheme.typography.bodySmall, color = TextMutedRose)
                                    Text("Основа в карточке: ${crit.cardBasis}", style = MaterialTheme.typography.labelMedium, color = AmberGoldSecondary)
                                    if (crit.detectedContradiction.isNotBlank() && crit.detectedContradiction != "Не обнаружено") {
                                        Text("Противоречие: ${crit.detectedContradiction}", style = MaterialTheme.typography.labelMedium, color = StatusReviewRed)
                                    }
                                    Text("Рекомендация: ${crit.improvementSuggestion}", style = MaterialTheme.typography.labelMedium, color = StatusCanonGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Mark response as unsuccessful / add remark
    messageForRemark?.let { targetMsg ->
        var remarkText by remember { mutableStateOf(targetMsg.userRemark) }
        AlertDialog(
            onDismissRequest = { messageForRemark = null },
            title = { Text("Замечание к ответу персонажа") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Отметьте, в чём ответ нарушил характер, знания или манеру речи персонажа:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                    OutlinedTextField(
                        value = remarkText,
                        onValueChange = { remarkText = it },
                        label = { Text("Описание замечания") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleUnsuccessful(targetMsg.id, remarkText.ifBlank { "Ответ отмечен автором как неточный" })
                        messageForRemark = null
                    }
                ) { Text("Сохранить замечание") }
            },
            dismissButton = {
                TextButton(onClick = { messageForRemark = null }) { Text("Отмена") }
            }
        )
    }

    // Dialog: Before & After ("До и после") Character Card Change Proposal
    if (showProposalDiffDialog && latestReport != null) {
        var editableProposedValue by remember(latestReport) {
            mutableStateOf(latestReport.proposedCardNewValue)
        }
        AlertDialog(
            onDismissRequest = { showProposalDiffDialog = false },
            title = { Text("Сравнение «До и после»: ${activeChar.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "ИИ не изменяет карточку персонажа автоматически. Проверьте сравнение и подтвердите или отклоните обновление:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                    Surface(
                        color = BurgundySurfaceElevated,
                        border = BorderStroke(1.dp, BurgundyCardBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("ДО (текущее значение в карточке):", style = MaterialTheme.typography.labelMedium, color = AmberGoldSecondary)
                            Text(
                                text = latestReport.proposedCardOldValue.ifBlank { "(не заполнено)" },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextIvoryPrimary
                            )
                        }
                    }
                    OutlinedTextField(
                        value = editableProposedValue,
                        onValueChange = { editableProposedValue = it },
                        label = { Text("ПОСЛЕ (предлагаемое дополнение карточки)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyCardProposal(activeChar, latestReport.proposedCardField, editableProposedValue)
                        showProposalDiffDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCanonGreen)
                ) {
                    Text("Принять изменение")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProposalDiffDialog = false }) {
                    Text("Отклонить", color = StatusReviewRed)
                }
            }
        )
    }

    // Dialog: Compare current evaluation with previous evaluation
    if (showComparisonDialog && latestReport != null && previousReport != null) {
        AlertDialog(
            onDismissRequest = { showComparisonDialog = false },
            title = { Text("Сравнение результатов тестирования") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Предыдущий общий балл: ${String.format(java.util.Locale.US, "%.1f", previousReport.overallScore)} / 10",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedRose
                    )
                    Text(
                        "Текущий общий балл: ${String.format(java.util.Locale.US, "%.1f", latestReport.overallScore)} / 10",
                        style = MaterialTheme.typography.titleMedium,
                        color = StatusCanonGreen,
                        fontWeight = FontWeight.Bold
                    )
                    val delta = latestReport.overallScore - previousReport.overallScore
                    Text(
                        text = if (delta >= 0) "Динамика: +${String.format(java.util.Locale.US, "%.1f", delta)} балла после уточнения карточки и реплик."
                        else "Динамика: ${String.format(java.util.Locale.US, "%.1f", delta)} балла.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberGoldSecondary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showComparisonDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

/**
 * Reusable Avatar Circle that decodes Base64 bitmap if available or renders a clean initial placeholder.
 */
@Composable
fun CharacterAvatarCircle(
    name: String,
    avatarBase64: String,
    sizeDp: Int = 48,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(avatarBase64) {
        if (avatarBase64.isBlank()) null
        else try {
            val clean = avatarBase64.substringAfter("base64,", avatarBase64)
            val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    Surface(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape),
        shape = CircleShape,
        color = CrimsonPrimary.copy(alpha = 0.20f),
        border = BorderStroke(1.5.dp, CrimsonPrimary.copy(alpha = 0.7f))
    ) {
        if (bitmap != null) {
            CharacterAvatarThumbnail(
                name = name,
                avatarBase64 = avatarBase64,
                size = sizeDp.dp
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                val initials = name.trim().split(Regex("\\s+"))
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.take(1).uppercase() }
                    .ifEmpty { "?" }
                Text(
                    text = initials,
                    style = if (sizeDp >= 56) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                    color = CrimsonPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
