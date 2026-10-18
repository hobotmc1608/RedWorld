package com.example.ui.screens

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CanonStatus
import com.example.data.local.CharacterEntity
import com.example.ui.components.CanonStatusBadge
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.BurgundySurfaceElevated
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.StatusCanonGreen
import com.example.ui.theme.TextIvoryPrimary
import com.example.ui.theme.TextMutedRose
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class CharacterFieldSpec(
    val key: String,
    val label: String,
    val isMultiline: Boolean = false,
    val supportsSubItemEditing: Boolean = false,
    val subItemDelimiter: String = "\n",
    val getter: (CharacterEntity) -> String,
    val setter: (CharacterEntity, String) -> CharacterEntity
)

data class CharacterThematicSectionSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val fields: List<CharacterFieldSpec>
)

private val customFieldsJsonParser = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun parseCustomFieldsMap(rawJson: String): Map<String, String> {
    if (rawJson.isBlank() || rawJson.trim() == "{}") return emptyMap()
    return try {
        val obj = customFieldsJsonParser.parseToJsonElement(rawJson).jsonObject
        obj.mapValues { (_, v) ->
            try {
                v.jsonPrimitive.content
            } catch (_: Exception) {
                v.toString()
            }
        }
    } catch (_: Exception) {
        mapOf("Заметка" to rawJson)
    }
}

fun encodeCustomFieldsMap(map: Map<String, String>): String {
    if (map.isEmpty()) return "{}"
    val obj = JsonObject(map.mapValues { (_, v) -> JsonPrimitive(v) })
    return customFieldsJsonParser.encodeToString(JsonObject.serializer(), obj)
}

fun getCharacterThematicSections(): List<CharacterThematicSectionSpec> = listOf(
    CharacterThematicSectionSpec(
        id = "basic",
        title = "1. Основная информация",
        subtitle = "Имя, полное имя, псевдонимы, категория, статус, возраст, происхождение и род занятий",
        fields = listOf(
            CharacterFieldSpec("name", "Имя персонажа", getter = { it.name }, setter = { c, v -> c.copy(name = v) }),
            CharacterFieldSpec("fullName", "Полное имя", getter = { it.fullName }, setter = { c, v -> c.copy(fullName = v) }),
            CharacterFieldSpec("aliases", "Псевдонимы и титулы", getter = { it.aliases }, setter = { c, v -> c.copy(aliases = v) }),
            CharacterFieldSpec("storyRole", "Категория / Роль в сюжете", getter = { it.storyRole }, setter = { c, v -> c.copy(storyRole = v) }),
            CharacterFieldSpec("characterStatus", "Статус персонажа", getter = { it.characterStatus }, setter = { c, v -> c.copy(characterStatus = v) }),
            CharacterFieldSpec("gender", "Пол", getter = { it.gender }, setter = { c, v -> c.copy(gender = v) }),
            CharacterFieldSpec("speciesOrOrigin", "Происхождение / Вид", getter = { it.speciesOrOrigin }, setter = { c, v -> c.copy(speciesOrOrigin = v) }),
            CharacterFieldSpec("age", "Возраст", getter = { it.age }, setter = { c, v -> c.copy(age = v) }),
            CharacterFieldSpec("birthDate", "Дата рождения", getter = { it.birthDate }, setter = { c, v -> c.copy(birthDate = v) }),
            CharacterFieldSpec("birthPlace", "Место рождения", getter = { it.birthPlace }, setter = { c, v -> c.copy(birthPlace = v) }),
            CharacterFieldSpec("occupation", "Работа / Род занятий", getter = { it.occupation }, setter = { c, v -> c.copy(occupation = v) }),
            CharacterFieldSpec("factionOrGroup", "Фракция / Организация / Дом", getter = { it.factionOrGroup }, setter = { c, v -> c.copy(factionOrGroup = v) }),
            CharacterFieldSpec("tagsCsv", "Теги", supportsSubItemEditing = true, subItemDelimiter = ",", getter = { it.tagsCsv }, setter = { c, v -> c.copy(tagsCsv = v) })
        )
    ),
    CharacterThematicSectionSpec(
        id = "appearance",
        title = "2. Внешность",
        subtitle = "Общий облик, телосложение, волосы, глаза, одежда, особые приметы и голос",
        fields = listOf(
            CharacterFieldSpec("appearance", "Общее описание внешности", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = "\n", getter = { it.appearance }, setter = { c, v -> c.copy(appearance = v) }),
            CharacterFieldSpec("physique", "Телосложение и рост", getter = { it.physique }, setter = { c, v -> c.copy(physique = v) }),
            CharacterFieldSpec("hair", "Волосы", getter = { it.hair }, setter = { c, v -> c.copy(hair = v) }),
            CharacterFieldSpec("eyes", "Глаза", getter = { it.eyes }, setter = { c, v -> c.copy(eyes = v) }),
            CharacterFieldSpec("defaultClothing", "Повседневная одежда и стиль", isMultiline = true, getter = { it.defaultClothing }, setter = { c, v -> c.copy(defaultClothing = v) }),
            CharacterFieldSpec("distinguishingMarks", "Особые приметы", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.distinguishingMarks }, setter = { c, v -> c.copy(distinguishingMarks = v) }),
            CharacterFieldSpec("voiceDescription", "Голос и тембр", getter = { it.voiceDescription }, setter = { c, v -> c.copy(voiceDescription = v) }),
            CharacterFieldSpec("visualPassportPrompt", "Паспорт внешности (Image Studio Prompt)", isMultiline = true, getter = { it.visualPassportPrompt }, setter = { c, v -> c.copy(visualPassportPrompt = v) })
        )
    ),
    CharacterThematicSectionSpec(
        id = "personality",
        title = "3. Характер и личность",
        subtitle = "Черты характера, архетип, сильные и слабые стороны, внутренний конфликт",
        fields = listOf(
            CharacterFieldSpec("personality", "Характер и ключевые черты", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = "\n", getter = { it.personality }, setter = { c, v -> c.copy(personality = v) }),
            CharacterFieldSpec("archetype", "Архетип / Темперамент", getter = { it.archetype }, setter = { c, v -> c.copy(archetype = v) }),
            CharacterFieldSpec("strengths", "Сильные стороны", supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.strengths }, setter = { c, v -> c.copy(strengths = v) }),
            CharacterFieldSpec("weaknesses", "Слабости и уязвимости", supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.weaknesses }, setter = { c, v -> c.copy(weaknesses = v) }),
            CharacterFieldSpec("internalConflict", "Внутренний конфликт и убеждения", isMultiline = true, getter = { it.internalConflict }, setter = { c, v -> c.copy(internalConflict = v) })
        )
    ),
    CharacterThematicSectionSpec(
        id = "behavior",
        title = "4. Поведение и привычки",
        subtitle = "Отдельные привычки, манера речи, любимые фразы и поведенческие реакции",
        fields = listOf(
            CharacterFieldSpec("habits", "Привычки", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.habits }, setter = { c, v -> c.copy(habits = v) }),
            CharacterFieldSpec("speechStyle", "Манера речи", isMultiline = true, getter = { it.speechStyle }, setter = { c, v -> c.copy(speechStyle = v) }),
            CharacterFieldSpec("catchphrases", "Любимые выражения и фразы", supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.catchphrases }, setter = { c, v -> c.copy(catchphrases = v) }),
            CharacterFieldSpec("behavioralTraits", "Поведенческие особенности и реакции", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = "\n", getter = { it.behavioralTraits }, setter = { c, v -> c.copy(behavioralTraits = v) })
        )
    ),
    CharacterThematicSectionSpec(
        id = "biography",
        title = "5. Биография",
        subtitle = "Краткое описание, подробная история жизни, личные тайны и секреты",
        fields = listOf(
            CharacterFieldSpec("shortDescription", "Краткое описание персонажа", isMultiline = true, getter = { it.shortDescription }, setter = { c, v -> c.copy(shortDescription = v) }),
            CharacterFieldSpec("detailedDescription", "Подробная биография и предыстория", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = "\n", getter = { it.detailedDescription }, setter = { c, v -> c.copy(detailedDescription = v) }),
            CharacterFieldSpec("personalSecrets", "Личные тайны и скрытые факты", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.personalSecrets }, setter = { c, v -> c.copy(personalSecrets = v) })
        )
    ),
    CharacterThematicSectionSpec(
        id = "preferences",
        title = "6. Предпочтения",
        subtitle = "Вкусы, симпатии, антипатии, интересы и хобби",
        fields = listOf(
            CharacterFieldSpec("preferences", "Предпочтения и вкусы", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.preferences }, setter = { c, v -> c.copy(preferences = v) }),
            CharacterFieldSpec("dislikes", "Что не нравится / Раздражает", supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.dislikes }, setter = { c, v -> c.copy(dislikes = v) }),
            CharacterFieldSpec("interests", "Интересы и хобби", supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.interests }, setter = { c, v -> c.copy(interests = v) })
        )
    ),
    CharacterThematicSectionSpec(
        id = "relationships",
        title = "7. Отношения",
        subtitle = "Текстовая сводка взаимоотношений с персонажами мира",
        fields = listOf(
            CharacterFieldSpec("relationshipsSummary", "Описание отношений и связей", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.relationshipsSummary }, setter = { c, v -> c.copy(relationshipsSummary = v) })
        )
    ),
    CharacterThematicSectionSpec(
        id = "skills",
        title = "8. Навыки и способности",
        subtitle = "Профессиональные умения, способности и личный инвентарь",
        fields = listOf(
            CharacterFieldSpec("skills", "Навыки и способности", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.skills }, setter = { c, v -> c.copy(skills = v) }),
            CharacterFieldSpec("inventorySummary", "Снаряжение и знаковые предметы", supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.inventorySummary }, setter = { c, v -> c.copy(inventorySummary = v) })
        )
    ),
    CharacterThematicSectionSpec(
        id = "world_role",
        title = "9. Роль в мире",
        subtitle = "Глобальные цели, мотивация, известные факты, табу и ограничения",
        fields = listOf(
            CharacterFieldSpec("goals", "Глобальные цели", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.goals }, setter = { c, v -> c.copy(goals = v) }),
            CharacterFieldSpec("motivation", "Внутренняя мотивация", isMultiline = true, getter = { it.motivation }, setter = { c, v -> c.copy(motivation = v) }),
            CharacterFieldSpec("knownFacts", "Известные персонажу факты и тайны", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = ";", getter = { it.knownFacts }, setter = { c, v -> c.copy(knownFacts = v) }),
            CharacterFieldSpec("unknownFactsRestriction", "Чего персонаж не знает (ограничение знаний)", isMultiline = true, getter = { it.unknownFactsRestriction }, setter = { c, v -> c.copy(unknownFactsRestriction = v) }),
            CharacterFieldSpec("fearsAndLimitations", "Страхи, табу и ограничения", isMultiline = true, getter = { it.fearsAndLimitations }, setter = { c, v -> c.copy(fearsAndLimitations = v) }),
            CharacterFieldSpec("fearsAndTaboos", "Дополнительные табу", isMultiline = true, getter = { it.fearsAndTaboos }, setter = { c, v -> c.copy(fearsAndTaboos = v) }),
            CharacterFieldSpec("limitations", "Сюжетные и физические ограничения", isMultiline = true, getter = { it.limitations }, setter = { c, v -> c.copy(limitations = v) })
        )
    ),
    CharacterThematicSectionSpec(
        id = "additional",
        title = "10. Дополнительные сведения",
        subtitle = "Авторские комментарии, заметки и особые пометки по персонажу",
        fields = listOf(
            CharacterFieldSpec("additionalNotes", "Дополнительные сведения и заметки", isMultiline = true, supportsSubItemEditing = true, subItemDelimiter = "\n", getter = { it.additionalNotes }, setter = { c, v -> c.copy(additionalNotes = v) })
        )
    )
)

/**
 * 3.1. Верхняя часть карточки персонажа:
 * - Аватар персонажа (без искажений)
 * - Имя персонажа + краткая характеристика под именем
 * - Кнопка изменения аватара (+ удаления аватара)
 * - Кнопка общего редактирования
 * - Меню дополнительных действий («Клонировать персонажа», «Удалить персонажа», экспорт, избранное, закрепление)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterCardHeaderSection(
    character: CharacterEntity,
    isDedicatedPageMode: Boolean = true,
    hasUnsavedChanges: Boolean = false,
    inlineNameDraft: String? = null,
    onStartInlineNameEdit: (CharacterFieldSpec) -> Unit = {},
    onUpdateInlineNameDraft: (String) -> Unit = {},
    onSaveInlineNameEdit: (CharacterFieldSpec) -> Unit = {},
    onCancelInlineNameEdit: () -> Unit = {},
    onSaveAllUnsavedChanges: () -> Unit = {},
    onDiscardAllUnsavedChanges: () -> Unit = {},
    onBackToList: () -> Unit,
    onUpdateAvatar: (String) -> Unit,
    onClearAvatar: () -> Unit,
    onOpenFullEdit: () -> Unit,
    onCloneCharacter: () -> Unit,
    onRequestDeleteCharacter: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleArchived: () -> Unit,
    onExportCharacterJson: () -> Unit,
    onExportCharacterMarkdown: () -> Unit,
    onInlineEditField: (CharacterFieldSpec) -> Unit
) {
    val context = LocalContext.current
    var showOverflowMenu by remember { mutableStateOf(false) }

    val headerAvatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        val out = java.io.ByteArrayOutputStream()
                        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, out)
                        val b64 = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
                        onUpdateAvatar(b64)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    val nameSpec = remember {
        CharacterFieldSpec("name", "Имя персонажа", getter = { it.name }, setter = { c, v -> c.copy(name = v) })
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("character_detail_header_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Верхняя панель карточки: Кнопка «Назад», избранное, закрепление, статус канона и меню действий с карточкой
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBackToList,
                    modifier = Modifier.testTag("back_to_character_list_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад к списку персонажей",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Назад")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("toggle_favorite_character_button")
                    ) {
                        Icon(
                            imageVector = if (character.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Избранное",
                            tint = if (character.isFavorite) AmberGoldSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onTogglePinned,
                        modifier = Modifier.testTag("toggle_pin_character_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Закрепить",
                            tint = if (character.isPinned) CrimsonPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CanonStatusBadge(statusName = character.canonStatus)
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("character_more_actions_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню действий с карточкой")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (character.avatarBase64.isNotBlank()) "Изменить аватар" else "Загрузить аватар") },
                                leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    headerAvatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("change_avatar_menu_item")
                            )
                            if (character.avatarBase64.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("Удалить аватар", color = CrimsonPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CrimsonPrimary) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onClearAvatar()
                                    },
                                    modifier = Modifier.testTag("remove_avatar_menu_item")
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Редактировать все поля") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenFullEdit()
                                },
                                modifier = Modifier.testTag("edit_full_character_menu_item")
                            )
                            DropdownMenuItem(
                                text = { Text("Клонировать персонажа") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onCloneCharacter()
                                },
                                modifier = Modifier.testTag("clone_character_menu_item")
                            )
                            DropdownMenuItem(
                                text = { Text(if (character.isArchived) "Вернуть из архива" else "Переместить в архив") },
                                leadingIcon = {
                                    Icon(
                                        if (character.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onToggleArchived()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Экспортировать карточку (JSON)") },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onExportCharacterJson()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Экспортировать досье (Markdown)") },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onExportCharacterMarkdown()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Удалить персонажа", color = CrimsonPrimary) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CrimsonPrimary) },
                                onClick = {
                                    showOverflowMenu = false
                                    onRequestDeleteCharacter()
                                },
                                modifier = Modifier.testTag("delete_character_menu_item")
                            )
                        }
                    }
                }
            }

            // Баннер несохранённых изменений (если пользователь начал редактировать поля на карточке)
            AnimatedVisibility(visible = hasUnsavedChanges) {
                Surface(
                    color = BurgundySurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AmberGoldSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unsaved_changes_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Есть несохранённые изменения в карточке",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberGoldSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = onDiscardAllUnsavedChanges,
                                modifier = Modifier.testTag("discard_all_character_changes_button")
                            ) {
                                Text("Сбросить")
                            }
                            Button(
                                onClick = onSaveAllUnsavedChanges,
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                modifier = Modifier.testTag("save_all_character_changes_button")
                            ) {
                                Text("Сохранить всё")
                            }
                        }
                    }
                }
            }

            // Main Identity Row: Avatar + Name + Short summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CharacterAvatarThumbnail(
                    name = character.name,
                    avatarBase64 = character.avatarBase64,
                    size = 86.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (inlineNameDraft != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = inlineNameDraft,
                                onValueChange = onUpdateInlineNameDraft,
                                label = { Text("Имя персонажа") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("inline_field_input_name")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onSaveInlineNameEdit(nameSpec) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    modifier = Modifier.testTag("save_inline_field_name")
                                ) {
                                    Text("Сохранить")
                                }
                                TextButton(
                                    onClick = onCancelInlineNameEdit,
                                    modifier = Modifier.testTag("cancel_inline_field_name")
                                ) {
                                    Text("Отмена")
                                }
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = character.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .testTag("character_card_header_name")
                            )
                            IconButton(
                                onClick = { onStartInlineNameEdit(nameSpec) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("inline_edit_character_name_button")
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Изменить имя персонажа",
                                    tint = AmberGoldSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    val subtitleParts = buildList {
                        if (character.storyRole.isNotBlank()) add(character.storyRole)
                        if (character.occupation.isNotBlank()) add(character.occupation)
                        if (character.age.isNotBlank()) add("${character.age} лет")
                    }
                    if (subtitleParts.isNotEmpty()) {
                        Text(
                            text = subtitleParts.joinToString(" • "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmberGoldSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (character.aliases.isNotBlank()) {
                        Text(
                            text = "Псевдонимы: ${character.aliases}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val shortSummary = character.shortDescription
                        .ifBlank { character.personality }
                        .ifBlank { character.appearance }
                        .ifBlank { character.detailedDescription }
                    if (shortSummary.isNotBlank()) {
                        Text(
                            text = shortSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "Версия карточки v${character.versionNumber}" +
                            if (character.sourceFileName.isNotBlank()) " • Источник: ${character.sourceFileName}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusCanonGreen
                    )
                }
            }

            // Action Buttons Row: Change Avatar, Remove Avatar, Full Edit, Clone, Delete
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        headerAvatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("change_character_avatar_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (character.avatarBase64.isNotBlank()) "Изменить аватар" else "Загрузить аватар")
                }

                if (character.avatarBase64.isNotBlank()) {
                    TextButton(
                        onClick = onClearAvatar,
                        modifier = Modifier.testTag("remove_character_avatar_button")
                    ) {
                        Text("Удалить аватар", color = CrimsonPrimary)
                    }
                }

                Button(
                    onClick = onOpenFullEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("edit_character_card_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Редактировать / Загрузить файл")
                }

                OutlinedButton(
                    onClick = onCloneCharacter,
                    modifier = Modifier.testTag("clone_character_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Клонировать")
                }

                OutlinedButton(
                    onClick = onRequestDeleteCharacter,
                    modifier = Modifier.testTag("delete_character_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Удалить", color = CrimsonPrimary)
                }
            }
        }
    }
}

/**
 * 3.2 & 4. Тематический раздел карточки персонажа с поддержкой независимого редактирования
 * каждого отдельного поля (как прямо внутри карточки inline, так и в модальном диалоге),
 * каждого отдельного абзаца или отдельного пункта списка!
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterThematicSectionCard(
    section: CharacterThematicSectionSpec,
    character: CharacterEntity,
    inlineFieldDrafts: Map<String, String> = emptyMap(),
    onStartInlineFieldEdit: (CharacterFieldSpec) -> Unit = {},
    onUpdateInlineFieldDraft: (String, String) -> Unit = { _, _ -> },
    onSaveInlineFieldEdit: (CharacterFieldSpec) -> Unit = {},
    onCancelInlineFieldEdit: (String) -> Unit = {},
    onEditSingleField: (CharacterFieldSpec) -> Unit,
    onEditSubItem: (CharacterFieldSpec, Int, List<String>, String) -> Unit,
    onAddSubItem: (CharacterFieldSpec, List<String>, String) -> Unit
) {
    val filledOrEditingFields = remember(character, section, inlineFieldDrafts) {
        section.fields.filter {
            it.getter(character).isNotBlank() || inlineFieldDrafts.containsKey(it.key)
        }
    }
    val emptyFields = remember(character, section, inlineFieldDrafts) {
        section.fields.filter {
            it.getter(character).isBlank() && !inlineFieldDrafts.containsKey(it.key)
        }
    }

    var isSectionExpanded by remember { mutableStateOf(true) }
    var showAddFieldPicker by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("character_section_${section.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isSectionExpanded = !isSectionExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${section.title} (${filledOrEditingFields.size}/${section.fields.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary
                    )
                    Text(
                        text = section.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { isSectionExpanded = !isSectionExpanded }) {
                    Icon(
                        if (isSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isSectionExpanded) "Свернуть раздел" else "Развернуть раздел"
                    )
                }
            }

            AnimatedVisibility(visible = isSectionExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (filledOrEditingFields.isEmpty()) {
                        Text(
                            text = "В этом разделе пока нет заполненных пунктов. Нажмите ниже, чтобы добавить нужную характеристику.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        filledOrEditingFields.forEach { fieldSpec ->
                            val rawValue = fieldSpec.getter(character)
                            val inlineDraft = inlineFieldDrafts[fieldSpec.key]
                            GranularEditableFieldCard(
                                fieldSpec = fieldSpec,
                                rawValue = rawValue,
                                inlineDraft = inlineDraft,
                                onStartInlineEdit = { onStartInlineFieldEdit(fieldSpec) },
                                onUpdateInlineDraft = { newVal -> onUpdateInlineFieldDraft(fieldSpec.key, newVal) },
                                onSaveInlineEdit = { onSaveInlineFieldEdit(fieldSpec) },
                                onCancelInlineEdit = { onCancelInlineFieldEdit(fieldSpec.key) },
                                onEditWholeFieldModal = { onEditSingleField(fieldSpec) },
                                onEditSubItem = { subIdx, items, delim ->
                                    onEditSubItem(fieldSpec, subIdx, items, delim)
                                },
                                onAddSubItem = { items, delim ->
                                    onAddSubItem(fieldSpec, items, delim)
                                }
                            )
                        }
                    }

                    if (emptyFields.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { showAddFieldPicker = !showAddFieldPicker },
                                modifier = Modifier.testTag("toggle_empty_fields_${section.id}")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (showAddFieldPicker) "Скрыть незаполненные поля (${emptyFields.size})"
                                    else "Добавить характеристику в раздел (+${emptyFields.size})"
                                )
                            }
                        }

                        AnimatedVisibility(visible = showAddFieldPicker) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                emptyFields.forEach { emptySpec ->
                                    AssistChip(
                                        onClick = { onStartInlineFieldEdit(emptySpec) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        label = { Text(emptySpec.label) },
                                        modifier = Modifier.testTag("add_field_${emptySpec.key}")
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

/**
 * Отображает одно поле карточки персонажа с:
 * 1) Кнопкой редактирования всего поля прямо в карточке (inline) или в отдельном окне
 * 2) Если поле содержит несколько абзацев или пунктов (через ';' или перенос строки) —
 *    позволяет отредактировать любой отдельный абзац / привычку / навык, или добавить новый пункт!
 */
@Composable
private fun GranularEditableFieldCard(
    fieldSpec: CharacterFieldSpec,
    rawValue: String,
    inlineDraft: String?,
    onStartInlineEdit: () -> Unit,
    onUpdateInlineDraft: (String) -> Unit,
    onSaveInlineEdit: () -> Unit,
    onCancelInlineEdit: () -> Unit,
    onEditWholeFieldModal: () -> Unit,
    onEditSubItem: (Int, List<String>, String) -> Unit,
    onAddSubItem: (List<String>, String) -> Unit
) {
    val activeDelimiter = remember(rawValue, fieldSpec.subItemDelimiter) {
        when {
            rawValue.contains("\n") -> "\n"
            rawValue.contains(";") -> ";"
            else -> fieldSpec.subItemDelimiter
        }
    }

    val subItems = remember(rawValue, activeDelimiter) {
        if (!fieldSpec.supportsSubItemEditing) emptyList()
        else rawValue.split(activeDelimiter).map { it.trim() }.filter { it.isNotEmpty() }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (inlineDraft != null) AmberGoldSecondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("field_card_${fieldSpec.key}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fieldSpec.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberGoldSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (fieldSpec.supportsSubItemEditing && inlineDraft == null) {
                        TextButton(
                            onClick = {
                                val currentItems = if (subItems.isNotEmpty()) subItems else listOf(rawValue.trim()).filter { it.isNotEmpty() }
                                onAddSubItem(currentItems, activeDelimiter)
                            },
                            modifier = Modifier.testTag("add_subitem_${fieldSpec.key}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(if (activeDelimiter == "\n") "Абзац" else "Пункт", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(
                        onClick = {
                            if (inlineDraft == null) onStartInlineEdit() else onEditWholeFieldModal()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_field_${fieldSpec.key}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Редактировать «${fieldSpec.label}»",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (inlineDraft != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inlineDraft,
                        onValueChange = onUpdateInlineDraft,
                        label = { Text(fieldSpec.label) },
                        minLines = if (fieldSpec.isMultiline) 3 else 1,
                        maxLines = if (fieldSpec.isMultiline) 8 else 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inline_field_input_${fieldSpec.key}")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onEditWholeFieldModal,
                            modifier = Modifier.testTag("open_modal_field_${fieldSpec.key}")
                        ) {
                            Text("В диалоге")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onCancelInlineEdit,
                                modifier = Modifier.testTag("cancel_inline_field_${fieldSpec.key}")
                            ) {
                                Text("Отмена")
                            }
                            Button(
                                onClick = onSaveInlineEdit,
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                modifier = Modifier.testTag("save_inline_field_${fieldSpec.key}")
                            ) {
                                Text("Сохранить")
                            }
                        }
                    }
                }
            } else if (subItems.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    subItems.forEachIndexed { idx, itemText ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• $itemText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onEditSubItem(idx, subItems, activeDelimiter) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("edit_subitem_${fieldSpec.key}_$idx")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Изменить пункт ${idx + 1}",
                                        tint = AmberGoldSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = rawValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Секция произвольных пользовательских характеристик (Custom Fields JSON),
 * позволяющая добавлять, редактировать и удалять любые индивидуальные поля карточки персонажа.
 */
@Composable
fun CharacterCustomFieldsSectionCard(
    character: CharacterEntity,
    onSaveCustomFieldsJson: (String, String) -> Unit
) {
    val customMap = remember(character.customFieldsJson) {
        parseCustomFieldsMap(character.customFieldsJson)
    }

    var editingKey by remember { mutableStateOf<String?>(null) }
    var editingValue by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }
    var newKeyInput by remember { mutableStateOf("") }
    var newValueInput by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("character_custom_fields_section")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "11. Пользовательские характеристики (${customMap.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary
                    )
                    Text(
                        text = "Собственные поля и параметры персонажа — редактируются по отдельности",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = {
                        newKeyInput = ""
                        newValueInput = ""
                        isAddingNew = true
                    },
                    modifier = Modifier.testTag("add_custom_character_field_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Новое поле")
                }
            }

            if (customMap.isEmpty()) {
                Text(
                    text = "Дополнительные пользовательские характеристики ещё не добавлены.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                customMap.entries.forEach { (k, v) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = k,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AmberGoldSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = v,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingKey = k
                                        editingValue = v
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Изменить $k", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val nextMap = customMap.toMutableMap().apply { remove(k) }
                                        onSaveCustomFieldsJson(
                                            encodeCustomFieldsMap(nextMap),
                                            "Удалено пользовательское поле «$k»"
                                        )
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить $k",
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingKey != null) {
        val keyToEdit = editingKey!!
        AlertDialog(
            onDismissRequest = { editingKey = null },
            title = { Text("Изменить поле «$keyToEdit»") },
            text = {
                OutlinedTextField(
                    value = editingValue,
                    onValueChange = { editingValue = it },
                    label = { Text(keyToEdit) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nextMap = customMap.toMutableMap().apply { put(keyToEdit, editingValue.trim()) }
                        onSaveCustomFieldsJson(
                            encodeCustomFieldsMap(nextMap),
                            "Обновлено пользовательское поле «$keyToEdit»"
                        )
                        editingKey = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingKey = null }) { Text("Отмена") }
            }
        )
    }

    if (isAddingNew) {
        AlertDialog(
            onDismissRequest = { isAddingNew = false },
            title = { Text("Добавить характеристику персонажа") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newKeyInput,
                        onValueChange = { newKeyInput = it },
                        label = { Text("Название характеристики (например: Архетип, Ранг)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_field_key_input")
                    )
                    OutlinedTextField(
                        value = newValueInput,
                        onValueChange = { newValueInput = it },
                        label = { Text("Значение") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_field_value_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKeyInput.isNotBlank() && newValueInput.isNotBlank()) {
                            val nextMap = customMap.toMutableMap().apply {
                                put(newKeyInput.trim(), newValueInput.trim())
                            }
                            onSaveCustomFieldsJson(
                                encodeCustomFieldsMap(nextMap),
                                "Добавлено пользовательское поле «${newKeyInput.trim()}»"
                            )
                            isAddingNew = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("confirm_add_custom_field_button")
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingNew = false }) { Text("Отмена") }
            }
        )
    }
}
