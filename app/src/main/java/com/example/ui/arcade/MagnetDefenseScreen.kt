package com.example.ui.arcade

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CharacterCustomizationState
import com.example.ui.character.AvatarView
import com.example.ui.character.Expression
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DefenseItemCategory
import com.example.util.MagnetDefenseState
import com.example.util.MriArcadeEngine
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun MagnetDefenseScreen(
    characterCustomization: CharacterCustomizationState,
    bestScore: Int,
    onScoreUpdated: (score: Int, rankTitle: String) -> Unit = { _, _ -> },
    onGameFinished: (score: Int, rankTitle: String) -> Unit,
    onBack: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    BackHandler {
        if (gameStateScoreHolder > 0) {
            onScoreUpdated(gameStateScoreHolder, gameStateRankHolder)
        }
        onBack()
    }

    var gameState by remember { mutableStateOf(MagnetDefenseState()) }
    var isPaused by remember { mutableStateOf(false) }
    var reported by remember { mutableStateOf(false) }
    var lastTapRipple by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(lastTapRipple) {
        if (lastTapRipple != null) {
            delay(220L)
            lastTapRipple = null
        }
    }

    LaunchedEffect(gameState.score, gameState.rankTitle) {
        gameStateScoreHolder = gameState.score
        gameStateRankHolder = gameState.rankTitle
        if (gameState.score > 0) {
            onScoreUpdated(gameState.score, gameState.rankTitle)
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
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
            gameState = MriArcadeEngine.stepMagnetDefense(gameState, dt)
        }
    }

    LaunchedEffect(gameState.isGameOver) {
        if (gameState.isGameOver && !reported) {
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
                Text(
                    text = "Nano Magnet Defense",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Wave ${gameState.wave} · ${gameState.fieldLabel} · Rekor: ${maxOf(bestScore, gameState.score)}",
                    fontSize = 11.sp,
                    color = CyanBright,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(
                onClick = { if (!gameState.isGameOver) isPaused = !isPaused },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Lanjutkan Game" else "Jeda Game",
                    tint = CyanBright
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // HUD Skor, Combo, Nyawa + Tombol EMP Perisai saat Combo >= 3
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
                if (gameState.combo >= 3 && !gameState.isGameOver) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            val unsafeCount = gameState.items.count { it.category == DefenseItemCategory.MR_UNSAFE }
                            val bonus = (unsafeCount.coerceAtLeast(1)) * 150
                            gameState = gameState.copy(
                                items = gameState.items.filter { it.category != DefenseItemCategory.MR_UNSAFE },
                                score = gameState.score + bonus,
                                combo = 0,
                                deflectedCount = gameState.deflectedCount + unsafeCount,
                                lastEventMessage = "⚡ Gelombang Perisai Aktif! $unsafeCount benda feromagnetik disapu (+$bonus)!",
                                lastEventGood = true
                            )
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = DeepObsidian),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("⚡ EMP 5G", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PERISAI 5G", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "🛡️".repeat(gameState.lives.coerceAtLeast(0)).ifBlank { "Habis" },
                        fontSize = 15.sp,
                        color = if (gameState.lives <= 1) RoseDanger else EmeraldSuccess
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Banner status & Nano reaktif
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = DarkNavySlate,
            border = BorderStroke(
                1.dp,
                if (gameState.lastEventGood) CyanPrimary.copy(alpha = 0.5f) else RoseDanger.copy(alpha = 0.7f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvatarView(
                    role = characterCustomization.avatarId,
                    equipped = characterCustomization,
                    expression = when {
                        gameState.isGameOver -> Expression.SEDIH
                        !gameState.lastEventGood -> Expression.WASPADA
                        gameState.combo >= 4 -> Expression.BANGGA
                        else -> Expression.SENANG
                    },
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = gameState.lastEventMessage,
                    fontSize = 12.sp,
                    color = if (gameState.lastEventGood) TextPrimary else RoseDanger,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Arena Polar 5 Gauss — responsif terhadap sentuhan langsung (finger-down)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF071322))
                .border(1.5.dp, CardSlateBorder, RoundedCornerShape(22.dp))
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
            val minDimPx = minOf(widthPx, heightPx)

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                val r5G = minDimPx * MriArcadeEngine.FIVE_GAUSS_RADIUS

                // Garis sumbu radar Zona III / IV
                drawLine(
                    color = CyanPrimary.copy(alpha = 0.14f),
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = CyanPrimary.copy(alpha = 0.14f),
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, size.height),
                    strokeWidth = 1.5f
                )

                // Ring isogauss luar (Zona III: 0.5 mT / 5 Gauss -> Bore)
                drawCircle(
                    color = CyanPrimary.copy(alpha = 0.14f),
                    radius = minDimPx * 0.42f,
                    center = center,
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = CyanPrimary.copy(alpha = 0.22f),
                    radius = minDimPx * 0.29f,
                    center = center,
                    style = Stroke(width = 2f)
                )

                // Vektor tarikan magnetik dari tiap proyektil ke pusat bore B0
                gameState.items.forEach { proj ->
                    val px = proj.x * size.width
                    val py = proj.y * size.height
                    val lineCol = if (proj.category == DefenseItemCategory.MR_UNSAFE) {
                        RoseDanger.copy(alpha = 0.35f)
                    } else {
                        EmeraldSuccess.copy(alpha = 0.25f)
                    }
                    drawLine(
                        color = lineCol,
                        start = Offset(px, py),
                        end = center,
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }

                // Garis batas 5 Gauss putus-putus berputar
                val dashPhase = (gameState.elapsedSec * 45f) % 26f
                drawCircle(
                    color = RoseDanger.copy(alpha = 0.88f),
                    radius = r5G,
                    center = center,
                    style = Stroke(
                        width = 4.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), dashPhase)
                    )
                )
                // Gantry & Bore Magnet B0 berlapis di pusat
                drawCircle(
                    color = CyanPrimary.copy(alpha = 0.18f),
                    radius = r5G * 0.78f,
                    center = center
                )
                drawCircle(
                    color = CardSlate,
                    radius = r5G * 0.62f,
                    center = center
                )
                drawCircle(
                    color = CyanBright,
                    radius = r5G * 0.62f,
                    center = center,
                    style = Stroke(width = 4f)
                )
                // Efek gelombang sentuhan instan
                lastTapRipple?.let { rippleCenter ->
                    drawCircle(
                        color = CyanBright.copy(alpha = 0.45f),
                        radius = 34.dp.toPx(),
                        center = rippleCenter,
                        style = Stroke(width = 3.5f)
                    )
                }
            }

            // Lapisan tangkap tap arena secara instan (radius toleransi luas)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .androidxComposePointerTap { tapOffset ->
                        if (!isPaused && !gameState.isGameOver) {
                            lastTapRipple = tapOffset
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            val normX = (tapOffset.x / widthPx).coerceIn(0f, 1f)
                            val normY = (tapOffset.y / heightPx).coerceIn(0f, 1f)
                            gameState = MriArcadeEngine.tapDefenseAtNormalized(
                                state = gameState,
                                normX = normX,
                                normY = normY,
                                hitRadius = 0.19f
                            )
                        }
                    }
            )

            // Label Bore di tengah
            Text(
                text = "B₀",
                color = CyanBright,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )

            // Render proyektil interaktif (>= 62.dp + key(proj.id) agar responsif saat bergerak)
            val itemSizePx = with(density) { 62.dp.toPx() }
            gameState.items.forEach { proj ->
                androidx.compose.runtime.key(proj.id) {
                    val xPx = (proj.x * widthPx - itemSizePx / 2f).roundToInt()
                    val yPx = (proj.y * heightPx - itemSizePx / 2f).roundToInt()
                    val isUnsafe = proj.category == DefenseItemCategory.MR_UNSAFE

                    Surface(
                        modifier = Modifier
                            .offset { IntOffset(xPx, yPx) }
                            .size(62.dp)
                            .clip(CircleShape)
                            .androidxComposePointerTap {
                                if (!isPaused && !gameState.isGameOver) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    lastTapRipple = Offset(proj.x * widthPx, proj.y * heightPx)
                                    gameState = MriArcadeEngine.tapDefenseProjectile(gameState, proj.id)
                                }
                            }
                            .clickable(role = Role.Button) {
                                if (!isPaused && !gameState.isGameOver) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    lastTapRipple = Offset(proj.x * widthPx, proj.y * heightPx)
                                    gameState = MriArcadeEngine.tapDefenseProjectile(gameState, proj.id)
                                }
                            }
                            .semantics {
                                contentDescription = "${proj.label}, ketuk untuk ${if (isUnsafe) "menangkis" else "memeriksa"}"
                            },
                        shape = CircleShape,
                        color = if (isUnsafe) RoseDanger.copy(alpha = 0.3f) else EmeraldSuccess.copy(alpha = 0.28f),
                        border = BorderStroke(2.4.dp, if (isUnsafe) RoseDanger else EmeraldSuccess),
                        shadowElevation = 6.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = proj.emoji, fontSize = 23.sp)
                            Text(
                                text = if (isUnsafe) "BAHAYA" else "AMAN",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isUnsafe) RoseDanger else EmeraldSuccess
                            )
                        }
                    }
                }
            }

            if (isPaused && !gameState.isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepObsidian.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Permainan Dijeda", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Button(
                            onClick = { isPaused = false },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepObsidian)
                        ) {
                            Text("Lanjutkan", fontWeight = FontWeight.Bold)
                        }
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
                            Text("Shift Pertahanan Selesai!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Text(
                                text = "Pangkat: ${gameState.rankTitle}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                            Text("Skor: ${gameState.score} · Rekor: $displayBest", fontSize = 14.sp, color = CyanBright, fontWeight = FontWeight.SemiBold)
                            Text("Benda Feromagnetik Ditangkis: ${gameState.deflectedCount}", fontSize = 12.sp, color = TextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = onBack,
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text("Hub Arcade", color = TextPrimary)
                                }
                                Button(
                                    onClick = {
                                        gameState = MagnetDefenseState()
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
    }
}

private var gameStateScoreHolder: Int = 0
private var gameStateRankHolder: String = "Magang Zona II"

private fun Modifier.androidxComposePointerTap(onDown: (Offset) -> Unit): Modifier =
    this.pointerInput(Unit) {
        detectTapGestures(
            onPress = { offset ->
                onDown(offset)
            }
        )
    }


