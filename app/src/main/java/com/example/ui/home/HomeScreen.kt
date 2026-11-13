package com.example.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MriModule
import com.example.data.MriRepository
import com.example.ui.path.FloatingNanoCompanion
import com.example.ui.path.NodeKind
import com.example.ui.path.PathAlignedSlot
import com.example.ui.path.PathConnector
import com.example.ui.path.PathGeometry
import com.example.ui.path.PathNavigationHints
import com.example.util.MapGuideStateMachine
import com.example.util.MapGuideStep
import com.example.util.ModuleOverview
import com.example.util.NodeDisplay
import kotlinx.coroutines.launch
import com.example.ui.path.PathNodeButton
import com.example.ui.path.SeamlessNodePathBackdrop
import com.example.ui.path.SectionDivider
import com.example.ui.path.moduleAccentColor
import com.example.ui.path.rememberModulePathStateHolder
import com.example.ui.path.rememberSkippedNodeCodes
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlate
import com.example.ui.path.InteractiveNanoIllustration
import com.example.ui.path.NanoIllustrationType
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDark
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.util.DailyShiftMission
import com.example.util.HomePathPlanner
import com.example.util.NodeStatus
import com.example.util.SpacedRepetition
import com.example.util.XpCalculator
import com.example.viewmodel.MainTab
import com.example.viewmodel.MriUiState
import com.example.viewmodel.MriViewModel

@Composable
fun HomeScreen(
    state: MriUiState,
    viewModel: MriViewModel,
    modifier: Modifier = Modifier
) {
    val pathHolder = rememberModulePathStateHolder()
    val skippedCodes = rememberSkippedNodeCodes(pathHolder)
    val hasSeenMapGuide by pathHolder.hasSeenMapGuideFlow.collectAsState(initial = null)
    val todayKey = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }
    val dailyCounts by pathHolder.observeDailyShiftCounts(todayKey).collectAsState(initial = Triple(0, 0, 0))
    val triedLabNodes by pathHolder.triedLabNodesFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val haptics = com.example.util.rememberMriHaptics()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    var homeRootTopInRoot by remember { mutableStateOf(0f) }
    var firstModuleBounds by remember { mutableStateOf<Rect?>(null) }
    var guideStep by remember { mutableStateOf(MapGuideStep.NONE) }
    var showLegendFromInfo by remember { mutableStateOf(false) }
    var selectedModuleForSheet by remember { mutableStateOf<MriModule?>(null) }

    LaunchedEffect(hasSeenMapGuide, state.isResumeRestored, state.completedCount, state.modulesList.isNotEmpty(), firstModuleBounds != null) {
        if (!PathNavigationHints.hasShownMapGuideThisSession &&
            hasSeenMapGuide == false &&
            state.isResumeRestored &&
            state.modulesList.isNotEmpty() &&
            guideStep == MapGuideStep.NONE
        ) {
            if (state.completedCount > 0) {
                pathHolder.markMapGuideSeenAsync()
                guideStep = MapGuideStep.DONE
            } else if (firstModuleBounds != null) {
                pathHolder.markMapGuideSeenAsync()
                guideStep = MapGuideStateMachine.initialStep(
                    hasSeenGuide = false,
                    completedNodeCount = state.completedCount,
                    hasSpotlightTarget = true,
                    screenWidthDp = screenWidthDp
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .onGloballyPositioned { coords ->
                homeRootTopInRoot = coords.boundsInRoot().top
            }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        // Top Header: Pinned at top, does NOT scroll away when swiping up!
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sisi Kiri: Avatar Pengguna & Judul Aplikasi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gambar Avatar Interaktif
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, CyanBright, CircleShape)
                        .clickable { viewModel.setAvatarDialogVisible(true) },
                    contentAlignment = Alignment.Center
                ) {
                    val avatarRes = MriRepository.getAvatarDrawableRes(state.userAvatarId)
                    Image(
                        painter = painterResource(id = avatarRes),
                        contentDescription = "Avatar Pengguna",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = state.userName.ifBlank { "Pengguna" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanBright
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "MRI EDU v${com.example.BuildConfig.VERSION_NAME}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        text = "Belajar MRI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }
            }

            // Sisi Kanan: Streak 🔥 + Info ℹ️
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            haptics.lightToggle()
                            viewModel.setStreakDialogVisible(true)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF451A03),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak belajar",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${state.studyStreakDays} Hari",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                        }
                    }
                }

                // GAMBAR INFO / PANDUAN PETA
                IconButton(
                    onClick = {
                        haptics.lightToggle()
                        showLegendFromInfo = true
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkNavySlate)
                            .border(1.dp, CardSlateBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info Aplikasi",
                            tint = CyanBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Content Below Pinned Header
        val homeScreenKey = "tab_beranda"
        val homeSavedPacked = remember(homeScreenKey) { viewModel.getSavedScrollOffset(homeScreenKey) }
        val homeHasSaved = (homeSavedPacked and (1 shl 30)) != 0
        val homeInitialIndex = if (homeHasSaved) ((homeSavedPacked ushr 16) and 0x3FFF) else 0
        val homeInitialOffset = if (homeHasSaved) (homeSavedPacked and 0xFFFF) else 0
        val homeListState: LazyListState = rememberLazyListState(
            initialFirstVisibleItemIndex = homeInitialIndex,
            initialFirstVisibleItemScrollOffset = homeInitialOffset
        )

        DisposableEffect(homeScreenKey, homeListState) {
            onDispose {
                val packed = (1 shl 30) or
                    ((homeListState.firstVisibleItemIndex and 0x3FFF) shl 16) or
                    (homeListState.firstVisibleItemScrollOffset.coerceIn(0, 0xFFFF))
                viewModel.updateScrollOffset(homeScreenKey, packed)
            }
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
        val chapterGroups = remember(state.modulesList, state.completedNodeCodes) {
            HomePathPlanner.plan(state.modulesList, state.completedNodeCodes)
        }
        val dailyShift = remember(dailyCounts) {
            DailyShiftMission.evaluate(
                nodesToday = dailyCounts.first,
                quizzesToday = dailyCounts.second,
                labsToday = dailyCounts.third
            )
        }
        val dueReviews = remember(state.quizAttempts, state.modulesList) {
            SpacedRepetition.selectDueReviews(
                attempts = state.quizAttempts,
                modules = state.modulesList,
                maxItems = 3
            )
        }

        LazyColumn(
            state = homeListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("home_path_list"),
            contentPadding = PaddingValues(bottom = 84.dp)
        ) {
            item(key = "hero_progress") {
                val resumeTitle = if (state.isResumeRestored) {
                    NodeDisplay.displayTitle(state.lastVisitedNode)
                } else {
                    "Memuat posisi terakhir…"
                }
                HeroProgressCard(
                    completedCount = state.completedCount,
                    totalCount = state.totalNodesCount,
                    totalXp = xpSummary.totalXp,
                    completedModulesCount = xpSummary.completedModulesCount,
                    lastVisitedTitle = resumeTitle,
                    onContinueClicked = { viewModel.resumeStudy() }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item(key = "learning_tools") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickToolChip(
                        title = "Tersimpan",
                        icon = Icons.Default.Bookmark,
                        iconColor = AmberWarning,
                        onClick = { viewModel.selectTab(MainTab.TERSIMPAN) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickToolChip(
                        title = "Catatan",
                        icon = Icons.Default.EditNote,
                        iconColor = EmeraldSuccess,
                        onClick = { viewModel.selectTab(MainTab.CATATAN) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickToolChip(
                        title = "Glosarium",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        iconColor = CyanBright,
                        onClick = { viewModel.selectTab(MainTab.GLOSARIUM) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickToolChip(
                        title = "Animasi",
                        icon = Icons.Default.PlayArrow,
                        iconColor = VioletAccent,
                        onClick = { viewModel.selectTab(MainTab.ANIMASI) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            item(key = "daily_shift_card") {
                val shiftProgress = if (dailyShift.totalCount > 0) {
                    dailyShift.completedCount.toFloat() / dailyShift.totalCount.toFloat()
                } else {
                    0f
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = DarkNavySlate,
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (dailyShift.isAllComplete) EmeraldSuccess.copy(alpha = 0.65f) else CyanBright.copy(alpha = 0.38f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        CyanDark.copy(alpha = 0.35f),
                                        DarkNavySlate,
                                        EmeraldDark.copy(alpha = 0.22f)
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Shift Nano Hari Ini",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = if (dailyShift.isAllComplete) EmeraldDark else CyanDark,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (dailyShift.isAllComplete) EmeraldSuccess else CyanBright
                                )
                            ) {
                                Text(
                                    text = "${dailyShift.completedCount}/${dailyShift.totalCount} Misi • +${dailyShift.bonusXpEarned} XP",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dailyShift.isAllComplete) EmeraldSuccess else CyanBright,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { shiftProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(99.dp)),
                            color = if (dailyShift.isAllComplete) EmeraldSuccess else CyanBright,
                            trackColor = CardSlate
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            dailyShift.tasks.forEachIndexed { idx, task ->
                                val shortLabel = when (idx) {
                                    0 -> "Materi"
                                    1 -> "Kuis"
                                    else -> "Lab / Game"
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (task.isDone) EmeraldDark.copy(alpha = 0.65f) else DeepObsidian.copy(alpha = 0.65f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (task.isDone) EmeraldSuccess.copy(alpha = 0.7f) else CardSlateBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = (if (task.isDone) "✓ " else "") + shortLabel,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (task.isDone) EmeraldSuccess else TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${task.current}/${task.target}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (task.isDone) EmeraldSuccess else CyanBright
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (dueReviews.isNotEmpty()) {
                item(key = "spaced_repetition_card") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = DarkNavySlate,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Ulasan Hari Ini",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                            dueReviews.forEach { candidate ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            state.modulesList.firstOrNull { it.id == candidate.moduleId }?.let {
                                                viewModel.openModuleDetail(it)
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${candidate.nodeCode} · ${candidate.title}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = candidate.reasonLabel,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Text(
                                        text = "Ulangi →",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanBright
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item(key = "curriculum_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Peta Belajar Kurikulum MRI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${chapterGroups.size} Bab • ${state.modulesList.size} Modul",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanBright
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            chapterGroups.forEachIndexed { chapterIdx, chapter ->
                val prevChapterLastMod = if (chapterIdx > 0) chapterGroups[chapterIdx - 1].modules.lastOrNull() else null
                val firstModInChapter = chapter.modules.firstOrNull()
                val chapterDoneCount = chapter.modules.sumOf { it.done }
                val chapterTotalCount = chapter.modules.sumOf { it.total }

                item(key = "home_chapter_${chapter.chapterKey}") {
                    SectionDivider(
                        title = chapter.chapterTitle,
                        subtitle = null,
                        doneCount = chapterDoneCount,
                        totalCount = chapterTotalCount,
                        topEntryX = prevChapterLastMod?.let { PathGeometry.xFractionFor(it.globalIndex) },
                        topEntryDone = prevChapterLastMod?.status == NodeStatus.DONE,
                        bottomExitX = firstModInChapter?.let { PathGeometry.xFractionFor(it.globalIndex) },
                        bottomExitDone = firstModInChapter?.status == NodeStatus.DONE
                    )
                }

                val chapterIllType = when (chapter.chapterKey) {
                    "M01" -> NanoIllustrationType.MRI_SCANNER
                    "M02" -> NanoIllustrationType.T1_T2_RELAX
                    "M03" -> NanoIllustrationType.SPIN_ECHO
                    "M04" -> NanoIllustrationType.K_SPACE
                    "M05" -> NanoIllustrationType.HYDROGEN_ATOM
                    "M06" -> NanoIllustrationType.GRADIENT_COIL
                    "M07" -> NanoIllustrationType.SAFETY_ZONES
                    "M08" -> NanoIllustrationType.ARTEFAK_ALIAS
                    else -> NanoIllustrationType.CLINICAL_CONSOLE
                }

                if (firstModInChapter != null) {
                    val connectorX = PathGeometry.xFractionFor(firstModInChapter.globalIndex)
                    val connectorDone = firstModInChapter.status == NodeStatus.DONE
                    item(key = "home_ill_${chapter.chapterKey}") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val strokePx = 6.dp.toPx()
                                val lineColor = if (connectorDone) CyanBright else Color(0xFF426794)
                                val x = size.width * connectorX.coerceIn(0f, 1f)
                                drawLine(
                                    color = lineColor.copy(alpha = 0.28f),
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = strokePx + 5.dp.toPx()
                                )
                                drawLine(
                                    color = lineColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = strokePx
                                )
                            }

                            InteractiveNanoIllustration(
                                type = chapterIllType,
                                characterCustomization = state.characterCustomization,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                chapter.modules.forEachIndexed { localIdx, modState ->
                    val module = modState.module
                    val xFraction = PathGeometry.xFractionFor(modState.globalIndex)
                    val accent = moduleAccentColor(module.code)
                    val prevModInChapter = if (localIdx > 0) chapter.modules[localIdx - 1] else null
                    val nextModInChapter = if (localIdx < chapter.modules.lastIndex) chapter.modules[localIdx + 1] else null
                    val hasNextChapter = chapterIdx < chapterGroups.lastIndex
                    val dimmed = HomePathPlanner.isDimmed(modState)

                    item(key = "home_mod_${module.id}") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Garis jalur tegas siku-siku menyambung langsung ke setiap blok kotak 3D modul & header bab
                            SeamlessNodePathBackdrop(
                                nodeX = xFraction,
                                prevX = prevModInChapter?.let { PathGeometry.xFractionFor(it.globalIndex) },
                                nextX = nextModInChapter?.let { PathGeometry.xFractionFor(it.globalIndex) },
                                incomingDone = if (prevModInChapter != null) {
                                    prevModInChapter.status == NodeStatus.DONE
                                } else {
                                    modState.status == NodeStatus.DONE
                                },
                                outgoingDone = modState.status == NodeStatus.DONE,
                                nodeCenterY = 60.dp,
                                accentColor = CyanBright,
                                strokeWidthDp = 6.dp,
                                connectFromHeader = prevModInChapter == null,
                                connectToFooter = nextModInChapter == null && hasNextChapter,
                                modifier = Modifier.matchParentSize()
                            )

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Kotak toggle 3D modul (M01-M09) bersih tanpa teks penjelasan di bawahnya
                                    PathAlignedSlot(xFraction = xFraction) {
                                        PathNodeButton(
                                            status = modState.status,
                                            kind = NodeKind.MODUL,
                                            label = module.code,
                                            onClick = { selectedModuleForSheet = module },
                                            accentColor = accent,
                                            progressPill = "${modState.done}/${modState.total}",
                                            dimmed = dimmed,
                                            semanticLabel = "${module.code} ${module.title}",
                                            modifier = Modifier
                                                .testTag("home_module_node_${module.id}")
                                                .then(
                                                    if (modState.globalIndex == 0) {
                                                        Modifier.onGloballyPositioned { coords ->
                                                            val raw = coords.boundsInRoot()
                                                            firstModuleBounds = raw.translate(0f, -homeRootTopInRoot)
                                                        }
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(if (nextModInChapter != null) 30.dp else 18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Nano Floating di atas peta belajar dengan bubble awan di atasnya agar tidak menutupi tab
    FloatingNanoCompanion(
        characterCustomization = state.characterCustomization,
        hints = listOf(
            "Halo! Aku Nano. Ketuk kotak M01–M09 untuk membuka materi MRI!",
            "Ketuk ilustrasi MRI di samping kotak untuk fakta fisika singkat!",
            "Selesaikan Shift Nano harianmu untuk bonus XP tambahan."
        ),
        openBubbleUpward = true,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 14.dp, bottom = 14.dp)
    )

    val sheetModule = selectedModuleForSheet
    if (sheetModule != null) {
        val overviewSummary = remember(
            sheetModule.id,
            state.modulesList,
            state.completedNodeCodes,
            state.quizzesList,
            triedLabNodes
        ) {
            ModuleOverview.compute(
                module = sheetModule,
                allModules = state.modulesList,
                completedCodes = state.completedNodeCodes,
                quizzes = state.quizzesList,
                triedLabNodes = triedLabNodes
            )
        }
        ModuleOverviewBottomSheet(
            summary = overviewSummary,
            onDismiss = { selectedModuleForSheet = null },
            onContinuePath = {
                selectedModuleForSheet = null
                viewModel.openModuleDetail(sheetModule)
            },
            onOpenAllNodes = {
                selectedModuleForSheet = null
                PathNavigationHints.requestOpenAllNodes()
                viewModel.openModuleDetail(sheetModule)
            },
            onOpenFlashcards = {
                selectedModuleForSheet = null
                PathNavigationHints.requestOpenFlashcards()
                viewModel.openModuleDetail(sheetModule)
            }
        )
    }

    val boundsForSpotlight = firstModuleBounds
    if (guideStep == MapGuideStep.SPOTLIGHT && boundsForSpotlight != null) {
        MapGuideCoachOverlay(
            targetBounds = boundsForSpotlight,
            onNext = {
                pathHolder.markMapGuideSeenAsync()
                guideStep = MapGuideStateMachine.nextStep(guideStep)
            },
            onSkip = {
                pathHolder.markMapGuideSeenAsync()
                guideStep = MapGuideStateMachine.skipToDone()
            }
        )
    }

    if (guideStep == MapGuideStep.LEGEND || showLegendFromInfo) {
        val isFromOnboarding = guideStep == MapGuideStep.LEGEND
        MapLegendBottomSheet(
            showStepBadge = isFromOnboarding,
            onDismiss = {
                if (isFromOnboarding) {
                    pathHolder.markMapGuideSeenAsync()
                    guideStep = MapGuideStateMachine.nextStep(guideStep)
                }
                showLegendFromInfo = false
            }
        )
    }
    }
}

@Composable
fun HeroProgressCard(
    completedCount: Int,
    totalCount: Int,
    totalXp: Int,
    completedModulesCount: Int,
    lastVisitedTitle: String,
    onContinueClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavySlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image MRI Scanner Hero
            Image(
                painter = painterResource(id = R.drawable.mri_scanner_hero),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC090D16),
                                Color(0xF2090D16)
                            )
                        )
                    )
            )

            // Content inside hero
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROGRES KURIKULUM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberWarning,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = "$totalXp XP · $completedModulesCount modul tuntas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$completedCount dari $totalCount materi",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Bar
                    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyanBright,
                        trackColor = CardSlate
                    )
                }

                // "Lanjutkan" Pill Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(onClick = onContinueClicked),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xF2F8FAFC)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Lanjutkan",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = lastVisitedTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D9488)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Lanjut",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToolChip(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = DarkNavySlate,
        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HomeModuleLabelColumn(
    title: String,
    subtitle: String,
    isCurrent: Boolean,
    dimmed: Boolean,
    alignEnd: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
            color = if (dimmed) TextMuted else TextPrimary,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = if (isCurrent) CyanBright else TextMuted,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SimInteractiveCard(
    title: String,
    subtitle: String = "",
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(138.dp)
            .height(114.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavySlate),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, iconColor.copy(alpha = 0.78f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DeepObsidian)
                    .border(1.dp, iconColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ModuleCard(
    module: MriModule,
    completedCount: Int,
    bookmarkedCount: Int = 0,
    onClick: () -> Unit
) {
    val totalNodes = module.nodes.size
    val isComplete = completedCount == totalNodes && totalNodes > 0
    val moduleProgress = if (totalNodes > 0) {
        (completedCount.toFloat() / totalNodes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val moduleNumber = module.code.drop(1).takeWhile { it.isDigit() }.toIntOrNull() ?: 9
    val moduleAccentColor = when {
        isComplete -> EmeraldSuccess
        moduleNumber in listOf(1, 5, 9) -> CyanBright
        moduleNumber in listOf(2, 6) -> Color(0xFF2DD4BF)
        moduleNumber in listOf(3, 7) -> AmberWarning
        else -> Color(0xFF38BDF8)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavySlate),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, moduleAccentColor.copy(alpha = 0.78f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val moduleIcon = when (module.code) {
                    "M01" -> Icons.Default.Science
                    "M02" -> Icons.Default.ShowChart
                    "M03A" -> Icons.Default.GraphicEq
                    "M03B" -> Icons.Default.Waves
                    "M03C" -> Icons.Default.WaterDrop
                    "M04A" -> Icons.Default.Layers
                    "M04B" -> Icons.Default.GridOn
                    "M04C" -> Icons.Default.Route
                    "M05" -> Icons.Default.PrecisionManufacturing
                    "M06A" -> Icons.Default.Tune
                    "M06B" -> Icons.Default.Sensors
                    "M07A" -> Icons.Default.Security
                    "M07B" -> Icons.Default.AcUnit
                    "M07C" -> Icons.Default.Bolt
                    "M08A" -> Icons.Default.AutoFixHigh
                    "M08B" -> Icons.Default.Air
                    "M08C" -> Icons.Default.FactCheck
                    else -> Icons.Default.Speed
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isComplete) EmeraldDark else CyanDark)
                            .border(1.dp, moduleAccentColor, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = moduleIcon,
                            contentDescription = module.code,
                            tint = if (isComplete) EmeraldSuccess else CyanBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = module.code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isComplete) EmeraldSuccess else CyanBright,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = module.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (bookmarkedCount > 0) {
                            Text(
                                text = "★ $bookmarkedCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bar progres dinamis yang menyesuaikan dengan progres modul
                    LinearProgressIndicator(
                        progress = { moduleProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isComplete) EmeraldSuccess else moduleAccentColor,
                        trackColor = CardSlate
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Lingkaran progres yang diperbesar tanpa lambang panah (>)
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { moduleProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (isComplete) EmeraldSuccess else moduleAccentColor,
                    trackColor = CardSlate,
                    strokeWidth = 4.5.dp
                )
                Text(
                    text = "$completedCount/$totalNodes",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isComplete) EmeraldSuccess else TextPrimary
                )
            }
        }
    }
}
