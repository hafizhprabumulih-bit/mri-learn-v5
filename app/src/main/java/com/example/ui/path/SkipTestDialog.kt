package com.example.ui.path

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.RoseDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.QuizShuffle
import com.example.util.SkipTestBuilder
import com.example.util.SkipTestPlan

/**
 * Dialog tes singkat untuk melompati simpul prasyarat menuju simpul target yang terkunci.
 */
@Composable
fun SkipTestDialog(
    plan: SkipTestPlan,
    onTestPassed: (List<String>) -> Unit,
    onOpenTargetNode: () -> Unit,
    onDismiss: () -> Unit
) {
    var retryRound by remember(plan.targetNodeCode) { mutableIntStateOf(0) }
    val shuffledQuestions = remember(plan, retryRound) {
        if (retryRound == 0) plan.questions else plan.questions.map { QuizShuffle.shuffled(it) }
    }

    var currentIndex by remember(plan.targetNodeCode, retryRound) { mutableIntStateOf(0) }
    var selectedOption by remember(plan.targetNodeCode, retryRound, currentIndex) { mutableStateOf<Int?>(null) }
    var submitted by remember(plan.targetNodeCode, retryRound, currentIndex) { mutableStateOf(false) }
    var correctCount by remember(plan.targetNodeCode, retryRound) { mutableIntStateOf(0) }
    var finished by remember(plan.targetNodeCode, retryRound) { mutableStateOf(false) }
    var persistedPass by remember(plan.targetNodeCode, retryRound) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = DarkNavySlate,
            border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyanDark,
                            border = BorderStroke(1.dp, CyanPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = null,
                                tint = CyanBright,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Tes Lompat Simpul",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Menuju ${plan.targetNodeCode} • Min. ${plan.requiredCorrect}/${shuffledQuestions.size} benar",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup dialog tes lompat",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!finished && shuffledQuestions.isNotEmpty()) {
                    val q = shuffledQuestions[currentIndex]
                    val progressFraction = (currentIndex + 1).toFloat() / shuffledQuestions.size.toFloat()

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyanBright,
                        trackColor = CardSlate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Soal ${currentIndex + 1}/${shuffledQuestions.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanBright
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DeepObsidian
                        ) {
                            Text(
                                text = q.nodeCode,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = q.question,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        q.options.forEachIndexed { idx, optionText ->
                            val isSelected = selectedOption == idx
                            val isCorrectOption = idx == q.correctIndex

                            val borderColor = when {
                                submitted && isCorrectOption -> EmeraldSuccess
                                submitted && isSelected && !isCorrectOption -> RoseDanger
                                isSelected -> CyanBright
                                else -> CardSlateBorder
                            }
                            val bgColor = when {
                                submitted && isCorrectOption -> EmeraldDark.copy(alpha = 0.7f)
                                submitted && isSelected && !isCorrectOption -> RoseDark.copy(alpha = 0.7f)
                                isSelected -> CyanDark.copy(alpha = 0.55f)
                                else -> CardSlate
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !submitted) {
                                        selectedOption = idx
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = bgColor,
                                border = BorderStroke(1.2.dp, borderColor)
                            ) {
                                Text(
                                    text = optionText,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }

                    if (submitted && q.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DeepObsidian),
                            border = BorderStroke(1.dp, CardSlateBorder)
                        ) {
                            Text(
                                text = q.explanation,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!submitted) {
                        Button(
                            onClick = {
                                val chosen = selectedOption ?: return@Button
                                submitted = true
                                if (chosen == q.correctIndex) {
                                    correctCount++
                                }
                            },
                            enabled = selectedOption != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("skip_test_submit_answer"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text(
                                text = "Periksa Jawaban",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepObsidian
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (currentIndex + 1 < shuffledQuestions.size) {
                                    currentIndex++
                                } else {
                                    finished = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("skip_test_next"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text(
                                text = if (currentIndex + 1 < shuffledQuestions.size) {
                                    "Soal Berikutnya"
                                } else {
                                    "Lihat Hasil Tes"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepObsidian
                            )
                        }
                    }
                } else {
                    val passed = SkipTestBuilder.isPassing(
                        correctCount = correctCount,
                        totalQuestions = shuffledQuestions.size
                    )

                    LaunchedEffect(finished, passed) {
                        if (finished && passed && !persistedPass) {
                            persistedPass = true
                            onTestPassed(plan.nodesToSkip.map { it.code })
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (passed) EmeraldDark else RoseDark.copy(alpha = 0.55f)
                        ),
                        border = BorderStroke(
                            1.2.dp,
                            if (passed) EmeraldSuccess else RoseDanger
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (passed) EmeraldSuccess else RoseDanger,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = if (passed) {
                                        "Lulus Tes Singkat ($correctCount/${shuffledQuestions.size})"
                                    } else {
                                        "Belum Lulus ($correctCount/${shuffledQuestions.size})"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = if (passed) {
                                    "${plan.nodesToSkip.size} simpul prasyarat ditandai dilewati via tes (${plan.targetNodeCode} kini terbuka). Simpul yang dilewati tidak memberi XP sampai Anda membacanya langsung."
                                } else {
                                    "Diperlukan minimal ${plan.requiredCorrect} jawaban benar untuk melompat ke ${plan.targetNodeCode}. Anda bisa mencoba lagi atau membaca simpul aktif terlebih dahulu."
                                },
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (passed) {
                        Button(
                            onClick = onOpenTargetNode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("skip_test_open_target"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                        ) {
                            Text(
                                text = "Buka ${plan.targetNodeCode}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepObsidian
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { retryRound++ },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CyanPrimary)
                            ) {
                                Text(
                                    text = "Coba Lagi",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyanBright
                                )
                            }
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CardSlate)
                            ) {
                                Text(
                                    text = "Tutup",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
