package com.example.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.NodeDisplay
import com.example.util.QuizSessionSummary

/**
 * Menyimpan jawaban sesi kuis terkini agar layar hasil tetap utuh ketika pengguna
 * membuka simpul lewat "Baca ulang" lalu menekan Back.
 */
object QuizSessionSnapshotHolder {
    var activeScopeKey: String = ""
    var sessionAnswers: Map<String, Boolean> = emptyMap()

    fun ensureScope(scopeKey: String) {
        if (activeScopeKey != scopeKey) {
            activeScopeKey = scopeKey
            sessionAnswers = emptyMap()
        }
    }

    fun record(quizId: String, isCorrect: Boolean) {
        sessionAnswers = sessionAnswers + (quizId to isCorrect)
    }

    fun clear() {
        sessionAnswers = emptyMap()
    }
}

@Composable
fun QuizResultSection(
    summary: QuizSessionSummary,
    moduleCode: String?,
    quizLevel: Int,
    onReadNodeAgain: (String) -> Unit,
    onRetryWrong: () -> Unit,
    onPrimaryNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sweepFraction = if (summary.totalQuestions > 0) {
        summary.correctCount.toFloat() / summary.totalQuestions.toFloat()
    } else {
        0f
    }
    val ringColor = if (summary.passed) CyanBright else AmberWarning
    val primaryNextLabel = if (moduleCode != null && quizLevel in 1..2) {
        "Lanjut ke Tingkat ${quizLevel + 1}"
    } else {
        "Selesai"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quiz_result_section"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cincin skor & bintang dengan deskripsi aksesibilitas gabungan
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription =
                    "${summary.correctCount} dari ${summary.totalQuestions} benar, ${summary.stars} bintang"
            }
        ) {
            Box(
                modifier = Modifier.size(124.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(124.dp)) {
                    val strokeW = 10.dp.toPx()
                    drawArc(
                        color = CardSlate,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * sweepFraction,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.correctCount}/${summary.totalQuestions}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "BENAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 1..3) {
                    Icon(
                        imageVector = if (i <= summary.stars) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (i <= summary.stars) AmberWarning else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (summary.passed) {
                    "Lulus! Penguasaan ${summary.percentage}%"
                } else {
                    "Belum lulus (${summary.percentage}%), coba lagi"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Batas lulus 70% · tingkat berikutnya tetap terbuka",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Tiga kartu statistik: Benar, Salah, XP
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuizStatMiniCard(
                label = "BENAR",
                value = "${summary.correctCount}",
                valueColor = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )
            QuizStatMiniCard(
                label = "SALAH",
                value = "${summary.wrongCount}",
                valueColor = RoseDanger,
                modifier = Modifier.weight(1f)
            )
            QuizStatMiniCard(
                label = "XP",
                value = "+${summary.earnedXp}",
                valueColor = AmberWarning,
                modifier = Modifier.weight(1f)
            )
        }

        // Daftar simpul yang perlu diulang
        if (summary.reviewNodes.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PERLU DIULANG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                summary.reviewNodes.forEach { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = DarkNavySlate,
                        border = BorderStroke(1.dp, CardSlateBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyanDark,
                                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = NodeDisplay.shortCode(item.nodeCode),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyanBright,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.nodeTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.wrongCount} soal salah",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            TextButton(
                                onClick = { onReadNodeAgain(item.nodeCode) },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("quiz_reread_${item.nodeCode}")
                            ) {
                                Text(
                                    text = "Baca ulang",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanBright
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tombol aksi bawah
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (summary.wrongQuizIds.isNotEmpty()) {
                OutlinedButton(
                    onClick = onRetryWrong,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("quiz_retry_wrong"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = "Ulangi ${summary.wrongCount} soal yang salah",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanBright
                    )
                }
            }
            Button(
                onClick = onPrimaryNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("quiz_result_primary_next"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text(
                    text = primaryNextLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepObsidian
                )
            }
        }
    }
}

@Composable
private fun QuizStatMiniCard(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = DarkNavySlate,
        border = BorderStroke(1.dp, CardSlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
        }
    }
}
