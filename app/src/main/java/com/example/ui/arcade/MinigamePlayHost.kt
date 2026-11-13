package com.example.ui.arcade

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
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
                        lineHeight = 15.5.sp
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(124.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF071322)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_arcade_screening_gate),
                                    contentDescription = "Portal Detektor Zona IV",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x66071322))
                                )
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val scanX = w * (0.18f + 0.64f * wavePhase)
                                    drawLine(
                                        color = if (combo >= 3) AmberWarning.copy(alpha = 0.85f) else CyanBright.copy(alpha = 0.78f),
                                        start = Offset(scanX, h * 0.10f),
                                        end = Offset(scanX, h * 0.90f),
                                        strokeWidth = 3.2f,
                                        cap = StrokeCap.Round
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xCC071322),
                                    border = BorderStroke(1.8.dp, CyanBright.copy(alpha = 0.75f)),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    MriGlobalAssetImage(
                                        assetKey = MriGlobalAssetCatalog.resolveKey(item.assetKey.ifBlank { item.name }),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        tintAccent = if (item.allowIntoZone4) EmeraldSuccess else RoseDanger
                                    )
                                }
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
                                val ok = !item.allowIntoZone4
                                if (ok) haptics.mediumAction() else haptics.warningBuzz()
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
                                feedbackMsg = (if (ok) "Tepat! " else "Bahaya! ") + item.reason
                                itemIdx++
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseDanger, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("TOLAK / LEPAS", fontWeight = FontWeight.ExtraBold)
                        }
                        Button(
                            onClick = {
                                val ok = item.allowIntoZone4
                                if (ok) haptics.mediumAction() else haptics.warningBuzz()
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
                                feedbackMsg = (if (ok) "Tepat! " else "Kurang tepat! ") + item.reason
                                itemIdx++
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = DeepObsidian)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("AMAN / LOLOS", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                MinigameKind.LARMOR_TAP -> {
                    val challenge = currentLarmorChallenge
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

                            // Monitor Osiloskop RF sesuai style aset aplikasi dengan gelombang & jarum di dalam layar monitor
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(178.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF071322))
                                    .border(1.2.dp, CyanPrimary.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_larmor_monitor),
                                    contentDescription = "Monitor Osiloskop Larmor",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )
                                // Area layar aktif di dalam bingkai monitor
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xAA06111F))
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.width
                                        val h = size.height

                                        // Zona tangkap (GOOD & PERFECT) di dalam layar monitor
                                        val targetX = challenge.targetNormalized * w
                                        drawRoundRect(
                                            color = CyanBright.copy(alpha = 0.22f),
                                            topLeft = Offset(targetX - w * 0.12f, h * 0.14f),
                                            size = Size(w * 0.24f, h * 0.72f),
                                            cornerRadius = CornerRadius(10f, 10f)
                                        )
                                        drawRoundRect(
                                            color = EmeraldSuccess.copy(alpha = 0.56f),
                                            topLeft = Offset(targetX - w * 0.055f, h * 0.10f),
                                            size = Size(w * 0.11f, h * 0.80f),
                                            cornerRadius = CornerRadius(10f, 10f)
                                        )

                                        // Gelombang pembawa RF langsung di dalam monitor
                                        val wavePath = Path()
                                        val steps = 54
                                        for (i in 0..steps) {
                                            val t = i / steps.toFloat()
                                            val x = 6f + t * (w - 12f)
                                            val distToTarget = kotlin.math.abs(t - challenge.targetNormalized)
                                            val env = (1f - distToTarget * 2.0f).coerceIn(0.18f, 1f)
                                            val y = h * 0.5f - sin((t * 8f + wavePhase * 2f) * PI).toFloat() * (h * 0.34f) * env
                                            if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                                        }
                                        drawPath(
                                            wavePath,
                                            color = CyanBright.copy(alpha = 0.85f),
                                            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                                        )

                                        // Jarum osilasi tuner
                                        val needleX = needle * w
                                        drawLine(AmberWarning, Offset(needleX, 4f), Offset(needleX, h - 4f), 6.5f, StrokeCap.Round)
                                        drawCircle(Color.White, radius = 5.5f, center = Offset(needleX, h * 0.5f))
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val acc = MriMinigameEngine.evaluateLarmorTap(needle, challenge.targetNormalized)
                                    if (acc != TapAccuracy.MISS) haptics.mediumAction() else haptics.warningBuzz()
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
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("TEMBAK PULSA RF 90°!", fontWeight = FontWeight.ExtraBold)
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
                                        val csfColor = when {
                                            q.prompt.contains("terang", ignoreCase = true) -> Color.White
                                            q.prompt.contains("gelap", ignoreCase = true) || q.prompt.contains("ditekan", ignoreCase = true) -> Color(0xFF071322)
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
                                        val ok = optIdx == shuffledOptions.second
                                        if (ok) haptics.mediumAction() else haptics.warningBuzz()
                                        totalAnswered++
                                        if (ok) {
                                            totalCorrect++
                                            combo++
                                            score += MriMinigameEngine.computePoints(110, combo)
                                        } else {
                                            combo = 0
                                        }
                                        feedbackGood = ok
                                        feedbackMsg = (if (ok) "Tepat! " else "Salah! ") + q.explanation
                                        itemIdx++
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp),
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
