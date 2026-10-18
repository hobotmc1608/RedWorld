package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AiDebugExplanation
import com.example.data.local.CharacterEntity
import com.example.data.local.LocationEntity
import com.example.data.local.PlotlineEntity
import com.example.data.local.ProposedCardFieldDiff
import com.example.data.local.RoleplayEvaluationReport
import com.example.data.local.WorldEventEntity
import com.example.data.local.WorldRuleEntity
import com.example.ui.RoleplayTestMessage
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.StatusAssumptionAmber
import com.example.ui.theme.StatusCanonGreen
import com.example.ui.theme.StatusReviewRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorldSimulationAndRoleplayScreen(
    characters: List<CharacterEntity>,
    locations: List<LocationEntity>,
    worldRules: List<WorldRuleEntity>,
    plotlines: List<PlotlineEntity>,
    simulationDrafts: List<WorldEventEntity>,
    isSimulatingEvents: Boolean,
    roleplayCharacterId: Long?,
    roleplayMessages: List<RoleplayTestMessage>,
    isRoleplayGenerating: Boolean,
    lastEvaluationReport: RoleplayEvaluationReport?,
    lastDebugExplanation: AiDebugExplanation?,
    onGenerateSimulationEvents: (scale: String, location: String, participants: String, surprise: String, plotline: String, count: Int) -> Unit,
    onConfirmDraftToHistory: (WorldEventEntity) -> Unit,
    onDeleteDraftEvent: (WorldEventEntity) -> Unit,
    onUpdateDraftEvent: (WorldEventEntity) -> Unit,
    onAddDraftToPlotline: (WorldEventEntity, String) -> Unit,
    onSelectRoleplayCharacter: (Long) -> Unit,
    onStartNewRoleplaySession: () -> Unit,
    onSendRoleplayMessage: (String) -> Unit,
    onMarkMessageFlawed: (String, String) -> Unit,
    onEvaluateRoleplay10Criteria: () -> Unit,
    onApplyProposedDiff: (Long, ProposedCardFieldDiff) -> Unit,
    onRejectProposedDiff: (String) -> Unit
) {
    var activeMode by remember { mutableIntStateOf(0) } // 0 = Random Events, 1 = Character Roleplay Test
    var showDebugPanel by remember { mutableStateOf(false) }

    // Simulation form states
    var scale by remember { mutableStateOf("Локальное") }
    var selectedLocation by remember { mutableStateOf("Весь мир") }
    var participantsInput by remember {
        mutableStateOf(characters.take(2).joinToString(", ") { it.name })
    }
    var surpriseLevel by remember { mutableStateOf("Средний") }
    var selectedPlotline by remember {
        mutableStateOf(plotlines.firstOrNull()?.title.orEmpty())
    }
    var variantsCount by remember { mutableIntStateOf(2) }

    // Confirmation & edit dialogs for draft events
    var draftToConfirm by remember { mutableStateOf<WorldEventEntity?>(null) }
    var draftToEdit by remember { mutableStateOf<WorldEventEntity?>(null) }

    // Roleplay states
    var roleplayInput by remember { mutableStateOf("") }
    var messageToFlag by remember { mutableStateOf<RoleplayTestMessage?>(null) }
    var flagRemarkText by remember { mutableStateOf("") }

    LaunchedEffect(characters) {
        if (roleplayCharacterId == null && characters.isNotEmpty()) {
            onSelectRoleplayCharacter(characters.first().id)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Globe Mode Switcher
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Глобус симуляции мира",
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Симуляция мира и Тестирование",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Генерация черновиков событий по запросу и проверка отыгрыша персонажей",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { showDebugPanel = !showDebugPanel },
                            modifier = Modifier.testTag("toggle_debug_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Режим отладки и объяснения результатов",
                                tint = if (showDebugPanel) AmberGoldSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = activeMode == 0,
                            onClick = { activeMode = 0 },
                            label = { Text("1. Случайные события") },
                            leadingIcon = {
                                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonPrimary.copy(alpha = 0.22f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("globe_mode_random_events")
                        )

                        FilterChip(
                            selected = activeMode == 1,
                            onClick = { activeMode = 1 },
                            label = { Text("2. Тест персонажа") },
                            leadingIcon = {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonPrimary.copy(alpha = 0.22f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("globe_mode_character_test")
                        )
                    }
                }
            }
        }

        // Collapsible Debug & Explainability Mode (Section 19)
        item {
            AnimatedVisibility(visible = showDebugPanel) {
                DebugExplanationCard(
                    debug = lastDebugExplanation,
                    onClose = { showDebugPanel = false }
                )
            }
        }

        if (activeMode == 0) {
            // ================= MODE 1: RANDOM EVENT SIMULATION =================
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Параметры генерации событий",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ИИ учитывает правила мира (${worldRules.size}), карточки персонажей (${characters.size}), отношения и сюжетные линии. События создаются в статусе «Черновик».",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Scale selector
                        Text("Масштаб события:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Камерное / Диалог", "Локальное", "Городское", "Масштаб всего мира").forEach { item ->
                                FilterChip(
                                    selected = scale == item,
                                    onClick = { scale = item },
                                    label = { Text(item) }
                                )
                            }
                        }

                        // Location selector
                        Text("Локация или весь мир:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val locOptions = listOf("Весь мир") + locations.map { it.name }
                            locOptions.forEach { locName ->
                                FilterChip(
                                    selected = selectedLocation == locName,
                                    onClick = { selectedLocation = locName },
                                    label = { Text(locName) }
                                )
                            }
                        }

                        // Participants
                        OutlinedTextField(
                            value = participantsInput,
                            onValueChange = { participantsInput = it },
                            label = { Text("Участники события (через запятую)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sim_participants_input"),
                            singleLine = true
                        )

                        // Surprise level & variants count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Уровень неожиданности:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Низкий", "Средний", "Высокий").forEach { lvl ->
                                        FilterChip(
                                            selected = surpriseLevel == lvl,
                                            onClick = { surpriseLevel = lvl },
                                            label = { Text(lvl, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                            Column {
                                Text("Вариантов:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(1, 2, 3).forEach { cnt ->
                                        FilterChip(
                                            selected = variantsCount == cnt,
                                            onClick = { variantsCount = cnt },
                                            label = { Text("$cnt") }
                                        )
                                    }
                                }
                            }
                        }

                        // Plotline link
                        OutlinedTextField(
                            value = selectedPlotline,
                            onValueChange = { selectedPlotline = it },
                            label = { Text("Связь с сюжетной линией") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                onGenerateSimulationEvents(
                                    scale,
                                    selectedLocation,
                                    participantsInput,
                                    surpriseLevel,
                                    selectedPlotline,
                                    variantsCount
                                )
                            },
                            enabled = !isSimulatingEvents,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_simulation_events_button")
                        ) {
                            if (isSimulatingEvents) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Сгенерировать черновики событий")
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Черновики симуляции (${simulationDrafts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Сгенерированные события не попадают в каноническую историю до вашего явного подтверждения.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (simulationDrafts.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, tint = AmberGoldSecondary, modifier = Modifier.size(32.dp))
                            Text(
                                text = "Нет активных черновиков симуляции",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Задайте параметры выше и нажмите «Сгенерировать черновики событий», чтобы получить варианты сцен.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    onGenerateSimulationEvents(
                                        scale,
                                        selectedLocation,
                                        participantsInput,
                                        surpriseLevel,
                                        selectedPlotline,
                                        2
                                    )
                                }
                            ) {
                                Text("Создать 2 варианта сейчас")
                            }
                        }
                    }
                }
            } else {
                items(simulationDrafts, key = { it.id }) { draft ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, AmberGoldSecondary.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = AmberGoldSecondary.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "ЧЕРНОВИК СИМУЛЯЦИИ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberGoldSecondary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Место: ${draft.locationName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrimsonPrimary
                                )
                            }

                            Text(
                                text = draft.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = draft.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            Text("Участники: ${draft.participantNamesCsv.ifBlank { "Не указаны" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (draft.causes.isNotBlank()) {
                                Text("Причины: ${draft.causes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            if (draft.consequences.isNotBlank()) {
                                Text("Возможные последствия: ${draft.consequences}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            if (draft.relatedProjectLore.isNotBlank()) {
                                Text("Связанные сведения проекта: ${draft.relatedProjectLore}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (draft.contradictionWarnings.isNotBlank()) {
                                Surface(
                                    color = if (draft.contradictionWarnings.contains("Внимание")) StatusReviewRed.copy(alpha = 0.16f) else StatusCanonGreen.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (draft.contradictionWarnings.contains("Внимание")) StatusReviewRed else StatusCanonGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = draft.contradictionWarnings,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Actions: Принять (в историю), В сюжет, Редактировать, Альтернативы, Отклонить
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { draftToConfirm = draft },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusCanonGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("confirm_draft_event_${draft.id}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Принять в историю")
                                }

                                OutlinedButton(
                                    onClick = { onAddDraftToPlotline(draft, selectedPlotline) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Добавить в сюжет")
                                }

                                OutlinedButton(
                                    onClick = { draftToEdit = draft },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Редактировать")
                                }

                                OutlinedButton(
                                    onClick = {
                                        onGenerateSimulationEvents(
                                            scale,
                                            draft.locationName,
                                            draft.participantNamesCsv,
                                            "Высокий",
                                            draft.linkedPlotlinesCsv,
                                            1
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Альтернатива")
                                }

                                TextButton(
                                    onClick = { onDeleteDraftEvent(draft) }
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
        } else {
            // ================= MODE 2: CHARACTER ROLEPLAY TESTING & 10-CRITERIA EVALUATION =================
            val activeCharacter = characters.firstOrNull { it.id == roleplayCharacterId } ?: characters.firstOrNull()

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Выберите персонажа для тестового диалога:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            characters.forEach { char ->
                                val isSelected = char.id == activeCharacter?.id
                                Surface(
                                    color = if (isSelected) CrimsonPrimary.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, if (isSelected) CrimsonPrimary else MaterialTheme.colorScheme.outline),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .clickable { onSelectRoleplayCharacter(char.id) }
                                        .testTag("select_roleplay_char_${char.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CharacterAvatarThumbnail(
                                            name = char.name,
                                            avatarBase64 = char.avatarBase64,
                                            size = 34.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = char.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = char.currentLocationName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (activeCharacter != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    CharacterAvatarThumbnail(
                                        name = activeCharacter.name,
                                        avatarBase64 = activeCharacter.avatarBase64,
                                        size = 46.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = activeCharacter.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Характер: ${activeCharacter.personality.take(65)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Тестовый диалог изолирован и НЕ перезаписывает каноническую историю.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberGoldSecondary
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = onStartNewRoleplaySession,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.testTag("new_roleplay_session_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Новая сессия")
                                }
                            }
                        }
                    }
                }
            }

            // Dialogue history
            items(roleplayMessages, key = { it.id }) { msg ->
                val isChar = msg.sender == "CHARACTER"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isChar) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (msg.isMarkedFlawed) StatusReviewRed else if (isChar) CrimsonPrimary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isChar) (activeCharacter?.name ?: "Персонаж") else "Вы (Тестировщик)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isChar) CrimsonPrimary else AmberGoldSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            if (isChar) {
                                TextButton(
                                    onClick = {
                                        messageToFlag = msg
                                        flagRemarkText = msg.userRemark
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = "Отметить ответ как неудачный",
                                        tint = if (msg.isMarkedFlawed) StatusReviewRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (msg.isMarkedFlawed) "Отмечено как ошибка" else "Замечание",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (msg.isMarkedFlawed) StatusReviewRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (msg.isMarkedFlawed && msg.userRemark.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Замечание: ${msg.userRemark}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusReviewRed,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }

            // Message input + Evaluate Roleplay button
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = roleplayInput,
                                onValueChange = { roleplayInput = it },
                                label = { Text("Реплика или проверка знаний персонажа...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("roleplay_message_input")
                            )
                            IconButton(
                                onClick = {
                                    if (roleplayInput.isNotBlank()) {
                                        onSendRoleplayMessage(roleplayInput)
                                        roleplayInput = ""
                                    }
                                },
                                enabled = !isRoleplayGenerating && roleplayInput.isNotBlank(),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonPrimary)
                                    .testTag("roleplay_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Отправить реплику",
                                    tint = Color.White
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onEvaluateRoleplay10Criteria,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("evaluate_roleplay_quality_button")
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Анализ качества отыгрыша (10 критериев)")
                        }
                    }
                }
            }

            // 10-Criteria Evaluation Report & Before/After Card Diffs
            if (lastEvaluationReport != null) {
                item {
                    RoleplayEvaluationReportSection(
                        report = lastEvaluationReport,
                        onApplyDiff = { diff -> onApplyProposedDiff(lastEvaluationReport.characterId, diff) },
                        onRejectDiff = onRejectProposedDiff,
                        onRepeatTest = onStartNewRoleplaySession
                    )
                }
            }
        }
    }

    // Confirm moving draft event to canonical history
    val pendingConfirm = draftToConfirm
    if (pendingConfirm != null) {
        AlertDialog(
            onDismissRequest = { draftToConfirm = null },
            title = { Text("Перенести событие в каноническую историю?") },
            text = {
                Text("Черновик «${pendingConfirm.title}» получит статус «Канон» и будет записан в раздел «История событий».")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmDraftToHistory(pendingConfirm)
                        draftToConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCanonGreen)
                ) {
                    Text("Подтвердить и перенести")
                }
            },
            dismissButton = {
                TextButton(onClick = { draftToConfirm = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Edit draft event dialog
    val editingDraft = draftToEdit
    if (editingDraft != null) {
        var title by remember(editingDraft) { mutableStateOf(editingDraft.title) }
        var desc by remember(editingDraft) { mutableStateOf(editingDraft.description) }
        var loc by remember(editingDraft) { mutableStateOf(editingDraft.locationName) }
        var parts by remember(editingDraft) { mutableStateOf(editingDraft.participantNamesCsv) }
        var causes by remember(editingDraft) { mutableStateOf(editingDraft.causes) }
        var conseq by remember(editingDraft) { mutableStateOf(editingDraft.consequences) }

        AlertDialog(
            onDismissRequest = { draftToEdit = null },
            title = { Text("Редактировать черновик события") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") })
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Место") })
                    OutlinedTextField(value = parts, onValueChange = { parts = it }, label = { Text("Участники") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, minLines = 2)
                    OutlinedTextField(value = causes, onValueChange = { causes = it }, label = { Text("Причины") })
                    OutlinedTextField(value = conseq, onValueChange = { conseq = it }, label = { Text("Последствия") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDraftEvent(
                            editingDraft.copy(
                                title = title,
                                locationName = loc,
                                participantNamesCsv = parts,
                                description = desc,
                                causes = causes,
                                consequences = conseq
                            )
                        )
                        draftToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Сохранить черновик")
                }
            },
            dismissButton = {
                TextButton(onClick = { draftToEdit = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Flag flawed message dialog
    val flaggingMsg = messageToFlag
    if (flaggingMsg != null) {
        AlertDialog(
            onDismissRequest = { messageToFlag = null },
            title = { Text("Отметить ответ как неудачный") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Укажите, в чём ответ противоречит карточке персонажа. Карточка НЕ будет изменена автоматически.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = flagRemarkText,
                        onValueChange = { flagRemarkText = it },
                        label = { Text("Замечание к ответу") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMarkMessageFlawed(flaggingMsg.id, flagRemarkText)
                        messageToFlag = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusReviewRed)
                ) {
                    Text("Сохранить замечание")
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToFlag = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun RoleplayEvaluationReportSection(
    report: RoleplayEvaluationReport,
    onApplyDiff: (ProposedCardFieldDiff) -> Unit,
    onRejectDiff: (String) -> Unit,
    onRepeatTest: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, CrimsonPrimary),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = report.disclaimer,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
                Surface(
                    color = CrimsonPrimary.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${report.overallScore} / 10",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberGoldSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (report.insufficientCardDataWarning.isNotBlank()) {
                Surface(
                    color = StatusAssumptionAmber.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, StatusAssumptionAmber),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = report.insufficientCardDataWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // 10 Criteria Cards
            report.criteria.forEach { item ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.criterionTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.scoreOutOf10}/10",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (item.scoreOutOf10 >= 8) StatusCanonGreen else AmberGoldSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text("Объяснение: ${item.explanation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("Основание карточки: ${item.cardBasis}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.exampleQuote.isNotBlank()) {
                            Text("Пример ответа: «${item.exampleQuote}»", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                        }
                        if (item.detectedContradiction.isNotBlank() && item.detectedContradiction != "Нет") {
                            Text("Противоречие: ${item.detectedContradiction}", style = MaterialTheme.typography.labelSmall, color = StatusReviewRed)
                        }
                        if (item.improvementSuggestion.isNotBlank()) {
                            Text("Рекомендация: ${item.improvementSuggestion}", style = MaterialTheme.typography.labelSmall, color = AmberGoldSecondary)
                        }
                    }
                }
            }

            // Before / After Proposed Changes to Character Card
            if (report.proposedFieldChanges.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "Предложенные улучшения карточки («До и После»)",
                    style = MaterialTheme.typography.titleSmall,
                    color = AmberGoldSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Изменения применяются к карточке персонажа только после вашего подтверждения.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                report.proposedFieldChanges.forEach { diff ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, AmberGoldSecondary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Поле: ${diff.fieldLabel}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Причина: ${diff.reason}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("ДО:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(diff.beforeValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("ПОСЛЕ:", style = MaterialTheme.typography.labelSmall, color = StatusCanonGreen)
                                    Text(diff.afterValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApplyDiff(diff) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusCanonGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Принять изменение")
                                }
                                OutlinedButton(
                                    onClick = { onRejectDiff(diff.id) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Отклонить")
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onRepeatTest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Повторить тест / Начать новую сессию")
            }
        }
    }
}

@Composable
private fun DebugExplanationCard(
    debug: AiDebugExplanation?,
    onClose: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, AmberGoldSecondary),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = AmberGoldSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Режим отладки и объяснения результатов",
                        style = MaterialTheme.typography.titleSmall,
                        color = AmberGoldSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть отладку", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (debug == null) {
                Text(
                    text = "Запустите генерацию события или отправьте реплику в тестировании персонажа, чтобы увидеть проверяемый отчёт использованного контекста.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text("Краткое объяснение: ${debug.conciseExplanation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                Text("• Использованные карточки: ${debug.usedCharacterCards.joinToString(", ").ifEmpty { "нет" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("• Учтённые правила мира: ${debug.appliedWorldRules.joinToString(", ").ifEmpty { "нет" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("• События истории в контексте: ${debug.usedHistoryEvents.joinToString(", ").ifEmpty { "нет" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("• Повлиявшие отношения: ${debug.influencingRelationships.joinToString("; ").ifEmpty { "нет" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("• Применённые ограничения: ${debug.appliedConstraints.joinToString("; ").ifEmpty { "стандартные" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (debug.missingDataWarnings.isNotEmpty()) {
                    Text("• Отсутствующие данные: ${debug.missingDataWarnings.joinToString("; ")}", style = MaterialTheme.typography.labelSmall, color = StatusAssumptionAmber)
                }
                if (debug.detectedContradictions.isNotEmpty()) {
                    Text("• Обнаруженные противоречия: ${debug.detectedContradictions.joinToString("; ")}", style = MaterialTheme.typography.labelSmall, color = StatusReviewRed)
                }
            }
        }
    }
}
