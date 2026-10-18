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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.preferences.StudioPreferences
import com.example.data.remote.AiProviderType
import com.example.data.remote.SupportedAiModels
import com.example.data.remote.SupportedGeminiModels
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.BuiltInThemePresets
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.JetBrainsMonoFont
import com.example.ui.theme.StatusAssumptionAmber
import com.example.ui.theme.StatusCanonGreen
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: StudioPreferences,
    isVerifyingKey: Boolean,
    verificationResultMessage: String?,
    onSaveGeminiApiKey: (String) -> Unit,
    onClearGeminiApiKey: () -> Unit,
    onVerifyGeminiApiKey: (String) -> Unit,
    onSelectDefaultTextModel: (String) -> Unit,
    onSelectDefaultImageModel: (String) -> Unit,
    onToggleTransmissionNotice: (Boolean) -> Unit,
    onSelectAiProvider: (AiProviderType) -> Unit = {},
    onSaveProviderApiKey: (AiProviderType, String) -> Unit = { provider, key ->
        if (provider == AiProviderType.GEMINI) onSaveGeminiApiKey(key)
    },
    onClearProviderApiKey: (AiProviderType) -> Unit = { provider ->
        if (provider == AiProviderType.GEMINI) onClearGeminiApiKey()
    },
    onVerifyProviderApiKey: (AiProviderType, String, String) -> Unit = { provider, key, _ ->
        if (provider == AiProviderType.GEMINI) onVerifyGeminiApiKey(key)
    },
    onSelectProviderPresetModel: (AiProviderType, String) -> Unit = { provider, modelId ->
        if (provider == AiProviderType.GEMINI) onSelectDefaultTextModel(modelId)
    },
    onSaveProviderCustomModelId: (AiProviderType, String) -> Unit = { _, _ -> },
    onUpdateAiGenerationParams: (Float, Float, Int) -> Unit = { _, _, _ -> },
    onResetAiGenerationParams: () -> Unit = {},
    onSelectAppThemeMode: (String) -> Unit = {},
    onToggleAiDebugMode: (Boolean) -> Unit = {},
    onExportActiveProjectJson: ((String) -> Unit) -> Unit = {},
    onImportProjectJson: (String, Boolean) -> Unit = { _, _ -> },
    onExportFullBackupUri: (Uri) -> Unit = {},
    onImportFullBackupUri: (Uri) -> Unit = {},
    onOpenConnectionsAndDrive: () -> Unit,
    onOpenVersionsAndBackups: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeSectionFilter by rememberSaveable { mutableIntStateOf(0) }

    val activeProvider = prefs.activeProviderType
    val currentProviderCustomKey = prefs.customApiKeyFor(activeProvider)
    val currentProviderCustomModelId = prefs.customModelIdFor(activeProvider)
    val currentEffectiveModelId = prefs.effectiveTextModelFor(activeProvider)
    val activeModelSpec = remember(activeProvider, currentEffectiveModelId) {
        SupportedAiModels.findModelSpec(activeProvider, currentEffectiveModelId)
    }

    var providerDropdownExpanded by remember { mutableStateOf(false) }

    var apiKeyInput by rememberSaveable(activeProvider.id, currentProviderCustomKey) {
        mutableStateOf(currentProviderCustomKey)
    }
    var customModelIdInput by rememberSaveable(activeProvider.id, currentProviderCustomModelId) {
        mutableStateOf(currentProviderCustomModelId)
    }
    var isKeyVisible by rememberSaveable { mutableStateOf(false) }
    var validationMessage by remember(activeProvider.id) { mutableStateOf<String?>(null) }

    var tempSliderValue by remember(prefs.aiTemperature) { mutableFloatStateOf(prefs.aiTemperature) }
    var topPSliderValue by remember(prefs.aiTopP) { mutableFloatStateOf(prefs.aiTopP) }

    var exportedJsonPreview by remember { mutableStateOf<String?>(null) }
    var showImportProjectDialog by remember { mutableStateOf(false) }
    var importJsonDraft by remember { mutableStateOf("") }
    var overwriteExistingOnImport by remember { mutableStateOf(false) }
    var showOverwriteConfirmDialog by remember { mutableStateOf(false) }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportFullBackupUri(uri)
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportFullBackupUri(uri)
    }

    val sections = listOf(
        "Все (1–8)",
        "1. Внешний вид",
        "2. ИИ и модели",
        "3. Подключения",
        "4. Хранилище и файлы",
        "5. Резервное копирование",
        "6. Импорт и экспорт",
        "7. Диагностика",
        "8. О приложении"
    )

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = activeSectionFilter,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp
        ) {
            sections.forEachIndexed { index, title ->
                Tab(
                    selected = activeSectionFilter == index,
                    onClick = { activeSectionFilter = index },
                    text = { Text(title) },
                    modifier = Modifier.testTag("settings_subtab_$index")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Общие настройки приложения отделены от настроек отдельных творческих проектов.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. ВНЕШНИЙ ВИД (Темы оформления)
            if (activeSectionFilter == 0 || activeSectionFilter == 1) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_appearance_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = CrimsonPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "1. Внешний вид и темы оформления",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Выбранная тема применяется ко всем экранам: боковому меню, списку персонажей, карточкам, локациям, документам и симуляции.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            AppThemeMode.entries.forEach { mode ->
                                val isSelected = prefs.appThemeMode.equals(mode.code, ignoreCase = true)
                                Surface(
                                    onClick = { onSelectAppThemeMode(mode.code) },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, if (isSelected) CrimsonPrimary else MaterialTheme.colorScheme.outline),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("theme_mode_${mode.code.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(mode.titleRu, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(mode.descriptionRu, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Выбрано", tint = StatusCanonGreen)
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "Доступные цветовые пресеты (${BuiltInThemePresets.size}): ${BuiltInThemePresets.joinToString { it.name }} (архитектура готова к пользовательским пресетам).",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberGoldSecondary
                            )
                        }
                    }
                }
            }

            // 2. ИИ И МОДЕЛИ (Мульти-провайдерная система: Google Gemini, OpenAI GPT, xAI Grok)
            if (activeSectionFilter == 0 || activeSectionFilter == 2) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_gemini_key_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "ИИ-провайдеры и API",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "2. ИИ-провайдеры и модели",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Активный провайдер: ${activeProvider.displayName} • Модель: $currentEffectiveModelId",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (prefs.isKeyConfiguredFor(activeProvider)) StatusCanonGreen else StatusAssumptionAmber
                                    )
                                }
                            }

                            // А. Основной провайдер ИИ (Выпадающий список + быстрые переключатели)
                            Text(
                                text = "А. Основной провайдер ИИ:",
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberGoldSecondary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { providerDropdownExpanded = true }
                                        .testTag("settings_ai_provider_dropdown")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activeProvider.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = activeProvider.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Выбрать провайдера ИИ",
                                            tint = AmberGoldSecondary
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = providerDropdownExpanded,
                                    onDismissRequest = { providerDropdownExpanded = false }
                                ) {
                                    AiProviderType.entries.forEach { provider ->
                                        val isConfigured = prefs.isKeyConfiguredFor(provider)
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = provider.displayName,
                                                        fontWeight = if (provider == activeProvider) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    Text(
                                                        text = if (isConfigured) "Ключ настроен • Модель: ${prefs.effectiveTextModelFor(provider)}"
                                                        else "Ключ не задан • Модель: ${prefs.effectiveTextModelFor(provider)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isConfigured) StatusCanonGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                providerDropdownExpanded = false
                                                onSelectAiProvider(provider)
                                            },
                                            modifier = Modifier.testTag("provider_dropdown_item_${provider.id.lowercase()}")
                                        )
                                    }
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AiProviderType.entries.forEach { provider ->
                                    val selected = provider == activeProvider
                                    val configured = prefs.isKeyConfiguredFor(provider)
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectAiProvider(provider) },
                                        label = {
                                            Text("${provider.displayName}${if (configured) " ✓" else ""}")
                                        },
                                        modifier = Modifier.testTag("provider_chip_${provider.id.lowercase()}")
                                    )
                                }
                            }

                            HorizontalDivider()

                            // Б. Управление API-ключом выбранного провайдера
                            Text(
                                text = "Б. API-ключ для ${activeProvider.displayName}:",
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberGoldSecondary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = when {
                                    prefs.isUsingCustomKeyFor(activeProvider) ->
                                        "Источник: Пользовательский ключ в защищённом хранилище DataStore"
                                    prefs.isKeyConfiguredFor(activeProvider) ->
                                        "Источник: BuildConfig (Secrets AI Studio)"
                                    else ->
                                        "Статус: Ключ для ${activeProvider.displayName} ещё не задан"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (prefs.isKeyConfiguredFor(activeProvider)) StatusCanonGreen else StatusAssumptionAmber
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (prefs.isKeyConfiguredFor(activeProvider)) Icons.Default.CheckCircle else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (prefs.isKeyConfiguredFor(activeProvider)) StatusCanonGreen else AmberGoldSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Текущий ключ (${activeProvider.shortName}):", style = MaterialTheme.typography.labelLarge)
                                    }
                                    Text(
                                        text = prefs.maskedApiKeyFor(activeProvider),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = JetBrainsMonoFont,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.testTag("settings_masked_key_text")
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = {
                                    apiKeyInput = it
                                    validationMessage = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_api_key_input"),
                                label = { Text("${activeProvider.displayName} API Key") },
                                placeholder = { Text(activeProvider.keyPlaceholder) },
                                singleLine = true,
                                visualTransformation = if (isKeyVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    autoCorrectEnabled = false,
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isKeyVisible = !isKeyVisible },
                                        modifier = Modifier.testTag("settings_toggle_key_visibility_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isKeyVisible) "Скрыть ключ" else "Показать ключ",
                                            tint = AmberGoldSecondary
                                        )
                                    }
                                }
                            )

                            if (!validationMessage.isNullOrBlank()) {
                                Text(validationMessage!!, style = MaterialTheme.typography.labelMedium, color = AmberGoldSecondary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val trimmed = apiKeyInput.trim()
                                        if (trimmed.isEmpty()) {
                                            validationMessage = "Пожалуйста, введите непустой ключ API для ${activeProvider.displayName}."
                                        } else {
                                            onSaveProviderApiKey(activeProvider, trimmed)
                                            validationMessage = "Ключ ${activeProvider.displayName} сохранён в защищённом хранилище DataStore."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_save_api_key_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Сохранить ключ")
                                }

                                OutlinedButton(
                                    onClick = {
                                        val candidate = apiKeyInput.trim().ifEmpty { prefs.effectiveApiKeyFor(activeProvider) }
                                        val modelToTest = customModelIdInput.trim().ifEmpty { currentEffectiveModelId }
                                        onVerifyProviderApiKey(activeProvider, candidate, modelToTest)
                                    },
                                    enabled = !isVerifyingKey && (apiKeyInput.isNotBlank() || prefs.isKeyConfiguredFor(activeProvider)),
                                    modifier = Modifier.testTag("settings_verify_api_key_button")
                                ) {
                                    if (isVerifyingKey) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(15.dp),
                                            strokeWidth = 2.dp,
                                            color = CrimsonPrimary
                                        )
                                    } else {
                                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Проверить")
                                }
                            }

                            if (prefs.isUsingCustomKeyFor(activeProvider) || apiKeyInput.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        apiKeyInput = ""
                                        validationMessage = "Пользовательский ключ ${activeProvider.displayName} удалён."
                                        onClearProviderApiKey(activeProvider)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_clear_api_key_button")
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Удалить пользовательский ключ (${activeProvider.shortName})")
                                }
                            }

                            if (!verificationResultMessage.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, AmberGoldSecondary.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = verificationResultMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            HorizontalDivider()

                            // В. Выбор модели из списка и ручной ввод ID модели
                            Text(
                                text = "В. Доступные модели ${activeProvider.displayName}:",
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberGoldSecondary,
                                fontWeight = FontWeight.SemiBold
                            )

                            val providerModels = SupportedAiModels.modelsForProvider(activeProvider)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                providerModels.forEach { spec ->
                                    val isSelected = currentProviderCustomModelId.isBlank() &&
                                        prefs.presetModelFor(activeProvider).equals(spec.modelId, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            customModelIdInput = ""
                                            onSelectProviderPresetModel(activeProvider, spec.modelId)
                                        },
                                        label = { Text(spec.displayName) },
                                        modifier = Modifier.testTag("model_chip_${spec.modelId}")
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${activeModelSpec.displayName} (${activeModelSpec.modelId})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = activeModelSpec.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (activeModelSpec.recommendedFor.isNotBlank()) {
                                        Text(
                                            text = "Рекомендуется для: ${activeModelSpec.recommendedFor}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberGoldSecondary
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = customModelIdInput,
                                onValueChange = { customModelIdInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_custom_model_id_input"),
                                label = { Text("Вручную указать ID модели (${activeProvider.shortName})") },
                                placeholder = {
                                    Text(
                                        when (activeProvider) {
                                            AiProviderType.GEMINI -> "Например: gemini-3.5-flash"
                                            AiProviderType.OPENAI -> "Например: gpt-4o-2024-11-20 или gpt-4.1"
                                            AiProviderType.XAI -> "Например: grok-3-fast или grok-3"
                                        }
                                    )
                                },
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onSaveProviderCustomModelId(activeProvider, customModelIdInput.trim())
                                    },
                                    enabled = customModelIdInput.trim().isNotEmpty(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_apply_custom_model_button")
                                ) {
                                    Text("Применить свой ID модели")
                                }
                                if (currentProviderCustomModelId.isNotBlank() || customModelIdInput.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            customModelIdInput = ""
                                            onSaveProviderCustomModelId(activeProvider, "")
                                        },
                                        modifier = Modifier.testTag("settings_clear_custom_model_button")
                                    ) {
                                        Text("Сбросить к списку")
                                    }
                                }
                            }

                            HorizontalDivider()

                            // Г. Настройка параметров генерации
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = AmberGoldSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Г. Параметры генерации (${activeModelSpec.modelId}):",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AmberGoldSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        tempSliderValue = 0.7f
                                        topPSliderValue = 0.95f
                                        onResetAiGenerationParams()
                                    },
                                    modifier = Modifier.testTag("settings_reset_gen_params_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Сбросить", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            if (!activeModelSpec.supportsTemperature) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, AmberGoldSecondary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Модель рассуждения «${activeModelSpec.modelId}» управляет параметрами Temperature и Top P автоматически на стороне сервера провайдера.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Температура (креативность):", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = String.format(Locale.US, "%.2f", tempSliderValue),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = JetBrainsMonoFont,
                                            color = AmberGoldSecondary
                                        )
                                    }
                                    Slider(
                                        value = tempSliderValue,
                                        onValueChange = { tempSliderValue = it },
                                        onValueChangeFinished = {
                                            onUpdateAiGenerationParams(tempSliderValue, topPSliderValue, prefs.aiMaxTokens)
                                        },
                                        valueRange = 0.0f..1.5f,
                                        steps = 14,
                                        modifier = Modifier.testTag("settings_temperature_slider")
                                    )
                                }

                                if (activeModelSpec.supportsTopP) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Top P (разнообразие выборки):", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                text = String.format(Locale.US, "%.2f", topPSliderValue),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontFamily = JetBrainsMonoFont,
                                                color = AmberGoldSecondary
                                            )
                                        }
                                        Slider(
                                            value = topPSliderValue,
                                            onValueChange = { topPSliderValue = it },
                                            onValueChangeFinished = {
                                                onUpdateAiGenerationParams(tempSliderValue, topPSliderValue, prefs.aiMaxTokens)
                                            },
                                            valueRange = 0.1f..1.0f,
                                            steps = 17,
                                            modifier = Modifier.testTag("settings_top_p_slider")
                                        )
                                    }
                                }
                            }

                            if (activeModelSpec.supportsMaxTokens) {
                                Text(
                                    text = "Максимальная длина ответа (Max Tokens): ${prefs.aiMaxTokens}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(512, 1024, 2048, 4096, 8192).forEach { tokenLimit ->
                                        FilterChip(
                                            selected = prefs.aiMaxTokens == tokenLimit,
                                            onClick = {
                                                onUpdateAiGenerationParams(tempSliderValue, topPSliderValue, tokenLimit)
                                            },
                                            label = { Text("$tokenLimit") },
                                            modifier = Modifier.testTag("max_tokens_chip_$tokenLimit")
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

                            // Д. Модели генерации изображений
                            Text("Д. Модели генерации изображений (Арт-Студия):", style = MaterialTheme.typography.labelLarge, color = AmberGoldSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SupportedGeminiModels.imageModels.forEach { spec ->
                                    FilterChip(
                                        selected = prefs.selectedImageModel == spec.modelId,
                                        onClick = { onSelectDefaultImageModel(spec.modelId) },
                                        label = { Text(spec.displayName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. ПОДКЛЮЧЕНИЯ
            if (activeSectionFilter == 0 || activeSectionFilter == 3) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = AmberGoldSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("3. Подключения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Статус облачной синхронизации Google Drive: ${if (prefs.googleDriveConnected) "Подключено (${prefs.googleAccountEmail})" else "Локальный автономный режим"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(onClick = onOpenConnectionsAndDrive) {
                                Text("Настроить облачное подключение Google Drive")
                            }
                        }
                    }
                }
            }

            // 4. ХРАНИЛИЩЕ И ФАЙЛЫ
            if (activeSectionFilter == 0 || activeSectionFilter == 4) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = CrimsonPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("4. Хранилище и файлы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("• Локальная база данных: Room SQLite (схема версии v5 с поддержкой миграции)", style = MaterialTheme.typography.bodySmall)
                            Text("• Изоляция данных: каждый проект хранит собственные карточки, сюжетные линии, историю событий и документы отдельно.", style = MaterialTheme.typography.bodySmall)
                            Text("• Поддерживаемые файлы импорта: .txt, .md, .json, .csv, .pdf, .docx, .xlsx, изображения (.png, .jpg, .webp).", style = MaterialTheme.typography.bodySmall)
                            Text("• Путь хранилища приложения: ${context.filesDir.absolutePath}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 5. РЕЗЕРВНОЕ КОПИРОВАНИЕ
            if (activeSectionFilter == 0 || activeSectionFilter == 5) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Backup, contentDescription = null, tint = StatusCanonGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("5. Резервное копирование и восстановление", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Автоматические снимки версий создаются при каждом изменении карточек персонажей и таблиц XLSX. Вы также можете выгрузить полную резервную копию базы данных в файл.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { backupExportLauncher.launch("WorldStudio_FullBackup.json") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                                ) {
                                    Text("Создать бэкап (SAF)")
                                }
                                OutlinedButton(
                                    onClick = { backupImportLauncher.launch(arrayOf("application/json", "*/*")) }
                                ) {
                                    Text("Восстановить из бэкапа")
                                }
                                OutlinedButton(onClick = onOpenVersionsAndBackups) {
                                    Text("Журнал версий")
                                }
                            }
                        }
                    }
                }
            }

            // 6. ИМПОРТ И ЭКСПОРТ ПРОЕКТА
            if (activeSectionFilter == 0 || activeSectionFilter == 6) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ImportExport, contentDescription = null, tint = AmberGoldSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("6. Импорт и экспорт проектов", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Экспортируемый пакет проекта содержит карточки персонажей, локации, правила мира, сюжетные линии, историю событий, связи и изображения. Секретные API-ключи НИКОГДА не включаются в экспорт.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onExportActiveProjectJson { json ->
                                            exportedJsonPreview = json
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    modifier = Modifier.testTag("settings_export_project_btn")
                                ) {
                                    Text("Экспорт текущего проекта (JSON)")
                                }
                                OutlinedButton(
                                    onClick = { showImportProjectDialog = true },
                                    modifier = Modifier.testTag("settings_import_project_btn")
                                ) {
                                    Text("Импорт проекта (с проверкой целостности)")
                                }
                            }
                        }
                    }
                }
            }

            // 7. ДИАГНОСТИКА И ОТЛАДКА
            if (activeSectionFilter == 0 || activeSectionFilter == 7) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = CrimsonPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("7. Диагностика и прозрачность ИИ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Режим объяснения и отладки ИИ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Показывать, какие карточки, правила мира, события и связи повлияли на ответ ИИ и какие ограничения были применены.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = prefs.aiDebugModeEnabled,
                                    onCheckedChange = onToggleAiDebugMode,
                                    modifier = Modifier.testTag("settings_ai_debug_switch")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Уведомление о передаваемых источниках контекста", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Отображать список фрагментов документов перед отправкой запроса внешнему провайдеру ИИ.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = prefs.showDataTransmissionNotice,
                                    onCheckedChange = onToggleTransmissionNotice,
                                    modifier = Modifier.testTag("settings_transmission_notice_switch")
                                )
                            }
                        }
                    }
                }
            }

            // 8. О ПРИЛОЖЕНИИ
            if (activeSectionFilter == 0 || activeSectionFilter == 8) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = AmberGoldSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("8. О приложении", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("World AI Studio • Версия 2.6 (Мульти-провайдерное ядро ИИ • Schema v5)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Многопроектная творческая среда с поддержкой Google Gemini, OpenAI GPT и xAI Grok для создания вымышленных миров, карточек персонажей, сюжетных линий, истории событий, документов, симуляции случайных сцен и 10-критериального тестирования отыгрыша ИИ.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Exported JSON Preview Dialog
    if (exportedJsonPreview != null) {
        AlertDialog(
            onDismissRequest = { exportedJsonPreview = null },
            title = { Text("Пакет экспорта проекта (без API-ключей)") },
            text = {
                OutlinedTextField(
                    value = exportedJsonPreview!!,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { exportedJsonPreview = null }) { Text("Закрыть") }
            }
        )
    }

    // Import Project JSON Dialog with Overwrite Warning
    if (showImportProjectDialog) {
        AlertDialog(
            onDismissRequest = { showImportProjectDialog = false },
            title = { Text("Импорт пакета проекта (JSON)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "При ошибке импорта существующие данные проекта НЕ удаляются. Перед импортом выполняется проверка целостности структуры.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = importJsonDraft,
                        onValueChange = { importJsonDraft = it },
                        label = { Text("Вставьте JSON пакета проекта") },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = overwriteExistingOnImport,
                            onCheckedChange = { overwriteExistingOnImport = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Перезаписать текущий активный проект", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (overwriteExistingOnImport) {
                            showOverwriteConfirmDialog = true
                        } else {
                            onImportProjectJson(importJsonDraft, false)
                            showImportProjectDialog = false
                        }
                    }
                ) { Text("Импортировать") }
            },
            dismissButton = {
                TextButton(onClick = { showImportProjectDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Overwrite Warning Dialog
    if (showOverwriteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showOverwriteConfirmDialog = false },
            title = { Text("Предупреждение о перезаписи данных") },
            text = {
                Text("Вы выбрали перезапись текущего проекта. Существующие карточки текущего проекта будут заменены данными из импортируемого пакета (только после успешной проверки целостности JSON). Продолжить?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportProjectJson(importJsonDraft, true)
                        showOverwriteConfirmDialog = false
                        showImportProjectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) { Text("Подтвердить перезапись") }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteConfirmDialog = false }) { Text("Отмена") }
            }
        )
    }
}
