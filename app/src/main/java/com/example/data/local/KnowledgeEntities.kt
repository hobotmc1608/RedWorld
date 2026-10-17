package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

/**
 * Supported document formats in RedWorld Knowledge module.
 */
@Serializable
enum class DocumentFormat(val label: String, val extension: String) {
    PDF("PDF Документ", "pdf"),
    TXT("Текстовый файл", "txt"),
    MARKDOWN("Markdown", "md"),
    DOCX("Word (DOCX)", "docx"),
    XLSX("Excel (XLSX)", "xlsx")
}

/**
 * Represents an imported or authored document in RedWorld Knowledge.
 */
@Serializable
@Entity(
    tableName = "knowledge_documents",
    indices = [
        Index(value = ["folderName"]),
        Index(value = ["collectionName"]),
        Index(value = ["title"])
    ]
)
data class KnowledgeDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val fileName: String,
    val format: String = DocumentFormat.MARKDOWN.name,
    val folderName: String = "Общие источники",
    val collectionName: String = "Основной канон",
    val contentText: String,
    val summary: String = "",
    val localUri: String = "",
    val fileSizeBytes: Long = 0L,
    val linkedCharacterIdsCsv: String = "", // Comma-separated character IDs
    val linkedLocationIdsCsv: String = "",  // Comma-separated location IDs
    val isAllowedInAiContext: Boolean = true,
    val driveFileId: String? = null,
    val versionNumber: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a RAG text chunk extracted from a KnowledgeDocumentEntity
 * for accurate passage retrieval and source citation.
 */
@Serializable
@Entity(
    tableName = "knowledge_document_chunks",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class DocumentChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val documentId: Long,
    val chunkIndex: Int,
    val sectionTitle: String = "",
    val chunkText: String,
    val startOffset: Int = 0,
    val endOffset: Int = 0
)

/**
 * Represents a user note in RedWorld Knowledge, optionally attached to a specific document,
 * character, or location.
 */
@Serializable
@Entity(
    tableName = "knowledge_notes",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class KnowledgeNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val documentId: Long? = null,
    val title: String,
    val content: String,
    val quotedExcerpt: String = "",
    val linkedCharacterId: Long? = null,
    val linkedLocationId: Long? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a reusable tag in RedWorld Knowledge.
 */
@Serializable
@Entity(
    tableName = "knowledge_tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class KnowledgeTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val colorHex: String = "#E11D48",
    val category: String = "Общее",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Many-to-many cross-reference between KnowledgeDocumentEntity and KnowledgeTagEntity.
 */
@Serializable
@Entity(
    tableName = "document_tag_cross_ref",
    primaryKeys = ["documentId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = KnowledgeTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["tagId"])
    ]
)
data class DocumentTagCrossRef(
    val documentId: Long,
    val tagId: Long
)

/**
 * Many-to-many cross-reference between KnowledgeNoteEntity and KnowledgeTagEntity.
 */
@Serializable
@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeNoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = KnowledgeTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["tagId"])
    ]
)
data class NoteTagCrossRef(
    val noteId: Long,
    val tagId: Long
)

/**
 * Relation wrapper returning a document with its associated tags and notes.
 */
data class KnowledgeDocumentWithDetails(
    @Embedded val document: KnowledgeDocumentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = DocumentTagCrossRef::class,
            parentColumn = "documentId",
            entityColumn = "tagId"
        )
    )
    val tags: List<KnowledgeTagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "documentId"
    )
    val notes: List<KnowledgeNoteEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "documentId"
    )
    val chunks: List<DocumentChunkEntity>
)

/**
 * Relation wrapper returning a note with its associated tags.
 */
data class KnowledgeNoteWithTags(
    @Embedded val note: KnowledgeNoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagId"
        )
    )
    val tags: List<KnowledgeTagEntity>
)
