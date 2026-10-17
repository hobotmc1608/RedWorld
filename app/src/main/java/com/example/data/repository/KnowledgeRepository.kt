package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.DocumentAndXlsxEngine
import com.example.data.local.DocumentChunkEntity
import com.example.data.local.DocumentFormat
import com.example.data.local.DocumentTagCrossRef
import com.example.data.local.KnowledgeDao
import com.example.data.local.KnowledgeDocumentEntity
import com.example.data.local.KnowledgeDocumentWithDetails
import com.example.data.local.KnowledgeNoteEntity
import com.example.data.local.KnowledgeNoteWithTags
import com.example.data.local.KnowledgeTagEntity
import com.example.data.local.NoteTagCrossRef
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class CitedSourceSnippet(
    val documentId: Long,
    val documentTitle: String,
    val folderName: String,
    val sectionTitle: String,
    val excerpt: String,
    val relevanceScore: Int
)

/**
 * Repository for the RedWorld Knowledge module (Documents, Notes, Tags, and RAG search).
 * Strictly abstracts all DAO operations from the UI/ViewModel layer.
 */
class KnowledgeRepository(private val knowledgeDao: KnowledgeDao) {

    val allDocuments: Flow<List<KnowledgeDocumentEntity>> = knowledgeDao.observeAllDocuments()
    val allDocumentsWithDetails: Flow<List<KnowledgeDocumentWithDetails>> =
        knowledgeDao.observeAllDocumentsWithDetails()
    val recentDocuments: Flow<List<KnowledgeDocumentEntity>> = knowledgeDao.observeRecentDocuments(5)
    val allNotes: Flow<List<KnowledgeNoteEntity>> = knowledgeDao.observeAllNotes()
    val allNotesWithTags: Flow<List<KnowledgeNoteWithTags>> = knowledgeDao.observeAllNotesWithTags()
    val allTags: Flow<List<KnowledgeTagEntity>> = knowledgeDao.observeAllTags()

    fun observeDocumentWithDetails(documentId: Long): Flow<KnowledgeDocumentWithDetails?> =
        knowledgeDao.observeDocumentWithDetailsById(documentId)

    fun searchDocuments(query: String): Flow<List<KnowledgeDocumentEntity>> =
        knowledgeDao.searchDocuments(query)

    fun searchNotes(query: String): Flow<List<KnowledgeNoteEntity>> =
        knowledgeDao.searchNotes(query)

    suspend fun createOrUpdateDocument(
        title: String,
        fileName: String,
        format: DocumentFormat,
        folderName: String,
        collectionName: String,
        contentText: String,
        summary: String = "",
        linkedCharacterIdsCsv: String = "",
        linkedLocationIdsCsv: String = "",
        tagIds: List<Long> = emptyList(),
        existingId: Long = 0L
    ): Long {
        val doc = KnowledgeDocumentEntity(
            id = existingId,
            title = title.trim(),
            fileName = fileName.trim(),
            format = format.name,
            folderName = folderName.trim().ifEmpty { "Общие источники" },
            collectionName = collectionName.trim().ifEmpty { "Основной канон" },
            contentText = contentText,
            summary = summary.ifEmpty { contentText.take(180).replace("\n", " ") },
            fileSizeBytes = contentText.toByteArray().size.toLong(),
            linkedCharacterIdsCsv = linkedCharacterIdsCsv,
            linkedLocationIdsCsv = linkedLocationIdsCsv,
            isAllowedInAiContext = true,
            updatedAt = System.currentTimeMillis(),
            lastOpenedAt = System.currentTimeMillis()
        )
        val docId = knowledgeDao.insertDocument(doc)

        // Rebuild RAG chunks
        knowledgeDao.deleteChunksForDocument(docId)
        val chunks = DocumentAndXlsxEngine.chunkDocumentText(docId, contentText)
        if (chunks.isNotEmpty()) {
            knowledgeDao.insertChunks(chunks)
        }

        // Link tags
        if (tagIds.isNotEmpty()) {
            knowledgeDao.clearTagsForDocument(docId)
            tagIds.forEach { tagId ->
                knowledgeDao.insertDocumentTagCrossRef(DocumentTagCrossRef(documentId = docId, tagId = tagId))
            }
        }
        return docId
    }

    suspend fun importDocumentFromUri(
        context: Context,
        uri: Uri,
        fileName: String,
        folderName: String,
        collectionName: String
    ): Long {
        val (format, extractedText) = DocumentAndXlsxEngine.extractTextFromUri(context, uri, fileName)
        val cleanTitle = fileName.substringBeforeLast('.').ifEmpty { fileName }
        return createOrUpdateDocument(
            title = cleanTitle,
            fileName = fileName,
            format = format,
            folderName = folderName,
            collectionName = collectionName,
            contentText = extractedText
        )
    }

    suspend fun deleteDocument(documentId: Long) {
        knowledgeDao.deleteDocumentById(documentId)
    }

    suspend fun markDocumentOpened(documentId: Long) {
        knowledgeDao.markDocumentOpened(documentId)
    }

    suspend fun toggleDocumentAiContext(documentId: Long, allowed: Boolean) {
        knowledgeDao.setDocumentAiContextAllowed(documentId, allowed)
    }

    // --- Notes ---
    suspend fun insertNote(
        title: String,
        content: String,
        documentId: Long? = null,
        quotedExcerpt: String = "",
        linkedCharacterId: Long? = null,
        linkedLocationId: Long? = null,
        isPinned: Boolean = false,
        tagIds: List<Long> = emptyList()
    ): Long {
        val noteId = knowledgeDao.insertNote(
            KnowledgeNoteEntity(
                documentId = documentId,
                title = title.trim(),
                content = content.trim(),
                quotedExcerpt = quotedExcerpt.trim(),
                linkedCharacterId = linkedCharacterId,
                linkedLocationId = linkedLocationId,
                isPinned = isPinned
            )
        )
        tagIds.forEach { tagId ->
            knowledgeDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = noteId, tagId = tagId))
        }
        return noteId
    }

    suspend fun deleteNote(noteId: Long) {
        knowledgeDao.deleteNoteById(noteId)
    }

    // --- Tags ---
    suspend fun createTag(name: String, colorHex: String = "#E11D48", category: String = "Общее"): Long {
        val cleanName = name.trim()
        val existing = knowledgeDao.getTagByName(cleanName)
        if (existing != null) return existing.id
        return knowledgeDao.insertTag(
            KnowledgeTagEntity(name = cleanName, colorHex = colorHex, category = category)
        )
    }

    suspend fun deleteTag(tagId: Long) {
        knowledgeDao.deleteTagById(tagId)
    }

    suspend fun attachTagToDocument(documentId: Long, tagId: Long) {
        knowledgeDao.insertDocumentTagCrossRef(DocumentTagCrossRef(documentId = documentId, tagId = tagId))
    }

    suspend fun removeTagFromDocument(documentId: Long, tagId: Long) {
        knowledgeDao.removeTagFromDocument(documentId = documentId, tagId = tagId)
    }

    /**
     * Performs RAG retrieval over user-authorized documents.
     * Scores chunks by keyword overlap and returns only genuinely matching fragments
     * with exact document and section attribution.
     */
    suspend fun retrieveRelevantChunksForQuery(
        query: String,
        allowedDocumentIds: Set<Long>? = null,
        maxChunks: Int = 5
    ): List<CitedSourceSnippet> {
        val enabledDocs = knowledgeDao.getAiEnabledDocuments()
            .filter { allowedDocumentIds == null || allowedDocumentIds.contains(it.id) }
        if (enabledDocs.isEmpty()) return emptyList()

        val docMap = enabledDocs.associateBy { it.id }
        val allChunks = knowledgeDao.getChunksForDocuments(enabledDocs.map { it.id })

        val queryTokens = query.lowercase()
            .split(Regex("""[^a-zа-яё0-9]+"""))
            .filter { it.length >= 3 }
            .distinct()

        if (queryTokens.isEmpty()) {
            return allChunks.take(maxChunks).mapNotNull { chunk ->
                val doc = docMap[chunk.documentId] ?: return@mapNotNull null
                CitedSourceSnippet(
                    documentId = doc.id,
                    documentTitle = doc.title,
                    folderName = doc.folderName,
                    sectionTitle = chunk.sectionTitle,
                    excerpt = chunk.chunkText,
                    relevanceScore = 1
                )
            }
        }

        val scored = allChunks.mapNotNull { chunk ->
            val doc = docMap[chunk.documentId] ?: return@mapNotNull null
            val haystack = "${doc.title} ${chunk.sectionTitle} ${chunk.chunkText}".lowercase()
            val score = queryTokens.sumOf { token ->
                when {
                    doc.title.lowercase().contains(token) -> 3
                    chunk.sectionTitle.lowercase().contains(token) -> 2
                    haystack.contains(token) -> 1
                    else -> 0
                }
            }
            if (score > 0) {
                CitedSourceSnippet(
                    documentId = doc.id,
                    documentTitle = doc.title,
                    folderName = doc.folderName,
                    sectionTitle = chunk.sectionTitle,
                    excerpt = chunk.chunkText,
                    relevanceScore = score
                )
            } else {
                null
            }
        }.sortedByDescending { it.relevanceScore }

        return scored.take(maxChunks)
    }
}
