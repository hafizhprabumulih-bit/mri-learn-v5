package com.example.ui.lesson

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.data.CharacterCustomizationState
import kotlinx.coroutines.flow.map
import com.example.data.GlossaryItem
import com.example.data.MriModule
import com.example.data.MriNode
import com.example.data.MriQuizQuestion
import com.example.data.MriRepository
import com.example.ui.character.AvatarView
import com.example.ui.character.Expression
import com.example.ui.components.MriGlobalAssetImage
import com.example.ui.components.MriGlobalAssetKey
import com.example.ui.path.ModulePathStateHolder
import com.example.ui.path.PathNavigationHints
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.data.LegacyNodeCodes
import com.example.ui.path.rememberReduceMotion
import com.example.util.NodeDisplay
import com.example.util.lesson.LessonChunk
import com.example.util.lesson.LessonEvent
import com.example.util.lesson.LessonPlanBuilder
import com.example.util.lesson.LessonReducer
import com.example.util.lesson.LessonState
import com.example.util.lesson.LessonStep
import com.example.util.lesson.QuestionPhase
import com.example.util.rememberMriHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    module: MriModule,
    node: MriNode,
    allQuizzes: List<MriQuizQuestion>,
    characterCustomization: CharacterCustomizationState,
    pathHolder: ModulePathStateHolder,
    isReplayMode: Boolean,
    onExitLesson: () -> Unit,
    onSwitchToQuickRead: () -> Unit,
    onLessonFinished: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberMriHaptics()
    val reduceMotion = rememberReduceMotion()
    val normalizedNodeCode = remember(node.code) { LegacyNodeCodes.normalize(node.code) }

    val rawAllowDraftsState by remember(pathHolder) {
        pathHolder.allowLessonDraftsFlow.map<Boolean, Boolean?> { it }
    }.collectAsState(initial = null)
    val allowDraftsLoaded = !BuildConfig.DEBUG || rawAllowDraftsState != null
    val allowDrafts = BuildConfig.DEBUG && (rawAllowDraftsState == true)
    val savedStepIndex by pathHolder.observeSavedLessonStep(normalizedNodeCode).collectAsState(initial = -1)
    val savedEncodedAnswers by pathHolder.observeSavedLessonAnswers(normalizedNodeCode).collectAsState(initial = "__LOADING__")
    val tapHintCount by pathHolder.lessonTapHintCountFlow.collectAsState(initial = 0)

    val nodeExtras = remember(normalizedNodeCode, pathHolder.lessonExtras) {
        pathHolder.lessonExtras.forNode(normalizedNodeCode)
    }

    val plan = remember(node, allQuizzes, pathHolder.lessonExtras, allowDrafts) {
        LessonPlanBuilder.build(
            node = node,
            quizzes = allQuizzes,
            extras = pathHolder.lessonExtras,
            allowDrafts = allowDrafts
        )
    }

    val correctIndexByItem = remember(normalizedNodeCode, nodeExtras, allQuizzes) {
        buildMap {
            nodeExtras?.stories.orEmpty().forEach { story ->
                put(story.id, story.correctIndex)
            }
            allQuizzes
                .filter { LegacyNodeCodes.normalize(it.nodeCode) == normalizedNodeCode }
                .forEach { quiz ->
                    put(quiz.id, quiz.correctIndex)
                }
        }
    }

    var lessonState by remember(plan) {
        mutableStateOf(LessonState(plan = plan))
    }

    var hasRestoredStep by remember(plan, normalizedNodeCode, isReplayMode) {
        mutableStateOf(false)
    }

    LaunchedEffect(
        savedStepIndex,
        savedEncodedAnswers,
        allowDraftsLoaded,
        plan,
        hasRestoredStep,
        isReplayMode
    ) {
        val canFinishLoading = allowDraftsLoaded &&
            (isReplayMode || (savedStepIndex >= 0 && savedEncodedAnswers != "__LOADING__"))
        if (!hasRestoredStep && canFinishLoading) {
            hasRestoredStep = true
            if (!isReplayMode && (savedStepIndex > 0 || savedEncodedAnswers.isNotEmpty())) {
                lessonState = LessonReducer.restoreProgress(
                    state = lessonState,
                    savedIndex = savedStepIndex,
                    encodedAttempts = savedEncodedAnswers,
                    correctIndexByItem = correctIndexByItem
                )
            }
        }
    }

    var completionDispatched by remember(plan) { mutableStateOf(false) }

    LaunchedEffect(lessonState.finished) {
        if (lessonState.finished && !completionDispatched) {
            completionDispatched = true
            pathHolder.clearLessonStepAsync(normalizedNodeCode)
            haptics.completionBuzz()
            onLessonFinished()
        }
    }

    // Simpan posisi langkah dan progres jawaban setiap kali berpindah langkah atau menjawab
    LaunchedEffect(
        lessonState.index,
        lessonState.attemptsByItem,
        lessonState.finished,
        hasRestoredStep,
        isReplayMode
    ) {
        if (!lessonState.finished && hasRestoredStep && !isReplayMode) {
            pathHolder.saveLessonProgressAsync(
                nodeCode = normalizedNodeCode,
                stepIndex = lessonState.index,
                encodedAnswers = LessonReducer.encodeAttempts(lessonState.attemptsByItem)
            )
        }
    }

    // Sembunyikan bottom navigation bar selama layar pelajaran aktif
    DisposableEffect(Unit) {
        PathNavigationHints.isFocusReaderActive = true
        onDispose {
            PathNavigationHints.isFocusReaderActive = false
        }
    }

    var showExitConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var activeGlossaryItem by remember { mutableStateOf<GlossaryItem?>(null) }
    var interactiveNanoExpression by remember { mutableStateOf(Expression.SENANG) }
    var interactiveNanoBubble by remember { mutableStateOf<String?>(null) }
    var lessonNanoTapCount by remember(lessonState.index, normalizedNodeCode) { mutableStateOf(0) }

    val currentStep = lessonState.currentStep

    BackHandler {
        val steppedBack = LessonReducer.reduce(lessonState, LessonEvent.Back)
        if (steppedBack.index != lessonState.index) {
            lessonState = steppedBack
        } else {
            showExitConfirmDialog = true
        }
    }

    val relevantGlossary = remember(normalizedNodeCode, module.id) {
        val nodeSpecific = MriRepository.glossary.filter { item ->
            LegacyNodeCodes.normalize(item.nodeCode) == normalizedNodeCode ||
                item.moduleId.equals(module.id, ignoreCase = true) ||
                item.moduleId.equals(module.code, ignoreCase = true)
        }
        val others = MriRepository.glossary.filterNot { item ->
            LegacyNodeCodes.normalize(item.nodeCode) == normalizedNodeCode ||
                item.moduleId.equals(module.id, ignoreCase = true) ||
                item.moduleId.equals(module.code, ignoreCase = true)
        }
        nodeSpecific + others
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(lessonState.index, currentStep) {
        scrollState.scrollTo(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            // 1. HEADER BAR: Tombol X + Continuous Progress Bar + Tombol Lapor Masalah
            LessonTopBar(
                currentIndex = lessonState.index,
                segmentCount = plan.segmentCount,
                onCloseClick = {
                    haptics.lightToggle()
                    showExitConfirmDialog = true
                },
                onReportClick = {
                    haptics.lightToggle()
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            "Laporan Materi MRI EDU — $normalizedNodeCode"
                        )
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Simpul: $normalizedNodeCode (${NodeDisplay.displayTitle(node)})\nLangkah: ${lessonState.index + 1}/${plan.segmentCount}\nCatatan perbaikan:"
                        )
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(shareIntent, "Laporkan Masalah Materi"))
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. BARIS PILL KODE SIMPUL & SUB-STATUS LANGKAH
            LessonStepBadgeRow(
                node = node,
                currentStep = currentStep
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. KONTEN UTAMA SESUAI JENIS LANGKAH
            when (currentStep) {
                is LessonStep.Opening,
                is LessonStep.Explain,
                is LessonStep.Analogy -> {
                    val tapInteraction = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = tapInteraction,
                                indication = null
                            ) {
                                haptics.lightToggle()
                                pathHolder.incrementLessonTapHintCountAsync()
                                lessonState = LessonReducer.reduce(lessonState, LessonEvent.Next)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProgressiveExplainStack(
                                steps = plan.steps,
                                activeStepIndex = lessonState.index,
                                glossaryItems = relevantGlossary,
                                reduceMotion = reduceMotion,
                                onGlossaryTermClick = { item ->
                                    haptics.lightToggle()
                                    activeGlossaryItem = item
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Area Bawah: Maskot Nano Interaktif + Balon Ucapan + Tombol Lanjut 3D
                        val currentNanoHint = remember(
                            lessonState.index,
                            lessonNanoTapCount,
                            currentStep,
                            relevantGlossary
                        ) {
                            val baseHint = when (currentStep) {
                                is LessonStep.Opening -> "Mari pahami inti simpul ${NodeDisplay.shortCode(node.code)} langkah demi langkah."
                                is LessonStep.Explain -> currentStep.nanoHint
                                    ?: "Baca perlahan konsep inti pada kartu ini sebelum berpindah ke jendela berikutnya."
                                is LessonStep.Analogy -> "Analogi klinis ini memudahkan kamu mengingat konsep fisika di atas."
                                else -> null
                            }
                            if (lessonNanoTapCount == 0) {
                                baseHint
                            } else {
                                val extraTips = buildList {
                                    relevantGlossary.firstOrNull()?.let { g ->
                                        add("Istilah penting: ${g.term} — ${g.definition.take(78)}…")
                                    }
                                    if (node.analogi.isNotBlank()) {
                                        add(node.analogi.take(88))
                                    }
                                    add("Ketuk lagi Nano kapan saja atau tekan Lanjut bila sudah paham!")
                                }
                                extraTips[(lessonNanoTapCount - 1) % extraTips.size]
                            }
                        }

                        LessonBottomNanoBar(
                            characterCustomization = characterCustomization,
                            expression = when {
                                lessonNanoTapCount % 3 == 1 -> Expression.BERPIKIR
                                lessonNanoTapCount % 3 == 2 -> Expression.BANGGA
                                currentStep is LessonStep.Analogy -> Expression.BANGGA
                                currentStep is LessonStep.Explain -> Expression.SENANG
                                else -> Expression.NETRAL
                            },
                            nanoTapCount = lessonNanoTapCount,
                            nanoBubbleText = currentNanoHint,
                            primaryButtonLabel = "Lanjut",
                            showTapAnywhereHint = tapHintCount < 4,
                            onNanoClick = {
                                haptics.lightToggle()
                                lessonNanoTapCount++
                            },
                            onPrimaryClick = {
                                haptics.lightToggle()
                                pathHolder.incrementLessonTapHintCountAsync()
                                lessonState = LessonReducer.reduce(lessonState, LessonEvent.Next)
                            }
                        )
                    }
                }

                is LessonStep.Interactive -> {
                    val interactiveContent = nodeExtras?.interactive
                    if (interactiveContent == null) {
                        LaunchedEffect(currentStep) {
                            lessonState = LessonReducer.reduce(lessonState, LessonEvent.Next)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState)
                            ) {
                                RevealChainCard(
                                    interactive = interactiveContent,
                                    reduceMotion = reduceMotion,
                                    stateKey = "$normalizedNodeCode:${interactiveContent.title}",
                                    onNanoExpressionChange = { exp, msg ->
                                        haptics.lightToggle()
                                        interactiveNanoExpression = exp
                                        interactiveNanoBubble = msg
                                    },
                                    onContinueLesson = {
                                        haptics.mediumAction()
                                        lessonState = LessonReducer.reduce(lessonState, LessonEvent.Next)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AvatarView(
                                    role = characterCustomization.avatarId,
                                    equipped = characterCustomization,
                                    expression = interactiveNanoExpression,
                                    modifier = Modifier.size(48.dp)
                                )
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = DarkNavySlate,
                                    border = BorderStroke(1.dp, CardSlateBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = interactiveNanoBubble
                                            ?: "Tebak langkah berikutnya, lalu ketuk tombol untuk melihat urutannya.",
                                        fontSize = 12.5.sp,
                                        color = TextPrimary,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                is LessonStep.Story -> {
                    val story = nodeExtras?.stories?.firstOrNull { it.id == currentStep.storyId }
                    if (story == null) {
                        LaunchedEffect(currentStep) {
                            lessonState = LessonReducer.reduce(
                                lessonState.copy(
                                    questionPhaseByItem = lessonState.questionPhaseByItem +
                                        (currentStep.storyId to QuestionPhase.RESOLVED_CORRECT)
                                ),
                                LessonEvent.Next
                            )
                        }
                    } else {
                        LessonQuestionView(
                            itemId = story.id,
                            scenarioText = story.scenario,
                            questionText = story.question,
                            options = story.options,
                            correctIndex = story.correctIndex,
                            explanation = story.explanation,
                            followUps = story.followUps,
                            lessonState = lessonState,
                            characterCustomization = characterCustomization,
                            onSelectOption = { selectedIdx ->
                                val isRight = selectedIdx == story.correctIndex
                                if (isRight) haptics.mediumAction() else haptics.warningBuzz()
                                pathHolder.recordLessonQuizAttemptAsync(
                                    quizId = story.id,
                                    nodeCode = normalizedNodeCode,
                                    moduleId = module.id,
                                    selectedIndex = selectedIdx,
                                    isCorrect = isRight
                                )
                                lessonState = LessonReducer.reduce(
                                    lessonState,
                                    LessonEvent.Answer(
                                        itemId = story.id,
                                        optionIndex = selectedIdx,
                                        correctIndex = story.correctIndex
                                    )
                                )
                            },
                            onRetry = {
                                haptics.lightToggle()
                                lessonState = LessonReducer.reduce(
                                    lessonState,
                                    LessonEvent.Retry(story.id)
                                )
                            },
                            onNext = {
                                haptics.lightToggle()
                                lessonState = LessonReducer.reduce(lessonState, LessonEvent.Next)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is LessonStep.Check -> {
                    val quiz = allQuizzes.firstOrNull { it.id == currentStep.quizId }
                    if (quiz == null) {
                        LaunchedEffect(currentStep) {
                            lessonState = LessonReducer.reduce(
                                lessonState.copy(
                                    questionPhaseByItem = lessonState.questionPhaseByItem +
                                        (currentStep.quizId to QuestionPhase.RESOLVED_CORRECT)
                                ),
                                LessonEvent.Next
                            )
                        }
                    } else {
                        LessonQuestionView(
                            itemId = quiz.id,
                            scenarioText = null,
                            questionText = quiz.question,
                            options = quiz.options,
                            correctIndex = quiz.correctIndex,
                            explanation = quiz.explanation,
                            followUps = emptyList(),
                            lessonState = lessonState,
                            characterCustomization = characterCustomization,
                            onSelectOption = { selectedIdx ->
                                val isRight = selectedIdx == quiz.correctIndex
                                if (isRight) haptics.mediumAction() else haptics.warningBuzz()
                                pathHolder.recordLessonQuizAttemptAsync(
                                    quizId = quiz.id,
                                    nodeCode = normalizedNodeCode,
                                    moduleId = module.id,
                                    selectedIndex = selectedIdx,
                                    isCorrect = isRight
                                )
                                lessonState = LessonReducer.reduce(
                                    lessonState,
                                    LessonEvent.Answer(
                                        itemId = quiz.id,
                                        optionIndex = selectedIdx,
                                        correctIndex = quiz.correctIndex
                                    )
                                )
                            },
                            onRetry = {
                                haptics.lightToggle()
                                lessonState = LessonReducer.reduce(
                                    lessonState,
                                    LessonEvent.Retry(quiz.id)
                                )
                            },
                            onNext = {
                                haptics.lightToggle()
                                lessonState = LessonReducer.reduce(lessonState, LessonEvent.Next)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is LessonStep.Complete -> {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Bottom sheet glosarium saat istilah baru diketuk
    activeGlossaryItem?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { activeGlossaryItem = null },
            containerColor = DarkNavySlate,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyanPrimary.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = item.symbolOrAbbr.ifBlank { "ISTILAH MRI" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanBright,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = item.term,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = item.definition,
                    fontSize = 14.5.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { activeGlossaryItem = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = DeepObsidian
                    )
                ) {
                    Text("Mengerti", fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Dialog konfirmasi keluar dengan tata letak tombol proporsional seimbang
    if (showExitConfirmDialog) {
        Dialog(onDismissRequest = { showExitConfirmDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = DarkNavySlate,
                border = BorderStroke(1.2.dp, CardSlateBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Keluar? Kemajuan disimpan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Posisi langkahmu di simpul ${NodeDisplay.shortCode(node.code)} sudah disimpan. Saat kembali nanti, kamu bisa langsung melanjutkan dari langkah ini.",
                        color = TextSecondary,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Button(
                        onClick = {
                            showExitConfirmDialog = false
                            onExitLesson()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("lesson_exit_confirm_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = DeepObsidian
                        )
                    ) {
                        Text("Simpan & Keluar", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showExitConfirmDialog = false
                                pathHolder.setPreferredReaderModeAsync("QUICK_READ")
                                onSwitchToQuickRead()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("lesson_exit_quickread_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = "Baca Cepat",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanBright,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        OutlinedButton(
                            onClick = { showExitConfirmDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("lesson_exit_resume_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CardSlateBorder)
                        ) {
                            Text(
                                text = "Lanjut Belajar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonTopBar(
    currentIndex: Int,
    segmentCount: Int,
    onCloseClick: () -> Unit,
    onReportClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("lesson_close_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Keluar dari pelajaran",
                tint = TextSecondary
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val total = segmentCount.coerceAtLeast(1)
            val targetProgress = ((currentIndex + 1).toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(
                targetValue = targetProgress,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                label = "lesson_continuous_progress"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(CardSlateBorder.copy(alpha = 0.55f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(CyanPrimary, CyanBright)
                            )
                        )
                )
            }
            Text(
                text = "${(currentIndex + 1).coerceAtMost(total)}/$total",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CyanBright,
                softWrap = false
            )
        }

        IconButton(
            onClick = onReportClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("lesson_report_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = "Laporkan masalah materi",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun LessonStepBadgeRow(
    node: MriNode,
    currentStep: LessonStep
) {
    val shortCode = NodeDisplay.shortCode(node.code)
    val nodeTitle = NodeDisplay.displayTitle(node)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f, fill = false),
            shape = RoundedCornerShape(999.dp),
            color = CyanPrimary.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f))
        ) {
            Text(
                text = "$shortCode · $nodeTitle",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CyanBright,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        when (currentStep) {
            is LessonStep.Opening -> {
                Text(
                    text = "Pembuka",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    softWrap = false
                )
            }
            is LessonStep.Explain -> {
                Text(
                    text = "Penjelasan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanBright,
                    softWrap = false
                )
            }
            is LessonStep.Analogy -> {
                Text(
                    text = "Analogi Klinis",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanBright,
                    softWrap = false
                )
            }
            is LessonStep.Interactive -> {
                if (currentStep.isDraft) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = AmberWarning.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = "Interaktif · DRAF",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberWarning,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Interaktif",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        softWrap = false
                    )
                }
            }
            is LessonStep.Story -> {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = AmberWarning.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = if (currentStep.isDraft) "Soal cerita · DRAF" else "Soal cerita",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberWarning,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            is LessonStep.Check -> {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = CyanPrimary.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "Cek ringkasan",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanBright,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            is LessonStep.Complete -> Unit
        }
    }
}

@Composable
private fun ProgressiveExplainStack(
    steps: List<LessonStep>,
    activeStepIndex: Int,
    glossaryItems: List<GlossaryItem>,
    reduceMotion: Boolean,
    onGlossaryTermClick: (GlossaryItem) -> Unit
) {
    val activeStep = steps.getOrNull(activeStepIndex) ?: return

    AnimatedContent(
        targetState = activeStepIndex,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
            } else {
                val forward = targetState >= initialState
                (fadeIn(animationSpec = tween(220)) +
                    slideInHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { width ->
                        if (forward) width / 4 else -width / 4
                    }) togetherWith
                    (fadeOut(animationSpec = tween(160)) +
                        slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { width ->
                            if (forward) -width / 5 else width / 5
                        })
            }
        },
        label = "lesson_window_slide"
    ) { stepIdx ->
        val step = steps.getOrNull(stepIdx) ?: activeStep
        var matchedGlossary: GlossaryItem? = null

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (step) {
                is LessonStep.Opening -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = DarkNavySlate,
                        border = BorderStroke(1.4.dp, CyanPrimary.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "TUJUAN SIMPUL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanBright,
                                letterSpacing = 0.7.sp
                            )
                            Text(
                                text = step.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                lineHeight = 27.sp
                            )
                            Text(
                                text = step.goal,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                lineHeight = 24.5.sp
                            )
                        }
                    }
                }

                is LessonStep.Explain -> {
                    val (annotated, foundGlossary) = remember(step.chunk, glossaryItems) {
                        buildChunkAnnotatedString(
                            chunk = step.chunk,
                            glossaryItems = glossaryItems,
                            isActive = true
                        )
                    }
                    matchedGlossary = foundGlossary

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = DarkNavySlate,
                        border = BorderStroke(1.4.dp, CyanPrimary.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ClickableText(
                                text = annotated,
                                modifier = Modifier.fillMaxWidth(),
                                style = TextStyle(
                                    fontSize = 17.sp,
                                    lineHeight = 26.5.sp,
                                    color = TextPrimary
                                ),
                                onClick = { offset ->
                                    annotated.getStringAnnotations(
                                        tag = "GLOSSARY",
                                        start = offset,
                                        end = offset
                                    ).firstOrNull()?.let { ann ->
                                        val item = glossaryItems.firstOrNull { it.term.equals(ann.item, ignoreCase = true) }
                                            ?: MriRepository.glossary.firstOrNull { it.term.equals(ann.item, ignoreCase = true) }
                                        item?.let(onGlossaryTermClick)
                                    }
                                }
                            )
                        }
                    }
                }

                is LessonStep.Analogy -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = androidx.compose.ui.graphics.Color(0xFF0A2433),
                        border = BorderStroke(1.4.dp, CyanPrimary.copy(alpha = 0.60f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ANALOGI KLINIS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanBright,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = step.text,
                                fontSize = 16.5.sp,
                                color = TextPrimary,
                                lineHeight = 25.sp
                            )
                        }
                    }
                }

                else -> Unit
            }

            // Kartu "Istilah baru: <term>" jika ada istilah glosarium yang cocok di jendela aktif
            matchedGlossary?.let { matched ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button) { onGlossaryTermClick(matched) },
                    shape = RoundedCornerShape(14.dp),
                    color = DarkNavySlate,
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.38f))
                ) {
                    Text(
                        text = buildAnnotatedString {
                            pushStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = TextPrimary))
                            append("Istilah baru: ")
                            pop()
                            pushStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = CyanBright,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append(matched.term.lowercase())
                            pop()
                            pushStyle(SpanStyle(color = TextSecondary))
                            append(". Ketuk istilah untuk melihat artinya.")
                            pop()
                        },
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

private fun buildChunkAnnotatedString(
    chunk: LessonChunk,
    glossaryItems: List<GlossaryItem>,
    isActive: Boolean
): Pair<AnnotatedString, GlossaryItem?> {
    val fullText = if (!chunk.leadIn.isNullOrBlank()) {
        "${chunk.leadIn} ${chunk.text}".trim()
    } else {
        chunk.text
    }
    val leadLength = chunk.leadIn?.length ?: 0

    data class TermMatch(val item: GlossaryItem, val range: IntRange)
    val matches = mutableListOf<TermMatch>()

    if (glossaryItems.isNotEmpty()) {
        val lowerText = fullText.lowercase()
        // Urutkan kandidat dari istilah terpanjang agar frasa lengkap seperti "Phase Encoding" tidak terpotong kata pendek
        val sortedItems = glossaryItems.sortedByDescending { it.term.length }
        for (item in sortedItems) {
            val candidates = listOf(item.term.trim(), item.symbolOrAbbr.trim())
                .filter { it.length >= 3 }
            for (cand in candidates) {
                var searchStart = 0
                while (searchStart < lowerText.length) {
                    val idx = lowerText.indexOf(cand.lowercase(), searchStart)
                    if (idx < 0) break
                    val endIdx = idx + cand.length
                    val leftBoundaryOk = idx == 0 || !lowerText[idx - 1].isLetterOrDigit()
                    val rightBoundaryOk = endIdx >= lowerText.length || !lowerText[endIdx].isLetterOrDigit()
                    val overlaps = matches.any { m -> !(endIdx <= m.range.first || idx > m.range.last) }
                    if (leftBoundaryOk && rightBoundaryOk && !overlaps) {
                        matches.add(TermMatch(item, idx until endIdx))
                    }
                    searchStart = idx + 1
                }
            }
        }
    }

    val primaryMatch = matches.firstOrNull()?.item

    val annotated = buildAnnotatedString {
        append(fullText)
        if (leadLength > 0 && leadLength <= fullText.length) {
            addStyle(
                style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = TextPrimary),
                start = 0,
                end = leadLength
            )
        } else {
            addStyle(
                style = SpanStyle(
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    color = TextPrimary
                ),
                start = 0,
                end = fullText.length
            )
        }

        matches.forEach { match ->
            addStyle(
                style = SpanStyle(
                    color = CyanBright,
                    fontWeight = FontWeight.ExtraBold,
                    textDecoration = TextDecoration.Underline
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
            addStringAnnotation(
                tag = "GLOSSARY",
                annotation = match.item.term,
                start = match.range.first,
                end = match.range.last + 1
            )
        }
    }

    return annotated to primaryMatch
}

@Composable
private fun LessonBottomNanoBar(
    characterCustomization: CharacterCustomizationState,
    expression: Expression,
    nanoTapCount: Int,
    nanoBubbleText: String?,
    primaryButtonLabel: String,
    showTapAnywhereHint: Boolean,
    onNanoClick: () -> Unit,
    onPrimaryClick: () -> Unit
) {
    val nanoScale by animateFloatAsState(
        targetValue = if (nanoTapCount % 2 == 1) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "lesson_nano_scale"
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Maskot Nano Interaktif & Balon Ucapan di Kiri Bawah
            Row(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClick = onNanoClick)
                    .semantics { contentDescription = "Maskot Nano, ketuk untuk tips tambahan" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AvatarView(
                    role = characterCustomization.avatarId,
                    equipped = characterCustomization,
                    expression = expression,
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            scaleX = nanoScale
                            scaleY = nanoScale
                        }
                )
                if (!nanoBubbleText.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.weight(1f, fill = false),
                        shape = RoundedCornerShape(14.dp),
                        color = DarkNavySlate,
                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = nanoBubbleText,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 16.5.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Tombol Lanjut di Kanan Bawah
            Button(
                onClick = onPrimaryClick,
                modifier = Modifier
                    .defaultMinSize(minWidth = 112.dp, minHeight = 50.dp)
                    .testTag("lesson_next_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = DeepObsidian
                ),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(
                    text = primaryButtonLabel,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepObsidian,
                    softWrap = false
                )
            }
        }

        if (showTapAnywhereHint) {
            Text(
                text = "Ketuk Nano untuk tips atau ketuk layar untuk lanjut",
                fontSize = 12.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun LessonQuestionView(
    itemId: String,
    scenarioText: String?,
    questionText: String,
    options: List<String>,
    correctIndex: Int,
    explanation: String,
    @Suppress("UNUSED_PARAMETER") followUps: List<String>,
    lessonState: LessonState,
    characterCustomization: CharacterCustomizationState,
    onSelectOption: (Int) -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = lessonState.phaseFor(itemId)
    val attempts = lessonState.attemptsByItem[itemId].orEmpty()
    val latestPicked = attempts.lastOrNull()
    val isAwaitingSecondTry = itemId in lessonState.awaitingSecondSelection
    val canTapOptions = phase == QuestionPhase.UNANSWERED ||
        (phase == QuestionPhase.RETRY_ALLOWED && isAwaitingSecondTry)

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Kartu KASUS (untuk Soal Cerita)
            if (!scenarioText.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkNavySlate,
                    border = BorderStroke(1.dp, CardSlateBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "KASUS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberWarning,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = scenarioText,
                            fontSize = 14.5.sp,
                            color = TextPrimary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Teks Pertanyaan
            Text(
                text = questionText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                lineHeight = 26.sp
            )

            // Pilihan A, B, C, D
            val letters = listOf("A", "B", "C", "D", "E")
            options.forEachIndexed { idx, optText ->
                val isSelected = latestPicked == idx && !isAwaitingSecondTry
                val isFirstWrongOption = attempts.firstOrNull() == idx && attempts.firstOrNull() != correctIndex
                val isCorrectOption = idx == correctIndex

                val borderColor = when {
                    phase == QuestionPhase.RESOLVED_CORRECT && isCorrectOption -> EmeraldSuccess
                    phase == QuestionPhase.RESOLVED_WRONG_TWICE && isCorrectOption -> EmeraldSuccess
                    isSelected && !isCorrectOption -> AmberWarning
                    isFirstWrongOption && isAwaitingSecondTry -> CardSlateBorder.copy(alpha = 0.45f)
                    else -> CardSlateBorder
                }

                val bgColor = when {
                    phase == QuestionPhase.RESOLVED_CORRECT && isCorrectOption -> EmeraldSuccess.copy(alpha = 0.14f)
                    phase == QuestionPhase.RESOLVED_WRONG_TWICE && isCorrectOption -> EmeraldSuccess.copy(alpha = 0.14f)
                    isSelected && !isCorrectOption -> AmberWarning.copy(alpha = 0.12f)
                    else -> DarkNavySlate
                }

                val badgeBg = when {
                    (phase == QuestionPhase.RESOLVED_CORRECT || phase == QuestionPhase.RESOLVED_WRONG_TWICE) && isCorrectOption -> EmeraldSuccess
                    isSelected && !isCorrectOption -> AmberWarning
                    else -> CardSlate
                }

                val badgeTextCol = when {
                    (phase == QuestionPhase.RESOLVED_CORRECT || phase == QuestionPhase.RESOLVED_WRONG_TWICE) && isCorrectOption -> DeepObsidian
                    isSelected && !isCorrectOption -> DeepObsidian
                    else -> TextSecondary
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            enabled = canTapOptions && !(isFirstWrongOption && isAwaitingSecondTry),
                            role = Role.Button
                        ) {
                            onSelectOption(idx)
                        }
                        .testTag("lesson_option_$idx"),
                    shape = RoundedCornerShape(14.dp),
                    color = bgColor,
                    border = BorderStroke(1.3.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(badgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letters.getOrElse(idx) { "${idx + 1}" },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeTextCol
                            )
                        }
                        Text(
                            text = optText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isFirstWrongOption && isAwaitingSecondTry) TextMuted else TextPrimary,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Panel Umpan Balik Bawah sesuai Fase Jawaban (Mockup 3)
        when {
            phase == QuestionPhase.RETRY_ALLOWED && !isAwaitingSecondTry -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1F1A14),
                    border = BorderStroke(1.2.dp, AmberWarning.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AvatarView(
                                role = characterCustomization.avatarId,
                                equipped = characterCustomization,
                                expression = Expression.BERPIKIR,
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                text = "Belum tepat — boleh coba 1 kali lagi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberWarning
                            )
                        }
                        if (explanation.isNotBlank()) {
                            Text(
                                text = explanation,
                                fontSize = 13.5.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onRetry,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 140.dp, minHeight = 48.dp)
                                    .testTag("lesson_retry_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberWarning,
                                    contentColor = DeepObsidian
                                )
                            ) {
                                Text(
                                    text = "Coba lagi",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            phase == QuestionPhase.RESOLVED_CORRECT || phase == QuestionPhase.RESOLVED_WRONG_TWICE -> {
                val isCorrect = phase == QuestionPhase.RESOLVED_CORRECT
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isCorrect) Color(0xFF0A2823) else Color(0xFF1B2434),
                    border = BorderStroke(
                        1.2.dp,
                        if (isCorrect) EmeraldSuccess.copy(alpha = 0.7f) else CyanPrimary.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MriGlobalAssetImage(
                                assetKey = if (isCorrect) MriGlobalAssetKey.CHECK_VERIFIED else MriGlobalAssetKey.SPARK_BOLT,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (isCorrect) "Tepat!" else "Jawaban yang benar telah ditandai",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCorrect) EmeraldSuccess else CyanBright
                            )
                        }
                        if (explanation.isNotBlank()) {
                            Text(
                                text = explanation,
                                fontSize = 13.5.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cip followUps non-interaktif sengaja disembunyikan agar tidak menyerupai tombol aksi.
                            Button(
                                onClick = onNext,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 136.dp, minHeight = 50.dp)
                                    .testTag("lesson_question_next_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanPrimary,
                                    contentColor = DeepObsidian
                                )
                            ) {
                                Text(
                                    text = "Lanjut",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepObsidian
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AvatarView(
                        role = characterCustomization.avatarId,
                        equipped = characterCustomization,
                        expression = if (isAwaitingSecondTry) Expression.BERPIKIR else Expression.NETRAL,
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = if (isAwaitingSecondTry) {
                            "Pilih satu jawaban lain yang menurutmu paling tepat."
                        } else {
                            "Ketuk salah satu pilihan jawaban di atas."
                        },
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
