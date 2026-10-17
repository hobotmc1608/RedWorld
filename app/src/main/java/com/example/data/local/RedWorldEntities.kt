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
 * Character card entity separating Permanent Facts (Постоянные сведения),
 * Current State (Текущее состояние), and Event History (История событий).
 */
@Serializable
@Entity(
    tableName = "redworld_characters",
    indices = [Index(value = ["name"])]
)
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    // --- 1. Постоянные сведения (Permanent Lore) ---
    val name: String,
    val aliases: String = "",
    val birthDate: String = "",
    val age: String = "",
    val birthPlace: String = "",
    val appearance: String = "",
    val physique: String = "",
    val hair: String = "",
    val eyes: String = "",
    val defaultClothing: String = "",
    val distinguishingMarks: String = "",
    val personality: String = "",
    val habits: String = "",
    val speechStyle: String = "",
    val occupation: String = "",
    val skills: String = "",
    val interests: String = "",
    val preferences: String = "",
    val goals: String = "",
    val motivation: String = "",
    val behavioralTraits: String = "",
    // Approved Visual Passport for Image Studio
    val visualPassportPrompt: String = "",
    // Custom expandable JSON/key-value fields
    val customFieldsJson: String = "{}",
    // --- 2. Текущее состояние (Current State - separate from permanent lore) ---
    val currentLocationName: String = "Красный особняк",
    val currentMoodAndCondition: String = "Спокоен, сосредоточен",
    val currentOutfit: String = "",
    val currentActiveGoal: String = "",
    // --- Metadata & Canon Status ---
    val canonStatus: String = CanonStatus.CANON.name,
    val relationshipsSummary: String = "",
    val linkedLocationIdsCsv: String = "",
    val linkedEventIdsCsv: String = "",
    val additionalNotes: String = "",
    val versionNumber: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Location entity for RedWorld (Красный особняк, Зелёная улица, Магазин, Бар, etc.)
 */
@Serializable
@Entity(
    tableName = "redworld_locations",
    indices = [Index(value = ["name"])]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val category: String = "Ключевая локация", // Особняк, Улица, Заведение, Район
    val description: String,
    val atmosphere: String = "",
    val interiorDetails: String = "",
    val exteriorDetails: String = "",
    val accessRules: String = "",
    val connectedLocationNames: String = "",
    val canonStatus: String = CanonStatus.CANON.name,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * World Rule / World Card section entity. Keeps general world laws strictly separated
 * from concrete event memories.
 */
@Serializable
@Entity(
    tableName = "redworld_rules",
    indices = [Index(value = ["category"])]
)
data class WorldRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val category: String, // Общие правила, География, Социальные правила, Организации, Сверхъестественное, Автономия персонажей, Взаимодействие с игроком, Правила сцен, Хронология
    val content: String,
    val priorityLevel: String = "Строгий закон",
    val canonStatus: String = CanonStatus.CANON.name,
    val linkedDocumentIdsCsv: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Event / Scene History entity (3. История событий).
 * Modifying current state does not overwrite historical events.
 */
@Serializable
@Entity(
    tableName = "redworld_events",
    indices = [Index(value = ["chronologyOrder"])]
)
data class WorldEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val eventType: String = "Событие", // Событие, Сцена, Изменение состояния
    val timelineDate: String,
    val chronologyOrder: Int = 0,
    val locationName: String = "Красный особняк",
    val participantNamesCsv: String = "",
    val description: String,
    val consequences: String = "",
    val canonStatus: String = CanonStatus.CANON.name,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Item / Organization entity for RedWorld inventory and factions.
 */
@Serializable
@Entity(tableName = "redworld_items")
data class WorldItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val itemType: String = "Предмет", // Предмет, Артефакт, Организация
    val ownerOrLeader: String = "",
    val locationName: String = "",
    val description: String,
    val properties: String = "",
    val canonStatus: String = CanonStatus.CANON.name,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Visual Relationship Map Node & Edge persistence.
 * Each relationship is a first-class database object.
 */
@Serializable
@Entity(
    tableName = "redworld_relationships",
    indices = [
        Index(value = ["sourceType", "sourceId"]),
        Index(value = ["targetType", "targetId"])
    ]
)
data class RelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sourceType: String, // CHARACTER, LOCATION, EVENT, DOCUMENT, RULE, ITEM, ORGANIZATION, IMAGE
    val sourceId: Long,
    val sourceLabel: String,
    val targetType: String,
    val targetId: Long,
    val targetLabel: String,
    val relationType: String, // Живёт в, Работает в, Знаком с, Состоит в отношениях с, Участвует в событии, Событие происходит в локации, Документ описывает персонажа, Изображение относится к персонажу, Правило регулирует сцену, Событие изменяет отношения, Локация связана с локацией
    val description: String = "",
    val canonStatus: String = CanonStatus.CANON.name,
    val sourceX: Float = 0f,
    val sourceY: Float = 0f,
    val targetX: Float = 0f,
    val targetY: Float = 0f,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Version Snapshot entity for characters, world rules, documents, and XLSX files.
 * Supports version comparison (diff) and rollback.
 */
@Serializable
@Entity(
    tableName = "redworld_version_snapshots",
    indices = [Index(value = ["entityType", "entityId"])]
)
data class VersionSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val entityType: String, // CHARACTER, WORLD_RULE, DOCUMENT, XLSX_WORKBOOK, AI_PROPOSAL
    val entityId: Long,
    val entityTitle: String,
    val versionNumber: Int,
    val changeSummary: String,
    val payloadJson: String,
    val backupFilePath: String = "",
    val author: String = "Пользователь", // Пользователь или ИИ (после подтверждения)
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
    val title: String,
    val modelId: String = "gemini-3.5-flash",
    val isRedWorldContextEnabled: Boolean = true,
    val selectedDocumentIdsCsv: String = "",
    val assistantMode: String = "CONSULTATION", // CONSULTATION, DRAFT, PROPOSAL, APPLY_CONFIRMED
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
    val role: String, // "user", "model", "system"
    val content: String,
    val citedSourcesJson: String = "[]",
    val proposedChangeJson: String = "", // Non-empty when AI proposes a canon/draft change awaiting user confirmation
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
    val prompt: String,
    val templateCategory: String = "Портрет",
    val modelId: String = "gemini-2.5-flash-image",
    val aspectRatio: String = "1:1",
    val imageBase64OrPath: String = "",
    val linkedCharacterId: Long? = null,
    val linkedCharacterName: String = "",
    val linkedLocationId: Long? = null,
    val linkedLocationName: String = "",
    val usedVisualPassport: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
