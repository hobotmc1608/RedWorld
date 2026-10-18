package com.example.data.repository

import com.example.data.local.CanonStatus
import com.example.data.local.CharacterEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.DeclaredCharacterRelation
import com.example.data.local.GeneratedImageEntity
import com.example.data.local.LocationEntity
import com.example.data.local.NotebookLmNoteItem
import com.example.data.local.NotebookLmNotebookEntity
import com.example.data.local.PlotlineEntity
import com.example.data.local.RedWorldDao
import com.example.data.local.RelationshipEntity
import com.example.data.local.VersionSnapshotEntity
import com.example.data.local.WorldEventEntity
import com.example.data.local.WorldItemEntity
import com.example.data.local.WorldProjectEntity
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

/**
 * Portable project bundle for Exporting, Importing, and Backing up a single project
 * or workspace. Secret API keys are NEVER included in project exports.
 */
@Serializable
data class ProjectExportBundle(
    val formatVersion: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val appIdentifier: String = "World AI Studio",
    val project: WorldProjectEntity,
    val characters: List<CharacterEntity> = emptyList(),
    val locations: List<LocationEntity> = emptyList(),
    val worldRules: List<WorldRuleEntity> = emptyList(),
    val plotlines: List<PlotlineEntity> = emptyList(),
    val events: List<WorldEventEntity> = emptyList(),
    val items: List<WorldItemEntity> = emptyList(),
    val relationships: List<RelationshipEntity> = emptyList(),
    val generatedImages: List<GeneratedImageEntity> = emptyList()
)

class RedWorldRepository(private val dao: RedWorldDao) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    val projects: Flow<List<WorldProjectEntity>> = dao.observeProjects()
    val notebookLmNotebooks: Flow<List<NotebookLmNotebookEntity>> = dao.observeNotebookLmNotebooks()
    val characters: Flow<List<CharacterEntity>> = dao.observeCharacters()
    val locations: Flow<List<LocationEntity>> = dao.observeLocations()
    val worldRules: Flow<List<WorldRuleEntity>> = dao.observeWorldRules()
    val plotlines: Flow<List<PlotlineEntity>> = dao.observePlotlines()
    val events: Flow<List<WorldEventEntity>> = dao.observeEvents()
    val items: Flow<List<WorldItemEntity>> = dao.observeItems()
    val relationships: Flow<List<RelationshipEntity>> = dao.observeRelationships()
    val versionSnapshots: Flow<List<VersionSnapshotEntity>> = dao.observeVersionSnapshots()
    val chatSessions: Flow<List<ChatSessionEntity>> = dao.observeChatSessions()
    val generatedImages: Flow<List<GeneratedImageEntity>> = dao.observeGeneratedImages()

    @Volatile
    var lastAutoLinkedSummary: List<String> = emptyList()
        private set

    fun observeMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> =
        dao.observeMessagesForSession(sessionId)

    fun searchChatMessages(query: String): Flow<List<ChatMessageEntity>> =
        dao.searchAllChatMessages(query)

    // --- Projects (Multi-Project Workspace) ---
    suspend fun saveProject(project: WorldProjectEntity): Long =
        dao.insertProject(project.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteProject(project: WorldProjectEntity) {
        dao.deleteCharactersByProject(project.id)
        dao.deleteLocationsByProject(project.id)
        dao.deleteRulesByProject(project.id)
        dao.deletePlotlinesByProject(project.id)
        dao.deleteEventsByProject(project.id)
        dao.deleteItemsByProject(project.id)
        dao.deleteRelationshipsByProject(project.id)
        dao.deleteGeneratedImagesByProject(project.id)
        dao.deleteProject(project)
    }

    suspend fun createProjectWithTemplate(
        project: WorldProjectEntity,
        templateType: String
    ): Long {
        val projectId = dao.insertProject(
            project.copy(
                templateType = templateType,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        when (templateType) {
            "DARK_FANTASY" -> {
                dao.insertLocation(
                    LocationEntity(
                        projectId = projectId,
                        name = "Цитадель Пепла",
                        category = "Крепость",
                        description = "Древняя базальтовая твердыня на границе туманных пустошей.",
                        atmosphere = "Холодный камень, факелы и кодексы ордена."
                    )
                )
                dao.insertWorldRule(
                    WorldRuleEntity(
                        projectId = projectId,
                        title = "Закон клятвенной печати",
                        category = "Магия и законы",
                        content = "Нарушение скреплённой печатью клятвы оставляет видимую метку и лишает доступа в святилища.",
                        scope = "Все ордена и правители",
                        limitations = "Действует только при добровольном произнесении клятвы."
                    )
                )
                dao.insertPlotline(
                    PlotlineEntity(
                        projectId = projectId,
                        title = "Пробуждение древнего архива",
                        description = "Поиск утраченных свитков первого круга хранителей.",
                        tasks = "1. Собрать совет.\n2. Открыть врата нижнего зала.",
                        status = "Активна"
                    )
                )
            }
            "SCI_FI" -> {
                dao.insertLocation(
                    LocationEntity(
                        projectId = projectId,
                        name = "Орбитальная станция «Гелиос-9»",
                        category = "Научный модуль",
                        description = "Автономная исследовательская станция на высокой орбите с лабораториями ИИ и куполом связи.",
                        atmosphere = "Стерильный металл, голографические терминалы и гул реактора."
                    )
                )
                dao.insertWorldRule(
                    WorldRuleEntity(
                        projectId = projectId,
                        title = "Протокол карантина ИИ и навигации",
                        category = "Технологические законы",
                        content = "Дальние прыжки возможны только после синхронизации бортового журнала со станцией.",
                        scope = "Все корабли и экипажи сектора",
                        limitations = "Вне зоны ретрансляторов связь задерживается на 14 часов."
                    )
                )
                dao.insertPlotline(
                    PlotlineEntity(
                        projectId = projectId,
                        title = "Сигнал из глубокого сектора",
                        description = "Расшифровка аномальной передачи с пропавшего исследовательского судна.",
                        tasks = "1. Собрать экипаж.\n2. Проверить бортовой блок памяти.",
                        status = "Активна"
                    )
                )
            }
            "DETECTIVE_NOIR" -> {
                dao.insertLocation(
                    LocationEntity(
                        projectId = projectId,
                        name = "Архивное бюро на набережной",
                        category = "Кабинет расследований",
                        description = "Кабинет на верхнем этаже старого здания с картотекой нераскрытых дел.",
                        atmosphere = "Дождь за окном, настольная лампа и запах старой бумаги."
                    )
                )
                dao.insertWorldRule(
                    WorldRuleEntity(
                        projectId = projectId,
                        title = "Кодекс частного расследования",
                        category = "Правила сцен",
                        content = "Каждая улика должна быть подтверждена минимум двумя независимыми источниками.",
                        scope = "Все расследования и допросы",
                        limitations = "Слухи без вещественных доказательств остаются в статусе предположений."
                    )
                )
                dao.insertPlotline(
                    PlotlineEntity(
                        projectId = projectId,
                        title = "Дело о пропавшем ключе",
                        description = "Расследование цепочки таинственных встреч в ночном городе.",
                        tasks = "1. Опросить свидетелей.\n2. Сопоставить время событий.",
                        status = "Активна"
                    )
                )
            }
            else -> {
                // EMPTY template: clean isolated project with no preset cards
            }
        }
        return projectId
    }

    suspend fun exportProjectToJson(projectId: Long): String {
        val project = dao.getProjectById(projectId)
            ?: WorldProjectEntity(id = projectId, name = "Экспортированный проект")
        val bundle = ProjectExportBundle(
            formatVersion = 2,
            exportedAt = System.currentTimeMillis(),
            project = project,
            characters = dao.getAllCharacters().filter { it.projectId == projectId },
            locations = dao.getAllLocations().filter { it.projectId == projectId },
            worldRules = dao.getAllWorldRules().filter { it.projectId == projectId },
            plotlines = dao.getAllPlotlines().filter { it.projectId == projectId },
            events = dao.getAllEvents().filter { it.projectId == projectId },
            items = dao.getAllItems().filter { it.projectId == projectId },
            relationships = dao.getAllRelationships().filter { it.projectId == projectId },
            generatedImages = dao.getAllGeneratedImages().filter { it.projectId == projectId }
        )
        return json.encodeToString(bundle)
    }

    suspend fun importProjectFromJson(rawJson: String, overwriteExistingProjectId: Long? = null): Result<Long> {
        return try {
            val bundle = json.decodeFromString<ProjectExportBundle>(rawJson)
            if (bundle.project.name.isBlank()) {
                return Result.failure(IllegalArgumentException("В файле проекта отсутствует название проекта."))
            }
            val targetProjectId = if (overwriteExistingProjectId != null && overwriteExistingProjectId > 0L) {
                dao.deleteCharactersByProject(overwriteExistingProjectId)
                dao.deleteLocationsByProject(overwriteExistingProjectId)
                dao.deleteRulesByProject(overwriteExistingProjectId)
                dao.deletePlotlinesByProject(overwriteExistingProjectId)
                dao.deleteEventsByProject(overwriteExistingProjectId)
                dao.deleteItemsByProject(overwriteExistingProjectId)
                dao.deleteRelationshipsByProject(overwriteExistingProjectId)
                dao.deleteGeneratedImagesByProject(overwriteExistingProjectId)
                dao.insertProject(
                    bundle.project.copy(
                        id = overwriteExistingProjectId,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                overwriteExistingProjectId
            } else {
                dao.insertProject(
                    bundle.project.copy(
                        id = 0L,
                        name = "${bundle.project.name} (Импорт)",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            bundle.locations.forEach { dao.insertLocation(it.copy(id = 0L, projectId = targetProjectId)) }
            bundle.characters.forEach { dao.insertCharacter(it.copy(id = 0L, projectId = targetProjectId)) }
            bundle.worldRules.forEach { dao.insertWorldRule(it.copy(id = 0L, projectId = targetProjectId)) }
            bundle.plotlines.forEach { dao.insertPlotline(it.copy(id = 0L, projectId = targetProjectId)) }
            bundle.events.forEach { dao.insertEvent(it.copy(id = 0L, projectId = targetProjectId)) }
            bundle.items.forEach { dao.insertItem(it.copy(id = 0L, projectId = targetProjectId)) }
            bundle.relationships.forEach { dao.insertRelationship(it.copy(id = 0L, projectId = targetProjectId)) }
            bundle.generatedImages.forEach { dao.insertGeneratedImage(it.copy(id = 0L, projectId = targetProjectId)) }
            Result.success(targetProjectId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Google NotebookLM Synced Notebooks ---
    suspend fun saveNotebookLmNotebook(notebook: NotebookLmNotebookEntity): Long =
        dao.insertNotebookLmNotebook(notebook.copy(lastSyncedAt = System.currentTimeMillis()))

    suspend fun getAllNotebookLmNotebooks(): List<NotebookLmNotebookEntity> =
        dao.getAllNotebookLmNotebooks()

    suspend fun markNotebookLmNoteIntegrated(
        notebookId: Long,
        noteId: String,
        targetWorldSummary: String
    ) {
        val notebook = dao.getNotebookLmNotebookById(notebookId) ?: return
        val notes = try {
            json.decodeFromString<List<NotebookLmNoteItem>>(notebook.notesJson)
        } catch (_: Exception) {
            emptyList()
        }
        val updatedNotes = notes.map { item ->
            if (item.id == noteId) {
                item.copy(
                    isIntegratedIntoWorld = true,
                    integratedWorldSummary = targetWorldSummary
                )
            } else item
        }
        dao.updateNotebookLmNotebook(
            notebook.copy(
                notesJson = json.encodeToString(updatedNotes),
                lastSyncedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addNoteToNotebookLmNotebook(
        notebookId: Long,
        newNote: NotebookLmNoteItem
    ) {
        val notebook = dao.getNotebookLmNotebookById(notebookId) ?: return
        val notes = try {
            json.decodeFromString<List<NotebookLmNoteItem>>(notebook.notesJson)
        } catch (_: Exception) {
            emptyList()
        }
        val updatedNotes = listOf(newNote) + notes
        dao.updateNotebookLmNotebook(
            notebook.copy(
                notesJson = json.encodeToString(updatedNotes),
                lastSyncedAt = System.currentTimeMillis()
            )
        )
    }

    // --- Characters, Auto-Linking Relationships & Version Snapshots ---
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

        // Automatically resolve and materialize relationships in both directions!
        val autoLinked = autoLinkCharacterRelationships(finalChar)
        lastAutoLinkedSummary = autoLinked

        val summaryWithLinks = if (autoLinked.isNotEmpty()) {
            "$changeSummary (Автоматически подтянуто связей: ${autoLinked.size})"
        } else {
            changeSummary
        }

        dao.insertVersionSnapshot(
            VersionSnapshotEntity(
                projectId = finalChar.projectId,
                entityType = "CHARACTER",
                entityId = savedId,
                entityTitle = finalChar.name,
                versionNumber = nextVersion,
                changeSummary = summaryWithLinks,
                payloadJson = json.encodeToString(finalChar),
                author = author
            )
        )
        return savedId
    }

    /**
     * Automatically links:
     * 1) Outgoing relationships declared in `savedChar.declaredRelationshipsJson` (or mentioned in `relationshipsSummary`)
     *    whose target characters or locations already exist in the project.
     * 2) Incoming pending relationships declared in OTHER already-existing characters' cards
     *    that reference `savedChar.name` or any of `savedChar.aliases`.
     */
    private suspend fun autoLinkCharacterRelationships(savedChar: CharacterEntity): List<String> {
        val createdLinks = mutableListOf<String>()
        val allCharacters = dao.getAllCharacters().filter { it.projectId == savedChar.projectId }
        val allLocations = dao.getAllLocations().filter { it.projectId == savedChar.projectId }
        val existingRels = dao.getAllRelationships().toMutableList()

        fun matchesCharacter(targetNameRaw: String, candidate: CharacterEntity): Boolean {
            val t = targetNameRaw.trim().lowercase()
            if (t.length < 2) return false
            val cName = candidate.name.trim().lowercase()
            if (cName == t || cName.contains(t) || t.contains(cName)) return true
            val aliases = candidate.aliases.split(",", ";")
                .map { it.trim().lowercase() }
                .filter { it.length >= 2 }
            return aliases.any { it == t || it.contains(t) || t.contains(it) }
        }

        fun matchesLocation(targetNameRaw: String, candidate: LocationEntity): Boolean {
            val t = targetNameRaw.trim().lowercase()
            if (t.length < 2) return false
            val lName = candidate.name.trim().lowercase()
            return lName == t || lName.contains(t) || t.contains(lName)
        }

        fun hasEdge(typeA: String, idA: Long, typeB: String, idB: Long): Boolean {
            return existingRels.any { r ->
                (r.sourceType == typeA && r.sourceId == idA && r.targetType == typeB && r.targetId == idB) ||
                    (r.sourceType == typeB && r.sourceId == idB && r.targetType == typeA && r.targetId == idA)
            }
        }

        fun parseDeclaredList(rawJson: String): List<DeclaredCharacterRelation> {
            if (rawJson.isBlank() || rawJson == "[]") return emptyList()
            return try {
                json.decodeFromString<List<DeclaredCharacterRelation>>(rawJson)
            } catch (_: Exception) {
                emptyList()
            }
        }

        // 1. Outgoing declared relations from savedChar
        val outgoingDeclared = parseDeclaredList(savedChar.declaredRelationshipsJson).toMutableList()
        // Also scan relationshipsSummary text for mentions of existing characters/locations not yet in outgoingDeclared
        if (savedChar.relationshipsSummary.isNotBlank()) {
            allCharacters.filter { it.id != savedChar.id }.forEach { other ->
                val firstToken = other.name.substringBefore(" ").trim()
                if ((savedChar.relationshipsSummary.contains(other.name, ignoreCase = true) ||
                        (firstToken.length >= 4 && savedChar.relationshipsSummary.contains(firstToken, ignoreCase = true))) &&
                    outgoingDeclared.none { matchesCharacter(it.targetName, other) }
                ) {
                    outgoingDeclared.add(
                        DeclaredCharacterRelation(
                            targetName = other.name,
                            targetType = "CHARACTER",
                            relationType = "Знаком с",
                            description = "Упомянуто в разделе отношений карточки «${savedChar.name}»"
                        )
                    )
                }
            }
        }

        for (decl in outgoingDeclared) {
            // Check character match
            val targetChar = allCharacters.firstOrNull { it.id != savedChar.id && matchesCharacter(decl.targetName, it) }
            if (targetChar != null && !hasEdge("CHARACTER", savedChar.id, "CHARACTER", targetChar.id)) {
                val newRel = RelationshipEntity(
                    projectId = savedChar.projectId,
                    sourceType = "CHARACTER",
                    sourceId = savedChar.id,
                    sourceLabel = savedChar.name,
                    targetType = "CHARACTER",
                    targetId = targetChar.id,
                    targetLabel = targetChar.name,
                    relationType = decl.relationType.ifBlank { "Знаком с" },
                    description = decl.description.ifBlank { "Автоматически подтянуто из карточки «${savedChar.name}»" },
                    canonStatus = savedChar.canonStatus,
                    sourceX = 240f + (savedChar.id * 90f) % 450f,
                    sourceY = 220f + (savedChar.id * 70f) % 360f,
                    targetX = 240f + (targetChar.id * 90f) % 450f,
                    targetY = 220f + (targetChar.id * 70f) % 360f
                )
                val relId = dao.insertRelationship(newRel)
                existingRels.add(newRel.copy(id = relId))
                createdLinks.add("${savedChar.name} ↔ ${targetChar.name} (${newRel.relationType})")
                continue
            }

            // Check location match
            val targetLoc = allLocations.firstOrNull { matchesLocation(decl.targetName, it) }
            if (targetLoc != null && !hasEdge("CHARACTER", savedChar.id, "LOCATION", targetLoc.id)) {
                val newRel = RelationshipEntity(
                    projectId = savedChar.projectId,
                    sourceType = "CHARACTER",
                    sourceId = savedChar.id,
                    sourceLabel = savedChar.name,
                    targetType = "LOCATION",
                    targetId = targetLoc.id,
                    targetLabel = targetLoc.name,
                    relationType = decl.relationType.ifBlank { "Связан с локацией" },
                    description = decl.description.ifBlank { "Автоматически подтянуто из карточки «${savedChar.name}»" },
                    canonStatus = savedChar.canonStatus,
                    sourceX = 260f + (savedChar.id * 80f) % 420f,
                    sourceY = 240f,
                    targetX = 540f,
                    targetY = 380f
                )
                val relId = dao.insertRelationship(newRel)
                existingRels.add(newRel.copy(id = relId))
                createdLinks.add("${savedChar.name} → ${targetLoc.name} (${newRel.relationType})")
            }
        }

        // 2. Incoming pending relations from OTHER existing characters pointing to savedChar
        for (otherChar in allCharacters) {
            if (otherChar.id == savedChar.id) continue
            val otherDeclared = parseDeclaredList(otherChar.declaredRelationshipsJson).toMutableList()
            if (otherChar.relationshipsSummary.contains(savedChar.name, ignoreCase = true) &&
                otherDeclared.none { matchesCharacter(it.targetName, savedChar) }
            ) {
                otherDeclared.add(
                    DeclaredCharacterRelation(
                        targetName = savedChar.name,
                        targetType = "CHARACTER",
                        relationType = "Знаком с",
                        description = "Подтянуто из описания связей карточки «${otherChar.name}»"
                    )
                )
            }

            for (decl in otherDeclared) {
                if (matchesCharacter(decl.targetName, savedChar) &&
                    !hasEdge("CHARACTER", otherChar.id, "CHARACTER", savedChar.id)
                ) {
                    val newRel = RelationshipEntity(
                        projectId = savedChar.projectId,
                        sourceType = "CHARACTER",
                        sourceId = otherChar.id,
                        sourceLabel = otherChar.name,
                        targetType = "CHARACTER",
                        targetId = savedChar.id,
                        targetLabel = savedChar.name,
                        relationType = decl.relationType.ifBlank { "Знаком с" },
                        description = decl.description.ifBlank { "Автоматически подтянуто при добавлении «${savedChar.name}» из досье «${otherChar.name}»" },
                        canonStatus = otherChar.canonStatus,
                        sourceX = 240f + (otherChar.id * 90f) % 450f,
                        sourceY = 220f + (otherChar.id * 70f) % 360f,
                        targetX = 320f + (savedChar.id * 90f) % 450f,
                        targetY = 340f + (savedChar.id * 70f) % 360f
                    )
                    val relId = dao.insertRelationship(newRel)
                    existingRels.add(newRel.copy(id = relId))
                    createdLinks.add("${otherChar.name} ↔ ${savedChar.name} (${newRel.relationType})")
                }
            }
        }

        return createdLinks
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
        newHealthStatus: String = "",
        logAsEvent: Boolean = true
    ) {
        val existing = dao.getCharacterById(characterId) ?: return
        val resolvedHealth = newHealthStatus.ifBlank { existing.currentHealthStatus }
        val updated = existing.copy(
            currentLocationName = newLocation,
            currentMoodAndCondition = newMoodAndCondition,
            currentHealthStatus = resolvedHealth,
            currentOutfit = newOutfit,
            currentActiveGoal = newActiveGoal,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateCharacter(updated)
        if (logAsEvent) {
            dao.insertEvent(
                WorldEventEntity(
                    projectId = existing.projectId,
                    title = "Смена состояния: ${existing.name} → $newLocation",
                    eventType = "Изменение состояния",
                    timelineDate = "Текущая сцена",
                    chronologyOrder = 99,
                    locationName = newLocation,
                    participantNamesCsv = existing.name,
                    description = "Текущее состояние обновлено: $newMoodAndCondition (Здоровье: $resolvedHealth). Активная задача: $newActiveGoal",
                    consequences = "Постоянные биографические данные сохранены без изменений.",
                    canonStatus = existing.canonStatus
                )
            )
        }
    }

    fun exportCharacterToJson(character: CharacterEntity): String =
        json.encodeToString(character)

    fun exportCharacterToMarkdown(character: CharacterEntity): String = buildString {
        appendLine("# Досье персонажа: ${character.name}")
        if (character.fullName.isNotBlank()) appendLine("- **Полное имя:** ${character.fullName}")
        if (character.aliases.isNotBlank()) appendLine("- **Псевдонимы / Титулы:** ${character.aliases}")
        appendLine("- **Роль в сюжете:** ${character.storyRole}")
        appendLine("- **Статус персонажа:** ${character.characterStatus}")
        if (character.factionOrGroup.isNotBlank()) appendLine("- **Фракция / Организация:** ${character.factionOrGroup}")
        if (character.tagsCsv.isNotBlank()) appendLine("- **Теги:** ${character.tagsCsv}")
        if (character.gender.isNotBlank()) appendLine("- **Пол:** ${character.gender}")
        if (character.speciesOrOrigin.isNotBlank()) appendLine("- **Происхождение / Вид:** ${character.speciesOrOrigin}")
        if (character.birthDate.isNotBlank() || character.age.isNotBlank()) {
            appendLine("- **Дата рождения и возраст:** ${character.birthDate} (${character.age})")
        }
        if (character.birthPlace.isNotBlank()) appendLine("- **Место рождения:** ${character.birthPlace}")
        if (character.occupation.isNotBlank()) appendLine("- **Род занятий / Должность:** ${character.occupation}")
        appendLine()
        appendLine("## 1. Краткое описание и биография")
        if (character.shortDescription.isNotBlank()) appendLine(character.shortDescription)
        if (character.detailedDescription.isNotBlank()) {
            appendLine()
            appendLine(character.detailedDescription)
        }
        appendLine()
        appendLine("## 2. Внешность и визуальный образ")
        if (character.appearance.isNotBlank()) appendLine("- **Общее описание:** ${character.appearance}")
        if (character.physique.isNotBlank()) appendLine("- **Телосложение:** ${character.physique}")
        if (character.hair.isNotBlank()) appendLine("- **Волосы:** ${character.hair}")
        if (character.eyes.isNotBlank()) appendLine("- **Глаза:** ${character.eyes}")
        if (character.voiceDescription.isNotBlank()) appendLine("- **Голос:** ${character.voiceDescription}")
        if (character.defaultClothing.isNotBlank()) appendLine("- **Повседневная одежда и стиль:** ${character.defaultClothing}")
        if (character.distinguishingMarks.isNotBlank()) appendLine("- **Особые приметы:** ${character.distinguishingMarks}")
        if (character.visualPassportPrompt.isNotBlank()) appendLine("- **Паспорт внешности (Image Studio):** `${character.visualPassportPrompt}`")
        appendLine()
        appendLine("## 3. Психология, речь и поведение")
        if (character.personality.isNotBlank()) appendLine("- **Характер:** ${character.personality}")
        if (character.archetype.isNotBlank()) appendLine("- **Архетип:** ${character.archetype}")
        if (character.habits.isNotBlank()) appendLine("- **Привычки и жесты:** ${character.habits}")
        if (character.speechStyle.isNotBlank()) appendLine("- **Манера речи:** ${character.speechStyle}")
        if (character.catchphrases.isNotBlank()) appendLine("- **Характерные фразы:** ${character.catchphrases}")
        if (character.behavioralTraits.isNotBlank()) appendLine("- **Поведенческие особенности:** ${character.behavioralTraits}")
        appendLine()
        appendLine("## 4. Способности, инвентарь и предпочтения")
        if (character.skills.isNotBlank()) appendLine("- **Навыки и умения:** ${character.skills}")
        if (character.strengths.isNotBlank()) appendLine("- **Сильные стороны:** ${character.strengths}")
        if (character.weaknesses.isNotBlank()) appendLine("- **Слабые стороны:** ${character.weaknesses}")
        if (character.inventorySummary.isNotBlank()) appendLine("- **Личные предметы и инвентарь:** ${character.inventorySummary}")
        if (character.interests.isNotBlank()) appendLine("- **Интересы и хобби:** ${character.interests}")
        if (character.preferences.isNotBlank()) appendLine("- **Предпочтения:** ${character.preferences}")
        if (character.dislikes.isNotBlank()) appendLine("- **Антипатии / Не любит:** ${character.dislikes}")
        appendLine()
        appendLine("## 5. Цели, мотивация, знания и тайны")
        if (character.goals.isNotBlank()) appendLine("- **Цели:** ${character.goals}")
        if (character.motivation.isNotBlank()) appendLine("- **Мотивация:** ${character.motivation}")
        if (character.internalConflict.isNotBlank()) appendLine("- **Внутренний конфликт:** ${character.internalConflict}")
        if (character.fearsAndLimitations.isNotBlank()) appendLine("- **Страхи и ограничения:** ${character.fearsAndLimitations}")
        if (character.knownFacts.isNotBlank()) appendLine("- **Известные персонажу факты:** ${character.knownFacts}")
        if (character.personalSecrets.isNotBlank()) appendLine("- **Личные тайны:** ${character.personalSecrets}")
        if (character.unknownFactsRestriction.isNotBlank()) appendLine("- **Чего персонаж не знает:** ${character.unknownFactsRestriction}")
        appendLine()
        appendLine("## 6. Текущее состояние (Слой 2)")
        appendLine("- **Текущая локация:** ${character.currentLocationName}")
        appendLine("- **Эмоциональное состояние:** ${character.currentMoodAndCondition}")
        appendLine("- **Здоровье:** ${character.currentHealthStatus}")
        if (character.currentOutfit.isNotBlank()) appendLine("- **Одежда в сцене:** ${character.currentOutfit}")
        if (character.currentActiveGoal.isNotBlank()) appendLine("- **Активная задача:** ${character.currentActiveGoal}")
        appendLine()
        appendLine("## 7. Отношения и заметки")
        if (character.relationshipsSummary.isNotBlank()) appendLine("- **Связи:** ${character.relationshipsSummary}")
        if (character.additionalNotes.isNotBlank()) appendLine("- **Заметки:** ${character.additionalNotes}")
    }

    suspend fun importCharacterCardFromJson(rawJson: String, targetProjectId: Long): Result<Long> {
        return try {
            val parsed = json.decodeFromString<CharacterEntity>(rawJson)
            if (parsed.name.isBlank()) {
                return Result.failure(IllegalArgumentException("В JSON-карточке отсутствует имя персонажа."))
            }
            val savedId = saveCharacterWithSnapshot(
                character = parsed.copy(id = 0L, projectId = targetProjectId),
                changeSummary = "Импорт карточки персонажа «${parsed.name}» из JSON",
                author = "Импорт карточки"
            )
            Result.success(savedId)
        } catch (e: Exception) {
            Result.failure(e)
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
                projectId = rule.projectId,
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

    // --- Plotlines (Сюжет) ---
    suspend fun savePlotline(plotline: PlotlineEntity): Long =
        dao.insertPlotline(plotline.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deletePlotline(plotline: PlotlineEntity) = dao.deletePlotline(plotline)

    // --- Events (История событий) ---
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
        assistantMode: String = "CONSULTATION",
        projectId: Long = 1L
    ): Long {
        return dao.insertChatSession(
            ChatSessionEntity(
                projectId = projectId,
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
                    projectId = image.projectId,
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
        diffsJson: String,
        projectId: Long = 1L
    ) {
        dao.insertVersionSnapshot(
            VersionSnapshotEntity(
                projectId = projectId,
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
