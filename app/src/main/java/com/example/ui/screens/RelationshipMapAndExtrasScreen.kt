package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.CharacterEntity
import com.example.data.local.LocationEntity
import com.example.data.local.RelationshipEntity
import com.example.data.local.VersionSnapshotEntity
import com.example.data.preferences.StudioPreferences
import com.example.data.remote.DriveRemoteFileMetadata
import com.example.data.remote.SyncConflictInfo
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.BurgundyCardBorder
import com.example.ui.theme.BurgundySurfaceContainer
import com.example.ui.theme.BurgundySurfaceElevated
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.StatusCanonGreen
import com.example.ui.theme.TextIvoryPrimary
import com.example.ui.theme.TextMutedRose
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val RELATION_TYPES = listOf(
    "Живёт в",
    "Работает в",
    "Знаком с",
    "Состоит в отношениях с",
    "Участвует в событии",
    "Событие происходит в локации",
    "Документ описывает персонажа",
    "Изображение относится к персонажу",
    "Правило регулирует сцену",
    "Событие изменяет отношения",
    "Локация связана с локацией"
)

private val NODE_TYPES = listOf(
    "ALL" to "Все узлы",
    "CHARACTER" to "Персонажи",
    "LOCATION" to "Локации",
    "EVENT" to "События",
    "DOCUMENT" to "Документы",
    "RULE" to "Правила",
    "ITEM" to "Предметы",
    "IMAGE" to "Изображения"
)

/**
 * Interactive Visual Relationship Map (Section 10):
 * Real database-backed graph with pan/zoom gestures, node dragging, filtering by node type or
 * character ego-network, automatic circular/radial layout, and relationship creation/deletion.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualRelationshipMapScreen(
    relationships: List<RelationshipEntity>,
    characters: List<CharacterEntity>,
    locations: List<LocationEntity>,
    onAddRelationship: (RelationshipEntity) -> Unit,
    onDeleteRelationship: (Long) -> Unit,
    onOpenCharacterCard: (Long) -> Unit,
    onOpenDocumentCard: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedNodeTypeFilter by remember { mutableStateOf("ALL") }
    var focusedCharacterName by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedNodeKey by remember { mutableStateOf<String?>(null) }
    var showAddRelationDialog by remember { mutableStateOf(false) }

    // Node position map in canvas coordinates
    val nodePositions = remember { mutableStateMapOf<String, Offset>() }

    val filteredRelationships = remember(relationships, selectedNodeTypeFilter, focusedCharacterName, searchQuery) {
        relationships.filter { rel ->
            val matchesType = selectedNodeTypeFilter == "ALL" ||
                rel.sourceType == selectedNodeTypeFilter ||
                rel.targetType == selectedNodeTypeFilter
            val matchesFocus = focusedCharacterName == null ||
                rel.sourceLabel.equals(focusedCharacterName, ignoreCase = true) ||
                rel.targetLabel.equals(focusedCharacterName, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                rel.sourceLabel.contains(searchQuery, ignoreCase = true) ||
                rel.targetLabel.contains(searchQuery, ignoreCase = true) ||
                rel.relationType.contains(searchQuery, ignoreCase = true)
            matchesType && matchesFocus && matchesSearch
        }
    }

    // Build distinct nodes from filtered relationships
    val distinctNodes = remember(filteredRelationships) {
        val list = mutableListOf<Triple<String, String, Long>>() // key, label, entityId
        filteredRelationships.forEach { r ->
            val sKey = "${r.sourceType}:${r.sourceLabel}"
            val tKey = "${r.targetType}:${r.targetLabel}"
            if (list.none { it.first == sKey }) list.add(Triple(sKey, r.sourceLabel, r.sourceId))
            if (list.none { it.first == tKey }) list.add(Triple(tKey, r.targetLabel, r.targetId))
        }
        list
    }

    // Ensure every node has a well-spaced readable position (Auto-layout)
    fun runAutoLayout() {
        val count = distinctNodes.size.coerceAtLeast(1)
        val centerX = 480f
        val centerY = 380f
        val radius = 250f
        distinctNodes.forEachIndexed { idx, (key, _, _) ->
            val angle = (2.0 * Math.PI * idx) / count
            nodePositions[key] = Offset(
                x = centerX + (radius * cos(angle)).toFloat(),
                y = centerY + (radius * sin(angle)).toFloat()
            )
        }
        scale = 1f
        panOffset = Offset.Zero
    }

    if (nodePositions.isEmpty() && distinctNodes.isNotEmpty()) {
        runAutoLayout()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Controls Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Поиск узла или связи...") },
                singleLine = true
            )
            IconButton(
                onClick = { runAutoLayout() },
                modifier = Modifier.testTag("map_auto_layout_button")
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Авто-раскладка", tint = AmberGoldSecondary)
            }
            Button(
                onClick = { showAddRelationDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.testTag("map_add_relation_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Новая связь")
            }
        }

        // Node Type & Character Ego Filter Chips
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NODE_TYPES.forEach { (typeCode, typeLabel) ->
                FilterChip(
                    selected = selectedNodeTypeFilter == typeCode && focusedCharacterName == null,
                    onClick = {
                        selectedNodeTypeFilter = typeCode
                        focusedCharacterName = null
                    },
                    label = { Text(typeLabel, style = MaterialTheme.typography.labelMedium) }
                )
            }
            characters.forEach { ch ->
                FilterChip(
                    selected = focusedCharacterName == ch.name,
                    onClick = {
                        focusedCharacterName = if (focusedCharacterName == ch.name) null else ch.name
                    },
                    label = { Text("Связи: ${ch.name}", style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        // Interactive Graph Canvas
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("visual_relationship_map_canvas"),
            colors = CardDefaults.cardColors(containerColor = ObsidianBackground),
            border = BorderStroke(1.dp, BurgundyCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(distinctNodes) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.55f, 2.4f)
                                panOffset += pan
                            }
                        }
                        .pointerInput(distinctNodes) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val localTouch = (change.position - panOffset) / scale
                                val hitNode = distinctNodes.firstOrNull { (key, _, _) ->
                                    val pos = nodePositions[key] ?: Offset(400f, 300f)
                                    hypot(localTouch.x - pos.x, localTouch.y - pos.y) < 65f
                                }
                                if (hitNode != null) {
                                    val oldPos = nodePositions[hitNode.first] ?: Offset(400f, 300f)
                                    nodePositions[hitNode.first] = oldPos + (dragAmount / scale)
                                    selectedNodeKey = hitNode.first
                                } else {
                                    panOffset += dragAmount
                                }
                            }
                        }
                        .pointerInput(distinctNodes) {
                            detectTapGestures { tapOffset ->
                                val localTouch = (tapOffset - panOffset) / scale
                                val hitNode = distinctNodes.firstOrNull { (key, _, _) ->
                                    val pos = nodePositions[key] ?: Offset(400f, 300f)
                                    hypot(localTouch.x - pos.x, localTouch.y - pos.y) < 60f
                                }
                                selectedNodeKey = hitNode?.first
                            }
                        }
                ) {
                    val labelPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 26f * scale
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    val edgePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#FDE68A")
                        textSize = 21f * scale
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    // Draw Edges
                    filteredRelationships.forEach { rel ->
                        val sKey = "${rel.sourceType}:${rel.sourceLabel}"
                        val tKey = "${rel.targetType}:${rel.targetLabel}"
                        val sPos = (nodePositions[sKey] ?: Offset(300f, 250f)) * scale + panOffset
                        val tPos = (nodePositions[tKey] ?: Offset(600f, 450f)) * scale + panOffset

                        drawLine(
                            color = Color(0x99E11D48),
                            start = sPos,
                            end = tPos,
                            strokeWidth = 3.5f * scale
                        )

                        val midX = (sPos.x + tPos.x) / 2f
                        val midY = (sPos.y + tPos.y) / 2f - 8f
                        drawContext.canvas.nativeCanvas.drawText(rel.relationType, midX, midY, edgePaint)
                    }

                    // Draw Nodes
                    distinctNodes.forEach { (key, label, _) ->
                        val type = key.substringBefore(':')
                        val rawPos = nodePositions[key] ?: Offset(400f, 300f)
                        val screenPos = rawPos * scale + panOffset
                        val nodeColor = when (type) {
                            "CHARACTER" -> Color(0xFFE11D48)
                            "LOCATION" -> Color(0xFFF59E0B)
                            "EVENT" -> Color(0xFF10B981)
                            "DOCUMENT" -> Color(0xFF6366F1)
                            "RULE" -> Color(0xFFA855F7)
                            else -> Color(0xFFFB7185)
                        }
                        val isSelected = selectedNodeKey == key
                        if (isSelected) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.3f),
                                radius = 42f * scale,
                                center = screenPos
                            )
                        }
                        drawCircle(
                            color = nodeColor,
                            radius = 30f * scale,
                            center = screenPos
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            screenPos.x,
                            screenPos.y + (50f * scale),
                            labelPaint
                        )
                    }
                }

                // Selected Node Inspection Bar at bottom of canvas
                if (selectedNodeKey != null) {
                    val hit = distinctNodes.firstOrNull { it.first == selectedNodeKey }
                    if (hit != null) {
                        val (key, label, entityId) = hit
                        val nodeType = key.substringBefore(':')
                        Surface(
                            color = BurgundySurfaceElevated.copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, CrimsonPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Узел: $label ($nodeType)", style = MaterialTheme.typography.titleSmall)
                                    Text("Перетаскивайте узел пальцем или откройте карточку объекта", style = MaterialTheme.typography.labelMedium, color = TextMutedRose)
                                }
                                if (nodeType == "CHARACTER") {
                                    Button(
                                        onClick = { onOpenCharacterCard(entityId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                                    ) {
                                        Text("Открыть карточку")
                                    }
                                } else if (nodeType == "DOCUMENT") {
                                    Button(
                                        onClick = { onOpenDocumentCard(entityId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                                    ) {
                                        Text("Открыть документ")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Database Relationship List below map
        LazyColumn(
            modifier = Modifier
                .height(145.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredRelationships, key = { it.id }) { rel ->
                Surface(
                    color = BurgundySurfaceContainer,
                    border = BorderStroke(1.dp, BurgundyCardBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${rel.sourceLabel}  ──[${rel.relationType}]──►  ${rel.targetLabel}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextIvoryPrimary
                            )
                            if (rel.description.isNotBlank()) {
                                Text(rel.description, style = MaterialTheme.typography.bodySmall, color = TextMutedRose)
                            }
                        }
                        IconButton(
                            onClick = { onDeleteRelationship(rel.id) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить связь", tint = TextMutedRose, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddRelationDialog) {
        var sourceLabel by remember { mutableStateOf(characters.firstOrNull()?.name ?: "Виктор Корвин") }
        var sourceType by remember { mutableStateOf("CHARACTER") }
        var relationType by remember { mutableStateOf(RELATION_TYPES.first()) }
        var targetLabel by remember { mutableStateOf(locations.firstOrNull()?.name ?: "Красный особняк") }
        var targetType by remember { mutableStateOf("LOCATION") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddRelationDialog = false },
            title = { Text("Создать связь на карте проекта") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sourceLabel, onValueChange = { sourceLabel = it }, label = { Text("Источник (Имя объекта)") })
                    OutlinedTextField(value = relationType, onValueChange = { relationType = it }, label = { Text("Тип связи (Живёт в, Работает в, Знаком с...)") })
                    OutlinedTextField(value = targetLabel, onValueChange = { targetLabel = it }, label = { Text("Цель (Имя объекта)") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Пояснение к связи") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sourceLabel.isNotBlank() && targetLabel.isNotBlank()) {
                            onAddRelationship(
                                RelationshipEntity(
                                    sourceType = sourceType,
                                    sourceId = 1L,
                                    sourceLabel = sourceLabel.trim(),
                                    targetType = targetType,
                                    targetId = 1L,
                                    targetLabel = targetLabel.trim(),
                                    relationType = relationType.trim(),
                                    description = desc.trim(),
                                    sourceX = 320f,
                                    sourceY = 300f,
                                    targetX = 620f,
                                    targetY = 420f
                                )
                            )
                            showAddRelationDialog = false
                            runAutoLayout()
                        }
                    }
                ) { Text("Создать связь") }
            },
            dismissButton = {
                TextButton(onClick = { showAddRelationDialog = false }) { Text("Отмена") }
            }
        )
    }
}

/**
 * Connections, Google Account, Gemini API Key, and Google Drive Sync Screen (Sections 5, 12, 13).
 * Strictly separates Google Account OAuth from Gemini API Key, never asks for Google passwords,
 * and handles conflict detection before importing backups.
 */
@Composable
fun ConnectionsAndDriveScreen(
    prefs: StudioPreferences,
    remoteFiles: List<DriveRemoteFileMetadata>,
    conflictInfo: SyncConflictInfo?,
    isSyncing: Boolean,
    onSaveCustomGeminiKey: (String) -> Unit,
    onSaveGoogleConnection: (String, Boolean, String, String) -> Unit,
    onDisconnectGoogle: () -> Unit,
    onSyncDriveRestApi: () -> Unit,
    onExportProjectSafUri: (Uri) -> Unit,
    onInspectAndImportSafUri: (Uri) -> Unit,
    onDismissConflict: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyInput by remember(prefs.customGeminiApiKey) { mutableStateOf(prefs.customGeminiApiKey) }
    var googleEmailInput by remember(prefs.googleAccountEmail) { mutableStateOf(prefs.googleAccountEmail) }
    var oauthTokenInput by remember(prefs.googleDriveAccessToken) { mutableStateOf(prefs.googleDriveAccessToken) }
    var driveFolderInput by remember(prefs.googleDriveFolderName) { mutableStateOf(prefs.googleDriveFolderName) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportProjectSafUri(uri)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onInspectAndImportSafUri(uri)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("connections_drive_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Gemini API Key Configuration (Separate from Google Account!)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = CrimsonPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("1. Настройка Gemini API Key", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Gemini API Key и аккаунт Google — это разные механизмы доступа. Вход в аккаунт Google не открывает автоматически квоту Gemini API. Вы можете задать ключ в панели Secrets AI Studio (BuildConfig) или ввести свой пользовательский ключ ниже:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input"),
                        label = { Text("Пользовательский Gemini API Key (AIza...)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSaveCustomGeminiKey(apiKeyInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.testTag("save_gemini_api_key_button")
                        ) {
                            Text("Сохранить API-ключ")
                        }
                        if (apiKeyInput.isNotEmpty()) {
                            OutlinedButton(onClick = {
                                apiKeyInput = ""
                                onSaveCustomGeminiKey("")
                            }) {
                                Text("Сбросить к BuildConfig")
                            }
                        }
                    }
                }
            }
        }

        // 2. Google Account & Google Drive Integration (OAuth2 + SAF)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = AmberGoldSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("2. Google Drive и резервная синхронизация", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Приложение никогда не запрашивает пароль Google. Поддерживаются два официальных режима:\n" +
                            "• Системный провайдер документов Android (SAF) — позволяет выбирать и сохранять файлы напрямую в приложении Google Drive на устройстве без ручного ввода токенов.\n" +
                            "• Прямой Google Drive REST API v3 по OAuth2 Bearer-токену (область drive.file).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                exportBackupLauncher.launch("RedWorld_Backup_${System.currentTimeMillis()}.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.testTag("saf_export_drive_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Экспорт в Google Drive (SAF)")
                        }
                        OutlinedButton(
                            onClick = {
                                importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.testTag("saf_import_drive_button")
                        ) {
                            Text("Импорт с проверкой версий")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email аккаунта Google (для идентификации сессии)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = driveFolderInput,
                        onValueChange = { driveFolderInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Целевая папка синхронизации в Google Drive") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = oauthTokenInput,
                        onValueChange = { oauthTokenInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("OAuth2 Access Token (scope: drive.file, необязательно при SAF)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSaveGoogleConnection(
                                    googleEmailInput,
                                    true,
                                    oauthTokenInput,
                                    driveFolderInput
                                )
                            }
                        ) {
                            Text("Подключить профиль Drive")
                        }
                        if (prefs.googleDriveConnected) {
                            OutlinedButton(onClick = onSyncDriveRestApi, enabled = !isSyncing) {
                                Text("Синхронизировать через Drive API")
                            }
                            IconButton(onClick = onDisconnectGoogle) {
                                Icon(Icons.Default.LinkOff, contentDescription = "Отключить Google Drive", tint = TextMutedRose)
                            }
                        }
                    }
                }
            }
        }

        // 3. Privacy & Security Notice (Section 13)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceElevated),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = StatusCanonGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Безопасность и приватность", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        text = "• Пароли Google никогда не запрашиваются и не сохраняются.\n" +
                            "• При отправке запроса в Gemini передаются только выбранные пользователем фрагменты документов (RAG).\n" +
                            "• Для демонстрационного прототипа APK: не публикуйте собранный APK в открытом доступе, если в .env упакован личный ключ.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                }
            }
        }
    }

    // Conflict Detection Dialog (Section 12: Never overwrite newer local version silently)
    if (conflictInfo != null) {
        AlertDialog(
            onDismissRequest = onDismissConflict,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AmberGoldSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Обнаружен конфликт версий документа")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Импортируемый файл «${conflictInfo.fileName}» (${conflictInfo.remoteModifiedTime}) старше вашей текущей локальной версии проекта!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmberGoldSecondary
                    )
                    Text("Локальная база: ${conflictInfo.localSummary}", style = MaterialTheme.typography.bodySmall)
                    Text("Импортируемая версия: ${conflictInfo.remoteSummary}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Система заблокировала автоматическую перезапись более новой локальной версии старой копией.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                }
            },
            confirmButton = {
                Button(onClick = onDismissConflict) {
                    Text("Сохранить текущую новую локальную версию")
                }
            }
        )
    }
}

/**
 * Version Snapshots & Documentation Guide Screen (Sections 9.3, 14, 15).
 */
@Composable
fun VersionsAndGuideScreen(
    snapshots: List<VersionSnapshotEntity>,
    onRestoreSnapshot: (VersionSnapshotEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceElevated),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Журнал версий проекта и инструкция по эксплуатации", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Каждое изменение карточки персонажа, правил мира, таблицы XLSX или подтверждённое предложение ИИ создаёт неизменяемый снимок версии с возможностью отката.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                }
            }
        }

        items(snapshots, key = { it.id }) { snap ->
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${snap.entityTitle} (${snap.entityType}) • Версия v${snap.versionNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            color = AmberGoldSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(snap.changeSummary, style = MaterialTheme.typography.bodySmall, color = TextIvoryPrimary)
                        Text("Автор: ${snap.author}", style = MaterialTheme.typography.labelMedium, color = TextMutedRose)
                    }
                    if (snap.entityType == "CHARACTER") {
                        AssistChip(
                            onClick = { onRestoreSnapshot(snap) },
                            label = { Text("Откатить") },
                            leadingIcon = {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                            }
                        )
                    }
                }
            }
        }
    }
}
