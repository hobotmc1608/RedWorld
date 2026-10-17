package com.example.data.remote

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

@Serializable
data class DriveRemoteFileMetadata(
    val id: String,
    val name: String,
    val mimeType: String = "application/json",
    val modifiedTime: String = "",
    val version: Long = 1L,
    val sizeBytes: Long = 0L
)

@Serializable
data class SyncConflictInfo(
    val fileName: String,
    val localUpdatedAt: Long,
    val remoteModifiedTime: String,
    val localSummary: String,
    val remoteSummary: String,
    val remotePayloadJson: String
)

sealed class DriveOperationResult {
    data class Success(val message: String, val files: List<DriveRemoteFileMetadata> = emptyList()) : DriveOperationResult()
    data class ConflictDetected(val conflict: SyncConflictInfo) : DriveOperationResult()
    data class Error(val message: String) : DriveOperationResult()
}

/**
 * Official Google Drive REST API v3 + Android Storage Access Framework (SAF) synchronization
 * and backup manager. Never requests Google passwords; uses OAuth2 Bearer access token
 * for direct REST calls and Android DocumentProvider (which natively mounts Google Drive).
 */
class GoogleDriveSyncManager(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Lists files in the user's Google Drive using official Drive API v3 (`GET /drive/v3/files`).
     */
    suspend fun listDriveFiles(accessToken: String): DriveOperationResult = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext DriveOperationResult.Error(
                "Токен OAuth2 для Google Drive не указан. Авторизуйтесь через Google или используйте системный выбор файлов Google Drive (SAF)."
            )
        }
        try {
            val url = "https://www.googleapis.com/drive/v3/files?pageSize=25&fields=files(id,name,mimeType,modifiedTime,version,size)&q=trashed=false"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${accessToken.trim()}")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext DriveOperationResult.Error(
                        "Ошибка Google Drive API (HTTP ${response.code}): проверьте срок действия OAuth-токена и область видимости drive.file."
                    )
                }
                // Parse file list
                val names = Regex(""""name"\s*:\s*"([^"]+)"""").findAll(bodyStr).map { it.groupValues[1] }.toList()
                val ids = Regex(""""id"\s*:\s*"([^"]+)"""").findAll(bodyStr).map { it.groupValues[1] }.toList()
                val items = names.mapIndexed { idx, name ->
                    DriveRemoteFileMetadata(
                        id = ids.getOrElse(idx) { "drive_$idx" },
                        name = name
                    )
                }
                DriveOperationResult.Success("Получено файлов из Google Drive: ${items.size}", items)
            }
        } catch (e: Exception) {
            DriveOperationResult.Error("Ошибка соединения с Google Drive API: ${e.localizedMessage}")
        }
    }

    /**
     * Uploads a JSON/XLSX backup snapshot to Google Drive via `POST /upload/drive/v3/files?uploadType=multipart`.
     */
    suspend fun uploadBackupToDriveApi(
        accessToken: String,
        fileName: String,
        contentJson: String
    ): DriveOperationResult = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext DriveOperationResult.Error(
                "Для прямой загрузки через REST API требуется OAuth2-токен Google Drive. Вы также можете экспортировать файл в Google Drive через системное меню Android (SAF)."
            )
        }
        try {
            val boundary = "RedWorldBoundary${System.currentTimeMillis()}"
            val metadataPart = """{"name": "$fileName", "mimeType": "application/json"}"""
            val multipartBody = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadataPart)
                append("\r\n--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(contentJson)
                append("\r\n--$boundary--")
            }

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer ${accessToken.trim()}")
                .post(multipartBody.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    DriveOperationResult.Success("Файл «$fileName» успешно синхронизирован с Google Drive.")
                } else {
                    DriveOperationResult.Error("Google Drive API вернул код ${response.code}. Проверьте права доступа OAuth.")
                }
            }
        } catch (e: Exception) {
            DriveOperationResult.Error("Сетевая ошибка при выгрузке в Google Drive: ${e.localizedMessage}")
        }
    }

    /**
     * Writes a full project backup file to a local or Google Drive DocumentProvider URI
     * selected via Android Storage Access Framework (SAF).
     */
    suspend fun exportBackupToSafUri(uri: Uri, payloadJson: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(payloadJson.toByteArray(StandardCharsets.UTF_8))
                    out.flush()
                } ?: return@withContext Result.failure(IllegalStateException("Не удалось открыть поток записи для выбранного URI"))
                Result.success("Резервная копия проекта успешно записана в выбранный файл (Google Drive / SAF).")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Exports a local file (e.g., edited XLSX workbook) to a target SAF/Google Drive URI.
     */
    suspend fun exportLocalFileToSafUri(sourceFile: File, targetUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                    out.flush()
                } ?: return@withContext Result.failure(IllegalStateException("Не удалось открыть целевой файл для записи"))
                Result.success("Таблица XLSX «${sourceFile.name}» успешно экспортирована со всеми листами, формулами и объединёнными ячейками.")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Checks an incoming backup file for version conflicts before importing, ensuring
     * a newer local version is never silently overwritten by an older remote/imported copy.
     */
    suspend fun inspectBackupFromSafUri(
        uri: Uri,
        currentLocalTimestamp: Long,
        currentLocalSummary: String
    ): DriveOperationResult = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use {
                String(it.readBytes(), StandardCharsets.UTF_8)
            } ?: return@withContext DriveOperationResult.Error("Не удалось прочитать выбранный файл")

            val exportedAtMatch = Regex(""""exportedAt"\s*:\s*(\d+)""").find(content)
            val remoteTs = exportedAtMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L

            if (remoteTs in 1 until currentLocalTimestamp) {
                return@withContext DriveOperationResult.ConflictDetected(
                    SyncConflictInfo(
                        fileName = uri.lastPathSegment ?: "redworld_backup.json",
                        localUpdatedAt = currentLocalTimestamp,
                        remoteModifiedTime = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(remoteTs)),
                        localSummary = currentLocalSummary,
                        remoteSummary = "Импортируемая копия старше локальной базы проекта на ${(currentLocalTimestamp - remoteTs) / 60000} мин.",
                        remotePayloadJson = content
                    )
                )
            }

            DriveOperationResult.Success("Резервная копия проверена и готова к применению.", emptyList())
        } catch (e: Exception) {
            DriveOperationResult.Error("Ошибка чтения файла синхронизации: ${e.localizedMessage}")
        }
    }
}
