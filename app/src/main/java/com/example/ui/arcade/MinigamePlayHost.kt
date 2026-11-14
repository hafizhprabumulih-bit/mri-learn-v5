package com.example.ui.arcade

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.CharAccentMint
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
import com.example.util.rememberMriHaptics
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun MinigamePlayHost(
    kind: MinigameKind,
    bestScore: Int,
    characterCustomization: CharacterCustomizationState,
    onScoreUpdated: (Int) -> Unit = {},
    onRoundFinished: (Int) -> Unit,
    onBack: () -> Unit
) {
    val haptics = rememberMriHaptics()
    var roundSeed by remember(kind) { mutableIntStateOf(1) }
    var timeLeft by remember(kind) { mutableIntStateOf(kind.durationSec) }
    var score by remember(kind) { mutableIntStateOf(0) }
    var combo by remember(kind) { mutableIntStateOf(0) }
    var totalAnswered by remember(kind) { mutableIntStateOf(0) }
    var totalCorrect by remember(kind) { mutableIntStateOf(0) }
    var itemIdx by remember(kind) { mutableIntStateOf(0) }
    var feedbackMsg by remember(kind) {
        mutableStateOf("Ketuk cepat & tepat! Pertahankan combo untuk mengaktifkan Overdrive 2x.")
    }
    var feedbackGood by remember(kind) { mutableStateOf(true) }
    var roundDone by remember(kind) { mutableStateOf(false) }

    // Interaksi Nano langsung di dalam minigame
    var nanoTipTap by remember(kind) { mutableIntStateOf(0) }
    var nanoOverrideExpression by remember(kind) { mutableStateOf<Expression?>(null) }

    val shuffledScreening = remember(kind, roundSeed) {
        MriMinigameEngine.buildNonRepeatingDeck(MriMinigameEngine.screeningBank)
    }
    val shuffledLarmor = remember(kind, roundSeed) {
        MriMinigameEngine.buildNonRepeatingDeck(MriMinigameEngine.larmorChallenges)
    }
    val shuffledArtifacts = remember(kind, roundSeed) {
        MriMinigameEngine.buildNonRepeatingDeck(MriMinigameEngine.contrastArtifactBank)
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
    val laserPhase by waveAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laserPhase"
    )

    // Umpan balik visual instan saat mengetuk / menyaring benda
    var actionBannerText by remember(kind) { mutableStateOf<String?>(null) }
    var actionBannerGood by remember(kind) { mutableStateOf(true) }
    var lastTapNormX by remember(kind) { mutableStateOf<Float?>(null) }

    LaunchedEffect(actionBannerText) {
        if (actionBannerText != null) {
            delay(720L)
            actionBannerText = null
            lastTapNormX = null
        }
    }

    // Fase jarum Larmor kontinu berbasis frame-delta agar bebas loncatan saat combo naik
    var larmorPhase by remember(kind) { mutableFloatStateOf(0f) }
    val currentLarmorChallenge = shuffledLarmor[itemIdx % shuffledLarmor.size]
    if (kind == MinigameKind.LARMOR_TAP && !roundDone) {
        LaunchedEffect(kind, roundDone, currentLarmorChallenge.id, combo) {
            var lastNs = 0L
            while (!roundDone) {
                withFrameNanos { frameNs ->
                    if (lastNs != 0L) {
                        val dt = (frameNs - lastNs) / 1_000_000_000f
                        larmorPhase = MriMinigameEngine.advanceLarmorPhase(
                            currentPhase = larmorPhase,
                            deltaSec = dt,
                            speedMultiplier = currentLarmorChallenge.speedMultiplier,
                            combo = combo
                        )
                    }
                    lastNs = frameNs
                }
            }
        }
    }
    val needle = MriMinigameEngine.needleFromPhase(larmorPhase)

    val nanoBounce by animateFloatAsState(
        targetValue = if (nanoTipTap % 2 == 1) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "minigame_nano_bounce"
    )

    LaunchedEffect(nanoOverrideExpression, nanoTipTap) {
        if (nanoOverrideExpression != null) {
            delay(1600L)
            nanoOverrideExpression = null
        }
    }

    LaunchedEffect(score) {
        if (score > 0) onScoreUpdated(score)
    }

    DisposableEffect(kind) {
        onDispose {
            if (score > 0 && !roundDone) onRoundFinished(score)
        }
    }

    LaunchedEffect(kind, roundDone, roundSeed) {
        if (roundDone) return@LaunchedEffect
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        haptics.completionBuzz()
        roundDone = true
        onRoundFinished(score)
    }

    val scrollModifier = if (kind == MinigameKind.CONTRAST_ARTIFACT_FIX || roundDone) {
        Modifier.verticalScroll(rememberScrollState())
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .then(scrollModifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    haptics.lightToggle()
                    onBack()
                },
                modifier = Modifier.size(48.dp)
            ) {
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
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (combo >= 3) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (combo >= 3) "OVERDRIVE x$combo" else "Combo: x$combo",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (combo >= 3) AmberWarning else TextSecondary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MriGlobalAssetImage(
                    assetKey = MriGlobalAssetKey.TROPHY_MEDAL,
                    modifier = Modifier.size(14.dp),
                    tintAccent = EmeraldSuccess
                )
                Text("Rekor: ${maxOf(bestScore, score)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Nano pendamping minigame yang interaktif saat diketuk
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) {
                    haptics.lightToggle()
                    nanoTipTap++
                    nanoOverrideExpression = if (nanoTipTap % 2 == 0) Expression.BANGGA else Expression.BERPIKIR
                    feedbackGood = true
                    feedbackMsg = when (kind) {
                        MinigameKind.ZONE_SCREENING -> "Tips Nano: Benda MR Safe/Conditional terverifikasi boleh lolos; feromagnetik WAJIB ditolak di pintu Zona IV!"
                        MinigameKind.LARMOR_TAP -> "Tips Nano: Amati laju jarum di monitor osiloskop lalu tekan tembak saat berada di zona hijau!"
                        MinigameKind.CONTRAST_ARTIFACT_FIX -> "Tips Nano: Perhatikan warna CSF & arah phase-encoding untuk mengenali T1w, T2w, FLAIR, atau artefak!"
                    }
                }
                .semantics { contentDescription = "Nano pendamping minigame, ketuk untuk petunjuk" },
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
                    expression = nanoOverrideExpression ?: when {
                        roundDone -> Expression.BANGGA
                        !feedbackGood -> Expression.WASPADA
                        combo >= 3 -> Expression.BANGGA
                        totalAnswered == 0 -> Expression.BERPIKIR
                        else -> Expression.SENANG
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = nanoBounce
                            scaleY = nanoBounce
                        }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = feedbackMsg,
                        fontSize = 11.8.sp,
                        color = TextPrimary,
                        lineHeight = 15.5.sp,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ketuk Nano untuk tips cepat",
                        fontSize = 9.5.sp,
                        color = CyanBright.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
                    MriGlobalAssetImage(
                        assetKey = MriGlobalAssetKey.TROPHY_MEDAL,
                        modifier = Modifier.size(48.dp),
                        tintAccent = AmberWarning
                    )
                    Text("Ronde Selesai!", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text(rank, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                    Text("Skor: $score · Akurasi: $acc% · Rekor: $displayBest", fontSize = 13.sp, color = CyanBright)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onBack, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                            Text("Kembali", color = TextPrimary)
                        }
                        Button(
                            onClick = {
                                haptics.lightToggle()
                                roundSeed++
                                timeLeft = kind.durationSec
                                score = 0
                                combo = 0
                                totalAnswered = 0
                                totalCorrect = 0
                                itemIdx = 0
                                larmorPhase = 0f
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
                    val itemPulse by animateFloatAsState(
                        targetValue = if (itemIdx % 2 == 1) 1.04f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "screening_item_pulse"
                    )
                    val resolveScreening: (Boolean) -> Unit = { allowChoice ->
                        val ok = (allowChoice == item.allowIntoZone4)
                        if (ok) haptics.mediumAction() else haptics.warningBuzz()
                        totalAnswered++
                        val earned = if (ok) {
                            totalCorrect++
                            combo++
                            val mult = if (combo >= 3) 20 else 0
                            val pts = MriMinigameEngine.computePoints(100 + mult, combo)
                            score += pts
                            if (timeLeft < kind.durationSec && combo % 4 == 0) timeLeft += 2
                            pts
                        } else {
                            combo = 0
                            0
                        }
                        feedbackGood = ok
                        actionBannerGood = ok
                        actionBannerText = if (ok) {
                            (if (allowChoice) "LOLOS!" else "DITOLAK!") + " +$earned"
                        } else {
                            if (item.allowIntoZone4) "SEHARUSNYA LOLOS!" else "BAHAYA PROYEKTIL!"
                        }
                        feedbackMsg = (if (ok) "Tepat! " else "Kurang tepat! ") + item.reason
                        itemIdx++
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = DarkNavySlate,
                            border = BorderStroke(
                                1.5.dp,
                                when {
                                    actionBannerText != null && actionBannerGood -> EmeraldSuccess
                                    actionBannerText != null && !actionBannerGood -> RoseDanger
                                    else -> CardSlateBorder
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Portal pintu detektor feromagnetik lebar dengan garis laser terkalibrasi penuh di dalam bingkai pintu
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 200.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF071322))
                                        .border(1.2.dp, CyanPrimary.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_arcade_screening_gate),
                                        contentDescription = "Portal Detektor Zona IV",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = 1.54f
                                                scaleY = 1.38f
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0x8A071322), Color(0x52071322), Color(0x9E071322))
                                                )
                                            )
                                    )
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.width
                                        val h = size.height
                                        val doorLeft = w * 0.055f
                                        val doorRight = w * 0.945f
                                        val doorTop = h * 0.06f
                                        val doorBottom = h * 0.93f
                                        val doorW = (doorRight - doorLeft).coerceAtLeast(1f)
                                        val doorH = (doorBottom - doorTop).coerceAtLeast(1f)

                                        // Bingkai pintu detektor feromagnetik lebar penuh
                                        drawRoundRect(
                                            color = Color(0xCC0C213B),
                                            topLeft = Offset(doorLeft, doorTop),
                                            size = Size(doorW, doorH),
                                            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                                            style = Stroke(width = 7.dp.toPx())
                                        )
                                        drawRoundRect(
                                            color = CyanPrimary.copy(alpha = 0.72f),
                                            topLeft = Offset(doorLeft + 4.dp.toPx(), doorTop + 4.dp.toPx()),
                                            size = Size(doorW - 8.dp.toPx(), doorH - 8.dp.toPx()),
                                            cornerRadius = CornerRadius(11.dp.toPx(), 11.dp.toPx()),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                        val topLedColor = if (combo >= 3) AmberWarning else CyanBright
                                        drawRoundRect(
                                            color = topLedColor.copy(alpha = 0.9f),
                                            topLeft = Offset(w * 0.36f, doorTop + 1.dp.toPx()),
                                            size = Size(w * 0.28f, 4.dp.toPx()),
                                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                        )

                                        // Garis pemindaian laser terkalibrasi ketat di dalam bukaan pintu
                                        val pad = 10.dp.toPx()
                                        val scanLeft = doorLeft + pad
                                        val scanRight = doorRight - pad
                                        val scanTop = doorTop + pad
                                        val scanBottom = doorBottom - pad
                                        val pingPong = 1f - abs((laserPhase * 2f) - 1f)
                                        val scanX = scanLeft + (scanRight - scanLeft) * pingPong
                                        val laserColor = if (combo >= 3) AmberWarning else CyanBright

                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color.Transparent, laserColor.copy(alpha = 0.26f), Color.Transparent),
                                                startX = (scanX - 24.dp.toPx()).coerceAtLeast(scanLeft),
                                                endX = (scanX + 24.dp.toPx()).coerceAtMost(scanRight)
                                            ),
                                            topLeft = Offset((scanX - 24.dp.toPx()).coerceAtLeast(scanLeft), scanTop),
                                            size = Size(48.dp.toPx().coerceAtMost(scanRight - scanLeft), scanBottom - scanTop)
                                        )
                                        drawLine(
                                            color = laserColor,
                                            start = Offset(scanX, scanTop),
                                            end = Offset(scanX, scanBottom),
                                            strokeWidth = 3.2.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                        val scanY = scanTop + (scanBottom - scanTop) * pingPong
                                        drawLine(
                                            color = laserColor.copy(alpha = 0.42f),
                                            start = Offset(scanLeft, scanY),
                                            end = Offset(scanRight, scanY),
                                            strokeWidth = 1.6.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }

                                    // Objek yang sedang diskrining di tengah portal lebar
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xDB071322),
                                        border = BorderStroke(2.2.dp, CyanBright.copy(alpha = 0.85f)),
                                        modifier = Modifier
                                            .size(104.dp)
                                            .graphicsLayer {
                                                scaleX = itemPulse
                                                scaleY = itemPulse
                                            }
                                    ) {
                                        MriGlobalAssetImage(
                                            assetKey = MriGlobalAssetCatalog.resolveKey(item.assetKey.ifBlank { item.name }),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(14.dp),
                                            tintAccent = CyanBright
                                        )
                                    }

                                    actionBannerText?.let { banner ->
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = 12.dp),
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (actionBannerGood) EmeraldSuccess else RoseDanger,
                                            shadowElevation = 6.dp
                                        ) {
                                            Text(
                                                text = banner,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (actionBannerGood) DeepObsidian else Color.White,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = item.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = CyanPrimary.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "Portal Detektor Feromagnetik · Pintu Zona II → IV (5 Gauss)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanBright,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { resolveScreening(false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoseDanger, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("TOLAK / LEPAS", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Button(
                                onClick = { resolveScreening(true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = DeepObsidian)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("AMAN / LOLOS", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                MinigameKind.LARMOR_TAP -> {
                    val challenge = currentLarmorChallenge
                    val shootLarmor: () -> Unit = {
                        val acc = MriMinigameEngine.evaluateLarmorTap(needle, challenge.targetNormalized)
                        if (acc != TapAccuracy.MISS) haptics.mediumAction() else haptics.warningBuzz()
                        totalAnswered++
                        lastTapNormX = needle
                        val pts = if (acc != TapAccuracy.MISS) {
                            totalCorrect++
                            combo++
                            val p = MriMinigameEngine.computePoints(acc.basePoints, combo)
                            score += p
                            feedbackGood = true
                            actionBannerGood = true
                            actionBannerText = "${acc.label.uppercase()} +$p"
                            p
                        } else {
                            combo = 0
                            feedbackGood = false
                            actionBannerGood = false
                            actionBannerText = "MELESET!"
                            0
                        }
                        feedbackMsg = "${acc.label} pada ${challenge.targetMhz} (${challenge.fieldStrengthTesla})" +
                            if (pts > 0) " · +$pts poin!" else " · Bidik zona hijau resonansi!"
                        itemIdx++
                    }

                    val sweepFactor = ((challenge.speedMultiplier / 1.75f) + combo.coerceIn(0, 6) * 0.045f).coerceIn(0.25f, 1f)
                    val snrMarginFactor = (1f - ((challenge.speedMultiplier - 0.70f) * 0.42f) - combo.coerceIn(0, 6) * 0.055f).coerceIn(0.22f, 0.92f)
                    val rbwKhz = 32 + (combo.coerceIn(0, 6) * 8)
                    val comboBonusPts = (combo.coerceIn(0, 6) * 20).coerceAtMost(120)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        color = DarkNavySlate,
                        border = BorderStroke(
                            1.5.dp,
                            when {
                                actionBannerText != null && actionBannerGood -> EmeraldSuccess
                                actionBannerText != null && !actionBannerGood -> RoseDanger
                                else -> CardSlateBorder
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            val speedTierLabel = when {
                                combo >= 6 -> "Tingkat 3 · Kilat (Overdrive)"
                                combo >= 3 -> "Tingkat 2 · Cepat"
                                else -> "Tingkat 1 · Santai"
                            }
                            val speedTierColor = when {
                                combo >= 6 -> EmeraldSuccess
                                combo >= 3 -> AmberWarning
                                else -> CyanBright
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Medan B₀: ${challenge.fieldStrengthTesla}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyanBright,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = EmeraldSuccess.copy(alpha = 0.18f),
                                        border = BorderStroke(1.2.dp, EmeraldSuccess)
                                    ) {
                                        Text(
                                            text = "ω₀ = ${challenge.targetMhz}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmeraldSuccess,
                                            softWrap = false,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Formula: ω₀ = γ · B₀ (γ = 42,58 MHz/T untuk ¹H)",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = speedTierColor.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, speedTierColor.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = speedTierLabel,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = speedTierColor,
                                            softWrap = false,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Monitor osiloskop persegi panjang lebar; HANYA tombol RF di bawah yang menembak pulsa
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2.2f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF071322))
                                    .border(1.5.dp, CyanPrimary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                    .semantics { contentDescription = "Layar monitor osiloskop frekuensi Larmor" },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_larmor_monitor),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = 1.48f
                                            scaleY = 1.24f
                                        },
                                    contentScale = ContentScale.Crop
                                )
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val bezelLeft = w * 0.025f
                                    val bezelTop = h * 0.035f
                                    val bezelW = w * 0.95f
                                    val bezelH = h * 0.93f
                                    drawRoundRect(
                                        color = Color(0xD9102A43),
                                        topLeft = Offset(bezelLeft, bezelTop),
                                        size = Size(bezelW, bezelH),
                                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                                    )
                                    drawRoundRect(
                                        color = CyanPrimary.copy(alpha = 0.6f),
                                        topLeft = Offset(bezelLeft, bezelTop),
                                        size = Size(bezelW, bezelH),
                                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                                        style = Stroke(width = 2.dp.toPx())
                                    )

                                    val screenLeft = w * 0.055f
                                    val screenRight = w * 0.945f
                                    val screenTop = h * 0.075f
                                    val screenBottom = h * 0.76f
                                    val screenW = (screenRight - screenLeft).coerceAtLeast(1f)
                                    val screenH = (screenBottom - screenTop).coerceAtLeast(1f)

                                    drawRoundRect(
                                        color = Color(0xFA061221),
                                        topLeft = Offset(screenLeft, screenTop),
                                        size = Size(screenW, screenH),
                                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                    )
                                    drawRoundRect(
                                        color = CyanBright.copy(alpha = 0.35f),
                                        topLeft = Offset(screenLeft, screenTop),
                                        size = Size(screenW, screenH),
                                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )

                                    for (g in 1..4) {
                                        val gy = screenTop + screenH * (g / 5f)
                                        drawLine(
                                            color = CyanPrimary.copy(alpha = 0.16f),
                                            start = Offset(screenLeft + 6f, gy),
                                            end = Offset(screenRight - 6f, gy),
                                            strokeWidth = 1.2f
                                        )
                                    }
                                    for (g in 1..7) {
                                        val gx = screenLeft + screenW * (g / 8f)
                                        drawLine(
                                            color = CyanPrimary.copy(alpha = 0.14f),
                                            start = Offset(gx, screenTop + 6f),
                                            end = Offset(gx, screenBottom - 6f),
                                            strokeWidth = 1.2f
                                        )
                                    }

                                    val padX = 12.dp.toPx()
                                    val usableW = (screenW - padX * 2f).coerceAtLeast(1f)
                                    val targetX = screenLeft + padX + challenge.targetNormalized.coerceIn(0.08f, 0.92f) * usableW
                                    val goodHalfW = usableW * 0.135f
                                    val perfHalfW = usableW * 0.06f
                                    drawRoundRect(
                                        color = CyanBright.copy(alpha = 0.22f),
                                        topLeft = Offset((targetX - goodHalfW).coerceAtLeast(screenLeft + 4f), screenTop + 8f),
                                        size = Size(goodHalfW * 2f, screenH - 16f),
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )
                                    drawRoundRect(
                                        color = EmeraldSuccess.copy(alpha = 0.56f),
                                        topLeft = Offset((targetX - perfHalfW).coerceAtLeast(screenLeft + 5f), screenTop + 6f),
                                        size = Size(perfHalfW * 2f, screenH - 12f),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )
                                    drawLine(
                                        color = EmeraldSuccess,
                                        start = Offset(targetX, screenTop + 6f),
                                        end = Offset(targetX, screenBottom - 6f),
                                        strokeWidth = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                    )

                                    val wavePath = Path()
                                    val steps = 80
                                    val centerY = screenTop + screenH * 0.50f
                                    for (i in 0..steps) {
                                        val t = i / steps.toFloat()
                                        val x = screenLeft + padX + t * usableW
                                        val distToTarget = abs(t - challenge.targetNormalized)
                                        val env = (1f - distToTarget * 2.0f).coerceIn(0.18f, 1f)
                                        val y = (centerY - sin((t * 10f + wavePhase * 2f) * PI).toFloat() * (screenH * 0.36f) * env)
                                            .coerceIn(screenTop + 8f, screenBottom - 8f)
                                        if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                                    }
                                    drawPath(
                                        wavePath,
                                        color = CyanBright,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    lastTapNormX?.let { tapNorm ->
                                        val tx = (screenLeft + padX + tapNorm.coerceIn(0f, 1f) * usableW)
                                            .coerceIn(screenLeft + 8f, screenRight - 8f)
                                        drawCircle(
                                            color = if (actionBannerGood) EmeraldSuccess else RoseDanger,
                                            radius = 15.dp.toPx(),
                                            center = Offset(tx, centerY),
                                            style = Stroke(width = 2.8.dp.toPx())
                                        )
                                    }

                                    val needleX = (screenLeft + padX + needle.coerceIn(0f, 1f) * usableW)
                                        .coerceIn(screenLeft + 6f, screenRight - 6f)
                                    drawLine(
                                        color = AmberWarning,
                                        start = Offset(needleX, screenTop + 6f),
                                        end = Offset(needleX, screenBottom - 6f),
                                        strokeWidth = 4.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5.dp.toPx(),
                                        center = Offset(needleX, centerY)
                                    )

                                    // Panel dial bawah di dalam bezel monitor
                                    val dialY = h * 0.875f
                                    listOf(0.16f, 0.28f, 0.40f).forEachIndexed { i, rx ->
                                        val cx = w * rx
                                        drawCircle(Color(0xFF1F5D8C), radius = 9.5.dp.toPx(), center = Offset(cx, dialY))
                                        drawCircle(CyanBright.copy(alpha = 0.65f), radius = 9.5.dp.toPx(), center = Offset(cx, dialY), style = Stroke(1.6.dp.toPx()))
                                        val angle = (wavePhase * 360f + i * 75f) * (PI.toFloat() / 180f)
                                        drawLine(
                                            Color.White,
                                            Offset(cx, dialY),
                                            Offset(cx + cos(angle) * 6.dp.toPx(), dialY + sin(angle) * 6.dp.toPx()),
                                            1.8.dp.toPx(),
                                            StrokeCap.Round
                                        )
                                    }
                                    drawRoundRect(
                                        color = AmberWarning.copy(alpha = 0.85f),
                                        topLeft = Offset(w * 0.66f, dialY - 7.5.dp.toPx()),
                                        size = Size(w * 0.24f, 15.dp.toPx()),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }

                                actionBannerText?.let { banner ->
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp),
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (actionBannerGood) EmeraldSuccess else RoseDanger,
                                        shadowElevation = 6.dp
                                    ) {
                                        Text(
                                            text = banner,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (actionBannerGood) DeepObsidian else Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Panel Visualisasi Komputer Konsol MRI & Trade-off Parameter RF
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF071322),
                                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                            MriGlobalAssetImage(
                                                assetKey = MriGlobalAssetKey.PULSE_WAVE,
                                                modifier = Modifier.size(16.dp),
                                                tintAccent = CyanBright
                                            )
                                            Text(
                                                text = "KONSOL RF · KOMPUTER MRI",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = CyanBright
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (combo >= 3) AmberWarning.copy(alpha = 0.18f) else CardSlate
                                        ) {
                                            Text(
                                                text = if (combo >= 3) "OVERDRIVE +$comboBonusPts pt" else "BONUS +$comboBonusPts pt",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (combo >= 3) AmberWarning else TextSecondary,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    val b0Compact = challenge.fieldStrengthTesla.substringBefore(" ").let { "$it T" }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        LarmorConsoleMetricChip(
                                            label = "B₀ MEDAN",
                                            value = b0Compact,
                                            accent = CyanBright,
                                            modifier = Modifier.weight(1f)
                                        )
                                        LarmorConsoleMetricChip(
                                            label = "PULSA RF",
                                            value = "90° B₁",
                                            accent = EmeraldSuccess,
                                            modifier = Modifier.weight(1f)
                                        )
                                        LarmorConsoleMetricChip(
                                            label = "RECEIVE BW",
                                            value = "±$rbwKhz kHz",
                                            accent = AmberWarning,
                                            modifier = Modifier.weight(1f)
                                        )
                                        LarmorConsoleMetricChip(
                                            label = "LAJU SAPU",
                                            value = "${(challenge.speedMultiplier * (1f + combo.coerceIn(0, 8) * 0.10f)).let { (it * 10f).toInt() / 10f }}x",
                                            accent = CyanBright,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        LarmorTradeoffBar(
                                            label = "Sapuan & Presisi Waktu",
                                            progress = sweepFactor,
                                            color = AmberWarning
                                        )
                                        LarmorTradeoffBar(
                                            label = "Margin SNR (FID)",
                                            progress = snrMarginFactor,
                                            color = EmeraldSuccess
                                        )
                                    }
                                    Text(
                                        text = "Fisika: B₀ naik → SNR naik linear (~B₀), SAR naik kuadratik (~B₀²)",
                                        fontSize = 9.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = shootLarmor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepObsidian),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("TEMBAK PULSA RF 90°!", fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold)
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
                        border = BorderStroke(
                            1.3.dp,
                            when {
                                actionBannerText != null && actionBannerGood -> EmeraldSuccess
                                actionBannerText != null && !actionBannerGood -> RoseDanger
                                else -> CardSlateBorder
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .size(104.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black)
                                        .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                ) {
                                    drawContrastArtifactThumbnail(q.id, wavePhase)
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
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
                                        actionBannerText?.let { banner ->
                                            Text(
                                                text = banner,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (actionBannerGood) EmeraldSuccess else RoseDanger
                                            )
                                        }
                                    }
                                    Text(
                                        text = q.prompt,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        lineHeight = 18.5.sp
                                    )
                                }
                            }

                            shuffledOptions.first.forEachIndexed { optIdx, label ->
                                OutlinedButton(
                                    onClick = {
                                        val ok = optIdx == shuffledOptions.second
                                        if (ok) haptics.mediumAction() else haptics.warningBuzz()
                                        totalAnswered++
                                        val pts = if (ok) {
                                            totalCorrect++
                                            combo++
                                            val p = MriMinigameEngine.computePoints(110, combo)
                                            score += p
                                            p
                                        } else {
                                            combo = 0
                                            0
                                        }
                                        feedbackGood = ok
                                        actionBannerGood = ok
                                        actionBannerText = if (ok) "TEPAT +$pts" else "SALAH"
                                        feedbackMsg = (if (ok) "Tepat! " else "Salah! ") + q.explanation
                                        itemIdx++
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 50.dp),
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

@Composable
private fun LarmorConsoleMetricChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = CardSlate.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LarmorTradeoffBar(
    label: String,
    progress: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.3f)
        )
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(0.7f)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = CardSlate
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawContrastArtifactThumbnail(
    questionId: String,
    phase: Float
) {
    val w = size.width
    val h = size.height
    val brainRectPos = Offset(w * 0.14f, h * 0.11f)
    val brainRectSize = Size(w * 0.72f, h * 0.78f)
    // Palet grayscale autentik MRI klinis (hitam-abu-putih)
    val bgBlack = Color.Black
    val voidBlack = Color(0xFF06080B)
    val darkGray = Color(0xFF3E4550)
    val midGray = Color(0xFF717985)
    val lightGray = Color(0xFFB5BCC7)
    val softWhite = Color(0xFFE7ECF2)
    val pureWhite = Color.White

    drawRect(bgBlack)

    when (questionId) {
        "ca_11" -> {
            // Spektrum K-Space 2D grayscale: pusat sangat terang (kontras & kecerahan), tepi gelap (detail halus)
            val grid = 7
            val cell = w / grid.toFloat()
            val center = (grid - 1) / 2f
            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    val dist = abs(r - center) + abs(c - center)
                    val shade = when {
                        dist <= 0.5f -> pureWhite
                        dist <= 1.5f -> softWhite
                        dist <= 2.5f -> lightGray
                        dist <= 3.5f -> midGray
                        dist <= 4.5f -> darkGray
                        else -> Color(0xFF1A1E24)
                    }
                    drawRect(
                        color = shade,
                        topLeft = Offset(c * cell + 1.5f, r * cell + 1.5f),
                        size = Size(cell - 3f, cell - 3f)
                    )
                }
            }
        }
        "ca_5" -> {
            // DWI (kiri gelap + infark putih terang) vs Peta ADC (kanan abu terang + infark hitam gelap)
            drawRect(Color(0xFF0A0C10), Offset(0f, 0f), Size(w * 0.485f, h))
            drawRect(Color(0xFF222730), Offset(w * 0.515f, 0f), Size(w * 0.485f, h))
            drawLine(midGray, Offset(w * 0.50f, 4f), Offset(w * 0.50f, h - 4f), strokeWidth = 1.5f)
            // DWI b=1000 kiri
            drawOval(darkGray, Offset(w * 0.06f, h * 0.22f), Size(w * 0.36f, h * 0.56f))
            drawCircle(pureWhite, radius = w * 0.085f, center = Offset(w * 0.29f, h * 0.46f))
            // Peta ADC kanan
            drawOval(lightGray, Offset(w * 0.58f, h * 0.22f), Size(w * 0.36f, h * 0.56f))
            drawCircle(voidBlack, radius = w * 0.085f, center = Offset(w * 0.81f, h * 0.46f))
        }
        "ca_4" -> {
            // Artefak Zipper: garis putus-putus RF bocor monokrom membelah citra otak
            drawOval(midGray, brainRectPos, brainRectSize)
            drawOval(softWhite, brainRectPos, brainRectSize, style = Stroke(2.4f))
            drawOval(darkGray, Offset(w * 0.36f, h * 0.34f), Size(w * 0.11f, h * 0.28f))
            drawOval(darkGray, Offset(w * 0.53f, h * 0.34f), Size(w * 0.11f, h * 0.28f))
            drawLine(
                color = pureWhite,
                start = Offset(w * 0.50f, 2f),
                end = Offset(w * 0.50f, h - 2f),
                strokeWidth = 3.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), phase * 20f)
            )
            drawLine(
                color = bgBlack,
                start = Offset(w * 0.50f, 8f),
                end = Offset(w * 0.50f, h - 2f),
                strokeWidth = 3.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), phase * 20f + 6f)
            )
        }
        "ca_3" -> {
            // Artefak Aliasing / Wrap-around grayscale: potongan kepala terlipat ke tepi berlawanan
            drawOval(midGray, Offset(w * 0.24f, h * 0.14f), Size(w * 0.60f, h * 0.72f))
            drawOval(softWhite, Offset(w * 0.24f, h * 0.14f), Size(w * 0.60f, h * 0.72f), style = Stroke(2.2f))
            drawOval(darkGray, Offset(w * 0.42f, h * 0.34f), Size(w * 0.09f, h * 0.25f))
            drawOval(darkGray, Offset(w * 0.56f, h * 0.34f), Size(w * 0.09f, h * 0.25f))
            // Ujung anatomi terlipat ke tepi kiri FOV
            drawArc(
                color = lightGray,
                startAngle = -68f,
                sweepAngle = 136f,
                useCenter = true,
                topLeft = Offset(-w * 0.08f, h * 0.28f),
                size = Size(w * 0.30f, h * 0.44f)
            )
            drawArc(
                color = pureWhite,
                startAngle = -68f,
                sweepAngle = 136f,
                useCenter = false,
                topLeft = Offset(-w * 0.08f, h * 0.28f),
                size = Size(w * 0.30f, h * 0.44f),
                style = Stroke(2.2f)
            )
        }
        "ca_6" -> {
            // Artefak Gerak (Motion Ghosting): replika kontur abu-abu berulang di sumbu phase-encoding
            for (i in -2..2) {
                val alpha = when (abs(i)) {
                    0 -> 0.85f
                    1 -> 0.42f
                    else -> 0.20f
                }
                drawOval(
                    color = softWhite.copy(alpha = alpha * 0.28f),
                    topLeft = Offset(w * 0.21f + i * 9f, h * 0.17f),
                    size = Size(w * 0.58f, h * 0.66f)
                )
                drawOval(
                    color = softWhite.copy(alpha = alpha),
                    topLeft = Offset(w * 0.21f + i * 9f, h * 0.17f),
                    size = Size(w * 0.58f, h * 0.66f),
                    style = Stroke(2.2f)
                )
            }
        }
        "ca_8" -> {
            // Chemical Shift Artifact: pita putih bersih di satu tepi dan pita hitam kosong di tepi berlawanan
            drawOval(midGray, brainRectPos, brainRectSize)
            drawArc(pureWhite, 100f, 160f, false, brainRectPos, brainRectSize, style = Stroke(5.5f))
            drawArc(voidBlack, -80f, 160f, false, brainRectPos, brainRectSize, style = Stroke(5.5f))
        }
        "ca_12" -> {
            // Susceptibility Artifact: signal void hitam pekat + pile-up rim putih terdistorsi
            drawOval(midGray, brainRectPos, brainRectSize)
            drawOval(softWhite, brainRectPos, brainRectSize, style = Stroke(2.2f))
            drawCircle(voidBlack, radius = w * 0.18f, center = Offset(w * 0.62f, h * 0.47f))
            drawArc(
                color = pureWhite,
                startAngle = 110f,
                sweepAngle = 210f,
                useCenter = false,
                topLeft = Offset(w * 0.41f, h * 0.26f),
                size = Size(w * 0.42f, h * 0.42f),
                style = Stroke(3.2f)
            )
        }
        else -> {
            // Kontras pembobotan jaringan asli hitam-putih (T1w, FLAIR, PDw, T2w, STIR)
            val isT1 = questionId == "ca_1"
            val isFlair = questionId == "ca_2"
            val isPdw = questionId == "ca_7"
            val isT2 = questionId == "ca_9"
            val isStir = questionId == "ca_10"

            val scalpFatRim = when {
                isT1 -> pureWhite         // Lemak sangat terang di T1w
                isStir -> voidBlack       // Lemak tersupresi hitam di STIR
                isT2 -> lightGray         // Lemak abu-terang di T2w
                isFlair -> midGray
                else -> lightGray
            }
            val cortexGm = when {
                isT1 -> darkGray          // GM lebih gelap daripada WM di T1w
                isT2 || isFlair -> lightGray // GM lebih terang daripada WM di T2w/FLAIR
                isStir -> midGray
                else -> Color(0xFF8C939F) // PDw: abu lembut hampir rata
            }
            val wmInner = when {
                isT1 -> lightGray         // WM lebih terang daripada GM di T1w
                isT2 || isFlair -> darkGray // WM lebih gelap daripada GM di T2w/FLAIR
                isStir -> darkGray
                else -> Color(0xFFA2A8B4) // PDw: WM sedikit terang
            }
            val csfColor = when {
                isT1 || isFlair -> voidBlack // CSF hitam pada T1w & FLAIR
                isT2 || isStir -> pureWhite  // CSF/cairan putih menyala pada T2w & STIR
                isPdw -> Color(0xFF9EA5B2)   // PDw: CSF abu sedang
                else -> midGray
            }

            // Lapisan lemak subkutan luar + korteks (GM) + substansia alba (WM)
            drawOval(cortexGm, brainRectPos, brainRectSize)
            drawOval(
                wmInner,
                Offset(w * 0.23f, h * 0.21f),
                Size(w * 0.54f, h * 0.58f)
            )
            drawOval(scalpFatRim, brainRectPos, brainRectSize, style = Stroke(4.5f))
            // Ventrikel lateral berisi CSF
            drawOval(csfColor, Offset(w * 0.36f, h * 0.34f), Size(w * 0.11f, h * 0.28f))
            drawOval(csfColor, Offset(w * 0.53f, h * 0.34f), Size(w * 0.11f, h * 0.28f))

            if (isFlair || isT2 || isStir) {
                // Fokus lesi/edema hiperintens putih bersih
                drawCircle(pureWhite, radius = 5.5f, center = Offset(w * 0.31f, h * 0.41f))
                drawCircle(softWhite, radius = 4.2f, center = Offset(w * 0.69f, h * 0.56f))
            }
        }
    }
}
