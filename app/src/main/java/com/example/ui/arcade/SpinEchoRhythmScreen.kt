package com.example.ui.arcade

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CharacterCustomizationState
import com.example.ui.character.AvatarView
import com.example.ui.character.Expression
import com.example.ui.components.MriGlobalAssetImage
import com.example.ui.components.MriGlobalAssetKey
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CharActionOrange
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.MriArcadeEngine
import com.example.util.PulseBeatKind
import com.example.util.SpinEchoRhythmState
import com.example.util.rememberMriHaptics
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SpinEchoRhythmScreen(
    characterCustomization: CharacterCustomizationState,
    bestScore: Int,
    onScoreUpdated: (score: Int, rankTitle: String) -> Unit = { _, _ -> },
    onGameFinished: (score: Int, rankTitle: String) -> Unit,
    onBack: () -> Unit
) {
    val haptics = rememberMriHaptics()
    var gameState by remember { mutableStateOf(SpinEchoRhythmState()) }
    var isPaused by remember { mutableStateOf(false) }
    var reported by remember { mutableStateOf(false) }
    var activeLaneFlash by remember { mutableStateOf<Int?>(null) }
    var nanoTipTap by remember { mutableIntStateOf(0) }
    var customNanoTip by remember { mutableStateOf<String?>(null) }

    val nanoBounce by animateFloatAsState(
        targetValue = if (nanoTipTap % 2 == 1) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rhythm_nano_bounce"
    )

    LaunchedEffect(activeLaneFlash) {
        if (activeLaneFlash != null) {
            delay(160L)
            activeLaneFlash = null
        }
    }

    LaunchedEffect(customNanoTip, nanoTipTap) {
        if (customNanoTip != null) {
            delay(2200L)
            customNanoTip = null
        }
    }

    BackHandler {
        if (gameState.score > 0 && !reported) {
            reported = true
            onGameFinished(gameState.score, gameState.rankTitle)
        }
        onBack()
    }

    LaunchedEffect(gameState.score, gameState.rankTitle) {
        if (gameState.score > 0) {
            onScoreUpdated(gameState.score, gameState.rankTitle)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (gameState.score > 0 && !reported) {
                onGameFinished(gameState.score, gameState.rankTitle)
            }
        }
    }

    LaunchedEffect(isPaused, gameState.isGameOver) {
        if (isPaused || gameState.isGameOver) return@LaunchedEffect
        var lastNs = System.nanoTime()
        while (!gameState.isGameOver && !isPaused) {
            delay(16L)
            val nowNs = System.nanoTime()
            val dt = ((nowNs - lastNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastNs = nowNs
            val prevLives = gameState.lives
            gameState = MriArcadeEngine.stepSpinEchoRhythm(gameState, dt)
            if (gameState.lives < prevLives) {
                haptics.warningBuzz()
            }
        }
    }

    LaunchedEffect(gameState.isGameOver) {
        if (gameState.isGameOver && !reported) {
            haptics.completionBuzz()
            reported = true
            onGameFinished(gameState.score, gameState.rankTitle)
        }
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
            IconButton(
                onClick = {
                    haptics.lightToggle()
                    if (gameState.score > 0 && !reported) {
                        reported = true
                        onGameFinished(gameState.score, gameState.rankTitle)
                    }
                    onBack()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali ke Hub Arcade",
                    tint = TextPrimary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Spin Echo Rhythm", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (gameState.isTurboMode) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (gameState.isTurboMode) {
                            "MODE TSE 2x AKTIF! · Rekor: ${maxOf(bestScore, gameState.score)}"
                        } else {
                            "90° → 180° → Baca Echo · Rekor: ${maxOf(bestScore, gameState.score)}"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (gameState.isTurboMode) AmberWarning else CyanBright
                    )
                }
            }
            IconButton(
                onClick = {
                    haptics.lightToggle()
                    if (!gameState.isGameOver) isPaused = !isPaused
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Lanjutkan" else "Jeda",
                    tint = CyanBright
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = DarkNavySlate,
            border = BorderStroke(1.dp, CardSlateBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SKOR", fontSize = 10.sp, color = TextSecondary)
                    Text("${gameState.score}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyanBright)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COMBO", fontSize = 10.sp, color = TextSecondary)
                    Text("x${gameState.combo}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("KOHERENSI", fontSize = 10.sp, color = TextSecondary)
                    if (gameState.lives <= 0) {
                        Text("Habis", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RoseDanger)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(gameState.lives) {
                                MriGlobalAssetImage(
                                    assetKey = MriGlobalAssetKey.SPARK_BOLT,
                                    modifier = Modifier.size(18.dp),
                                    tintAccent = EmeraldSuccess
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Nano Interaktif & Osiloskop Echo
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) {
                    haptics.lightToggle()
                    nanoTipTap++
                    customNanoTip = when {
                        gameState.isTurboMode -> "Nano: Turbo Spin Echo 2x menyala! Banyak gema 180° muncul berurutan!"
                        else -> "Nano: Ketuk tombol atau lajur tepat saat blok menyentuh cincin target di bawah!"
                    }
                }
                .semantics { contentDescription = "Osiloskop Echo dan Nano interaktif, ketuk untuk tips" },
            shape = RoundedCornerShape(14.dp),
            color = DarkNavySlate,
            border = BorderStroke(1.dp, CardSlateBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvatarView(
                    role = characterCustomization.avatarId,
                    equipped = characterCustomization,
                    expression = when {
                        gameState.isGameOver -> Expression.SEDIH
                        gameState.combo >= 4 -> Expression.BANGGA
                        customNanoTip != null -> Expression.BERPIKIR
                        else -> Expression.SENANG
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            scaleX = nanoBounce
                            scaleY = nanoBounce
                        }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customNanoTip ?: gameState.lastJudgment,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (customNanoTip != null) CyanBright else TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    ) {
                        val path = Path()
                        val steps = 40
                        for (i in 0..steps) {
                            val t = i / steps.toFloat()
                            val x = t * size.width
                            val env = 1f - kotlin.math.abs(t - 0.5f) * 1.9f
                            val y = size.height / 2f - sin(t * 7f * PI).toFloat() * (size.height * 0.45f) * env.coerceAtLeast(0.05f) * gameState.echoAmplitude
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = EmeraldSuccess, style = Stroke(width = 3f, cap = StrokeCap.Round))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Jalur Ritme 3-Kolom dengan latar panggung sekuens
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF071322))
                .border(1.5.dp, CardSlateBorder, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { tapOffset ->
                            if (!isPaused && !gameState.isGameOver) {
                                val laneIdx = ((tapOffset.x / size.width.coerceAtLeast(1)) * 3f).toInt().coerceIn(0, 2)
                                activeLaneFlash = laneIdx
                                val preferred = when (laneIdx) {
                                    0 -> PulseBeatKind.RF_90
                                    1 -> PulseBeatKind.REFOCUS_180
                                    else -> PulseBeatKind.ECHO_READ
                                }
                                val next = MriArcadeEngine.tapRhythmLane(gameState, preferred)
                                if (next.combo > gameState.combo) haptics.mediumAction() else haptics.warningBuzz()
                                gameState = next
                            }
                        }
                    )
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_arcade_rhythm_stage),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.40f },
                contentScale = ContentScale.Crop
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val laneW = size.width / 3f
                val targetY = size.height * 0.82f
                val laneColors = listOf(CyanBright, CharActionOrange, EmeraldSuccess)

                val kRows = 8
                val filledRows = ((gameState.score / 180) + (gameState.combo / 2)).coerceIn(1, kRows)
                for (r in 0 until kRows) {
                    val ry = size.height * 0.08f + r * (size.height * 0.65f / kRows)
                    val isFilled = r < filledRows
                    drawLine(
                        color = if (isFilled) CyanPrimary.copy(alpha = 0.16f) else CardSlateBorder.copy(alpha = 0.18f),
                        start = Offset(14.dp.toPx(), ry),
                        end = Offset(size.width - 14.dp.toPx(), ry),
                        strokeWidth = if (r == kRows / 2) 2.5f else 1.2f
                    )
                }

                activeLaneFlash?.let { flashLane ->
                    drawRect(
                        color = laneColors[flashLane].copy(alpha = 0.18f),
                        topLeft = Offset(laneW * flashLane, 0f),
                        size = Size(laneW, size.height)
                    )
                }

                for (i in 1..2) {
                    drawLine(
                        color = CardSlateBorder.copy(alpha = 0.7f),
                        start = Offset(laneW * i, 0f),
                        end = Offset(laneW * i, size.height),
                        strokeWidth = 2.5f
                    )
                }

                for (lane in 0..2) {
                    val cx = laneW * lane + laneW * 0.5f
                    val col = laneColors[lane]
                    val isFlashing = activeLaneFlash == lane
                    drawRoundRect(
                        color = col.copy(alpha = if (isFlashing) 0.36f else 0.16f),
                        topLeft = Offset(laneW * lane + 10.dp.toPx(), targetY - 22.dp.toPx()),
                        size = Size(laneW - 20.dp.toPx(), 44.dp.toPx()),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                    drawCircle(
                        color = col.copy(alpha = if (isFlashing) 0.95f else 0.65f),
                        radius = if (isFlashing) 19.dp.toPx() else 16.dp.toPx(),
                        center = Offset(cx, targetY),
                        style = Stroke(width = if (isFlashing) 4f else 3f)
                    )
                }

                drawLine(
                    color = CyanBright.copy(alpha = 0.75f),
                    start = Offset(10.dp.toPx(), targetY),
                    end = Offset(size.width - 10.dp.toPx(), targetY),
                    strokeWidth = 4f
                )

                gameState.notes.forEach { note ->
                    val y = note.progress * targetY
                    val (laneIdx, color) = when (note.kind) {
                        PulseBeatKind.RF_90 -> 0 to CyanBright
                        PulseBeatKind.REFOCUS_180 -> 1 to CharActionOrange
                        PulseBeatKind.ECHO_READ -> 2 to EmeraldSuccess
                    }
                    val noteLeft = laneW * laneIdx + 12.dp.toPx()
                    val noteW = laneW - 24.dp.toPx()
                    drawRoundRect(
                        color = color.copy(alpha = 0.42f),
                        topLeft = Offset(noteLeft, y - 12.dp.toPx()),
                        size = Size(noteW, 34.dp.toPx()),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(noteLeft, y - 16.dp.toPx()),
                        size = Size(noteW, 32.dp.toPx()),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.45f),
                        topLeft = Offset(noteLeft + 6.dp.toPx(), y - 12.dp.toPx()),
                        size = Size((noteW - 12.dp.toPx()).coerceAtLeast(4f), 6.dp.toPx()),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            val nextNote = gameState.notes.minByOrNull { kotlin.math.abs(it.progress - 1f) }
            if (nextNote != null && !gameState.isGameOver) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = DarkNavySlate,
                    border = BorderStroke(1.dp, CyanPrimary)
                ) {
                    Text(
                        text = "Target: ${nextNote.kind.label}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanBright,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (isPaused && !gameState.isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepObsidian.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            haptics.lightToggle()
                            isPaused = false
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepObsidian)
                    ) {
                        Text("Lanjutkan", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (gameState.isGameOver) {
                val displayBest = maxOf(bestScore, gameState.score)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepObsidian.copy(alpha = 0.9f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DarkNavySlate,
                        border = BorderStroke(1.5.dp, CyanBright)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MriGlobalAssetImage(
                                assetKey = MriGlobalAssetKey.TROPHY_MEDAL,
                                modifier = Modifier.size(44.dp),
                                tintAccent = AmberWarning
                            )
                            Text("Sekuens Ritme Selesai!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Text("Pangkat: ${gameState.rankTitle}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                            Text("Skor: ${gameState.score} · Rekor: $displayBest", fontSize = 14.sp, color = CyanBright, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = onBack,
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text("Hub Arcade", color = TextPrimary)
                                }
                                Button(
                                    onClick = {
                                        haptics.lightToggle()
                                        gameState = SpinEchoRhythmState()
                                        reported = false
                                        isPaused = false
                                    },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepObsidian)
                                ) {
                                    Text("Main Lagi", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(PulseBeatKind.RF_90, "90° RF", CyanPrimary),
                Triple(PulseBeatKind.REFOCUS_180, "180° Refocus", CharActionOrange),
                Triple(PulseBeatKind.ECHO_READ, "Baca Echo", EmeraldSuccess)
            ).forEachIndexed { idx, (kind, label, btnColor) ->
                Button(
                    onClick = {
                        if (!isPaused && !gameState.isGameOver) {
                            activeLaneFlash = idx
                            val next = MriArcadeEngine.tapRhythmLane(gameState, kind)
                            if (next.combo > gameState.combo) haptics.mediumAction() else haptics.warningBuzz()
                            gameState = next
                        }
                    },
                    enabled = !isPaused && !gameState.isGameOver,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = btnColor,
                        contentColor = DeepObsidian
                    )
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
