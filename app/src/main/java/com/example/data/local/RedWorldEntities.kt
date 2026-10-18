package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Canon verification status for lore facts, character cards, and world rules.
 */
@Serializable
enum class CanonStatus(val label: String, val badgeColorHex: String) {
    CANON("Канон", "#10B981"),
    ASSUMPTION("Предположение", "#F59E0B"),
    DRAFT("Черновик", "#6366F1"),
    NEEDS_REVIEW("Требует проверки", "#EF4444")
}

/**
 * Represents an independent creative World / Project in World AI Studio.
 * Users can create, rename, edit description, choose cover, archive/restore,
 * export/import, backup, and switch between projects seamlessly.
 */
@Serializable
@Entity(tableName = "world_projects")
data class WorldProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val genreOrTheme: String = "Готический техно-нуар",
    val description: String = "",
    val accentColorHex: String = "#E11D48",
    val coverImageBase64: String = "",
    val isArchived: Boolean = false,
    val templateType: String = "CUSTOM", // EMPTY, DARK_FANTASY, SCI_FI, DETECTIVE_NOIR, CUSTOM
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val genre: String get() = genreOrTheme
}

/**
 * Legacy table retained for Room schema compatibility; NotebookLM UI is completely excluded
 * per user requirements in favor of the built-in Documents & Notes system.
 */
@Serializable
data class NotebookLmNoteItem(
    val id: String = "nlm_note_${System.currentTimeMillis()}",
    val title: String,
    val content: String,
    val suggestedIntegrationType: String = "CHARACTER",
    val sourceCitation: String = "",
    val isIntegratedIntoWorld: Boolean = false,
    val integratedWorldSummary: String = "",
    val tags: List<String> = emptyList()
) {
    val suggestedTargetType: String get() = suggestedIntegrationType
    val integratedTargetSummary: String get() = integratedWorldSummary
}

@Serializable
@Entity(tableName = "notebooklm_synced_notebooks")
data class NotebookLmNotebookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val googleAccountEmail: String = "",
    val notebookTitle: String,
    val notebookCategory: String = "Исследования и лор",
    val sourcesCount: Int = 3,
    val audioOverviewSummary: String = "",
    val notesJson: String = "[]",
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    val title: String get() = notebookTitle
    val accountEmail: String get() = googleAccountEmail
    val sourceCount: Int get() = sourcesCount
    val description: String get() = audioOverviewSummary
}

/**
 * Reference image attached directly to a character card for visual consistency
 * and direct use in Image Studio.
 */
@Serializable
data class CharacterReferenceImage(
    val id: String = "ref_${System.currentTimeMillis()}",
    val title: String,
    val base64Data: String,
    val notes: String = ""
)

/**
 * Saved generation prompt stored inside a character card for quick reuse in Image Studio.
 */
@Serializable
data class CharacterSavedPrompt(
    val id: String = "prm_${System.currentTimeMillis()}",
    val title: String,
    val promptText: String,
    val category: String = "Портрет"
)

/**
 * Declared relationship extracted from a character's file or added in the character card.
 */
@Serializable
data class DeclaredCharacterRelation(
    val targetName: String,
    val targetType: String = "CHARACTER", // CHARACTER or LOCATION
    val relationType: String = "Знаком с",
    val description: String = ""
)

/**
 * Structured dossier extracted by Gemini AI (or offline parser) when uploading a character file.
 */
@Serializable
data class AiExtractedCharacterDossier(
    val name: String = "",
    val fullName: String = "",
    val aliases: String = "",
    val storyRole: String = "Ключевой персонаж",
    val characterStatus: String = "Активен",
    val factionOrGroup: String = "",
    val tagsCsv: String = "",
    val gender: String = "",
    val speciesOrOrigin: String = "Человек",
    val shortDescription: String = "",
    val detailedDescription: String = "",
    val birthDate: String = "",
    val age: String = "",
    val birthPlace: String = "",
    val appearance: String = "",
    val physique: String = "",
    val hair: String = "",
    val eyes: String = "",
    val voiceDescription: String = "",
    val defaultClothing: String = "",
    val distinguishingMarks: String = "",
    val personality: String = "",
    val archetype: String = "",
    val habits: String = "",
    val speechStyle: String = "",
    val catchphrases: String = "",
    val occupation: String = "",
    val skills: String = "",
    val strengths: String = "",
    val weaknesses: String = "",
    val inventorySummary: String = "",
    val interests: String = "",
    val preferences: String = "",
    val dislikes: String = "",
    val goals: String = "",
    val motivation: String = "",
    val internalConflict: String = "",
    val fearsAndLimitations: String = "",
    val knownFacts: String = "",
    val personalSecrets: String = "",
    val unknownFactsRestriction: String = "",
    val behavioralTraits: String = "",
    val currentLocationName: String = "Красный особняк",
    val currentMoodAndCondition: String = "Спокоен, сосредоточен",
    val currentHealthStatus: String = "В норме",
    val currentOutfit: String = "",
    val currentActiveGoal: String = "",
    val visualPassportPrompt: String = "",
    val relationshipsSummary: String = "",
    val additionalNotes: String = "",
    val savedPrompts: List<CharacterSavedPrompt> = emptyList(),
    val declaredRelationships: List<DeclaredCharacterRelation> = emptyList()
)

/**
 * Expandable Character Card Entity separating Permanent Traits (Постоянные характеристики),
 * Current State (Текущее состояние), and Event History (История конкретных событий хранится отдельно).
 */
@Serializable
@Entity(
    tableName = "redworld_characters",
    indices = [Index(value = ["name"]), Index(value = ["projectId"])]
)
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long = 1L,
    val sortOrder: Int = 0,
    // --- 1. Постоянные характеристики карточки персонажа ---
    val name: String,
    val fullName: String = "",
    val aliases: String = "",
    val storyRole: String = "Ключевой персонаж", // Главный герой, Второстепенный, Антагонист, Союзник, Эпизодический, NPC
    val characterStatus: String = "Активен",    // Активен, Вне сцены, Ранен, Пропал без вести, Погиб, В архиве
    val factionOrGroup: String = "",            // Фракция, организация, дом, группа
    val tagsCsv: String = "",                   // Теги персонажа через запятую
    val gender: String = "",
    val speciesOrOrigin: String = "Человек",
    val shortDescription: String = "",
    val detailedDescription: String = "",
    val birthDate: String = "",
    val age: String = "",
    val birthPlace: String = "",
    val appearance: String = "",
    val physique: String = "",
    val hair: String = "",
    val eyes: String = "",
    val voiceDescription: String = "",
    val defaultClothing: String = "",
    val distinguishingMarks: String = "",
    val personality: String = "",
    val archetype: String = "",
    val habits: String = "",
    val speechStyle: String = "",
    val catchphrases: String = "",
    val occupation: String = "",
    val skills: String = "",
    val strengths: String = "",
    val weaknesses: String = "",
    val inventorySummary: String = "",
    val interests: String = "",
    val preferences: String = "",
    val dislikes: String = "",
    val goals: String = "",
    val motivation: String = "",
    val internalConflict: String = "",
    val fearsAndLimitations: String = "",
    val fearsAndTaboos: String = "",
    val limitations: String = "",
    val knownFacts: String = "",
    val personalSecrets: String = "",
    val unknownFactsRestriction: String = "",
    val behavioralTraits: String = "",
    // --- Avatar, Visual Passport, Reference Images & Saved Prompts for Image Studio ---
    val avatarBase64: String = "",
    val visualPassportPrompt: String = "",
    val referenceImagesJson: String = "[]", // List<CharacterReferenceImage>
    val savedPromptsJson: String = "[]",    // List<CharacterSavedPrompt>
    // --- Declared & Pending Relationships for Auto-Linking ---
    val declaredRelationshipsJson: String = "[]", // List<DeclaredCharacterRelation>
    val sourceFileName: String = "",
    // Expandable custom fields (JSON key-value pairs)
    val customFieldsJson: String = "{}",
    // --- 2. Текущее состояние (отделено от постоянных характеристик и истории) ---
    val currentLocationName: String = "Красный особняк",
    val currentMoodAndCondition: String = "Спокоен, сосредоточен",
    val currentHealthStatus: String = "В норме",
    val currentOutfit: String = "",
    val currentActiveGoal: String = "",
    // --- Metadata, Organization Flags & Links ---
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val canonStatus: String = CanonStatus.CANON.name,
    val relationshipsSummary: String = "",
    val linkedLocationIdsCsv: String = "",
    val linkedEventIdsCsv: String = "",
    val linkedPlotlineIdsCsv: String = "",
    val linkedDocumentIdsCsv: String = "",
    val linkedItemIdsCsv: String = "",
    val additionalNotes: String = "",
    val notes: String = "",
    val versionNumber: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun parsedTags(): List<String> =
        tagsCsv.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }

    fun calculateCompletenessPercent(): Int {
        val coreChecks = listOf(
            name.isNotBlank(),
            shortDescription.isNotBlank() || detailedDescription.isNotBlank(),
            occupation.isNotBlank() || storyRole.isNotBlank(),
            appearance.isNotBlank(),
            hair.isNotBlank() || eyes.isNotBlank(),
            defaultClothing.isNotBlank(),
            personality.isNotBlank(),
            habits.isNotBlank(),
            speechStyle.isNotBlank(),
            goals.isNotBlank() || motivation.isNotBlank(),
            fearsAndLimitations.isNotBlank() || fearsAndTaboos.isNotBlank(),
            knownFacts.isNotBlank(),
            relationshipsSummary.isNotBlank() || declaredRelationshipsJson.length > 4,
            visualPassportPrompt.isNotBlank(),
            currentLocationName.isNotBlank() && currentMoodAndCondition.isNotBlank()
        )
        val filled = coreChecks.count { it }
        return ((filled.toDouble() / coreChecks.size.toDouble()) * 100.0).toInt().coerceIn(10, 100)
    }

    fun missingRecommendedFields(): List<String> {
        val missing = mutableListOf<String>()
        if (shortDescription.isBlank() && detailedDescription.isBlank()) missing.add("Биография / описание")
        if (appearance.isBlank()) missing.add("Внешность")
        if (personality.isBlank()) missing.add("Характер")
        if (speechStyle.isBlank()) missing.add("Манера речи")
        if (habits.isBlank()) missing.add("Привычки")
        if (goals.isBlank() && motivation.isBlank()) missing.add("Цели и мотивация")
        if (fearsAndLimitations.isBlank() && fearsAndTaboos.isBlank()) missing.add("Страхи и ограничения")
        if (knownFacts.isBlank()) missing.add("Известные персонажу факты")
        if (visualPassportPrompt.isBlank()) missing.add("Визуальный паспорт (Image Studio)")
        return missing
    }

    fun buildAutoVisualPassportPrompt(): String {
        val parts = mutableListOf<String>()
        parts.add("Character portrait of $name")
        if (age.isNotBlank()) parts.add("$age years old")
        if (occupation.isNotBlank()) parts.add("role: $occupation")
        if (appearance.isNotBlank()) parts.add(appearance)
        if (physique.isNotBlank()) parts.add("physique: $physique")
        if (hair.isNotBlank()) parts.add("hair: $hair")
        if (eyes.isNotBlank()) parts.add("eyes: $eyes")
        if (defaultClothing.isNotBlank()) parts.add("wearing: $defaultClothing")
        if (distinguishingMarks.isNotBlank()) parts.add("details: $distinguishingMarks")
        parts.add("high quality detailed concept art, atmospheric lighting")
        return parts.joinToString(", ")
    }
}

/**
 * Location entity for World AI Studio projects.
 * Supports name, description, image, location type, linked characters, linked objects,
 * linked events, and custom expandable fields.
 */
@Serializable
@Entity(
    tableName = "redworld_locations",
    indices = [Index(value = ["name"]), Index(value = ["projectId"])]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long = 1L,
    val name: String,
    val category: String = "Ключевая локация", // Тип локации
    val description: String,
    val imageBase64: String = "",
    val atmosphere: String = "",
    val interiorDetails: String = "",
    val exteriorDetails: String = "",
    val accessRules: String = "",
    val connectedLocationNames: String = "",
    val subLocationsCsv: String = "",
    val linkedCharactersCsv: String = "",
    val linkedCharacterNamesCsv: String = "",
    val linkedObjectsCsv: String = "",
    val linkedEventsCsv: String = "",
    val notes: String = "",
    val customFieldsJson: String = "{}",
    val canonStatus: String = CanonStatus.CANON.name,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * World Rule entity.
 * Supports rule title, description, scope of application, limitations, and linked documents/notes.
 */
@Serializable
@Entity(
    tableName = "redworld_rules",
    indices = [Index(value = ["category"]), Index(value = ["projectId"])]
)
data class WorldRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long = 1L,
    val title: String,
    val category: String,
    val content: String, // Описание правила
    val scope: String = "Весь мир", // Область применения
    val limitations: String = "", // Ограничения
    val restrictions: String = "",
    val exceptions: String = "",
    val linkedLocationNamesCsv: String = "",
    val linkedCharacterNamesCsv: String = "",
    val priorityLevel: String = "Строгий закон",
    val canonStatus: String = CanonStatus.CANON.name,
    val linkedDocumentIdsCsv: String = "", // Связанные документы или заметки
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Plotline entity (Раздел «Сюжет»).
 * Contains plotlines, tasks, possible development options, linked characters,
 * linked locations, and plotline state.
 */
@Serializable
@Entity(
    tableName = "redworld_plotlines",
    indices = [Index(value = ["projectId"])]
)
data class PlotlineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long = 1L,
    val title: String,
    val description: String,
    val tasks: String = "", // Задачи сюжетной линии
    val developmentOptions: String = "", // Возможные варианты развития
    val possibleOutcomes: String = "",
    val involvedCharactersCsv: String = "", // Связанные персонажи
    val linkedCharacterNamesCsv: String = "",
    val linkedLocationsCsv: String = "", // Связанные локации
    val linkedLocationNamesCsv: String = "",
    val status: String = "Активна", // Состояние сюжетной линии (Активна, В разработке, Заморожена, Завершена)
    val notes: String = "",
    val canonStatus: String = CanonStatus.CANON.name,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Event History entity (Раздел «История событий»).
 * Contains title, description, date/moment, place, participants, causes,
 * consequences, linked events, and link to plotlines.
 * Draft simulation events stay separate (`isDraftSimulation = true`) until confirmed!
 */
@Serializable
@Entity(
    tableName = "redworld_events",
    indices = [Index(value = ["chronologyOrder"]), Index(value = ["projectId"])]
)
data class WorldEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long = 1L,
    val title: String,
    val eventType: String = "Событие",
    val timelineDate: String,
    val chronologyOrder: Int = 0,
    val locationName: String = "Красный особняк",
    val participantNamesCsv: String = "",
    val description: String,
    val causes: String = "", // Причины события
    val consequences: String = "", // Последствия события
    val linkedEventsCsv: String = "", // Связанные события
    val linkedEventTitlesCsv: String = "",
    val linkedPlotlinesCsv: String = "", // Связь с сюжетными линиями
    val linkedPlotlineTitlesCsv: String = "",
    val relatedProjectLore: String = "", // Связанные сведения проекта
    val contradictionWarnings: String = "", // Предупреждения о противоречиях
    val isDraftSimulation: Boolean = false, // Черновик симуляции (не в каноне до подтверждения)
    val isSimulationDraft: Boolean = false,
    val canonStatus: String = CanonStatus.CANON.name,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Object / Artifact / Faction entity (Раздел «Объекты»).
 * Supports name, description, image, location, owner, properties/limitations, and linked events.
 */
@Serializable
@Entity(tableName = "redworld_items")
data class WorldItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long = 1L,
    val name: String,
    val itemType: String = "Объект",
    val category: String = "Предмет",
    val imageBase64: String = "",
    val origin: String = "",
    val ownerOrLeader: String = "", // Владелец, если применимо
    val ownerCharacterName: String = "",
    val locationName: String = "", // Местоположение
    val description: String,
    val properties: String = "", // Свойства и ограничения
    val linkedEventsCsv: String = "", // Связанные события
    val notes: String = "",
    val canonStatus: String = CanonStatus.CANON.name,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Visual Relationship Map Node & Edge persistence.
 */
@Serializable
@Entity(
    tableName = "redworld_relationships",
    indices = [
        Index(value = ["sourceType", "sourceId"]),
        Index(value = ["targetType", "targetId"]),
        Index(value = ["projectId"])
    ]
)
data class RelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long = 1L,
    val sourceType: String, // CHARACTER, LOCATION, EVENT, DOCUMENT, RULE, ITEM, PLOTLINE, IMAGE
    val sourceId: Long,
    val sourceLabel: String,
    val targetType: String,
    val targetId: Long,
    val targetLabel: String,
    val relationType: String,
    val description: String = "",
    val canonStatus: String = CanonStatus.CANON.name,
    val sourceX: Float = 0f,
    val sourceY: Float = 0f,
    val targetX: Float = 0f,
    val targetY: Float = 0f,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Version Snapshot entity for characters, world rules, documents, and backups.
 */
@Serializable
@Entity(
    tableName = "redworld_version_snapshots",
    indices = [Index(value = ["entityType", "entityId"])]
)
data class VersionSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long = 1L,
    val entityType: String,
    val entityId: Long,
    val entityTitle: String,
    val versionNumber: Int,
    val changeSummary: String,
    val payloadJson: String,
    val backupFilePath: String = "",
    val author: String = "Пользователь",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Gemini Chat Session entity.
 */
@Serializable
@Entity(tableName = "gemini_chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long = 1L,
    val title: String,
    val modelId: String = "gemini-3.5-flash",
    val isRedWorldContextEnabled: Boolean = true,
    val selectedDocumentIdsCsv: String = "",
    val assistantMode: String = "CONSULTATION",
    val draftInputText: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Gemini Chat Message entity.
 */
@Serializable
@Entity(
    tableName = "gemini_chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionId: Long,
    val role: String,
    val content: String,
    val citedSourcesJson: String = "[]",
    val proposedChangeJson: String = "",
    val isProposalApplied: Boolean = false,
    val attachmentSummary: String = "",
    val modelUsed: String = "gemini-3.5-flash",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Image Studio Generation Record entity.
 */
@Serializable
@Entity(tableName = "studio_generated_images")
data class GeneratedImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long = 1L,
    val prompt: String,
    val templateCategory: String = "Портрет",
    val modelId: String = "gemini-2.5-flash-image",
    val aspectRatio: String = "1:1",
    val imageBase64OrPath: String = "",
    val linkedCharacterId: Long? = null,
    val linkedCharacterName: String = "",
    val linkedLocationId: Long? = null,
    val linkedLocationName: String = "",
    val linkedEventId: Long? = null,
    val linkedEventTitle: String = "",
    val usedVisualPassport: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Proposed before/after diff for a character card field.
 * Never applied automatically — always requires explicit user confirmation.
 */
@Serializable
data class ProposedCardFieldDiff(
    val id: String = "diff_${System.currentTimeMillis()}",
    val fieldKey: String,       // e.g., "personality", "speechStyle", "habits", "motivation", "goals", "fearsAndLimitations", "knownFacts"
    val fieldLabel: String,     // e.g., "Характер", "Манера речи", "Привычки"
    val beforeValue: String,
    val afterValue: String,
    val reason: String = ""
)

/**
 * Individual criterion score for the 10-point Roleplay Quality Evaluation.
 */
@Serializable
data class RoleplayCriterionScore(
    val criterionNumber: Int,
    val criterionTitle: String,
    val scoreOutOf10: Int,
    val explanation: String,
    val exampleQuote: String = "",
    val cardBasis: String = "",
    val detectedContradiction: String = "",
    val improvementSuggestion: String = ""
)

/**
 * Debug & Explainability metadata showing verifiable context used for AI generation/simulation
 * without exposing internal hidden chain-of-thought.
 */
@Serializable
data class AiDebugExplanation(
    val usedCharacterCards: List<String> = emptyList(),
    val appliedWorldRules: List<String> = emptyList(),
    val usedHistoryEvents: List<String> = emptyList(),
    val influencingRelationships: List<String> = emptyList(),
    val appliedConstraints: List<String> = emptyList(),
    val missingDataWarnings: List<String> = emptyList(),
    val detectedContradictions: List<String> = emptyList(),
    val conciseExplanation: String = ""
)

/**
 * Full 10-criteria Roleplay Evaluation report.
 */
@Serializable
data class RoleplayEvaluationReport(
    val characterId: Long,
    val characterName: String,
    val overallScore: Double,
    val insufficientCardDataWarning: String = "",
    val disclaimer: String = "Оценка ИИ носит аналитический рекомендательный характер и не является объективной истиной.",
    val criteria: List<RoleplayCriterionScore> = emptyList(),
    val proposedFieldChanges: List<ProposedCardFieldDiff> = emptyList(),
    val debugExplanation: AiDebugExplanation = AiDebugExplanation(),
    val evaluatedAt: Long = System.currentTimeMillis()
)
