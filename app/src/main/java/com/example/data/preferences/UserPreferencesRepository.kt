package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "redworld_studio_prefs")

data class StudioPreferences(
    val customGeminiApiKey: String = "",
    val selectedTextModel: String = "gemini-3.5-flash",
    val selectedImageModel: String = "gemini-2.5-flash-image",
    val activeBottomTab: String = "HOME",
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
    val showDataTransmissionNotice: Boolean = true
) {
    /**
     * Resolves the effective Gemini API key: prioritizes user-configured key in settings,
     * falling back to BuildConfig.GEMINI_API_KEY injected by AI Studio Secrets.
     */
    val effectiveGeminiApiKey: String
        get() {
            val custom = customGeminiApiKey.trim()
            if (custom.isNotEmpty()) return custom
            val buildKey = BuildConfig.GEMINI_API_KEY.trim()
            return if (buildKey != "MY_GEMINI_API_KEY" && buildKey.isNotEmpty()) buildKey else ""
        }

    val isGeminiKeyConfigured: Boolean
        get() = effectiveGeminiApiKey.isNotEmpty()
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val CUSTOM_GEMINI_KEY = stringPreferencesKey("custom_gemini_api_key")
        val SELECTED_TEXT_MODEL = stringPreferencesKey("selected_text_model")
        val SELECTED_IMAGE_MODEL = stringPreferencesKey("selected_image_model")
        val ACTIVE_BOTTOM_TAB = stringPreferencesKey("active_bottom_tab")
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
    }

    val preferencesFlow: Flow<StudioPreferences> = context.dataStore.data.map { prefs ->
        StudioPreferences(
            customGeminiApiKey = prefs[Keys.CUSTOM_GEMINI_KEY] ?: "",
            selectedTextModel = prefs[Keys.SELECTED_TEXT_MODEL] ?: "gemini-3.5-flash",
            selectedImageModel = prefs[Keys.SELECTED_IMAGE_MODEL] ?: "gemini-2.5-flash-image",
            activeBottomTab = prefs[Keys.ACTIVE_BOTTOM_TAB] ?: "HOME",
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
            showDataTransmissionNotice = prefs[Keys.SHOW_TRANSMISSION_NOTICE] ?: true
        )
    }

    suspend fun setCustomGeminiApiKey(apiKey: String) {
        context.dataStore.edit { it[Keys.CUSTOM_GEMINI_KEY] = apiKey.trim() }
    }

    suspend fun setSelectedTextModel(modelId: String) {
        context.dataStore.edit { it[Keys.SELECTED_TEXT_MODEL] = modelId }
    }

    suspend fun setSelectedImageModel(modelId: String) {
        context.dataStore.edit { it[Keys.SELECTED_IMAGE_MODEL] = modelId }
    }

    suspend fun saveNavigationState(
        tab: String? = null,
        chatSessionId: Long? = null,
        documentId: Long? = null,
        characterId: Long? = null
    ) {
        context.dataStore.edit { prefs ->
            tab?.let { prefs[Keys.ACTIVE_BOTTOM_TAB] = it }
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
