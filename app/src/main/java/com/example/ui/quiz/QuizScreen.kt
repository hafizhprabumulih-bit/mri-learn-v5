package com.example.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CharacterCatalog
import com.example.data.LegacyNodeCodes
import com.example.ui.path.PathNavigationHints
import com.example.util.QuizSessionResult
import com.example.util.QuizStatsCalculator
import com.example.ui.character.AvatarView
import com.example.ui.character.Expression
import com.example.ui.components.rememberRestorableScrollState
import com.example.ui.theme.AmberDark
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
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.RoseDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MriUiState
import com.example.viewmodel.MriViewModel

@Composable
fun QuizScreen(
    state: MriUiState,
    viewModel: MriViewModel,
    modifier: Modifier = Modifier
) {
    val quizzes = state.filteredQuizzes
    val allQuizzes = state.quizzesList
    val scopeKey = "${state.quizModuleFilter}_${state.quizLevelFilter}"
    QuizSessionSnapshotHolder.ensureScope(scopeKey)

    var retryQuizIds by remember(scopeKey) { mutableStateOf<List<String>?>(null) }
    var retryIndex by remember(scopeKey) { mutableIntStateOf(0) }
    var retrySelected by remember(scopeKey) { mutableStateOf<Int?>(null) }
    var retrySubmitted by remember(scopeKey) { mutableStateOf(false) }
    var retryFinished by remember(scopeKey) { mutableStateOf(false) }
    var retryAnswers by remember(scopeKey) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var retrySelectedMap by remember(scopeKey) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var wrongStreak by remember(scopeKey) { mutableIntStateOf(0) }
    var countedSessionQuizIds by remember(scopeKey) { mutableStateOf<Set<String>>(emptySet()) }

    val pathHolder = com.example.ui.path.rememberModulePathStateHolder()
    val todayKey = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }

    val isRetryMode = retryQuizIds != null
    val activeQuizzes = remember(quizzes, retryQuizIds) {
        val ids = retryQuizIds?.toSet()
        if (ids == null) quizzes else quizzes.filter { it.id in ids }
    }
    val activeIndex = if (isRetryMode) retryIndex else state.currentQuizIndex
    val activeSelected = if (isRetryMode) retrySelected else state.selectedOptionIndex
    val activeSubmitted = if (isRetryMode) retrySubmitted else state.isAnswerSubmitted
    val activeFinished = if (isRetryMode) retryFinished else state.isQuizFinished

    val mriHaptics = com.example.util.rememberMriHaptics()
    var hasInitializedDefaultModuleFilter by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasInitializedDefaultModuleFilter) {
            hasInitializedDefaultModuleFilter = true
            if (state.quizModuleFilter == "SEMUA") {
                viewModel.setQuizModuleFilter("M01")
                viewModel.setQuizLevelFilter(0)
            }
        }
    }

    var showStatsDialog by rememberSaveable { mutableStateOf(false) }
    var isFilterExpanded by rememberSaveable { mutableStateOf(false) }
    var showPreSubmitHint by rememberSaveable(activeIndex, state.quizModuleFilter, isRetryMode) {
        mutableStateOf(false)
    }
    var nanoInteractionMode by rememberSaveable(activeIndex, state.quizModuleFilter, isRetryMode) {
        mutableIntStateOf(0) // 0 = Default, 1 = Petunjuk, 2 = Fakta MRI, 3 = Fokus Kunci, 4 = Semangat
    }

    val quizStats = remember(state.quizzesList, state.quizAttempts, state.quizModuleFilter) {
        QuizStatsCalculator.compute(
            allQuizzes = state.quizzesList,
            attempts = state.quizAttempts,
            selectedModuleFilter = state.quizModuleFilter
        )
    }
    val totalAttempts = quizStats.totalAttempts
    val correctAttempts = quizStats.correctAttempts
    val stillWrongAttempts = quizStats.stillWrongAttempts
    val accuracyPct = quizStats.accuracyPct

    val currentQuiz = activeQuizzes.getOrNull(activeIndex)
    val quizScrollState = rememberRestorableScrollState("tab_kuis", viewModel)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidian)
    ) {
        // 1. HEADER RINGKAS & ERGONOMIS: Judul + Filter Paket + Tombol Statistik
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val headerTopic = when (state.quizModuleFilter) {
                        "SEMUA" -> "Kuis Evaluasi Mandiri"
                        "M01" -> "Kuis · Fisika Dasar MRI"
                        "M02" -> "Kuis · Relaksasi T1/T2"
                        "M03", "M03A", "M03B", "M03C" -> "Kuis · Sekuens & Kontras"
                        "M04", "M04A", "M04B", "M04C" -> "Kuis · K-space & Citra"
                        "M05" -> "Kuis · Perangkat Magnet"
                        "M06", "M06A", "M06B" -> "Kuis · Gradien & Koil RF"
                        "M07", "M07A", "M07B", "M07C" -> "Kuis · Keselamatan MR"
                        "M08", "M08A", "M08B", "M08C" -> "Kuis · Artefak & QA"
                        "M09" -> "Kuis · Konsol Klinis"
                        else -> "Kuis · ${state.quizModuleFilter}"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = headerTopic,
                            fontSize = 16.5.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!activeFinished && activeQuizzes.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyanDark,
                                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "${activeIndex + 1}/${activeQuizzes.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyanBright,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Tombol Statistik Ringkas
                Surface(
                    onClick = {
                        mriHaptics.lightToggle()
                        showStatsDialog = !showStatsDialog
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (showStatsDialog) CyanPrimary else DarkNavySlate,
                    border = BorderStroke(
                        1.dp,
                        if (showStatsDialog) CyanBright else CyanPrimary.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val barColor = if (showStatsDialog) DeepObsidian else CyanBright
                        Canvas(modifier = Modifier.size(14.dp)) {
                            val barW = size.width * 0.22f
                            val gap = size.width * 0.11f
                            val startX = (size.width - (3 * barW + 2 * gap)) / 2f
                            val bottomY = size.height * 0.88f
                            listOf(0.35f, 0.62f, 0.88f).forEachIndexed { idx, frac ->
                                val h = size.height * frac
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(startX + idx * (barW + gap), bottomY - h),
                                    size = Size(barW, h),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                            }
                        }
                        Text(
                            text = if (totalAttempts > 0) "$accuracyPct%" else "Stats",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showStatsDialog) DeepObsidian else CyanBright
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // 2. RINGKASAN FILTER RINGKAS + TOGGLE PANEL FILTER LENGKAP
            val levelTitle = when (state.quizLevelFilter) {
                1 -> "T1 Dasar"
                2 -> "T2 Menengah"
                3 -> "T3 Lanjut"
                else -> "Semua Tingkat"
            }
            val modTitle = if (state.quizModuleFilter == "SEMUA") "Semua Modul" else state.quizModuleFilter

            Surface(
                onClick = {
                    mriHaptics.lightToggle()
                    isFilterExpanded = !isFilterExpanded
                },
                modifier = Modifier.defaultMinSize(minHeight = 38.dp),
                shape = RoundedCornerShape(12.dp),
                color = DarkNavySlate,
                border = BorderStroke(
                    1.dp,
                    if (isFilterExpanded) CyanPrimary else CardSlateBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = CyanBright,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Paket: $modTitle · $levelTitle",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isFilterExpanded) "Tutup" else "Ubah Paket",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanBright
                        )
                        Icon(
                            imageVector = if (isFilterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = CyanBright,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isFilterExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Default dimulai dari M01, sedangkan "SEMUA" ditempatkan di bagian paling akhir
                        val moduleFilters = listOf(
                            "M01" to "M01: Fisika (${allQuizzes.count { it.moduleId == "M01" || it.nodeCode.startsWith("M01") }})",
                            "M02" to "M02: T1/T2 (${allQuizzes.count { it.moduleId == "M02" || it.nodeCode.startsWith("M02") }})",
                            "M03" to "M03: Sekuens (${allQuizzes.count { it.moduleId.startsWith("M03") || it.nodeCode.startsWith("M03") }})",
                            "M03A" to "M03A (${allQuizzes.count { it.moduleId == "M03A" || it.nodeCode.startsWith("M03A") }})",
                            "M03B" to "M03B (${allQuizzes.count { it.moduleId == "M03B" || it.nodeCode.startsWith("M03B") }})",
                            "M03C" to "M03C (${allQuizzes.count { it.moduleId == "M03C" || it.nodeCode.startsWith("M03C") }})",
                            "M04" to "M04: K-space (${allQuizzes.count { it.moduleId.startsWith("M04") || it.nodeCode.startsWith("M04") }})",
                            "M04A" to "M04A (${allQuizzes.count { it.moduleId == "M04A" || it.nodeCode.startsWith("M04A") }})",
                            "M04B" to "M04B (${allQuizzes.count { it.moduleId == "M04B" || it.nodeCode.startsWith("M04B") }})",
                            "M04C" to "M04C (${allQuizzes.count { it.moduleId == "M04C" || it.nodeCode.startsWith("M04C") }})",
                            "M05" to "M05: Magnet (${allQuizzes.count { it.moduleId == "M05" || it.nodeCode.startsWith("M05") }})",
                            "M06" to "M06: Gradien/RF (${allQuizzes.count { it.moduleId.startsWith("M06") || it.nodeCode.startsWith("M06") }})",
                            "M06A" to "M06A (${allQuizzes.count { it.moduleId == "M06A" || it.nodeCode.startsWith("M06A") }})",
                            "M06B" to "M06B (${allQuizzes.count { it.moduleId == "M06B" || it.nodeCode.startsWith("M06B") }})",
                            "M07" to "M07: Safety (${allQuizzes.count { it.moduleId.startsWith("M07") || it.nodeCode.startsWith("M07") }})",
                            "M07A" to "M07A (${allQuizzes.count { it.moduleId == "M07A" || it.nodeCode.startsWith("M07A") }})",
                            "M07B" to "M07B (${allQuizzes.count { it.moduleId == "M07B" || it.nodeCode.startsWith("M07B") }})",
                            "M07C" to "M07C (${allQuizzes.count { it.moduleId == "M07C" || it.nodeCode.startsWith("M07C") }})",
                            "M08" to "M08: Artefak/QA (${allQuizzes.count { it.moduleId.startsWith("M08") || it.nodeCode.startsWith("M08") }})",
                            "M08A" to "M08A (${allQuizzes.count { it.moduleId == "M08A" || it.nodeCode.startsWith("M08A") }})",
                            "M08B" to "M08B (${allQuizzes.count { it.moduleId == "M08B" || it.nodeCode.startsWith("M08B") }})",
                            "M08C" to "M08C (${allQuizzes.count { it.moduleId == "M08C" || it.nodeCode.startsWith("M08C") }})",
                            "M09" to "M09: Konsol (${allQuizzes.count { it.moduleId == "M09" || it.nodeCode.startsWith("M09") }})",
                            "SEMUA" to "Semua (${allQuizzes.size})"
                        )
                        moduleFilters.forEach { (modId, label) ->
                            val isSelected = state.quizModuleFilter == modId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    mriHaptics.lightToggle()
                                    viewModel.setQuizModuleFilter(modId)
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .testTag("quiz_filter_$modId"),
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    selectedLabelColor = DeepObsidian,
                                    containerColor = CardSlate,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) CyanBright else CardSlateBorder
                                )
                            )
                        }
                    }

                    val moduleScopedQuizzes = if (state.quizModuleFilter == "SEMUA") {
                        allQuizzes
                    } else {
                        val filter = state.quizModuleFilter
                        allQuizzes.filter { q ->
                            q.moduleId == filter ||
                                (filter.length <= 3 && q.moduleId.startsWith(filter)) ||
                                (filter.length <= 3 && q.nodeCode.startsWith(filter)) ||
                                q.nodeCode.startsWith(filter)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val levelFilters = listOf(
                            0 to "Semua Tingkat (${moduleScopedQuizzes.size})",
                            1 to "T1 · Dasar (${moduleScopedQuizzes.count { it.level == 1 }})",
                            2 to "T2 · Menengah (${moduleScopedQuizzes.count { it.level == 2 }})",
                            3 to "T3 · Lanjut (${moduleScopedQuizzes.count { it.level == 3 }})"
                        )
                        levelFilters.forEach { (lvl, label) ->
                            val isSelected = state.quizLevelFilter == lvl
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    mriHaptics.lightToggle()
                                    viewModel.setQuizLevelFilter(lvl)
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .testTag("quiz_level_filter_$lvl"),
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (lvl) {
                                        1 -> EmeraldSuccess
                                        2 -> CyanPrimary
                                        3 -> AmberWarning
                                        else -> CyanPrimary
                                    },
                                    selectedLabelColor = DeepObsidian,
                                    containerColor = CardSlate,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) CyanBright else CardSlateBorder
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. AREA SOAL & OPSI ABCD KOMPAK AGAR MUAT TANPA PERLU SWIPE
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(quizScrollState)
                .padding(horizontal = 14.dp)
        ) {
            if (quizzes.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkNavySlate),
                    border = BorderStroke(1.dp, CardSlateBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tidak ada soal pada kombinasi modul dan tingkat ini.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(onClick = { viewModel.setQuizLevelFilter(0) }) {
                            Text("Tampilkan Semua Tingkat", fontSize = 11.sp, color = CyanBright)
                        }
                    }
                }
            } else if (activeFinished) {
                BackHandler {
                    retryQuizIds = null
                    retryFinished = false
                    viewModel.navigateBack()
                }
                val sessionAnswersMap = if (isRetryMode) {
                    retryAnswers
                } else {
                    QuizSessionSnapshotHolder.sessionAnswers
                }
                val resultSummary = remember(sessionAnswersMap, activeQuizzes, state.modulesList) {
                    QuizSessionResult.compute(
                        sessionAnswers = sessionAnswersMap,
                        sessionQuizzes = activeQuizzes,
                        modules = state.modulesList
                    )
                }
                val activeModuleCode = state.quizModuleFilter.takeIf { it != "SEMUA" }
                QuizResultSection(
                    summary = resultSummary,
                    moduleCode = activeModuleCode,
                    quizLevel = state.quizLevelFilter,
                    onReadNodeAgain = { nodeCode ->
                        val norm = LegacyNodeCodes.normalize(nodeCode)
                        val targetModule = state.modulesList.find { mod ->
                            mod.nodes.any { LegacyNodeCodes.normalize(it.code) == norm }
                        }
                        if (targetModule != null) {
                            PathNavigationHints.requestDirectReader(norm)
                            viewModel.openModuleDetail(targetModule)
                        }
                    },
                    onRetryWrong = {
                        retryQuizIds = resultSummary.wrongQuizIds
                        retryIndex = 0
                        retrySelected = null
                        retrySubmitted = false
                        retryFinished = false
                        retryAnswers = emptyMap()
                        retrySelectedMap = emptyMap()
                        wrongStreak = 0
                    },
                    onPrimaryNext = {
                        if (activeModuleCode != null && state.quizLevelFilter in 1..2) {
                            retryQuizIds = null
                            retryFinished = false
                            retrySelectedMap = emptyMap()
                            wrongStreak = 0
                            countedSessionQuizIds = emptySet()
                            QuizSessionSnapshotHolder.clear()
                            viewModel.setQuizLevelFilter(state.quizLevelFilter + 1)
                        } else {
                            retryQuizIds = null
                            retryFinished = false
                            retrySelectedMap = emptyMap()
                            wrongStreak = 0
                            countedSessionQuizIds = emptySet()
                            viewModel.navigateBack()
                        }
                    }
                )
            } else if (currentQuiz != null) {
                val specialistProfile = CharacterCatalog.getSpecialistProfile(state.characterCustomization.avatarId)
                val nanoTransition = rememberInfiniteTransition(label = "quiz_nano_motion")
                val nanoBob by nanoTransition.animateFloat(
                    initialValue = -2.5f,
                    targetValue = 2.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1300, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "nanoBob"
                )
                val nanoTilt by nanoTransition.animateFloat(
                    initialValue = -3.5f,
                    targetValue = 3.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "nanoTilt"
                )
                var nanoTapBounceCount by remember(currentQuiz.id) { mutableIntStateOf(0) }
                val nanoTapScale by animateFloatAsState(
                    targetValue = if (nanoTapBounceCount % 2 == 1) 1.12f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "nanoTapScale"
                )

                val isAnswerCorrect = activeSelected == currentQuiz.correctIndex
                val autoHintActive = wrongStreak >= 2
                val quizExpression = when {
                    activeSubmitted && isAnswerCorrect && activeIndex % 2 == 0 -> Expression.BANGGA
                    activeSubmitted && isAnswerCorrect -> Expression.SENANG
                    activeSubmitted && !isAnswerCorrect -> Expression.SEDIH
                    nanoInteractionMode == 1 -> Expression.BERPIKIR
                    nanoInteractionMode == 2 -> Expression.SENANG
                    nanoInteractionMode == 3 -> Expression.WASPADA
                    nanoInteractionMode == 4 -> Expression.BANGGA
                    autoHintActive -> Expression.WASPADA
                    showPreSubmitHint -> Expression.SENANG
                    else -> Expression.BERPIKIR
                }

                val cleanExplanation = currentQuiz.explanation
                    .replace("Ruang-k", "K-space")
                    .replace("ruang-k", "K-space")
                val bubbleMessage = when {
                    activeSubmitted && isAnswerCorrect ->
                        "✓ Jawaban Tepat! $cleanExplanation"
                    activeSubmitted && !isAnswerCorrect ->
                        "✗ Kurang Tepat. $cleanExplanation"
                    nanoInteractionMode == 1 ->
                        "💡 Petunjuk Nano: Perhatikan hubungan konsep di materi ${currentQuiz.nodeCode} (${currentQuiz.levelBadge}). Eliminasi opsi yang bertentangan dengan hukum fisika dasar!"
                    nanoInteractionMode == 2 ->
                        "🔬 Fakta MRI: Pada modul ${currentQuiz.moduleId}, perubahan parameter sekuens berdampak langsung pada SNR, kontras jaringan, dan keselamatan pasien."
                    nanoInteractionMode == 3 ->
                        "🎯 Fokus Kunci: Cermati kata kunci utama pada soal ini (${currentQuiz.nodeCode}) sebelum mengunci pilihan A, B, C, atau D!"
                    nanoInteractionMode == 4 ->
                        "⚡ Semangat! ${specialistProfile.greetings.getOrElse((activeIndex + nanoTapBounceCount) % specialistProfile.greetings.size) { specialistProfile.greetings.first() }}"
                    autoHintActive ->
                        "Tenang, fokus pada konsep ${currentQuiz.levelBadge} (${currentQuiz.nodeCode})!"
                    showPreSubmitHint ->
                        specialistProfile.greetings.getOrElse(
                            (activeIndex + nanoTapBounceCount) % specialistProfile.greetings.size
                        ) { specialistProfile.greetings.first() }
                    currentQuiz.moduleId.startsWith("M07", ignoreCase = true) ->
                        "Hati-hati, pikirkan keselamatan ${currentQuiz.levelBadge} (${currentQuiz.nodeCode})!"
                    else ->
                        "Ketuk Nano di samping untuk petunjuk soal ${currentQuiz.nodeCode}!"
                }

                // AREA NANO INTERAKTIF TRANSPARAN: Maskot berdiri bebas tanpa kartu kaku
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Jendela Nano berdiri langsung di atas latar belakang dengan bayangan lantai halus
                        Box(
                            modifier = Modifier
                                .size(width = 96.dp, height = 96.dp)
                                .clickable {
                                    mriHaptics.lightToggle()
                                    nanoInteractionMode = (nanoInteractionMode + 1) % 5
                                    showPreSubmitHint = !showPreSubmitHint
                                    nanoTapBounceCount++
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp)
                                    .size(width = 74.dp, height = 16.dp)
                            ) {
                                drawOval(
                                    color = CyanBright.copy(alpha = 0.28f),
                                    topLeft = Offset(0f, size.height * 0.15f),
                                    size = Size(size.width, size.height * 0.75f)
                                )
                            }
                            AvatarView(
                                role = state.characterCustomization.avatarId,
                                equipped = state.characterCustomization,
                                expression = quizExpression,
                                modifier = Modifier
                                    .size(88.dp)
                                    .graphicsLayer {
                                        translationY = nanoBob * 1.6f
                                        rotationZ = nanoTilt
                                        scaleX = nanoTapScale
                                        scaleY = nanoTapScale
                                    }
                            )
                        }

                        // Balon Dialog Nano di samping kanan dengan keterbacaan lebih lega
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 84.dp)
                                .clickable {
                                    mriHaptics.lightToggle()
                                    nanoInteractionMode = (nanoInteractionMode + 1) % 5
                                    showPreSubmitHint = !showPreSubmitHint
                                    nanoTapBounceCount++
                                },
                            shape = RoundedCornerShape(
                                topStart = 8.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            ),
                            color = when {
                                activeSubmitted && isAnswerCorrect -> androidx.compose.ui.graphics.Color(0xFFDFF7EE)
                                activeSubmitted && !isAnswerCorrect -> androidx.compose.ui.graphics.Color(0xFFFCE4E8)
                                else -> androidx.compose.ui.graphics.Color(0xFFE2EFF8)
                            },
                            border = BorderStroke(
                                1.2.dp,
                                when {
                                    activeSubmitted && isAnswerCorrect -> EmeraldSuccess
                                    activeSubmitted && !isAnswerCorrect -> RoseDanger
                                    else -> CyanBright.copy(alpha = 0.65f)
                                }
                            )
                        ) {
                            Text(
                                text = bubbleMessage,
                                fontSize = 12.6.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = androidx.compose.ui.graphics.Color(0xFF0B192C),
                                lineHeight = 17.5.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // KARTU BAWAH: Pertanyaan & Tombol 3D Opsi Jawaban (A, B, C, D) Muat Tanpa Perlu Swipe
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkNavySlate),
                    border = BorderStroke(1.dp, CardSlateBorder)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                        Text(
                            text = currentQuiz.question
                                .replace("Ruang-k", "K-space")
                                .replace("ruang-k", "K-space"),
                            fontSize = 14.2.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            lineHeight = 19.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        currentQuiz.options.forEachIndexed { index, rawOption ->
                            val option = rawOption
                                .replace("Ruang-k", "K-space")
                                .replace("ruang-k", "K-space")
                            val isSelected = activeSelected == index
                            val isSubmitted = activeSubmitted
                            val isCorrectOption = index == currentQuiz.correctIndex

                            val containerColor = when {
                                isSubmitted && isCorrectOption -> EmeraldDark
                                isSubmitted && isSelected && !isCorrectOption -> RoseDark
                                isSelected -> CyanDark
                                else -> androidx.compose.ui.graphics.Color(0xFF102640)
                            }

                            val shadowLipColor = when {
                                isSubmitted && isCorrectOption -> EmeraldSuccess.copy(alpha = 0.70f)
                                isSubmitted && isSelected && !isCorrectOption -> RoseDanger.copy(alpha = 0.70f)
                                isSelected -> CyanBright.copy(alpha = 0.80f)
                                else -> androidx.compose.ui.graphics.Color(0xFF081526)
                            }

                            val borderColor = when {
                                isSubmitted && isCorrectOption -> EmeraldSuccess
                                isSubmitted && isSelected && !isCorrectOption -> RoseDanger
                                isSelected -> CyanBright
                                else -> androidx.compose.ui.graphics.Color(0xFF2B5480)
                            }

                            val textColor = when {
                                isSubmitted && (isCorrectOption || isSelected) -> TextPrimary
                                isSelected -> CyanBright
                                else -> TextPrimary
                            }

                            val optionLetter = ('A' + index)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                // Dudukan 3D bawah opsi
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(top = 3.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(shadowLipColor)
                                )
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = if (isSelected) 1.dp else 3.dp)
                                        .clickable(enabled = !isSubmitted) {
                                            mriHaptics.lightToggle()
                                            if (isRetryMode) {
                                                retrySelected = index
                                                retrySelectedMap = retrySelectedMap + (currentQuiz.id to index)
                                            } else {
                                                viewModel.selectQuizOption(index)
                                            }
                                        }
                                        .testTag("quiz_option_$index"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = containerColor,
                                    border = BorderStroke(if (isSelected) 1.8.dp else 1.2.dp, borderColor)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Keycap 3D huruf A/B/C/D
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) CyanBright else DeepObsidian,
                                            border = BorderStroke(1.dp, borderColor)
                                        ) {
                                            Text(
                                                text = "$optionLetter",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) DeepObsidian else CyanBright,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        Text(
                                            text = option,
                                            fontSize = 13.2.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor,
                                            lineHeight = 17.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (isSubmitted) {
                                            if (isCorrectOption) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Benar",
                                                    tint = EmeraldSuccess,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            } else if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Salah",
                                                    tint = RoseDanger,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // 4. DOCK TOMBOL AKSI BAWAH PINNED: Selalu mudah dijangkau ibu jari!
        if (!activeFinished && currentQuiz != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkNavySlate,
                border = BorderStroke(1.dp, CardSlateBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            mriHaptics.lightToggle()
                            if (isRetryMode) {
                                if (retryIndex > 0) {
                                    val prevIdx = retryIndex - 1
                                    val prevQuizId = activeQuizzes.getOrNull(prevIdx)?.id
                                    retryIndex = prevIdx
                                    retrySelected = prevQuizId?.let { retrySelectedMap[it] }
                                    retrySubmitted = prevQuizId != null && retryAnswers.containsKey(prevQuizId)
                                }
                            } else {
                                viewModel.prevQuizQuestion()
                            }
                        },
                        enabled = activeIndex > 0,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardSlateBorder),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Soal Sebelumnya",
                            tint = if (activeIndex > 0) TextPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!activeSubmitted) {
                        Button(
                            onClick = {
                                if (isRetryMode) {
                                    val pickedIdx = retrySelected
                                    val isCorrect = pickedIdx == currentQuiz.correctIndex
                                    if (isCorrect) mriHaptics.mediumAction() else mriHaptics.warningBuzz()
                                    if (pickedIdx != null) {
                                        retrySelectedMap = retrySelectedMap + (currentQuiz.id to pickedIdx)
                                    }
                                    retryAnswers = retryAnswers + (currentQuiz.id to isCorrect)
                                    retrySubmitted = true
                                    pathHolder.recordDailyShiftAsync(todayKey, quizzesDelta = 1)
                                    wrongStreak = if (isCorrect) 0 else wrongStreak + 1
                                } else {
                                    val isCorrect = state.selectedOptionIndex == currentQuiz.correctIndex
                                    if (isCorrect) mriHaptics.mediumAction() else mriHaptics.warningBuzz()
                                    QuizSessionSnapshotHolder.record(currentQuiz.id, isCorrect)
                                    if (currentQuiz.id !in countedSessionQuizIds) {
                                        countedSessionQuizIds = countedSessionQuizIds + currentQuiz.id
                                        pathHolder.recordDailyShiftAsync(todayKey, quizzesDelta = 1)
                                        wrongStreak = if (isCorrect) 0 else wrongStreak + 1
                                    }
                                    viewModel.submitQuizAnswer()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("submit_quiz_button"),
                            enabled = activeSelected != null,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("Periksa Jawaban", color = DeepObsidian, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                mriHaptics.lightToggle()
                                if (isRetryMode) {
                                    if (retryIndex < activeQuizzes.lastIndex) {
                                        val nextIdx = retryIndex + 1
                                        val nextQuizId = activeQuizzes.getOrNull(nextIdx)?.id
                                        retryIndex = nextIdx
                                        retrySelected = nextQuizId?.let { retrySelectedMap[it] }
                                        retrySubmitted = nextQuizId != null && retryAnswers.containsKey(nextQuizId)
                                    } else {
                                        retryFinished = true
                                    }
                                } else {
                                    viewModel.nextQuizQuestion()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("next_quiz_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("Soal Berikutnya", color = DeepObsidian, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = DeepObsidian,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            retryQuizIds = null
                            retryFinished = false
                            retrySelectedMap = emptyMap()
                            wrongStreak = 0
                            countedSessionQuizIds = emptySet()
                            QuizSessionSnapshotHolder.clear()
                            viewModel.restartQuiz()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardSlateBorder),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Ulangi Kuis",
                            tint = CyanBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showStatsDialog) {
        Dialog(onDismissRequest = { showStatsDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavySlate),
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Statistik & Riwayat Kuis",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanBright
                        )
                        IconButton(
                            onClick = { showStatsDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup statistik",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (totalAttempts == 0) {
                        Text(
                            text = "Belum ada riwayat jawaban untuk filter modul ini. Jawab minimal 1 soal untuk melihat statistik.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    } else {
                        Text(
                            text = "Total Percobaan: $totalAttempts jawaban | Benar: $correctAttempts ($accuracyPct%)",
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (stillWrongAttempts.isNotEmpty()) {
                            Text(
                                text = "Soal yang masih salah (${stillWrongAttempts.size}):",
                                fontSize = 11.sp,
                                color = RoseDanger,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            stillWrongAttempts.take(5).forEach { att ->
                                val q = allQuizzes.find { it.id == att.quizId }
                                if (q != null) {
                                    Text(
                                        text = "• [${q.moduleId}] ${q.question}",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                            }
                        } else {
                            Text(
                                text = "Semua soal yang pernah dicoba pada cakupan ini sudah terjawab benar!",
                                fontSize = 11.sp,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizFinishedView(
    score: Int,
    total: Int,
    onRestart: () -> Unit
) {
    val haptics = com.example.util.rememberMriHaptics()
    LaunchedEffect(score, total) {
        haptics.completionBuzz()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavySlate),
        border = BorderStroke(1.dp, CardSlateBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CyanDark)
                    .border(2.dp, CyanPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = CyanBright,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Evaluasi Selesai!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Skor Anda: $score dari $total Benar",
                fontSize = 14.sp,
                color = CyanBright,
                fontWeight = FontWeight.SemiBold
            )

            val percentage = if (total > 0) (score.toFloat() / total.toFloat()) * 100f else 0f
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DeepObsidian,
                border = BorderStroke(1.dp, CardSlateBorder)
            ) {
                Text(
                    text = "Akurasi: ${percentage.toInt()}%",
                    fontSize = 13.sp,
                    color = if (percentage >= 75f) EmeraldSuccess else AmberWarning,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = DeepObsidian,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ulangi Latihan Kuis",
                    color = DeepObsidian,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
