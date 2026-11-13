package com.example.ui.arcade

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArcadeStats
import com.example.data.CharacterCustomizationState
import com.example.ui.character.AvatarView
import com.example.ui.character.Expression
import com.example.ui.path.rememberModulePathStateHolder
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CharAccentMint
import com.example.ui.theme.CharActionOrange
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.MinigameKind
import com.example.util.MriMinigameEngine
import com.example.util.TapAccuracy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private enum class ArcadeRoute {
    HUB,
    MINIGAME_SCREENING,
    MINIGAME_LARMOR,
    MINIGAME_ARTIFACT,
    ARCADE_MAGNET_DEFENSE,
    ARCADE_SPIN_ECHO_RHYTHM
}

@Composable
fun ArcadeHubScreen(
    characterCustomization: CharacterCustomizationState,
    onBackToHome: () -> Unit
) {
    val pathHolder = rememberModulePathStateHolder()
    val stats by pathHolder.arcadeStatsFlow.collectAsState(initial = ArcadeStats())
    val scope = rememberCoroutineScope()
    val todayKey = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }
    var route by remember { mutableStateOf(ArcadeRoute.HUB) }

    BackHandler {
        if (route != ArcadeRoute.HUB) {
            route = ArcadeRoute.HUB
        } else {
            onBackToHome()
        }
    }

    when (route) {
        ArcadeRoute.HUB -> ArcadeHubMenu(
            characterCustomization = characterCustomization,
            stats = stats,
            onSelectRoute = { route = it },
            onBackToHome = onBackToHome
        )

        ArcadeRoute.MINIGAME_SCREENING -> MinigamePlayHost(
            kind = MinigameKind.ZONE_SCREENING,
            bestScore = stats.bestScreening,
            characterCustomization = characterCustomization,
            onScoreUpdated = { liveScore ->
                if (liveScore > stats.bestScreening) {
                    pathHolder.recordMinigameResultAsync(MinigameKind.ZONE_SCREENING.key, liveScore)
                }
            },
            onRoundFinished = { finalScore ->
                pathHolder.recordMinigameResultAsync(MinigameKind.ZONE_SCREENING.key, finalScore)
                pathHolder.recordDailyShiftAsync(todayKey, labsDelta = 1)
            },
            onBack = { route = ArcadeRoute.HUB }
        )

        ArcadeRoute.MINIGAME_LARMOR -> MinigamePlayHost(
            kind = MinigameKind.LARMOR_TAP,
            bestScore = stats.bestLarmor,
            characterCustomization = characterCustomization,
            onScoreUpdated = { liveScore ->
                if (liveScore > stats.bestLarmor) {
                    pathHolder.recordMinigameResultAsync(MinigameKind.LARMOR_TAP.key, liveScore)
                }
            },
            onRoundFinished = { finalScore ->
                pathHolder.recordMinigameResultAsync(MinigameKind.LARMOR_TAP.key, finalScore)
                pathHolder.recordDailyShiftAsync(todayKey, labsDelta = 1)
            },
            onBack = { route = ArcadeRoute.HUB }
        )

        ArcadeRoute.MINIGAME_ARTIFACT -> MinigamePlayHost(
            kind = MinigameKind.CONTRAST_ARTIFACT_FIX,
            bestScore = stats.bestArtifact,
            characterCustomization = characterCustomization,
            onScoreUpdated = { liveScore ->
                if (liveScore > stats.bestArtifact) {
                    pathHolder.recordMinigameResultAsync(MinigameKind.CONTRAST_ARTIFACT_FIX.key, liveScore)
                }
            },
            onRoundFinished = { finalScore ->
                pathHolder.recordMinigameResultAsync(MinigameKind.CONTRAST_ARTIFACT_FIX.key, finalScore)
                pathHolder.recordDailyShiftAsync(todayKey, quizzesDelta = 1)
            },
            onBack = { route = ArcadeRoute.HUB }
        )

        ArcadeRoute.ARCADE_MAGNET_DEFENSE -> MagnetDefenseScreen(
            characterCustomization = characterCustomization,
            bestScore = stats.bestDefense,
            onScoreUpdated = { liveScore, rankTitle ->
                if (liveScore > stats.bestDefense) {
                    pathHolder.recordArcadeResultAsync("MAGNET_DEFENSE", liveScore, rankTitle)
                }
            },
            onGameFinished = { finalScore, rankTitle ->
                pathHolder.recordArcadeResultAsync("MAGNET_DEFENSE", finalScore, rankTitle)
                pathHolder.recordDailyShiftAsync(todayKey, labsDelta = 1)
            },
            onBack = { route = ArcadeRoute.HUB }
        )

        ArcadeRoute.ARCADE_SPIN_ECHO_RHYTHM -> SpinEchoRhythmScreen(
            characterCustomization = characterCustomization,
            bestScore = stats.bestRhythm,
            onScoreUpdated = { liveScore, rankTitle ->
                if (liveScore > stats.bestRhythm) {
                    pathHolder.recordArcadeResultAsync("SPIN_ECHO_RHYTHM", liveScore, rankTitle)
                }
            },
            onGameFinished = { finalScore, rankTitle ->
                pathHolder.recordArcadeResultAsync("SPIN_ECHO_RHYTHM", finalScore, rankTitle)
                pathHolder.recordDailyShiftAsync(todayKey, labsDelta = 1)
            },
            onBack = { route = ArcadeRoute.HUB }
        )
    }
}

@Composable
private fun ArcadeHubMenu(
    characterCustomization: CharacterCustomizationState,
    stats: ArcadeStats,
    onSelectRoute: (ArcadeRoute) -> Unit,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBackToHome, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali ke Beranda",
                    tint = TextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Arcade & Minigame Nano", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text("Latihan interaktif fisika & keselamatan MRI", fontSize = 11.5.sp, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = DarkNavySlate,
                    border = BorderStroke(1.2.dp, CyanPrimary.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarView(
                            role = characterCustomization.avatarId,
                            equipped = characterCustomization,
                            expression = Expression.BANGGA,
                            modifier = Modifier.size(62.dp)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Pangkat: ${stats.highestRank}", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = AmberWarning)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = CyanPrimary.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "🎮 ${stats.minigameRounds} Ronde",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanBright,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "⚡ +Shift Nano",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Minigame Cepat · 35 Detik", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = CyanBright)
            }

            item {
                ArcadeGameCard(
                    title = MinigameKind.ZONE_SCREENING.title,
                    subtitle = "🛡️ Zona IV · Feromagnetik vs MR Safe",
                    bestLabel = "Rekor: ${stats.bestScreening}",
                    accent = RoseDanger,
                    artKind = 0,
                    onClick = { onSelectRoute(ArcadeRoute.MINIGAME_SCREENING) }
                )
            }
            item {
                ArcadeGameCard(
                    title = MinigameKind.LARMOR_TAP.title,
                    subtitle = "📡 Presesi ¹H · Ketuk Tepat Frekuensi RF",
                    bestLabel = "Rekor: ${stats.bestLarmor}",
                    accent = CharActionOrange,
                    artKind = 1,
                    onClick = { onSelectRoute(ArcadeRoute.MINIGAME_LARMOR) }
                )
            }
            item {
                ArcadeGameCard(
                    title = MinigameKind.CONTRAST_ARTIFACT_FIX.title,
                    subtitle = "🩻 T1/T2/FLAIR & Solusi Cepat Artefak",
                    bestLabel = "Rekor: ${stats.bestArtifact}",
                    accent = CharAccentMint,
                    artKind = 2,
                    onClick = { onSelectRoute(ArcadeRoute.MINIGAME_ARTIFACT) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Game Arcade · Wave & Combo", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = AmberWarning)
            }

            item {
                ArcadeGameCard(
                    title = "Nano Magnet Defense",
                    subtitle = "🧲 Arena 5 Gauss · Tangkis Proyektil + EMP Burst",
                    bestLabel = "High Score: ${stats.bestDefense}",
                    accent = CyanBright,
                    artKind = 3,
                    onClick = { onSelectRoute(ArcadeRoute.ARCADE_MAGNET_DEFENSE) }
                )
            }
            item {
                ArcadeGameCard(
                    title = "Spin Echo Rhythm",
                    subtitle = "🎵 3 Lajur Pulsa · 90° → 180° → Echo + Mode TSE 2x",
                    bestLabel = "High Score: ${stats.bestRhythm}",
                    accent = EmeraldSuccess,
                    artKind = 4,
                    onClick = { onSelectRoute(ArcadeRoute.ARCADE_SPIN_ECHO_RHYTHM) }
                )
            }
        }
    }
}

@Composable
private fun ArcadeGameCard(
    title: String,
    subtitle: String,
    bestLabel: String,
    accent: Color,
    artKind: Int,
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val anim = rememberInfiniteTransition(label = "arcade_card_art_$artKind")
    val phase by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 88.dp)
    ) {
        // Dudukan 3D bawah kartu arcade
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(accent.copy(alpha = 0.32f))
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(
                    role = Role.Button,
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                ),
            shape = RoundedCornerShape(18.dp),
            color = DarkNavySlate,
            border = BorderStroke(1.4.dp, accent.copy(alpha = 0.72f))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Ilustrasi Canvas multi-layer beranimasi untuk tiap game
                    Canvas(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFF071322))
                    ) {
                        val w = size.width
                        val h = size.height
                        val glow = 0.5f + 0.5f * sin(phase * 2f * PI).toFloat()
                        drawCircle(
                            color = accent.copy(alpha = 0.16f + 0.08f * glow),
                            radius = w * 0.45f,
                            center = Offset(w * 0.5f, h * 0.5f)
                        )
                        when (artKind) {
                            0 -> {
                                // Portal detektor feromagnetik Zona IV + laser pemindai dinamis
                                drawRoundRect(
                                    color = CyanPrimary.copy(alpha = 0.55f),
                                    topLeft = Offset(w * 0.16f, h * 0.10f),
                                    size = Size(w * 0.68f, h * 0.80f),
                                    cornerRadius = CornerRadius(10f, 10f),
                                    style = Stroke(width = 3.2f)
                                )
                                val scanY = h * (0.20f + 0.60f * glow)
                                drawLine(CyanBright.copy(alpha = 0.75f), Offset(w * 0.20f, scanY), Offset(w * 0.80f, scanY), 2.5f)
                                drawLine(RoseDanger, Offset(w * 0.14f, h * 0.78f), Offset(w * 0.86f, h * 0.22f), 4.5f, StrokeCap.Round)
                                drawCircle(AmberWarning, radius = 8f, center = Offset(w * 0.36f, h * 0.36f), style = Stroke(3f))
                                drawCircle(EmeraldSuccess, radius = 7.5f, center = Offset(w * 0.66f, h * 0.66f))
                            }
                            1 -> {
                                // Spektrum resonansi Larmor + osilasi pulsa RF
                                drawLine(
                                    color = CardSlateBorder,
                                    start = Offset(6f, h * 0.5f),
                                    end = Offset(w - 6f, h * 0.5f),
                                    strokeWidth = 2f
                                )
                                val path = Path()
                                for (i in 0..28) {
                                    val t = i / 28f
                                    val env = 1f - kotlin.math.abs(t - 0.5f) * 1.4f
                                    val y = h * 0.5f - sin((t * 5f + phase) * 2f * PI).toFloat() * h * 0.34f * env.coerceAtLeast(0.2f)
                                    if (i == 0) path.moveTo(6f, y) else path.lineTo(6f + t * (w - 12f), y)
                                }
                                drawPath(path, color = CharActionOrange, style = Stroke(3.6f, cap = StrokeCap.Round))
                                drawCircle(CyanBright, radius = 4.8f, center = Offset(w * 0.5f, h * 0.2f))
                            }
                            2 -> {
                                // Slice aksial otak MRI + garis koreksi K-space
                                drawRoundRect(
                                    color = CharAccentMint.copy(alpha = 0.22f),
                                    topLeft = Offset(8f, 8f),
                                    size = Size(w - 16f, h - 16f),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                                drawOval(
                                    color = CharAccentMint,
                                    topLeft = Offset(w * 0.22f, h * 0.16f),
                                    size = Size(w * 0.56f, h * 0.68f),
                                    style = Stroke(3.2f)
                                )
                                drawLine(CyanBright, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), 2.2f)
                                drawCircle(AmberWarning, radius = 4.5f, center = Offset(w * 0.62f, h * (0.34f + 0.16f * glow)))
                            }
                            3 -> {
                                // Radar kubah 5 Gauss + proyektil mengorbit
                                drawCircle(CyanBright.copy(alpha = 0.30f), w * 0.42f, Offset(w / 2f, h / 2f), style = Stroke(2.4f))
                                drawCircle(RoseDanger, w * 0.27f, Offset(w / 2f, h / 2f), style = Stroke(3.2f))
                                drawCircle(CyanBright, w * 0.13f, Offset(w / 2f, h / 2f))
                                val px = w * 0.5f + kotlin.math.cos(phase * 2f * PI).toFloat() * w * 0.34f
                                val py = h * 0.5f + sin(phase * 2f * PI).toFloat() * h * 0.34f
                                drawCircle(AmberWarning, 5f, Offset(px, py))
                                drawCircle(EmeraldSuccess, 4.2f, Offset(w * 0.24f, h * 0.74f))
                            }
                            else -> {
                                // 3 lajur pulsa ritme Spin Echo + gelombang gema
                                drawRoundRect(CyanBright, Offset(10f, h * 0.68f), Size(w - 20f, 7f), CornerRadius(4f, 4f))
                                drawRoundRect(CharActionOrange, Offset(14f, h * 0.46f), Size(w - 28f, 7f), CornerRadius(4f, 4f))
                                drawRoundRect(EmeraldSuccess, Offset(18f, h * 0.24f), Size(w - 36f, 7f), CornerRadius(4f, 4f))
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                fontSize = 14.2.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Informasi High Score / Rekor ditempatkan di ujung kanan kartu
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = accent.copy(alpha = 0.18f),
                                border = BorderStroke(1.2.dp, accent.copy(alpha = 0.65f))
                            ) {
                                Text(
                                    text = "🏆 $bestLabel",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accent,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = subtitle,
                            fontSize = 11.2.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinigamePlayHost(
    kind: MinigameKind,
    bestScore: Int,
    characterCustomization: CharacterCustomizationState,
    onScoreUpdated: (Int) -> Unit = {},
    onRoundFinished: (Int) -> Unit,
    onBack: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var roundSeed by remember(kind) { mutableIntStateOf(1) }
    var timeLeft by remember(kind) { mutableIntStateOf(kind.durationSec) }
    var score by remember(kind) { mutableIntStateOf(0) }
    var combo by remember(kind) { mutableIntStateOf(0) }
    var totalAnswered by remember(kind) { mutableIntStateOf(0) }
    var totalCorrect by remember(kind) { mutableIntStateOf(0) }
    var itemIdx by remember(kind) { mutableIntStateOf(0) }
    var feedbackMsg by remember(kind) { mutableStateOf("Ketuk cepat & tepat! Pertahankan combo untuk mengaktifkan Overdrive 2x.") }
    var feedbackGood by remember(kind) { mutableStateOf(true) }
    var roundDone by remember(kind) { mutableStateOf(false) }

    val shuffledScreening = remember(kind, roundSeed) {
        MriMinigameEngine.screeningBank.shuffled()
    }
    val shuffledLarmor = remember(kind, roundSeed) {
        MriMinigameEngine.larmorChallenges.shuffled()
    }
    val shuffledArtifacts = remember(kind, roundSeed) {
        MriMinigameEngine.contrastArtifactBank.shuffled()
    }

    val waveAnim = rememberInfiniteTransition(label = "minigame_visual_wave")
    val wavePhase by waveAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    LaunchedEffect(score) {
        if (score > 0) {
            onScoreUpdated(score)
        }
    }

    androidx.compose.runtime.DisposableEffect(kind) {
        onDispose {
            if (score > 0 && !roundDone) {
                onRoundFinished(score)
            }
        }
    }

    LaunchedEffect(kind, roundDone) {
        if (roundDone) return@LaunchedEffect
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        roundDone = true
        onRoundFinished(score)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
            }
            Text(kind.title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (timeLeft <= 8) RoseDanger.copy(alpha = 0.25f) else CardSlate
            ) {
                Text(
                    text = "${timeLeft}s",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (timeLeft <= 8) RoseDanger else CyanBright,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        LinearProgressIndicator(
            progress = { timeLeft.toFloat() / kind.durationSec.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (timeLeft <= 8) RoseDanger else CyanBright,
            trackColor = CardSlate
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Skor: $score", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyanBright)
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (combo >= 3) AmberWarning.copy(alpha = 0.20f) else CardSlate,
                border = BorderStroke(1.dp, if (combo >= 3) AmberWarning else CardSlateBorder)
            ) {
                Text(
                    text = if (combo >= 3) "🔥 OVERDRIVE x$combo" else "Combo: x$combo",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (combo >= 3) AmberWarning else TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            Text("🏆 Rekor: ${maxOf(bestScore, score)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Nano pendamping minigame
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = DarkNavySlate,
            border = BorderStroke(1.dp, if (feedbackGood) CyanPrimary.copy(alpha = 0.5f) else RoseDanger.copy(alpha = 0.65f))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvatarView(
                    role = characterCustomization.avatarId,
                    equipped = characterCustomization,
                    expression = when {
                        roundDone -> Expression.BANGGA
                        !feedbackGood -> Expression.WASPADA
                        combo >= 3 -> Expression.BANGGA
                        totalAnswered == 0 -> Expression.BERPIKIR
                        else -> Expression.SENANG
                    },
                    modifier = Modifier.size(46.dp)
                )
                Text(
                    text = feedbackMsg,
                    fontSize = 11.8.sp,
                    color = TextPrimary,
                    lineHeight = 15.5.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (roundDone) {
            val acc = if (totalAnswered > 0) (totalCorrect * 100) / totalAnswered else 0
            val rank = MriMinigameEngine.evaluateRank(score, acc)
            val displayBest = maxOf(bestScore, score)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DarkNavySlate,
                border = BorderStroke(1.5.dp, CyanBright)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Ronde Selesai!", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text(rank, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                    Text("Skor: $score · Akurasi: $acc% · Rekor: $displayBest", fontSize = 13.sp, color = CyanBright)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onBack, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                            Text("Kembali", color = TextPrimary)
                        }
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                roundSeed++
                                timeLeft = kind.durationSec
                                score = 0
                                combo = 0
                                totalAnswered = 0
                                totalCorrect = 0
                                itemIdx = 0
                                feedbackGood = true
                                feedbackMsg = "Ronde baru dimulai dengan urutan & variasi acak!"
                                roundDone = false
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepObsidian)
                        ) {
                            Text("Main Lagi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            when (kind) {
                MinigameKind.ZONE_SCREENING -> {
                    val item = shuffledScreening[itemIdx % shuffledScreening.size]
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = DarkNavySlate,
                        border = BorderStroke(1.2.dp, CardSlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Ilustrasi Portal Detektor Feromagnetik Zona IV & Garis Medan B0
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(108.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF071322)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val scanX = w * (0.18f + 0.64f * wavePhase)
                                    // Lengkung portal detektor Zona III -> Zona IV
                                    drawRoundRect(
                                        color = CyanPrimary.copy(alpha = 0.45f),
                                        topLeft = Offset(w * 0.14f, h * 0.10f),
                                        size = Size(w * 0.72f, h * 0.82f),
                                        cornerRadius = CornerRadius(18f, 18f),
                                        style = Stroke(width = 4f)
                                    )
                                    // Garis fluks magnetik ke arah bore kanan
                                    for (r in 1..3) {
                                        val y = h * (0.22f * r)
                                        drawLine(
                                            color = CyanBright.copy(alpha = 0.22f),
                                            start = Offset(w * 0.18f, y),
                                            end = Offset(w * 0.82f, y),
                                            strokeWidth = 2f
                                        )
                                    }
                                    // Sinar pemindai vertikal
                                    drawLine(
                                        color = if (combo >= 3) AmberWarning.copy(alpha = 0.7f) else CyanBright.copy(alpha = 0.6f),
                                        start = Offset(scanX, h * 0.12f),
                                        end = Offset(scanX, h * 0.88f),
                                        strokeWidth = 3f,
                                        cap = StrokeCap.Round
                                    )
                                }
                                Text(item.emoji, fontSize = 48.sp)
                            }
                            Text(item.name, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = CyanPrimary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "Portal Detektor Feromagnetik · Pintu Zona IV (5 Gauss)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanBright,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                val ok = !item.allowIntoZone4
                                totalAnswered++
                                if (ok) {
                                    totalCorrect++
                                    combo++
                                    val mult = if (combo >= 3) 20 else 0
                                    score += MriMinigameEngine.computePoints(100 + mult, combo)
                                    if (timeLeft < kind.durationSec && combo % 4 == 0) timeLeft += 2
                                } else {
                                    combo = 0
                                }
                                feedbackGood = ok
                                feedbackMsg = (if (ok) "✓ Tepat! " else "✗ Bahaya! ") + item.reason
                                itemIdx++
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseDanger, contentColor = Color.White)
                        ) {
                            Text("🚫 TOLAK / LEPAS", fontWeight = FontWeight.ExtraBold)
                        }
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                val ok = item.allowIntoZone4
                                totalAnswered++
                                if (ok) {
                                    totalCorrect++
                                    combo++
                                    val mult = if (combo >= 3) 20 else 0
                                    score += MriMinigameEngine.computePoints(100 + mult, combo)
                                    if (timeLeft < kind.durationSec && combo % 4 == 0) timeLeft += 2
                                } else {
                                    combo = 0
                                }
                                feedbackGood = ok
                                feedbackMsg = (if (ok) "✓ Tepat! " else "✗ Kurang tepat! ") + item.reason
                                itemIdx++
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = DeepObsidian)
                        ) {
                            Text("✅ AMAN / LOLOS", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                MinigameKind.LARMOR_TAP -> {
                    val challenge = shuffledLarmor[itemIdx % shuffledLarmor.size]
                    val speedBoost = (1f + (combo.coerceAtMost(6) * 0.08f)) * challenge.speedMultiplier
                    val anim = rememberInfiniteTransition(label = "larmor_needle")
                    val needle by anim.animateFloat(
                        initialValue = 0.06f,
                        targetValue = 0.94f,
                        animationSpec = infiniteRepeatable(
                            animation = tween((1400 / speedBoost).toInt().coerceAtLeast(480), easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "needle"
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = DarkNavySlate,
                        border = BorderStroke(1.2.dp, CardSlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Medan B₀: ${challenge.fieldStrengthTesla}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyanBright
                                )
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, EmeraldSuccess)
                                ) {
                                    Text(
                                        text = "ω₀ = ${challenge.targetMhz}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldSuccess,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Ilustrasi Vektor Presesi Bloch 3D + Gelombang Spektrum RF
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF071322))
                            ) {
                                val w = size.width
                                val h = size.height
                                // Spektrum gelombang pembawa RF di kiri-tengah
                                val wavePath = Path()
                                val steps = 48
                                for (i in 0..steps) {
                                    val t = i / steps.toFloat()
                                    val x = 12f + t * (w - 24f)
                                    val distToTarget = kotlin.math.abs(t - challenge.targetNormalized)
                                    val env = (1f - distToTarget * 2.2f).coerceIn(0.15f, 1f)
                                    val y = h * 0.5f - sin((t * 8f + wavePhase * 2f) * PI).toFloat() * (h * 0.36f) * env
                                    if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                                }
                                drawPath(wavePath, color = CyanPrimary.copy(alpha = 0.55f), style = Stroke(width = 2.6f, cap = StrokeCap.Round))

                                // Bar spektrum target & jarum tuner
                                val targetX = challenge.targetNormalized * w
                                drawRoundRect(
                                    color = CyanBright.copy(alpha = 0.24f),
                                    topLeft = Offset(targetX - w * 0.12f, h * 0.18f),
                                    size = Size(w * 0.24f, h * 0.64f),
                                    cornerRadius = CornerRadius(10f, 10f)
                                )
                                drawRoundRect(
                                    color = EmeraldSuccess.copy(alpha = 0.58f),
                                    topLeft = Offset(targetX - w * 0.055f, h * 0.12f),
                                    size = Size(w * 0.11f, h * 0.76f),
                                    cornerRadius = CornerRadius(10f, 10f)
                                )
                                val needleX = needle * w
                                drawLine(AmberWarning, Offset(needleX, 4f), Offset(needleX, h - 4f), 7f, StrokeCap.Round)
                                drawCircle(Color.White, radius = 5.5f, center = Offset(needleX, h * 0.5f))
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    val acc = MriMinigameEngine.evaluateLarmorTap(needle, challenge.targetNormalized)
                                    totalAnswered++
                                    if (acc != TapAccuracy.MISS) {
                                        totalCorrect++
                                        combo++
                                        score += MriMinigameEngine.computePoints(acc.basePoints, combo)
                                        feedbackGood = true
                                    } else {
                                        combo = 0
                                        feedbackGood = false
                                    }
                                    feedbackMsg = "${acc.label} pada ${challenge.targetMhz} (${challenge.fieldStrengthTesla})!"
                                    itemIdx++
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepObsidian)
                            ) {
                                Text("⚡ TEMBAK PULSA RF 90°!", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                MinigameKind.CONTRAST_ARTIFACT_FIX -> {
                    val q = shuffledArtifacts[itemIdx % shuffledArtifacts.size]
                    val shuffledOptions = remember(q, itemIdx, roundSeed) {
                        val correctText = q.options[q.correctIndex]
                        val mixed = q.options.shuffled()
                        mixed to mixed.indexOf(correctText)
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = DarkNavySlate,
                        border = BorderStroke(1.2.dp, CardSlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Ilustrasi Phantom Citra MRI / K-space sesuai konteks soal
                                Canvas(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF071322))
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    val isKspace = q.badge.contains("K-space", ignoreCase = true)
                                    val isArtifact = q.badge.contains("Artefak", ignoreCase = true)
                                    if (isKspace) {
                                        val cell = w / 4f
                                        for (r in 0..3) {
                                            for (c in 0..3) {
                                                val center = r in 1..2 && c in 1..2
                                                drawRoundRect(
                                                    color = if (center) CyanBright else Color(0xFF194163),
                                                    topLeft = Offset(c * cell + 3f, r * cell + 3f),
                                                    size = Size(cell - 6f, cell - 6f),
                                                    cornerRadius = CornerRadius(3f, 3f)
                                                )
                                            }
                                        }
                                    } else {
                                        drawOval(
                                            color = Color(0xFF1E4970),
                                            topLeft = Offset(w * 0.18f, h * 0.12f),
                                            size = Size(w * 0.64f, h * 0.76f)
                                        )
                                        drawOval(
                                            color = CharAccentMint,
                                            topLeft = Offset(w * 0.18f, h * 0.12f),
                                            size = Size(w * 0.64f, h * 0.76f),
                                            style = Stroke(width = 2.6f)
                                        )
                                        // Ventrikel CSF
                                        val csfColor = when {
                                            q.prompt.contains("SANGAT TERANG") -> Color.White
                                            q.prompt.contains("GELAP") || q.prompt.contains("ditekan") -> Color(0xFF071322)
                                            else -> CyanBright
                                        }
                                        drawOval(csfColor, Offset(w * 0.36f, h * 0.36f), Size(w * 0.11f, h * 0.26f))
                                        drawOval(csfColor, Offset(w * 0.53f, h * 0.36f), Size(w * 0.11f, h * 0.26f))
                                        if (isArtifact) {
                                            drawArc(
                                                color = RoseDanger,
                                                startAngle = -80f,
                                                sweepAngle = 160f,
                                                useCenter = false,
                                                topLeft = Offset(w * 0.04f, h * 0.20f),
                                                size = Size(w * 0.32f, h * 0.60f),
                                                style = Stroke(width = 2.6f)
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = AmberWarning.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = q.badge,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AmberWarning,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.5.dp)
                                        )
                                    }
                                    Text(
                                        text = q.prompt,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            shuffledOptions.first.forEachIndexed { optIdx, label ->
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        val ok = optIdx == shuffledOptions.second
                                        totalAnswered++
                                        if (ok) {
                                            totalCorrect++
                                            combo++
                                            score += MriMinigameEngine.computePoints(110, combo)
                                        } else {
                                            combo = 0
                                        }
                                        feedbackGood = ok
                                        feedbackMsg = (if (ok) "✓ Tepat! " else "✗ Salah! ") + q.explanation
                                        itemIdx++
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, CyanPrimary.copy(alpha = 0.45f))
                                ) {
                                    Text(label, color = TextPrimary, fontSize = 12.8.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
