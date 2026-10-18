package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.KnowledgeDocumentWithDetails
import com.example.data.preferences.StudioPreferences
import com.example.data.remote.AiProviderType
import com.example.data.remote.SupportedAiModels
import com.example.data.remote.SupportedGeminiModels
import com.example.data.repository.AiChangeProposal
import com.example.data.repository.CitedSourceSnippet
import com.example.ui.components.MarkdownMessageRenderer
import com.example.ui.components.copyTextToClipboard
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.BurgundyCardBorder
import com.example.ui.theme.BurgundySurfaceContainer
import com.example.ui.theme.BurgundySurfaceElevated
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.StatusCanonGreen
import com.example.ui.theme.TextIvoryPrimary
import com.example.ui.theme.TextMutedRose
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeminiWorkspaceScreen(
    prefs: StudioPreferences,
    sessions: List<ChatSessionEntity>,
    activeSessionId: Long,
    messages: List<ChatMessageEntity>,
    allDocuments: List<KnowledgeDocumentWithDetails>,
    inputDraft: String,
    searchQuery: String,
    searchResults: List<ChatMessageEntity>,
    isGenerating: Boolean,
    errorBanner: String?,
    attachedFileName: String?,
    onInputDraftChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectSession: (Long) -> Unit,
    onCreateSession: (String) -> Unit,
    onRenameSession: (Long, String) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectProvider: (AiProviderType) -> Unit = {},
    onToggleRedWorldContext: (ChatSessionEntity, Boolean) -> Unit,
    onToggleDocInContext: (ChatSessionEntity, Long) -> Unit,
    onSetAssistantMode: (ChatSessionEntity, String) -> Unit,
    onAttachImage: (String, String) -> Unit,
    onClearAttachment: () -> Unit,
    onSendMessage: (String?) -> Unit,
    onStopGeneration: () -> Unit,
    onRegenerateLast: () -> Unit,
    onConfirmAiProposal: (ChatMessageEntity, AiChangeProposal) -> Unit,
    onOpenCitedDocument: (Long) -> Unit,
    onOpenApiKeySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeSession = sessions.firstOrNull { it.id == activeSessionId } ?: sessions.firstOrNull()
    var showContextDocsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showAssistantQuickTools by remember { mutableStateOf(false) }

    val json = remember { Json { ignoreUnknownKeys = true } }

    // Zero-permission Photo Picker for multimodal image attachment
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                        onAttachImage(b64, uri.lastPathSegment ?: "image_attachment.jpg")
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    val selectedDocIdsSet = remember(activeSession?.selectedDocumentIdsCsv) {
        activeSession?.selectedDocumentIdsCsv
            ?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 1. Top Session Selector + Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    val isSelected = session.id == activeSession?.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSession(session.id) },
                        label = {
                            Text(
                                text = session.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = TextIvoryPrimary
                        ),
                        modifier = Modifier.testTag("chat_session_chip_${session.id}")
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showNewChatDialog = true },
                    modifier = Modifier.testTag("create_chat_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Новый чат", tint = CrimsonPrimary)
                }
                if (activeSession != null) {
                    IconButton(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier.testTag("rename_chat_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Переименовать чат", tint = TextMutedRose)
                    }
                    IconButton(
                        onClick = { onDeleteSession(activeSession.id) },
                        modifier = Modifier.testTag("delete_chat_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить чат", tint = TextMutedRose)
                    }
                }
                IconButton(
                    onClick = { showSearchBar = !showSearchBar },
                    modifier = Modifier.testTag("search_chat_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Поиск по истории", tint = AmberGoldSecondary)
                }
            }
        }

        val activeProvider = prefs.activeProviderType
        val activeModelId = prefs.activeProviderTextModel
        val activeProviderModels = SupportedAiModels.modelsForProvider(activeProvider)
        val customModelId = prefs.customModelIdFor(activeProvider).trim()

        // 2. Provider + Model Selector + RedWorld RAG Context + Assistant Mode Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
            border = BorderStroke(1.dp, BurgundyCardBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AiProviderType.entries.forEach { provider ->
                        val isProvSelected = provider == activeProvider
                        FilterChip(
                            selected = isProvSelected,
                            onClick = { onSelectProvider(provider) },
                            label = {
                                Text(
                                    text = provider.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isProvSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonPrimary.copy(alpha = 0.28f),
                                selectedLabelColor = TextIvoryPrimary
                            ),
                            modifier = Modifier.testTag("workspace_provider_chip_${provider.id.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (customModelId.isNotEmpty()) {
                            item(key = "custom_$customModelId") {
                                FilterChip(
                                    selected = true,
                                    onClick = onOpenApiKeySettings,
                                    label = {
                                        Text(
                                            text = "$customModelId (Свой ID)",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                )
                            }
                        }
                        items(activeProviderModels, key = { it.modelId }) { spec ->
                            val selected = customModelId.isEmpty() && activeModelId.equals(spec.modelId, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectModel(spec.modelId) },
                                label = { Text(spec.displayName, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenApiKeySettings,
                        modifier = Modifier.testTag("open_api_key_settings_button")
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = "Настроить ИИ и API-ключ",
                            tint = if (prefs.isActiveProviderKeyConfigured) StatusCanonGreen else AmberGoldSecondary
                        )
                    }
                }

                if (activeSession != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = activeSession.isRedWorldContextEnabled,
                                onCheckedChange = { onToggleRedWorldContext(activeSession, it) },
                                modifier = Modifier.testTag("toggle_redworld_context_switch")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (activeSession.isRedWorldContextEnabled)
                                    "Контекст RedWorld: ${selectedDocIdsSet.size.coerceAtLeast(1)} ист."
                                else "Контекст RedWorld выключен",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextIvoryPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(
                                onClick = { showContextDocsDialog = true },
                                label = { Text("Источники", style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                            AssistChip(
                                onClick = { showAssistantQuickTools = !showAssistantQuickTools },
                                label = { Text("ИИ-Куратор", style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = AmberGoldSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Optional AI Curator Quick Prompts & Mode Selector (Section 11)
        AnimatedVisibility(visible = showAssistantQuickTools && activeSession != null) {
            if (activeSession != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = BurgundySurfaceElevated),
                    border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Режимы AI-Помощника RedWorld (канон защищён от авто-перезаписи):",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberGoldSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            listOf(
                                "CONSULTATION" to "Консультация",
                                "DRAFT" to "Черновик",
                                "PROPOSAL" to "Предложение изменений",
                                "APPLY_CONFIRMED" to "С подтверждением"
                            ).forEach { (modeCode, modeLabel) ->
                                FilterChip(
                                    selected = activeSession.assistantMode == modeCode,
                                    onClick = { onSetAssistantMode(activeSession, modeCode) },
                                    label = { Text(modeLabel, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    onSendMessage("Проанализируй карточки Виктора Корвина и Елены Ростовой на соответствие правилам мира и найди возможные противоречия.")
                                },
                                label = { Text("Поиск противоречий", style = MaterialTheme.typography.labelMedium) }
                            )
                            AssistChip(
                                onClick = {
                                    onSendMessage("Предложи изменение в карточку персонажа на основе последних событий в Баре на Зелёной улице (в режиме черновика для подтверждения).")
                                },
                                label = { Text("Предложить правку карточки", style = MaterialTheme.typography.labelMedium) }
                            )
                            AssistChip(
                                onClick = {
                                    onSendMessage("Создай описание новой сцены в Красном особняке с учётом текущих состояний персонажей.")
                                },
                                label = { Text("Черновик сцены", style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }
        }

        // Optional Search History Bar
        AnimatedVisibility(visible = showSearchBar) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Поиск по всей истории сообщений...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true
                )
                if (searchResults.isNotEmpty()) {
                    Text(
                        text = "Найдено совпадений в истории: ${searchResults.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberGoldSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Error Banner if API call failed
        if (!errorBanner.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B0A14)),
                border = BorderStroke(1.dp, CrimsonPrimary)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextIvoryPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenApiKeySettings) {
                        Text("Настроить ключ", color = AmberGoldSecondary)
                    }
                }
            }
        }

        // 3. Messages List
        val displayedMessages = if (showSearchBar && searchQuery.isNotBlank()) searchResults else messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("gemini_messages_list"),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(displayedMessages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                val citedList = remember(msg.citedSourcesJson) {
                    try {
                        json.decodeFromString<List<CitedSourceSnippet>>(msg.citedSourcesJson)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                val proposal = remember(msg.proposedChangeJson) {
                    if (msg.proposedChangeJson.isBlank()) null
                    else try {
                        json.decodeFromString<AiChangeProposal>(msg.proposedChangeJson)
                    } catch (_: Exception) {
                        null
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (isUser) 28.dp else 0.dp,
                            end = if (isUser) 0.dp else 16.dp
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) BurgundySurfaceElevated else BurgundySurfaceContainer
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isUser) CrimsonPrimary.copy(alpha = 0.55f) else BurgundyCardBorder
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUser) "Вы" else SupportedAiModels.resolveProviderAndModelLabel(msg.modelUsed, activeProvider),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isUser) CrimsonPrimary else AmberGoldSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                IconButton(
                                    onClick = { copyTextToClipboard(context, msg.content) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Скопировать сообщение",
                                        tint = TextMutedRose,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        if (msg.attachmentSummary.isNotBlank()) {
                            Text(
                                text = "📎 Вложение: ${msg.attachmentSummary}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberGoldSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        MarkdownMessageRenderer(markdownText = msg.content)

                        // Cited RAG Sources (clickable to jump to the document!)
                        if (citedList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Подтверждённые источники из RedWorld Knowledge:",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMutedRose
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                citedList.forEach { src ->
                                    AssistChip(
                                        onClick = { onOpenCitedDocument(src.documentId) },
                                        label = {
                                            Text(
                                                "${src.documentTitle}: ${src.sectionTitle}",
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = CrimsonPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // AI Proposal Confirmation Card (Section 11: Never overwrite Canon without confirmation)
                        if (proposal != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = ObsidianSurface,
                                border = BorderStroke(1.dp, AmberGoldSecondary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Предложение изменений ИИ: ${proposal.proposalTitle}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = AmberGoldSecondary
                                    )
                                    Text(
                                        text = "Объект: ${proposal.targetEntityName} • Статус: Черновик (требует подтверждения)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMutedRose
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (msg.isProposalApplied) {
                                        Text(
                                            text = "✓ Изменение подтверждено и записано в историю версий",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = StatusCanonGreen
                                        )
                                    } else {
                                        Button(
                                            onClick = { onConfirmAiProposal(msg, proposal) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                            modifier = Modifier.testTag("confirm_ai_proposal_button")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Подтвердить и сохранить новую версию")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Generation Progress / Stop / Regenerate Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isGenerating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = CrimsonPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini анализирует выбранные фрагменты и генерирует ответ...",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberGoldSecondary
                    )
                }
                OutlinedButton(
                    onClick = onStopGeneration,
                    modifier = Modifier.testTag("stop_generation_button")
                ) {
                    Icon(Icons.Default.StopCircle, contentDescription = null, tint = CrimsonPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Стоп")
                }
            } else if (messages.isNotEmpty()) {
                Text(
                    text = if (activeSession?.isRedWorldContextEnabled == true)
                        "RAG-фильтр активен: передаются только релевантные фрагменты"
                    else "Отправка без документов проекта",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMutedRose
                )
                TextButton(
                    onClick = onRegenerateLast,
                    modifier = Modifier.testTag("regenerate_response_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Повторить ответ")
                }
            }
        }

        // Attachment indicator pill
        if (!attachedFileName.isNullOrBlank()) {
            Surface(
                color = BurgundySurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Прикреплено: $attachedFileName",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberGoldSecondary
                    )
                    IconButton(onClick = onClearAttachment, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Убрать вложение", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // 5. Message Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.testTag("attach_media_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Прикрепить изображение",
                    tint = AmberGoldSecondary
                )
            }

            OutlinedTextField(
                value = inputDraft,
                onValueChange = onInputDraftChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("gemini_prompt_input"),
                placeholder = { Text("Введите запрос к Gemini или вопрос по канону RedWorld...") },
                maxLines = 4,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { onSendMessage(null) },
                enabled = inputDraft.isNotBlank() && !isGenerating,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("gemini_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
            }
        }
    }

    // Dialog: Select which RedWorld documents are permitted in this chat session
    if (showContextDocsDialog && activeSession != null) {
        AlertDialog(
            onDismissRequest = { showContextDocsDialog = false },
            title = { Text("Разрешённые документы для контекста") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Выберите, какие именно источники RedWorld Knowledge разрешено использовать в этом диалоге. В запрос будут переданы только релевантные фрагменты (RAG):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                    allDocuments.forEach { docWithDetails ->
                        val doc = docWithDetails.document
                        val checked = selectedDocIdsSet.contains(doc.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleDocInContext(activeSession, doc.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggleDocInContext(activeSession, doc.id) }
                            )
                            Column {
                                Text(doc.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${doc.folderName} • Фрагментов RAG: ${docWithDetails.chunks.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMutedRose
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextDocsDialog = false }) {
                    Text("Готово")
                }
            }
        )
    }

    // Dialog: Create New Chat Session
    if (showNewChatDialog) {
        var newTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Новый диалог Gemini") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Название диалога") },
                    placeholder = { Text("Например: Сцена в Красном особняке") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateSession(newTitle.ifBlank { "Диалог RedWorld" })
                        showNewChatDialog = false
                    }
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Dialog: Rename Chat Session
    if (showRenameDialog && activeSession != null) {
        var editedTitle by remember(activeSession.title) { mutableStateOf(activeSession.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Переименовать диалог") },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Новое название") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameSession(activeSession.id, editedTitle)
                        showRenameDialog = false
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Отмена") }
            }
        )
    }
}
