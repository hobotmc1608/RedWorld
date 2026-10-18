package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Summarize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DocumentFormat
import com.example.data.local.KnowledgeDocumentEntity
import com.example.data.local.KnowledgeDocumentWithDetails
import com.example.data.local.KnowledgeNoteEntity
import com.example.data.local.KnowledgeTagEntity
import com.example.ui.components.MarkdownMessageRenderer
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.StatusCanonGreen

/**
 * Собственная система работы с документами и заметками («Документы» и «Заметки»):
 * - Импорт поддерживаемых файлов (.txt, .md, .pdf, .docx, .xlsx, .json)
 * - Просмотр документов и индексированных фрагментов
 * - Поиск по документам и фильтрация по папкам/тегам
 * - Краткий пересказ с помощью ИИ
 * - Извлечение фактов с помощью ИИ
 * - Анализ документа с помощью ИИ
 * - Связывание документов с персонажами, локациями, правилами мира и событиями
 * - Раздел «Заметки» и система тегов
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RedWorldKnowledgeScreen(
    documents: List<KnowledgeDocumentWithDetails>,
    notes: List<KnowledgeNoteEntity>,
    tags: List<KnowledgeTagEntity>,
    selectedDocumentId: Long?,
    onSelectDocument: (Long) -> Unit,
    onCreateDocument: (String, String, DocumentFormat, String, String, String, List<Long>) -> Unit,
    onImportDocumentUri: (Uri, String, String, String) -> Unit,
    onDeleteDocument: (Long) -> Unit,
    onCreateNote: (String, String, Long?, String, List<Long>) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onCreateTag: (String, String) -> Unit,
    onDeleteTag: (Long) -> Unit,
    onAttachTagToDoc: (Long, Long) -> Unit,
    onAskGeminiAboutDocument: (String) -> Unit,
    onRunDocumentAiAction: (KnowledgeDocumentEntity, String) -> Unit = { _, _ -> },
    onUpdateDocumentEntityLinks: (KnowledgeDocumentEntity, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    initialSubTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab.coerceIn(0, 3)) }
    // 0 = Документы, 1 = Чтение, ИИ-анализ и связи, 2 = Заметки, 3 = Теги
    var searchQuery by remember { mutableStateOf("") }
    var selectedFolderFilter by remember { mutableStateOf("Все") }
    var showAddDocDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var editingLinksForDoc by remember { mutableStateOf<KnowledgeDocumentEntity?>(null) }

    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_document.txt"
            onImportDocumentUri(uri, fileName, "Импортированные файлы", "Документы проекта")
        }
    }

    val folders = remember(documents) {
        listOf("Все") + documents.map { it.document.folderName }.distinct()
    }

    val filteredDocuments = remember(documents, searchQuery, selectedFolderFilter) {
        documents.filter { item ->
            val matchesFolder = selectedFolderFilter == "Все" || item.document.folderName == selectedFolderFilter
            val matchesQuery = searchQuery.isBlank() ||
                item.document.title.contains(searchQuery, ignoreCase = true) ||
                item.document.contentText.contains(searchQuery, ignoreCase = true) ||
                item.document.linkedCharacterIdsCsv.contains(searchQuery, ignoreCase = true) ||
                item.document.linkedLocationIdsCsv.contains(searchQuery, ignoreCase = true) ||
                item.tags.any { it.name.contains(searchQuery, ignoreCase = true) }
            matchesFolder && matchesQuery
        }
    }

    val activeDocWithDetails = documents.firstOrNull { it.document.id == selectedDocumentId }
        ?: documents.firstOrNull()

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("Документы (${documents.size})") },
                modifier = Modifier.testTag("knowledge_tab_documents")
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Просмотр и ИИ-анализ") },
                modifier = Modifier.testTag("knowledge_tab_reader")
            )
            Tab(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                text = { Text("Заметки (${notes.size})") },
                modifier = Modifier.testTag("knowledge_tab_notes")
            )
            Tab(
                selected = activeSubTab == 3,
                onClick = { activeSubTab = 3 },
                text = { Text("Теги (${tags.size})") },
                modifier = Modifier.testTag("knowledge_tab_tags")
            )
        }

        when (activeSubTab) {
            // TAB 0: DOCUMENTS & IMPORT & SEARCH
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("knowledge_search_input"),
                            placeholder = { Text("Поиск по документам, фактам и связям...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true
                        )
                        Button(
                            onClick = { showAddDocDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.testTag("knowledge_add_doc_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Создать документ")
                        }
                        OutlinedButton(
                            onClick = {
                                fileImportLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "text/plain",
                                        "text/markdown",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "*/*"
                                    )
                                )
                            },
                            modifier = Modifier.testTag("knowledge_import_file_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Импорт файла")
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        folders.forEach { folder ->
                            FilterChip(
                                selected = selectedFolderFilter == folder,
                                onClick = { selectedFolderFilter = folder },
                                label = { Text(folder, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredDocuments, key = { it.document.id }) { item ->
                            val doc = item.document
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectDocument(doc.id)
                                        activeSubTab = 1
                                    }
                                    .testTag("knowledge_doc_card_${doc.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.Description,
                                                contentDescription = null,
                                                tint = CrimsonPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = doc.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${doc.format} • Папка: ${doc.folderName} • Коллекция: ${doc.collectionName}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = AmberGoldSecondary
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(onClick = { editingLinksForDoc = doc }) {
                                                Icon(Icons.Default.Link, contentDescription = "Связать с сущностями", tint = AmberGoldSecondary)
                                            }
                                            IconButton(onClick = { onDeleteDocument(doc.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    Text(
                                        text = doc.summary.ifEmpty { doc.contentText.take(150) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Display entity links if present
                                    val linksSummary = buildList {
                                        if (doc.linkedCharacterIdsCsv.isNotBlank()) add("Персонажи: ${doc.linkedCharacterIdsCsv}")
                                        if (doc.linkedLocationIdsCsv.isNotBlank()) add("Локации: ${doc.linkedLocationIdsCsv}")
                                        if (doc.linkedRuleIdsCsv.isNotBlank()) add("Правила: ${doc.linkedRuleIdsCsv}")
                                        if (doc.linkedEventIdsCsv.isNotBlank()) add("События: ${doc.linkedEventIdsCsv}")
                                    }.joinToString(" • ")

                                    if (linksSummary.isNotBlank()) {
                                        Text(
                                            text = "Связи: $linksSummary",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusCanonGreen
                                        )
                                    }

                                    if (item.tags.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            item.tags.forEach { tag ->
                                                AssistChip(
                                                    onClick = { searchQuery = tag.name },
                                                    label = { Text("#${tag.name}", style = MaterialTheme.typography.labelMedium) }
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

            // TAB 1: DOCUMENT READER, AI SUMMARY, FACT EXTRACTION, AI ANALYSIS & ENTITY LINKING
            1 -> {
                if (activeDocWithDetails == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Выберите документ во вкладке «Документы»")
                    }
                } else {
                    val doc = activeDocWithDetails.document
                    var questionAboutDoc by remember { mutableStateOf("") }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(doc.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Файл: ${doc.fileName} (${doc.format}) • Папка: ${doc.folderName} • Фрагментов: ${activeDocWithDetails.chunks.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AmberGoldSecondary
                                    )

                                    // AI Document Actions: Краткий пересказ, Извлечение фактов, Анализ документа, Связи
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onRunDocumentAiAction(doc, "SUMMARY") },
                                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                            modifier = Modifier.testTag("doc_ai_summary_btn")
                                        ) {
                                            Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Краткий пересказ ИИ")
                                        }

                                        OutlinedButton(
                                            onClick = { onRunDocumentAiAction(doc, "FACTS") },
                                            modifier = Modifier.testTag("doc_ai_facts_btn")
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Извлечь факты")
                                        }

                                        OutlinedButton(
                                            onClick = { onRunDocumentAiAction(doc, "ANALYSIS") },
                                            modifier = Modifier.testTag("doc_ai_analysis_btn")
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Анализ документа")
                                        }

                                        OutlinedButton(
                                            onClick = { editingLinksForDoc = doc },
                                            modifier = Modifier.testTag("doc_edit_links_btn")
                                        ) {
                                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Связать с миром")
                                        }
                                    }

                                    // Entity Links Display
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Связи документа с сущностями мира:", style = MaterialTheme.typography.labelLarge, color = StatusCanonGreen)
                                            Text("• Персонажи: ${doc.linkedCharacterIdsCsv.ifBlank { "Не привязаны" }}", style = MaterialTheme.typography.bodySmall)
                                            Text("• Локации: ${doc.linkedLocationIdsCsv.ifBlank { "Не привязаны" }}", style = MaterialTheme.typography.bodySmall)
                                            Text("• Правила мира: ${doc.linkedRuleIdsCsv.ifBlank { "Не привязаны" }}", style = MaterialTheme.typography.bodySmall)
                                            Text("• События хронологии: ${doc.linkedEventIdsCsv.ifBlank { "Не привязаны" }}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = questionAboutDoc,
                                            onValueChange = { questionAboutDoc = it },
                                            modifier = Modifier.weight(1f),
                                            placeholder = { Text("Задать вопрос по документу через Gemini...") },
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                val q = questionAboutDoc.ifBlank {
                                                    "Проанализируй документ «${doc.title}» и выдели ключевые канонические факты."
                                                }
                                                onAskGeminiAboutDocument(q)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                                        ) {
                                            Text("Спросить")
                                        }
                                    }
                                }
                            }
                        }

                        // AI Summary, Extracted Facts & Analysis Cards
                        if (doc.summary.isNotBlank()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    border = BorderStroke(1.dp, AmberGoldSecondary)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Краткий пересказ (ИИ):", style = MaterialTheme.typography.titleSmall, color = AmberGoldSecondary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(doc.summary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        if (doc.extractedFacts.isNotBlank()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    border = BorderStroke(1.dp, StatusCanonGreen)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Извлечённые факты:", style = MaterialTheme.typography.titleSmall, color = StatusCanonGreen, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(doc.extractedFacts, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        if (doc.aiAnalysisReport.isNotBlank()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    border = BorderStroke(1.dp, CrimsonPrimary)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Анализ документа и связей с каноном:", style = MaterialTheme.typography.titleSmall, color = CrimsonPrimary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(doc.aiAnalysisReport, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Полный текст документа",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AmberGoldSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    MarkdownMessageRenderer(markdownText = doc.contentText)
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Индексированные фрагменты документа (${activeDocWithDetails.chunks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(activeDocWithDetails.chunks, key = { it.id }) { chunk ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Фрагмент #${chunk.chunkIndex + 1} • Раздел: ${chunk.sectionTitle}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CrimsonPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = chunk.chunkText,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: NOTES (ЗАМЕТКИ)
            2 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Авторские заметки проекта",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { showAddNoteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.testTag("knowledge_add_note_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Новая заметка")
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("knowledge_note_card_${note.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Bookmark,
                                                contentDescription = null,
                                                tint = AmberGoldSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = note.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        IconButton(onClick = { onDeleteNote(note.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить заметку")
                                        }
                                    }
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (note.quotedExcerpt.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "«${note.quotedExcerpt}»",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AmberGoldSecondary,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 3: TAGS
            3 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Система тегов и категорий документов",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { showAddTagDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.testTag("knowledge_add_tag_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Новый тег")
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags, key = { it.id }) { tag ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("knowledge_tag_card_${tag.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Label, contentDescription = null, tint = CrimsonPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("#${tag.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text("Категория: ${tag.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row {
                                        if (activeDocWithDetails != null) {
                                            TextButton(onClick = { onAttachTagToDoc(activeDocWithDetails.document.id, tag.id) }) {
                                                Text("К документу")
                                            }
                                        }
                                        IconButton(onClick = { onDeleteTag(tag.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить тег")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Link Document to Characters, Locations, Rules, Events
    if (editingLinksForDoc != null) {
        val doc = editingLinksForDoc!!
        var chars by remember(doc) { mutableStateOf(doc.linkedCharacterIdsCsv) }
        var locs by remember(doc) { mutableStateOf(doc.linkedLocationIdsCsv) }
        var rules by remember(doc) { mutableStateOf(doc.linkedRuleIdsCsv) }
        var events by remember(doc) { mutableStateOf(doc.linkedEventIdsCsv) }

        AlertDialog(
            onDismissRequest = { editingLinksForDoc = null },
            title = { Text("Связи документа «${doc.title}»") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Укажите связанные сущности проекта (через запятую):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = chars,
                        onValueChange = { chars = it },
                        label = { Text("Связанные персонажи (Катрин, Александра и др.)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = locs,
                        onValueChange = { locs = it },
                        label = { Text("Связанные локации (Красный особняк и др.)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rules,
                        onValueChange = { rules = it },
                        label = { Text("Связанные правила мира") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = events,
                        onValueChange = { events = it },
                        label = { Text("Связанные события хронологии") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDocumentEntityLinks(doc, chars.trim(), locs.trim(), rules.trim(), events.trim())
                        editingLinksForDoc = null
                    }
                ) { Text("Сохранить связи") }
            },
            dismissButton = {
                TextButton(onClick = { editingLinksForDoc = null }) { Text("Отмена") }
            }
        )
    }

    // Dialog: Create Document
    if (showAddDocDialog) {
        var title by remember { mutableStateOf("") }
        var folder by remember { mutableStateOf("Правила мира") }
        var collection by remember { mutableStateOf("Основной канон") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDocDialog = false },
            title = { Text("Создать документ проекта") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название документа") })
                    OutlinedTextField(value = folder, onValueChange = { folder = it }, label = { Text("Папка") })
                    OutlinedTextField(value = collection, onValueChange = { collection = it }, label = { Text("Коллекция") })
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Текст (Markdown / TXT)") },
                        minLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onCreateDocument(
                                title,
                                "${title.lowercase().replace(" ", "_")}.md",
                                DocumentFormat.MARKDOWN,
                                folder,
                                collection,
                                content.ifBlank { "# $title\nОписание документа." },
                                emptyList()
                            )
                            showAddDocDialog = false
                        }
                    }
                ) { Text("Сохранить и индексировать") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDocDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Dialog: Create Note
    if (showAddNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteContent by remember { mutableStateOf("") }
        var noteQuote by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Новая заметка") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = noteTitle, onValueChange = { noteTitle = it }, label = { Text("Заголовок заметки") })
                    OutlinedTextField(value = noteContent, onValueChange = { noteContent = it }, label = { Text("Содержание заметки") }, minLines = 3)
                    OutlinedTextField(value = noteQuote, onValueChange = { noteQuote = it }, label = { Text("Цитата из документа (необязательно)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank()) {
                            onCreateNote(noteTitle, noteContent, activeDocWithDetails?.document?.id, noteQuote, emptyList())
                            showAddNoteDialog = false
                        }
                    }
                ) { Text("Сохранить заметку") }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Dialog: Create Tag
    if (showAddTagDialog) {
        var tagName by remember { mutableStateOf("") }
        var tagCategory by remember { mutableStateOf("Локация") }
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Создать тег") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tagName, onValueChange = { tagName = it }, label = { Text("Имя тега") })
                    OutlinedTextField(value = tagCategory, onValueChange = { tagCategory = it }, label = { Text("Категория") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tagName.isNotBlank()) {
                            onCreateTag(tagName, tagCategory)
                            showAddTagDialog = false
                        }
                    }
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) { Text("Отмена") }
            }
        )
    }
}
