package com.example.data.repository

import com.example.data.local.CanonStatus
import com.example.data.local.CharacterEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.GeneratedImageEntity
import com.example.data.local.LocationEntity
import com.example.data.local.RedWorldDao
import com.example.data.local.RelationshipEntity
import com.example.data.local.VersionSnapshotEntity
import com.example.data.local.WorldEventEntity
import com.example.data.local.WorldItemEntity
import com.example.data.local.WorldRuleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AiChangeProposal(
    val proposalTitle: String,
    val targetEntityType: String, // CHARACTER, LOCATION, WORLD_RULE
    val targetEntityName: String,
    val fieldToUpdate: String,
    val previousValue: String,
    val proposedValue: String,
    val proposedStatus: String = CanonStatus.DRAFT.name,
    val rationale: String,
    val sourceDocumentsUsed: List<String> = emptyList()
)

class RedWorldRepository(private val dao: RedWorldDao) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    val characters: Flow<List<CharacterEntity>> = dao.observeCharacters()
    val locations: Flow<List<LocationEntity>> = dao.observeLocations()
    val worldRules: Flow<List<WorldRuleEntity>> = dao.observeWorldRules()
    val events: Flow<List<WorldEventEntity>> = dao.observeEvents()
    val items: Flow<List<WorldItemEntity>> = dao.observeItems()
    val relationships: Flow<List<RelationshipEntity>> = dao.observeRelationships()
    val versionSnapshots: Flow<List<VersionSnapshotEntity>> = dao.observeVersionSnapshots()
    val chatSessions: Flow<List<ChatSessionEntity>> = dao.observeChatSessions()
    val generatedImages: Flow<List<GeneratedImageEntity>> = dao.observeGeneratedImages()

    fun observeMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> =
        dao.observeMessagesForSession(sessionId)

    fun searchChatMessages(query: String): Flow<List<ChatMessageEntity>> =
        dao.searchAllChatMessages(query)

    // --- Characters & Version Snapshots ---
    suspend fun saveCharacterWithSnapshot(
        character: CharacterEntity,
        changeSummary: String = "Обновление карточки персонажа",
        author: String = "Пользователь"
    ): Long {
        val nextVersion = if (character.id == 0L) 1 else character.versionNumber + 1
        val updated = character.copy(
            versionNumber = nextVersion,
            updatedAt = System.currentTimeMillis()
        )
        val savedId = dao.insertCharacter(updated)
        val finalChar = updated.copy(id = savedId)
        dao.insertVersionSnapshot(
            VersionSnapshotEntity(
                entityType = "CHARACTER",
                entityId = savedId,
                entityTitle = finalChar.name,
                versionNumber = nextVersion,
                changeSummary = changeSummary,
                payloadJson = json.encodeToString(finalChar),
                author = author
            )
        )
        return savedId
    }

    /**
     * Updates ONLY the character's Current State (Текущее состояние) without overwriting
     * Permanent Lore (Постоянные сведения) or past events. Optionally records a state transition event.
     */
    suspend fun updateCharacterCurrentStateOnly(
        characterId: Long,
        newLocation: String,
        newMoodAndCondition: String,
        newOutfit: String,
        newActiveGoal: String,
        logAsEvent: Boolean = true
    ) {
        val existing = dao.getCharacterById(characterId) ?: return
        val updated = existing.copy(
            currentLocationName = newLocation,
            currentMoodAndCondition = newMoodAndCondition,
            currentOutfit = newOutfit,
            currentActiveGoal = newActiveGoal,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateCharacter(updated)
        if (logAsEvent) {
            dao.insertEvent(
                WorldEventEntity(
                    title = "Смена состояния: ${existing.name} → $newLocation",
                    eventType = "Изменение состояния",
                    timelineDate = "Текущая сцена",
                    chronologyOrder = 99,
                    locationName = newLocation,
                    participantNamesCsv = existing.name,
                    description = "Текущее состояние обновлено: $newMoodAndCondition. Активная задача: $newActiveGoal",
                    consequences = "Постоянные биографические данные сохранены без изменений.",
                    canonStatus = existing.canonStatus
                )
            )
        }
    }

    suspend fun restoreCharacterFromSnapshot(snapshot: VersionSnapshotEntity): Boolean {
        return try {
            val restored = json.decodeFromString<CharacterEntity>(snapshot.payloadJson)
            saveCharacterWithSnapshot(
                character = restored,
                changeSummary = "Откат к версии v${snapshot.versionNumber}",
                author = "Пользователь (Восстановление версии)"
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteCharacter(character: CharacterEntity) {
        dao.deleteCharacter(character)
    }

    // --- Locations ---
    suspend fun saveLocation(location: LocationEntity): Long =
        dao.insertLocation(location.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteLocation(location: LocationEntity) = dao.deleteLocation(location)

    // --- World Rules ---
    suspend fun saveWorldRule(rule: WorldRuleEntity): Long {
        val id = dao.insertWorldRule(rule.copy(updatedAt = System.currentTimeMillis()))
        dao.insertVersionSnapshot(
            VersionSnapshotEntity(
                entityType = "WORLD_RULE",
                entityId = id,
                entityTitle = rule.title,
                versionNumber = 1,
                changeSummary = "Сохранение правила мира (${rule.category})",
                payloadJson = json.encodeToString(rule.copy(id = id)),
                author = "Пользователь"
            )
        )
        return id
    }

    suspend fun deleteWorldRule(rule: WorldRuleEntity) = dao.deleteWorldRule(rule)

    // --- Events ---
    suspend fun saveEvent(event: WorldEventEntity): Long = dao.insertEvent(event)
    suspend fun deleteEvent(event: WorldEventEntity) = dao.deleteEvent(event)

    // --- Items & Organizations ---
    suspend fun saveItem(item: WorldItemEntity): Long =
        dao.insertItem(item.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteItem(item: WorldItemEntity) = dao.deleteItem(item)

    // --- Visual Relationship Map ---
    suspend fun saveRelationship(relationship: RelationshipEntity): Long =
        dao.insertRelationship(relationship.copy(updatedAt = System.currentTimeMillis()))

    suspend fun updateRelationshipCoordinates(
        relationship: RelationshipEntity,
        sourceX: Float,
        sourceY: Float,
        targetX: Float,
        targetY: Float
    ) {
        dao.updateRelationship(
            relationship.copy(
                sourceX = sourceX,
                sourceY = sourceY,
                targetX = targetX,
                targetY = targetY,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteRelationship(id: Long) = dao.deleteRelationshipById(id)

    // --- Gemini Chat Sessions & Messages ---
    suspend fun createChatSession(
        title: String,
        modelId: String = "gemini-3.5-flash",
        isRedWorldContextEnabled: Boolean = true,
        selectedDocumentIdsCsv: String = "",
        assistantMode: String = "CONSULTATION"
    ): Long {
        return dao.insertChatSession(
            ChatSessionEntity(
                title = title,
                modelId = modelId,
                isRedWorldContextEnabled = isRedWorldContextEnabled,
                selectedDocumentIdsCsv = selectedDocumentIdsCsv,
                assistantMode = assistantMode
            )
        )
    }

    suspend fun renameChatSession(sessionId: Long, newTitle: String) {
        val current = dao.getChatSessionById(sessionId) ?: return
        dao.updateChatSession(
            current.copy(
                title = newTitle.trim().ifEmpty { current.title },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateChatSessionSettings(
        sessionId: Long,
        modelId: String? = null,
        isRedWorldContextEnabled: Boolean? = null,
        selectedDocumentIdsCsv: String? = null,
        assistantMode: String? = null
    ) {
        val current = dao.getChatSessionById(sessionId) ?: return
        dao.updateChatSession(
            current.copy(
                modelId = modelId ?: current.modelId,
                isRedWorldContextEnabled = isRedWorldContextEnabled ?: current.isRedWorldContextEnabled,
                selectedDocumentIdsCsv = selectedDocumentIdsCsv ?: current.selectedDocumentIdsCsv,
                assistantMode = assistantMode ?: current.assistantMode,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteChatSession(sessionId: Long) {
        dao.deleteChatSessionById(sessionId)
    }

    suspend fun getMessagesForSession(sessionId: Long): List<ChatMessageEntity> =
        dao.getMessagesForSession(sessionId)

    suspend fun insertChatMessage(message: ChatMessageEntity): Long {
        val id = dao.insertChatMessage(message)
        val session = dao.getChatSessionById(message.sessionId)
        if (session != null) {
            dao.updateChatSession(session.copy(updatedAt = System.currentTimeMillis()))
        }
        return id
    }

    suspend fun deleteChatMessage(messageId: Long) = dao.deleteChatMessageById(messageId)

    suspend fun markProposalApplied(message: ChatMessageEntity) {
        dao.updateChatMessage(message.copy(isProposalApplied = true))
    }

    // --- Image Studio ---
    suspend fun saveGeneratedImage(image: GeneratedImageEntity): Long {
        val id = dao.insertGeneratedImage(image)
        if (image.linkedCharacterId != null && image.linkedCharacterName.isNotBlank()) {
            dao.insertRelationship(
                RelationshipEntity(
                    sourceType = "IMAGE",
                    sourceId = id,
                    sourceLabel = "Арт: ${image.templateCategory}",
                    targetType = "CHARACTER",
                    targetId = image.linkedCharacterId,
                    targetLabel = image.linkedCharacterName,
                    relationType = "Изображение относится к персонажу",
                    description = image.prompt.take(90),
                    sourceX = 360f,
                    sourceY = 380f,
                    targetX = 240f,
                    targetY = 220f
                )
            )
        }
        return id
    }

    suspend fun deleteGeneratedImage(image: GeneratedImageEntity) = dao.deleteGeneratedImage(image)

    suspend fun recordXlsxVersionSnapshot(
        fileName: String,
        changeSummary: String,
        backupFilePath: String,
        diffsJson: String
    ) {
        dao.insertVersionSnapshot(
            VersionSnapshotEntity(
                entityType = "XLSX_WORKBOOK",
                entityId = 1L,
                entityTitle = fileName,
                versionNumber = (System.currentTimeMillis() % 10000).toInt(),
                changeSummary = changeSummary,
                payloadJson = diffsJson,
                backupFilePath = backupFilePath,
                author = "Редактор XLSX"
            )
        )
    }
}
