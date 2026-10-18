package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.data.remote.AiGenerationParams
import com.example.data.remote.AiProviderType
import com.example.data.remote.SupportedAiModels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "redworld_studio_prefs")

data class StudioPreferences(
    val selectedAiProvider: String = AiProviderType.GEMINI.id,
    val customGeminiApiKey: String = "",
    val customOpenAiApiKey: String = "",
    val customXaiApiKey: String = "",
    val selectedTextModel: String = "gemini-3.5-flash",
    val selectedOpenAiModel: String = "gpt-4o",
    val selectedXaiModel: String = "grok-3",
    val customGeminiModelId: String = "",
    val customOpenAiModelId: String = "",
    val customXaiModelId: String = "",
    val aiTemperature: Float = 0.7f,
    val aiTopP: Float = 0.95f,
    val aiMaxTokens: Int = 2048,
    val selectedImageModel: String = "gemini-2.5-flash-image",
    val activeBottomTab: String = "HOME",
    val activeProjectId: Long = 1L,
    val activeChatSessionId: Long = 1L,
    val activeDocumentId: Long = 1L,
    val activeCharacterId: Long = 1L,
    val unsentPromptDraft: String = "",
    val unsentImagePromptDraft: String = "",
    val googleAccountEmail: String = "",
    val googleDriveConnected: Boolean = false,
    val googleDriveAccessToken: String = "",
    val googleDriveFolderName: String = "RedWorld_Studio_Sync",
    val lastDriveSyncTimestamp: Long = 0L,
    val showDataTransmissionNotice: Boolean = true,
    val appThemeMode: String = "REDWORLD",
    val characterSortMode: String = "NAME",
    val aiDebugModeEnabled: Boolean = false
) {
    val activeProviderType: AiProviderType
        get() = AiProviderType.fromId(selectedAiProvider)

    val generationParams: AiGenerationParams
        get() = AiGenerationParams(
            temperature = aiTemperature.coerceIn(0.0f, 2.0f),
            topP = aiTopP.coerceIn(0.05f, 1.0f),
            maxTokens = aiMaxTokens.coerceIn(128, 32768)
        )

    /**
     * Resolves the effective Gemini API key: prioritizes user-configured key in DataStore,
     * falling back to BuildConfig.GEMINI_API_KEY injected by AI Studio Secrets.
     */
    val effectiveGeminiApiKey: String
        get() {
            val custom = customGeminiApiKey.trim()
            if (custom.isNotEmpty()) return custom
            val buildKey = BuildConfig.GEMINI_API_KEY.trim()
            return if (buildKey != "MY_GEMINI_API_KEY" && buildKey.isNotEmpty()) buildKey else ""
        }

    val effectiveOpenAiApiKey: String
        get() {
            val custom = customOpenAiApiKey.trim()
            if (custom.isNotEmpty()) return custom
            val buildKey = try { BuildConfig.OPENAI_API_KEY.trim() } catch (_: Throwable) { "" }
            return if (buildKey != "MY_OPENAI_API_KEY" && buildKey.isNotEmpty()) buildKey else ""
        }

    val effectiveXaiApiKey: String
        get() {
            val custom = customXaiApiKey.trim()
            if (custom.isNotEmpty()) return custom
            val buildKey = try { BuildConfig.XAI_API_KEY.trim() } catch (_: Throwable) { "" }
            return if (buildKey != "MY_XAI_API_KEY" && buildKey.isNotEmpty()) buildKey else ""
        }

    fun effectiveApiKeyFor(provider: AiProviderType): String = when (provider) {
        AiProviderType.GEMINI -> effectiveGeminiApiKey
        AiProviderType.OPENAI -> effectiveOpenAiApiKey
        AiProviderType.XAI -> effectiveXaiApiKey
    }

    fun customApiKeyFor(provider: AiProviderType): String = when (provider) {
        AiProviderType.GEMINI -> customGeminiApiKey
        AiProviderType.OPENAI -> customOpenAiApiKey
        AiProviderType.XAI -> customXaiApiKey
    }

    fun isUsingCustomKeyFor(provider: AiProviderType): Boolean =
        customApiKeyFor(provider).trim().isNotEmpty()

    fun isKeyConfiguredFor(provider: AiProviderType): Boolean =
        effectiveApiKeyFor(provider).isNotEmpty()

    fun maskedApiKeyFor(provider: AiProviderType): String {
        val key = effectiveApiKeyFor(provider)
        if (key.isEmpty()) return "Не задан"
        if (key.length <= 8) return "••••••••"
        return "${key.take(4)}••••••••••••${key.takeLast(4)}"
    }

    fun presetModelFor(provider: AiProviderType): String = when (provider) {
        AiProviderType.GEMINI -> selectedTextModel.ifBlank { SupportedAiModels.defaultModelForProvider(AiProviderType.GEMINI) }
        AiProviderType.OPENAI -> selectedOpenAiModel.ifBlank { SupportedAiModels.defaultModelForProvider(AiProviderType.OPENAI) }
        AiProviderType.XAI -> selectedXaiModel.ifBlank { SupportedAiModels.defaultModelForProvider(AiProviderType.XAI) }
    }

    fun customModelIdFor(provider: AiProviderType): String = when (provider) {
        AiProviderType.GEMINI -> customGeminiModelId
        AiProviderType.OPENAI -> customOpenAiModelId
        AiProviderType.XAI -> customXaiModelId
    }

    fun effectiveTextModelFor(provider: AiProviderType): String {
        val customId = customModelIdFor(provider).trim()
        if (customId.isNotEmpty()) return customId
        return presetModelFor(provider)
    }

    val activeProviderEffectiveApiKey: String
        get() = effectiveApiKeyFor(activeProviderType)

    val activeProviderTextModel: String
        get() = effectiveTextModelFor(activeProviderType)

    val isActiveProviderKeyConfigured: Boolean
        get() = isKeyConfiguredFor(activeProviderType)

    val isGeminiKeyConfigured: Boolean
        get() = effectiveGeminiApiKey.isNotEmpty()

    val isUsingCustomDataStoreKey: Boolean
        get() = customGeminiApiKey.trim().isNotEmpty()

    val maskedGeminiApiKey: String
        get() = maskedApiKeyFor(AiProviderType.GEMINI)
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val SELECTED_AI_PROVIDER = stringPreferencesKey("selected_ai_provider")
        val CUSTOM_GEMINI_KEY = stringPreferencesKey("custom_gemini_api_key")
        val CUSTOM_OPENAI_KEY = stringPreferencesKey("custom_openai_api_key")
        val CUSTOM_XAI_KEY = stringPreferencesKey("custom_xai_api_key")
        val SELECTED_TEXT_MODEL = stringPreferencesKey("selected_text_model")
        val SELECTED_OPENAI_MODEL = stringPreferencesKey("selected_openai_model")
        val SELECTED_XAI_MODEL = stringPreferencesKey("selected_xai_model")
        val CUSTOM_GEMINI_MODEL_ID = stringPreferencesKey("custom_gemini_model_id")
        val CUSTOM_OPENAI_MODEL_ID = stringPreferencesKey("custom_openai_model_id")
        val CUSTOM_XAI_MODEL_ID = stringPreferencesKey("custom_xai_model_id")
        val AI_TEMPERATURE = floatPreferencesKey("ai_temperature")
        val AI_TOP_P = floatPreferencesKey("ai_top_p")
        val AI_MAX_TOKENS = intPreferencesKey("ai_max_tokens")
        val SELECTED_IMAGE_MODEL = stringPreferencesKey("selected_image_model")
        val ACTIVE_BOTTOM_TAB = stringPreferencesKey("active_bottom_tab")
        val ACTIVE_PROJECT_ID = longPreferencesKey("active_project_id")
        val ACTIVE_CHAT_SESSION_ID = longPreferencesKey("active_chat_session_id")
        val ACTIVE_DOCUMENT_ID = longPreferencesKey("active_document_id")
        val ACTIVE_CHARACTER_ID = longPreferencesKey("active_character_id")
        val UNSENT_PROMPT_DRAFT = stringPreferencesKey("unsent_prompt_draft")
        val UNSENT_IMAGE_PROMPT_DRAFT = stringPreferencesKey("unsent_image_prompt_draft")
        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        val GOOGLE_DRIVE_CONNECTED = booleanPreferencesKey("google_drive_connected")
        val GOOGLE_DRIVE_ACCESS_TOKEN = stringPreferencesKey("google_drive_access_token")
        val GOOGLE_DRIVE_FOLDER_NAME = stringPreferencesKey("google_drive_folder_name")
        val LAST_DRIVE_SYNC_TS = longPreferencesKey("last_drive_sync_ts")
        val SHOW_TRANSMISSION_NOTICE = booleanPreferencesKey("show_transmission_notice")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val CHARACTER_SORT_MODE = stringPreferencesKey("character_sort_mode")
        val AI_DEBUG_MODE_ENABLED = booleanPreferencesKey("ai_debug_mode_enabled")
    }

    val preferencesFlow: Flow<StudioPreferences> = context.dataStore.data.map { prefs ->
        StudioPreferences(
            selectedAiProvider = prefs[Keys.SELECTED_AI_PROVIDER] ?: AiProviderType.GEMINI.id,
            customGeminiApiKey = prefs[Keys.CUSTOM_GEMINI_KEY] ?: "",
            customOpenAiApiKey = prefs[Keys.CUSTOM_OPENAI_KEY] ?: "",
            customXaiApiKey = prefs[Keys.CUSTOM_XAI_KEY] ?: "",
            selectedTextModel = prefs[Keys.SELECTED_TEXT_MODEL] ?: "gemini-3.5-flash",
            selectedOpenAiModel = prefs[Keys.SELECTED_OPENAI_MODEL] ?: "gpt-4o",
            selectedXaiModel = prefs[Keys.SELECTED_XAI_MODEL] ?: "grok-3",
            customGeminiModelId = prefs[Keys.CUSTOM_GEMINI_MODEL_ID] ?: "",
            customOpenAiModelId = prefs[Keys.CUSTOM_OPENAI_MODEL_ID] ?: "",
            customXaiModelId = prefs[Keys.CUSTOM_XAI_MODEL_ID] ?: "",
            aiTemperature = prefs[Keys.AI_TEMPERATURE] ?: 0.7f,
            aiTopP = prefs[Keys.AI_TOP_P] ?: 0.95f,
            aiMaxTokens = prefs[Keys.AI_MAX_TOKENS] ?: 2048,
            selectedImageModel = prefs[Keys.SELECTED_IMAGE_MODEL] ?: "gemini-2.5-flash-image",
            activeBottomTab = prefs[Keys.ACTIVE_BOTTOM_TAB] ?: "HOME",
            activeProjectId = prefs[Keys.ACTIVE_PROJECT_ID] ?: 1L,
            activeChatSessionId = prefs[Keys.ACTIVE_CHAT_SESSION_ID] ?: 1L,
            activeDocumentId = prefs[Keys.ACTIVE_DOCUMENT_ID] ?: 1L,
            activeCharacterId = prefs[Keys.ACTIVE_CHARACTER_ID] ?: 1L,
            unsentPromptDraft = prefs[Keys.UNSENT_PROMPT_DRAFT] ?: "",
            unsentImagePromptDraft = prefs[Keys.UNSENT_IMAGE_PROMPT_DRAFT] ?: "",
            googleAccountEmail = prefs[Keys.GOOGLE_ACCOUNT_EMAIL] ?: "",
            googleDriveConnected = prefs[Keys.GOOGLE_DRIVE_CONNECTED] ?: false,
            googleDriveAccessToken = prefs[Keys.GOOGLE_DRIVE_ACCESS_TOKEN] ?: "",
            googleDriveFolderName = prefs[Keys.GOOGLE_DRIVE_FOLDER_NAME] ?: "RedWorld_Studio_Sync",
            lastDriveSyncTimestamp = prefs[Keys.LAST_DRIVE_SYNC_TS] ?: 0L,
            showDataTransmissionNotice = prefs[Keys.SHOW_TRANSMISSION_NOTICE] ?: true,
            appThemeMode = prefs[Keys.APP_THEME_MODE] ?: "REDWORLD",
            characterSortMode = prefs[Keys.CHARACTER_SORT_MODE] ?: "NAME",
            aiDebugModeEnabled = prefs[Keys.AI_DEBUG_MODE_ENABLED] ?: false
        )
    }

    suspend fun setSelectedAiProvider(provider: AiProviderType) {
        context.dataStore.edit { it[Keys.SELECTED_AI_PROVIDER] = provider.id }
    }

    suspend fun setCustomGeminiApiKey(apiKey: String) {
        context.dataStore.edit { it[Keys.CUSTOM_GEMINI_KEY] = apiKey.trim() }
    }

    suspend fun clearCustomGeminiApiKey() {
        context.dataStore.edit { it.remove(Keys.CUSTOM_GEMINI_KEY) }
    }

    suspend fun setCustomApiKeyForProvider(provider: AiProviderType, apiKey: String) {
        val trimmed = apiKey.trim()
        context.dataStore.edit { prefs ->
            when (provider) {
                AiProviderType.GEMINI -> prefs[Keys.CUSTOM_GEMINI_KEY] = trimmed
                AiProviderType.OPENAI -> prefs[Keys.CUSTOM_OPENAI_KEY] = trimmed
                AiProviderType.XAI -> prefs[Keys.CUSTOM_XAI_KEY] = trimmed
            }
        }
    }

    suspend fun clearCustomApiKeyForProvider(provider: AiProviderType) {
        context.dataStore.edit { prefs ->
            when (provider) {
                AiProviderType.GEMINI -> prefs.remove(Keys.CUSTOM_GEMINI_KEY)
                AiProviderType.OPENAI -> prefs.remove(Keys.CUSTOM_OPENAI_KEY)
                AiProviderType.XAI -> prefs.remove(Keys.CUSTOM_XAI_KEY)
            }
        }
    }

    suspend fun setSelectedTextModel(modelId: String) {
        context.dataStore.edit {
            it[Keys.SELECTED_TEXT_MODEL] = modelId
            it[Keys.CUSTOM_GEMINI_MODEL_ID] = ""
        }
    }

    suspend fun setSelectedModelForProvider(provider: AiProviderType, modelId: String) {
        val clean = modelId.trim()
        context.dataStore.edit { prefs ->
            when (provider) {
                AiProviderType.GEMINI -> {
                    prefs[Keys.SELECTED_TEXT_MODEL] = clean
                    prefs[Keys.CUSTOM_GEMINI_MODEL_ID] = ""
                }
                AiProviderType.OPENAI -> {
                    prefs[Keys.SELECTED_OPENAI_MODEL] = clean
                    prefs[Keys.CUSTOM_OPENAI_MODEL_ID] = ""
                }
                AiProviderType.XAI -> {
                    prefs[Keys.SELECTED_XAI_MODEL] = clean
                    prefs[Keys.CUSTOM_XAI_MODEL_ID] = ""
                }
            }
        }
    }

    suspend fun setCustomModelIdForProvider(provider: AiProviderType, customModelId: String) {
        val clean = customModelId.trim()
        context.dataStore.edit { prefs ->
            when (provider) {
                AiProviderType.GEMINI -> prefs[Keys.CUSTOM_GEMINI_MODEL_ID] = clean
                AiProviderType.OPENAI -> prefs[Keys.CUSTOM_OPENAI_MODEL_ID] = clean
                AiProviderType.XAI -> prefs[Keys.CUSTOM_XAI_MODEL_ID] = clean
            }
        }
    }

    suspend fun setAiGenerationParameters(temperature: Float, topP: Float, maxTokens: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AI_TEMPERATURE] = temperature.coerceIn(0.0f, 2.0f)
            prefs[Keys.AI_TOP_P] = topP.coerceIn(0.05f, 1.0f)
            prefs[Keys.AI_MAX_TOKENS] = maxTokens.coerceIn(128, 32768)
        }
    }

    suspend fun resetAiGenerationParameters() {
        context.dataStore.edit { prefs ->
            prefs[Keys.AI_TEMPERATURE] = 0.7f
            prefs[Keys.AI_TOP_P] = 0.95f
            prefs[Keys.AI_MAX_TOKENS] = 2048
        }
    }

    suspend fun setSelectedImageModel(modelId: String) {
        context.dataStore.edit { it[Keys.SELECTED_IMAGE_MODEL] = modelId }
    }

    suspend fun setShowTransmissionNotice(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TRANSMISSION_NOTICE] = enabled }
    }

    suspend fun setAppThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.APP_THEME_MODE] = mode }
    }

    suspend fun setCharacterSortMode(mode: String) {
        context.dataStore.edit { it[Keys.CHARACTER_SORT_MODE] = mode }
    }

    suspend fun setAiDebugModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AI_DEBUG_MODE_ENABLED] = enabled }
    }

    suspend fun saveNavigationState(
        tab: String? = null,
        projectId: Long? = null,
        chatSessionId: Long? = null,
        documentId: Long? = null,
        characterId: Long? = null
    ) {
        context.dataStore.edit { prefs ->
            tab?.let { prefs[Keys.ACTIVE_BOTTOM_TAB] = it }
            projectId?.let { prefs[Keys.ACTIVE_PROJECT_ID] = it }
            chatSessionId?.let { prefs[Keys.ACTIVE_CHAT_SESSION_ID] = it }
            documentId?.let { prefs[Keys.ACTIVE_DOCUMENT_ID] = it }
            characterId?.let { prefs[Keys.ACTIVE_CHARACTER_ID] = it }
        }
    }

    suspend fun saveUnsentPromptDraft(draft: String) {
        context.dataStore.edit { it[Keys.UNSENT_PROMPT_DRAFT] = draft }
    }

    suspend fun saveUnsentImagePromptDraft(draft: String) {
        context.dataStore.edit { it[Keys.UNSENT_IMAGE_PROMPT_DRAFT] = draft }
    }

    suspend fun updateGoogleConnection(
        email: String,
        driveConnected: Boolean,
        accessToken: String = "",
        folderName: String = "RedWorld_Studio_Sync"
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOOGLE_ACCOUNT_EMAIL] = email
            prefs[Keys.GOOGLE_DRIVE_CONNECTED] = driveConnected
            prefs[Keys.GOOGLE_DRIVE_ACCESS_TOKEN] = accessToken
            prefs[Keys.GOOGLE_DRIVE_FOLDER_NAME] = folderName
        }
    }

    suspend fun markDriveSynced(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[Keys.LAST_DRIVE_SYNC_TS] = timestamp }
    }

    suspend fun disconnectGoogleAccount() {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOOGLE_ACCOUNT_EMAIL] = ""
            prefs[Keys.GOOGLE_DRIVE_CONNECTED] = false
            prefs[Keys.GOOGLE_DRIVE_ACCESS_TOKEN] = ""
        }
    }

    suspend fun clearAllSettings() {
        context.dataStore.edit { it.clear() }
    }
}
