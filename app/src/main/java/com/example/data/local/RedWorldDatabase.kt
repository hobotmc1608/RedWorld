package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        KnowledgeDocumentEntity::class,
        DocumentChunkEntity::class,
        KnowledgeNoteEntity::class,
        KnowledgeTagEntity::class,
        DocumentTagCrossRef::class,
        NoteTagCrossRef::class,
        CharacterEntity::class,
        LocationEntity::class,
        WorldRuleEntity::class,
        WorldEventEntity::class,
        WorldItemEntity::class,
        RelationshipEntity::class,
        VersionSnapshotEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        GeneratedImageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RedWorldDatabase : RoomDatabase() {

    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun redWorldDao(): RedWorldDao

    companion object {
        private const val DATABASE_NAME = "redworld_ai_studio.db"

        @Volatile
        private var INSTANCE: RedWorldDatabase? = null

        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun getInstance(context: Context): RedWorldDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RedWorldDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                applicationScope.launch {
                    seedInitialDataIfNeeded(instance)
                }
                instance
            }
        }

        /**
         * Creates an in-memory database instance for unit/Robolectric tests.
         */
        fun createInMemory(context: Context): RedWorldDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                RedWorldDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }

        suspend fun seedInitialDataIfNeeded(db: RedWorldDatabase) {
            val kDao = db.knowledgeDao()
            val rwDao = db.redWorldDao()

            if (kDao.getDocumentCount() > 0 || rwDao.getCharacterCount() > 0) {
                return
            }

            // 1. Seed Tags
            val tagCanonId = kDao.insertTag(KnowledgeTagEntity(name = "Канон", colorHex = "#10B981", category = "Статус"))
            val tagMansionId = kDao.insertTag(KnowledgeTagEntity(name = "Красный особняк", colorHex = "#E11D48", category = "Локация"))
            val tagCharactersId = kDao.insertTag(KnowledgeTagEntity(name = "Персонажи", colorHex = "#F43F5E", category = "Сущности"))
            val tagRulesId = kDao.insertTag(KnowledgeTagEntity(name = "Правила мира", colorHex = "#A855F7", category = "Механика"))
            val tagGreenStId = kDao.insertTag(KnowledgeTagEntity(name = "Зелёная улица", colorHex = "#22C55E", category = "Локация"))

            // 2. Seed Locations
            val locMansionId = rwDao.insertLocation(
                LocationEntity(
                    name = "Красный особняк",
                    category = "Ключевая резиденция",
                    description = "Центральное место действия проекта RedWorld. Старинный особняк из тёмно-багрового кирпича с закрытым внутренним двором, библиотекой архивов и витражным залом.",
                    atmosphere = "Камерная готика, приглушённый тёплый свет ламп, шорох страниц и строгий внутренний распорядок.",
                    interiorDetails = "Двухуровневая библиотека, каминный зал, крыло жилых комнат, подземный архив.",
                    exteriorDetails = "Кованая ограда, багровый фасад, туманный сад, выход к переулку Зелёной улицы.",
                    accessRules = "Вход по приглашению хранителя особняка или для постоянных резидентов.",
                    connectedLocationNames = "Зелёная улица, Архив",
                    canonStatus = CanonStatus.CANON.name
                )
            )
            val locGreenStId = rwDao.insertLocation(
                LocationEntity(
                    name = "Зелёная улица",
                    category = "Городская артерия",
                    description = "Тихая обсаженная старыми платанами улица, соединяющая Красный особняк с деловым кварталом, Магазином редких вещей и ночным Баром.",
                    atmosphere = "Влажный брусчатый тротуар, неоновые отражения витрин и старые фонари.",
                    interiorDetails = "Арочные проходы и внутренние дворики.",
                    exteriorDetails = "Ряды довоенных фасадов, трамвайная линия, вход в Магазин и Бар.",
                    accessRules = "Открытая городская зона.",
                    connectedLocationNames = "Красный особняк, Магазин, Бар",
                    canonStatus = CanonStatus.CANON.name
                )
            )
            val locShopId = rwDao.insertLocation(
                LocationEntity(
                    name = "Магазин",
                    category = "Торговая точка",
                    description = "Антикварный и технический магазин на Зелёной улице, где персонажи приобретают редкие предметы, носители данных и артефакты.",
                    atmosphere = "Запах старого дерева, озона и чая.",
                    interiorDetails = "Стеклянные витрины, мастерская в задней комнате, сейф редких заказов.",
                    exteriorDetails = "Латунная вывеска и колокольчик над дверью.",
                    accessRules = "Открыт с 10:00 до 21:00; задняя комната — только для доверенных лиц.",
                    connectedLocationNames = "Зелёная улица",
                    canonStatus = CanonStatus.CANON.name
                )
            )
            val locBarId = rwDao.insertLocation(
                LocationEntity(
                    name = "Бар",
                    category = "Социальный узел",
                    description = "Полуподвальный бар «Алый Бархат» на пересечении Зелёной улицы. Место неформальных встреч, обмена слухами и пересечения сюжетных линий.",
                    atmosphere = "Тёмное дерево, джазовый винил, бордовая подсветка стойки.",
                    interiorDetails = "Длинная барная стойка, угловые кабинки со звукоизоляцией.",
                    exteriorDetails = "Спуск по каменным ступеням под козырьком.",
                    accessRules = "Свободный вход вечером, действует правило нейтралитета внутри зала.",
                    connectedLocationNames = "Зелёная улица",
                    canonStatus = CanonStatus.CANON.name
                )
            )

            // 3. Seed Characters
            val charVictorId = rwDao.insertCharacter(
                CharacterEntity(
                    name = "Виктор Корвин",
                    aliases = "Архивариус, Хранитель",
                    birthDate = "14 ноября 1988",
                    age = "37",
                    birthPlace = "Старый город",
                    appearance = "Высокий, собранный мужчина с пронзительным взглядом и аккуратной тёмной бородкой.",
                    physique = "Худощавое, подтянутое",
                    hair = "Тёмно-каштановые с лёгкой сединой на висках",
                    eyes = "Янтарно-карие",
                    defaultClothing = "Тёмно-бордовый жилет, белая рубашка с закатанными рукавами, карманные часы.",
                    distinguishingMarks = "Тонкий шрам на левом запястье, кольцо-печатка Красного особняка.",
                    personality = "Аналитичный, сдержанный, требовательный к точности фактов, но защищающий обитателей особняка.",
                    habits = "Делает пометки перьевой ручкой в кожаном блокноте, сверяет хронологию событий.",
                    speechStyle = "Размеренная, точная, без лишних слов, с лёгкой иронией.",
                    occupation = "Хранитель архива Красного особняка",
                    skills = "Систематизация архивов, криптография, переговоры, реконструкция событий.",
                    interests = "Редкие рукописи, шахматы, часовые механизмы.",
                    preferences = "Крепкий чёрный кофе, тишина в библиотеке после полуночи.",
                    goals = "Сохранить целостность канона RedWorld и предотвратить искажение памяти о событиях.",
                    motivation = "Долг перед основателями Красного особняка.",
                    behavioralTraits = "Всегда проверяет источники перед принятием решений; не доверяет непроверенным слухам.",
                    visualPassportPrompt = "Portrait of Victor Corvin, 37 year old male archivist, sharp cheekbones, dark brown hair with silver temples, amber eyes, wearing a deep burgundy tailored vest over a crisp dark shirt, antique pocket watch chain, moody crimson library lighting, detailed concept art.",
                    customFieldsJson = """{"Уровень допуска":"Высший","Любимый артефакт":"Хронометр Корвина"}""",
                    currentLocationName = "Красный особняк",
                    currentMoodAndCondition = "Изучает несоответствия в старых записях Зелёной улицы",
                    currentOutfit = "Повседневный костюм архивариуса",
                    currentActiveGoal = "Сверить показания Елены с журналом посетителей Бара",
                    canonStatus = CanonStatus.CANON.name,
                    relationshipsSummary = "Наставник и коллега Елены Ростовой; старый знакомый владельца Бара.",
                    linkedLocationIdsCsv = "$locMansionId,$locGreenStId"
                )
            )

            val charElenaId = rwDao.insertCharacter(
                CharacterEntity(
                    name = "Елена Ростова",
                    aliases = "Тень Зелёной улицы, Лена",
                    birthDate = "03 апреля 1998",
                    age = "27",
                    birthPlace = "Восточный округ",
                    appearance = "Гибкая, стремительная девушка с выразительными чертами лица и внимательным взглядом.",
                    physique = "Атлетичное, лёгкое",
                    hair = "Чёрные, каре до плеч с багровым отливом",
                    eyes = "Серые, холодные",
                    defaultClothing = "Кожаный плащ цвета спелой вишни, практичные ботинки, сумка через плечо.",
                    distinguishingMarks = "Серебряный кулон в форме ключа.",
                    personality = "Наблюдательная, независимая, склонная к риску ради установления правды.",
                    habits = "Садится лицом ко входу в Баре, фотографирует детали улиц.",
                    speechStyle = "Прямая, живая, использует городской сленг Зелёной улицы.",
                    occupation = "Полевой исследователь и курьер редких документов",
                    skills = "Ориентирование в городе, поиск свидетелей, анализ улик, скрытное наблюдение.",
                    interests = "Городские легенды, аналоговая фотография, ночные прогулки.",
                    preferences = "Вишнёвый тоник в Баре, старые карты города.",
                    goals = "Раскрыть тайну исчезнувшей коллекции из Магазина на Зелёной улице.",
                    motivation = "Личный поиск ответов о прошлом своей семьи.",
                    behavioralTraits = "Действует самостоятельно, если считает, что официальный протокол медлит.",
                    visualPassportPrompt = "Character portrait of Elena Rostova, 27 year old female investigator, sleek black bob hair with subtle crimson tint, piercing grey eyes, wearing a dark cherry leather trench coat and silver key pendant, rainy neon reflection of Green Street at night, cinematic illustration.",
                    customFieldsJson = """{"Полевой ранг":"Старший курьер","Камера":"Leica M6"}""",
                    currentLocationName = "Бар",
                    currentMoodAndCondition = "Насторожена после встречи с информатором",
                    currentOutfit = "Дорожный плащ с поднятым воротником",
                    currentActiveGoal = "Передать найденный реестр Виктору в Красный особняк",
                    canonStatus = CanonStatus.CANON.name,
                    relationshipsSummary = "Работает с Виктором Корвином; регулярно бывает в Магазине и Баре.",
                    linkedLocationIdsCsv = "$locGreenStId,$locBarId,$locMansionId"
                )
            )

            // 4. Seed World Rules
            val ruleAutonomyId = rwDao.insertWorldRule(
                WorldRuleEntity(
                    title = "Механика самостоятельного поведения персонажей",
                    category = "Автономия персонажей",
                    content = "Персонажи RedWorld обладают собственными целями, распорядком дня и мотивацией. Они перемещаются между локациями (Красный особняк, Зелёная улица, Магазин, Бар) в соответствии со своими задачами даже без прямого участия игрока. Изменение текущего состояния персонажа фиксируется отдельно от постоянных биографических сведений.",
                    priorityLevel = "Фундаментальный канон",
                    canonStatus = CanonStatus.CANON.name
                )
            )
            val ruleMansionId = rwDao.insertWorldRule(
                WorldRuleEntity(
                    title = "Законы Красного особняка и Архива",
                    category = "Общие правила",
                    content = "1. Любое изменение канонических карточек требует фиксации версии.\n2. Предположения и черновики никогда автоматически не приравниваются к канону.\n3. В пределах Красного особняка и Бара «Алый Бархат» действует запрет на открытые силовые конфликты.",
                    priorityLevel = "Строгий закон",
                    canonStatus = CanonStatus.CANON.name
                )
            )
            rwDao.insertWorldRule(
                WorldRuleEntity(
                    title = "Правила взаимодействия с игроком и построения сцен",
                    category = "Правила сцен",
                    content = "Каждая сцена учитывает: (а) локацию и её правила доступа, (б) текущее состояние присутствующих персонажей, (в) их отношение к игроку на основе прошлых событий. ИИ-помощник обязан опираться только на подтверждённые документы проекта.",
                    priorityLevel = "Строгий закон",
                    canonStatus = CanonStatus.CANON.name
                )
            )

            // 5. Seed Events
            val eventArrivalId = rwDao.insertEvent(
                WorldEventEntity(
                    title = "Передача шифрованного реестра на Зелёной улице",
                    eventType = "Ключевое событие",
                    timelineDate = "День 12, 21:40",
                    chronologyOrder = 1,
                    locationName = "Бар",
                    participantNamesCsv = "Елена Ростова, Виктор Корвин",
                    description = "В угловой кабинке Бара на Зелёной улице Елена получила фрагмент старой ведомости Магазина и доставила его в архив Красного особняка.",
                    consequences = "Уровень доверия между Виктором и Еленой укрепился; открыт доступ к проверке архива Магазина.",
                    canonStatus = CanonStatus.CANON.name
                )
            )

            // 6. Seed Items
            rwDao.insertItem(
                WorldItemEntity(
                    name = "Реестр Красного особняка (XLSX-каталог)",
                    itemType = "Артефакт / Документ",
                    ownerOrLeader = "Виктор Корвин",
                    locationName = "Красный особняк",
                    description = "Структурированная ведомость всех резидентов, комнат и правил доступа Красного особняка.",
                    properties = "Хранится в библиотеке и синхронизируется с таблицами проекта.",
                    canonStatus = CanonStatus.CANON.name
                )
            )

            // 7. Seed Knowledge Documents + Chunks + Notes + Tag Cross-Refs
            val docCanonText = """
                # Кодекс проекта RedWorld: Архитектура мира
                
                ## 1. География и ключевые точки
                Центром событий является **Красный особняк** — закрытая резиденция и исследовательский архив. От его кованых ворот начинается переход к **Зелёной улице**, где расположены **Магазин** редких вещей и ночной **Бар** «Алый Бархат».
                
                ## 2. Разделение памяти и состояний
                В системе RedWorld строго разделяются три слоя данных:
                - **Постоянные сведения**: биография, дата рождения, черты внешности и базовый характер.
                - **Текущее состояние**: где персонаж находится прямо сейчас, во что одет и какова его текущая цель.
                - **История событий**: хронологический журнал сцен, который не переписывает прошлое при смене текущей локации.
                
                ## 3. Ключевые действующие лица
                - **Виктор Корвин** (37 лет, дата рождения: 14 ноября 1988) — Хранитель архива Красного особняка. Отличается аналитическим складом ума, носит тёмно-бордовый жилет и карманные часы.
                - **Елена Ростова** (27 лет, дата рождения: 03 апреля 1998) — полевой исследователь, работающая на Зелёной улице и в Баре. Носит вишнёвый кожаный плащ и серебряный кулон-ключ.
            """.trimIndent()

            val doc1Id = kDao.insertDocument(
                KnowledgeDocumentEntity(
                    title = "Кодекс RedWorld: Канон и Персонажи",
                    fileName = "redworld_codex_v1.md",
                    format = DocumentFormat.MARKDOWN.name,
                    folderName = "Правила мира",
                    collectionName = "Основной канон",
                    contentText = docCanonText,
                    summary = "Базовый документ канона RedWorld: локации (Красный особняк, Зелёная улица, Магазин, Бар), три слоя памяти и досье Виктора и Елены.",
                    fileSizeBytes = docCanonText.toByteArray().size.toLong(),
                    linkedCharacterIdsCsv = "$charVictorId,$charElenaId",
                    linkedLocationIdsCsv = "$locMansionId,$locGreenStId,$locShopId,$locBarId",
                    isAllowedInAiContext = true
                )
            )

            val doc2Text = """
                # Протокол работы с XLSX-карточками RedWorld
                
                ## Структура листов карточки персонажа
                1. Лист «Постоянные сведения»: Поля Имя, Псевдонимы, Дата рождения, Возраст, Внешность, Характер, Привычки, Манера речи.
                2. Лист «Текущее состояние»: Локация сейчас, Эмоциональный фон, Активная задача.
                3. Лист «Отношения и связи»: Таблица связей с другими персонажами и локациями.
                
                ## Правило сохранения структуры
                При редактировании отдельной ячейки в XLSX-файле система обязана сохранять все остальные листы, порядок строк, объединённые ячейки (mergeCells), формулы и стили оформления, создавая резервную копию перед сохранением.
            """.trimIndent()

            val doc2Id = kDao.insertDocument(
                KnowledgeDocumentEntity(
                    title = "Регламент карточек и таблиц XLSX",
                    fileName = "xlsx_structure_guide.txt",
                    format = DocumentFormat.TXT.name,
                    folderName = "Техническая документация",
                    collectionName = "Шаблоны карточек",
                    contentText = doc2Text,
                    summary = "Описание структуры таблиц персонажей RedWorld и правил безопасного редактирования XLSX без потери форматирования.",
                    fileSizeBytes = doc2Text.toByteArray().size.toLong(),
                    linkedCharacterIdsCsv = "$charVictorId",
                    linkedLocationIdsCsv = "$locMansionId",
                    isAllowedInAiContext = true
                )
            )

            // Seed RAG Chunks for doc1 and doc2
            kDao.insertChunks(
                listOf(
                    DocumentChunkEntity(
                        documentId = doc1Id,
                        chunkIndex = 0,
                        sectionTitle = "География и ключевые точки",
                        chunkText = "Центром событий является Красный особняк — закрытая резиденция и исследовательский архив. От его кованых ворот начинается переход к Зелёной улице, где расположены Магазин редких вещей и ночной Бар «Алый Бархат»."
                    ),
                    DocumentChunkEntity(
                        documentId = doc1Id,
                        chunkIndex = 1,
                        sectionTitle = "Разделение памяти и состояний",
                        chunkText = "В системе RedWorld строго разделяются три слоя данных: Постоянные сведения (биография, дата рождения, внешность), Текущее состояние (где персонаж сейчас и его активная цель) и История событий (хронологический журнал сцен)."
                    ),
                    DocumentChunkEntity(
                        documentId = doc1Id,
                        chunkIndex = 2,
                        sectionTitle = "Ключевые действующие лица",
                        chunkText = "Виктор Корвин (37 лет, род. 14 ноября 1988) — Хранитель архива Красного особняка, носит бордовый жилет и карманные часы. Елена Ростова (27 лет, род. 03 апреля 1998) — полевой исследователь на Зелёной улице и в Баре, носит вишнёвый плащ и серебряный кулон-ключ."
                    ),
                    DocumentChunkEntity(
                        documentId = doc2Id,
                        chunkIndex = 0,
                        sectionTitle = "Структура листов и сохранение XLSX",
                        chunkText = "Лист 1: Постоянные сведения. Лист 2: Текущее состояние. Лист 3: Отношения и связи. При редактировании ячейки сохраняются все листы, формулы, объединённые ячейки и форматирование с автоматическим созданием резервной версии."
                    )
                )
            )

            // Cross-refs between documents and tags
            kDao.insertDocumentTagCrossRef(DocumentTagCrossRef(documentId = doc1Id, tagId = tagCanonId))
            kDao.insertDocumentTagCrossRef(DocumentTagCrossRef(documentId = doc1Id, tagId = tagMansionId))
            kDao.insertDocumentTagCrossRef(DocumentTagCrossRef(documentId = doc1Id, tagId = tagCharactersId))
            kDao.insertDocumentTagCrossRef(DocumentTagCrossRef(documentId = doc2Id, tagId = tagRulesId))

            // Seed Notes
            val note1Id = kDao.insertNote(
                KnowledgeNoteEntity(
                    documentId = doc1Id,
                    title = "Проверка хронологии встречи в Баре",
                    content = "Убедиться, что время прихода Елены в Бар (21:40) не противоречит графику закрытия Магазина на Зелёной улице (21:00).",
                    quotedExcerpt = "В угловой кабинке Бара на Зелёной улице Елена получила фрагмент старой ведомости Магазина",
                    linkedCharacterId = charElenaId,
                    linkedLocationId = locBarId,
                    isPinned = true
                )
            )
            kDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = note1Id, tagId = tagGreenStId))
            kDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = note1Id, tagId = tagCanonId))

            // 8. Seed Visual Relationship Map Nodes & Edges
            rwDao.insertRelationship(
                RelationshipEntity(
                    sourceType = "CHARACTER",
                    sourceId = charVictorId,
                    sourceLabel = "Виктор Корвин",
                    targetType = "LOCATION",
                    targetId = locMansionId,
                    targetLabel = "Красный особняк",
                    relationType = "Живёт в",
                    description = "Постоянный хранитель архива и резидент особняка",
                    sourceX = 240f,
                    sourceY = 220f,
                    targetX = 520f,
                    targetY = 180f
                )
            )
            rwDao.insertRelationship(
                RelationshipEntity(
                    sourceType = "CHARACTER",
                    sourceId = charElenaId,
                    sourceLabel = "Елена Ростова",
                    targetType = "CHARACTER",
                    targetId = charVictorId,
                    targetLabel = "Виктор Корвин",
                    relationType = "Знаком с",
                    description = "Полевой исследователь, передаёт Виктору найденные документы",
                    sourceX = 220f,
                    sourceY = 480f,
                    targetX = 240f,
                    targetY = 220f
                )
            )
            rwDao.insertRelationship(
                RelationshipEntity(
                    sourceType = "CHARACTER",
                    sourceId = charElenaId,
                    sourceLabel = "Елена Ростова",
                    targetType = "LOCATION",
                    targetId = locBarId,
                    targetLabel = "Бар",
                    relationType = "Работает в",
                    description = "Встречается с информаторами в Баре на Зелёной улице",
                    sourceX = 220f,
                    sourceY = 480f,
                    targetX = 560f,
                    targetY = 500f
                )
            )
            rwDao.insertRelationship(
                RelationshipEntity(
                    sourceType = "LOCATION",
                    sourceId = locMansionId,
                    sourceLabel = "Красный особняк",
                    targetType = "LOCATION",
                    targetId = locGreenStId,
                    targetLabel = "Зелёная улица",
                    relationType = "Локация связана с локацией",
                    description = "Выход из сада особняка ведёт прямо на Зелёную улицу",
                    sourceX = 520f,
                    sourceY = 180f,
                    targetX = 820f,
                    targetY = 340f
                )
            )
            rwDao.insertRelationship(
                RelationshipEntity(
                    sourceType = "LOCATION",
                    sourceId = locGreenStId,
                    sourceLabel = "Зелёная улица",
                    targetType = "LOCATION",
                    targetId = locShopId,
                    targetLabel = "Магазин",
                    relationType = "Локация связана с локацией",
                    description = "Магазин редких вещей расположен в доме №14 по Зелёной улице",
                    sourceX = 820f,
                    sourceY = 340f,
                    targetX = 840f,
                    targetY = 560f
                )
            )
            rwDao.insertRelationship(
                RelationshipEntity(
                    sourceType = "EVENT",
                    sourceId = eventArrivalId,
                    sourceLabel = "Передача реестра",
                    targetType = "LOCATION",
                    targetId = locBarId,
                    targetLabel = "Бар",
                    relationType = "Событие происходит в локации",
                    description = "Сцена День 12, 21:40",
                    sourceX = 480f,
                    sourceY = 720f,
                    targetX = 560f,
                    targetY = 500f
                )
            )
            rwDao.insertRelationship(
                RelationshipEntity(
                    sourceType = "DOCUMENT",
                    sourceId = doc1Id,
                    sourceLabel = "Кодекс RedWorld",
                    targetType = "CHARACTER",
                    targetId = charVictorId,
                    targetLabel = "Виктор Корвин",
                    relationType = "Документ описывает персонажа",
                    description = "Каноническое досье в разделе 3 Кодекса",
                    sourceX = 140f,
                    sourceY = 700f,
                    targetX = 240f,
                    targetY = 220f
                )
            )
            rwDao.insertRelationship(
                RelationshipEntity(
                    sourceType = "RULE",
                    sourceId = ruleAutonomyId,
                    sourceLabel = "Автономия персонажей",
                    targetType = "LOCATION",
                    targetId = locMansionId,
                    targetLabel = "Красный особняк",
                    relationType = "Правило регулирует сцену",
                    description = "Определяет самостоятельное перемещение резидентов",
                    sourceX = 780f,
                    sourceY = 140f,
                    targetX = 520f,
                    targetY = 180f
                )
            )

            // 9. Seed Initial Version Snapshot
            rwDao.insertVersionSnapshot(
                VersionSnapshotEntity(
                    entityType = "CHARACTER",
                    entityId = charVictorId,
                    entityTitle = "Виктор Корвин",
                    versionNumber = 1,
                    changeSummary = "Базовая каноническая версия карточки персонажа (Создание проекта)",
                    payloadJson = """{"name":"Виктор Корвин","currentLocationName":"Красный особняк","canonStatus":"CANON","occupation":"Хранитель архива Красного особняка"}""",
                    author = "Система (Инициализация)"
                )
            )

            // 10. Seed Initial Chat Session
            val sessionId = rwDao.insertChatSession(
                ChatSessionEntity(
                    title = "Анализ канона Красного особняка",
                    modelId = "gemini-3.5-flash",
                    isRedWorldContextEnabled = true,
                    selectedDocumentIdsCsv = "$doc1Id,$doc2Id",
                    assistantMode = "CONSULTATION"
                )
            )
            rwDao.insertChatMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "model",
                    content = "Добро пожаловать в **RedWorld AI Studio**! Я подключён к локальной базе знаний проекта.\n\nАктивные источники в контексте:\n- `Кодекс RedWorld: Канон и Персонажи`\n- `Регламент карточек и таблиц XLSX`\n\nВы можете задавать вопросы по канону, проверять карточки персонажей на противоречия, генерировать промпты по паспорту внешности или готовить черновики новых сцен.",
                    citedSourcesJson = """[{"documentId":$doc1Id,"title":"Кодекс RedWorld: Канон и Персонажи","section":"География и ключевые точки"}]""",
                    modelUsed = "gemini-3.5-flash"
                )
            )
        }
    }
}
