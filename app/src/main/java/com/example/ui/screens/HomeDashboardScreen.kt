package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CharacterEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.GeneratedImageEntity
import com.example.data.local.KnowledgeDocumentEntity
import com.example.data.local.KnowledgeDocumentWithDetails
import com.example.data.preferences.StudioPreferences
import com.example.ui.MainBottomTab
import com.example.ui.SecondaryWorkspaceScreen
import com.example.ui.components.CanonStatusBadge
import com.example.ui.components.RedWorldHeroArtBanner
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.BurgundyCardBorder
import com.example.ui.theme.BurgundySurfaceContainer
import com.example.ui.theme.BurgundySurfaceElevated
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.TextIvoryPrimary
import com.example.ui.theme.TextMutedRose

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardScreen(
    prefs: StudioPreferences,
    recentChats: List<ChatSessionEntity>,
    recentDocs: List<KnowledgeDocumentEntity>,
    allDocsWithDetails: List<KnowledgeDocumentWithDetails>,
    characters: List<CharacterEntity>,
    generatedImages: List<GeneratedImageEntity>,
    onSelectBottomTab: (MainBottomTab) -> Unit,
    onOpenSecondaryScreen: (SecondaryWorkspaceScreen) -> Unit,
    onOpenChatSession: (Long) -> Unit,
    onOpenDocument: (Long) -> Unit,
    onOpenCharacter: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val collections = allDocsWithDetails
        .groupBy { it.document.collectionName }
        .map { it.key to it.value.size }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Banner + Service Connection Status
        item {
            RedWorldHeroArtBanner(
                title = "RedWorld AI Studio",
                subtitle = "Единое рабочее пространство: Gemini AI, база знаний RAG, редактор канона, XLSX и карта связей",
                isGeminiReady = prefs.isGeminiKeyConfigured,
                isDriveConnected = prefs.googleDriveConnected,
                onOpenConnections = {
                    onOpenSecondaryScreen(SecondaryWorkspaceScreen.CONNECTIONS_AND_AUTH)
                }
            )
        }

        // 2. Quick Actions Grid (Быстрые действия)
        item {
            SectionHeaderRow(title = "Быстрые действия и модули")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionChip(
                    label = "Диалог Gemini",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    tag = "quick_action_gemini",
                    onClick = { onSelectBottomTab(MainBottomTab.GEMINI) }
                )
                QuickActionChip(
                    label = "База знаний RAG",
                    icon = Icons.Default.MenuBook,
                    tag = "quick_action_knowledge",
                    onClick = { onOpenSecondaryScreen(SecondaryWorkspaceScreen.KNOWLEDGE_RAG) }
                )
                QuickActionChip(
                    label = "Карта связей",
                    icon = Icons.Default.AccountTree,
                    tag = "quick_action_map",
                    onClick = { onOpenSecondaryScreen(SecondaryWorkspaceScreen.RELATIONSHIP_MAP) }
                )
                QuickActionChip(
                    label = "Таблицы XLSX",
                    icon = Icons.Default.TableChart,
                    tag = "quick_action_xlsx",
                    onClick = { onOpenSecondaryScreen(SecondaryWorkspaceScreen.XLSX_EDITOR) }
                )
                QuickActionChip(
                    label = "ИИ-Куратор канона",
                    icon = Icons.Default.AutoAwesome,
                    tag = "quick_action_ai_curator",
                    onClick = { onOpenSecondaryScreen(SecondaryWorkspaceScreen.AI_ASSISTANT) }
                )
                QuickActionChip(
                    label = "Google Drive / Бэкап",
                    icon = Icons.Default.CloudSync,
                    tag = "quick_action_drive",
                    onClick = { onOpenSecondaryScreen(SecondaryWorkspaceScreen.FILES_AND_DRIVE) }
                )
                QuickActionChip(
                    label = "Версии проекта",
                    icon = Icons.Default.History,
                    tag = "quick_action_versions",
                    onClick = { onOpenSecondaryScreen(SecondaryWorkspaceScreen.VERSIONS_AND_BACKUPS) }
                )
            }
        }

        // 3. Recent Character Cards (Последние изменённые карточки персонажей)
        item {
            SectionHeaderRow(
                title = "Карточки персонажей RedWorld",
                actionLabel = "Все (${characters.size})",
                onActionClick = { onSelectBottomTab(MainBottomTab.REDWORLD) }
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(characters, key = { it.id }) { character ->
                    Card(
                        modifier = Modifier
                            .width(265.dp)
                            .clickable { onOpenCharacter(character.id) }
                            .testTag("home_character_card_${character.id}"),
                        colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                        border = BorderStroke(1.dp, BurgundyCardBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = character.name,
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = character.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextIvoryPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                CanonStatusBadge(statusName = character.canonStatus)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = character.occupation.ifEmpty { character.aliases },
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberGoldSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = BurgundySurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Локация сейчас",
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Сейчас: ${character.currentLocationName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextIvoryPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Recent Gemini Chats (Последние чаты Gemini)
        item {
            SectionHeaderRow(
                title = "Последние чаты Gemini",
                actionLabel = "Открыть чат",
                onActionClick = { onSelectBottomTab(MainBottomTab.GEMINI) }
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentChats.take(3).forEach { chat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenChatSession(chat.id)
                                onSelectBottomTab(MainBottomTab.GEMINI)
                            }
                            .testTag("home_chat_item_${chat.id}"),
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = chat.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextIvoryPrimary
                                    )
                                    Text(
                                        text = "Модель: ${chat.modelId} • Контекст RedWorld: ${if (chat.isRedWorldContextEnabled) "Вкл" else "Выкл"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMutedRose
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Recently Opened Documents & Active Source Collections
        item {
            SectionHeaderRow(
                title = "Недавние документы и коллекции источников",
                actionLabel = "База знаний",
                onActionClick = { onOpenSecondaryScreen(SecondaryWorkspaceScreen.KNOWLEDGE_RAG) }
            )
            if (collections.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    collections.forEach { (colName, count) ->
                        AssistChip(
                            onClick = { onOpenSecondaryScreen(SecondaryWorkspaceScreen.KNOWLEDGE_RAG) },
                            label = { Text("$colName ($count)") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Book,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = AmberGoldSecondary
                                )
                            }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentDocs.forEach { doc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDocument(doc.id) }
                            .testTag("home_doc_item_${doc.id}"),
                        colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                        border = BorderStroke(1.dp, BurgundyCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = doc.title,
                                tint = AmberGoldSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextIvoryPrimary
                                )
                                Text(
                                    text = "${doc.format} • Папка: ${doc.folderName} • ${doc.collectionName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMutedRose
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Recent Image Studio Prompts & Generations
        item {
            SectionHeaderRow(
                title = "Image Studio и паспорта внешности",
                actionLabel = "Открыть студию",
                onActionClick = { onSelectBottomTab(MainBottomTab.GENERATION) }
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectBottomTab(MainBottomTab.GENERATION) },
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (generatedImages.isEmpty()) "Готовы шаблоны портретов и локаций RedWorld"
                            else "Сохранено генераций в истории: ${generatedImages.size}",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextIvoryPrimary
                        )
                        Text(
                            text = "Поддержка паспортов внешности персонажей Виктора Корвина и Елены Ростовой",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedRose
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        modifier = Modifier.testTag(tag),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CrimsonPrimary,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = BurgundySurfaceElevated,
            labelColor = TextIvoryPrimary
        ),
        border = BorderStroke(1.dp, BurgundyCardBorder)
    )
}
