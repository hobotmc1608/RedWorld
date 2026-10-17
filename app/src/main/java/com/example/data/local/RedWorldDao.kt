package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RedWorldDao {

    // ================= CHARACTERS =================
    @Query("SELECT * FROM redworld_characters ORDER BY updatedAt DESC")
    fun observeCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM redworld_characters WHERE id = :id LIMIT 1")
    suspend fun getCharacterById(id: Long): CharacterEntity?

    @Query("SELECT * FROM redworld_characters ORDER BY updatedAt DESC")
    suspend fun getAllCharacters(): List<CharacterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)

    @Query("SELECT COUNT(*) FROM redworld_characters")
    suspend fun getCharacterCount(): Int

    // ================= LOCATIONS =================
    @Query("SELECT * FROM redworld_locations ORDER BY id ASC")
    fun observeLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM redworld_locations ORDER BY id ASC")
    suspend fun getAllLocations(): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    // ================= WORLD RULES =================
    @Query("SELECT * FROM redworld_rules ORDER BY category ASC, id ASC")
    fun observeWorldRules(): Flow<List<WorldRuleEntity>>

    @Query("SELECT * FROM redworld_rules ORDER BY category ASC, id ASC")
    suspend fun getAllWorldRules(): List<WorldRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorldRule(rule: WorldRuleEntity): Long

    @Update
    suspend fun updateWorldRule(rule: WorldRuleEntity)

    @Delete
    suspend fun deleteWorldRule(rule: WorldRuleEntity)

    // ================= EVENTS & SCENES =================
    @Query("SELECT * FROM redworld_events ORDER BY chronologyOrder ASC, createdAt DESC")
    fun observeEvents(): Flow<List<WorldEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: WorldEventEntity): Long

    @Update
    suspend fun updateEvent(event: WorldEventEntity)

    @Delete
    suspend fun deleteEvent(event: WorldEventEntity)

    // ================= ITEMS & ORGANIZATIONS =================
    @Query("SELECT * FROM redworld_items ORDER BY updatedAt DESC")
    fun observeItems(): Flow<List<WorldItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WorldItemEntity): Long

    @Delete
    suspend fun deleteItem(item: WorldItemEntity)

    // ================= RELATIONSHIPS (VISUAL MAP) =================
    @Query("SELECT * FROM redworld_relationships ORDER BY updatedAt DESC")
    fun observeRelationships(): Flow<List<RelationshipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: RelationshipEntity): Long

    @Update
    suspend fun updateRelationship(relationship: RelationshipEntity)

    @Delete
    suspend fun deleteRelationship(relationship: RelationshipEntity)

    @Query("DELETE FROM redworld_relationships WHERE id = :id")
    suspend fun deleteRelationshipById(id: Long)

    // ================= VERSION SNAPSHOTS =================
    @Query("SELECT * FROM redworld_version_snapshots ORDER BY createdAt DESC")
    fun observeVersionSnapshots(): Flow<List<VersionSnapshotEntity>>

    @Query("SELECT * FROM redworld_version_snapshots WHERE entityType = :type AND entityId = :entityId ORDER BY versionNumber DESC")
    fun observeSnapshotsForEntity(type: String, entityId: Long): Flow<List<VersionSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersionSnapshot(snapshot: VersionSnapshotEntity): Long

    // ================= GEMINI CHATS =================
    @Query("SELECT * FROM gemini_chat_sessions ORDER BY updatedAt DESC")
    fun observeChatSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM gemini_chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getChatSessionById(sessionId: Long): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateChatSession(session: ChatSessionEntity)

    @Query("DELETE FROM gemini_chat_sessions WHERE id = :sessionId")
    suspend fun deleteChatSessionById(sessionId: Long)

    @Query("SELECT * FROM gemini_chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM gemini_chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: Long): List<ChatMessageEntity>

    @Query("SELECT * FROM gemini_chat_messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchAllChatMessages(query: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM gemini_chat_messages WHERE id = :messageId")
    suspend fun deleteChatMessageById(messageId: Long)

    // ================= IMAGE STUDIO =================
    @Query("SELECT * FROM studio_generated_images ORDER BY createdAt DESC")
    fun observeGeneratedImages(): Flow<List<GeneratedImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedImage(image: GeneratedImageEntity): Long

    @Delete
    suspend fun deleteGeneratedImage(image: GeneratedImageEntity)
}
