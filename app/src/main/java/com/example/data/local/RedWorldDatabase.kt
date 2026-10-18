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
        PlotlineEntity::class,
        WorldEventEntity::class,
        WorldItemEntity::class,
        RelationshipEntity::class,
        VersionSnapshotEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        GeneratedImageEntity::class,
        WorldProjectEntity::class,
        NotebookLmNotebookEntity::class
    ],
    version = 7,
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

            // 0. Seed Multi-Project Workspaces in World AI Studio
            rwDao.insertProject(
                WorldProjectEntity(
                    id = 1L,
                    name = "RedWorld — Красный особняк",
                    genreOrTheme = "Готический детектив и техно-нуар",
                    description = "Основной авторский проект: Красный особняк, Зелёная улица, Магазин редких артефактов и Бар «Алый Бархат».",
                    accentColorHex = "#E11D48",
                    templateType = "DETECTIVE_NOIR"
                )
            )
            val projAetherId = rwDao.insertProject(
                WorldProjectEntity(
                    id = 2L,
                    name = "Эфириум — Хроники Цитадели",
                    genreOrTheme = "Научное фэнтези и небесные архивы",
                    description = "Второй мир в World AI Studio: парящая Обсерватория Эфира, орден навигаторов и звёздные манускрипты.",
                    accentColorHex = "#F59E0B",
                    templateType = "SCI_FI"
                )
            )

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
                    linkedCharactersCsv = "Катрин, Александра, Виктор Корвин",
                    linkedObjectsCsv = "Реестр Красного особняка (XLSX-каталог)",
                    linkedEventsCsv = "Передача шифрованного реестра на Зелёной улице",
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
                    linkedCharactersCsv = "Нина, Лола, Елена Ростова",
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
                    linkedCharactersCsv = "Нина, Виктор Корвин",
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
                    linkedCharactersCsv = "Лола, Елена Ростова",
                    linkedEventsCsv = "Передача шифрованного реестра на Зелёной улице",
                    canonStatus = CanonStatus.CANON.name
                )
            )

            // 3. Seed Characters (Катрин, Александра, Нина, Лола, Виктор Корвин, Елена Ростова)
            rwDao.insertCharacter(
                CharacterEntity(
                    sortOrder = 1,
                    name = "Катрин",
                    fullName = "Леди Катрин де Валье",
                    aliases = "Хозяйка салона, Леди Катрин",
                    storyRole = "Главный герой",
                    characterStatus = "Активен",
                    factionOrGroup = "Красный особняк",
                    tagsCsv = "Аристократия, Дипломатия, Красный особняк, Хранители",
                    gender = "Женский",
                    speciesOrOrigin = "Человек (Старый род)",
                    shortDescription = "Распорядительница закрытых вечеров и хранительница дипломатического протокола Красного особняка.",
                    detailedDescription = "Аристократичная женщина с безупречной выдержкой, управляющая приёмами и внутренним порядком в Красном особняке. Знает тайные обязательства большинства гостей.",
                    birthDate = "22 сентября 1991",
                    age = "34",
                    birthPlace = "Старый город",
                    appearance = "Статная женщина с платиновыми волосами, уложенными в строгую волну, и холодными изумрудными глазами.",
                    physique = "Стройное, грациозное (рост 174 см)",
                    hair = "Платиновый блонд",
                    eyes = "Изумрудно-зелёные",
                    voiceDescription = "Бархатистое меццо-сопрано, говорит размеренно и негромко",
                    defaultClothing = "Алое бархатное платье со строгим воротником и серебряная брошь с рубином.",
                    distinguishingMarks = "Тонкое кольцо с гербовой гравировкой на правой руке.",
                    personality = "Сдержанная, проницательная, дипломатичная, не терпит нарушения этикета и хаоса.",
                    archetype = "Правитель / Дипломат",
                    habits = "Поправляет манжеты перед началом важного разговора, пьёт жасминовый чай из тонкого фарфора.",
                    speechStyle = "Вежливая, литературная, с тонкими намёками и безупречной интонацией.",
                    catchphrases = "«В этих стенах даже тишина имеет свою цену», «Прошу вас соблюдать протокол дома»",
                    occupation = "Распорядительница Красного особняка",
                    skills = "Дипломатия, чтение невербальных сигналов, организация закрытых переговоров.",
                    strengths = "Безупречное самообладание, обширная сеть связей среди элиты города",
                    weaknesses = "Связана старым семейным обетом, избегает открытого риска вне стен особняка",
                    inventorySummary = "Серебряная брошь-ключ от каминного зала, гербовая печать, личный дневник приёмов",
                    interests = "Старинная музыка, парфюмерия, архивные письма.",
                    preferences = "Каминный зал вечером, тихая инструментальная музыка.",
                    dislikes = "Грубость, нарушение нейтралитета, непрошеные визиты",
                    goals = "Сохранить нейтралитет Красного особняка и предотвратить раскол среди резидентов.",
                    motivation = "Личная клятва хранить безопасность дома и его обитателей.",
                    internalConflict = "Выбор между строгим соблюдением старого договора и личной привязанностью к обитателям дома",
                    fearsAndLimitations = "Опасается раскрытия старого семейного договора; никогда не покидает особняк без сопровождения.",
                    knownFacts = "Знает список всех гостей Виктора Корвина и тайный проход из библиотеки в сад Зелёной улицы.",
                    personalSecrets = "Хранит оригинал завещания основателя Красного особняка в тайнике за витражом",
                    unknownFactsRestriction = "Не знает о тайных ночных встречах Елены в угловой кабинке Бара",
                    behavioralTraits = "В споре говорит тише обычного, заставляя собеседника прислушиваться.",
                    visualPassportPrompt = "Portrait of Katrin, 34 year old aristocratic woman, platinum blonde wavy hair, emerald green eyes, wearing a tailored crimson velvet evening dress with silver ruby brooch, warm gothic chandelier lighting.",
                    declaredRelationshipsJson = """[{"targetName":"Александра","targetType":"CHARACTER","relationType":"Доверяет","description":"Ближайшая союзница в делах Красного особняка"},{"targetName":"Виктор Корвин","targetType":"CHARACTER","relationType":"Сотрудничает","description":"Совместно управляют архивом и приёмами особняка"}]""",
                    customFieldsJson = """{"Титул":"Хранительница зала","Любимый цветок":"Чёрная роза"}""",
                    currentLocationName = "Красный особняк",
                    currentMoodAndCondition = "Спокойна, готовит вечерний приём",
                    currentHealthStatus = "В норме",
                    currentOutfit = "Алое бархатное платье",
                    currentActiveGoal = "Обсудить с Александрой меры безопасности перед визитом гостей",
                    isFavorite = true,
                    isPinned = true,
                    canonStatus = CanonStatus.CANON.name,
                    relationshipsSummary = "Тесно сотрудничает с Виктором Корвином и Александрой; настороженно относится к сделкам в Баре.",
                    linkedLocationIdsCsv = "$locMansionId"
                )
            )

            rwDao.insertCharacter(
                CharacterEntity(
                    sortOrder = 2,
                    name = "Александра",
                    fullName = "Александра Верещагина",
                    aliases = "Алекс, Куратор безопасности",
                    storyRole = "Главный герой",
                    characterStatus = "Активен",
                    factionOrGroup = "Красный особняк",
                    tagsCsv = "Безопасность, Криминалистика, Тактика, Красный особняк",
                    gender = "Женский",
                    speciesOrOrigin = "Человек",
                    shortDescription = "Аналитик и руководитель внутренней службы безопасности Красного особняка.",
                    detailedDescription = "Отвечает за защиту периметра Красного особняка, проверку посетителей и техническую экспертизу найденных носителей информации.",
                    birthDate = "09 февраля 1994",
                    age = "31",
                    birthPlace = "Северный бастион",
                    appearance = "Высокая девушка с короткими тёмно-русыми волосами и внимательным взглядом карих глаз.",
                    physique = "Спортивное, собранное (рост 178 см)",
                    hair = "Тёмно-русые, короткая стрижка",
                    eyes = "Тёмно-карие",
                    voiceDescription = "Низкий, чёткий голос с командными нотками",
                    defaultClothing = "Чёрный китель с бордовой окантовкой, кожаные перчатки, тактический коммуникатор.",
                    distinguishingMarks = "Небольшой шрам над правой бровью.",
                    personality = "Прагматичная, прямолинейная, надёжная, ценит дисциплину и конкретные факты.",
                    archetype = "Хранитель / Страж",
                    habits = "Проверяет замки и окна при входе в помещение, носит с собой металлический жетон.",
                    speechStyle = "Лаконичная, чёткая, говорит по делу без лишних украшений.",
                    catchphrases = "«Покажи факты, а версии обсудим позже», «Периметр чист»",
                    occupation = "Глава безопасности Красного особняка",
                    skills = "Тактический анализ, криминалистика, защита информации, фехтование.",
                    strengths = "Мгновенная реакция в критических ситуациях, аналитический склад ума",
                    weaknesses = "Излишняя подозрительность к посторонним, прямолинейность в дипломатии",
                    inventorySummary = "Трость-клинок, тактический коммуникатор, связка мастер-ключей сектора А",
                    interests = "Инженерные головоломки, стрельба из лука, картография.",
                    preferences = "Утренние обходы сада, холодный мятный настой.",
                    dislikes = "Опоздания, небрежность с документами, двойную игру",
                    goals = "Выявить источник утечки сведений из Магазина на Зелёной улице.",
                    motivation = "Защита Катрин и архива особняка от внешнего проникновения.",
                    internalConflict = "Между строгим приказом не вмешиваться в дела Зелёной улицы и желанием лично нейтрализовать угрозу",
                    fearsAndLimitations = "Не доверяет неформальным информаторам из Бара; связана уставом службы особняка.",
                    knownFacts = "Знает коды доступа к подземному хранилищу и расписание смены охраны особняка.",
                    personalSecrets = "Ведёт собственное негласное досье на каждого гостя Красного особняка",
                    unknownFactsRestriction = "Не знает истинного происхождения серебряного кулона Елены Ростовой",
                    behavioralTraits = "Всегда занимает позицию с обзором на все выходы.",
                    visualPassportPrompt = "Portrait of Alexandra, 31 year old female security chief, short dark ash-brown hair, sharp brown eyes, black tactical coat with burgundy trim, gothic manor courtyard background.",
                    declaredRelationshipsJson = """[{"targetName":"Катрин","targetType":"CHARACTER","relationType":"Защищает","description":"Отвечает за личную безопасность Катрин"},{"targetName":"Нина","targetType":"CHARACTER","relationType":"Консультируется","description":"Заказывает у Нины редкие технические приборы в Магазине"}]""",
                    customFieldsJson = """{"Допуск":"Сектор А и Подземный архив","Оружие":"Трость-клинок"}""",
                    currentLocationName = "Красный особняк",
                    currentMoodAndCondition = "Бдительна, проверяет отчёты периметра",
                    currentHealthStatus = "В норме",
                    currentOutfit = "Чёрный китель с бордовой окантовкой",
                    currentActiveGoal = "Проверить подлинность пропуска, доставленного с Зелёной улицы",
                    isFavorite = true,
                    canonStatus = CanonStatus.CANON.name,
                    relationshipsSummary = "Предана Катрин; уважает Виктора Корвина; периодически спорит с Еленой из-за её рискованных методов.",
                    linkedLocationIdsCsv = "$locMansionId,$locGreenStId"
                )
            )

            rwDao.insertCharacter(
                CharacterEntity(
                    sortOrder = 3,
                    name = "Нина",
                    fullName = "Нина Соколова",
                    aliases = "Мастер Нина, Хранительница витрин",
                    storyRole = "Союзник",
                    characterStatus = "Активен",
                    factionOrGroup = "Магазин на Зелёной улице",
                    tagsCsv = "Механика, Антиквариат, Зелёная улица, Артефакты",
                    gender = "Женский",
                    speciesOrOrigin = "Человек",
                    shortDescription = "Владелица и главный реставратор Магазина редких вещей на Зелёной улице.",
                    detailedDescription = "Талантливый часовой мастер и специалист по старинным механизмам. Через её Магазин проходят редкие артефакты, шифровальные шкатулки и архивные принадлежности.",
                    birthDate = "17 мая 1996",
                    age = "29",
                    birthPlace = "Ремесленный квартал",
                    appearance = "Невысокая девушка с рыжевато-медными кудрями, веснушками и живыми зелёными глазами.",
                    physique = "Хрупкое (рост 163 см)",
                    hair = "Медно-рыжие, собраны заколкой",
                    eyes = "Светло-зелёные",
                    voiceDescription = "Звонкий, живой голос, ускоряющийся при обсуждении редких механизмов",
                    defaultClothing = "Жилет реставратора с множеством карманов, часовая лупа на цепочке, льняная блузка.",
                    distinguishingMarks = "Следы чернил и машинного масла на пальцах.",
                    personality = "Любознательная, увлекающаяся, дружелюбная, но крайне щепетильная в вопросах цены и подлинности вещей.",
                    archetype = "Творец / Изобретатель",
                    habits = "Постукивает карандашом по прилавку во время раздумий, заводит старинные часы.",
                    speechStyle = "Быстрая, образная, часто использует ремесленные и технические метафоры.",
                    catchphrases = "«У каждой шестерёнки есть свой секрет», «Подделку слышно по первому щелчку»",
                    occupation = "Владелица Магазина на Зелёной улице",
                    skills = "Реставрация механизмов, оценка антиквариата, вскрытие сложных шкатулок.",
                    strengths = "Уникальная техническая экспертиза, доступ к редким товарам города",
                    weaknesses = "Чрезмерное любопытство к опасным артефактам",
                    inventorySummary = "Часовая лупа с двойной линзой, набор отмычек-калибров, журнал заказов деда",
                    interests = "Автоматоны, редкие сплавы, старые каталоги.",
                    preferences = "Чай с бергамотом и корицей, звук дождя по витрине Магазина.",
                    dislikes = "Вандализм по отношению к старинным вещам, нечестных скупщиков",
                    goals = "Вернуть похищенный хронометр из первой коллекции своего деда.",
                    motivation = "Сохранить репутацию семейной мастерской на Зелёной улице.",
                    internalConflict = "Желание разгадать шифр закрытых шкатулок клиентов вопреки правилам конфиденциальности",
                    fearsAndLimitations = "Боится потерять лицензию Магазина; не участвует в открытых конфликтах фракций.",
                    knownFacts = "Знает настоящих владельцев большинства заложенных в Магазине предметов и тайник под прилавком.",
                    personalSecrets = "Собрала рабочую копию шифровального диска Красного особняка по старым чертежам",
                    unknownFactsRestriction = "Не знает содержания секретных протоколов безопасности Александры",
                    behavioralTraits = "Мгновенно определяет подделку по весу и звуку металла.",
                    visualPassportPrompt = "Portrait of Nina, 29 year old clockmaker and antique shop owner, copper red curly hair, green eyes, brass jeweler loupe, vintage vest, warm antique shop interior.",
                    declaredRelationshipsJson = """[{"targetName":"Лола","targetType":"CHARACTER","relationType":"Подруга","description":"Часто обменивается новостями Зелёной улицы после закрытия Магазина"},{"targetName":"Виктор Корвин","targetType":"CHARACTER","relationType":"Поставщик","description":"Реставрирует редкие книги и приборы для архива особняка"}]""",
                    customFieldsJson = """{"Специализация":"Часовые и шифровальные механизмы","Часы работы":"10:00–21:00"}""",
                    currentLocationName = "Магазин",
                    currentMoodAndCondition = "Увлечена разборкой старинной шкатулки",
                    currentHealthStatus = "В норме",
                    currentOutfit = "Рабочий жилет реставратора",
                    currentActiveGoal = "Расшифровать гравировку на внутренней крышке хронометра",
                    canonStatus = CanonStatus.CANON.name,
                    relationshipsSummary = "Дружит с Лолой и Еленой Ростовой; выполняет спецзаказы для Виктора и Александры.",
                    linkedLocationIdsCsv = "$locShopId,$locGreenStId"
                )
            )

            rwDao.insertCharacter(
                CharacterEntity(
                    sortOrder = 4,
                    name = "Лола",
                    fullName = "Лола Монтеро",
                    aliases = "Голос «Алого Бархата»",
                    storyRole = "Второстепенный",
                    characterStatus = "Активен",
                    factionOrGroup = "Бар «Алый Бархат»",
                    tagsCsv = "Бар, Информатор, Музыка, Нейтралитет, Зелёная улица",
                    gender = "Женский",
                    speciesOrOrigin = "Человек",
                    shortDescription = "Хозяйка зала и певица ночного Бара «Алый Бархат» на Зелёной улице.",
                    detailedDescription = "Харизматичная распорядительница Бара, знающая все негласные договорённости города. Следит за соблюдением правила нейтралитета внутри «Алого Бархата».",
                    birthDate = "04 августа 1995",
                    age = "30",
                    birthPlace = "Южный порт",
                    appearance = "Эффектная брюнетка с выразительными тёмными глазами и мягкой полуулыбкой.",
                    physique = "Стройное (рост 170 см)",
                    hair = "Тёмно-каштановые, длинные локоны",
                    eyes = "Карие с золотистым оттенком",
                    voiceDescription = "Глубокое грудное контральто с джазовыми интонациями",
                    defaultClothing = "Бордовый пиджак поверх шёлкового топа, длинные серьги, микрофонная стойка рядом.",
                    distinguishingMarks = "Родинка у левого уголка губ.",
                    personality = "Обаятельная, ироничная, проницательная, умеет разрядить любую напряжённую сцену.",
                    archetype = "трикстер / Дипломат теней",
                    habits = "Протирает бокал перед тем, как поделиться важной новостью, напевает джазовые мотивы.",
                    speechStyle = "Мягкая, мелодичная, с лёгким юмором и множеством полутонов.",
                    catchphrases = "«В „Алом Бархате“ пьют за перемирие, дорогуша», «Эта песня посвящается тем, кто умеет хранить секреты»",
                    occupation = "Хозяйка зала в Баре «Алый Бархат»",
                    skills = "Посредничество в переговорах, сбор слухов, управление атмосферой зала.",
                    strengths = "Знает все слухи Зелёной улицы раньше официальных служб, пользуется иммунитетом нейтральной стороны",
                    weaknesses = "Зависит от репутации Бара как безопасного места",
                    inventorySummary = "Ключи от звукоизолированных кабинок, коллекционная зажигалка, записная книжка шифров под стойкой",
                    interests = "Виниловые пластинки, ночная жизнь города, психология людей.",
                    preferences = "Гранатовый коктейль со льдом, приглушённый свет софитов.",
                    dislikes = "Драки в зале, шантаж постоянных гостей",
                    goals = "Сделать Бар главной нейтральной площадкой для переговоров всех сторон.",
                    motivation = "Независимость и защита своих постоянных гостей.",
                    internalConflict = "Балансирование между нейтралитетом Бара и помощью своим друзьям в опасных расследованиях",
                    fearsAndLimitations = "Не выдаёт тайны клиентов без крайней угрозы городу; не покидает Зелёную улицу ночью.",
                    knownFacts = "Знает, кто встречался в угловой кабинке Бара в ночь передачи шифрованного реестра.",
                    personalSecrets = "Помогла укрыть пропавшего свидетеля из Южного порта в подсобке Бара",
                    unknownFactsRestriction = "Не знает внутренних шифров подземного архива Красного особняка",
                    behavioralTraits = "Замечает малейшую фальшь в голосе собеседника.",
                    visualPassportPrompt = "Portrait of Lola, 30 year old jazz lounge hostess, long dark chestnut waves, warm brown eyes, burgundy tailored blazer, vintage microphone, moody crimson velvet bar lighting.",
                    declaredRelationshipsJson = """[{"targetName":"Нина","targetType":"CHARACTER","relationType":"Подруга","description":"Соседка по Зелёной улице и близкая подруга"},{"targetName":"Елена Ростова","targetType":"CHARACTER","relationType":"Информатор","description":"Оставляет для Елены зашифрованные записки за стойкой Бара"}]""",
                    customFieldsJson = """{"Правило зала":"Никакого оружия внутри Бара","Любимая пластинка":"Nocturne in Crimson"}""",
                    currentLocationName = "Бар",
                    currentMoodAndCondition = "В хорошем настроении, встречает вечерних посетителей",
                    currentHealthStatus = "В норме",
                    currentOutfit = "Бордовый бархатный пиджак",
                    currentActiveGoal = "Передать Елене сведения о странном покупателе из Магазина",
                    canonStatus = CanonStatus.CANON.name,
                    relationshipsSummary = "Близкая подруга Нины; помогает Елене Ростовой; поддерживает нейтралитет с Красным особняком.",
                    linkedLocationIdsCsv = "$locBarId,$locGreenStId"
                )
            )

            val charVictorId = rwDao.insertCharacter(
                CharacterEntity(
                    sortOrder = 5,
                    name = "Виктор Корвин",
                    fullName = "Виктор Корвин",
                    aliases = "Архивариус, Хранитель",
                    storyRole = "Главный герой",
                    characterStatus = "Активен",
                    factionOrGroup = "Красный особняк",
                    tagsCsv = "Архив, Канон, Криптография, Красный особняк",
                    gender = "Мужской",
                    speciesOrOrigin = "Человек",
                    shortDescription = "Хранитель архива Красного особняка и главный исследователь канона.",
                    detailedDescription = "Аналитичный и сдержанный хранитель подземного и библиотечного архива Красного особняка. Отвечает за сверку исторических свидетельств и сохранность реестров.",
                    birthDate = "14 ноября 1988",
                    age = "37",
                    birthPlace = "Старый город",
                    appearance = "Высокий, собранный мужчина с пронзительным взглядом и аккуратной тёмной бородкой.",
                    physique = "Худощавое, подтянутое (рост 184 см)",
                    hair = "Тёмно-каштановые с лёгкой сединой на висках",
                    eyes = "Янтарно-карие",
                    voiceDescription = "Спокойный, глубокий баритон, взвешивает каждое слово",
                    defaultClothing = "Тёмно-бордовый жилет, белая рубашка с закатанными рукавами, карманные часы.",
                    distinguishingMarks = "Тонкий шрам на левом запястье, кольцо-печатка Красного особняка.",
                    personality = "Аналитичный, сдержанный, требовательный к точности фактов, но защищающий обитателей особняка.",
                    archetype = "Мудрец / Хранитель знаний",
                    habits = "Делает пометки перьевой ручкой в кожаном блокноте, сверяет хронологию событий.",
                    speechStyle = "Размеренная, точная, без лишних слов, с лёгкой иронией.",
                    catchphrases = "«Если факт не зафиксирован в реестре, это лишь гипотеза», «Сверим первоисточники»",
                    occupation = "Хранитель архива Красного особняка",
                    skills = "Систематизация архивов, криптография, переговоры, реконструкция событий.",
                    strengths = "Энциклопедическая память, доступ ко всем уровням архива Красного особняка",
                    weaknesses = "Склонен недооценивать эмоциональные мотивы людей, редко покидает библиотеку",
                    inventorySummary = "Хронометр Корвина, кольцо-печатка архива, кожаный блокнот с шифрами",
                    interests = "Редкие рукописи, шахматы, часовые механизмы.",
                    preferences = "Крепкий чёрный кофе, тишина в библиотеке после полуночи.",
                    dislikes = "Искажение исторических документов, спешку без проверки источников",
                    goals = "Сохранить целостность канона RedWorld и предотвратить искажение памяти о событиях.",
                    motivation = "Долг перед основателями Красного особняка.",
                    internalConflict = "Необходимость скрывать опасные страницы архива ради стабильности города",
                    fearsAndLimitations = "Опасается утраты первоисточников; не принимает неподтверждённые слухи как факты.",
                    knownFacts = "Знает полную историю основания Красного особняка и содержание шифрованного реестра.",
                    personalSecrets = "Известно имя автора анонимных писем, приходящих в Красный особняк",
                    unknownFactsRestriction = "Не знает, кто именно подменил хронометр в Магазине Нины",
                    behavioralTraits = "Всегда проверяет источники перед принятием решений; не доверяет непроверенным слухам.",
                    visualPassportPrompt = "Portrait of Victor Corvin, 37 year old male archivist, sharp cheekbones, dark brown hair with silver temples, amber eyes, wearing a deep burgundy tailored vest over a crisp dark shirt, antique pocket watch chain, moody crimson library lighting, detailed concept art.",
                    savedPromptsJson = """[{"id":"prm_v1","title":"Канонический портрет в библиотеке","promptText":"Portrait of Victor Corvin, 37 year old male archivist, sharp cheekbones, dark brown hair with silver temples, amber eyes, wearing a deep burgundy tailored vest, warm amber library lamp, gothic crimson aesthetic","category":"Портрет"},{"id":"prm_v2","title":"Работа с древним манускриптом","promptText":"Full body shot of Victor Corvin standing at a mahogany desk in the Red Mansion archive, holding an antique pocket watch and cipher ledger, dramatic chiaroscuro lighting","category":"Иллюстрации игровых сцен"}]""",
                    declaredRelationshipsJson = """[{"targetName":"Елена Ростова","targetType":"CHARACTER","relationType":"Знаком с","description":"Наставник и координатор полевых расследований Елены"},{"targetName":"Красный особняк","targetType":"LOCATION","relationType":"Живёт в","description":"Хранитель архива и постоянный резидент"},{"targetName":"Катрин","targetType":"CHARACTER","relationType":"Сотрудничает","description":"Совместно управляют делами Красного особняка"}]""",
                    customFieldsJson = """{"Уровень допуска":"Высший","Любимый артефакт":"Хронометр Корвина"}""",
                    currentLocationName = "Красный особняк",
                    currentMoodAndCondition = "Изучает несоответствия в старых записях Зелёной улицы",
                    currentHealthStatus = "В норме",
                    currentOutfit = "Повседневный костюм архивариуса",
                    currentActiveGoal = "Сверить показания Елены с журналом посетителей Бара",
                    isFavorite = true,
                    canonStatus = CanonStatus.CANON.name,
                    relationshipsSummary = "Наставник и коллега Елены Ростовой; союзник Катрин и Александры.",
                    linkedLocationIdsCsv = "$locMansionId,$locGreenStId"
                )
            )

            val charElenaId = rwDao.insertCharacter(
                CharacterEntity(
                    sortOrder = 6,
                    name = "Елена Ростова",
                    fullName = "Елена Ростова",
                    aliases = "Тень Зелёной улицы, Лена",
                    storyRole = "Главный герой",
                    characterStatus = "Активен",
                    factionOrGroup = "Вольные исследователи Зелёной улицы",
                    tagsCsv = "Расследование, Курьер, Зелёная улица, Бар, Фотография",
                    gender = "Женский",
                    speciesOrOrigin = "Человек",
                    shortDescription = "Полевой исследователь и курьер редких документов на Зелёной улице.",
                    detailedDescription = "Независимая исследовательница, собирающая сведения между Магазином, Баром и Красным особняком. Умеет находить общий язык с любыми жителями города.",
                    birthDate = "03 апреля 1998",
                    age = "27",
                    birthPlace = "Восточный округ",
                    appearance = "Гибкая, стремительная девушка с выразительными чертами лица и внимательным взглядом.",
                    physique = "Атлетичное, лёгкое (рост 169 см)",
                    hair = "Чёрные, каре до плеч с багровым отливом",
                    eyes = "Серые, холодные",
                    voiceDescription = "Энергичный, чуть хрипловатый голос, переходящий на шёпот при обсуждении улик",
                    defaultClothing = "Кожаный плащ цвета спелой вишни, практичные ботинки, сумка через плечо.",
                    distinguishingMarks = "Серебряный кулон в форме ключа.",
                    personality = "Наблюдательная, независимая, склонная к риску ради установления правды.",
                    archetype = "Искатель / Детектив",
                    habits = "Садится лицом ко входу в Баре, фотографирует детали улиц.",
                    speechStyle = "Прямая, живая, использует городской сленг Зелёной улицы.",
                    catchphrases = "«На Зелёной улице стены слышат лучше архивариусов», «У меня есть снимок, который всё объяснит»",
                    occupation = "Полевой исследователь и курьер редких документов",
                    skills = "Ориентирование в городе, поиск свидетелей, анализ улик, скрытное наблюдение.",
                    strengths = "Быстро заводит знакомства, замечает скрытые улики на месте событий",
                    weaknesses = "Часто действует в обход официального протокола, рискуя собственной безопасностью",
                    inventorySummary = "Плёночная камера Leica M6, серебряный кулон-ключ, тубус для редких документов",
                    interests = "Городские легенды, аналоговая фотография, ночные прогулки.",
                    preferences = "Вишнёвый тоник в Баре, старые карты города.",
                    dislikes = "Бюрократию, закрытые двери архива, недомолвки",
                    goals = "Раскрыть тайну исчезнувшей коллекции из Магазина на Зелёной улице.",
                    motivation = "Личный поиск ответов о прошлом своей семьи.",
                    internalConflict = "Между лояльностью Виктору Корвину и желанием самостоятельно вскрыть опечатанный сектор архива",
                    fearsAndLimitations = "Не имеет прямого доступа в закрытый сектор архива без разрешения Виктора.",
                    knownFacts = "Знает маршруты курьеров Зелёной улицы и расписание встреч в Баре «Алый Бархат».",
                    personalSecrets = "Её серебряный кулон подходит к одной из старых шкатулок в Магазине Нины",
                    unknownFactsRestriction = "Не знает условий старого семейного договора Катрин",
                    behavioralTraits = "Действует самостоятельно, если считает, что официальный протокол медлит.",
                    visualPassportPrompt = "Character portrait of Elena Rostova, 27 year old female investigator, sleek black bob hair with subtle crimson tint, piercing grey eyes, wearing a dark cherry leather trench coat and silver key pendant, rainy neon reflection of Green Street at night, cinematic illustration.",
                    savedPromptsJson = """[{"id":"prm_e1","title":"Ночной портрет на Зелёной улице","promptText":"Character portrait of Elena Rostova, sleek black bob hair with crimson tint, grey eyes, dark cherry leather trench coat, silver key pendant, rainy neon Green Street background","category":"Портрет"},{"id":"prm_e2","title":"Встреча с информатором в Баре","promptText":"Cinematic scene of Elena Rostova sitting in a corner booth of the Crimson Velvet Bar, Leica camera on the table, moody burgundy lighting","category":"Иллюстрации игровых сцен"}]""",
                    declaredRelationshipsJson = """[{"targetName":"Виктор Корвин","targetType":"CHARACTER","relationType":"Знаком с","description":"Передаёт Виктору найденные архивные документы"},{"targetName":"Бар","targetType":"LOCATION","relationType":"Работает в","description":"Проводит встречи с информаторами"}]""",
                    customFieldsJson = """{"Полевой ранг":"Старший курьер","Камера":"Leica M6"}""",
                    currentLocationName = "Бар",
                    currentMoodAndCondition = "Насторожена после встречи с информатором",
                    currentHealthStatus = "В норме",
                    currentOutfit = "Дорожный плащ с поднятым воротником",
                    currentActiveGoal = "Передать найденный реестр Виктору в Красный особняк",
                    isFavorite = true,
                    canonStatus = CanonStatus.CANON.name,
                    relationshipsSummary = "Работает с Виктором Корвином; дружит с Лолой и Ниной.",
                    linkedLocationIdsCsv = "$locGreenStId,$locBarId,$locMansionId"
                )
            )

            // 4. Seed World Rules
            rwDao.insertWorldRule(
                WorldRuleEntity(
                    title = "Механика самостоятельного поведения персонажей",
                    category = "Автономия персонажей",
                    content = "Персонажи RedWorld обладают собственными целями, распорядком дня и мотивацией. Они перемещаются между локациями (Красный особняк, Зелёная улица, Магазин, Бар) в соответствии со своими задачами даже без прямого участия игрока. Изменение текущего состояния персонажа фиксируется отдельно от постоянных биографических сведений.",
                    scope = "Все персонажи и симуляция событий",
                    limitations = "Персонажи не могут знать факты, которых нет в их карточке или известных им событиях.",
                    priorityLevel = "Фундаментальный канон",
                    linkedDocumentIdsCsv = "Кодекс RedWorld: Канон и Персонажи",
                    canonStatus = CanonStatus.CANON.name
                )
            )
            rwDao.insertWorldRule(
                WorldRuleEntity(
                    title = "Законы Красного особняка и Архива",
                    category = "Общие правила",
                    content = "1. Любое изменение канонических карточек требует фиксации версии.\n2. Предположения и черновики никогда автоматически не приравниваются к канону.\n3. В пределах Красного особняка и Бара «Алый Бархат» действует запрет на открытые силовые конфликты.",
                    scope = "Красный особняк, Бар «Алый Бархат»",
                    limitations = "Нарушение нейтралитета ведёт к немедленному лишению допуска в особняк и Бар.",
                    priorityLevel = "Строгий закон",
                    linkedDocumentIdsCsv = "Регламент карточек и таблиц XLSX",
                    canonStatus = CanonStatus.CANON.name
                )
            )
            rwDao.insertWorldRule(
                WorldRuleEntity(
                    title = "Правила взаимодействия с игроком и построения сцен",
                    category = "Правила сцен",
                    content = "Каждая сцена учитывает: (а) локацию и её правила доступа, (б) текущее состояние присутствующих персонажей, (в) их отношение к игроку на основе прошлых событий. ИИ-помощник обязан опираться только на подтверждённые документы проекта.",
                    scope = "Симуляция событий и Тестирование персонажей",
                    limitations = "Запрещено автоматически перезаписывать историю событий без подтверждения пользователя.",
                    priorityLevel = "Строгий закон",
                    canonStatus = CanonStatus.CANON.name
                )
            )

            // 4.5 Seed Plotlines (Сюжет)
            rwDao.insertPlotline(
                PlotlineEntity(
                    title = "Тайна пропавшей коллекции Магазина",
                    description = "Расследование исчезновения старинных хронометров и шифровальных ведомостей, связанных с основателями Красного особняка.",
                    tasks = "1. Сверить фрагмент ведомости у Виктора.\n2. Опросить свидетелей в Баре «Алый Бархат».\n3. Проверить клеймо мастера в Магазине у Нины.",
                    developmentOptions = "Вариант А: Коллекция спрятана в закрытом крыле Красного особняка.\nВариант Б: Предметы были выкуплены через подставного курьера на Зелёной улице.",
                    involvedCharactersCsv = "Катрин, Александра, Нина, Лола, Виктор Корвин, Елена Ростова",
                    linkedLocationsCsv = "Красный особняк, Зелёная улица, Магазин, Бар",
                    status = "Активна",
                    canonStatus = CanonStatus.CANON.name
                )
            )
            rwDao.insertPlotline(
                PlotlineEntity(
                    title = "Дипломатический вечер в Красном особняке",
                    description = "Катрин и Александра готовят закрытый приём, на котором должны встретиться представители старых семей города и хранители архива.",
                    tasks = "1. Утвердить список гостей.\n2. Проверить систему безопасности особняка.\n3. Обеспечить нейтралитет сторон.",
                    developmentOptions = "Вариант А: Мирный договор и открытие части архива.\nВариант Б: Неожиданное разоблачение одного из гостей во время приёма.",
                    involvedCharactersCsv = "Катрин, Александра, Виктор Корвин",
                    linkedLocationsCsv = "Красный особняк",
                    status = "В разработке",
                    canonStatus = CanonStatus.CANON.name
                )
            )

            // 5. Seed Events (История событий)
            val eventArrivalId = rwDao.insertEvent(
                WorldEventEntity(
                    title = "Передача шифрованного реестра на Зелёной улице",
                    eventType = "Ключевое событие",
                    timelineDate = "День 12, 21:40",
                    chronologyOrder = 1,
                    locationName = "Бар",
                    participantNamesCsv = "Елена Ростова, Лола, Виктор Корвин",
                    description = "В угловой кабинке Бара на Зелёной улице Елена при содействии Лолы получила фрагмент старой ведомости Магазина и доставила его в архив Красного особняка.",
                    causes = "Обнаружение пропажи редких часов из мастерской Нины на Зелёной улице.",
                    consequences = "Уровень доверия между Виктором и Еленой укрепился; открыт доступ к проверке архива Магазина.",
                    linkedEventsCsv = "Ревизия архива Красного особняка",
                    linkedPlotlinesCsv = "Тайна пропавшей коллекции Магазина",
                    relatedProjectLore = "Законы Красного особняка и Архива",
                    isDraftSimulation = false,
                    canonStatus = CanonStatus.CANON.name
                )
            )

            // 6. Seed Items (Объекты)
            rwDao.insertItem(
                WorldItemEntity(
                    name = "Реестр Красного особняка (XLSX-каталог)",
                    itemType = "Артефакт / Документ",
                    ownerOrLeader = "Виктор Корвин",
                    locationName = "Красный особняк",
                    description = "Структурированная ведомость всех резидентов, комнат и правил доступа Красного особняка.",
                    properties = "Хранится в библиотеке; вынос за пределы особняка запрещён уставом.",
                    linkedEventsCsv = "Передача шифрованного реестра на Зелёной улице",
                    canonStatus = CanonStatus.CANON.name
                )
            )
            rwDao.insertItem(
                WorldItemEntity(
                    name = "Хронометр мастера Корвина",
                    itemType = "Механический артефакт",
                    ownerOrLeader = "Нина",
                    locationName = "Магазин",
                    description = "Старинные карманные часы с двойным циферблатом и внутренним шифровальным диском.",
                    properties = "Открывает сейфовую ячейку в подземном архиве; требует специального ключа.",
                    linkedEventsCsv = "Передача шифрованного реестра на Зелёной улице",
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
                    sourceId = 1L,
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
                    content = "Добро пожаловать в **World AI Studio**! Я подключён к локальной базе знаний активного проекта.\n\nАктивные источники в контексте:\n- `Кодекс RedWorld: Канон и Персонажи`\n- `Регламент карточек и таблиц XLSX`\n\nВы можете переключаться между проектами, импортировать и анализировать файлы в собственном разделе «Документы» и «Заметки», запускать симуляцию событий и тестирование отыгрыша персонажей через кнопку Глобуса.",
                    citedSourcesJson = """[{"documentId":$doc1Id,"title":"Кодекс RedWorld: Канон и Персонажи","section":"География и ключевые точки"}]""",
                    modelUsed = "gemini-3.5-flash"
                )
            )

            // 11. Seed Second Project (Эфириум) starter entities so multi-project switching has immediate content
            val locObsId = rwDao.insertLocation(
                LocationEntity(
                    projectId = projAetherId,
                    name = "Небесная Обсерватория",
                    category = "Цитадель",
                    description = "Главная башня проекта «Эфириум», парящая над облачным поясом.",
                    atmosphere = "Звёздный свет, латунные астролябии и гул эфирных реакторов.",
                    canonStatus = CanonStatus.CANON.name
                )
            )
            val charAuronId = rwDao.insertCharacter(
                CharacterEntity(
                    projectId = projAetherId,
                    name = "Аурон Велес",
                    aliases = "Первый Навигатор",
                    birthDate = "07 июля 3012",
                    age = "34",
                    birthPlace = "Верхний Шпиль",
                    occupation = "Главный астроном Небесной Обсерватории",
                    appearance = "Высокий исследователь в тёмно-синем плаще с золотым шитьём.",
                    hair = "Пепельно-белые",
                    eyes = "Золотистые",
                    defaultClothing = "Мундир навигатора, эфирный компас.",
                    personality = "Вдохновенный, точный, увлечённый картографией созвездий.",
                    goals = "Найти потерянный сектор Первого Архива.",
                    currentLocationName = "Небесная Обсерватория",
                    currentMoodAndCondition = "Настраивает главный телескоп",
                    visualPassportPrompt = "Portrait of Auron Veles, 34 year old celestial navigator, silver-white hair, golden eyes, wearing a midnight blue and gold embroidered coat inside a brass celestial observatory, concept art",
                    savedPromptsJson = """[{"id":"prm_au1","title":"Портрет у телескопа","promptText":"Portrait of Auron Veles, silver-white hair, golden eyes, brass celestial observatory background, warm starlight","category":"Портрет"}]""",
                    declaredRelationshipsJson = """[{"targetName":"Небесная Обсерватория","targetType":"LOCATION","relationType":"Работает в","description":"Руководит наблюдениями"}]""",
                    canonStatus = CanonStatus.CANON.name,
                    linkedLocationIdsCsv = "$locObsId"
                )
            )
            rwDao.insertRelationship(
                RelationshipEntity(
                    projectId = projAetherId,
                    sourceType = "CHARACTER",
                    sourceId = charAuronId,
                    sourceLabel = "Аурон Велес",
                    targetType = "LOCATION",
                    targetId = locObsId,
                    targetLabel = "Небесная Обсерватория",
                    relationType = "Персонаж работает в локации",
                    description = "Главный астроном Обсерватории",
                    sourceX = 300f,
                    sourceY = 260f,
                    targetX = 580f,
                    targetY = 260f
                )
            )

            // 12. Seed Google NotebookLM Synced Notebooks (available upon Google Account connection)
            rwDao.insertNotebookLmNotebook(
                NotebookLmNotebookEntity(
                    googleAccountEmail = "hobot.mc@gmail.com",
                    notebookTitle = "NotebookLM: Архив Красного особняка и Зелёной улицы",
                    notebookCategory = "Канон и Персонажи",
                    sourcesCount = 6,
                    audioOverviewSummary = "Аудио-сводка NotebookLM (08:42): Разбор связей между Виктором Корвином, Еленой Ростовой, часовщиком Маркусом Вейном и хозяйкой Бара Лидией Морган.",
                    notesJson = """[
                        {
                            "id": "nlm_1",
                            "title": "Досье из NotebookLM: Маркус Вейн (Часовщик Зелёной улицы)",
                            "content": "Имя: Маркус Вейн\nПсевдонимы: Часовщик Зелёной улицы, Старый Маркус\nДата рождения: 22 августа 1974\nВозраст: 51\nРабота: Владелец антикварного Магазина на Зелёной улице\nВнешность: Коренастый мастер с благородной сединой и изумрудными глазами за тонкими очками.\nХарактер: Прагматичный, рассудительный, хранит секреты квартала.\nЦели: Восстановить башенные часы Красного особняка.\nЛокация сейчас: Магазин\nПаспорт внешности: Portrait of Marcus Vane, 51 year old watchmaker, silver-streaked hair, emerald eyes behind brass spectacles, antique shop lighting.\nСвязь: Виктор Корвин | CHARACTER | Знаком с | Консультант Хранителя архива по старинным механизмам\nСвязь: Елена Ростова | CHARACTER | Друг | Снабжает Елену редким снаряжением\nСвязь: Магазин | LOCATION | Работает в | Владелец антикварной лавки",
                            "suggestedIntegrationType": "CHARACTER",
                            "sourceCitation": "Google NotebookLM • Источник: Записи Зелёной улицы.pdf"
                        },
                        {
                            "id": "nlm_2",
                            "title": "Тайный подземный переход под Красным особняком",
                            "content": "Скрытая галерея, соединяющая подземный архив Красного особняка с задней мастерской Магазина на Зелёной улице. Открывается только парным ключом Хранителя и Часовщика.",
                            "suggestedIntegrationType": "LOCATION",
                            "sourceCitation": "Google NotebookLM • Источник: Чертежи фундамента 1912.docx"
                        },
                        {
                            "id": "nlm_3",
                            "title": "Закон о неприкосновенности курьеров Зелёной улицы",
                            "content": "Любой полевой исследователь или курьер, имеющий серебряный жетон-ключ, пользуется правом безопасного прохода между Красным особняком, Магазином и Баром «Алый Бархат».",
                            "suggestedIntegrationType": "WORLD_RULE",
                            "sourceCitation": "Google NotebookLM • Источник: Кодекс нейтралитета.md"
                        },
                        {
                            "id": "nlm_4",
                            "title": "Сводка NotebookLM: Ночной совет в Баре «Алый Бархат»",
                            "content": "На закрытой встрече в угловой кабинке Бара Виктор Корвин, Елена Ростова и Маркус Вейн сопоставили страницы пропавшего реестра и обнаружили печать старого ордена.",
                            "suggestedIntegrationType": "EVENT",
                            "sourceCitation": "Google NotebookLM • Источник: Хроника Дня 14"
                        }
                    ]""".trimIndent()
                )
            )
            rwDao.insertNotebookLmNotebook(
                NotebookLmNotebookEntity(
                    googleAccountEmail = "hobot.mc@gmail.com",
                    notebookTitle = "NotebookLM: Концепты миров и визуальные референсы",
                    notebookCategory = "Миростроение и Арт",
                    sourcesCount = 4,
                    audioOverviewSummary = "Аудио-сводка NotebookLM (06:15): Сравнение эстетики готического нуара RedWorld и небесных обсерваторий Эфириума.",
                    notesJson = """[
                        {
                            "id": "nlm_5",
                            "title": "Досье из NotebookLM: Лидия Морган (Хозяйка Бара)",
                            "content": "Имя: Лидия Морган\nПсевдонимы: Мадам Лидия, Хозяйка «Алого Бархата»\nДата рождения: 19 февраля 1990\nВозраст: 35\nРабота: Владелица ночного Бара на Зелёной улице\nВнешность: Высокая элегантная женщина с тёмно-каштановыми волнистыми волосами и карими глазами с золотистой искрой.\nОдежда: Вечернее платье цвета тёмного бордо и брошь с гранатом.\nХарактер: Дипломатичная, проницательная, строго хранит нейтралитет заведения.\nЛокация сейчас: Бар\nПаспорт внешности: Portrait of Lidia Morgan, 35 year old owner of the Crimson Velvet Bar, dark wavy chestnut hair, deep burgundy velvet dress, garnet brooch, jazz bar lighting.\nСвязь: Маркус Вейн | CHARACTER | Коллега | Обменивается редкими пластинками и письмами\nСвязь: Елена Ростова | CHARACTER | Знаком с | Предоставляет звукоизолированную кабинку в Баре\nСвязь: Виктор Корвин | CHARACTER | Знаком с | Соблюдает пакт о нейтралитете\nСвязь: Бар | LOCATION | Работает в | Владелица и управляющая Бара",
                            "suggestedIntegrationType": "CHARACTER",
                            "sourceCitation": "Google NotebookLM • Источник: Персонажи Бара.docx"
                        },
                        {
                            "id": "nlm_6",
                            "title": "Методика RAG-проверки канона при генерации сцен",
                            "content": "Перед утверждением новой сюжетной сцены ИИ-куратор обязан сверить текущую локацию каждого участника со Слоем 2 (Текущее состояние), чтобы избежать одновременного присутствия персонажа в двух разных местах.",
                            "suggestedIntegrationType": "KNOWLEDGE",
                            "sourceCitation": "Google NotebookLM • Источник: Архитектура памяти ИИ.pdf"
                        }
                    ]""".trimIndent()
                )
            )
        }
    }
}
