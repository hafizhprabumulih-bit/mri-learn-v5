package com.example.ui.path

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.data.LegacyNodeCodes
import com.example.data.MriModule
import com.example.data.MriNode
import com.example.data.UserDataRepository
import com.example.util.ModulePathState
import com.example.util.PATH_STRICT_LOCKS
import com.example.util.PathNodeState
import com.example.util.PathStateResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Penanda navigasi ringan agar layar Tersimpan / Glosarium dapat membuka pembaca materi
 * langsung pada simpul tertentu tanpa menambah state baru ke MriViewModel.
 */
object PathNavigationHints {
    @Volatile
    private var pendingDirectNodeCode: String? = null

    @Volatile
    private var pendingOpenAllNodes: Boolean = false

    @Volatile
    private var pendingOpenFlashcards: Boolean = false

    var isFocusReaderActive: Boolean by mutableStateOf(false)

    var isArcadeOpen: Boolean by mutableStateOf(false)

    var hasShownMapGuideThisSession: Boolean by mutableStateOf(false)

    fun requestDirectReader(nodeCode: String?) {
        pendingDirectNodeCode = nodeCode?.trim()?.takeIf { it.isNotBlank() }
    }

    fun requestOpenAllNodes() {
        pendingOpenAllNodes = true
    }

    fun requestOpenFlashcards() {
        pendingOpenFlashcards = true
    }

    fun consumePendingDirectNodeCode(): String? {
        val code = pendingDirectNodeCode
        pendingDirectNodeCode = null
        return code
    }

    fun consumePendingOpenAllNodes(): Boolean {
        val flag = pendingOpenAllNodes
        pendingOpenAllNodes = false
        return flag
    }

    fun consumePendingOpenFlashcards(): Boolean {
        val flag = pendingOpenFlashcards
        pendingOpenFlashcards = false
        return flag
    }
}

@Immutable
data class ModulePathSectionUi(
    val title: String,
    val subtitle: String?,
    val startIndex: Int,
    val nodes: List<IndexedPathNode>
)

@Immutable
data class IndexedPathNode(
    val globalIndex: Int,
    val state: PathNodeState,
    val kind: NodeKind,
    val quizCount: Int
)

/**
 * State holder khusus Peta Belajar di lapisan UI agar logika peta tidak menumpuk di MriViewModel.
 */
@Stable
class ModulePathStateHolder(context: Context) {
    private val appContext: Context = context.applicationContext
    val repository: UserDataRepository = UserDataRepository(appContext)
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val skippedCodesFlow: Flow<Set<String>> =
        repository.observeCompletedNodes().map { rows ->
            rows.asSequence()
                .filter { it.source == SOURCE_SKIP_TEST }
                .map { LegacyNodeCodes.normalize(it.nodeCode) }
                .toSet()
        }

    val hasSeenMapGuideFlow: Flow<Boolean> = repository.hasSeenMapGuideFlow

    val celebratedModuleIdsFlow: Flow<Set<String>> = repository.celebratedModuleIdsFlow

    val triedLabNodesFlow: Flow<Set<String>> = repository.triedLabNodesFlow

    val arcadeStatsFlow: Flow<com.example.data.ArcadeStats> = repository.arcadeStatsFlow

    val preferredReaderModeFlow: Flow<String> = repository.preferredReaderModeFlow

    val lessonTapHintCountFlow: Flow<Int> = repository.lessonTapHintCountFlow

    val manualReviewNodesFlow: Flow<Set<String>> = repository.manualReviewNodesFlow

    val excludedReviewNodesFlow: Flow<Set<String>> = repository.excludedReviewNodesFlow

    val allowLessonDraftsFlow: Flow<Boolean> = repository.allowLessonDraftsFlow

    val lessonExtras: com.example.data.lesson.LessonExtras by lazy {
        com.example.data.lesson.LessonExtrasLoader.load(appContext)
    }

    fun observeSavedLessonStep(nodeCode: String): Flow<Int> =
        repository.observeSavedLessonStep(nodeCode)

    fun observeSavedLessonAnswers(nodeCode: String): Flow<String> =
        repository.observeSavedLessonAnswers(nodeCode)

    fun saveLessonStepAsync(nodeCode: String, stepIndex: Int): Job =
        persistenceScope.launch {
            repository.saveLessonStep(nodeCode, stepIndex)
        }

    fun saveLessonProgressAsync(nodeCode: String, stepIndex: Int, encodedAnswers: String): Job =
        persistenceScope.launch {
            repository.saveLessonProgress(nodeCode, stepIndex, encodedAnswers)
        }

    fun clearLessonStepAsync(nodeCode: String): Job =
        persistenceScope.launch {
            repository.clearLessonStep(nodeCode)
        }

    fun setPreferredReaderModeAsync(mode: String): Job =
        persistenceScope.launch {
            repository.setPreferredReaderMode(mode)
        }

    fun incrementLessonTapHintCountAsync(): Job =
        persistenceScope.launch {
            repository.incrementLessonTapHintCount()
        }

    fun setNodeManualReviewAsync(nodeCode: String, enabled: Boolean): Job =
        persistenceScope.launch {
            repository.setNodeManualReview(nodeCode, enabled)
        }

    fun setAllowLessonDraftsAsync(allow: Boolean): Job =
        persistenceScope.launch {
            repository.setAllowLessonDrafts(allow)
        }

    fun recordLessonQuizAttemptAsync(
        quizId: String,
        nodeCode: String,
        moduleId: String? = null,
        selectedIndex: Int,
        isCorrect: Boolean
    ): Job = persistenceScope.launch {
        repository.recordLessonQuizAttempt(
            quizId = quizId,
            rawNodeCode = nodeCode,
            explicitModuleId = moduleId,
            selectedIndex = selectedIndex,
            isCorrect = isCorrect
        )
    }

    fun observeDailyShiftCounts(todayKey: String): Flow<Triple<Int, Int, Int>> =
        repository.observeDailyShiftCounts(todayKey)

    suspend fun markMapGuideSeen() {
        PathNavigationHints.hasShownMapGuideThisSession = true
        repository.setHasSeenMapGuide(true)
    }

    fun markMapGuideSeenAsync() {
        PathNavigationHints.hasShownMapGuideThisSession = true
        persistenceScope.launch {
            repository.setHasSeenMapGuide(true)
        }
    }

    suspend fun markLabTried(nodeCode: String) {
        repository.markLabNodeTried(nodeCode)
    }

    suspend fun recordDailyShift(todayKey: String, nodesDelta: Int = 0, quizzesDelta: Int = 0, labsDelta: Int = 0) {
        repository.incrementDailyShift(todayKey, nodesDelta, quizzesDelta, labsDelta)
    }

    fun recordDailyShiftAsync(todayKey: String, nodesDelta: Int = 0, quizzesDelta: Int = 0, labsDelta: Int = 0) {
        persistenceScope.launch {
            repository.incrementDailyShift(todayKey, nodesDelta, quizzesDelta, labsDelta)
        }
    }

    suspend fun recordMinigameResult(kindKey: String, score: Int) {
        repository.recordMinigameResult(kindKey, score)
    }

    fun recordMinigameResultAsync(kindKey: String, score: Int) {
        persistenceScope.launch {
            repository.recordMinigameResult(kindKey, score)
        }
    }

    suspend fun recordArcadeResult(kindKey: String, score: Int, rankTitle: String) {
        repository.recordArcadeResult(kindKey, score, rankTitle)
    }

    fun recordArcadeResultAsync(kindKey: String, score: Int, rankTitle: String) {
        persistenceScope.launch {
            repository.recordArcadeResult(kindKey, score, rankTitle)
        }
    }

    suspend fun markModuleCelebrated(moduleId: String) {
        repository.markModulesCelebrated(setOf(moduleId))
    }

    fun markModuleCelebratedAsync(moduleId: String): Job =
        persistenceScope.launch {
            repository.markModulesCelebrated(setOf(moduleId))
        }

    suspend fun ensureCelebratedModulesInitialized(alreadyCompletedModuleIds: Set<String>) {
        repository.ensureCelebratedModulesInitialized(alreadyCompletedModuleIds)
    }

    suspend fun getSkippedNodeCodes(): Set<String> = repository.getSkippedNodeCodes()

    fun resolveModulePath(
        module: MriModule,
        allModules: List<MriModule>,
        completedCodes: Set<String>,
        skippedCodes: Set<String>,
        strictLocks: Boolean = PATH_STRICT_LOCKS
    ): ModulePathState {
        return PathStateResolver.resolveModule(
            module = module,
            allModules = allModules,
            completedCodes = completedCodes,
            strictLocks = strictLocks,
            skippedCodes = skippedCodes
        )
    }

    suspend fun markNodeRead(nodeCode: String): Boolean {
        return repository.markNodeRead(nodeCode)
    }

    fun markNodeReadAsync(nodeCode: String): Job =
        persistenceScope.launch {
            repository.markNodeRead(nodeCode)
        }

    suspend fun markNodesSkippedByTest(nodeCodes: List<String>): Int {
        return repository.markNodesSkippedByTest(nodeCodes)
    }

    fun markNodesSkippedByTestAsync(nodeCodes: List<String>): Job =
        persistenceScope.launch {
            repository.markNodesSkippedByTest(nodeCodes)
        }

    fun buildSections(
        pathState: ModulePathState,
        quizCountsByNode: Map<String, Int>
    ): List<ModulePathSectionUi> {
        val items = pathState.nodes
        if (items.isEmpty()) return emptyList()

        val sections = mutableListOf<ModulePathSectionUi>()
        var currentLevelGroup = sectionKeyFor(items.first().node)
        var currentBucket = mutableListOf<IndexedPathNode>()
        var currentStart = 0

        items.forEachIndexed { index, nodeState ->
            val group = sectionKeyFor(nodeState.node)
            if (group != currentLevelGroup && currentBucket.isNotEmpty()) {
                val (title, subtitle) = sectionHeaderFor(currentLevelGroup)
                sections.add(
                    ModulePathSectionUi(
                        title = title,
                        subtitle = subtitle,
                        startIndex = currentStart,
                        nodes = currentBucket.toList()
                    )
                )
                currentLevelGroup = group
                currentBucket = mutableListOf()
                currentStart = index
            }
            currentBucket.add(
                IndexedPathNode(
                    globalIndex = index,
                    state = nodeState,
                    kind = resolveNodeKind(nodeState.node),
                    quizCount = quizCountsByNode[LegacyNodeCodes.normalize(nodeState.node.code)] ?: 0
                )
            )
        }

        if (currentBucket.isNotEmpty()) {
            val (title, subtitle) = sectionHeaderFor(currentLevelGroup)
            sections.add(
                ModulePathSectionUi(
                    title = title,
                    subtitle = subtitle,
                    startIndex = currentStart,
                    nodes = currentBucket.toList()
                )
            )
        }

        return sections
    }

    companion object {
        const val SOURCE_READ = "READ"
        const val SOURCE_SKIP_TEST = "SKIP_TEST"

        @Volatile
        private var cachedInstance: ModulePathStateHolder? = null

        fun getInstance(context: Context): ModulePathStateHolder {
            val appCtx = context.applicationContext
            return cachedInstance?.takeIf { it.appContext === appCtx } ?: synchronized(this) {
                cachedInstance?.takeIf { it.appContext === appCtx }
                    ?: ModulePathStateHolder(appCtx).also { cachedInstance = it }
            }
        }

        fun resolveNodeKind(node: MriNode): NodeKind {
            val codeUpper = node.code.uppercase()
            val titleLower = node.title.lowercase()
            return when {
                node.level == 0 || codeUpper.endsWith(".T0") -> NodeKind.TUJUAN
                codeUpper.endsWith(".R") ||
                    codeUpper.contains(".R.") ||
                    titleLower.contains("rangkuman") ||
                    titleLower.contains("ringkasan") -> NodeKind.RINGKASAN
                !node.labType.isNullOrBlank() -> NodeKind.LAB
                else -> NodeKind.MATERI
            }
        }

        private fun sectionKeyFor(node: MriNode): Int = when (node.level) {
            0 -> 0
            1 -> 1
            2 -> 2
            else -> 3
        }

        private fun sectionHeaderFor(level: Int): Pair<String, String?> = when (level) {
            0 -> "Pendahuluan & Tujuan" to "Gambaran umum kompetensi modul"
            1 -> "Tingkat 1 · Fundamental" to "Konsep inti dan dasar mekanisme"
            2 -> "Tingkat 2 · Klinis Menengah" to "Penerapan parameter dan akuisisi"
            else -> "Tingkat 3 · Fisika Lanjut" to "Pendalaman kuantitatif dan optimasi"
        }
    }
}

@Composable
fun rememberModulePathStateHolder(): ModulePathStateHolder {
    val context = LocalContext.current
    val appCtx = context.applicationContext
    return remember(appCtx) {
        ModulePathStateHolder.getInstance(appCtx)
    }
}

@Composable
fun rememberSkippedNodeCodes(holder: ModulePathStateHolder): Set<String> {
    val skipped by holder.skippedCodesFlow.collectAsState(initial = emptySet())
    return skipped
}
