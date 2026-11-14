package com.example.ui.arcade

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ArcadeStats
import com.example.data.CharacterCustomizationState
import com.example.ui.character.AvatarView
import com.example.ui.character.Expression
import com.example.ui.components.MriGlobalAssetImage
import com.example.ui.components.MriGlobalAssetKey
import com.example.ui.path.rememberModulePathStateHolder
import com.example.ui.theme.AmberWarning
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
import com.example.util.rememberMriHaptics
import kotlin.math.PI
import kotlin.math.cos
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
                pathHolder.recordDailyShiftAsync(todayKey, labsDelta = 1)
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
    val haptics = rememberMriHaptics()
    var nanoTapCount by remember { mutableIntStateOf(0) }
    var showNanoTip by remember { mutableStateOf(false) }

    val nanoTips = remember(stats.highestRank, stats.minigameRounds) {
        listOf(
            "Ketuk aku kapan saja! Selesaikan ronde Arcade & Minigame untuk mengisi misi Shift Nano harianmu.",
            "Di Tembak Frekuensi Larmor, perhatikan monitor osiloskop: ω₀ = γ · B₀ (42,58 MHz/T untuk ¹H).",
            "Di Magnet Defense, benda MR Safe aman masuk bore B₀ — fokus tangkis benda feromagnetik merah!",
            "Di Spin Echo Rhythm, capai Combo x6 untuk mengaktifkan Mode TSE 2x dan menggandakan skor!"
        )
    }
    val nanoExpression = when {
        !showNanoTip -> Expression.BANGGA
        nanoTapCount % 4 == 1 -> Expression.SENANG
        nanoTapCount % 4 == 2 -> Expression.BERPIKIR
        nanoTapCount % 4 == 3 -> Expression.WASPADA
        else -> Expression.BANGGA
    }
    val nanoBounce by animateFloatAsState(
        targetValue = if (showNanoTip && nanoTapCount % 2 == 1) 1.09f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "arcade_hub_nano_bounce"
    )

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
            IconButton(
                onClick = {
                    haptics.lightToggle()
                    onBackToHome()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali ke Beranda",
                    tint = TextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Arcade & Minigame Nano", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(role = Role.Button) {
                            haptics.lightToggle()
                            if (!showNanoTip) {
                                showNanoTip = true
                            } else {
                                nanoTapCount++
                            }
                        }
                        .semantics { contentDescription = "Kartu Nano Arcade, ketuk untuk petunjuk Nano" },
                    shape = RoundedCornerShape(18.dp),
                    color = DarkNavySlate,
                    border = BorderStroke(1.2.dp, CyanPrimary.copy(alpha = 0.65f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AvatarView(
                                role = characterCustomization.avatarId,
                                equipped = characterCustomization,
                                expression = nanoExpression,
                                modifier = Modifier
                                    .size(64.dp)
                                    .graphicsLayer {
                                        scaleX = nanoBounce
                                        scaleY = nanoBounce
                                    }
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MriGlobalAssetImage(
                                        assetKey = MriGlobalAssetKey.TROPHY_MEDAL,
                                        modifier = Modifier.size(18.dp),
                                        tintAccent = AmberWarning
                                    )
                                    Text(
                                        text = "Pangkat: ${stats.highestRank}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AmberWarning
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = CyanPrimary.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            MriGlobalAssetImage(
                                                assetKey = MriGlobalAssetKey.GAME_CONTROLLER,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "${stats.minigameRounds} Ronde",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyanBright
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = EmeraldSuccess.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            MriGlobalAssetImage(
                                                assetKey = MriGlobalAssetKey.SPARK_BOLT,
                                                modifier = Modifier.size(13.dp),
                                                tintAccent = EmeraldSuccess
                                            )
                                            Text(
                                                text = "+Shift Nano",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldSuccess
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (showNanoTip) "Ketuk Nano untuk tips berikutnya" else "Ketuk Nano untuk briefing arena",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        AnimatedVisibility(visible = showNanoTip) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DeepObsidian.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, CyanBright.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = nanoTips[nanoTapCount % nanoTips.size],
                                    fontSize = 11.5.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
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
                    subtitle = "Zona IV · Feromagnetik vs MR Safe",
                    bestLabel = "Rekor: ${stats.bestScreening}",
                    accent = RoseDanger,
                    artKind = 0,
                    onClick = { onSelectRoute(ArcadeRoute.MINIGAME_SCREENING) }
                )
            }
            item {
                ArcadeGameCard(
                    title = MinigameKind.LARMOR_TAP.title,
                    subtitle = "Presesi ¹H · Ketuk Tepat Frekuensi Monitor RF",
                    bestLabel = "Rekor: ${stats.bestLarmor}",
                    accent = CharActionOrange,
                    artKind = 1,
                    onClick = { onSelectRoute(ArcadeRoute.MINIGAME_LARMOR) }
                )
            }
            item {
                ArcadeGameCard(
                    title = MinigameKind.CONTRAST_ARTIFACT_FIX.title,
                    subtitle = "T1/T2/FLAIR & Solusi Cepat Artefak",
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
                    subtitle = "Arena 5 Gauss · Tangkis Proyektil + EMP Burst",
                    bestLabel = "High Score: ${stats.bestDefense}",
                    accent = CyanBright,
                    artKind = 3,
                    onClick = { onSelectRoute(ArcadeRoute.ARCADE_MAGNET_DEFENSE) }
                )
            }
            item {
                ArcadeGameCard(
                    title = "Spin Echo Rhythm",
                    subtitle = "3 Lajur Pulsa · 90° → 180° → Echo + Mode TSE 2x",
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
    val haptics = rememberMriHaptics()
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
    val backdropRes = when (artKind) {
        0 -> R.drawable.img_arcade_screening_gate
        1 -> R.drawable.img_larmor_monitor
        2 -> R.drawable.img_mri_global_placeholder
        3 -> R.drawable.img_arcade_magnet_arena
        else -> R.drawable.img_arcade_rhythm_stage
    }

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
                        haptics.lightToggle()
                        onClick()
                    }
                ),
            shape = RoundedCornerShape(18.dp),
            color = DarkNavySlate,
            border = BorderStroke(1.4.dp, accent.copy(alpha = 0.72f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Simbol minigame/arcade berbasis aset game + vektor beranimasi selaras tema Nano
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFF071322)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = backdropRes),
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 0.72f },
                        contentScale = ContentScale.Crop
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val glow = 0.5f + 0.5f * sin(phase * 2f * PI).toFloat()
                        drawRoundRect(
                            color = accent.copy(alpha = 0.55f),
                            topLeft = Offset(1.5f, 1.5f),
                            size = Size(w - 3f, h - 3f),
                            cornerRadius = CornerRadius(15.dp.toPx(), 15.dp.toPx()),
                            style = Stroke(width = 2.4f)
                        )
                        when (artKind) {
                            0 -> {
                                val scanY = h * (0.22f + 0.56f * glow)
                                drawLine(CyanBright.copy(alpha = 0.85f), Offset(w * 0.16f, scanY), Offset(w * 0.84f, scanY), 2.8f, StrokeCap.Round)
                                drawCircle(RoseDanger, radius = 6.5f, center = Offset(w * 0.34f, h * 0.36f))
                                drawCircle(EmeraldSuccess, radius = 6.5f, center = Offset(w * 0.66f, h * 0.64f))
                            }
                            1 -> {
                                val path = Path()
                                for (i in 0..24) {
                                    val t = i / 24f
                                    val env = 1f - kotlin.math.abs(t - 0.5f) * 1.4f
                                    val y = h * 0.44f - sin((t * 5f + phase) * 2f * PI).toFloat() * h * 0.20f * env.coerceAtLeast(0.2f)
                                    val x = w * 0.16f + t * w * 0.68f
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path, color = CharActionOrange, style = Stroke(3.2f, cap = StrokeCap.Round))
                                val needleX = w * (0.26f + 0.48f * glow)
                                drawLine(AmberWarning, Offset(needleX, h * 0.24f), Offset(needleX, h * 0.66f), 2.6f, StrokeCap.Round)
                            }
                            2 -> {
                                drawOval(CharAccentMint, Offset(w * 0.24f, h * 0.18f), Size(w * 0.52f, h * 0.64f), style = Stroke(3f))
                                drawCircle(AmberWarning, radius = 4.2f, center = Offset(w * 0.60f, h * (0.36f + 0.14f * glow)))
                            }
                            3 -> {
                                drawCircle(RoseDanger, w * 0.24f, Offset(w / 2f, h / 2f), style = Stroke(3f))
                                val px = w * 0.5f + cos(phase * 2f * PI).toFloat() * w * 0.34f
                                val py = h * 0.5f + sin(phase * 2f * PI).toFloat() * h * 0.34f
                                drawCircle(AmberWarning, 4.8f, Offset(px, py))
                            }
                            else -> {
                                val yOff = (phase * h * 0.24f)
                                drawRoundRect(CyanBright, Offset(w * 0.14f, h * 0.24f + yOff), Size(w * 0.20f, 9f), CornerRadius(4f, 4f))
                                drawRoundRect(CharActionOrange, Offset(w * 0.40f, h * 0.52f), Size(w * 0.20f, 9f), CornerRadius(4f, 4f))
                                drawRoundRect(EmeraldSuccess, Offset(w * 0.66f, h * 0.34f + yOff * 0.5f), Size(w * 0.20f, 9f), CornerRadius(4f, 4f))
                            }
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
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = accent.copy(alpha = 0.18f),
                            border = BorderStroke(1.2.dp, accent.copy(alpha = 0.65f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                MriGlobalAssetImage(
                                    assetKey = MriGlobalAssetKey.TROPHY_MEDAL,
                                    modifier = Modifier.size(12.dp),
                                    tintAccent = accent
                                )
                                Text(
                                    text = bestLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accent,
                                    maxLines = 1
                                )
                            }
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
