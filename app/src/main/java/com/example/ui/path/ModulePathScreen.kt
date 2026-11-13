package com.example.ui.path

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LegacyNodeCodes
import com.example.data.MriModule
import com.example.data.MriNode
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.LevelQuizCompletionState
import com.example.util.LevelQuizStatus
import com.example.util.ModulePathState
import com.example.util.NodeDisplay
import com.example.util.NodeStatus
import com.example.util.PATH_STRICT_LOCKS
import com.example.util.PathNodeState
import com.example.util.PathStateResolver
import com.example.util.SkipTestBuilder
import com.example.util.SkipTestPlan
import com.example.viewmodel.MriUiState
import com.example.viewmodel.MriViewModel
import kotlinx.coroutines.launch

private const val PATH_SCROLL_RESTORED_FLAG = 1 shl 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulePathScreen(
    module: MriModule,
    state: MriUiState,
    viewModel: MriViewModel,
    pathHolder: ModulePathStateHolder,
    skippedCodes: Set<String>,
    onOpenNodeReader: (String) -> Unit,
    onOpenAllNodesReader: () -> Unit,
    onOpenFlashcards: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val reduceMotion = rememberReduceMotion()
    val scope = rememberCoroutineScope()
    val accentColor = remember(module.code) { moduleAccentColor(module.code) }

    val pathState: ModulePathState = remember(
        module,
        state.modulesList,
        state.completedNodeCodes,
        skippedCodes
    ) {
        pathHolder.resolveModulePath(
            module = module,
            allModules = state.modulesList,
            completedCodes = state.completedNodeCodes,
            skippedCodes = skippedCodes,
            strictLocks = PATH_STRICT_LOCKS
        )
    }

    val moduleQuizzes = remember(state.quizzesList, module.id) {
        state.quizzesList.filter { it.moduleId == module.id }
    }
    val quizCountByNode = remember(moduleQuizzes) {
        moduleQuizzes.groupingBy { LegacyNodeCodes.normalize(it.nodeCode) }.eachCount()
    }
    val preferredQuizLevelByNode = remember(moduleQuizzes, module.nodes) {
        val levelByNode = module.nodes.associate {
            LegacyNodeCodes.normalize(it.code) to it.level.coerceIn(1, 3)
        }
        moduleQuizzes.groupBy { LegacyNodeCodes.normalize(it.nodeCode) }.mapValues { (normCode, quizzes) ->
            val nodeLevel = levelByNode[normCode]
            if (nodeLevel != null && quizzes.any { it.level == nodeLevel }) {
                nodeLevel
            } else {
                quizzes.first().level.coerceIn(1, 3)
            }
        }
    }
    val sections = remember(pathState, quizCountByNode) {
        pathHolder.buildSections(pathState, quizCountByNode)
    }

    val levelQuizByLevel = remember(moduleQuizzes, pathState, state.quizAttempts) {
        (1..3).associateWith { lvl ->
            LevelQuizStatus.compute(
                level = lvl,
                levelQuizzes = moduleQuizzes.filter { it.level == lvl },
                levelNodeStates = pathState.nodes.filter { it.node.level == lvl },
                attempts = state.quizAttempts,
                strictLocks = PATH_STRICT_LOCKS
            )
        }
    }

    // Pemetaan indeks global node -> indeks item dalam LazyColumn agar auto-scroll akurat
    val lazyIndexByNodeCode = remember(sections, levelQuizByLevel) {
        buildMap {
            var listIdx = 0
            sections.forEach { section ->
                listIdx++ // header section
                section.nodes.forEach { item ->
                    put(LegacyNodeCodes.normalize(item.state.node.code), listIdx)
                    listIdx++
                }
                val sectionLevel = section.nodes.firstOrNull()?.state?.node?.level ?: 0
                if (sectionLevel in 1..3 && (levelQuizByLevel[sectionLevel]?.questionCount ?: 0) > 0) {
                    listIdx++ // kuis tingkat item
                }
            }
        }
    }

    val screenKey = "path_${module.id}"
    val initialPackedScroll = remember(screenKey) { viewModel.getSavedScrollOffset(screenKey) }
    val hasSavedScroll = (initialPackedScroll and PATH_SCROLL_RESTORED_FLAG) != 0
    val initialIndex = if (hasSavedScroll) ((initialPackedScroll ushr 16) and 0x3FFF) else 0
    val initialOffset = if (hasSavedScroll) (initialPackedScroll and 0xFFFF) else 0

    val listState: LazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex,
        initialFirstVisibleItemScrollOffset = initialOffset
    )

    DisposableEffect(screenKey, listState) {
        onDispose {
            val packed = PATH_SCROLL_RESTORED_FLAG or
                ((listState.firstVisibleItemIndex and 0x3FFF) shl 16) or
                (listState.firstVisibleItemScrollOffset.coerceIn(0, 0xFFFF))
            viewModel.updateScrollOffset(screenKey, packed)
        }
    }

    // Gulir otomatis ke simpul CURRENT hanya pada kunjungan awal saat belum ada posisi tersimpan
    LaunchedEffect(module.id, hasSavedScroll) {
        if (!hasSavedScroll) {
            val currentNode = PathStateResolver.currentNodeOf(pathState)
            val targetLazyIdx = currentNode?.let {
                lazyIndexByNodeCode[LegacyNodeCodes.normalize(it.node.code)]
            }
            if (targetLazyIdx != null && targetLazyIdx > 1) {
                val scrollTarget = (targetLazyIdx - 1).coerceAtLeast(0)
                if (reduceMotion) {
                    listState.scrollToItem(scrollTarget)
                } else {
                    listState.animateScrollToItem(scrollTarget)
                }
            }
        }
    }

    var selectedNodeState by remember(module.id) { mutableStateOf<PathNodeState?>(null) }
    var showQuizSheet by remember(module.id) { mutableStateOf(false) }
    var activeSkipTestPlan by remember(module.id) { mutableStateOf<SkipTestPlan?>(null) }

    val currentNodeState = remember(pathState) { PathStateResolver.currentNodeOf(pathState) }
    val currentNodeKey = currentNodeState?.let { "node_${module.id}_${it.node.code}" }
    val currentNodeLazyIdx = currentNodeState?.let {
        lazyIndexByNodeCode[LegacyNodeCodes.normalize(it.node.code)]
    }
    val showJumpToCurrent by remember(listState, currentNodeKey, currentNodeLazyIdx) {
        derivedStateOf {
            if (currentNodeKey == null || currentNodeLazyIdx == null) {
                false
            } else {
                listState.layoutInfo.totalItemsCount > 0 &&
                    listState.layoutInfo.visibleItemsInfo.none { it.key == currentNodeKey }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ModulePathStickyHeader(
            module = module,
            pathState = pathState,
            onBack = { viewModel.navigateBack() },
            onOpenAllNodes = onOpenAllNodesReader,
            onOpenFlashcards = onOpenFlashcards
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("module_path_list_${module.id}"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 14.dp,
                    bottom = if (showJumpToCurrent) 80.dp else 14.dp
                )
            ) {
                sections.forEachIndexed { sectionIdx, section ->
                    val sectionLevel = section.nodes.firstOrNull()?.state?.node?.level ?: 0
                    val levelQuizSummary = if (sectionLevel in 1..3) {
                        levelQuizByLevel[sectionLevel]?.takeIf { it.questionCount > 0 }
                    } else {
                        null
                    }
                    val sectionDoneCount = section.nodes.count {
                        it.state.status == NodeStatus.DONE || it.state.status == NodeStatus.SKIPPED
                    }
                    val sectionTotalCount = section.nodes.size

                    val prevSectionExitX: Float? = if (sectionIdx > 0) {
                        val prevSec = sections[sectionIdx - 1]
                        val prevLvl = prevSec.nodes.firstOrNull()?.state?.node?.level ?: 0
                        val prevQuiz = if (prevLvl in 1..3) levelQuizByLevel[prevLvl]?.takeIf { it.questionCount > 0 } else null
                        val lastPrevNode = prevSec.nodes.lastOrNull()
                        if (prevQuiz != null && lastPrevNode != null) {
                            PathGeometry.xFractionFor(lastPrevNode.globalIndex + 1)
                        } else if (lastPrevNode != null) {
                            PathGeometry.xFractionFor(lastPrevNode.globalIndex)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                    val firstSectionNode = section.nodes.firstOrNull()

                    item(key = "sec_${module.id}_${section.startIndex}_${section.title}") {
                        SectionDivider(
                            title = section.title,
                            subtitle = section.subtitle,
                            doneCount = sectionDoneCount,
                            totalCount = sectionTotalCount,
                            topEntryX = prevSectionExitX,
                            topEntryDone = sectionDoneCount > 0,
                            bottomExitX = firstSectionNode?.let { PathGeometry.xFractionFor(it.globalIndex) },
                            bottomExitDone = firstSectionNode?.let {
                                it.state.status == NodeStatus.DONE || it.state.status == NodeStatus.SKIPPED
                            } ?: false
                        )
                    }

                    val sectionIllType = when {
                        sectionIdx == 0 -> NanoIllustrationType.MRI_SCANNER
                        module.code.startsWith("M01") -> NanoIllustrationType.HYDROGEN_ATOM
                        module.code.startsWith("M02") -> NanoIllustrationType.MRI_BRAIN_PHOTO
                        module.code.startsWith("M03") -> NanoIllustrationType.RF_PULSE
                        module.code.startsWith("M04") -> NanoIllustrationType.K_SPACE
                        module.code.startsWith("M05") -> NanoIllustrationType.CRYO_HELIUM
                        module.code.startsWith("M06") -> NanoIllustrationType.GRADIENT_COIL
                        module.code.startsWith("M07") -> NanoIllustrationType.SAFETY_ZONES
                        module.code.startsWith("M08") -> NanoIllustrationType.QA_PHANTOM
                        else -> NanoIllustrationType.CLINICAL_CONSOLE
                    }

                    section.nodes.forEachIndexed { localIdx, indexedNode ->
                        val nodeState = indexedNode.state
                        val nodeCode = nodeState.node.code
                        val xFraction = PathGeometry.xFractionFor(indexedNode.globalIndex)
                        val prevNode = if (localIdx > 0) section.nodes[localIdx - 1] else null
                        val nextNode = if (localIdx < section.nodes.lastIndex) section.nodes[localIdx + 1] else null
                        val nextTargetX = nextNode?.let { PathGeometry.xFractionFor(it.globalIndex) }
                            ?: if (levelQuizSummary != null) PathGeometry.xFractionFor(indexedNode.globalIndex + 1) else null
                        val inDone = if (prevNode != null) {
                            (prevNode.state.status == NodeStatus.DONE || prevNode.state.status == NodeStatus.SKIPPED) &&
                                nodeState.status != NodeStatus.LOCKED
                        } else {
                            nodeState.status == NodeStatus.DONE || nodeState.status == NodeStatus.SKIPPED
                        }
                        val outDone = if (nextNode != null) {
                            (nodeState.status == NodeStatus.DONE || nodeState.status == NodeStatus.SKIPPED) &&
                                nextNode.state.status != NodeStatus.LOCKED
                        } else if (levelQuizSummary != null) {
                            (nodeState.status == NodeStatus.DONE || nodeState.status == NodeStatus.SKIPPED) &&
                                levelQuizSummary.nodeStatus != NodeStatus.LOCKED
                        } else {
                            nodeState.status == NodeStatus.DONE || nodeState.status == NodeStatus.SKIPPED
                        }
                        val nodeCenterYDp = 56.dp
                        val hasFollowingSectionOrEval = sectionIdx < sections.lastIndex || moduleQuizzes.isNotEmpty()

                        item(key = "node_${module.id}_$nodeCode") {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                SeamlessNodePathBackdrop(
                                    nodeX = xFraction,
                                    prevX = prevNode?.let { PathGeometry.xFractionFor(it.globalIndex) },
                                    nextX = nextTargetX,
                                    incomingDone = inDone,
                                    outgoingDone = outDone,
                                    nodeCenterY = nodeCenterYDp,
                                    accentColor = CyanBright,
                                    strokeWidthDp = 6.dp,
                                    connectFromHeader = prevNode == null,
                                    connectToFooter = nextTargetX == null && hasFollowingSectionOrEval,
                                    modifier = Modifier.matchParentSize()
                                )

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Mini item proporsional di samping kotak materi tanpa menggeser tata letak peta
                                        if (localIdx == 0 && sectionIdx <= 2) {
                                            val putOnRight = xFraction <= 0.5f
                                            InteractiveNanoIllustration(
                                                type = sectionIllType,
                                                alignRight = putOnRight,
                                                modifier = Modifier
                                                    .align(if (putOnRight) Alignment.CenterEnd else Alignment.CenterStart)
                                                    .padding(horizontal = 22.dp, vertical = 4.dp)
                                            )
                                        }

                                        PathAlignedSlot(
                                            xFraction = xFraction
                                        ) {
                                            PathNodeSlotContent(
                                                indexedNode = indexedNode,
                                                accentColor = accentColor,
                                                onClick = { selectedNodeState = nodeState }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(if (nextTargetX != null) 28.dp else 16.dp))
                                }
                            }
                        }
                    }

                    if (levelQuizSummary != null && section.nodes.isNotEmpty()) {
                        val lastSectionNode = section.nodes.last()
                        val quizGlobalIdx = lastSectionNode.globalIndex + 1
                        val quizXFraction = PathGeometry.xFractionFor(quizGlobalIdx)
                        val quizInDone = (lastSectionNode.state.status == NodeStatus.DONE || lastSectionNode.state.status == NodeStatus.SKIPPED) &&
                            levelQuizSummary.nodeStatus != NodeStatus.LOCKED
                        val hasFollowingSectionOrEval = sectionIdx < sections.lastIndex || moduleQuizzes.isNotEmpty()
                        item(key = "level_quiz_${module.id}_T$sectionLevel") {
                            val quizTitle = "Kuis Tingkat $sectionLevel"
                            val quizPill = when (levelQuizSummary.status) {
                                LevelQuizCompletionState.SELESAI ->
                                    "${levelQuizSummary.latestCorrectCount}/${levelQuizSummary.questionCount}"
                                else -> "${levelQuizSummary.questionCount} soal"
                            }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                SeamlessNodePathBackdrop(
                                    nodeX = quizXFraction,
                                    prevX = PathGeometry.xFractionFor(lastSectionNode.globalIndex),
                                    nextX = null,
                                    incomingDone = quizInDone,
                                    outgoingDone = levelQuizSummary.status == LevelQuizCompletionState.SELESAI,
                                    nodeCenterY = 56.dp,
                                    accentColor = CyanBright,
                                    strokeWidthDp = 6.dp,
                                    connectFromHeader = false,
                                    connectToFooter = hasFollowingSectionOrEval,
                                    modifier = Modifier.matchParentSize()
                                )

                                PathAlignedSlot(
                                    xFraction = quizXFraction,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                                ) {
                                    PathNodeButton(
                                        status = levelQuizSummary.nodeStatus,
                                        kind = NodeKind.KUIS_TINGKAT,
                                        label = "KT$sectionLevel",
                                        onClick = { viewModel.openModuleQuiz(module.id, level = sectionLevel) },
                                        accentColor = AmberWarning,
                                        progressPill = quizPill,
                                        semanticLabel = "$quizTitle ${module.code}, ${levelQuizSummary.questionCount} soal",
                                        modifier = Modifier.testTag("path_level_quiz_${module.id}_T$sectionLevel")
                                    )
                                }
                            }
                        }
                    }
                }

            if (moduleQuizzes.isNotEmpty()) {
                val lastSec = sections.lastOrNull()
                val lastSecLvl = lastSec?.nodes?.firstOrNull()?.state?.node?.level ?: 0
                val lastSecQuiz = if (lastSecLvl in 1..3) levelQuizByLevel[lastSecLvl]?.takeIf { it.questionCount > 0 } else null
                val lastSecNode = lastSec?.nodes?.lastOrNull()
                val evalEntryX = if (lastSecQuiz != null && lastSecNode != null) {
                    PathGeometry.xFractionFor(lastSecNode.globalIndex + 1)
                } else if (lastSecNode != null) {
                    PathGeometry.xFractionFor(lastSecNode.globalIndex)
                } else {
                    0.5f
                }

                item(key = "sec_eval_${module.id}") {
                    SectionDivider(
                        title = "Evaluasi Akhir ${module.code}",
                        subtitle = "${moduleQuizzes.size} soal kuis tersedia",
                        doneCount = pathState.progress.done,
                        totalCount = pathState.progress.total,
                        topEntryX = evalEntryX,
                        topEntryDone = pathState.progress.done == pathState.progress.total && pathState.progress.total > 0,
                        bottomExitX = 0.5f,
                        bottomExitDone = pathState.progress.done == pathState.progress.total && pathState.progress.total > 0
                    )
                }
                item(key = "quiz_capstone_${module.id}") {
                    val evalStatus = if (pathState.progress.done == pathState.progress.total && pathState.progress.total > 0) {
                        NodeStatus.CURRENT
                    } else {
                        NodeStatus.OPEN
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SeamlessNodePathBackdrop(
                            nodeX = 0.5f,
                            prevX = null,
                            nextX = null,
                            incomingDone = pathState.progress.done == pathState.progress.total && pathState.progress.total > 0,
                            outgoingDone = false,
                            nodeCenterY = 56.dp,
                            accentColor = CyanBright,
                            strokeWidthDp = 6.dp,
                            connectFromHeader = true,
                            connectToFooter = false,
                            modifier = Modifier.matchParentSize()
                        )
                        PathAlignedSlot(
                            xFraction = 0.5f,
                            modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
                        ) {
                            PathNodeButton(
                                status = evalStatus,
                                kind = NodeKind.KUIS_MODUL,
                                label = "EVAL",
                                onClick = { showQuizSheet = true },
                                accentColor = accentColor,
                                progressPill = "${moduleQuizzes.size} soal",
                                semanticLabel = "Kuis Evaluasi ${module.code}",
                                modifier = Modifier.testTag("path_quiz_node_${module.id}")
                            )
                        }
                    }
                }
            } else {
                item(key = "spacer_end_${module.id}") {
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }

            // Di bagian materi, Nano berada di pojok bawah kiri (balon awan membuka ke atas-kanan)
            FloatingNanoCompanion(
                characterCustomization = state.characterCustomization,
                hints = listOf(
                    "Ini peta modul ${module.code}! Ketuk kotak 3D materi untuk membaca atau memulai lab.",
                    "Ketuk ilustrasi gaya Nano di samping kotak untuk fakta fisika singkat!",
                    "Setelah selesai membaca tiap tingkat, coba Kuis Tingkat (KT1–KT3)."
                ),
                openBubbleUpward = true,
                alignStart = true,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 14.dp)
            )

            if (showJumpToCurrent && currentNodeLazyIdx != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            scope.launch {
                                val target = (currentNodeLazyIdx - 1).coerceAtLeast(0)
                                if (reduceMotion) {
                                    listState.scrollToItem(target)
                                } else {
                                    listState.animateScrollToItem(target)
                                }
                            }
                        }
                        .testTag("path_jump_to_active"),
                    shape = RoundedCornerShape(24.dp),
                    color = CyanPrimary,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = DeepObsidian,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Ke materi aktif",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepObsidian
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheet Detail Materi
    val sheetNodeState = selectedNodeState
    if (sheetNodeState != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val activeNodeState = remember(pathState) { PathStateResolver.currentNodeOf(pathState) }
        val nodeIndex = pathState.nodes.indexOfFirst { it.node.code == sheetNodeState.node.code }
        val previousNode = if (nodeIndex > 0) pathState.nodes[nodeIndex - 1] else null
        val skipPlan = remember(
            sheetNodeState.node.code,
            module,
            state.completedNodeCodes,
            state.quizzesList
        ) {
            if (PATH_STRICT_LOCKS && sheetNodeState.status == NodeStatus.LOCKED) {
                SkipTestBuilder.build(
                    targetNodeCode = sheetNodeState.node.code,
                    module = module,
                    completedCodes = state.completedNodeCodes,
                    allQuizzes = state.quizzesList
                )
            } else {
                null
            }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedNodeState = null },
            sheetState = sheetState,
            containerColor = DarkNavySlate,
            contentColor = TextPrimary
        ) {
            NodePreviewBottomSheetContent(
                module = module,
                nodeState = sheetNodeState,
                quizCount = quizCountByNode[LegacyNodeCodes.normalize(sheetNodeState.node.code)] ?: 0,
                preferredQuizLevel = preferredQuizLevelByNode[LegacyNodeCodes.normalize(sheetNodeState.node.code)]
                    ?: sheetNodeState.node.level.coerceIn(1, 3),
                strictLocks = PATH_STRICT_LOCKS,
                activeNodeState = activeNodeState,
                previousNodeState = previousNode,
                skipPlan = skipPlan,
                onDismiss = { selectedNodeState = null },
                onStartReading = {
                    val code = sheetNodeState.node.code
                    selectedNodeState = null
                    onOpenNodeReader(code)
                },
                onLaunchLab = { labType ->
                    selectedNodeState = null
                    viewModel.launchLab(labType)
                },
                onOpenNodeQuiz = { lvl ->
                    selectedNodeState = null
                    viewModel.openModuleQuiz(module.id, lvl)
                },
                onJumpToActiveNode = { targetCode ->
                    selectedNodeState = null
                    val targetIdx = lazyIndexByNodeCode[LegacyNodeCodes.normalize(targetCode)]
                    if (targetIdx != null) {
                        scope.launch {
                            val scrollTarget = (targetIdx - 1).coerceAtLeast(0)
                            if (reduceMotion) {
                                listState.scrollToItem(scrollTarget)
                            } else {
                                listState.animateScrollToItem(scrollTarget)
                            }
                        }
                    }
                },
                onStartSkipTest = { plan ->
                    selectedNodeState = null
                    activeSkipTestPlan = plan
                }
            )
        }
    }

    val currentSkipPlan = activeSkipTestPlan
    if (currentSkipPlan != null) {
        SkipTestDialog(
            plan = currentSkipPlan,
            onTestPassed = { codes ->
                scope.launch {
                    pathHolder.markNodesSkippedByTest(codes)
                }
            },
            onOpenTargetNode = {
                val targetCode = currentSkipPlan.targetNodeCode
                activeSkipTestPlan = null
                onOpenNodeReader(targetCode)
            },
            onDismiss = { activeSkipTestPlan = null }
        )
    }

    // Bottom Sheet Evaluasi Kuis Modul
    if (showQuizSheet && moduleQuizzes.isNotEmpty()) {
        val quizSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showQuizSheet = false },
            sheetState = quizSheetState,
            containerColor = DarkNavySlate,
            contentColor = TextPrimary
        ) {
            ModuleQuizSheetContent(
                module = module,
                quizzes = moduleQuizzes,
                onStartQuiz = { lvl ->
                    showQuizSheet = false
                    viewModel.openModuleQuiz(module.id, lvl)
                }
            )
        }
    }
}

@Composable
private fun ModulePathStickyHeader(
    module: MriModule,
    pathState: ModulePathState,
    onBack: () -> Unit,
    onOpenAllNodes: () -> Unit,
    onOpenFlashcards: () -> Unit
) {
    val progress = pathState.progress
    val isComplete = progress.done == progress.total && progress.total > 0
    val reduceMotion = rememberReduceMotion()
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress.fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 650),
        label = "moduleHeaderCircleProgress"
    )
    val ringColor = if (isComplete) EmeraldSuccess else CyanBright

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkNavySlate,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("path_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Beranda",
                        tint = TextPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyanDark,
                        border = BorderStroke(1.dp, CyanPrimary)
                    ) {
                        Text(
                            text = module.code,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyanBright,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = module.title.replace("Ruang-k", "K-space").replace("ruang-k", "K-space"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                // Lingkaran berprogres di samping kanan judul berisi angka materi selesai (mis. "0/5")
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = "Progres materi ${progress.done} dari ${progress.total}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 4.dp.toPx()
                        val inset = strokeW / 2f + 1.dp.toPx()
                        val arcSize = androidx.compose.ui.geometry.Size(
                            size.width - inset * 2f,
                            size.height - inset * 2f
                        )
                        drawCircle(
                            color = DeepObsidian.copy(alpha = 0.80f),
                            radius = (size.minDimension - strokeW) / 2f
                        )
                        drawArc(
                            color = CardSlateBorder.copy(alpha = 0.55f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                            size = arcSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeW,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                        val sweep = when {
                            progress.total <= 0 -> 0f
                            progress.done <= 0 -> 18f
                            else -> (animatedProgress * 360f).coerceAtLeast(24f)
                        }
                        val startDeg = -90f + animatedProgress * 180f
                        drawArc(
                            color = if (progress.done > 0) ringColor else CyanBright.copy(alpha = 0.55f),
                            startAngle = startDeg,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                            size = arcSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeW,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                    Text(
                        text = "${progress.done}/${progress.total}",
                        color = if (isComplete) EmeraldSuccess else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenAllNodes,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("path_open_all_nodes"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CardSlateBorder)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = null,
                        tint = CyanBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Semua Materi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onOpenFlashcards,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("path_open_flashcards"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CardSlateBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = CyanBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Flashcards",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PathNodeSlotContent(
    indexedNode: IndexedPathNode,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val node = indexedNode.state.node
    val status = indexedNode.state.status
    val shortCode = NodeDisplay.shortCode(node.code)
    val cleanTitle = NodeDisplay.displayTitle(node)

    // Semua simpul materi menjadi kotak toggle bergaya "M01" tanpa teks penjelasan di bawahnya
    PathNodeButton(
        status = status,
        kind = indexedNode.kind,
        label = shortCode,
        onClick = onClick,
        accentColor = accentColor,
        hasLabBadge = !node.labType.isNullOrBlank(),
        semanticLabel = "$shortCode $cleanTitle",
        modifier = Modifier.testTag("path_node_${node.code}")
    )
}

@Composable
private fun NodePreviewBottomSheetContent(
    module: MriModule,
    nodeState: PathNodeState,
    quizCount: Int,
    preferredQuizLevel: Int,
    strictLocks: Boolean,
    activeNodeState: PathNodeState?,
    previousNodeState: PathNodeState?,
    skipPlan: SkipTestPlan?,
    onDismiss: () -> Unit,
    onStartReading: () -> Unit,
    onLaunchLab: (String) -> Unit,
    onOpenNodeQuiz: (Int) -> Unit,
    onJumpToActiveNode: (String) -> Unit,
    onStartSkipTest: (SkipTestPlan) -> Unit
) {
    val node = nodeState.node
    val isLockedBlocked = strictLocks && nodeState.status == NodeStatus.LOCKED
    val readMinutes = remember(node.detailFull) {
        val wordCount = node.detailFull.trim().split(Regex("\\s+")).size
        (wordCount / 130).coerceIn(2, 8)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DeepObsidian,
                    border = BorderStroke(1.dp, CardSlateBorder)
                ) {
                    Text(
                        text = NodeDisplay.shortCode(node.code),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyanBright,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                val (statusText, statusBg, statusFg) = when {
                    nodeState.status == NodeStatus.DONE -> Triple("Selesai", EmeraldDark, EmeraldSuccess)
                    nodeState.status == NodeStatus.SKIPPED -> Triple("Dilewati Tes", EmeraldDark.copy(alpha = 0.75f), CyanBright)
                    nodeState.status == NodeStatus.CURRENT -> Triple("Aktif", CyanDark, CyanBright)
                    isLockedBlocked -> Triple("Terkunci", CardSlate, TextMuted)
                    else -> Triple("Terbuka", CardSlate, TextSecondary)
                }
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "$readMinutes mnt",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                if (quizCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CardSlate
                    ) {
                        Text(
                            text = "$quizCount kuis",
                            fontSize = 11.sp,
                            color = CyanBright,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = node.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = node.summaryShort,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 20.sp
        )

        if (!nodeState.softPrerequisiteHint.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.14f)),
                border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Disarankan memahami lebih dulu: ${nodeState.softPrerequisiteHint}",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLockedBlocked) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, CardSlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        val blockerCode = previousNodeState?.node?.code ?: module.code
                        Text(
                            text = "Materi ini belum terbuka",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Selesaikan $blockerCode terlebih dahulu untuk membuka materi ini secara bertahap.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val jumpTargetCode = activeNodeState?.node?.code ?: previousNodeState?.node?.code ?: module.code
            Button(
                onClick = { onJumpToActiveNode(jumpTargetCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("sheet_jump_active_${node.code}"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = DeepObsidian,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Lanjut ke $jumpTargetCode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepObsidian
                )
            }

            if (skipPlan != null && skipPlan.canOfferSkipTest) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onStartSkipTest(skipPlan) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("sheet_skip_test_${node.code}"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.75f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = CyanBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lompat dengan tes singkat (${skipPlan.questions.size} soal)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanBright
                    )
                }
            }
        } else {
            if (nodeState.status == NodeStatus.SKIPPED) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyanDark.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "Materi ini ditandai dilewati lewat tes singkat. Baca dan tandai selesai untuk memahami penuh serta memperoleh +10 XP.",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Button(
                onClick = onStartReading,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("sheet_start_learning_${node.code}"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (nodeState.status == NodeStatus.DONE) EmeraldSuccess else CyanPrimary
                )
            ) {
                Icon(
                    imageVector = if (nodeState.status == NodeStatus.DONE) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    tint = DeepObsidian,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (nodeState.status) {
                        NodeStatus.DONE -> "Baca ulang materi"
                        NodeStatus.SKIPPED -> "Baca & klaim +10 XP"
                        else -> "Mulai belajar"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepObsidian
                )
            }

            if (!node.labType.isNullOrBlank() || quizCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!node.labType.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { onLaunchLab(node.labType) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CyanPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = CyanBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Buka Lab",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanBright
                            )
                        }
                    }

                    if (quizCount > 0) {
                        OutlinedButton(
                            onClick = { onOpenNodeQuiz(preferredQuizLevel) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardSlateBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Latihan Kuis",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleQuizSheetContent(
    module: MriModule,
    quizzes: List<com.example.data.QuizQuestion>,
    onStartQuiz: (Int) -> Unit
) {
    val t1Count = quizzes.count { it.level == 1 }
    val t2Count = quizzes.count { it.level == 2 }
    val t3Count = quizzes.count { it.level == 3 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(
            text = "Evaluasi Pemahaman ${module.code}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tersedia ${quizzes.size} soal kuis untuk menguji pemahaman materi ${module.title}.",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (t1Count > 0 || t2Count > 0 || t3Count > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (t1Count > 0) {
                    OutlinedButton(
                        onClick = { onStartQuiz(1) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.7f))
                    ) {
                        Text("T1 ($t1Count)", fontSize = 12.sp, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                    }
                }
                if (t2Count > 0) {
                    OutlinedButton(
                        onClick = { onStartQuiz(2) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyanBright.copy(alpha = 0.7f))
                    ) {
                        Text("T2 ($t2Count)", fontSize = 12.sp, color = CyanBright, fontWeight = FontWeight.Bold)
                    }
                }
                if (t3Count > 0) {
                    OutlinedButton(
                        onClick = { onStartQuiz(3) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.7f))
                    ) {
                        Text("T3 ($t3Count)", fontSize = 12.sp, color = AmberWarning, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { onStartQuiz(0) },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
        ) {
            Icon(
                imageVector = Icons.Default.Quiz,
                contentDescription = null,
                tint = DeepObsidian,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Mulai Kuis ${module.code} (${quizzes.size} Soal)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DeepObsidian
            )
        }
    }
}

private fun shortNodeLabel(node: MriNode): String {
    val cleanTitle = node.title
        .replace(Regex("^T\\d+(?:\\.\\d+[A-Za-z]?|\\.R)?\\s+"), "")
        .trim()
    return "${node.code} · $cleanTitle"
}
