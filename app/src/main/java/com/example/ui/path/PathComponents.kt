package com.example.ui.path

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CharAccentMint
import com.example.ui.theme.CharAccentSky
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.NodeStatus
import kotlin.math.abs
import kotlin.math.roundToInt

enum class NodeKind {
    MATERI,
    TUJUAN,
    RINGKASAN,
    LAB,
    KUIS_TINGKAT,
    KUIS_MODUL,
    MODUL
}

fun moduleAccentColor(moduleCode: String): Color {
    val moduleNumber = moduleCode.drop(1).takeWhile { it.isDigit() }.toIntOrNull() ?: 9
    return when {
        moduleNumber in listOf(1, 5, 9) -> CyanBright
        moduleNumber in listOf(2, 6) -> CharAccentMint
        moduleNumber in listOf(3, 7) -> AmberWarning
        else -> CharAccentSky
    }
}

@Composable
fun rememberReduceMotion(): Boolean {
    if (LocalInspectionMode.current) return true
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

private fun readableInkOn(background: Color, darkInk: Color, lightInk: Color): Color =
    if (background.luminance() > 0.18f) darkInk else lightInk

private fun compactToggleCode(rawLabel: String, kind: NodeKind): String {
    val trimmed = rawLabel.trim()
    return when (kind) {
        NodeKind.MODUL -> trimmed.substringBefore(" ").uppercase()
        NodeKind.KUIS_TINGKAT -> {
            val digits = trimmed.filter { it.isDigit() }
            if (digits.isNotEmpty()) "KT$digits" else "KUIS"
        }
        NodeKind.KUIS_MODUL -> "EVAL"
        else -> {
            val firstToken = trimmed.substringBefore(" ")
            firstToken.removePrefix("M01.").removePrefix("M02.")
                .removePrefix("M03A.").removePrefix("M03B.").removePrefix("M03C.")
                .removePrefix("M04A.").removePrefix("M04B.").removePrefix("M04C.")
                .removePrefix("M05.").removePrefix("M06A.").removePrefix("M06B.")
                .removePrefix("M07A.").removePrefix("M07B.").removePrefix("M07C.")
                .removePrefix("M08A.").removePrefix("M08B.").removePrefix("M08C.")
                .removePrefix("M09.")
                .take(6)
                .uppercase()
        }
    }
}

@Composable
fun PathNodeButton(
    status: NodeStatus,
    kind: NodeKind,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    progressPill: String? = null,
    hasLabBadge: Boolean = false,
    dimmed: Boolean = false,
    semanticLabel: String = label
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptics = com.example.util.rememberMriHaptics()
    val reduceMotion = rememberReduceMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseSize = if (kind == NodeKind.MODUL) 88.dp else 82.dp
    val shadowDepth = 10.dp
    val pressTravel = 5.dp
    val ringPadding = 6.dp
    // Semua simpul berbentuk tombol 3D sudut membulat seragam ala toggle "M01"
    val shape = RoundedCornerShape(22.dp)

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) pressTravel else 0.dp,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 90),
        label = "pathNodePress"
    )

    val ringPulseAlpha = if (status == NodeStatus.CURRENT && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "currentNodeRing")
        val anim by transition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ringAlpha"
        )
        anim
    } else {
        1.0f
    }

    val doneColor = colorScheme.secondary
    val skippedColor = lerp(colorScheme.surfaceVariant, doneColor, 0.45f)
    val rawFaceColor = when (status) {
        NodeStatus.DONE -> lerp(doneColor, Color.White, 0.08f)
        NodeStatus.SKIPPED -> skippedColor
        NodeStatus.CURRENT -> lerp(accentColor, Color(0xFF38E8FF), 0.35f)
        NodeStatus.OPEN -> lerp(Color(0xFF193552), accentColor, 0.32f)
        NodeStatus.LOCKED -> Color(0xFF1A2D46)
    }

    // Warna dasar bodi 3D bawah yang kontras terhadap latar gelap agar efek tombol 3D selalu tegas seperti M01
    val rawShadowColor = when (status) {
        NodeStatus.DONE -> lerp(doneColor, Color(0xFF062920), 0.62f)
        NodeStatus.SKIPPED -> lerp(skippedColor, Color(0xFF071E2E), 0.62f)
        NodeStatus.CURRENT -> lerp(accentColor, Color(0xFF062A3E), 0.56f)
        NodeStatus.OPEN -> lerp(accentColor, Color(0xFF0A1D33), 0.72f)
        NodeStatus.LOCKED -> Color(0xFF0E1B2E)
    }

    val rawBorderColor = when (status) {
        NodeStatus.DONE -> lerp(doneColor, Color.White, 0.32f)
        NodeStatus.SKIPPED -> doneColor.copy(alpha = 0.80f)
        NodeStatus.CURRENT -> lerp(accentColor, Color.White, 0.35f)
        NodeStatus.OPEN -> accentColor.copy(alpha = 0.82f)
        NodeStatus.LOCKED -> Color(0xFF426288)
    }

    val rawShadowBorderColor = when (status) {
        NodeStatus.DONE -> doneColor.copy(alpha = 0.75f)
        NodeStatus.SKIPPED -> doneColor.copy(alpha = 0.55f)
        NodeStatus.CURRENT -> accentColor.copy(alpha = 0.88f)
        NodeStatus.OPEN -> accentColor.copy(alpha = 0.58f)
        NodeStatus.LOCKED -> Color(0xFF2D4868)
    }

    val colorAnimMs = if (reduceMotion) 0 else 480
    val faceColor by animateColorAsState(
        targetValue = rawFaceColor,
        animationSpec = tween(durationMillis = colorAnimMs),
        label = "nodeFaceColor"
    )
    val shadowColor by animateColorAsState(
        targetValue = rawShadowColor,
        animationSpec = tween(durationMillis = colorAnimMs),
        label = "nodeShadowColor"
    )
    val borderColor by animateColorAsState(
        targetValue = rawBorderColor,
        animationSpec = tween(durationMillis = colorAnimMs),
        label = "nodeBorderColor"
    )
    val shadowBorderColor by animateColorAsState(
        targetValue = rawShadowBorderColor,
        animationSpec = tween(durationMillis = colorAnimMs),
        label = "nodeShadowBorderColor"
    )

    val contentInk = when (status) {
        NodeStatus.DONE -> DeepObsidian
        NodeStatus.CURRENT -> DeepObsidian
        NodeStatus.SKIPPED -> colorScheme.onSurface
        NodeStatus.OPEN -> Color.White
        NodeStatus.LOCKED -> Color(0xFFD0E2F8)
    }

    val statusTextId = when (status) {
        NodeStatus.DONE -> "selesai"
        NodeStatus.SKIPPED -> "dilewati via tes"
        NodeStatus.CURRENT -> "sedang dikerjakan"
        NodeStatus.OPEN -> "terbuka"
        NodeStatus.LOCKED -> "terkunci"
    }
    val kindPrefix = when (kind) {
        NodeKind.MATERI -> "Materi "
        NodeKind.TUJUAN -> "Tujuan, "
        NodeKind.RINGKASAN -> "Ringkasan, "
        NodeKind.LAB -> "Lab, "
        NodeKind.KUIS_TINGKAT -> "Kuis tingkat, "
        NodeKind.KUIS_MODUL -> "Kuis modul, "
        NodeKind.MODUL -> "Modul "
    }
    val semanticDescription = buildString {
        append(kindPrefix)
        append(semanticLabel)
        append(", ")
        append(statusTextId)
        if (!progressPill.isNullOrBlank()) {
            append(", progres ")
            append(progressPill)
        }
        if (dimmed && status != NodeStatus.DONE && status != NodeStatus.CURRENT) {
            append(", prasyarat disarankan belum tercapai")
        }
    }

    val outerWidth = baseSize + ringPadding * 2
    val outerHeight = baseSize + shadowDepth + ringPadding * 2
    val nodeAlpha = if (dimmed && status != NodeStatus.DONE && status != NodeStatus.CURRENT) 0.90f else 1f
    val shortCode = remember(label, kind) { compactToggleCode(label, kind) }

    val resolvedPill = progressPill ?: when {
        kind == NodeKind.MODUL -> null
        status == NodeStatus.DONE -> "✓"
        status == NodeStatus.SKIPPED -> "Lulus"
        kind == NodeKind.LAB -> "Lab"
        kind == NodeKind.KUIS_TINGKAT || kind == NodeKind.KUIS_MODUL -> "Kuis"
        else -> null
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .size(width = outerWidth, height = outerHeight)
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Penghalang latar solid agar garis jalur berhenti tepat di tepi luar blok kotak 3D
        Box(
            modifier = Modifier
                .padding(top = ringPadding)
                .size(width = baseSize, height = baseSize + shadowDepth)
                .clip(shape)
                .background(colorScheme.background)
        )

        // Cincin luar menyala untuk simpul CURRENT (dan rim halus untuk simpul lain agar menyatu 3D)
        Box(
            modifier = Modifier
                .padding(top = 0.dp)
                .size(width = baseSize + ringPadding * 2, height = baseSize + shadowDepth + ringPadding * 2)
                .border(
                    width = if (status == NodeStatus.CURRENT) 3.dp else 1.5.dp,
                    color = if (status == NodeStatus.CURRENT) {
                        accentColor.copy(alpha = ringPulseAlpha)
                    } else {
                        shadowBorderColor.copy(alpha = 0.45f)
                    },
                    shape = RoundedCornerShape(26.dp)
                )
        )

        // Lapisan interaksi tombol timbul 3D seragam seperti toggle "M01"
        Box(
            modifier = Modifier
                .padding(top = ringPadding)
                .size(width = baseSize, height = baseSize + shadowDepth)
                .alpha(nodeAlpha)
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        haptics.lightToggle()
                        onClick()
                    }
                )
        ) {
            // Bodi / dudukan 3D bawah dengan border tegas agar efek 3 dimensi selalu tampak nyata
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = shadowDepth)
                    .size(baseSize)
                    .clip(shape)
                    .background(shadowColor)
                    .border(2.dp, shadowBorderColor, shape)
            )

            // Permukaan tombol 3D utama dengan highlight atas (bevel specular) dan kode tegas
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = pressOffset)
                    .size(baseSize)
                    .clip(shape)
                    .background(faceColor)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (status == NodeStatus.CURRENT || status == NodeStatus.DONE) 0.28f else 0.16f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.22f)
                            )
                        )
                    )
                    .border(2.2.dp, borderColor, shape),
                contentAlignment = Alignment.Center
            ) {
                // Bevel kilau 3D pada sisi atas dalam tombol
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp, start = 12.dp, end = 12.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Color.White.copy(
                                alpha = if (status == NodeStatus.CURRENT || status == NodeStatus.DONE) 0.42f else 0.22f
                            )
                        )
                )

                val topSmallIcon: ImageVector? = when {
                    status == NodeStatus.DONE -> Icons.Default.Check
                    status == NodeStatus.LOCKED || (dimmed && status != NodeStatus.CURRENT) -> Icons.Default.Lock
                    kind == NodeKind.LAB -> Icons.Default.Science
                    kind == NodeKind.KUIS_TINGKAT || kind == NodeKind.KUIS_MODUL -> Icons.Default.Quiz
                    kind == NodeKind.RINGKASAN -> Icons.Default.Star
                    kind == NodeKind.TUJUAN -> Icons.Default.Flag
                    status == NodeStatus.CURRENT && kind != NodeKind.MODUL -> Icons.Default.PlayArrow
                    else -> null
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    if (topSmallIcon != null) {
                        Icon(
                            imageVector = topSmallIcon,
                            contentDescription = null,
                            tint = contentInk.copy(alpha = 0.90f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = shortCode,
                        color = contentInk,
                        fontSize = if (shortCode.length <= 4) 17.sp else 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Pil progres di sudut kanan-bawah blok kotak 3D (persis seperti toggle M01 di referensi)
        if (!resolvedPill.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 1.dp, bottom = 0.dp),
                shape = RoundedCornerShape(10.dp),
                color = DeepObsidian,
                border = BorderStroke(1.4.dp, borderColor),
                shadowElevation = 3.dp
            ) {
                Text(
                    text = resolvedPill,
                    color = if (status == NodeStatus.CURRENT) accentColor else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // Lencana kecil lab jika materi memiliki simulasi terintegrasi
        if (hasLabBadge && kind != NodeKind.LAB && kind != NodeKind.MODUL) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(24.dp),
                shape = CircleShape,
                color = DarkNavySlate,
                border = BorderStroke(1.5.dp, CyanBright)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = CyanBright,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Menempatkan konten (simpul) tepat pada pusat horizontal `xFraction` (0f..1f)
 * sehingga tepat sejajar dengan titik ujung [PathConnector].
 */
@Composable
fun PathAlignedSlot(
    xFraction: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier.fillMaxWidth()
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0)
        val placeables = measurables.map { it.measure(childConstraints) }
        val totalWidth = constraints.maxWidth
        val targetCenter = (totalWidth * xFraction.coerceIn(0f, 1f)).roundToInt()
        val maxHeight = placeables.maxOfOrNull { it.height } ?: 0
        layout(totalWidth, maxHeight) {
            placeables.forEach { placeable ->
                val maxLeft = (totalWidth - placeable.width).coerceAtLeast(0)
                val x = (targetCenter - placeable.width / 2).coerceIn(0, maxLeft)
                placeable.placeRelative(x, 0)
            }
        }
    }
}

@Composable
fun PathConnector(
    fromX: Float,
    toX: Float,
    height: Dp,
    done: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
    variant: Int = 0,
    strokeWidthDp: Dp = 5.5.dp
) {
    val reduceMotion = rememberReduceMotion()
    val inactiveColor = Color(0xFF3E608A)
    val strokeColor by animateColorAsState(
        targetValue = if (done) accentColor else inactiveColor,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 480),
        label = "pathConnectorColor"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val startX = size.width * fromX.coerceIn(0f, 1f)
        val endX = size.width * toX.coerceIn(0f, 1f)
        val midY = size.height * 0.5f
        val strokePx = strokeWidthDp.toPx()
        val nodeBoxPx = 9.dp.toPx()

        val path = Path().apply {
            moveTo(startX, 0f)
            if (abs(startX - endX) < 2f) {
                lineTo(endX, size.height)
            } else {
                lineTo(startX, midY)
                lineTo(endX, midY)
                lineTo(endX, size.height)
            }
        }

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )
        if (abs(startX - endX) >= 2f) {
            drawRect(
                color = strokeColor,
                topLeft = Offset(startX - nodeBoxPx / 2f, midY - nodeBoxPx / 2f),
                size = Size(nodeBoxPx, nodeBoxPx)
            )
            drawRect(
                color = strokeColor,
                topLeft = Offset(endX - nodeBoxPx / 2f, midY - nodeBoxPx / 2f),
                size = Size(nodeBoxPx, nodeBoxPx)
            )
        }
    }
}

/**
 * Menggambar garis jalur TEGAS (solid, kontras tinggi, siku bertitik sambung kotak)
 * langsung dari atas hingga bawah setiap blok kotak 3D sehingga menyambung mulus tanpa putus.
 *
 * - Garis Masuk: turun lurus vertikal pada `nodeX` dari `y = 0` ke pusat tombol (`centerY`),
 *   melanjutkan segmen keluar dari baris di atasnya.
 * - Garis Keluar: turun lurus dari pusat tombol (`centerY`) melewati tepi bawah tombol ke `turnY`
 *   (di celah kosong bawah tombol), berbelok 90° horizontal ke `nextX`, lalu turun ke `size.height`
 *   tepat di atas pusat tombol baris berikutnya!
 */
@Composable
fun SeamlessNodePathBackdrop(
    nodeX: Float,
    prevX: Float?,
    nextX: Float?,
    incomingDone: Boolean,
    outgoingDone: Boolean,
    nodeCenterY: Dp,
    modifier: Modifier = Modifier,
    accentColor: Color = CyanBright,
    strokeWidthDp: Dp = 6.dp,
    connectFromHeader: Boolean = true,
    connectToFooter: Boolean = true
) {
    val reduceMotion = rememberReduceMotion()
    val inactiveColor = Color(0xFF426794)
    val inColor by animateColorAsState(
        targetValue = if (incomingDone) accentColor else inactiveColor,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 480),
        label = "seamlessInColor"
    )
    val outColor by animateColorAsState(
        targetValue = if (outgoingDone) accentColor else inactiveColor,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 480),
        label = "seamlessOutColor"
    )

    Canvas(modifier = modifier) {
        val nodePx = size.width * nodeX.coerceIn(0f, 1f)
        val centerY = nodeCenterY.toPx().coerceIn(0f, size.height)
        val strokePx = strokeWidthDp.toPx()
        val junctionPx = 10.dp.toPx()

        // 1. Garis Masuk vertikal lurus ke bagian atas tengah kotak saat ini
        val hasIncoming = prevX != null || connectFromHeader
        if (hasIncoming && centerY > 1f) {
            val inPath = Path().apply {
                moveTo(nodePx, 0f)
                lineTo(nodePx, centerY)
            }
            drawPath(
                path = inPath,
                color = inColor.copy(alpha = 0.28f),
                style = Stroke(width = strokePx + 5.dp.toPx(), cap = StrokeCap.Butt)
            )
            drawPath(
                path = inPath,
                color = inColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
            )
        }

        // 2. Garis Keluar dari bawah tengah kotak saat ini menuju kotak berikutnya atau Header Bab berikutnya
        val targetNextX = nextX ?: if (connectToFooter) nodeX else null
        if (targetNextX != null && size.height - centerY > 1f) {
            val nextPx = size.width * targetNextX.coerceIn(0f, 1f)
            // Titik belok berada di celah kosong di bawah kotak tombol (13.dp dari dasar baris)
            val turnY = (size.height - 13.dp.toPx()).coerceAtLeast(centerY + 46.dp.toPx()).coerceAtMost(size.height - 3.dp.toPx())
            val outPath = Path().apply {
                moveTo(nodePx, centerY)
                if (abs(nextPx - nodePx) < 2f) {
                    lineTo(nodePx, size.height)
                } else {
                    lineTo(nodePx, turnY)
                    lineTo(nextPx, turnY)
                    lineTo(nextPx, size.height)
                }
            }
            drawPath(
                path = outPath,
                color = outColor.copy(alpha = 0.28f),
                style = Stroke(width = strokePx + 5.dp.toPx(), cap = StrokeCap.Butt, join = StrokeJoin.Miter)
            )
            drawPath(
                path = outPath,
                color = outColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
            )
            if (abs(nextPx - nodePx) >= 2f) {
                drawRoundRect(
                    color = outColor,
                    topLeft = Offset(nodePx - junctionPx / 2f, turnY - junctionPx / 2f),
                    size = Size(junctionPx, junctionPx),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
                drawRoundRect(
                    color = outColor,
                    topLeft = Offset(nextPx - junctionPx / 2f, turnY - junctionPx / 2f),
                    size = Size(junctionPx, junctionPx),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun SectionDivider(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    doneCount: Int? = null,
    totalCount: Int? = null,
    topEntryX: Float? = null,
    bottomExitX: Float? = null,
    topEntryDone: Boolean = false,
    bottomExitDone: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val reduceMotion = rememberReduceMotion()
    val resolvedTitle = remember(title) {
        title.replace("Ruang-k", "K-space").replace("ruang-k", "K-space").replace("Ruang K", "K-space")
    }
    val resolvedSubtitle = remember(subtitle) {
        subtitle
            ?.replace("simpul", "materi")
            ?.replace("Simpul", "Materi")
            ?.replace("Ruang-k", "K-space")
            ?.replace("ruang-k", "K-space")
    }

    // Parse "done/total" otomatis dari subtitle bila doneCount/totalCount tidak dikirim eksplisit
    val (resolvedDone, resolvedTotal) = remember(doneCount, totalCount, subtitle) {
        if (doneCount != null && totalCount != null && totalCount > 0) {
            doneCount to totalCount
        } else {
            val match = Regex("(\\d+)\\s*/\\s*(\\d+)").find(subtitle.orEmpty())
            if (match != null) {
                val d = match.groupValues[1].toIntOrNull() ?: 0
                val t = match.groupValues[2].toIntOrNull() ?: 0
                d to t
            } else {
                0 to 0
            }
        }
    }

    val rawProgress = if (resolvedTotal > 0) {
        (resolvedDone.toFloat() / resolvedTotal.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 650),
        label = "sectionCircleProgress"
    )
    // Efek putar dinamis sesuai progres + animasi rotasi halus saat sedang dikerjakan
    val infiniteTransition = rememberInfiniteTransition(label = "sectionProgressSpin")
    val continuousSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinDeg"
    )
    val startAngleDeg = if (reduceMotion) {
        -90f + animatedProgress * 180f
    } else {
        -90f + animatedProgress * 180f + (if (resolvedDone in 1 until resolvedTotal) continuousSpin * 0.15f else 0f)
    }
    val isComplete = resolvedTotal > 0 && resolvedDone >= resolvedTotal
    val ringColor = if (isComplete) EmeraldSuccess else CyanBright

    Box(modifier = modifier.fillMaxWidth()) {
        // Garis penghubung tegas yang menembus celah atas & bawah kartu Bab
        if (topEntryX != null || bottomExitX != null) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val strokePx = 6.dp.toPx()
                val inactiveLine = Color(0xFF426794)
                if (topEntryX != null) {
                    val x = size.width * topEntryX.coerceIn(0f, 1f)
                    val col = if (topEntryDone) CyanBright else inactiveLine
                    drawLine(
                        color = col.copy(alpha = 0.28f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height * 0.5f),
                        strokeWidth = strokePx + 5.dp.toPx()
                    )
                    drawLine(
                        color = col,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height * 0.5f),
                        strokeWidth = strokePx
                    )
                }
                if (bottomExitX != null) {
                    val x = size.width * bottomExitX.coerceIn(0f, 1f)
                    val col = if (bottomExitDone) CyanBright else inactiveLine
                    drawLine(
                        color = col.copy(alpha = 0.28f),
                        start = Offset(x, size.height * 0.5f),
                        end = Offset(x, size.height),
                        strokeWidth = strokePx + 5.dp.toPx()
                    )
                    drawLine(
                        color = col,
                        start = Offset(x, size.height * 0.5f),
                        end = Offset(x, size.height),
                        strokeWidth = strokePx
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) { heading() },
            shape = RoundedCornerShape(18.dp),
            color = colorScheme.surface,
            border = BorderStroke(1.6.dp, if (isComplete) EmeraldSuccess.copy(alpha = 0.7f) else colorScheme.primary.copy(alpha = 0.55f)),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                colorScheme.primary.copy(alpha = 0.18f),
                                colorScheme.surface,
                                colorScheme.secondary.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    Text(
                        text = resolvedTitle,
                        color = colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!resolvedSubtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = resolvedSubtitle,
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Lingkaran progres berputar berisi angka materi yang sudah diselesaikan (mis. "2/20")
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 3.6.dp.toPx()
                        val inset = strokeW / 2f + 1.dp.toPx()
                        val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                        drawCircle(
                            color = DeepObsidian.copy(alpha = 0.72f),
                            radius = (size.minDimension - strokeW) / 2f
                        )
                        drawArc(
                            color = colorScheme.outline.copy(alpha = 0.35f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                        val sweep = when {
                            resolvedTotal <= 0 -> 0f
                            resolvedDone <= 0 -> 16f
                            else -> (animatedProgress * 360f).coerceAtLeast(24f)
                        }
                        drawArc(
                            color = if (resolvedDone > 0) ringColor else colorScheme.primary.copy(alpha = 0.55f),
                            startAngle = startAngleDeg,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = if (resolvedTotal > 0) "$resolvedDone/$resolvedTotal" else "✓",
                        color = if (isComplete) EmeraldSuccess else colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// =========================================================================
// PREVIEWS (Tema Gelap & Tema Terang)
// =========================================================================

private val PreviewDarkColors = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = DeepObsidian,
    primaryContainer = CyanDark,
    onPrimaryContainer = CyanGlow,
    secondary = EmeraldSuccess,
    onSecondary = DeepObsidian,
    secondaryContainer = EmeraldDark,
    onSecondaryContainer = EmeraldSuccess,
    tertiary = CyanBright,
    background = DeepObsidian,
    onBackground = TextPrimary,
    surface = DarkNavySlate,
    onSurface = TextPrimary,
    surfaceVariant = CardSlate,
    onSurfaceVariant = TextSecondary,
    outline = CardSlateBorder,
    error = RoseDanger
)

private val PreviewLightColors = lightColorScheme(
    primary = CyanPrimary,
    onPrimary = DeepObsidian,
    secondary = EmeraldSuccess,
    onSecondary = DeepObsidian,
    tertiary = CyanBright
)

@Composable
private fun PathGalleryPreviewContent() {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionDivider(title = "Dasar")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PathNodeButton(NodeStatus.DONE, NodeKind.MATERI, "T1.1", onClick = {})
            PathNodeButton(NodeStatus.CURRENT, NodeKind.MATERI, "T1.2", onClick = {})
            PathNodeButton(NodeStatus.OPEN, NodeKind.TUJUAN, "T0", onClick = {})
            PathNodeButton(NodeStatus.LOCKED, NodeKind.MATERI, "T1.3", onClick = {})
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PathNodeButton(NodeStatus.OPEN, NodeKind.RINGKASAN, "T1.R", onClick = {})
            PathNodeButton(NodeStatus.OPEN, NodeKind.LAB, "Lab Spin", onClick = {})
            PathNodeButton(NodeStatus.OPEN, NodeKind.KUIS_MODUL, "Kuis", onClick = {})
            PathNodeButton(
                status = NodeStatus.CURRENT,
                kind = NodeKind.MODUL,
                label = "M01",
                progressPill = "3/23",
                onClick = {}
            )
        }
        PathConnector(fromX = 0.50f, toX = 0.72f, height = 40.dp, done = true)
        PathConnector(fromX = 0.72f, toX = 0.50f, height = 40.dp, done = false)
    }
}

@Preview(name = "Path Components - Dark", showBackground = true)
@Composable
private fun PathComponentsDarkPreview() {
    MaterialTheme(colorScheme = PreviewDarkColors) {
        PathGalleryPreviewContent()
    }
}

@Preview(name = "Path Components - Light", showBackground = true)
@Composable
private fun PathComponentsLightPreview() {
    MaterialTheme(colorScheme = PreviewLightColors) {
        PathGalleryPreviewContent()
    }
}

@Composable
private fun PathAllKindsMatrixContent() {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NodeKind.entries.forEach { kind ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = kind.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 10.sp,
                    modifier = Modifier.width(72.dp)
                )
                NodeStatus.entries.forEach { st ->
                    PathNodeButton(
                        status = st,
                        kind = kind,
                        label = if (kind == NodeKind.MODUL) "M01" else "T1",
                        progressPill = if (kind == NodeKind.MODUL) "1/5" else null,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Preview(name = "Path Node Statuses & Kinds - Dark", showBackground = true)
@Composable
private fun PathAllKindsMatrixDarkPreview() {
    MaterialTheme(colorScheme = PreviewDarkColors) {
        PathAllKindsMatrixContent()
    }
}

@Preview(name = "Path Node Statuses & Kinds - Light", showBackground = true)
@Composable
private fun PathAllKindsMatrixLightPreview() {
    MaterialTheme(colorScheme = PreviewLightColors) {
        PathAllKindsMatrixContent()
    }
}
