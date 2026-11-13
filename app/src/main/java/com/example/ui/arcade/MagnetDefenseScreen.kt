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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CharacterCustomizationState
import com.example.ui.character.AvatarView
import com.example.ui.character.Expression
import com.example.ui.components.MriGlobalAssetCatalog
import com.example.ui.components.MriGlobalAssetImage
import com.example.ui.components.MriGlobalAssetKey
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
import com.example.util.rememberMriHaptics
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
    val haptics = rememberMriHaptics()
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
    var nanoTipTap by remember { mutableIntStateOf(0) }
    var customNanoTip by remember { mutableStateOf<String?>(null) }

    val nanoBounce by animateFloatAsState(
        targetValue = if (nanoTipTap % 2 == 1) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "defense_nano_bounce"
    )

    LaunchedEffect(lastTapRipple) {
        if (lastTapRipple != null) {
            delay(220L)
            lastTapRipple = null
        }
    }

    LaunchedEffect(customNanoTip, nanoTipTap) {
        if (customNanoTip != null) {
            delay(2200L)
            customNanoTip = null
        }
    }

    LaunchedEffect(gameState.score, gameState.rankTitle) {
        gameStateScoreHolder = gameState.score
        gameStateRankHolder = gameState.rankTitle
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
            gameState = MriArcadeEngine.stepMagnetDefense(gameState, dt)
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
                onClick = {
                    haptics.lightToggle()
                    if (!gameState.isGameOver) isPaused = !isPaused
                },
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

        // HUD Skor, Combo, Perisai 5G + Tombol EMP saat Combo >= 3
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
                            haptics.completionBuzz()
                            val unsafeCount = gameState.items.count { it.category == DefenseItemCategory.MR_UNSAFE }
                            val bonus = (unsafeCount.coerceAtLeast(1)) * 150
                            gameState = gameState.copy(
                                items = gameState.items.filter { it.category != DefenseItemCategory.MR_UNSAFE },
                                score = gameState.score + bonus,
                                combo = 0,
                                deflectedCount = gameState.deflectedCount + unsafeCount,
                                lastEventMessage = "Gelombang Perisai Aktif! $unsafeCount benda feromagnetik disapu (+$bonus)!",
                                lastEventGood = true
                            )
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = DeepObsidian),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("EMP 5G", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PERISAI 5G", fontSize = 10.sp, color = TextSecondary)
                    if (gameState.lives <= 0) {
                        Text("Habis", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RoseDanger)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(gameState.lives) {
                                MriGlobalAssetImage(
                                    assetKey = MriGlobalAssetKey.SHIELD_5G,
                                    modifier = Modifier.size(18.dp),
                                    tintAccent = if (gameState.lives <= 1) RoseDanger else EmeraldSuccess
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Banner status & Nano interaktif
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) {
                    haptics.lightToggle()
                    nanoTipTap++
                    customNanoTip = when {
                        gameState.combo >= 3 -> "Nano: Combo x${gameState.combo} siap! Tekan EMP 5G untuk menyapu seluruh benda bahaya!"
                        gameState.wave >= 3 -> "Nano: Hati-hati! Di ${gameState.fieldLabel}, benda feromagnetik berbelok spiral!"
                        else -> "Nano: Ketuk benda merah (MR Unsafe); biarkan item hijau (MR Safe) masuk ke bore (+45)."
                    }
                }
                .semantics { contentDescription = "Status arena dan Nano interaktif, ketuk untuk strategi" },
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
                        text = customNanoTip ?: gameState.lastEventMessage,
                        fontSize = 12.sp,
                        color = if (customNanoTip != null) CyanBright else if (gameState.lastEventGood) TextPrimary else RoseDanger,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Ketuk Nano untuk strategi wave",
                        fontSize = 9.5.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Arena Polar 5 Gauss berlatar aset arena
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

            Image(
                painter = painterResource(id = R.drawable.img_arcade_magnet_arena),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.42f },
                contentScale = ContentScale.Crop
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                val r5G = minDimPx * MriArcadeEngine.FIVE_GAUSS_RADIUS

                drawLine(CyanPrimary.copy(alpha = 0.16f), Offset(0f, center.y), Offset(size.width, center.y), 1.5f)
                drawLine(CyanPrimary.copy(alpha = 0.16f), Offset(center.x, 0f), Offset(center.x, size.height), 1.5f)
                drawCircle(CyanPrimary.copy(alpha = 0.16f), minDimPx * 0.42f, center, style = Stroke(2f))
                drawCircle(CyanPrimary.copy(alpha = 0.24f), minDimPx * 0.29f, center, style = Stroke(2f))

                gameState.items.forEach { proj ->
                    val px = proj.x * size.width
                    val py = proj.y * size.height
                    val lineCol = if (proj.category == DefenseItemCategory.MR_UNSAFE) {
                        RoseDanger.copy(alpha = 0.40f)
                    } else {
                        EmeraldSuccess.copy(alpha = 0.28f)
                    }
                    drawLine(
                        color = lineCol,
                        start = Offset(px, py),
                        end = center,
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }

                val dashPhase = (gameState.elapsedSec * 45f) % 26f
                drawCircle(
                    color = RoseDanger.copy(alpha = 0.88f),
                    radius = r5G,
                    center = center,
                    style = Stroke(width = 4.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), dashPhase))
                )
                drawCircle(CyanPrimary.copy(alpha = 0.22f), r5G * 0.78f, center)
                drawCircle(CardSlate, r5G * 0.62f, center)
                drawCircle(CyanBright, r5G * 0.62f, center, style = Stroke(4f))

                lastTapRipple?.let { rippleCenter ->
                    drawCircle(
                        color = CyanBright.copy(alpha = 0.45f),
                        radius = 34.dp.toPx(),
                        center = rippleCenter,
                        style = Stroke(width = 3.5f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .androidxComposePointerTap { tapOffset ->
                        if (!isPaused && !gameState.isGameOver) {
                            lastTapRipple = tapOffset
                            val normX = (tapOffset.x / widthPx).coerceIn(0f, 1f)
                            val normY = (tapOffset.y / heightPx).coerceIn(0f, 1f)
                            val nextState = MriArcadeEngine.tapDefenseAtNormalized(
                                state = gameState,
                                normX = normX,
                                normY = normY,
                                hitRadius = 0.19f
                            )
                            if (nextState.deflectedCount > gameState.deflectedCount) {
                                haptics.mediumAction()
                            } else if (!nextState.lastEventGood && nextState !== gameState) {
                                haptics.warningBuzz()
                            } else {
                                haptics.lightToggle()
                            }
                            gameState = nextState
                        }
                    }
            )

            Text(
                text = "B₀",
                color = CyanBright,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )

            val itemSizePx = with(density) { 64.dp.toPx() }
            gameState.items.forEach { proj ->
                key(proj.id) {
                    val xPx = (proj.x * widthPx - itemSizePx / 2f).roundToInt()
                    val yPx = (proj.y * heightPx - itemSizePx / 2f).roundToInt()
                    val isUnsafe = proj.category == DefenseItemCategory.MR_UNSAFE
                    val assetKey = MriGlobalAssetCatalog.resolveKey(MriArcadeEngine.resolveAssetKey(proj))

                    Surface(
                        modifier = Modifier
                            .offset { IntOffset(xPx, yPx) }
                            .size(64.dp)
                            .clip(CircleShape)
                            .androidxComposePointerTap {
                                if (!isPaused && !gameState.isGameOver) {
                                    lastTapRipple = Offset(proj.x * widthPx, proj.y * heightPx)
                                    val nextState = MriArcadeEngine.tapDefenseProjectile(gameState, proj.id)
                                    if (isUnsafe) haptics.mediumAction() else haptics.warningBuzz()
                                    gameState = nextState
                                }
                            }
                            .clickable(role = Role.Button) {
                                if (!isPaused && !gameState.isGameOver) {
                                    lastTapRipple = Offset(proj.x * widthPx, proj.y * heightPx)
                                    val nextState = MriArcadeEngine.tapDefenseProjectile(gameState, proj.id)
                                    if (isUnsafe) haptics.mediumAction() else haptics.warningBuzz()
                                    gameState = nextState
                                }
                            }
                            .semantics {
                                contentDescription = "${proj.label}, ketuk untuk ${if (isUnsafe) "menangkis" else "memeriksa"}"
                            },
                        shape = CircleShape,
                        color = if (isUnsafe) RoseDanger.copy(alpha = 0.28f) else EmeraldSuccess.copy(alpha = 0.26f),
                        border = BorderStroke(2.4.dp, if (isUnsafe) RoseDanger else EmeraldSuccess),
                        shadowElevation = 6.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            MriGlobalAssetImage(
                                assetKey = assetKey,
                                modifier = Modifier.size(30.dp),
                                tintAccent = if (isUnsafe) RoseDanger else EmeraldSuccess
                            )
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
                                        haptics.lightToggle()
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
