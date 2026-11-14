package com.example.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.example.data.local.CurriculumNodeEntity
import com.example.data.local.CurriculumQuizEntity
import com.example.data.local.MriDatabase
import com.example.data.local.MriModuleEntity
import com.example.data.local.NodeProgressEntity
import com.example.data.local.QuizAttemptEntity
import com.example.data.local.StudyDayEntity
import com.example.data.local.StudyNoteEntity
import com.example.data.local.normalizeLegacyNodeCodes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
private val seedMutex = Mutex()

data class ArcadeStats(
    val bestScreening: Int = 0,
    val bestLarmor: Int = 0,
    val bestArtifact: Int = 0,
    val minigameRounds: Int = 0,
    val bestDefense: Int = 0,
    val bestRhythm: Int = 0,
    val highestRank: String = "Magang Zona II"
)

class UserDataRepository(
    context: Context,
    private val database: MriDatabase = MriDatabase.getDatabase(context)
) {
    private val dataStore = context.applicationContext.dataStore

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AVATAR_ID = stringPreferencesKey("user_avatar_id")
        val SEEDED_NOTES = booleanPreferencesKey("seeded_notes")
        val BOOKMARKED_NODES = stringSetPreferencesKey("bookmarked_nodes")
        val BOOKMARKED_GLOSSARY = stringSetPreferencesKey("bookmarked_glossary")
        val MASTERED_FLASHCARDS = stringSetPreferencesKey("mastered_flashcards")
        val LAST_VISITED_NODE_CODE = stringPreferencesKey("last_visited_node_code")
        val LEGACY_CODES_V1 = booleanPreferencesKey("legacy_node_codes_normalized_v1")
        val CURRICULUM_FINGERPRINT = stringPreferencesKey("curriculum_seed_fingerprint")
        val CURRICULUM_VERSION = intPreferencesKey("curriculum_version")
        val EQUIPPED_HAIR = stringPreferencesKey("equipped_hair")
        val EQUIPPED_UNIFORM = stringPreferencesKey("equipped_uniform")
        val EQUIPPED_HEADSET = stringPreferencesKey("equipped_headset")
        val EQUIPPED_ACCESSORY = stringPreferencesKey("equipped_accessory")
        val BACKGROUND_ID = stringPreferencesKey("background_id")
        val HAS_SEEN_MAP_GUIDE = booleanPreferencesKey("has_seen_map_guide")
        val CELEBRATED_MODULE_IDS = stringSetPreferencesKey("celebrated_module_ids")
        val CELEBRATED_MODULES_INITIALIZED = booleanPreferencesKey("celebrated_modules_initialized")
        val TRIED_LAB_NODES = stringSetPreferencesKey("tried_lab_nodes")
        val DAILY_SHIFT_DATE = stringPreferencesKey("daily_shift_date")
        val DAILY_SHIFT_NODES = intPreferencesKey("daily_shift_nodes")
        val DAILY_SHIFT_QUIZZES = intPreferencesKey("daily_shift_quizzes")
        val DAILY_SHIFT_LABS = intPreferencesKey("daily_shift_labs")
        val MINIGAME_BEST_SCREENING = intPreferencesKey("minigame_best_screening")
        val MINIGAME_BEST_LARMOR = intPreferencesKey("minigame_best_larmor")
        val MINIGAME_BEST_ARTIFACT = intPreferencesKey("minigame_best_artifact")
        val MINIGAME_ROUNDS_COMPLETED = intPreferencesKey("minigame_rounds_completed")
        val ARCADE_HIGH_SCORE_DEFENSE = intPreferencesKey("arcade_high_score_defense")
        val ARCADE_HIGH_SCORE_RHYTHM = intPreferencesKey("arcade_high_score_rhythm")
        val ARCADE_HIGHEST_RANK = stringPreferencesKey("arcade_highest_rank")
        val PREFERRED_READER_MODE = stringPreferencesKey("preferred_reader_mode")
        val LESSON_TAP_HINT_COUNT = intPreferencesKey("lesson_tap_hint_count")
        val MANUAL_REVIEW_NODES = stringSetPreferencesKey("manual_review_nodes")
        val ALLOW_LESSON_DRAFTS = booleanPreferencesKey("allow_lesson_drafts")

        fun lessonStepKey(nodeCode: String): Preferences.Key<Int> {
            val norm = LegacyNodeCodes.normalize(nodeCode.trim())
            return intPreferencesKey("lesson_step_$norm")
        }
    }

    suspend fun normalizeLegacyNodeCodesIfNeeded() {
        val done = dataStore.data.first()[PreferencesKeys.LEGACY_CODES_V1] ?: false
        if (done) return
        database.normalizeLegacyNodeCodes()
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.BOOKMARKED_NODES]?.let { current ->
                val mapped = LegacyNodeCodes.normalizeAll(current)
                if (mapped != current) prefs[PreferencesKeys.BOOKMARKED_NODES] = mapped
            }
            prefs[PreferencesKeys.LAST_VISITED_NODE_CODE]?.let { last ->
                val mapped = LegacyNodeCodes.normalize(last)
                if (mapped != last) prefs[PreferencesKeys.LAST_VISITED_NODE_CODE] = mapped
            }
            prefs[PreferencesKeys.LEGACY_CODES_V1] = true
        }
    }

    val lastVisitedNodeCodeFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_VISITED_NODE_CODE]
    }

    val userNameFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_NAME] ?: ""
    }

    val userAvatarIdFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_AVATAR_ID] ?: "radiografer_mri"
    }

    val hasSeenMapGuideFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_SEEN_MAP_GUIDE] ?: false
    }

    suspend fun setHasSeenMapGuide(seen: Boolean = true) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_MAP_GUIDE] = seen
        }
    }

    val celebratedModuleIdsFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CELEBRATED_MODULE_IDS] ?: emptySet()
    }

    suspend fun markModulesCelebrated(moduleIds: Set<String>) {
        if (moduleIds.isEmpty()) return
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.CELEBRATED_MODULE_IDS] ?: emptySet()
            preferences[PreferencesKeys.CELEBRATED_MODULE_IDS] = current + moduleIds
            preferences[PreferencesKeys.CELEBRATED_MODULES_INITIALIZED] = true
        }
    }

    suspend fun ensureCelebratedModulesInitialized(alreadyCompletedModuleIds: Set<String>) {
        val initialized = dataStore.data.first()[PreferencesKeys.CELEBRATED_MODULES_INITIALIZED] ?: false
        if (initialized) return
        dataStore.edit { preferences ->
            if (alreadyCompletedModuleIds.isNotEmpty()) {
                val current = preferences[PreferencesKeys.CELEBRATED_MODULE_IDS] ?: emptySet()
                preferences[PreferencesKeys.CELEBRATED_MODULE_IDS] = current + alreadyCompletedModuleIds
            }
            preferences[PreferencesKeys.CELEBRATED_MODULES_INITIALIZED] = true
        }
    }

    val triedLabNodesFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TRIED_LAB_NODES] ?: emptySet()
    }

    suspend fun markLabNodeTried(nodeCode: String) {
        val norm = LegacyNodeCodes.normalize(nodeCode)
        if (norm.isBlank()) return
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.TRIED_LAB_NODES] ?: emptySet()
            preferences[PreferencesKeys.TRIED_LAB_NODES] = current + norm
        }
    }

    val preferredReaderModeFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.PREFERRED_READER_MODE] ?: "LESSON"
    }

    suspend fun setPreferredReaderMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.PREFERRED_READER_MODE] = mode
        }
    }

    val lessonTapHintCountFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LESSON_TAP_HINT_COUNT] ?: 0
    }

    suspend fun incrementLessonTapHintCount() {
        dataStore.edit { prefs ->
            val cur = prefs[PreferencesKeys.LESSON_TAP_HINT_COUNT] ?: 0
            if (cur < 10) {
                prefs[PreferencesKeys.LESSON_TAP_HINT_COUNT] = cur + 1
            }
        }
    }

    val allowLessonDraftsFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ALLOW_LESSON_DRAFTS] ?: false
    }

    suspend fun setAllowLessonDrafts(allow: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.ALLOW_LESSON_DRAFTS] = allow
        }
    }

    fun observeSavedLessonStep(nodeCode: String): Flow<Int> {
        val key = PreferencesKeys.lessonStepKey(nodeCode)
        return dataStore.data.map { prefs -> prefs[key] ?: 0 }
    }

    suspend fun saveLessonStep(nodeCode: String, stepIndex: Int) {
        val norm = LegacyNodeCodes.normalize(nodeCode.trim())
        if (norm.isBlank()) return
        val key = PreferencesKeys.lessonStepKey(norm)
        dataStore.edit { prefs ->
            if (stepIndex <= 0) {
                prefs.remove(key)
            } else {
                prefs[key] = stepIndex
            }
        }
    }

    suspend fun clearLessonStep(nodeCode: String) {
        val norm = LegacyNodeCodes.normalize(nodeCode.trim())
        if (norm.isBlank()) return
        val key = PreferencesKeys.lessonStepKey(norm)
        dataStore.edit { prefs ->
            prefs.remove(key)
        }
    }

    val manualReviewNodesFlow: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.MANUAL_REVIEW_NODES] ?: emptySet()
    }

    suspend fun setNodeManualReview(nodeCode: String, enabled: Boolean) {
        val norm = LegacyNodeCodes.normalize(nodeCode.trim())
        if (norm.isBlank()) return
        dataStore.edit { prefs ->
            val cur = prefs[PreferencesKeys.MANUAL_REVIEW_NODES] ?: emptySet()
            prefs[PreferencesKeys.MANUAL_REVIEW_NODES] = if (enabled) cur + norm else cur - norm
        }
    }

    suspend fun recordLessonQuizAttempt(
        quizId: String,
        rawNodeCode: String,
        explicitModuleId: String? = null,
        selectedIndex: Int,
        isCorrect: Boolean
    ) {
        val normCode = LegacyNodeCodes.normalize(rawNodeCode.trim())
        if (quizId.isBlank() || normCode.isBlank()) return
        val resolvedModuleId = explicitModuleId?.takeIf { it.isNotBlank() } ?: resolveModuleIdFromCode(normCode)
        database.quizAttemptDao().insert(
            QuizAttemptEntity(
                id = 0,
                quizId = quizId.trim(),
                nodeCode = normCode,
                moduleId = resolvedModuleId,
                selectedIndex = selectedIndex,
                isCorrect = isCorrect,
                answeredAt = System.currentTimeMillis()
            )
        )
    }

    fun observeDailyShiftCounts(todayKey: String): Flow<Triple<Int, Int, Int>> = dataStore.data.map { prefs ->
        if (prefs[PreferencesKeys.DAILY_SHIFT_DATE] == todayKey) {
            Triple(
                prefs[PreferencesKeys.DAILY_SHIFT_NODES] ?: 0,
                prefs[PreferencesKeys.DAILY_SHIFT_QUIZZES] ?: 0,
                prefs[PreferencesKeys.DAILY_SHIFT_LABS] ?: 0
            )
        } else {
            Triple(0, 0, 0)
        }
    }

    suspend fun incrementDailyShift(todayKey: String, nodesDelta: Int = 0, quizzesDelta: Int = 0, labsDelta: Int = 0) {
        dataStore.edit { prefs ->
            val sameDay = prefs[PreferencesKeys.DAILY_SHIFT_DATE] == todayKey
            val curN = if (sameDay) (prefs[PreferencesKeys.DAILY_SHIFT_NODES] ?: 0) else 0
            val curQ = if (sameDay) (prefs[PreferencesKeys.DAILY_SHIFT_QUIZZES] ?: 0) else 0
            val curL = if (sameDay) (prefs[PreferencesKeys.DAILY_SHIFT_LABS] ?: 0) else 0
            prefs[PreferencesKeys.DAILY_SHIFT_DATE] = todayKey
            prefs[PreferencesKeys.DAILY_SHIFT_NODES] = curN + nodesDelta
            prefs[PreferencesKeys.DAILY_SHIFT_QUIZZES] = curQ + quizzesDelta
            prefs[PreferencesKeys.DAILY_SHIFT_LABS] = curL + labsDelta
        }
    }

    val arcadeStatsFlow: Flow<ArcadeStats> = dataStore.data.map { prefs ->
        ArcadeStats(
            bestScreening = prefs[PreferencesKeys.MINIGAME_BEST_SCREENING] ?: 0,
            bestLarmor = prefs[PreferencesKeys.MINIGAME_BEST_LARMOR] ?: 0,
            bestArtifact = prefs[PreferencesKeys.MINIGAME_BEST_ARTIFACT] ?: 0,
            minigameRounds = prefs[PreferencesKeys.MINIGAME_ROUNDS_COMPLETED] ?: 0,
            bestDefense = prefs[PreferencesKeys.ARCADE_HIGH_SCORE_DEFENSE] ?: 0,
            bestRhythm = prefs[PreferencesKeys.ARCADE_HIGH_SCORE_RHYTHM] ?: 0,
            highestRank = prefs[PreferencesKeys.ARCADE_HIGHEST_RANK] ?: "Magang Zona II"
        )
    }

    private fun rankLevel(rankTitle: String): Int = when {
        rankTitle.contains("Master", ignoreCase = true) || rankTitle.contains("Utama", ignoreCase = true) -> 3
        rankTitle.contains("Kilat", ignoreCase = true) || rankTitle.contains("Senior", ignoreCase = true) -> 2
        rankTitle.contains("Penjaga", ignoreCase = true) || rankTitle.contains("Siaga", ignoreCase = true) -> 1
        else -> 0
    }

    suspend fun recordMinigameResult(kindKey: String, score: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.MINIGAME_ROUNDS_COMPLETED] =
                (prefs[PreferencesKeys.MINIGAME_ROUNDS_COMPLETED] ?: 0) + 1
            val key = when (kindKey) {
                "ZONE_SCREENING" -> PreferencesKeys.MINIGAME_BEST_SCREENING
                "LARMOR_TAP" -> PreferencesKeys.MINIGAME_BEST_LARMOR
                else -> PreferencesKeys.MINIGAME_BEST_ARTIFACT
            }
            val prev = prefs[key] ?: 0
            if (score > prev) prefs[key] = score

            val earnedRank = when {
                score >= 900 -> "Master Superkonduktor"
                score >= 550 -> "Radiografer Kilat"
                score >= 250 -> "Penjaga 5 Gauss"
                else -> "Magang Zona II"
            }
            val prevRank = prefs[PreferencesKeys.ARCADE_HIGHEST_RANK] ?: "Magang Zona II"
            if (rankLevel(earnedRank) >= rankLevel(prevRank)) {
                prefs[PreferencesKeys.ARCADE_HIGHEST_RANK] = earnedRank
            }
        }
    }

    suspend fun recordArcadeResult(kindKey: String, score: Int, rankTitle: String) {
        dataStore.edit { prefs ->
            val key = if (kindKey == "MAGNET_DEFENSE") {
                PreferencesKeys.ARCADE_HIGH_SCORE_DEFENSE
            } else {
                PreferencesKeys.ARCADE_HIGH_SCORE_RHYTHM
            }
            val prev = prefs[key] ?: 0
            if (score > prev) {
                prefs[key] = score
            }
            val prevRank = prefs[PreferencesKeys.ARCADE_HIGHEST_RANK] ?: "Magang Zona II"
            if (rankLevel(rankTitle) >= rankLevel(prevRank)) {
                prefs[PreferencesKeys.ARCADE_HIGHEST_RANK] = rankTitle
            }
        }
    }

    val characterCustomizationFlow: Flow<CharacterCustomizationState> = dataStore.data.map { prefs ->
        CharacterCustomizationState(
            avatarId = prefs[PreferencesKeys.USER_AVATAR_ID] ?: "radiografer_mri",
            equippedHair = prefs[PreferencesKeys.EQUIPPED_HAIR] ?: "hair_default",
            equippedUniform = prefs[PreferencesKeys.EQUIPPED_UNIFORM] ?: "uniform_scrub_mint",
            equippedHeadset = prefs[PreferencesKeys.EQUIPPED_HEADSET] ?: "headset_radio",
            equippedAccessory = prefs[PreferencesKeys.EQUIPPED_ACCESSORY] ?: "acc_magnet_detector",
            backgroundId = prefs[PreferencesKeys.BACKGROUND_ID] ?: "bg_control_room"
        )
    }

    suspend fun updateCharacterCustomization(state: CharacterCustomizationState) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.USER_AVATAR_ID] = state.avatarId
            prefs[PreferencesKeys.EQUIPPED_HAIR] = state.equippedHair
            prefs[PreferencesKeys.EQUIPPED_UNIFORM] = state.equippedUniform
            prefs[PreferencesKeys.EQUIPPED_HEADSET] = state.equippedHeadset
            prefs[PreferencesKeys.EQUIPPED_ACCESSORY] = state.equippedAccessory
            prefs[PreferencesKeys.BACKGROUND_ID] = state.backgroundId
        }
    }

    val seededNotesFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SEEDED_NOTES] ?: false
    }

    val bookmarkedNodesFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BOOKMARKED_NODES] ?: emptySet()
    }

    val bookmarkedGlossaryFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BOOKMARKED_GLOSSARY] ?: emptySet()
    }

    val masteredFlashcardsFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.MASTERED_FLASHCARDS] ?: emptySet()
    }

    suspend fun updateUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }

    suspend fun updateUserAvatarId(avatarId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_AVATAR_ID] = avatarId
        }
    }

    suspend fun setLastVisitedNodeCode(code: String) {
        dataStore.edit { preferences ->
            if (preferences[PreferencesKeys.LAST_VISITED_NODE_CODE] != code) {
                preferences[PreferencesKeys.LAST_VISITED_NODE_CODE] = code
            }
        }
    }

    suspend fun setSeededNotes(seeded: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEEDED_NOTES] = seeded
        }
    }

    suspend fun toggleNodeBookmark(nodeCode: String) {
        val normalizedCode = LegacyNodeCodes.normalize(nodeCode.trim())
        if (normalizedCode.isBlank()) return
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.BOOKMARKED_NODES] ?: emptySet()
            preferences[PreferencesKeys.BOOKMARKED_NODES] = if (current.contains(normalizedCode) || current.contains(nodeCode)) {
                current - normalizedCode - nodeCode
            } else {
                current + normalizedCode
            }
        }
    }

    suspend fun removeNodeBookmark(nodeCode: String) {
        val normalizedCode = LegacyNodeCodes.normalize(nodeCode.trim())
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.BOOKMARKED_NODES] ?: emptySet()
            preferences[PreferencesKeys.BOOKMARKED_NODES] = current - normalizedCode - nodeCode
        }
    }

    suspend fun toggleGlossaryBookmark(term: String) {
        val cleanTerm = term.trim()
        if (cleanTerm.isBlank()) return
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.BOOKMARKED_GLOSSARY] ?: emptySet()
            preferences[PreferencesKeys.BOOKMARKED_GLOSSARY] = if (current.contains(cleanTerm) || current.contains(term)) {
                current - cleanTerm - term
            } else {
                current + cleanTerm
            }
        }
    }

    suspend fun removeGlossaryBookmark(term: String) {
        val cleanTerm = term.trim()
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.BOOKMARKED_GLOSSARY] ?: emptySet()
            preferences[PreferencesKeys.BOOKMARKED_GLOSSARY] = current - cleanTerm - term
        }
    }

    suspend fun clearAllBookmarks() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BOOKMARKED_NODES] = emptySet()
            preferences[PreferencesKeys.BOOKMARKED_GLOSSARY] = emptySet()
        }
    }

    suspend fun toggleFlashcardMastered(cardId: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.MASTERED_FLASHCARDS] ?: emptySet()
            preferences[PreferencesKeys.MASTERED_FLASHCARDS] = if (current.contains(cardId)) {
                current - cardId
            } else {
                current + cardId
            }
        }
    }

    // Curriculum & Quiz Seeding + Queries (CurriculumDao & MriCurriculumDao)
    suspend fun seedCurriculumIfNeeded() {
        seedMutex.withLock {
            val snapshot = CurriculumSnapshot.build()
            if (!snapshot.isComplete) {
                Log.w("CurriculumSeed", "data kurikulum kosong, seed dilewati")
                return
            }
            val fingerprint = snapshot.fingerprint()
            val prefs = dataStore.data.first()
            val stored = prefs[PreferencesKeys.CURRICULUM_FINGERPRINT]
            val storedVersion = prefs[PreferencesKeys.CURRICULUM_VERSION] ?: 0
            val targetVersion = 4
            if (storedVersion >= targetVersion && stored == fingerprint && database.isCurriculumPresent(snapshot)) {
                Log.i("CurriculumSeed", "seed dilewati (versi & fingerprint sama)")
                return
            }
            database.replaceCurriculum(snapshot)
            dataStore.edit {
                it[PreferencesKeys.CURRICULUM_FINGERPRINT] = fingerprint
                it[PreferencesKeys.CURRICULUM_VERSION] = targetVersion
            }
            Log.i("CurriculumSeed", "seed selesai / diperbarui ke versi $targetVersion")
        }
    }

    suspend fun seedCurriculumData() {
        seedCurriculumIfNeeded()
    }

    fun observeAllModules(): Flow<List<MriModuleEntity>> =
        database.mriCurriculumDao().observeAllModules()

    fun observeCurriculumNodes(): Flow<List<CurriculumNodeEntity>> =
        database.curriculumDao().observeAllNodes()

    fun observeCurriculumNodesByModule(moduleId: String): Flow<List<CurriculumNodeEntity>> =
        database.curriculumDao().observeNodesByModule(moduleId)

    fun observeCurriculumNodesByModuleAndLevel(moduleId: String, level: Int): Flow<List<CurriculumNodeEntity>> =
        database.curriculumDao().observeNodesByModuleAndLevel(moduleId, level)

    fun observeCurriculumQuizzes(): Flow<List<CurriculumQuizEntity>> =
        database.curriculumDao().observeAllQuizzes()

    fun observeCurriculumQuizzesByModule(moduleId: String): Flow<List<CurriculumQuizEntity>> =
        database.curriculumDao().observeQuizzesByModule(moduleId)

    fun observeCurriculumQuizzesByModuleAndLevel(moduleId: String, level: Int): Flow<List<CurriculumQuizEntity>> =
        database.curriculumDao().observeQuizzesByModuleAndLevel(moduleId, level)

    // Node Progress DAO
    fun observeCompletedNodes(): Flow<List<NodeProgressEntity>> =
        database.nodeProgressDao().observeCompletedNodes()

    fun observeCompletedNodesByModule(moduleId: String): Flow<List<NodeProgressEntity>> =
        database.nodeProgressDao().observeCompletedNodesByModule(moduleId)

    fun observeCompletedNodesByPrefix(prefix: String): Flow<List<NodeProgressEntity>> =
        database.nodeProgressDao().observeCompletedNodesByPrefix(prefix)

    suspend fun insertNodeProgress(entity: NodeProgressEntity) =
        database.nodeProgressDao().insert(entity)

    suspend fun deleteNodeProgress(nodeCode: String) =
        database.nodeProgressDao().delete(nodeCode)

    suspend fun getCompletedNodesCountByModule(moduleId: String): Int =
        database.nodeProgressDao().getCountByModule(moduleId)

    // Study Notes DAO
    fun observeNotes(): Flow<List<StudyNoteEntity>> =
        database.studyNoteDao().observeNotes()

    fun observeNotesByModule(moduleId: String): Flow<List<StudyNoteEntity>> =
        database.studyNoteDao().observeNotesByModule(moduleId)

    fun observeNotesByNode(nodeCode: String): Flow<List<StudyNoteEntity>> =
        database.studyNoteDao().observeNotesByNode(nodeCode)

    suspend fun insertNote(entity: StudyNoteEntity) =
        database.studyNoteDao().insert(entity)

    suspend fun getNoteById(id: String): StudyNoteEntity? =
        database.studyNoteDao().getById(id)

    suspend fun deleteNote(id: String) =
        database.studyNoteDao().delete(id)

    suspend fun getNotesCount(): Int =
        database.studyNoteDao().getCount()

    // Quiz Attempts DAO
    fun observeQuizAttempts(): Flow<List<QuizAttemptEntity>> =
        database.quizAttemptDao().observeQuizAttempts()

    fun observeQuizAttemptsByModule(moduleId: String): Flow<List<QuizAttemptEntity>> =
        database.quizAttemptDao().observeQuizAttemptsByModule(moduleId)

    fun observeQuizAttemptsByNode(nodeCode: String): Flow<List<QuizAttemptEntity>> =
        database.quizAttemptDao().observeQuizAttemptsByNode(nodeCode)

    suspend fun insertQuizAttempt(entity: QuizAttemptEntity) =
        database.quizAttemptDao().insert(entity)

    suspend fun getCorrectQuizAttemptsCountByModule(moduleId: String): Int =
        database.quizAttemptDao().getCorrectCountByModule(moduleId)

    // Study Days DAO
    fun observeStudyDays(): Flow<List<StudyDayEntity>> =
        database.studyDayDao().observeStudyDays()

    suspend fun insertStudyDay(entity: StudyDayEntity) =
        database.studyDayDao().insert(entity)

    suspend fun getStudyDaysCount(): Int =
        database.studyDayDao().getCount()

    /**
     * Memperbarui baris `study_days` secara non-destruktif (B-03):
     * - Menghitung jumlah simpul yang benar-benar diselesaikan pada tanggal `dateIso` dari `node_progress`.
     * - Mempertahankan `lastNodeCode` dan `lastModuleId` yang sudah ada jika pemanggil tidak mengirim nilai baru
     *   (misalnya saat menandai flashcard dikuasai).
     */
    suspend fun recordStudyActivity(
        dateIso: String,
        lastNodeCode: String? = null,
        lastModuleId: String? = null,
        flashcardActivity: Boolean = false
    ) {
        database.withTransaction {
            upsertStudyDayInternal(dateIso, lastNodeCode, lastModuleId, flashcardActivity)
        }
    }

    private suspend fun upsertStudyDayInternal(
        dateIso: String,
        lastNodeCode: String? = null,
        lastModuleId: String? = null,
        flashcardActivity: Boolean = false
    ) {
        val existing = database.studyDayDao().getByDate(dateIso)
        val parsedDate = runCatching { LocalDate.parse(dateIso) }.getOrElse { LocalDate.now() }
        val zone = ZoneId.systemDefault()
        val startMillis = parsedDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = parsedDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val countToday = database.nodeProgressDao().getCountReadBetween(startMillis, endMillis)
        val resolvedCount = if (countToday > 0 || lastNodeCode != null) {
            countToday
        } else {
            existing?.nodesCompletedCount ?: 0
        }
        val resolvedFlashcardActivity = flashcardActivity ||
            lastNodeCode == "FLASHCARD_REVIEW" ||
            (existing?.hasFlashcardActivity ?: false)
        database.studyDayDao().insert(
            StudyDayEntity(
                dateIso = dateIso,
                nodesCompletedCount = resolvedCount,
                lastNodeCode = lastNodeCode ?: existing?.lastNodeCode,
                lastModuleId = lastModuleId ?: existing?.lastModuleId,
                hasFlashcardActivity = resolvedFlashcardActivity
            )
        )
    }

    /**
     * Menandai simpul kurikulum selesai secara satu-arah / idempoten (NB-01).
     * Jika simpul sudah ada di `node_progress`, tidak melakukan apa-apa dan mengembalikan `false`.
     * Jika simpul belum selesai, menyisipkan ke `node_progress`, memperbarui `study_days`, dan mengembalikan `true`.
     */
    suspend fun markNodeCompletedIfNotYet(
        rawNodeCode: String,
        explicitModuleId: String? = null,
        dateIso: String = LocalDate.now().toString()
    ): Boolean {
        val normalizedCode = LegacyNodeCodes.normalize(rawNodeCode.trim())
        if (normalizedCode.isBlank()) return false
        val resolvedModuleId = explicitModuleId?.takeIf { it.isNotBlank() } ?: when {
            normalizedCode.startsWith("M03A.") -> "M03A"
            normalizedCode.startsWith("M03B.") -> "M03B"
            normalizedCode.startsWith("M03C.") -> "M03C"
            normalizedCode.contains(".") -> normalizedCode.substringBefore(".")
            else -> ""
        }
        return database.withTransaction {
            val existing = database.nodeProgressDao().getByNodeCode(normalizedCode)
            if (existing != null) {
                false
            } else {
                database.nodeProgressDao().insert(
                    NodeProgressEntity(
                        nodeCode = normalizedCode,
                        completedAt = System.currentTimeMillis(),
                        moduleId = resolvedModuleId
                    )
                )
                upsertStudyDayInternal(
                    dateIso = dateIso,
                    lastNodeCode = normalizedCode,
                    lastModuleId = resolvedModuleId
                )
                true
            }
        }
    }

    /**
     * Menandai simpul selesai lewat membaca (`source = "READ"`).
     * Jika simpul sebelumnya berstatus `SKIP_TEST`, status ditingkatkan menjadi `"READ"` agar
     * XP baca (+10) dan status `DONE` pada peta langsung terhitung.
     */
    suspend fun markNodeRead(
        rawNodeCode: String,
        explicitModuleId: String? = null,
        dateIso: String = LocalDate.now().toString()
    ): Boolean {
        val normalizedCode = LegacyNodeCodes.normalize(rawNodeCode.trim())
        if (normalizedCode.isBlank()) return false
        val resolvedModuleId = explicitModuleId?.takeIf { it.isNotBlank() } ?: resolveModuleIdFromCode(normalizedCode)
        val changed = database.withTransaction {
            val existing = database.nodeProgressDao().getByNodeCode(normalizedCode)
            if (existing != null && existing.source == "READ") {
                false
            } else {
                database.nodeProgressDao().insert(
                    NodeProgressEntity(
                        nodeCode = normalizedCode,
                        completedAt = System.currentTimeMillis(),
                        moduleId = resolvedModuleId,
                        source = "READ"
                    )
                )
                upsertStudyDayInternal(
                    dateIso = dateIso,
                    lastNodeCode = normalizedCode,
                    lastModuleId = resolvedModuleId
                )
                true
            }
        }
        if (changed) {
            setLastVisitedNodeCode(normalizedCode)
        }
        return changed
    }

    suspend fun getSkippedNodeCodes(): Set<String> =
        LegacyNodeCodes.normalizeAll(
            database.nodeProgressDao().getNodeCodesBySource("SKIP_TEST").toSet()
        )

    /**
     * Menandai sekumpulan simpul yang dilewati lewat fitur "Lompat dengan tes singkat"
     * (`source = "SKIP_TEST"`). Simpul yang sudah selesai (`READ` / sudah ada di `node_progress`)
     * tidak ditimpa, dan lompatan 0-XP tidak mencatat hari belajar (`study_days`) agar streak
     * tetap hanya dihitung dari belajar/baca/kuis nyata.
     */
    suspend fun markNodesSkippedByTest(
        rawNodeCodes: List<String>,
        @Suppress("UNUSED_PARAMETER") dateIso: String = LocalDate.now().toString()
    ): Int {
        val normalizedDistinct = rawNodeCodes
            .map { LegacyNodeCodes.normalize(it.trim()) }
            .filter { it.isNotBlank() }
            .distinct()
        if (normalizedDistinct.isEmpty()) return 0

        return database.withTransaction {
            val now = System.currentTimeMillis()
            val toInsert = mutableListOf<NodeProgressEntity>()
            for (code in normalizedDistinct) {
                val existing = database.nodeProgressDao().getByNodeCode(code)
                if (existing == null) {
                    toInsert.add(
                        NodeProgressEntity(
                            nodeCode = code,
                            completedAt = now,
                            moduleId = resolveModuleIdFromCode(code),
                            source = "SKIP_TEST"
                        )
                    )
                }
            }
            if (toInsert.isNotEmpty()) {
                database.nodeProgressDao().insertAll(toInsert)
            }
            toInsert.size
        }
    }

    private fun resolveModuleIdFromCode(normalizedCode: String): String = when {
        normalizedCode.startsWith("M03A.") -> "M03A"
        normalizedCode.startsWith("M03B.") -> "M03B"
        normalizedCode.startsWith("M03C.") -> "M03C"
        normalizedCode.contains(".") -> normalizedCode.substringBefore(".")
        else -> ""
    }

    /**
     * Toggle status selesai simpul kurikulum secara atomik di dalam transaksi database (B-05, B-03, B-17, FINAL-02, K-07 & P3).
     * Mengembalikan `true` jika simpul menjadi selesai (inserted), atau `false` jika dibatalkan (deleted).
     */
    suspend fun toggleNodeCompletionTransactional(
        normalizedCode: String,
        resolvedModuleId: String,
        dateIso: String = LocalDate.now().toString()
    ): Boolean {
        return database.withTransaction {
            val existing = database.nodeProgressDao().getByNodeCode(normalizedCode)
            if (existing != null) {
                database.nodeProgressDao().delete(normalizedCode)
                val zone = ZoneId.systemDefault()
                val completedDateIso = runCatching {
                    Instant.ofEpochMilli(existing.completedAt).atZone(zone).toLocalDate().toString()
                }.getOrElse { dateIso }
                for (targetDateIso in setOf(completedDateIso, dateIso)) {
                    val existingDay = database.studyDayDao().getByDate(targetDateIso)
                    if (existingDay != null) {
                        val parsedDate = runCatching { LocalDate.parse(targetDateIso) }.getOrElse { LocalDate.now() }
                        val startMillis = parsedDate.atStartOfDay(zone).toInstant().toEpochMilli()
                        val endMillis = parsedDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                        val countOnDay = database.nodeProgressDao().getCountReadBetween(startMillis, endMillis)
                        val quizCountOnDay = database.quizAttemptDao().getCountAnsweredBetween(startMillis, endMillis)
                        val hasFlashcardActivity = existingDay.hasFlashcardActivity || existingDay.lastNodeCode == "FLASHCARD_REVIEW"
                        if (countOnDay == 0 && quizCountOnDay == 0 && !hasFlashcardActivity) {
                            database.studyDayDao().deleteByDate(targetDateIso)
                        } else {
                            val latestRemainingCode = database.nodeProgressDao()
                                .getLatestReadNodeCodeBetween(startMillis, endMillis)
                                ?: if (existingDay.lastNodeCode == "FLASHCARD_REVIEW") "FLASHCARD_REVIEW" else null
                            val updatedLastNodeCode = if (existingDay.lastNodeCode == normalizedCode || existingDay.lastNodeCode == "FLASHCARD_REVIEW") {
                                latestRemainingCode
                            } else {
                                existingDay.lastNodeCode
                            }
                            val updatedLastModuleId = if (updatedLastNodeCode != null && updatedLastNodeCode != "FLASHCARD_REVIEW") {
                                when {
                                    updatedLastNodeCode.startsWith("M03A.") -> "M03A"
                                    updatedLastNodeCode.startsWith("M03B.") -> "M03B"
                                    updatedLastNodeCode.startsWith("M03C.") -> "M03C"
                                    updatedLastNodeCode.contains(".") -> updatedLastNodeCode.substringBefore(".")
                                    else -> existingDay.lastModuleId
                                }
                            } else {
                                existingDay.lastModuleId
                            }
                            database.studyDayDao().insert(
                                existingDay.copy(
                                    nodesCompletedCount = countOnDay,
                                    lastNodeCode = updatedLastNodeCode,
                                    lastModuleId = updatedLastModuleId,
                                    hasFlashcardActivity = hasFlashcardActivity
                                )
                            )
                        }
                    }
                }
                false
            } else {
                database.nodeProgressDao().insert(
                    NodeProgressEntity(
                        nodeCode = normalizedCode,
                        completedAt = System.currentTimeMillis(),
                        moduleId = resolvedModuleId
                    )
                )
                upsertStudyDayInternal(
                    dateIso = dateIso,
                    lastNodeCode = normalizedCode,
                    lastModuleId = resolvedModuleId
                )
                true
            }
        }
    }
}
