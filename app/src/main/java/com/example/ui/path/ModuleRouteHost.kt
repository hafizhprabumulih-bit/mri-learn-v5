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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import com.example.data.LegacyNodeCodes
import com.example.data.MriModule
import com.example.ui.module.ModuleDetailScreen
import com.example.ui.module.ReaderMode
import com.example.util.ModuleCompletionCelebration
import com.example.util.PathStateResolver
import com.example.util.XpCalculator
import com.example.viewmodel.MainTab
import com.example.viewmodel.MriUiState
import com.example.viewmodel.MriViewModel

/**
 * Pengarah rute per-modul antara Peta Modul ([ModulePathScreen]), Pembaca Materi ([ModuleDetailScreen]),
 * dan layar selebrasi [NodeCompletedScreen].
 * Menjaga tombol Back bergerak alami (Pembaca -> Peta Modul -> Asal tab) sekaligus mempertahankan
 * pembukaan langsung ke simpul target dari Tersimpan / Glosarium.
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
    val scope = rememberCoroutineScope()

    var inReaderMode by rememberSaveable(module.id) { mutableStateOf(false) }
    var focusedNodeCode by rememberSaveable(module.id) { mutableStateOf<String?>(null) }
    var openedDirectlyToReader by rememberSaveable(module.id) { mutableStateOf(false) }

    var celebrationNodeCode by rememberSaveable(module.id) { mutableStateOf<String?>(null) }
    var celebrationEarnedXp by rememberSaveable(module.id) { mutableIntStateOf(0) }
    var pendingModuleCelebration by rememberSaveable(module.id) { mutableStateOf(false) }
    var showModuleCompleted by rememberSaveable(module.id) { mutableStateOf(false) }
    val celebratedThisSession = remember(module.id) { mutableStateListOf<String>() }
    val celebratedModuleIds by pathHolder.celebratedModuleIdsFlow.collectAsState(initial = emptySet())

    LaunchedEffect(state.modulesList, state.isResumeRestored) {
        if (state.isResumeRestored && state.modulesList.isNotEmpty()) {
            val alreadyDoneIds = PathStateResolver
                .resolveAll(state.modulesList, state.completedNodeCodes)
                .filter { it.progress.total > 0 && it.progress.done >= it.progress.total }
                .map { it.module.id }
                .toSet()
            pathHolder.ensureCelebratedModulesInitialized(alreadyDoneIds)
        }
    }

    val activeReaderMode = if (inReaderMode && !focusedNodeCode.isNullOrBlank()) {
        ReaderMode.FOCUS_NODE
    } else {
        ReaderMode.ALL_NODES
    }

    LaunchedEffect(activeReaderMode) {
        PathNavigationHints.isFocusReaderActive = activeReaderMode == ReaderMode.FOCUS_NODE
    }
    DisposableEffect(Unit) {
        onDispose {
            PathNavigationHints.isFocusReaderActive = false
        }
    }

    // Bersihkan catatan selebrasi jika pengguna membatalkan status selesai agar bisa memicu lagi bila diselesaikan ulang
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
            inReaderMode = true
            viewModel.setModuleFlashcardMode(false)
        } else if (openAll) {
            openedDirectlyToReader = true
            focusedNodeCode = null
            inReaderMode = true
            viewModel.setModuleFlashcardMode(false)
        } else if (openFlash) {
            openedDirectlyToReader = true
            focusedNodeCode = null
            inReaderMode = true
            viewModel.setModuleFlashcardMode(true)
        }
    }

    if (inReaderMode) {
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
                viewModel.setModuleFlashcardMode(false)
            },
            onBackOverride = {
                if (openedDirectlyToReader) {
                    viewModel.navigateBack()
                } else {
                    focusedNodeCode = null
                    inReaderMode = false
                    viewModel.setModuleFlashcardMode(false)
                }
            },
            onNodeJustCompleted = { rawCode, completedBefore, skippedBefore ->
                val normCode = LegacyNodeCodes.normalize(rawCode)
                if (normCode !in celebratedThisSession) {
                    val nodeObj = module.nodes.find {
                        LegacyNodeCodes.normalize(it.code) == normCode
                    }
                    if (nodeObj != null) {
                        celebratedThisSession.add(normCode)
                        celebrationEarnedXp = XpCalculator.earnedForNodeCompletion(
                            node = nodeObj,
                            module = module,
                            completedCodesBefore = completedBefore,
                            skippedCodesBefore = skippedBefore
                        )
                        celebrationNodeCode = nodeObj.code
                        val beforeProgress = PathStateResolver.resolveModule(
                            module = module,
                            allModules = state.modulesList,
                            completedCodes = completedBefore,
                            skippedCodes = skippedBefore
                        ).progress
                        val afterProgress = PathStateResolver.resolveModule(
                            module = module,
                            allModules = state.modulesList,
                            completedCodes = completedBefore + normCode,
                            skippedCodes = skippedBefore
                        ).progress
                        if (ModuleCompletionCelebration.shouldShow(
                                moduleId = module.id,
                                before = beforeProgress,
                                after = afterProgress,
                                alreadyCelebratedModuleIds = celebratedModuleIds
                            )
                        ) {
                            pendingModuleCelebration = true
                            scope.launch { pathHolder.markModuleCelebrated(module.id) }
                        }
                    }
                }
            },
            onMarkSkippedNodeAsRead = { code ->
                scope.launch {
                    pathHolder.markNodeRead(code)
                }
            }
        )
    } else {
        ModulePathScreen(
            module = module,
            state = state,
            viewModel = viewModel,
            pathHolder = pathHolder,
            skippedCodes = skippedCodes,
            onOpenNodeReader = { code ->
                openedDirectlyToReader = false
                focusedNodeCode = code
                inReaderMode = true
                viewModel.setModuleFlashcardMode(false)
            },
            onOpenAllNodesReader = {
                openedDirectlyToReader = false
                focusedNodeCode = null
                inReaderMode = true
                viewModel.setModuleFlashcardMode(false)
            },
            onOpenFlashcards = {
                openedDirectlyToReader = false
                focusedNodeCode = null
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
                        inReaderMode = true
                        viewModel.setModuleFlashcardMode(false)
                    } else {
                        openedDirectlyToReader = false
                        focusedNodeCode = null
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
                        inReaderMode = false
                        viewModel.setModuleFlashcardMode(false)
                    }
                }
            )
        }
    }

    if (showModuleCompleted) {
        val currentProgress = remember(module, state.modulesList, state.completedNodeCodes) {
            PathStateResolver.resolveModule(module, state.modulesList, state.completedNodeCodes).progress
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
                inReaderMode = false
                viewModel.openModuleDetail(target)
            },
            onReturnToPath = {
                showModuleCompleted = false
                openedDirectlyToReader = false
                focusedNodeCode = null
                inReaderMode = false
                viewModel.setModuleFlashcardMode(false)
            }
        )
    }
}
