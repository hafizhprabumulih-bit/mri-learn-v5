package com.example.ui.dialog

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CharacterCustomizationState
import com.example.ui.character.AvatarView
import com.example.ui.character.Expression
import com.example.ui.theme.DeepObsidian
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(
    customMessage: String? = null,
    onFinished: (() -> Unit)? = null
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var currentStatusMsg by remember { mutableStateOf("Menyiapkan aplikasi…") }

    val msgs = remember(customMessage) {
        if (!customMessage.isNullOrBlank()) {
            listOf(
                "Menyiapkan $customMessage…",
                "Memuat materi…",
                "Menyiapkan simulasi…",
                "Hampir selesai…"
            )
        } else {
            listOf(
                "Menyiapkan aplikasi…",
                "Memuat materi…",
                "Menyiapkan simulasi…",
                "Hampir selesai…"
            )
        }
    }

    // Durasi 2,4 detik (2400 ms) terkalibrasi halus; langsung panggil onFinished begitu menyentuh 100%
    val sessionKey = remember(customMessage) { System.nanoTime() }
    LaunchedEffect(customMessage, sessionKey) {
        progress = 0f
        var startNs = 0L
        val durationMs = 2400f
        var completed = false
        while (!completed) {
            androidx.compose.runtime.withFrameNanos { frameNs ->
                if (startNs == 0L) startNs = frameNs
                val elapsedMs = (frameNs - startNs) / 1_000_000f
                val p = (elapsedMs / durationMs).coerceIn(0f, 1f)
                val smoothStep = p * p * (3f - 2f * p)
                val eased = p * 0.78f + smoothStep * 0.22f
                val idx = (p * msgs.size).toInt().coerceIn(0, msgs.size - 1)
                currentStatusMsg = msgs[idx]
                if (p >= 1f) {
                    progress = 1f
                    currentStatusMsg = msgs.last()
                    completed = true
                } else {
                    progress = eased
                }
            }
        }
        onFinished?.invoke()
    }

    val percentage = if (progress >= 1f) 100 else (progress * 100f).toInt().coerceIn(0, 99)

    val infiniteTransition = rememberInfiniteTransition(label = "loading_anim")

    // Float animation 3.4s ease-in-out (1.7s each direction) applied to the whole stage in unison
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // Spin ring 2.6s linear matching @keyframes rot
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    // Pulse ring 1 (2.8s ease-out) starting directly from the logo perimeter
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.44f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )

    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    // Pulse ring 2 (delayed by 1.4s) matching .pulse.p2
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.44f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            initialStartOffset = StartOffset(1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            initialStartOffset = StartOffset(1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    val density = androidx.compose.ui.platform.LocalDensity.current
    val gradientRadiusPx = remember(density) { with(density) { 420.dp.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0F3A44), DeepObsidian),
                    radius = gradientRadiusPx
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(24.dp)
        ) {
            // Stage containing wave rings, spin arc, and MRI logo all moving in unison (proporsi seimbang)
            Box(
                modifier = Modifier
                    .size(204.dp)
                    .offset(y = floatOffset.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow aura matching drop-shadow(0 14px 30px rgba(45,212,191,.28))
                Canvas(
                    modifier = Modifier.size(190.dp)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF2DD4BF).copy(alpha = 0.22f), Color.Transparent),
                            radius = size.minDimension / 2f
                        )
                    )
                }

                // Pulse 1 ring - concentric with logo, scaled on GPU layer without relayout
                Box(
                    modifier = Modifier
                        .size(138.dp)
                        .graphicsLayer {
                            scaleX = pulseScale1
                            scaleY = pulseScale1
                        }
                        .border(
                            width = 2.dp,
                            color = Color(0xFF2DD4BF).copy(alpha = pulseAlpha1),
                            shape = CircleShape
                        )
                )

                // Pulse 2 ring - concentric with logo, delayed by 1.4s
                Box(
                    modifier = Modifier
                        .size(138.dp)
                        .graphicsLayer {
                            scaleX = pulseScale2
                            scaleY = pulseScale2
                        }
                        .border(
                            width = 2.dp,
                            color = Color(0xFF2DD4BF).copy(alpha = pulseAlpha2),
                            shape = CircleShape
                        )
                )

                // Spin dashed ring - concentric around the logo circle
                Canvas(
                    modifier = Modifier
                        .size(168.dp)
                        .rotate(spinAngle)
                ) {
                    drawCircle(
                        color = Color(0xFF2DD4BF).copy(alpha = 0.88f),
                        radius = size.minDimension / 2f - 2.dp.toPx(),
                        style = Stroke(
                            width = 1.8.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(50f, 210f), 0f),
                            cap = StrokeCap.Round
                        )
                    )
                }

                // MRI Icon perfectly centered and symmetrical
                Box(
                    modifier = Modifier.size(138.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mri_icon_bersih),
                        contentDescription = "Ikon mesin MRI",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Title: Belajar MRI matching HTML h1
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Belajar ",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        text = "MRI",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2DD4BF),
                        letterSpacing = 0.2.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Radiologi Medis · MRI EDU",
                    fontSize = 11.sp,
                    color = Color(0xFF8FA3B5),
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            val walkBob by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(260, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "nano_walk_bob"
            )
            val walkTilt by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(520, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "nano_walk_tilt"
            )

            // Progress Bar proporsional (280.dp) + Nano berjalan di atas bar
            Column(
                modifier = Modifier.width(280.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    AvatarView(
                        role = "radiografer_mri",
                        equipped = CharacterCustomizationState(),
                        expression = if (percentage >= 95) Expression.SENANG else Expression.BERPIKIR,
                        modifier = Modifier
                            .offset(x = (progress.coerceIn(0f, 1f) * 234f).dp, y = walkBob.dp)
                            .size(46.dp)
                            .graphicsLayer {
                                rotationZ = walkTilt
                            }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Track proporsional (12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(Color(0xFF0A1A28), RoundedCornerShape(99.dp))
                        .border(1.2.dp, Color(0xFF16394F), RoundedCornerShape(99.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(12.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF14917F), Color(0xFF2DD4BF))
                                ),
                                shape = RoundedCornerShape(99.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentStatusMsg,
                        fontSize = 12.sp,
                        color = Color(0xFF8FA3B5),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$percentage%",
                        fontSize = 12.5.sp,
                        color = Color(0xFF2DD4BF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
