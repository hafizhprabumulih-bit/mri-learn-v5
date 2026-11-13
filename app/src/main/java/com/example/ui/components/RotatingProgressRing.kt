package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.path.rememberReduceMotion
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CyanBright
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextPrimary
import com.example.util.CircularProgressMotion

/**
 * Lingkaran progres berputar dengan teks angka progres (mis. "4/5") di tengah lingkaran.
 * Busur progres berputar mengikuti rasio [done]/[total] serta berotasi lembut saat aktif.
 */
@Composable
fun RotatingProgressRing(
    done: Int,
    total: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    strokeWidth: Dp = 3.8.dp,
    centerText: String = if (total > 0) "$done/$total" else "0",
    fontSize: TextUnit = 10.sp,
    activeColor: Color = CyanBright,
    completeColor: Color = EmeraldSuccess,
    trackColor: Color = CardSlateBorder.copy(alpha = 0.45f),
    semanticLabel: String = "Progres $done dari $total"
) {
    val reduceMotion = rememberReduceMotion()
    val rawProgress = CircularProgressMotion.normalizeProgress(done, total)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 650),
        label = "rotatingRingProgress"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "rotatingRingSpin")
    val spinDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringSpinDeg"
    )

    val isComplete = total > 0 && done >= total
    val isInProgress = !reduceMotion && total > 0 && done in 1 until total
    val startAngleDeg = CircularProgressMotion.calculateStartAngleDeg(
        progress = animatedProgress,
        continuousSpinDeg = spinDeg,
        isInProgress = isInProgress
    )
    val sweepDeg = CircularProgressMotion.calculateSweepAngleDeg(
        progress = animatedProgress,
        total = total
    )
    val ringColor = if (isComplete) completeColor else activeColor

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = semanticLabel },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f + 1.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2f, this.size.height - inset * 2f)

            drawCircle(
                color = DeepObsidian.copy(alpha = 0.78f),
                radius = (this.size.minDimension - strokePx) / 2f
            )
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (sweepDeg > 0f) {
                drawArc(
                    color = if (done > 0) ringColor else ringColor.copy(alpha = 0.55f),
                    startAngle = startAngleDeg,
                    sweepAngle = sweepDeg,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = centerText,
            color = if (isComplete) completeColor else TextPrimary,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}
