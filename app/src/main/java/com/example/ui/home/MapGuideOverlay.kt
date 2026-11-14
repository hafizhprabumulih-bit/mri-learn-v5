package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.path.NodeKind
import com.example.ui.path.PathNodeButton
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.NodeStatus
import kotlin.math.roundToInt

/**
 * Lapisan coach mark Langkah 1/2 yang menyorot kotak modul aktif pada kunjungan pertama.
 */
@Composable
fun MapGuideCoachOverlay(
    targetBounds: Rect,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    stepBadge: String = "1 DARI 6",
    title: String = "Mulai dari sini",
    description: String = "Ketuk kotak modul untuk membuka jalur materinya."
) {
    BackHandler(onBack = onSkip)

    var overlayOffsetInRoot by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                overlayOffsetInRoot = coords.positionInRoot()
            }
            .background(DeepObsidian.copy(alpha = 0.82f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("map_guide_spotlight")
    ) {
        val density = LocalDensity.current
        val paddingPx = with(density) { 8.dp.toPx() }

        // Konversi koordinat global targetBounds ke koordinat lokal lapisan overlay
        val localTargetLeft = targetBounds.left - overlayOffsetInRoot.x
        val localTargetTop = targetBounds.top - overlayOffsetInRoot.y
        val localTargetBottom = targetBounds.bottom - overlayOffsetInRoot.y

        val ringLeft = (localTargetLeft - paddingPx).roundToInt().coerceAtLeast(0)
        val ringTop = (localTargetTop - paddingPx).roundToInt().coerceAtLeast(0)
        val ringWidthDp = with(density) { (targetBounds.width + paddingPx * 2).toDp() }
        val ringHeightDp = with(density) { (targetBounds.height + paddingPx * 2).toDp() }

        val containerHeightPx = with(density) { maxHeight.toPx() }
        val estimatedCardHeightPx = with(density) { 174.dp.toPx() }
        val gapPx = with(density) { 12.dp.toPx() }
        val safeBottomMarginPx = with(density) { 20.dp.toPx() }
        val safeTopMarginPx = with(density) { 16.dp.toPx() }

        // Tempatkan kartu di ATAS cincin sorotan bila posisi target berada di paruh bawah layar
        // sehingga tidak pernah tertutup oleh tab bar bawah.
        val belowTop = localTargetBottom + gapPx
        val aboveTop = localTargetTop - estimatedCardHeightPx - gapPx
        val bubbleTopPx = if (belowTop + estimatedCardHeightPx > containerHeightPx - safeBottomMarginPx) {
            aboveTop.coerceIn(safeTopMarginPx, (containerHeightPx - estimatedCardHeightPx - safeBottomMarginPx).coerceAtLeast(safeTopMarginPx))
        } else {
            belowTop.coerceIn(safeTopMarginPx, (containerHeightPx - estimatedCardHeightPx - safeBottomMarginPx).coerceAtLeast(safeTopMarginPx))
        }.roundToInt()

        // Cincin sorotan tepat di sekitar kotak modul target
        Box(
            modifier = Modifier
                .offset { IntOffset(ringLeft, ringTop) }
                .size(width = ringWidthDp, height = ringHeightDp)
                .border(3.dp, CyanBright, RoundedCornerShape(24.dp))
                .clickable(onClick = onNext)
        )

        // Kartu petunjuk dengan posisi adaptif (aman dari bottom navigation bar)
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .offset { IntOffset(0, bubbleTopPx) }
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CyanBright,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stepBadge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepObsidian.copy(alpha = 0.75f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepObsidian
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = DeepObsidian.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(
                            text = "Lewati",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepObsidian
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onNext,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepObsidian)
                    ) {
                        Text(
                            text = "Lanjut",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanBright
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sheet legenda Langkah 2/2 ("Cara membaca peta") yang memakai komponen kotak yang sama dengan peta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLegendBottomSheet(
    showStepBadge: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkNavySlate,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cara membaca peta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (showStepBadge) {
                    Text(
                        text = "2 DARI 2",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            LegendNodeRow(
                title = "Aktif",
                description = "Lanjutkan belajar dari materi ini",
                status = NodeStatus.CURRENT,
                kind = NodeKind.MATERI,
                accentColor = CyanBright
            )
            LegendNodeRow(
                title = "Selesai",
                description = "Materi sudah dipelajari",
                status = NodeStatus.DONE,
                kind = NodeKind.MATERI,
                accentColor = EmeraldSuccess
            )
            LegendNodeRow(
                title = "Terkunci",
                description = "Selesaikan materi sebelumnya, atau lompat dengan tes singkat",
                status = NodeStatus.LOCKED,
                kind = NodeKind.MATERI,
                accentColor = CyanBright
            )
            LegendNodeRow(
                title = "Ringkasan",
                description = "Rangkuman satu tingkat materi",
                status = NodeStatus.OPEN,
                kind = NodeKind.RINGKASAN,
                accentColor = CyanBright
            )
            LegendLabBadgeRow()

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("map_legend_confirm"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text(
                    text = "Mengerti",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepObsidian
                )
            }
        }
    }
}

@Composable
private fun LegendNodeRow(
    title: String,
    description: String,
    status: NodeStatus,
    kind: NodeKind,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.scale(0.42f)) {
                PathNodeButton(
                    status = status,
                    kind = kind,
                    label = title,
                    onClick = {},
                    accentColor = accentColor,
                    semanticLabel = title
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun LegendLabBadgeRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = DarkNavySlate,
                border = BorderStroke(1.5.dp, AmberWarning)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ada simulasi",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Lencana labu: bisa dicoba di laboratorium",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
