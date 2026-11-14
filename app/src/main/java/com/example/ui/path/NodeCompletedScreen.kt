package com.example.ui.path

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CharacterCustomizationState
import com.example.data.MriModule
import com.example.data.MriNode
import com.example.ui.character.AvatarView
import com.example.ui.character.Expression
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CharacterUnlockProgress
import com.example.util.NodeDisplay
import com.example.util.XpSummary

@Composable
fun NodeCompletedScreen(
    module: MriModule,
    completedNode: MriNode,
    nextNode: MriNode?,
    earnedXp: Int,
    xpSummary: XpSummary,
    streakDays: Int,
    nodeCorrectQuizzes: Int,
    nodeTotalQuizzes: Int,
    unlockPreview: CharacterUnlockProgress?,
    onLaunchLab: (String) -> Unit,
    onContinueNext: () -> Unit,
    onReturnToPath: () -> Unit,
    characterCustomization: CharacterCustomizationState = CharacterCustomizationState(),
    doneNodesInModule: Int = 1,
    totalNodesInModule: Int = module.nodes.size.coerceAtLeast(1),
    masteryStars: Int = 2,
    starCaptionText: String = "2 bintang: soal cerita benar di percobaan pertama",
    recapBullets: List<String> = emptyList(),
    addToDailyReviewChecked: Boolean = false,
    onToggleDailyReview: (Boolean) -> Unit = {},
    onReplayLesson: (() -> Unit)? = null
) {
    val haptics = com.example.util.rememberMriHaptics()
    val reduceMotion = rememberReduceMotion()
    var revealed by remember(completedNode.code) { mutableStateOf(reduceMotion) }

    LaunchedEffect(completedNode.code, reduceMotion) {
        haptics.completionBuzz()
        revealed = true
    }

    val badgeScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.72f,
        animationSpec = if (reduceMotion) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "completedBadgeScale"
    )

    val effectiveRecap = remember(recapBullets, completedNode) {
        if (recapBullets.isNotEmpty()) {
            recapBullets
        } else {
            buildList {
                completedNode.learningGoal?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
                completedNode.summaryShort.trim().takeIf { it.isNotEmpty() && it !in this }?.let { add(it) }
                completedNode.analogi.trim().takeIf { it.isNotEmpty() && size < 3 }?.let { add(it) }
                if (isEmpty()) {
                    add("Memahami konsep inti ${NodeDisplay.displayTitle(completedNode)}.")
                }
            }.take(3)
        }
    }

    Dialog(
        onDismissRequest = onReturnToPath,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler {
            onReturnToPath()
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("node_completed_screen_${completedNode.code}"),
            color = DeepObsidian
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Maskot Nano dengan Pendaran Radial di Belakang
                        Box(
                            modifier = Modifier
                                .size(124.dp)
                                .graphicsLayer {
                                    scaleX = badgeScale
                                    scaleY = badgeScale
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                EmeraldSuccess.copy(alpha = 0.28f),
                                                CyanPrimary.copy(alpha = 0.12f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            AvatarView(
                                role = characterCustomization.avatarId,
                                equipped = characterCustomization,
                                expression = Expression.BANGGA,
                                modifier = Modifier.size(96.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${NodeDisplay.shortCode(completedNode.code)} · ${NodeDisplay.displayTitle(completedNode).uppercase()} SELESAI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanBright,
                            letterSpacing = 0.6.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Mantap, kamu paham!",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Baris 3 Bintang Penguasaan
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (starIdx in 1..3) {
                                val filled = starIdx <= masteryStars.coerceIn(1, 3)
                                Icon(
                                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = if (filled) "Bintang $starIdx tercapai" else "Bintang $starIdx belum tercapai",
                                    tint = if (filled) AmberWarning else CardSlateBorder,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = starCaptionText,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3 Kartu Metrik: XP, SIMPUL, STREAK (sesuai Mockup 4)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CompletedMetricBox(
                                label = "XP",
                                value = if (earnedXp > 0) "+$earnedXp" else "+0",
                                valueColor = AmberWarning,
                                modifier = Modifier.weight(1f)
                            )
                            CompletedMetricBox(
                                label = "SIMPUL",
                                value = "$doneNodesInModule/$totalNodesInModule",
                                valueColor = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            CompletedMetricBox(
                                label = "STREAK",
                                value = "$streakDays hari",
                                valueColor = AmberWarning,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Kartu "YANG KAMU PELAJARI"
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = DarkNavySlate,
                            border = BorderStroke(1.dp, CardSlateBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "YANG KAMU PELAJARI",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextSecondary,
                                    letterSpacing = 0.6.sp
                                )

                                effectiveRecap.forEach { bullet ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(18.dp)
                                        )
                                        Text(
                                            text = bullet,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Kartu Switch "Tambahkan ke Ulas Hari Ini"
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(role = Role.Switch) {
                                    haptics.lightToggle()
                                    onToggleDailyReview(!addToDailyReviewChecked)
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = DarkNavySlate,
                            border = BorderStroke(1.dp, CardSlateBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp)
                                ) {
                                    Text(
                                        text = "Tambahkan ke Ulas Hari Ini",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Diulang beberapa hari lagi agar tidak lupa",
                                        fontSize = 11.5.sp,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = addToDailyReviewChecked,
                                    onCheckedChange = { checked ->
                                        haptics.lightToggle()
                                        onToggleDailyReview(checked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DeepObsidian,
                                        checkedTrackColor = CyanBright,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = CardSlate
                                    )
                                )
                            }
                        }

                        if (!completedNode.labType.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkNavySlate),
                                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = CyanDark,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Science,
                                                contentDescription = null,
                                                tint = CyanBright,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Lab Simulasi Terbuka",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Uji langsung konsep ${completedNode.code} di simulator interaktif.",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onLaunchLab(completedNode.labType) },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, CyanBright)
                                    ) {
                                        Text(
                                            text = "Coba Lab",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanBright
                                        )
                                    }
                                }
                            }
                        }

                        if (unlockPreview != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkNavySlate),
                                border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.55f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CardGiftcard,
                                            contentDescription = null,
                                            tint = AmberAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Hadiah Karakter Berikutnya",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = unlockPreview.item.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberAccent
                                    )
                                    Text(
                                        text = "${unlockPreview.moduleTitle} (${unlockPreview.completedInModule}/${unlockPreview.requiredCount} simpul)",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { unlockPreview.fraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = AmberAccent,
                                        trackColor = CardSlate
                                    )
                                }
                            }
                        }
                    }

                    // Tombol Aksi Bawah (Mockup 4)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = if (nextNode != null) onContinueNext else onReturnToPath,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 54.dp)
                                .testTag("completed_continue_next"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPrimary,
                                contentColor = DeepObsidian
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = if (nextNode != null) {
                                    "Lanjut: ${NodeDisplay.displayTitle(nextNode)}"
                                } else {
                                    "Kembali ke Peta ${module.code}"
                                },
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepObsidian,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onReplayLesson != null) {
                                TextButton(
                                    onClick = onReplayLesson,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("completed_replay_lesson")
                                ) {
                                    Text(
                                        text = "Ulangi pelajaran ini",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }
                            }
                            if (nextNode != null) {
                                TextButton(
                                    onClick = onReturnToPath,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("completed_return_to_path")
                                ) {
                                    Text(
                                        text = "Kembali ke peta",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMuted
                                    )
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
private fun CompletedMetricBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = DarkNavySlate,
        border = BorderStroke(1.dp, CardSlateBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextSecondary,
                letterSpacing = 0.6.sp
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
