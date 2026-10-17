package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.CharacterEntity
import com.example.data.local.GeneratedImageEntity
import com.example.data.local.LocationEntity
import com.example.data.preferences.StudioPreferences
import com.example.data.remote.SupportedGeminiModels
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.BurgundyCardBorder
import com.example.ui.theme.BurgundySurfaceContainer
import com.example.ui.theme.BurgundySurfaceElevated
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.TextIvoryPrimary
import com.example.ui.theme.TextMutedRose
import java.io.ByteArrayOutputStream

private val PROMPT_TEMPLATES = listOf(
    "Портрет" to "Detailed character portrait, dramatic crimson and warm amber lighting, expressive gaze, 8k concept art",
    "Полный рост" to "Full-body character design standing in an atmospheric setting, clear silhouette and outfit details",
    "Внешность персонажа" to "Character reference sheet showing facial structure, hair texture, eye color and distinguishing marks",
    "Эмоции" to "Expressive character study showing subtle tension and analytical focus, cinematic close-up",
    "Позы" to "Dynamic action and investigation pose, dramatic camera angle, gothic noir composition",
    "Одежда" to "Detailed wardrobe and attire concept art, tailored burgundy fabrics, leather textures and accessories",
    "Интерьер" to "Atmospheric gothic interior of the Red Mansion library, stained glass, warm desk lamps, bookshelves",
    "Экстерьер" to "Rainy evening exterior on Green Street leading to the Red Mansion gates, neon reflections on cobblestones",
    "Концепт-арт" to "Cinematic environment and mood concept art in the signature RedWorld dark crimson aesthetic",
    "Предметы" to "Detailed prop illustration of a rare artifact from the Green Street Antique Shop, dark velvet background",
    "Иллюстрации игровых сцен" to "Story scene illustration with characters interacting inside the Crimson Velvet Bar, moody lighting"
)

private val ASPECT_RATIOS = listOf("1:1", "3:4", "4:3", "16:9", "9:16")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageStudioScreen(
    prefs: StudioPreferences,
    characters: List<CharacterEntity>,
    locations: List<LocationEntity>,
    generatedImages: List<GeneratedImageEntity>,
    promptDraft: String,
    isGenerating: Boolean,
    onPromptDraftChange: (String) -> Unit,
    onSelectImageModel: (String) -> Unit,
    onApplyVisualPassport: (CharacterEntity, String) -> Unit,
    onGenerateImage: (String, String, String, String, CharacterEntity?, LocationEntity?, String?) -> Unit,
    onDeleteImage: (GeneratedImageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTemplate by remember { mutableStateOf(PROMPT_TEMPLATES.first().first) }
    var selectedAspectRatio by remember { mutableStateOf("1:1") }
    var selectedCharacter by remember { mutableStateOf<CharacterEntity?>(characters.firstOrNull()) }
    var selectedLocation by remember { mutableStateOf<LocationEntity?>(locations.firstOrNull()) }
    var referenceBase64 by remember { mutableStateOf<String?>(null) }
    var referenceName by remember { mutableStateOf<String?>(null) }

    val refPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        val out = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        referenceBase64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                        referenceName = uri.lastPathSegment ?: "reference.jpg"
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("image_studio_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Model Selection & Capability Notice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Image Studio — Генерация и Паспорта внешности RedWorld",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextIvoryPrimary
                    )
                    Text(
                        text = "Доступность генерации изображений зависит от тарифа вашего Gemini API Key и выбранной модели. Перед отправкой вы всегда можете просмотреть и изменить сформированный промпт.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SupportedGeminiModels.imageModels.forEach { spec ->
                            FilterChip(
                                selected = prefs.selectedImageModel == spec.modelId,
                                onClick = { onSelectImageModel(spec.modelId) },
                                label = { Text(spec.displayName, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }
        }

        // 2. Character Visual Passport & Location Linking
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceElevated),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Утверждённый паспорт внешности персонажа и привязка к локации",
                        style = MaterialTheme.typography.titleSmall,
                        color = AmberGoldSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Персонаж:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMutedRose
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedCharacter == null,
                            onClick = { selectedCharacter = null },
                            label = { Text("Без привязки") }
                        )
                        characters.forEach { ch ->
                            FilterChip(
                                selected = selectedCharacter?.id == ch.id,
                                onClick = { selectedCharacter = ch },
                                label = { Text(ch.name) },
                                leadingIcon = {
                                    Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            )
                        }
                    }

                    if (selectedCharacter != null) {
                        OutlinedButton(
                            onClick = {
                                val tpl = PROMPT_TEMPLATES.firstOrNull { it.first == selectedTemplate }?.second.orEmpty()
                                onApplyVisualPassport(selectedCharacter!!, tpl)
                            },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .testTag("apply_visual_passport_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Подготовить промпт по паспорту внешности «${selectedCharacter!!.name}»")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Локация:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMutedRose
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedLocation == null,
                            onClick = { selectedLocation = null },
                            label = { Text("Любая") }
                        )
                        locations.forEach { loc ->
                            FilterChip(
                                selected = selectedLocation?.id == loc.id,
                                onClick = { selectedLocation = loc },
                                label = { Text(loc.name) },
                                leadingIcon = {
                                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Prompt Templates (All 11 required categories) & Aspect Ratios
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Шаблоны промптов RedWorld:", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        PROMPT_TEMPLATES.forEach { (categoryName, templatePrompt) ->
                            FilterChip(
                                selected = selectedTemplate == categoryName,
                                onClick = {
                                    selectedTemplate = categoryName
                                    val locPart = selectedLocation?.let { ", location: ${it.name} (${it.atmosphere})" }.orEmpty()
                                    val charPart = selectedCharacter?.visualPassportPrompt?.takeIf { it.isNotBlank() }
                                        ?.let { " $it" }.orEmpty()
                                    onPromptDraftChange("$templatePrompt$charPart$locPart")
                                },
                                label = { Text(categoryName, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }

                    Text("Соотношение сторон:", style = MaterialTheme.typography.labelMedium, color = TextMutedRose)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ASPECT_RATIOS.forEach { ratio ->
                            FilterChip(
                                selected = selectedAspectRatio == ratio,
                                onClick = { selectedAspectRatio = ratio },
                                label = { Text(ratio) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = promptDraft,
                        onValueChange = onPromptDraftChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_studio_prompt_input"),
                        label = { Text("Промпт для генерации (доступен для проверки и редактирования перед отправкой)") },
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                refPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (referenceName != null) "Референс: $referenceName" else "Загрузить референс")
                        }

                        Button(
                            onClick = {
                                onGenerateImage(
                                    promptDraft,
                                    selectedTemplate,
                                    prefs.selectedImageModel,
                                    selectedAspectRatio,
                                    selectedCharacter,
                                    selectedLocation,
                                    referenceBase64
                                )
                            },
                            enabled = promptDraft.isNotBlank() && !isGenerating,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.testTag("generate_image_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = TextIvoryPrimary
                                )
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Сгенерировать")
                        }
                    }
                }
            }
        }

        // 4. Gallery & History of Generations
        item {
            Text(
                text = "Галерея и история генераций (${generatedImages.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TextIvoryPrimary
            )
        }

        items(generatedImages, key = { it.id }) { imgRecord ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generated_image_card_${imgRecord.id}"),
                colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${imgRecord.templateCategory} • Формат ${imgRecord.aspectRatio} • ${imgRecord.modelId}",
                                style = MaterialTheme.typography.titleSmall,
                                color = AmberGoldSecondary
                            )
                            val links = listOfNotNull(
                                imgRecord.linkedCharacterName.takeIf { it.isNotBlank() }?.let { "Персонаж: $it" },
                                imgRecord.linkedLocationName.takeIf { it.isNotBlank() }?.let { "Локация: $it" }
                            ).joinToString(" | ")
                            if (links.isNotEmpty()) {
                                Text(links, style = MaterialTheme.typography.labelMedium, color = CrimsonPrimary)
                            }
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    onPromptDraftChange(imgRecord.prompt)
                                    onGenerateImage(
                                        imgRecord.prompt,
                                        imgRecord.templateCategory,
                                        imgRecord.modelId,
                                        imgRecord.aspectRatio,
                                        selectedCharacter,
                                        selectedLocation,
                                        null
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Повторить генерацию", tint = AmberGoldSecondary)
                            }
                            IconButton(onClick = { onDeleteImage(imgRecord) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = TextMutedRose)
                            }
                        }
                    }

                    if (imgRecord.imageBase64OrPath.isNotBlank()) {
                        val decodedBitmap = remember(imgRecord.imageBase64OrPath) {
                            try {
                                val bytes = Base64.decode(imgRecord.imageBase64OrPath, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (decodedBitmap != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Image(
                                bitmap = decodedBitmap,
                                contentDescription = imgRecord.prompt,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = BurgundySurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Промпт: ${imgRecord.prompt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextIvoryPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            AssistChip(
                                onClick = { onPromptDraftChange(imgRecord.prompt) },
                                label = { Text("В редактор") }
                            )
                        }
                    }
                }
            }
        }
    }
}
