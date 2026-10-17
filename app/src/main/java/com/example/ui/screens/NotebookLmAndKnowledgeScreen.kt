package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.DocumentFormat
import com.example.data.local.KnowledgeDocumentWithDetails
import com.example.data.local.KnowledgeNoteEntity
import com.example.data.local.KnowledgeTagEntity
import com.example.ui.components.MarkdownMessageRenderer
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.BurgundyCardBorder
import com.example.ui.theme.BurgundySurfaceContainer
import com.example.ui.theme.BurgundySurfaceElevated
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.TextIvoryPrimary
import com.example.ui.theme.TextMutedRose

/**
 * Dedicated NotebookLM tab (Section 6) with clear transparency about official Google NotebookLM
 * web interface vs. local RedWorld Knowledge RAG library, persistent session cookies, and quick switching.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NotebookLmTabScreen(
    onSwitchToKnowledgeLibrary: () -> Unit,
    onSwitchToGeminiChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEmbeddedWebView by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Transparency & Mode Indicator Banner (Required by Section 6)
        Card(
            colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
            border = BorderStroke(1.dp, BurgundyCardBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AmberGoldSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Режим интеграции с оригинальным Google NotebookLM",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextIvoryPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Потребительский сервис Google NotebookLM не предоставляет открытого публичного REST API для стороннего управления блокнотами. В соответствии с архитектурой безопасности RedWorld AI Studio предоставляет:\n" +
                        "1) Прямой доступ к оригинальному веб-интерфейсу notebooklm.google.com с сохранением авторизованной сессии.\n" +
                        "2) Независимую офлайн-систему RedWorld Knowledge (локальная БД Room + RAG) для работы с вашими PDF, DOCX, XLSX и MD файлами.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedRose
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showEmbeddedWebView = !showEmbeddedWebView },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_notebooklm_webview_button")
                    ) {
                        Text(if (showEmbeddedWebView) "Скрыть WebView" else "Открыть NotebookLM в приложении")
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://notebooklm.google.com"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.testTag("open_notebooklm_external_button")
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Браузер")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = onSwitchToKnowledgeLibrary,
                        label = { Text("Перейти в RedWorld Knowledge (Локальный RAG)") }
                    )
                    AssistChip(
                        onClick = onSwitchToGeminiChat,
                        label = { Text("К чату Gemini") }
                    )
                }
            }
        }

        if (showEmbeddedWebView) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                border = BorderStroke(1.dp, BurgundyCardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Сессия: notebooklm.google.com (Cookies сохранены)",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberGoldSecondary
                        )
                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = TextIvoryPrimary)
                        }
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean = false
                                }
                                loadUrl("https://notebooklm.google.com")
                                webViewRef = this
                            }
                        }
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceElevated),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Быстрое взаимодействие NotebookLM ↔ RedWorld",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextIvoryPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Используйте оригинальный NotebookLM для аудио-обзоров и глобальных сводок Google.\n" +
                            "• Используйте RedWorld Knowledge для офлайн-хранения PDF, TXT, MD, DOCX, XLSX, привязки тегов и персонажей, а также точного RAG-поиска с цитатами в Gemini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedRose
                    )
                }
            }
        }
    }
}

/**
 * Full RedWorld Knowledge Module (Section 7):
 * Documents (PDF, TXT, Markdown, DOCX, XLSX), Folders & Collections, Notes, Tags,
 * RAG chunks inspection, and direct Q&A via Gemini with cited source navigation.
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Документы, 1 = Просмотр и RAG, 2 = Заметки, 3 = Теги
    var searchQuery by remember { mutableStateOf("") }
    var selectedFolderFilter by remember { mutableStateOf("Все") }
    var showAddDocDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }

    // SAF launcher for importing PDF, TXT, MD, DOCX, XLSX
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_source.txt"
            onImportDocumentUri(uri, fileName, "Импортированные файлы", "Внешние источники")
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
                item.tags.any { it.name.contains(searchQuery, ignoreCase = true) }
            matchesFolder && matchesQuery
        }
    }

    val activeDocWithDetails = documents.firstOrNull { it.document.id == selectedDocumentId }
        ?: documents.firstOrNull()

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = BurgundySurfaceContainer,
            contentColor = CrimsonPrimary
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("Источники (${documents.size})") },
                modifier = Modifier.testTag("knowledge_tab_documents")
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Чтение и RAG") },
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
            // TAB 0: DOCUMENTS & COLLECTIONS
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
                            placeholder = { Text("Поиск по документам, тексту и тегам...") },
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
                                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                                border = BorderStroke(1.dp, BurgundyCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
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
                                                    color = TextIvoryPrimary
                                                )
                                                Text(
                                                    text = "${doc.format} • Папка: ${doc.folderName} • Коллекция: ${doc.collectionName}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = AmberGoldSecondary
                                                )
                                            }
                                        }
                                        IconButton(onClick = { onDeleteDocument(doc.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = TextMutedRose)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = doc.summary.ifEmpty { doc.contentText.take(140) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMutedRose,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (item.tags.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 6.dp)
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

            // TAB 1: DOCUMENT READER & RAG CHUNKS + ASK GEMINI
            1 -> {
                if (activeDocWithDetails == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Выберите документ во вкладке «Источники»")
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
                                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceElevated),
                                border = BorderStroke(1.dp, BurgundyCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(doc.title, style = MaterialTheme.typography.headlineMedium, color = TextIvoryPrimary)
                                    Text(
                                        "Файл: ${doc.fileName} (${doc.format}) • Папка: ${doc.folderName} • RAG-фрагментов: ${activeDocWithDetails.chunks.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AmberGoldSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = questionAboutDoc,
                                            onValueChange = { questionAboutDoc = it },
                                            modifier = Modifier.weight(1f),
                                            placeholder = { Text("Задать вопрос по этому источнику через Gemini...") },
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
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Спросить")
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                                border = BorderStroke(1.dp, BurgundyCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Содержимое документа",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AmberGoldSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    MarkdownMessageRenderer(markdownText = doc.contentText)
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Индексированные RAG-фрагменты для точного цитирования (${activeDocWithDetails.chunks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextIvoryPrimary
                            )
                        }

                        items(activeDocWithDetails.chunks, key = { it.id }) { chunk ->
                            Surface(
                                color = BurgundySurfaceContainer,
                                border = BorderStroke(1.dp, BurgundyCardBorder),
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextIvoryPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: NOTES
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
                            text = "Исследовательские заметки RedWorld",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextIvoryPrimary
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
                                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                                border = BorderStroke(1.dp, BurgundyCardBorder)
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
                                                color = TextIvoryPrimary
                                            )
                                        }
                                        IconButton(onClick = { onDeleteNote(note.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить заметку", tint = TextMutedRose)
                                        }
                                    }
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextIvoryPrimary
                                    )
                                    if (note.quotedExcerpt.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = BurgundySurfaceElevated,
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
                            text = "Система тегов и категорий источников",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextIvoryPrimary
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
                                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                                border = BorderStroke(1.dp, BurgundyCardBorder)
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
                                            Text("#${tag.name}", style = MaterialTheme.typography.titleSmall, color = TextIvoryPrimary)
                                            Text("Категория: ${tag.category}", style = MaterialTheme.typography.bodySmall, color = TextMutedRose)
                                        }
                                    }
                                    Row {
                                        if (activeDocWithDetails != null) {
                                            TextButton(onClick = { onAttachTagToDoc(activeDocWithDetails.document.id, tag.id) }) {
                                                Text("К документу")
                                            }
                                        }
                                        IconButton(onClick = { onDeleteTag(tag.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить тег", tint = TextMutedRose)
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

    // Dialog: Create Document
    if (showAddDocDialog) {
        var title by remember { mutableStateOf("") }
        var folder by remember { mutableStateOf("Правила мира") }
        var collection by remember { mutableStateOf("Основной канон") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDocDialog = false },
            title = { Text("Добавить источник в RedWorld Knowledge") },
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
                                content.ifBlank { "# $title\nОписание источника RedWorld." },
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
                    OutlinedTextField(value = noteQuote, onValueChange = { noteQuote = it }, label = { Text("Цитата из источника (необязательно)") })
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
