package com.example.ui.path

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.LegacyNodeCodes
import com.example.data.MriModule
import com.example.ui.lesson.LessonScreen
import com.example.ui.module.ModuleDetailScreen
import com.example.ui.module.ReaderMode
import com.example.util.ModuleCompletionCelebration
import com.example.util.PathStateResolver
import com.example.util.XpCalculator
import com.example.util.lesson.AttemptRecord
import com.example.util.lesson.LESSON_FLOW_ENABLED
import com.example.util.lesson.MasteryStars
import com.example.viewmodel.MainTab
import com.example.viewmodel.MriUiState
import com.example.viewmodel.MriViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pengarah rute per-modul antara Peta Modul ([ModulePathScreen]), Mode Pelajaran Bertahap ([LessonScreen]),
 * Pembaca Materi / Baca Cepat ([ModuleDetailScreen]), dan layar selebrasi [NodeCompletedScreen].
 */
@Composable
fun ModuleRouteHost(
    module: MriModule,
    state: MriUiState,
    viewModel: MriViewModel,
    modifier: Modifier = Modifier
) {
    val pathHolder = rememberModulePathStateHolder()
    val skippedCodes = rememberSkippedNodeCodes(pathHolder)
    val preferredReaderMode by pathHolder.preferredReaderModeFlow.collectAsState(initial = "LESSON")
    val allowDrafts by pathHolder.allowLessonDraftsFlow.collectAsState(initial = false)
    val manualReviewNodes by pathHolder.manualReviewNodesFlow.collectAsState(initial = emptySet())

    var inReaderMode by rememberSaveable(module.id) { mutableStateOf(false) }
    var inLessonMode by rememberSaveable(module.id) { mutableStateOf(false) }
    var isReplayLesson by rememberSaveable(module.id) { mutableStateOf(false) }
    var focusedNodeCode by rememberSaveable(module.id) { mutableStateOf<String?>(null) }
    var openedDirectlyToReader by rememberSaveable(module.id) { mutableStateOf(false) }

    var celebrationNodeCode by rememberSaveable(module.id) { mutableStateOf<String?>(null) }
    var celebrationEarnedXp by rememberSaveable(module.id) { mutableIntStateOf(0) }
    var pendingModuleCelebration by rememberSaveable(module.id) { mutableStateOf(false) }
    var showModuleCompleted by rememberSaveable(module.id) { mutableStateOf(false) }
    val celebratedThisSession = remember(module.id) { mutableStateListOf<String>() }
    val celebratedModuleIds by pathHolder.celebratedModuleIdsFlow.collectAsState(initial = emptySet())

    val todayKey = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    LaunchedEffect(state.modulesList, state.isResumeRestored) {
        if (state.isResumeRestored && state.modulesList.isNotEmpty()) {
            val currentSkipped = pathHolder.getSkippedNodeCodes()
            val alreadyDoneIds = state.modulesList
                .filter { mod ->
                    val readProgress = ModuleCompletionCelebration.readProgressOf(
                        module = mod,
                        completedCodes = state.completedNodeCodes,
                        skippedCodes = currentSkipped
                    )
                    readProgress.total > 0 && readProgress.done >= readProgress.total
                }
                .map { it.id }
                .toSet()
            pathHolder.ensureCelebratedModulesInitialized(alreadyDoneIds)
        }
    }

    val activeReaderMode = if (inReaderMode && !focusedNodeCode.isNullOrBlank()) {
        ReaderMode.FOCUS_NODE
    } else {
        ReaderMode.ALL_NODES
    }

    LaunchedEffect(activeReaderMode, inLessonMode) {
        PathNavigationHints.isFocusReaderActive = inLessonMode || activeReaderMode == ReaderMode.FOCUS_NODE
    }
    DisposableEffect(Unit) {
        onDispose {
            PathNavigationHints.isFocusReaderActive = false
        }
    }

    LaunchedEffect(state.completedNodeCodes) {
        val currentNormalized = LegacyNodeCodes.normalizeAll(state.completedNodeCodes)
        celebratedThisSession.removeAll { it !in currentNormalized }
    }

    LaunchedEffect(module.id) {
        val pendingDirectCode = PathNavigationHints.consumePendingDirectNodeCode()
        val openAll = PathNavigationHints.consumePendingOpenAllNodes()
        val openFlash = PathNavigationHints.consumePendingOpenFlashcards()
        if (pendingDirectCode != null) {
            val normalizedTarget = LegacyNodeCodes.normalize(pendingDirectCode)
            val resolvedCode = module.nodes.find {
                LegacyNodeCodes.normalize(it.code) == normalizedTarget
            }?.code
            openedDirectlyToReader = true
            focusedNodeCode = resolvedCode
            inLessonMode = false
            inReaderMode = true
            viewModel.setModuleFlashcardMode(false)
        } else if (openAll) {
            openedDirectlyToReader = true
            focusedNodeCode = null
            inLessonMode = false
            inReaderMode = true
            viewModel.setModuleFlashcardMode(false)
        } else if (openFlash) {
            openedDirectlyToReader = true
            focusedNodeCode = null
            inLessonMode = false
            inReaderMode = true
            viewModel.setModuleFlashcardMode(true)
        }
    }

    val handleNodeCompletionCelebration: (String, Set<String>, Set<String>, Boolean) -> Unit = { rawCode, completedBefore, skippedBefore, forceShow ->
        val normCode = LegacyNodeCodes.normalize(rawCode)
        val nodeObj = module.nodes.find { LegacyNodeCodes.normalize(it.code) == normCode }
        if (nodeObj != null && (forceShow || normCode !in celebratedThisSession)) {
            if (normCode !in celebratedThisSession) {
                celebratedThisSession.add(normCode)
            }
            celebrationEarnedXp = XpCalculator.earnedForNodeCompletion(
                node = nodeObj,
                module = module,
                completedCodesBefore = completedBefore,
                skippedCodesBefore = skippedBefore
            )
            celebrationNodeCode = nodeObj.code
            if (ModuleCompletionCelebration.shouldShowForReadCompletion(
                    moduleId = module.id,
                    module = module,
                    completedBefore = completedBefore,
                    skippedBefore = skippedBefore,
                    justCompletedCode = normCode,
                    alreadyCelebratedModuleIds = celebratedModuleIds
                )
            ) {
                pendingModuleCelebration = true
                pathHolder.markModuleCelebratedAsync(module.id)
            }
        }
    }

    val activeLessonNode = remember(inLessonMode, focusedNodeCode, module.nodes) {
        if (!inLessonMode || focusedNodeCode.isNullOrBlank()) null
        else {
            val norm = LegacyNodeCodes.normalize(focusedNodeCode!!)
            module.nodes.find { LegacyNodeCodes.normalize(it.code) == norm }
        }
    }

    if (inLessonMode && activeLessonNode != null) {
        LessonScreen(
            module = module,
            node = activeLessonNode,
            allQuizzes = state.quizzesList,
            characterCustomization = state.characterCustomization,
            pathHolder = pathHolder,
            isReplayMode = isReplayLesson,
            onExitLesson = {
                inLessonMode = false
                isReplayLesson = false
                focusedNodeCode = null
            },
            onSwitchToQuickRead = {
                inLessonMode = false
                isReplayLesson = false
                inReaderMode = true
                viewModel.setModuleFlashcardMode(false)
            },
            onLessonFinished = {
                val targetCode = activeLessonNode.code
                val normTarget = LegacyNodeCodes.normalize(targetCode)
                val completedBefore = state.completedNodeCodes
                val skippedBefore = skippedCodes
                val wasDoneAsRead = (completedBefore.contains(targetCode) || completedBefore.contains(normTarget)) &&
                    normTarget !in skippedBefore && targetCode !in skippedBefore

                if (!wasDoneAsRead) {
                    if (normTarget in skippedBefore || targetCode in skippedBefore) {
                        pathHolder.markNodeReadAsync(targetCode)
                    } else {
                        viewModel.markNodeCompleted(targetCode)
                    }
                    pathHolder.recordDailyShiftAsync(todayKey, nodesDelta = 1)
                }

                inLessonMode = false
                isReplayLesson = false
                handleNodeCompletionCelebration(targetCode, completedBefore, skippedBefore, true)
            }
        )
    } else if (inReaderMode) {
        ModuleDetailScreen(
            module = module,
            state = state,
            viewModel = viewModel,
            modifier = modifier,
            focusedNodeCode = focusedNodeCode,
            readerMode = activeReaderMode,
            skippedNodeCodes = skippedCodes,
            onClearFocusedNode = { focusedNodeCode = null },
            onSelectFocusedNode = { nextCode -> focusedNodeCode = nextCode },
            onReturnToPath = {
                openedDirectlyToReader = false
                focusedNodeCode = null
                inReaderMode = false
                inLessonMode = false
                viewModel.setModuleFlashcardMode(false)
            },
            onBackOverride = {
                if (openedDirectlyToReader) {
                    viewModel.navigateBack()
                } else {
                    focusedNodeCode = null
                    inReaderMode = false
                    inLessonMode = false
                    viewModel.setModuleFlashcardMode(false)
                }
            },
            onNodeJustCompleted = { rawCode, completedBefore, skippedBefore ->
                handleNodeCompletionCelebration(rawCode, completedBefore, skippedBefore, false)
            },
            onMarkSkippedNodeAsRead = { code ->
                pathHolder.markNodeReadAsync(code)
            }
        )
    } else {
        ModulePathScreen(
            module = module,
            state = state,
            viewModel = viewModel,
            pathHolder = pathHolder,
            skippedCodes = skippedCodes,
            onOpenLesson = { code ->
                val norm = LegacyNodeCodes.normalize(code)
                val alreadyRead = (state.completedNodeCodes.contains(code) || state.completedNodeCodes.contains(norm)) &&
                    norm !in skippedCodes && code !in skippedCodes
                openedDirectlyToReader = false
                focusedNodeCode = code
                isReplayLesson = alreadyRead
                inLessonMode = LESSON_FLOW_ENABLED
                inReaderMode = !LESSON_FLOW_ENABLED
                viewModel.setModuleFlashcardMode(false)
            },
            onOpenNodeReader = { code ->
                openedDirectlyToReader = false
                focusedNodeCode = code
                inLessonMode = false
                inReaderMode = true
                viewModel.setModuleFlashcardMode(false)
            },
            onOpenAllNodesReader = {
                openedDirectlyToReader = false
                focusedNodeCode = null
                inLessonMode = false
                inReaderMode = true
                viewModel.setModuleFlashcardMode(false)
            },
            onOpenFlashcards = {
                openedDirectlyToReader = false
                focusedNodeCode = null
                inLessonMode = false
                inReaderMode = true
                viewModel.setModuleFlashcardMode(true)
            },
            modifier = modifier
        )
    }

    val doneNodeCode = celebrationNodeCode
    if (doneNodeCode != null) {
        val orderedNodes = remember(module.nodes) { PathStateResolver.orderNodesPublic(module.nodes) }
        val normDoneCode = LegacyNodeCodes.normalize(doneNodeCode)
        val completedNode = orderedNodes.find { LegacyNodeCodes.normalize(it.code) == normDoneCode }
        if (completedNode != null) {
            val completedIdx = orderedNodes.indexOfFirst { it.code == completedNode.code }
            val nextNode = if (completedIdx >= 0 && completedIdx + 1 < orderedNodes.size) {
                orderedNodes[completedIdx + 1]
            } else {
                null
            }
            val xpSummary = remember(
                state.completedNodeCodes,
                skippedCodes,
                state.quizAttempts,
                state.modulesList
            ) {
                XpCalculator.calculate(
                    completedCodes = state.completedNodeCodes,
                    skippedCodes = skippedCodes,
                    quizAttempts = state.quizAttempts,
                    modules = state.modulesList
                )
            }
            val normCode = LegacyNodeCodes.normalize(completedNode.code)
            val nodeQuizzes = remember(state.quizzesList, normCode) {
                state.quizzesList.filter { LegacyNodeCodes.normalize(it.nodeCode) == normCode }
            }
            val nodeCorrectCount = remember(state.quizAttempts, nodeQuizzes) {
                val ids = nodeQuizzes.map { it.id }.toSet()
                state.quizAttempts
                    .filter { it.isCorrect && it.quizId in ids }
                    .map { it.quizId }
                    .distinct()
                    .size
            }
            val unlockPreview = remember(
                state.completedNodeCodes,
                state.modulesList,
                module.id
            ) {
                XpCalculator.nextUnlockPreview(
                    completedCodes = state.completedNodeCodes,
                    modules = state.modulesList,
                    preferredModuleId = module.id
                )
            }

            // Perhitungan Bintang Penguasaan & Ulasan Hari Ini (P6)
            val nodeExtras = remember(normCode, pathHolder.lessonExtras) {
                pathHolder.lessonExtras.forNode(normCode)
            }
            val activeStories = remember(nodeExtras, allowDrafts) {
                nodeExtras?.stories.orEmpty().filter { it.reviewed || allowDrafts }.take(2)
            }
            val firstCheckQuiz = remember(nodeQuizzes, activeStories) {
                val storyIds = activeStories.mapTo(HashSet()) { it.id }
                nodeQuizzes.firstOrNull { it.id !in storyIds }
            }
            val assessmentIds = remember(activeStories, firstCheckQuiz) {
                buildList {
                    activeStories.forEach { add(it.id) }
                    firstCheckQuiz?.let { add(it.id) }
                }
            }
            val attemptRecords = remember(state.quizAttempts, normCode) {
                state.quizAttempts
                    .filter { LegacyNodeCodes.normalize(it.nodeCode) == normCode }
                    .map { AttemptRecord(quizId = it.quizId, isCorrect = it.isCorrect, answeredAt = it.answeredAt) }
            }
            val masteryStars = remember(attemptRecords, assessmentIds) {
                MasteryStars.stars(attemptRecords, assessmentIds)
            }
            val starCaption = remember(masteryStars, activeStories, firstCheckQuiz) {
                MasteryStars.starCaption(
                    stars = masteryStars,
                    hasStory = activeStories.isNotEmpty(),
                    hasCheck = firstCheckQuiz != null
                )
            }
            val autoNeedsReview = remember(attemptRecords, assessmentIds) {
                MasteryStars.needsReview(attemptRecords, assessmentIds)
            }
            val isManualReview = normCode in manualReviewNodes || autoNeedsReview

            val moduleReadProgress = remember(module, state.completedNodeCodes, skippedCodes, normCode) {
                val effectiveCompleted = state.completedNodeCodes + normCode
                ModuleCompletionCelebration.readProgressOf(module, effectiveCompleted, skippedCodes)
            }

            val recapBullets = remember(nodeExtras) {
                nodeExtras?.recap.orEmpty()
            }

            NodeCompletedScreen(
                module = module,
                completedNode = completedNode,
                nextNode = nextNode,
                earnedXp = celebrationEarnedXp,
                xpSummary = xpSummary,
                streakDays = state.studyStreakDays,
                nodeCorrectQuizzes = nodeCorrectCount,
                nodeTotalQuizzes = nodeQuizzes.size,
                unlockPreview = unlockPreview,
                characterCustomization = state.characterCustomization,
                doneNodesInModule = moduleReadProgress.done.coerceAtLeast(1),
                totalNodesInModule = moduleReadProgress.total.coerceAtLeast(1),
                masteryStars = masteryStars,
                starCaptionText = starCaption,
                recapBullets = recapBullets,
                addToDailyReviewChecked = isManualReview,
                onToggleDailyReview = { enabled ->
                    pathHolder.setNodeManualReviewAsync(normCode, enabled)
                },
                onReplayLesson = {
                    celebrationNodeCode = null
                    openedDirectlyToReader = false
                    focusedNodeCode = completedNode.code
                    isReplayLesson = true
                    inLessonMode = true
                    inReaderMode = false
                    viewModel.setModuleFlashcardMode(false)
                },
                onLaunchLab = { labType ->
                    celebrationNodeCode = null
                    viewModel.launchLab(labType)
                },
                onContinueNext = {
                    celebrationNodeCode = null
                    if (pendingModuleCelebration) {
                        pendingModuleCelebration = false
                        showModuleCompleted = true
                    } else if (nextNode != null) {
                        openedDirectlyToReader = false
                        focusedNodeCode = nextNode.code
                        val useLesson = LESSON_FLOW_ENABLED && preferredReaderMode != "QUICK_READ"
                        val normNext = LegacyNodeCodes.normalize(nextNode.code)
                        isReplayLesson = (state.completedNodeCodes.contains(nextNode.code) || state.completedNodeCodes.contains(normNext)) &&
                            normNext !in skippedCodes
                        inLessonMode = useLesson
                        inReaderMode = !useLesson
                        viewModel.setModuleFlashcardMode(false)
                    } else {
                        openedDirectlyToReader = false
                        focusedNodeCode = null
                        inLessonMode = false
                        inReaderMode = false
                    }
                },
                onReturnToPath = {
                    celebrationNodeCode = null
                    if (pendingModuleCelebration) {
                        pendingModuleCelebration = false
                        showModuleCompleted = true
                    } else {
                        openedDirectlyToReader = false
                        focusedNodeCode = null
                        inLessonMode = false
                        inReaderMode = false
                        viewModel.setModuleFlashcardMode(false)
                    }
                }
            )
        }
    }

    if (showModuleCompleted) {
        val currentProgress = remember(module, state.completedNodeCodes, skippedCodes) {
            ModuleCompletionCelebration.readProgressOf(module, state.completedNodeCodes, skippedCodes)
        }
        val quizMastery = remember(module.id, state.quizzesList, state.quizAttempts) {
            ModuleCompletionCelebration.moduleQuizMasteryPercent(
                moduleId = module.id,
                quizzes = state.quizzesList,
                attempts = state.quizAttempts
            )
        }
        val unlockedItem = remember(module.id) {
            ModuleCompletionCelebration.unlockedCharacterItemName(module.id)
        }
        val nextModule = remember(module.id, state.modulesList, state.completedNodeCodes) {
            ModuleCompletionCelebration.findNextRecommendedModule(
                currentModuleId = module.id,
                allModules = state.modulesList,
                completedCodes = state.completedNodeCodes
            )
        }
        ModuleCompletedScreen(
            module = module,
            doneNodes = currentProgress.done,
            totalNodes = currentProgress.total,
            quizMasteryPercent = quizMastery,
            moduleBonusXp = XpCalculator.XP_MODULE_BONUS,
            unlockedItemName = unlockedItem,
            nextRecommendedModule = nextModule,
            onEquipItem = {
                showModuleCompleted = false
                viewModel.selectTab(MainTab.KARAKTER)
            },
            onContinueNextModule = { target ->
                showModuleCompleted = false
                openedDirectlyToReader = false
                focusedNodeCode = null
                inLessonMode = false
                inReaderMode = false
                viewModel.openModuleDetail(target)
            },
            onReturnToPath = {
                showModuleCompleted = false
                openedDirectlyToReader = false
                focusedNodeCode = null
                inLessonMode = false
                inReaderMode = false
                viewModel.setModuleFlashcardMode(false)
            }
        )
    }
}
