package com.example.ui.lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.lesson.InteractiveContent
import com.example.ui.character.Expression
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RevealChainCard(
    interactive: InteractiveContent,
    reduceMotion: Boolean,
    onNanoExpressionChange: (Expression, String?) -> Unit,
    onContinueLesson: () -> Unit,
    modifier: Modifier = Modifier,
    stateKey: String = interactive.title
) {
    val totalSteps = interactive.steps.size.coerceAtLeast(1)
    var revealedCount by rememberSaveable(stateKey) { mutableIntStateOf(1) }
    val selectedPredictions = remember(stateKey) { mutableStateMapOf<Int, Int>() }

    val allRevealed = revealedCount >= totalSteps
    val nextStepIndex = revealedCount
    val nextStep = interactive.steps.getOrNull(nextStepIndex)
    val nextPrediction = nextStep?.prediction

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = interactive.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            lineHeight = 28.sp
        )
        if (interactive.intro.isNotBlank()) {
            Text(
                text = interactive.intro,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }

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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lab Mini · ${interactive.title}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = CyanPrimary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "Prediksi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanBright,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Daftar langkah yang sudah terbuka
                for (stepIdx in 0 until revealedCount.coerceAtMost(totalSteps)) {
                    val step = interactive.steps[stepIdx]
                    val isNewest = stepIdx == revealedCount - 1
                    val predAnswer = selectedPredictions[stepIdx]
                    val pred = step.prediction

                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(if (reduceMotion) 0 else 220))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isNewest) CyanPrimary else CyanPrimary.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${stepIdx + 1}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepObsidian
                                    )
                                }
                                Text(
                                    text = step.text,
                                    fontSize = 14.5.sp,
                                    fontWeight = if (isNewest) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isNewest) TextPrimary else TextSecondary,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (pred != null && predAnswer != null) {
                                val wasCorrect = predAnswer == pred.correctIndex
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (wasCorrect) EmeraldSuccess.copy(alpha = 0.14f) else AmberWarning.copy(alpha = 0.14f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (wasCorrect) EmeraldSuccess.copy(alpha = 0.45f) else AmberWarning.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Text(
                                        text = if (wasCorrect) {
                                            "Betul! Prediksi langkah ${stepIdx + 1} tepat."
                                        } else {
                                            "Hampir, perhatikan penjelasan langkah ${stepIdx + 1} di atas."
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (wasCorrect) EmeraldSuccess else AmberWarning,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Placeholder langkah berikutnya yang belum dibuka
                if (!allRevealed && nextStep != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                drawCircle(
                                    color = TextMuted,
                                    style = Stroke(
                                        width = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                    )
                                )
                            }
                            Text(
                                text = "${nextStepIndex + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = "Ketuk \"Langkah berikutnya\" untuk membuka tahapan ini.",
                            fontSize = 13.5.sp,
                            color = TextMuted,
                            lineHeight = 19.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Chip prediksi sebelum langkah berikutnya dibuka
                    if (nextPrediction != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = CardSlate.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, CardSlateBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "TEBAK DULU LANGKAH ${nextStepIndex + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextSecondary,
                                    letterSpacing = 0.6.sp
                                )
                                if (nextPrediction.question.isNotBlank()) {
                                    Text(
                                        text = nextPrediction.question,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    nextPrediction.options.forEachIndexed { optIdx, optionLabel ->
                                        val isPicked = selectedPredictions[nextStepIndex] == optIdx
                                        Surface(
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(role = Role.Button) {
                                                    selectedPredictions[nextStepIndex] = optIdx
                                                    val isRight = optIdx == nextPrediction.correctIndex
                                                    if (isRight) {
                                                        onNanoExpressionChange(
                                                            Expression.SENANG,
                                                            "Betul! Ketuk Langkah berikutnya untuk membuka tahapan ke-${nextStepIndex + 1}."
                                                        )
                                                    } else {
                                                        onNanoExpressionChange(
                                                            Expression.BERPIKIR,
                                                            "Hampir! Ketuk Langkah berikutnya untuk melihat pembuktiannya."
                                                        )
                                                    }
                                                }
                                                .testTag("reveal_pred_option_$optIdx"),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isPicked) CyanPrimary.copy(alpha = 0.22f) else DarkNavySlate,
                                            border = BorderStroke(
                                                width = if (isPicked) 1.5.dp else 1.dp,
                                                color = if (isPicked) CyanBright else CardSlateBorder
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = optionLabel,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPicked) CyanBright else TextPrimary
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
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Baris kontrol bawah: Tombol Ulangi (kiri) & Langkah berikutnya / Lanjut (kanan)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    revealedCount = 1
                    selectedPredictions.clear()
                    onNanoExpressionChange(
                        Expression.NETRAL,
                        "Urutan dikembalikan ke langkah pertama."
                    )
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkNavySlate)
                    .border(1.dp, CardSlateBorder, CircleShape)
                    .testTag("reveal_chain_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Ulangi urutan langkah",
                    tint = TextSecondary
                )
            }

            Button(
                onClick = {
                    if (!allRevealed) {
                        revealedCount = (revealedCount + 1).coerceAtMost(totalSteps)
                        if (revealedCount >= totalSteps) {
                            onNanoExpressionChange(
                                Expression.BANGGA,
                                "Semua tahapan sudah terbuka! Ketuk Lanjut."
                            )
                        }
                    } else {
                        onContinueLesson()
                    }
                },
                modifier = Modifier
                    .defaultMinSize(minWidth = 176.dp, minHeight = 52.dp)
                    .testTag("reveal_chain_next_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = DeepObsidian
                ),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (allRevealed) "Lanjut" else "Langkah berikutnya",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepObsidian
                )
            }
        }
    }
}
