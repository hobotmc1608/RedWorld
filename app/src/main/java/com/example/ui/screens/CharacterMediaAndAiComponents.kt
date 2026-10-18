package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.AiExtractedCharacterDossier
import com.example.data.local.CanonStatus
import com.example.data.local.CharacterEntity
import com.example.data.local.CharacterReferenceImage
import com.example.data.local.CharacterSavedPrompt
import com.example.data.local.DeclaredCharacterRelation
import com.example.data.local.RelationshipEntity
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.BurgundyCardBorder
import com.example.ui.theme.BurgundySurfaceContainer
import com.example.ui.theme.BurgundySurfaceElevated
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.StatusCanonGreen
import com.example.ui.theme.TextIvoryPrimary
import com.example.ui.theme.TextMutedRose
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

private val characterMediaJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

fun parseCharacterReferences(jsonStr: String): List<CharacterReferenceImage> {
    if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
    return try {
        characterMediaJson.decodeFromString<List<CharacterReferenceImage>>(jsonStr)
    } catch (_: Exception) {
        emptyList()
    }
}

fun parseCharacterPrompts(jsonStr: String): List<CharacterSavedPrompt> {
    if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
    return try {
        characterMediaJson.decodeFromString<List<CharacterSavedPrompt>>(jsonStr)
    } catch (_: Exception) {
        emptyList()
    }
}

fun parseCharacterDeclaredRelations(jsonStr: String): List<DeclaredCharacterRelation> {
    if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
    return try {
        characterMediaJson.decodeFromString<List<DeclaredCharacterRelation>>(jsonStr)
    } catch (_: Exception) {
        emptyList()
    }
}

@Composable
fun CharacterAvatarThumbnail(
    name: String,
    avatarBase64: String,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(avatarBase64) {
        if (avatarBase64.isBlank()) null
        else try {
            val bytes = Base64.decode(avatarBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(BurgundySurfaceElevated)
            .border(2.dp, CrimsonPrimary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = "Аватарка $name",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val initials = name.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
                .ifEmpty { "RW" }
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AmberGoldSecondary
            )
        }
    }
}

/**
 * Displays Character Avatar, Reference Images Gallery, and Saved Generation Prompts
 * inside the Character Card, with direct 1-click launch into Image Studio.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterMediaAndPromptsSection(
    character: CharacterEntity,
    onUpdateAvatar: (String) -> Unit,
    onAddReferenceImage: (String, String, String) -> Unit,
    onRemoveReferenceImage: (String) -> Unit,
    onAddSavedPrompt: (String, String, String) -> Unit,
    onRemoveSavedPrompt: (String) -> Unit,
    onOpenInImageStudio: (promptText: String?, referenceBase64: String?, referenceTitle: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val references = remember(character.referenceImagesJson) { parseCharacterReferences(character.referenceImagesJson) }
    val savedPrompts = remember(character.savedPromptsJson) { parseCharacterPrompts(character.savedPromptsJson) }

    var showAddPromptDialog by remember { mutableStateOf(false) }
    var pendingRefBase64 by remember { mutableStateOf<String?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            encodeUriToBase64(context, uri)?.let { b64 ->
                onUpdateAvatar(b64)
            }
        }
    }

    val referencePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            encodeUriToBase64(context, uri)?.let { b64 ->
                pendingRefBase64 = b64
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
        border = BorderStroke(1.dp, BurgundyCardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Avatar & Visual Studio Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CharacterAvatarThumbnail(
                    name = character.name,
                    avatarBase64 = character.avatarBase64,
                    size = 72.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Аватарка, референсы и промпты персонажа",
                        style = MaterialTheme.typography.titleSmall,
                        color = AmberGoldSecondary
                    )
                    Text(
                        text = "Используются в Image Studio при генерации портретов, поз, одежды и игровых сцен с участием «${character.name}».",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("upload_character_avatar_button")
                        ) {
                            Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (character.avatarBase64.isNotBlank()) "Сменить аватарку" else "Загрузить аватарку")
                        }
                        Button(
                            onClick = {
                                onOpenInImageStudio(
                                    character.visualPassportPrompt.ifBlank { null },
                                    character.avatarBase64.ifBlank { null },
                                    "Аватарка: ${character.name}"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.testTag("generate_from_character_card_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("В Image Studio")
                        }
                    }
                }
            }

            HorizontalDivider(color = BurgundyCardBorder)

            // 2. Reference Images Gallery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Референсы внешности и костюмов (${references.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextIvoryPrimary
                )
                OutlinedButton(
                    onClick = {
                        referencePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("add_character_reference_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Загрузить референс")
                }
            }

            if (references.isEmpty()) {
                Text(
                    text = "Референсы ещё не загружены. Добавьте изображения лица, одежды или предметов персонажа, чтобы обращаться к ним при генерации.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedRose
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(references, key = { it.id }) { refItem ->
                        val bmp = remember(refItem.base64Data) {
                            try {
                                val bytes = Base64.decode(refItem.base64Data, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }
                        Surface(
                            color = BurgundySurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BurgundyCardBorder),
                            modifier = Modifier.width(190.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = refItem.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = refItem.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextIvoryPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (refItem.notes.isNotBlank()) {
                                    Text(
                                        text = refItem.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMutedRose,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AssistChip(
                                        onClick = {
                                            onOpenInImageStudio(
                                                character.visualPassportPrompt,
                                                refItem.base64Data,
                                                refItem.title
                                            )
                                        },
                                        label = { Text("В генерацию") }
                                    )
                                    IconButton(
                                        onClick = { onRemoveReferenceImage(refItem.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить референс", tint = TextMutedRose)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = BurgundyCardBorder)

            // 3. Saved Character Prompts for Image Studio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Сохранённые промпты персонажа (${savedPrompts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextIvoryPrimary
                )
                OutlinedButton(
                    onClick = { showAddPromptDialog = true },
                    modifier = Modifier.testTag("add_character_prompt_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить промпт")
                }
            }

            savedPrompts.forEach { prm ->
                Surface(
                    color = BurgundySurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BurgundyCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${prm.title} • [${prm.category}]",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AmberGoldSecondary
                                )
                                Text(
                                    text = prm.promptText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextIvoryPrimary
                                )
                            }
                            IconButton(onClick = { onRemoveSavedPrompt(prm.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить промпт", tint = TextMutedRose)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                onOpenInImageStudio(
                                    prm.promptText,
                                    character.avatarBase64.ifBlank { references.firstOrNull()?.base64Data },
                                    prm.title
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Обратиться к промпту в Image Studio")
                        }
                    }
                }
            }
        }
    }

    // Dialog to name a newly picked Reference Image
    if (pendingRefBase64 != null) {
        var refTitle by remember { mutableStateOf("Референс: ${character.name}") }
        var refNotes by remember { mutableStateOf("Детали внешности и костюма") }
        AlertDialog(
            onDismissRequest = { pendingRefBase64 = null },
            title = { Text("Сохранить референс персонажа") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = refTitle, onValueChange = { refTitle = it }, label = { Text("Название референса") })
                    OutlinedTextField(value = refNotes, onValueChange = { refNotes = it }, label = { Text("Заметка (ракурс, одежда, эмоции)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddReferenceImage(refTitle, pendingRefBase64!!, refNotes)
                        pendingRefBase64 = null
                    }
                ) { Text("Сохранить в карточку") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRefBase64 = null }) { Text("Отмена") }
            }
        )
    }

    // Dialog to add a new Saved Prompt to the Character Card
    if (showAddPromptDialog) {
        var title by remember { mutableStateOf("Канонический ракурс: ${character.name}") }
        var category by remember { mutableStateOf("Портрет") }
        var promptText by remember {
            mutableStateOf(
                character.visualPassportPrompt.ifBlank {
                    "Portrait of ${character.name}, ${character.appearance}, hair: ${character.hair}, eyes: ${character.eyes}, wearing ${character.defaultClothing}, dramatic lighting"
                }
            )
        }
        AlertDialog(
            onDismissRequest = { showAddPromptDialog = false },
            title = { Text("Добавить промпт в карточку «${character.name}»") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название промпта") })
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Категория (Портрет, Одежда, Эмоции, Сцена)") })
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text("Текст промпта для генерации изображений") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (promptText.isNotBlank()) {
                            onAddSavedPrompt(title, promptText, category)
                            showAddPromptDialog = false
                        }
                    }
                ) { Text("Сохранить промпт") }
            },
            dismissButton = {
                TextButton(onClick = { showAddPromptDialog = false }) { Text("Отмена") }
            }
        )
    }
}

/**
 * Displays both Active Relationships (already materialized on the Relationship Map)
 * and Declared / Pending Relationships extracted from character files, with automatic
 * linking indicator and 1-click creation of missing characters.
 */
@Composable
fun CharacterAutoRelationshipsSection(
    character: CharacterEntity,
    allCharacters: List<CharacterEntity>,
    allRelationships: List<RelationshipEntity>,
    onAddDeclaredRelation: (DeclaredCharacterRelation) -> Unit,
    onQuickCreateMissingCharacter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val declaredRels = remember(character.declaredRelationshipsJson) {
        parseCharacterDeclaredRelations(character.declaredRelationshipsJson)
    }
    val activeCharRels = remember(allRelationships, character.id) {
        allRelationships.filter {
            (it.sourceType == "CHARACTER" && it.sourceId == character.id) ||
                (it.targetType == "CHARACTER" && it.targetId == character.id)
        }
    }

    var showAddRelationDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
        border = BorderStroke(1.dp, BurgundyCardBorder),
        shape = RoundedCornerShape(16.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = CrimsonPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Связи персонажа (Автоподтягивание при добавлении)",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextIvoryPrimary
                        )
                        Text(
                            text = "Если в досье прописана связь с персонажем, которого ещё нет в базе, она подтянется автоматически сразу после его добавления.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedRose
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showAddRelationDialog = true },
                    modifier = Modifier.testTag("add_declared_relation_button")
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Связь")
                }
            }

            if (activeCharRels.isNotEmpty()) {
                Text(
                    text = "Активные связи на Карте связей (${activeCharRels.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    color = StatusCanonGreen
                )
                activeCharRels.forEach { rel ->
                    val otherLabel = if (rel.sourceType == "CHARACTER" && rel.sourceId == character.id) {
                        rel.targetLabel
                    } else {
                        rel.sourceLabel
                    }
                    Surface(
                        color = BurgundySurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusCanonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${character.name} ↔ $otherLabel (${rel.relationType})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextIvoryPrimary
                                )
                                if (rel.description.isNotBlank()) {
                                    Text(rel.description, style = MaterialTheme.typography.bodySmall, color = TextMutedRose)
                                }
                            }
                        }
                    }
                }
            }

            if (declaredRels.isNotEmpty()) {
                Text(
                    text = "Прописанные связи в досье персонажа (${declaredRels.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberGoldSecondary
                )
                declaredRels.forEach { decl ->
                    val existsInWorld = allCharacters.any {
                        it.name.equals(decl.targetName, ignoreCase = true) ||
                            it.aliases.contains(decl.targetName, ignoreCase = true)
                    } || decl.targetType == "LOCATION"

                    Surface(
                        color = BurgundySurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (existsInWorld) StatusCanonGreen.copy(alpha = 0.5f) else AmberGoldSecondary.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (existsInWorld) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (existsInWorld) StatusCanonGreen else AmberGoldSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${decl.targetName} • ${decl.relationType}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextIvoryPrimary
                                    )
                                    Text(
                                        text = if (existsInWorld) {
                                            "✓ Связь автоматически подтянута в граф мира (${decl.description})"
                                        } else {
                                            "⏳ Ожидает добавления персонажа «${decl.targetName}» — подтянется автоматически (${decl.description})"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (existsInWorld) StatusCanonGreen else AmberGoldSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRelationDialog) {
        var targetName by remember { mutableStateOf("") }
        var relationType by remember { mutableStateOf("Знаком с") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddRelationDialog = false },
            title = { Text("Прописать связь в карточке «${character.name}»") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Если указанный персонаж уже есть в мире, связь появится на Карте связей сразу. Если его ещё нет, связь сохранится в карточке и подтянется автоматически при создании этого персонажа.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                    OutlinedTextField(
                        value = targetName,
                        onValueChange = { targetName = it },
                        label = { Text("Имя другого персонажа или локации") }
                    )
                    OutlinedTextField(
                        value = relationType,
                        onValueChange = { relationType = it },
                        label = { Text("Тип связи (Друг, Враг, Коллега, Родственник, Знаком с)") }
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Описание связи / истории знакомства") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetName.isNotBlank()) {
                            onAddDeclaredRelation(
                                DeclaredCharacterRelation(
                                    targetName = targetName.trim(),
                                    targetType = "CHARACTER",
                                    relationType = relationType.trim().ifEmpty { "Знаком с" },
                                    description = description.trim()
                                )
                            )
                            showAddRelationDialog = false
                        }
                    }
                ) { Text("Сохранить связь") }
            },
            dismissButton = {
                TextButton(onClick = { showAddRelationDialog = false }) { Text("Отмена") }
            }
        )
    }
}

/**
 * Full Create / Edit Character Dialog with:
 * - Any file upload (.DOC, .DOCX, .PDF, .TXT, .MD, .XLSX) + 1-click Demo .DOC files
 * - Gemini AI automatic field extraction & relationship detection
 * - Avatar upload right inside the dialog
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCharacterDossierDialog(
    initialCharacter: CharacterEntity,
    isNewCharacter: Boolean,
    isExtractingFile: Boolean,
    onExtractFromUri: (Uri, String, (AiExtractedCharacterDossier, String) -> Unit) -> Unit,
    onExtractFromSampleDoc: (String, (AiExtractedCharacterDossier, String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (CharacterEntity, String) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialCharacter.name) }
    var fullName by remember { mutableStateOf(initialCharacter.fullName) }
    var aliases by remember { mutableStateOf(initialCharacter.aliases) }
    var gender by remember { mutableStateOf(initialCharacter.gender) }
    var speciesOrOrigin by remember { mutableStateOf(initialCharacter.speciesOrOrigin) }
    var storyRole by remember { mutableStateOf(initialCharacter.storyRole) }
    var characterStatus by remember { mutableStateOf(initialCharacter.characterStatus) }
    var factionOrGroup by remember { mutableStateOf(initialCharacter.factionOrGroup) }
    var tagsCsv by remember { mutableStateOf(initialCharacter.tagsCsv) }
    var birthDate by remember { mutableStateOf(initialCharacter.birthDate) }
    var age by remember { mutableStateOf(initialCharacter.age) }
    var birthPlace by remember { mutableStateOf(initialCharacter.birthPlace) }
    var occupation by remember { mutableStateOf(initialCharacter.occupation) }
    var shortDescription by remember { mutableStateOf(initialCharacter.shortDescription) }

    var appearance by remember { mutableStateOf(initialCharacter.appearance) }
    var physique by remember { mutableStateOf(initialCharacter.physique) }
    var hair by remember { mutableStateOf(initialCharacter.hair) }
    var eyes by remember { mutableStateOf(initialCharacter.eyes) }
    var defaultClothing by remember { mutableStateOf(initialCharacter.defaultClothing) }
    var distinguishingMarks by remember { mutableStateOf(initialCharacter.distinguishingMarks) }
    var voiceDescription by remember { mutableStateOf(initialCharacter.voiceDescription) }

    var personality by remember { mutableStateOf(initialCharacter.personality) }
    var archetype by remember { mutableStateOf(initialCharacter.archetype) }
    var strengths by remember { mutableStateOf(initialCharacter.strengths) }
    var weaknesses by remember { mutableStateOf(initialCharacter.weaknesses) }
    var internalConflict by remember { mutableStateOf(initialCharacter.internalConflict) }

    var habits by remember { mutableStateOf(initialCharacter.habits) }
    var speechStyle by remember { mutableStateOf(initialCharacter.speechStyle) }
    var catchphrases by remember { mutableStateOf(initialCharacter.catchphrases) }
    var behavioralTraits by remember { mutableStateOf(initialCharacter.behavioralTraits) }

    var detailedDescription by remember { mutableStateOf(initialCharacter.detailedDescription) }

    var preferences by remember { mutableStateOf(initialCharacter.preferences) }
    var dislikes by remember { mutableStateOf(initialCharacter.dislikes) }
    var interests by remember { mutableStateOf(initialCharacter.interests) }

    var skills by remember { mutableStateOf(initialCharacter.skills) }
    var inventorySummary by remember { mutableStateOf(initialCharacter.inventorySummary) }

    var goals by remember { mutableStateOf(initialCharacter.goals) }
    var motivation by remember { mutableStateOf(initialCharacter.motivation) }
    var knownFacts by remember { mutableStateOf(initialCharacter.knownFacts) }
    var fearsAndLimitations by remember { mutableStateOf(initialCharacter.fearsAndLimitations) }
    var additionalNotes by remember { mutableStateOf(initialCharacter.additionalNotes) }

    var currentLocationName by remember { mutableStateOf(initialCharacter.currentLocationName) }
    var currentMood by remember { mutableStateOf(initialCharacter.currentMoodAndCondition) }
    var currentOutfit by remember { mutableStateOf(initialCharacter.currentOutfit) }
    var currentActiveGoal by remember { mutableStateOf(initialCharacter.currentActiveGoal) }

    var visualPassport by remember { mutableStateOf(initialCharacter.visualPassportPrompt) }
    var relationshipsSummary by remember { mutableStateOf(initialCharacter.relationshipsSummary) }
    var avatarBase64 by remember { mutableStateOf(initialCharacter.avatarBase64) }
    var savedPromptsJson by remember { mutableStateOf(initialCharacter.savedPromptsJson) }
    var declaredRelationshipsJson by remember { mutableStateOf(initialCharacter.declaredRelationshipsJson) }
    var sourceFileName by remember { mutableStateOf(initialCharacter.sourceFileName) }
    var customJson by remember { mutableStateOf(initialCharacter.customFieldsJson) }
    var canonStatus by remember { mutableStateOf(initialCharacter.canonStatus) }

    fun applyExtractedDossier(dossier: AiExtractedCharacterDossier, fileName: String) {
        if (dossier.name.isNotBlank()) name = dossier.name
        if (dossier.fullName.isNotBlank()) fullName = dossier.fullName
        if (dossier.aliases.isNotBlank()) aliases = dossier.aliases
        if (dossier.gender.isNotBlank()) gender = dossier.gender
        if (dossier.speciesOrOrigin.isNotBlank()) speciesOrOrigin = dossier.speciesOrOrigin
        if (dossier.storyRole.isNotBlank()) storyRole = dossier.storyRole
        if (dossier.characterStatus.isNotBlank()) characterStatus = dossier.characterStatus
        if (dossier.factionOrGroup.isNotBlank()) factionOrGroup = dossier.factionOrGroup
        if (dossier.tagsCsv.isNotBlank()) tagsCsv = dossier.tagsCsv
        if (dossier.birthDate.isNotBlank()) birthDate = dossier.birthDate
        if (dossier.age.isNotBlank()) age = dossier.age
        if (dossier.birthPlace.isNotBlank()) birthPlace = dossier.birthPlace
        if (dossier.occupation.isNotBlank()) occupation = dossier.occupation
        if (dossier.shortDescription.isNotBlank()) shortDescription = dossier.shortDescription
        if (dossier.appearance.isNotBlank()) appearance = dossier.appearance
        if (dossier.physique.isNotBlank()) physique = dossier.physique
        if (dossier.hair.isNotBlank()) hair = dossier.hair
        if (dossier.eyes.isNotBlank()) eyes = dossier.eyes
        if (dossier.defaultClothing.isNotBlank()) defaultClothing = dossier.defaultClothing
        if (dossier.distinguishingMarks.isNotBlank()) distinguishingMarks = dossier.distinguishingMarks
        if (dossier.voiceDescription.isNotBlank()) voiceDescription = dossier.voiceDescription
        if (dossier.personality.isNotBlank()) personality = dossier.personality
        if (dossier.archetype.isNotBlank()) archetype = dossier.archetype
        if (dossier.strengths.isNotBlank()) strengths = dossier.strengths
        if (dossier.weaknesses.isNotBlank()) weaknesses = dossier.weaknesses
        if (dossier.internalConflict.isNotBlank()) internalConflict = dossier.internalConflict
        if (dossier.habits.isNotBlank()) habits = dossier.habits
        if (dossier.speechStyle.isNotBlank()) speechStyle = dossier.speechStyle
        if (dossier.catchphrases.isNotBlank()) catchphrases = dossier.catchphrases
        if (dossier.behavioralTraits.isNotBlank()) behavioralTraits = dossier.behavioralTraits
        if (dossier.detailedDescription.isNotBlank()) detailedDescription = dossier.detailedDescription
        if (dossier.preferences.isNotBlank()) preferences = dossier.preferences
        if (dossier.dislikes.isNotBlank()) dislikes = dossier.dislikes
        if (dossier.interests.isNotBlank()) interests = dossier.interests
        if (dossier.skills.isNotBlank()) skills = dossier.skills
        if (dossier.inventorySummary.isNotBlank()) inventorySummary = dossier.inventorySummary
        if (dossier.goals.isNotBlank()) goals = dossier.goals
        if (dossier.motivation.isNotBlank()) motivation = dossier.motivation
        if (dossier.knownFacts.isNotBlank()) knownFacts = dossier.knownFacts
        if (dossier.fearsAndLimitations.isNotBlank()) fearsAndLimitations = dossier.fearsAndLimitations
        if (dossier.additionalNotes.isNotBlank()) additionalNotes = dossier.additionalNotes
        if (dossier.currentLocationName.isNotBlank()) currentLocationName = dossier.currentLocationName
        if (dossier.currentMoodAndCondition.isNotBlank()) currentMood = dossier.currentMoodAndCondition
        if (dossier.visualPassportPrompt.isNotBlank()) visualPassport = dossier.visualPassportPrompt
        if (dossier.relationshipsSummary.isNotBlank()) relationshipsSummary = dossier.relationshipsSummary
        if (dossier.savedPrompts.isNotEmpty()) {
            savedPromptsJson = characterMediaJson.encodeToString(dossier.savedPrompts)
        }
        if (dossier.declaredRelationships.isNotEmpty()) {
            declaredRelationshipsJson = characterMediaJson.encodeToString(dossier.declaredRelationships)
        }
        sourceFileName = fileName
    }

    val anyFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fName = uri.lastPathSegment?.substringAfterLast('/') ?: "character_dossier.doc"
            onExtractFromUri(uri, fName) { dossier, srcName ->
                applyExtractedDossier(dossier, srcName)
            }
        }
    }

    val dialogAvatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            encodeUriToBase64(context, uri)?.let { avatarBase64 = it }
        }
    }

    var showDiscardDialogWarning by remember { mutableStateOf(false) }

    val hasDialogChanges = remember(
        name, fullName, aliases, gender, speciesOrOrigin, storyRole, characterStatus,
        factionOrGroup, tagsCsv, birthDate, age, birthPlace, occupation, shortDescription,
        appearance, physique, hair, eyes, defaultClothing, distinguishingMarks, voiceDescription,
        personality, archetype, strengths, weaknesses, internalConflict,
        habits, speechStyle, catchphrases, behavioralTraits, detailedDescription,
        preferences, dislikes, interests, skills, inventorySummary,
        goals, motivation, knownFacts, fearsAndLimitations, additionalNotes,
        currentLocationName, currentMood, currentOutfit, currentActiveGoal,
        visualPassport, relationshipsSummary, avatarBase64, canonStatus, initialCharacter
    ) {
        name != initialCharacter.name ||
            fullName != initialCharacter.fullName ||
            aliases != initialCharacter.aliases ||
            gender != initialCharacter.gender ||
            speciesOrOrigin != initialCharacter.speciesOrOrigin ||
            storyRole != initialCharacter.storyRole ||
            characterStatus != initialCharacter.characterStatus ||
            factionOrGroup != initialCharacter.factionOrGroup ||
            tagsCsv != initialCharacter.tagsCsv ||
            birthDate != initialCharacter.birthDate ||
            age != initialCharacter.age ||
            birthPlace != initialCharacter.birthPlace ||
            occupation != initialCharacter.occupation ||
            shortDescription != initialCharacter.shortDescription ||
            appearance != initialCharacter.appearance ||
            physique != initialCharacter.physique ||
            hair != initialCharacter.hair ||
            eyes != initialCharacter.eyes ||
            defaultClothing != initialCharacter.defaultClothing ||
            distinguishingMarks != initialCharacter.distinguishingMarks ||
            voiceDescription != initialCharacter.voiceDescription ||
            personality != initialCharacter.personality ||
            archetype != initialCharacter.archetype ||
            strengths != initialCharacter.strengths ||
            weaknesses != initialCharacter.weaknesses ||
            internalConflict != initialCharacter.internalConflict ||
            habits != initialCharacter.habits ||
            speechStyle != initialCharacter.speechStyle ||
            catchphrases != initialCharacter.catchphrases ||
            behavioralTraits != initialCharacter.behavioralTraits ||
            detailedDescription != initialCharacter.detailedDescription ||
            preferences != initialCharacter.preferences ||
            dislikes != initialCharacter.dislikes ||
            interests != initialCharacter.interests ||
            skills != initialCharacter.skills ||
            inventorySummary != initialCharacter.inventorySummary ||
            goals != initialCharacter.goals ||
            motivation != initialCharacter.motivation ||
            knownFacts != initialCharacter.knownFacts ||
            fearsAndLimitations != initialCharacter.fearsAndLimitations ||
            additionalNotes != initialCharacter.additionalNotes ||
            currentLocationName != initialCharacter.currentLocationName ||
            currentMood != initialCharacter.currentMoodAndCondition ||
            currentOutfit != initialCharacter.currentOutfit ||
            currentActiveGoal != initialCharacter.currentActiveGoal ||
            visualPassport != initialCharacter.visualPassportPrompt ||
            relationshipsSummary != initialCharacter.relationshipsSummary ||
            avatarBase64 != initialCharacter.avatarBase64 ||
            canonStatus != initialCharacter.canonStatus
    }

    val requestDismiss = {
        if (hasDialogChanges) {
            showDiscardDialogWarning = true
        } else {
            onDismiss()
        }
    }

    if (showDiscardDialogWarning) {
        AlertDialog(
            onDismissRequest = { showDiscardDialogWarning = false },
            title = { Text("Несохранённые изменения") },
            text = {
                Text("В форме персонажа есть несохранённые изменения. Вы уверены, что хотите закрыть редактор без сохранения?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialogWarning = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("confirm_discard_dialog_changes_button")
                ) {
                    Text("Закрыть без сохранения")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialogWarning = false },
                    modifier = Modifier.testTag("cancel_discard_dialog_changes_button")
                ) {
                    Text("Продолжить редактирование")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = requestDismiss,
        title = {
            Text(if (isNewCharacter) "Создание карточки персонажа" else "Полное редактирование: ${initialCharacter.name}")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(500.dp)
            ) {
                // 1. AI File Auto-Fill Banner (.DOC, .DOCX, .PDF, .TXT, .MD, .XLSX)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BurgundySurfaceElevated),
                        border = BorderStroke(1.dp, CrimsonPrimary)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberGoldSecondary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ИИ-чтение досье из любого файла (.DOC, .DOCX, .PDF, .TXT, .XLSX)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextIvoryPrimary
                                )
                            }
                            Text(
                                text = "Подгрузите файл персонажа — нейросеть сама прочитает его, заполнит все поля карточки, промпты генерации и автоматически подтянет связи с другими персонажами.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedRose
                            )
                            Button(
                                onClick = { anyFileLauncher.launch(arrayOf("*/*")) },
                                enabled = !isExtractingFile,
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upload_character_file_ai_button")
                            ) {
                                if (isExtractingFile) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = TextIvoryPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Подгрузить любой файл (.DOC / .DOCX / .PDF / .TXT)")
                            }

                            Text(
                                text = "Или протестируйте чтение готового .DOC файла одним нажатием:",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberGoldSecondary
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AssistChip(
                                    onClick = {
                                        onExtractFromSampleDoc("Маркус_Вейн_Досье.doc") { dossier, fName ->
                                            applyExtractedDossier(dossier, fName)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    label = { Text("Демо .DOC: Маркус Вейн") },
                                    modifier = Modifier.testTag("demo_doc_marcus_button")
                                )
                                AssistChip(
                                    onClick = {
                                        onExtractFromSampleDoc("Лидия_Морган_Досье.doc") { dossier, fName ->
                                            applyExtractedDossier(dossier, fName)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    label = { Text("Демо .DOC: Лидия Морган") },
                                    modifier = Modifier.testTag("demo_doc_lidia_button")
                                )
                            }

                            if (sourceFileName.isNotBlank()) {
                                val parsedRels = parseCharacterDeclaredRelations(declaredRelationshipsJson)
                                Text(
                                    text = "✓ Загружено из «$sourceFileName» • Обнаружено связей: ${parsedRels.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusCanonGreen
                                )
                            }
                        }
                    }
                }

                // 2. Avatar Upload inside Dialog
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CharacterAvatarThumbnail(name = name.ifBlank { "RW" }, avatarBase64 = avatarBase64, size = 54.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    dialogAvatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (avatarBase64.isNotBlank()) "Сменить аватар" else "Выбрать аватар")
                            }
                            if (avatarBase64.isNotBlank()) {
                                TextButton(onClick = { avatarBase64 = "" }) {
                                    Text("Удалить аватар", color = CrimsonPrimary)
                                }
                            }
                        }
                    }
                }

                // 3. Canon Status & Role Category
                item {
                    Text("Статус достоверности данных:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CanonStatus.entries.forEach { st ->
                            FilterChip(
                                selected = canonStatus == st.name,
                                onClick = { canonStatus = st.name },
                                label = { Text(st.label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }

                item {
                    Text("Категория / Роль персонажа:", style = MaterialTheme.typography.labelMedium)
                    val roleOptions = listOf("Главный герой", "Второстепенный персонаж", "Антагонист", "Ключевой персонаж", "Эпизодический / NPC")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        roleOptions.forEach { rOpt ->
                            FilterChip(
                                selected = storyRole == rOpt,
                                onClick = { storyRole = rOpt },
                                label = { Text(rOpt, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }

                // SECTION 1: Основная информация
                item { Text("1. Основная информация", style = MaterialTheme.typography.titleSmall, color = AmberGoldSecondary) }
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя персонажа *") }, modifier = Modifier.fillMaxWidth().testTag("dialog_character_name_input")) }
                item { OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Полное имя") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = aliases, onValueChange = { aliases = it }, label = { Text("Псевдонимы / Титулы") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Пол") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Возраст") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Дата рождения") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = birthPlace, onValueChange = { birthPlace = it }, label = { Text("Место рождения") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(value = speciesOrOrigin, onValueChange = { speciesOrOrigin = it }, label = { Text("Раса / Происхождение") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = occupation, onValueChange = { occupation = it }, label = { Text("Род занятий / Профессия") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = factionOrGroup, onValueChange = { factionOrGroup = it }, label = { Text("Фракция / Группа / Дом") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = characterStatus, onValueChange = { characterStatus = it }, label = { Text("Статус персонажа (Жив, пропал и т.д.)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = shortDescription, onValueChange = { shortDescription = it }, label = { Text("Краткое описание (подзаголовок)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = tagsCsv, onValueChange = { tagsCsv = it }, label = { Text("Теги (через запятую)") }, modifier = Modifier.fillMaxWidth()) }

                // SECTION 2: Внешность
                item { Text("2. Внешность", style = MaterialTheme.typography.titleSmall, color = AmberGoldSecondary) }
                item { OutlinedTextField(value = appearance, onValueChange = { appearance = it }, label = { Text("Общее описание внешности") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = physique, onValueChange = { physique = it }, label = { Text("Рост и телосложение") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = hair, onValueChange = { hair = it }, label = { Text("Волосы") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = eyes, onValueChange = { eyes = it }, label = { Text("Глаза") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(value = defaultClothing, onValueChange = { defaultClothing = it }, label = { Text("Повседневная одежда и аксессуары") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = distinguishingMarks, onValueChange = { distinguishingMarks = it }, label = { Text("Особые приметы (шрамы, тату)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = voiceDescription, onValueChange = { voiceDescription = it }, label = { Text("Голос и тембр") }, modifier = Modifier.fillMaxWidth()) }

                // SECTION 3: Характер и личность
                item { Text("3. Характер и личность", style = MaterialTheme.typography.titleSmall, color = AmberGoldSecondary) }
                item { OutlinedTextField(value = personality, onValueChange = { personality = it }, label = { Text("Характер и темперамент") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = archetype, onValueChange = { archetype = it }, label = { Text("Архетип личности") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = strengths, onValueChange = { strengths = it }, label = { Text("Сильные стороны") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = weaknesses, onValueChange = { weaknesses = it }, label = { Text("Слабости и уязвимости") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = internalConflict, onValueChange = { internalConflict = it }, label = { Text("Внутренний конфликт") }, modifier = Modifier.fillMaxWidth()) }

                // SECTION 4: Поведение и привычки
                item { Text("4. Поведение и привычки", style = MaterialTheme.typography.titleSmall, color = AmberGoldSecondary) }
                item { OutlinedTextField(value = habits, onValueChange = { habits = it }, label = { Text("Привычки и манеры") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = speechStyle, onValueChange = { speechStyle = it }, label = { Text("Манера речи") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = catchphrases, onValueChange = { catchphrases = it }, label = { Text("Любимые фразы") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = behavioralTraits, onValueChange = { behavioralTraits = it }, label = { Text("Поведенческие особенности и реакции") }, modifier = Modifier.fillMaxWidth()) }

                // SECTION 5: Биография
                item { Text("5. Биография", style = MaterialTheme.typography.titleSmall, color = AmberGoldSecondary) }
                item { OutlinedTextField(value = detailedDescription, onValueChange = { detailedDescription = it }, label = { Text("Биография и предыстория") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }

                // SECTION 6: Предпочтения
                item { Text("6. Предпочтения", style = MaterialTheme.typography.titleSmall, color = AmberGoldSecondary) }
                item { OutlinedTextField(value = preferences, onValueChange = { preferences = it }, label = { Text("Предпочтения / Что любит") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = dislikes, onValueChange = { dislikes = it }, label = { Text("Антипатии / Что не любит") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = interests, onValueChange = { interests = it }, label = { Text("Хобби и увлечения") }, modifier = Modifier.fillMaxWidth()) }

                // SECTION 7: Отношения и связи
                item { Text("7. Отношения и связи", style = MaterialTheme.typography.titleSmall, color = AmberGoldSecondary) }
                item { OutlinedTextField(value = relationshipsSummary, onValueChange = { relationshipsSummary = it }, label = { Text("Связи с другими персонажами (подтягиваются автоматически)") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }

                // SECTION 8: Навыки и способности
                item { Text("8. Навыки и способности", style = MaterialTheme.typography.titleSmall, color = AmberGoldSecondary) }
                item { OutlinedTextField(value = skills, onValueChange = { skills = it }, label = { Text("Навыки и способности") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = inventorySummary, onValueChange = { inventorySummary = it }, label = { Text("Снаряжение и личные предметы") }, modifier = Modifier.fillMaxWidth()) }

                // SECTION 9: Роль в мире и Дополнительные сведения
                item { Text("9. Роль в мире и Дополнительные сведения", style = MaterialTheme.typography.titleSmall, color = AmberGoldSecondary) }
                item { OutlinedTextField(value = goals, onValueChange = { goals = it }, label = { Text("Глобальные цели") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = motivation, onValueChange = { motivation = it }, label = { Text("Мотивация") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = knownFacts, onValueChange = { knownFacts = it }, label = { Text("Известные факты и тайны мира") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = fearsAndLimitations, onValueChange = { fearsAndLimitations = it }, label = { Text("Страхи, табу и ограничения") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = visualPassport, onValueChange = { visualPassport = it }, label = { Text("Паспорт внешности и базовый промпт (Image Studio)") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = currentLocationName, onValueChange = { currentLocationName = it }, label = { Text("Текущая локация (Слой 2)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = additionalNotes, onValueChange = { additionalNotes = it }, label = { Text("Дополнительные сведения") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = customJson, onValueChange = { customJson = it }, label = { Text("Пользовательские поля (JSON)") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            initialCharacter.copy(
                                name = name.trim(),
                                fullName = fullName.trim(),
                                aliases = aliases.trim(),
                                gender = gender.trim(),
                                speciesOrOrigin = speciesOrOrigin.trim(),
                                storyRole = storyRole.trim().ifEmpty { "Второстепенный персонаж" },
                                characterStatus = characterStatus.trim().ifEmpty { "Жив" },
                                factionOrGroup = factionOrGroup.trim(),
                                tagsCsv = tagsCsv.trim(),
                                birthDate = birthDate.trim(),
                                age = age.trim(),
                                birthPlace = birthPlace.trim(),
                                occupation = occupation.trim(),
                                shortDescription = shortDescription.trim(),
                                appearance = appearance.trim(),
                                physique = physique.trim(),
                                hair = hair.trim(),
                                eyes = eyes.trim(),
                                defaultClothing = defaultClothing.trim(),
                                distinguishingMarks = distinguishingMarks.trim(),
                                voiceDescription = voiceDescription.trim(),
                                personality = personality.trim(),
                                archetype = archetype.trim(),
                                strengths = strengths.trim(),
                                weaknesses = weaknesses.trim(),
                                internalConflict = internalConflict.trim(),
                                habits = habits.trim(),
                                speechStyle = speechStyle.trim(),
                                catchphrases = catchphrases.trim(),
                                behavioralTraits = behavioralTraits.trim(),
                                detailedDescription = detailedDescription.trim(),
                                preferences = preferences.trim(),
                                dislikes = dislikes.trim(),
                                interests = interests.trim(),
                                skills = skills.trim(),
                                inventorySummary = inventorySummary.trim(),
                                goals = goals.trim(),
                                motivation = motivation.trim(),
                                knownFacts = knownFacts.trim(),
                                fearsAndLimitations = fearsAndLimitations.trim(),
                                additionalNotes = additionalNotes.trim(),
                                currentLocationName = currentLocationName.trim().ifEmpty { "Красный особняк" },
                                currentMoodAndCondition = currentMood.trim(),
                                currentOutfit = currentOutfit.trim(),
                                currentActiveGoal = currentActiveGoal.trim(),
                                visualPassportPrompt = visualPassport.trim(),
                                relationshipsSummary = relationshipsSummary.trim(),
                                avatarBase64 = avatarBase64,
                                savedPromptsJson = savedPromptsJson,
                                declaredRelationshipsJson = declaredRelationshipsJson,
                                sourceFileName = sourceFileName,
                                customFieldsJson = customJson.trim(),
                                canonStatus = canonStatus
                            ),
                            if (isNewCharacter) {
                                if (sourceFileName.isNotBlank()) "Создание карточки персонажа из файла «$sourceFileName»"
                                else "Создание карточки персонажа"
                            } else {
                                "Обновление досье персонажа"
                            }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.testTag("confirm_save_character_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Сохранить и подтянуть связи")
            }
        },
        dismissButton = {
            TextButton(onClick = requestDismiss) { Text("Отмена") }
        }
    )
}

private fun encodeUriToBase64(context: android.content.Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bmp = BitmapFactory.decodeStream(stream) ?: return null
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 75, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }
    } catch (_: Exception) {
        null
    }
}
