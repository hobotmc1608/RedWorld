package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null
)

@Serializable
data class ImageConfig(
    val aspectRatio: String,
    val imageSize: String? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
    val promptFeedback: PromptFeedback? = null
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

@Serializable
data class PromptFeedback(
    val blockReason: String? = null
)

/**
 * Metadata describing the capabilities of supported Gemini models.
 * Follows the gemini-api skill model selection rules strictly.
 */
data class GeminiModelSpec(
    val modelId: String,
    val displayName: String,
    val description: String,
    val supportsText: Boolean = true,
    val supportsVisionInput: Boolean = true,
    val supportsImageGeneration: Boolean = false,
    val supportsHighResImageSize: Boolean = false,
    val recommendedFor: String
)

object SupportedGeminiModels {
    val textModels = listOf(
        GeminiModelSpec(
            modelId = "gemini-3.5-flash",
            displayName = "Gemini 3.5 Flash",
            description = "Быстрая универсальная модель для диалогов, RAG-поиска по документам и анализа сцен RedWorld.",
            supportsText = true,
            supportsVisionInput = true,
            supportsImageGeneration = false,
            recommendedFor = "Повседневные чаты, быстрый поиск по канону, суммаризация"
        ),
        GeminiModelSpec(
            modelId = "gemini-3.1-pro-preview",
            displayName = "Gemini 3.1 Pro Preview",
            description = "Глубокое рассуждение, поиск сложных сюжетных противоречий, проектирование карточек и правил мира.",
            supportsText = true,
            supportsVisionInput = true,
            supportsImageGeneration = false,
            recommendedFor = "Аудит канона RedWorld, сложная логика, код и архитектура"
        ),
        GeminiModelSpec(
            modelId = "gemini-3.1-flash-lite-preview",
            displayName = "Gemini 3.1 Flash Lite",
            description = "Экономичная модель с минимальной задержкой для быстрых справок и черновиков.",
            supportsText = true,
            supportsVisionInput = true,
            supportsImageGeneration = false,
            recommendedFor = "Экономия квоты, короткие вопросы"
        )
    )

    val imageModels = listOf(
        GeminiModelSpec(
            modelId = "gemini-2.5-flash-image",
            displayName = "Gemini 2.5 Flash Image (Nano Banana)",
            description = "Базовая модель генерации и редактирования иллюстраций, портретов персонажей и концепт-артов.",
            supportsText = true,
            supportsVisionInput = true,
            supportsImageGeneration = true,
            supportsHighResImageSize = false,
            recommendedFor = "Портреты персонажей, концепт-арт локаций, быстрые референсы"
        ),
        GeminiModelSpec(
            modelId = "gemini-nano-banana-2.1",
            displayName = "Gemini Nano Banana 2.1 (HD)",
            description = "Высокодетализированная генерация изображений с поддержкой разрешений 1K/2K.",
            supportsText = true,
            supportsVisionInput = true,
            supportsImageGeneration = true,
            supportsHighResImageSize = true,
            recommendedFor = "Финальные иллюстрации паспорта внешности и ключевых сцен"
        ),
        GeminiModelSpec(
            modelId = "gemini-3-pro-image-preview",
            displayName = "Gemini 3 Pro Image Preview",
            description = "Продвинутая художественная генерация сложных многофигурных сцен.",
            supportsText = true,
            supportsVisionInput = true,
            supportsImageGeneration = true,
            supportsHighResImageSize = false,
            recommendedFor = "Сложные композиции и игровые сцены"
        )
    )

    fun findSpec(modelId: String): GeminiModelSpec {
        return (textModels + imageModels).firstOrNull { it.modelId == modelId }
            ?: textModels.first()
    }
}

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

sealed class GeminiResult<out T> {
    data class Success<T>(val data: T) : GeminiResult<T>()
    data class Error(val userMessage: String, val technicalDetails: String = "") : GeminiResult<Nothing>()
}

data class ImageGenerationPayload(
    val imageBase64: String?,
    val mimeType: String,
    val accompanyingText: String
)

class GeminiRemoteDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun generateChatResponse(
        apiKey: String,
        modelId: String,
        conversationHistory: List<Content>,
        systemInstructionText: String? = null,
        temperature: Float = 0.7f,
        topP: Float = 0.95f,
        maxOutputTokens: Int? = null
    ): GeminiResult<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error(
                userMessage = "API-ключ Gemini не настроен. Укажите ключ в разделе «Настройки → ИИ и модели» или через панель Secrets в AI Studio.",
                technicalDetails = "Missing GEMINI_API_KEY"
            )
        }

        val request = GenerateContentRequest(
            contents = conversationHistory,
            generationConfig = GenerationConfig(
                temperature = temperature,
                topP = topP,
                maxOutputTokens = maxOutputTokens
            ),
            systemInstruction = systemInstructionText?.takeIf { it.isNotBlank() }?.let {
                Content(parts = listOf(Part(text = it)))
            }
        )

        try {
            val response = apiService.generateContent(
                model = modelId,
                apiKey = apiKey,
                request = request
            )
            val blockReason = response.promptFeedback?.blockReason
            if (!blockReason.isNullOrBlank()) {
                return@withContext GeminiResult.Error(
                    userMessage = "Запрос отклонён фильтром безопасности модели ($blockReason).",
                    technicalDetails = "blockReason=$blockReason"
                )
            }

            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("\n")
                ?.trim()

            if (text.isNullOrEmpty()) {
                GeminiResult.Error(
                    userMessage = "Модель вернула пустой ответ. Попробуйте переформулировать запрос или сменить модель.",
                    technicalDetails = "Empty candidate parts"
                )
            } else {
                GeminiResult.Success(text)
            }
        } catch (e: HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() ?: "" } catch (_: Exception) { "" }
            val friendlyMessage = when (e.code()) {
                400 -> "Ошибка параметров запроса (400). Проверьте совместимость выбранной модели ($modelId) и вложений."
                401, 403 -> "Ошибка доступа (401/403). Проверьте правильность Gemini API Key и разрешения для модели $modelId."
                404 -> "Модель $modelId недоступна для вашего ключа (404). Выберите другую модель в списке."
                429 -> "Превышен лимит запросов или квота Gemini API (429 Rate Limit). Подождите немного или переключитесь на облегчённую модель."
                500, 503 -> "Временная перегрузка серверов Gemini (${e.code()}). Повторите попытку через несколько секунд."
                else -> "Ошибка HTTP ${e.code()} при обращении к Gemini API."
            }
            GeminiResult.Error(friendlyMessage, errorBody.take(400))
        } catch (e: java.util.concurrent.CancellationException) {
            throw e
        } catch (e: Exception) {
            GeminiResult.Error(
                userMessage = "Сетевая ошибка при обращении к Gemini API: ${e.localizedMessage ?: "Проверьте подключение к интернету"}",
                technicalDetails = e.javaClass.simpleName
            )
        }
    }

    suspend fun generateImage(
        apiKey: String,
        modelId: String,
        prompt: String,
        aspectRatio: String = "1:1",
        referenceImageBase64: String? = null,
        referenceMimeType: String = "image/jpeg"
    ): GeminiResult<ImageGenerationPayload> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error(
                userMessage = "Для генерации изображений необходим действующий Gemini API Key. Настройте его в разделе «Подключения и API».",
                technicalDetails = "Missing GEMINI_API_KEY"
            )
        }

        val spec = SupportedGeminiModels.findSpec(modelId)
        val parts = mutableListOf<Part>()
        parts.add(Part(text = prompt))
        if (!referenceImageBase64.isNullOrBlank()) {
            parts.add(Part(inlineData = InlineData(mimeType = referenceMimeType, data = referenceImageBase64)))
        }

        val imgConfig = if (spec.supportsHighResImageSize) {
            ImageConfig(aspectRatio = aspectRatio, imageSize = "1K")
        } else {
            ImageConfig(aspectRatio = aspectRatio)
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                imageConfig = imgConfig,
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        try {
            val response = apiService.generateContent(
                model = modelId,
                apiKey = apiKey,
                request = request
            )

            val candidateParts = response.candidates?.firstOrNull()?.content?.parts.orEmpty()
            val inlineImage = candidateParts.firstOrNull { it.inlineData != null }?.inlineData
            val textNotes = candidateParts.mapNotNull { it.text }.joinToString("\n").trim()

            if (inlineImage != null && inlineImage.data.isNotBlank()) {
                GeminiResult.Success(
                    ImageGenerationPayload(
                        imageBase64 = inlineImage.data,
                        mimeType = inlineImage.mimeType,
                        accompanyingText = textNotes
                    )
                )
            } else {
                GeminiResult.Error(
                    userMessage = "Модель $modelId не вернула изображение (возможно, функция генерации изображений ограничена для вашего тарифа API). Ответ модели: ${textNotes.ifEmpty { "нет данных" }}",
                    technicalDetails = "No inlineData in response"
                )
            }
        } catch (e: HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() ?: "" } catch (_: Exception) { "" }
            GeminiResult.Error(
                userMessage = "Ошибка генерации изображения (HTTP ${e.code()}). Убедитесь, что ваш API-ключ поддерживает модель $modelId.",
                technicalDetails = errorBody.take(350)
            )
        } catch (e: java.util.concurrent.CancellationException) {
            throw e
        } catch (e: Exception) {
            GeminiResult.Error(
                userMessage = "Ошибка сети при генерации изображения: ${e.localizedMessage ?: "неизвестная ошибка"}",
                technicalDetails = e.javaClass.simpleName
            )
        }
    }
}
