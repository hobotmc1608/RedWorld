package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {

    // ================= DOCUMENTS =================

    @Query("SELECT * FROM knowledge_documents ORDER BY updatedAt DESC")
    fun observeAllDocuments(): Flow<List<KnowledgeDocumentEntity>>

    @Transaction
    @Query("SELECT * FROM knowledge_documents ORDER BY updatedAt DESC")
    fun observeAllDocumentsWithDetails(): Flow<List<KnowledgeDocumentWithDetails>>

    @Transaction
    @Query("SELECT * FROM knowledge_documents WHERE id = :documentId LIMIT 1")
    fun observeDocumentWithDetailsById(documentId: Long): Flow<KnowledgeDocumentWithDetails?>

    @Query("SELECT * FROM knowledge_documents WHERE id = :documentId LIMIT 1")
    suspend fun getDocumentById(documentId: Long): KnowledgeDocumentEntity?

    @Query("SELECT * FROM knowledge_documents ORDER BY lastOpenedAt DESC LIMIT :limit")
    fun observeRecentDocuments(limit: Int = 5): Flow<List<KnowledgeDocumentEntity>>

    @Query(
        """
        SELECT * FROM knowledge_documents
        WHERE title LIKE '%' || :query || '%'
           OR contentText LIKE '%' || :query || '%'
           OR summary LIKE '%' || :query || '%'
           OR folderName LIKE '%' || :query || '%'
           OR collectionName LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun searchDocuments(query: String): Flow<List<KnowledgeDocumentEntity>>

    @Query("SELECT * FROM knowledge_documents WHERE folderName = :folder ORDER BY updatedAt DESC")
    fun observeDocumentsByFolder(folder: String): Flow<List<KnowledgeDocumentEntity>>

    @Query("SELECT * FROM knowledge_documents WHERE collectionName = :collection ORDER BY updatedAt DESC")
    fun observeDocumentsByCollection(collection: String): Flow<List<KnowledgeDocumentEntity>>

    @Query("SELECT * FROM knowledge_documents WHERE isAllowedInAiContext = 1 ORDER BY updatedAt DESC")
    suspend fun getAiEnabledDocuments(): List<KnowledgeDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: KnowledgeDocumentEntity): Long

    @Update
    suspend fun updateDocument(document: KnowledgeDocumentEntity)

    @Delete
    suspend fun deleteDocument(document: KnowledgeDocumentEntity)

    @Query("DELETE FROM knowledge_documents WHERE id = :documentId")
    suspend fun deleteDocumentById(documentId: Long)

    @Query("UPDATE knowledge_documents SET lastOpenedAt = :timestamp WHERE id = :documentId")
    suspend fun markDocumentOpened(documentId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE knowledge_documents SET isAllowedInAiContext = :allowed WHERE id = :documentId")
    suspend fun setDocumentAiContextAllowed(documentId: Long, allowed: Boolean)

    @Query("SELECT COUNT(*) FROM knowledge_documents")
    suspend fun getDocumentCount(): Int

    // ================= DOCUMENT CHUNKS (RAG) =================

    @Query("SELECT * FROM knowledge_document_chunks WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    suspend fun getChunksForDocument(documentId: Long): List<DocumentChunkEntity>

    @Query("SELECT * FROM knowledge_document_chunks WHERE documentId IN (:documentIds) ORDER BY documentId, chunkIndex ASC")
    suspend fun getChunksForDocuments(documentIds: List<Long>): List<DocumentChunkEntity>

    @Query(
        """
        SELECT * FROM knowledge_document_chunks
        WHERE chunkText LIKE '%' || :keyword || '%'
           OR sectionTitle LIKE '%' || :keyword || '%'
        LIMIT :limit
        """
    )
    suspend fun searchChunksByKeyword(keyword: String, limit: Int = 20): List<DocumentChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DocumentChunkEntity>)

    @Query("DELETE FROM knowledge_document_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksForDocument(documentId: Long)

    // ================= NOTES =================

    @Query("SELECT * FROM knowledge_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllNotes(): Flow<List<KnowledgeNoteEntity>>

    @Transaction
    @Query("SELECT * FROM knowledge_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllNotesWithTags(): Flow<List<KnowledgeNoteWithTags>>

    @Query("SELECT * FROM knowledge_notes WHERE documentId = :documentId ORDER BY isPinned DESC, updatedAt DESC")
    fun observeNotesForDocument(documentId: Long): Flow<List<KnowledgeNoteEntity>>

    @Query(
        """
        SELECT * FROM knowledge_notes
        WHERE title LIKE '%' || :query || '%'
           OR content LIKE '%' || :query || '%'
           OR quotedExcerpt LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun searchNotes(query: String): Flow<List<KnowledgeNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: KnowledgeNoteEntity): Long

    @Update
    suspend fun updateNote(note: KnowledgeNoteEntity)

    @Delete
    suspend fun deleteNote(note: KnowledgeNoteEntity)

    @Query("DELETE FROM knowledge_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    // ================= TAGS & CROSS-REFS =================

    @Query("SELECT * FROM knowledge_tags ORDER BY name ASC")
    fun observeAllTags(): Flow<List<KnowledgeTagEntity>>

    @Query("SELECT * FROM knowledge_tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): KnowledgeTagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: KnowledgeTagEntity): Long

    @Update
    suspend fun updateTag(tag: KnowledgeTagEntity)

    @Delete
    suspend fun deleteTag(tag: KnowledgeTagEntity)

    @Query("DELETE FROM knowledge_tags WHERE id = :tagId")
    suspend fun deleteTagById(tagId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDocumentTagCrossRef(crossRef: DocumentTagCrossRef)

    @Query("DELETE FROM document_tag_cross_ref WHERE documentId = :documentId AND tagId = :tagId")
    suspend fun removeTagFromDocument(documentId: Long, tagId: Long)

    @Query("DELETE FROM document_tag_cross_ref WHERE documentId = :documentId")
    suspend fun clearTagsForDocument(documentId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun removeTagFromNote(noteId: Long, tagId: Long)

    @Query(
        """
        SELECT d.* FROM knowledge_documents d
        INNER JOIN document_tag_cross_ref ref ON d.id = ref.documentId
        WHERE ref.tagId = :tagId
        ORDER BY d.updatedAt DESC
        """
    )
    fun observeDocumentsByTag(tagId: Long): Flow<List<KnowledgeDocumentEntity>>
}
