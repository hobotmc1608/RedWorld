package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Supported AI Providers in World AI Studio.
 * Architecture is extensible so additional providers can be registered without modifying core chat logic.
 */
enum class AiProviderType(
    val id: String,
    val displayName: String,
    val shortName: String,
    val baseUrl: String,
    val keyPlaceholder: String,
    val description: String
) {
    GEMINI(
        id = "GEMINI",
        displayName = "Google Gemini",
        shortName = "Gemini",
        baseUrl = "https://generativelanguage.googleapis.com/",
        keyPlaceholder = "AIzaSy...",
        description = "Официальный API Google Gemini (мультимодальный чат, RAG-анализ канона и генерация иллюстраций)"
    ),
    OPENAI(
        id = "OPENAI",
        displayName = "OpenAI GPT",
        shortName = "OpenAI",
        baseUrl = "https://api.openai.com/",
        keyPlaceholder = "sk-...",
        description = "Официальный API OpenAI (модели GPT-4o, GPT-4.1, o3-mini)"
    ),
    XAI(
        id = "XAI",
        displayName = "xAI Grok",
        shortName = "Grok",
        baseUrl = "https://api.x.ai/",
        keyPlaceholder = "xai-...",
        description = "Официальный API xAI (модели Grok 3, Grok 3 Mini, Grok 2)"
    );

    companion object {
        fun fromId(id: String?): AiProviderType {
            if (id.isNullOrBlank()) return GEMINI
            return entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: GEMINI
        }
    }
}

/**
 * Metadata and parameter support for an AI model of any provider.
 */
data class AiModelSpec(
    val provider: AiProviderType,
    val modelId: String,
    val displayName: String,
    val description: String,
    val supportsVisionInput: Boolean = true,
    val supportsTemperature: Boolean = true,
    val supportsTopP: Boolean = true,
    val supportsMaxTokens: Boolean = true,
    val recommendedFor: String = "",
    val isCustomModel: Boolean = false
)

/**
 * User-configurable generation parameters applied across providers when supported by the active model.
 */
data class AiGenerationParams(
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val maxTokens: Int = 2048
)

object SupportedAiModels {

    val geminiModels: List<AiModelSpec> = SupportedGeminiModels.textModels.map { spec ->
        AiModelSpec(
            provider = AiProviderType.GEMINI,
            modelId = spec.modelId,
            displayName = spec.displayName,
            description = spec.description,
            supportsVisionInput = spec.supportsVisionInput,
            supportsTemperature = true,
            supportsTopP = true,
            supportsMaxTokens = true,
            recommendedFor = spec.recommendedFor
        )
    }

    val openAiModels: List<AiModelSpec> = listOf(
        AiModelSpec(
            provider = AiProviderType.OPENAI,
            modelId = "gpt-4o",
            displayName = "GPT-4o",
            description = "Флагманская мультимодальная модель OpenAI для работы с лором, диалогами, документами и изображениями.",
            supportsVisionInput = true,
            supportsTemperature = true,
            supportsTopP = true,
            supportsMaxTokens = true,
            recommendedFor = "Универсальный чат, глубокий разбор карточек и сцен"
        ),
        AiModelSpec(
            provider = AiProviderType.OPENAI,
            modelId = "gpt-4o-mini",
            displayName = "GPT-4o Mini",
            description = "Быстрая и экономичная модель OpenAI с поддержкой изображений и низкой задержкой.",
            supportsVisionInput = true,
            supportsTemperature = true,
            supportsTopP = true,
            supportsMaxTokens = true,
            recommendedFor = "Повседневные диалоги, быстрый пересказ документов"
        ),
        AiModelSpec(
            provider = AiProviderType.OPENAI,
            modelId = "gpt-4.1",
            displayName = "GPT-4.1",
            description = "Модель повышенной точности следования инструкциям и работы с большими контекстами мира.",
            supportsVisionInput = true,
            supportsTemperature = true,
            supportsTopP = true,
            supportsMaxTokens = true,
            recommendedFor = "Сложный аудит канона, структурированное извлечение досье"
        ),
        AiModelSpec(
            provider = AiProviderType.OPENAI,
            modelId = "gpt-4.1-mini",
            displayName = "GPT-4.1 Mini",
            description = "Сбалансированная модель серии GPT-4.1 для быстрой генерации и ролевого отыгрыша.",
            supportsVisionInput = true,
            supportsTemperature = true,
            supportsTopP = true,
            supportsMaxTokens = true,
            recommendedFor = "Тестирование персонажей, генерация сцен"
        ),
        AiModelSpec(
            provider = AiProviderType.OPENAI,
            modelId = "o3-mini",
            displayName = "OpenAI o3-mini (Reasoning)",
            description = "Модель глубокого логического рассуждения (параметры Temperature и Top P фиксируются моделью автоматически).",
            supportsVisionInput = false,
            supportsTemperature = false,
            supportsTopP = false,
            supportsMaxTokens = true,
            recommendedFor = "Поиск логических противоречий в сюжете и правилах мира"
        )
    )

    val xaiModels: List<AiModelSpec> = listOf(
        AiModelSpec(
            provider = AiProviderType.XAI,
            modelId = "grok-3",
            displayName = "Grok 3",
            description = "Флагманская модель xAI для выразительных диалогов, нестандартных сюжетных ходов и глубокого анализа.",
            supportsVisionInput = false,
            supportsTemperature = true,
            supportsTopP = true,
            supportsMaxTokens = true,
            recommendedFor = "Живой отыгрыш персонажей, сложные сюжетные линии"
        ),
        AiModelSpec(
            provider = AiProviderType.XAI,
            modelId = "grok-3-mini",
            displayName = "Grok 3 Mini",
            description = "Быстрая рассуждающая модель xAI для оперативных проверок и генерации черновиков.",
            supportsVisionInput = false,
            supportsTemperature = true,
            supportsTopP = true,
            supportsMaxTokens = true,
            recommendedFor = "Быстрые ответы, черновики событий и заметок"
        ),
        AiModelSpec(
            provider = AiProviderType.XAI,
            modelId = "grok-2-latest",
            displayName = "Grok 2 Latest",
            description = "Стабильная текстовая модель xAI второго поколения.",
            supportsVisionInput = false,
            supportsTemperature = true,
            supportsTopP = true,
            supportsMaxTokens = true,
            recommendedFor = "Консультации по миру и документам"
        ),
        AiModelSpec(
            provider = AiProviderType.XAI,
            modelId = "grok-2-vision-latest",
            displayName = "Grok 2 Vision",
            description = "Мультимодальная модель xAI с поддержкой анализа прикреплённых изображений и референсов.",
            supportsVisionInput = true,
            supportsTemperature = true,
            supportsTopP = true,
            supportsMaxTokens = true,
            recommendedFor = "Анализ иллюстраций, портретов персонажей и карт"
        )
    )

    fun modelsForProvider(provider: AiProviderType): List<AiModelSpec> = when (provider) {
        AiProviderType.GEMINI -> geminiModels
        AiProviderType.OPENAI -> openAiModels
        AiProviderType.XAI -> xaiModels
    }

    fun defaultModelForProvider(provider: AiProviderType): String = when (provider) {
        AiProviderType.GEMINI -> "gemini-3.5-flash"
        AiProviderType.OPENAI -> "gpt-4o"
        AiProviderType.XAI -> "grok-3"
    }

    fun findModelSpec(provider: AiProviderType, modelId: String): AiModelSpec {
        val cleanId = modelId.trim().ifEmpty { defaultModelForProvider(provider) }
        val preset = modelsForProvider(provider).firstOrNull { it.modelId.equals(cleanId, ignoreCase = true) }
        if (preset != null) return preset

        val isReasoningModel = cleanId.startsWith("o1", ignoreCase = true) ||
            cleanId.startsWith("o3", ignoreCase = true) ||
            cleanId.startsWith("o4", ignoreCase = true)

        return AiModelSpec(
            provider = provider,
            modelId = cleanId,
            displayName = "$cleanId (Свой ID)",
            description = "Пользовательский идентификатор модели для провайдера ${provider.displayName}.",
            supportsVisionInput = true,
            supportsTemperature = !isReasoningModel,
            supportsTopP = !isReasoningModel,
            supportsMaxTokens = true,
            recommendedFor = "Задано пользователем вручную",
            isCustomModel = true
        )
    }

    fun resolveProviderAndModelLabel(modelUsed: String, fallbackProvider: AiProviderType): String {
        val clean = modelUsed.trim()
        if (clean.contains("•")) return clean
        val lower = clean.lowercase()
        val detectedProvider = when {
            lower.startsWith("gemini") -> AiProviderType.GEMINI
            lower.startsWith("gpt-") || lower.startsWith("o1") || lower.startsWith("o3") || lower.startsWith("chatgpt") -> AiProviderType.OPENAI
            lower.startsWith("grok") -> AiProviderType.XAI
            else -> fallbackProvider
        }
        return "${detectedProvider.displayName} • $clean"
    }
}

// ================= OPENAI & XAI COMPATIBLE REST DTOs =================

@Serializable
data class OpenAiChatCompletionRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    val temperature: Float? = null,
    @SerialName("top_p")
    val topP: Float? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("max_completion_tokens")
    val maxCompletionTokens: Int? = null
)

@Serializable
data class OpenAiChatMessage(
    val role: String,
    val content: JsonElement
)

@Serializable
data class OpenAiChatCompletionResponse(
    val id: String? = null,
    val choices: List<OpenAiChoice>? = null,
    val error: OpenAiErrorDetail? = null
)

@Serializable
data class OpenAiChoice(
    val index: Int? = null,
    val message: OpenAiResponseMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class OpenAiResponseMessage(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class OpenAiErrorDetail(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

interface OpenAiCompatibleApiService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: OpenAiChatCompletionRequest
    ): OpenAiChatCompletionResponse
}

// ================= UNIFIED PROVIDER CLIENT CONTRACT =================

interface AiChatProviderClient {
    val providerType: AiProviderType

    suspend fun generateChatResponse(
        apiKey: String,
        modelId: String,
        conversationHistory: List<Content>,
        systemInstructionText: String? = null,
        params: AiGenerationParams = AiGenerationParams()
    ): GeminiResult<String>
}

/**
 * Adapter around the existing GeminiRemoteDataSource so all existing Gemini functionality is 100% preserved.
 */
class GeminiProviderClient(
    private val geminiRemote: GeminiRemoteDataSource
) : AiChatProviderClient {
    override val providerType: AiProviderType = AiProviderType.GEMINI

    override suspend fun generateChatResponse(
        apiKey: String,
        modelId: String,
        conversationHistory: List<Content>,
        systemInstructionText: String?,
        params: AiGenerationParams
    ): GeminiResult<String> {
        val spec = SupportedAiModels.findModelSpec(AiProviderType.GEMINI, modelId)
        return geminiRemote.generateChatResponse(
            apiKey = apiKey,
            modelId = modelId.ifBlank { SupportedAiModels.defaultModelForProvider(AiProviderType.GEMINI) },
            conversationHistory = conversationHistory,
            systemInstructionText = systemInstructionText,
            temperature = if (spec.supportsTemperature) params.temperature else 0.7f,
            topP = if (spec.supportsTopP) params.topP else 0.95f,
            maxOutputTokens = if (spec.supportsMaxTokens && params.maxTokens > 0) params.maxTokens else null
        )
    }
}

/**
 * Official REST client for OpenAI GPT (https://api.openai.com/) and xAI Grok (https://api.x.ai/).
 */
class OpenAiCompatibleProviderClient(
    override val providerType: AiProviderType
) : AiChatProviderClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: OpenAiCompatibleApiService by lazy {
        Retrofit.Builder()
            .baseUrl(providerType.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenAiCompatibleApiService::class.java)
    }

    override suspend fun generateChatResponse(
        apiKey: String,
        modelId: String,
        conversationHistory: List<Content>,
        systemInstructionText: String?,
        params: AiGenerationParams
    ): GeminiResult<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext GeminiResult.Error(
                userMessage = "API-ключ для провайдера ${providerType.displayName} не настроен. Укажите ключ в разделе «Настройки → 2. ИИ и модели».",
                technicalDetails = "Missing API Key for ${providerType.id}"
            )
        }

        val resolvedModel = modelId.trim().ifEmpty { SupportedAiModels.defaultModelForProvider(providerType) }
        val spec = SupportedAiModels.findModelSpec(providerType, resolvedModel)

        val messages = mutableListOf<OpenAiChatMessage>()
        if (!systemInstructionText.isNullOrBlank()) {
            messages.add(
                OpenAiChatMessage(
                    role = "system",
                    content = JsonPrimitive(systemInstructionText.trim())
                )
            )
        }

        conversationHistory.forEach { contentItem ->
            val mappedRole = when (contentItem.role?.lowercase()) {
                "model", "assistant" -> "assistant"
                "system" -> "system"
                else -> "user"
            }

            val textParts = contentItem.parts.mapNotNull { it.text }.filter { it.isNotBlank() }
            val imageParts = contentItem.parts.mapNotNull { it.inlineData }.filter { it.data.isNotBlank() }

            if (imageParts.isNotEmpty() && spec.supportsVisionInput && mappedRole == "user") {
                val contentArray: JsonArray = buildJsonArray {
                    val combinedText = textParts.joinToString("\n").ifBlank { "Проанализируй прикреплённое изображение." }
                    add(
                        buildJsonObject {
                            put("type", "text")
                            put("text", combinedText)
                        }
                    )
                    imageParts.forEach { img ->
                        val dataUri = "data:${img.mimeType.ifBlank { "image/jpeg" }};base64,${img.data}"
                        add(
                            buildJsonObject {
                                put("type", "image_url")
                                putJsonObject("image_url") {
                                    put("url", dataUri)
                                }
                            }
                        )
                    }
                }
                messages.add(OpenAiChatMessage(role = mappedRole, content = contentArray))
            } else {
                val combinedText = textParts.joinToString("\n").ifBlank { "..." }
                messages.add(
                    OpenAiChatMessage(
                        role = mappedRole,
                        content = JsonPrimitive(combinedText)
                    )
                )
            }
        }

        val request = if (spec.supportsTemperature) {
            OpenAiChatCompletionRequest(
                model = resolvedModel,
                messages = messages,
                temperature = params.temperature,
                topP = if (spec.supportsTopP) params.topP else null,
                maxTokens = if (spec.supportsMaxTokens && params.maxTokens > 0) params.maxTokens else null
            )
        } else {
            // Reasoning models (e.g., o1/o3-mini) use max_completion_tokens and omit temperature/top_p
            OpenAiChatCompletionRequest(
                model = resolvedModel,
                messages = messages,
                maxCompletionTokens = if (spec.supportsMaxTokens && params.maxTokens > 0) params.maxTokens else null
            )
        }

        try {
            val response = apiService.createChatCompletion(
                authHeader = "Bearer $cleanKey",
                request = request
            )
            val errMsg = response.error?.message
            if (!errMsg.isNullOrBlank()) {
                return@withContext GeminiResult.Error(
                    userMessage = "Ошибка ${providerType.displayName} ($resolvedModel): $errMsg",
                    technicalDetails = response.error.code.orEmpty()
                )
            }

            val replyText = response.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?.trim()

            if (replyText.isNullOrEmpty()) {
                GeminiResult.Error(
                    userMessage = "Модель $resolvedModel (${providerType.displayName}) вернула пустой ответ. Попробуйте переформулировать запрос или выбрать другую модель.",
                    technicalDetails = "Empty choices[0].message.content"
                )
            } else {
                GeminiResult.Success(replyText)
            }
        } catch (e: HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() ?: "" } catch (_: Exception) { "" }
            val friendlyMessage = when (e.code()) {
                400 -> "Некорректные параметры запроса (400) к ${providerType.displayName}. Проверьте идентификатор модели «$resolvedModel» и поддерживаемые параметры."
                401, 403 -> "Ошибка авторизации (${e.code()}). Проверьте правильность API-ключа ${providerType.displayName} и доступ к модели «$resolvedModel»."
                404 -> "Модель «$resolvedModel» не найдена (404) у провайдера ${providerType.displayName}. Выберите модель из списка или проверьте правильность введённого ID."
                429 -> "Превышен лимит запросов или квота ${providerType.displayName} (429 Rate Limit / Quota). Проверьте баланс или переключитесь на другого провайдера."
                500, 502, 503 -> "Временная перегрузка серверов ${providerType.displayName} (${e.code()}). Повторите попытку через несколько секунд."
                else -> "Ошибка HTTP ${e.code()} при обращении к ${providerType.displayName} (модель: $resolvedModel)."
            }
            GeminiResult.Error(friendlyMessage, errorBody.take(400))
        } catch (e: java.util.concurrent.CancellationException) {
            throw e
        } catch (e: Exception) {
            GeminiResult.Error(
                userMessage = "Сетевая ошибка при обращении к ${providerType.displayName}: ${e.localizedMessage ?: "Проверьте подключение к интернету"}",
                technicalDetails = e.javaClass.simpleName
            )
        }
    }
}

/**
 * Unified multi-provider AI router.
 * Delegates requests to the selected provider (Google Gemini, OpenAI GPT, xAI Grok, or future providers)
 * without requiring changes to the application's business logic.
 */
class MultiProviderAiService(
    val geminiRemote: GeminiRemoteDataSource = GeminiRemoteDataSource()
) {
    private val clients: MutableMap<AiProviderType, AiChatProviderClient> = mutableMapOf(
        AiProviderType.GEMINI to GeminiProviderClient(geminiRemote),
        AiProviderType.OPENAI to OpenAiCompatibleProviderClient(AiProviderType.OPENAI),
        AiProviderType.XAI to OpenAiCompatibleProviderClient(AiProviderType.XAI)
    )

    fun registerProviderClient(client: AiChatProviderClient) {
        clients[client.providerType] = client
    }

    suspend fun generateChatResponse(
        provider: AiProviderType,
        apiKey: String,
        modelId: String,
        conversationHistory: List<Content>,
        systemInstructionText: String? = null,
        params: AiGenerationParams = AiGenerationParams()
    ): GeminiResult<String> {
        val client = clients[provider] ?: clients.getValue(AiProviderType.GEMINI)
        return client.generateChatResponse(
            apiKey = apiKey,
            modelId = modelId,
            conversationHistory = conversationHistory,
            systemInstructionText = systemInstructionText,
            params = params
        )
    }
}
