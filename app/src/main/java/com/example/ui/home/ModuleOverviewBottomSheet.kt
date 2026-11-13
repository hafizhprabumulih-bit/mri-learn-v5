package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ModuleOverviewSummary
import com.example.util.NodeDisplay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModuleOverviewBottomSheet(
    summary: ModuleOverviewSummary,
    onDismiss: () -> Unit,
    onContinuePath: () -> Unit,
    onOpenAllNodes: () -> Unit,
    onOpenFlashcards: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val module = summary.module

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
                .padding(bottom = 24.dp)
                .testTag("module_overview_sheet_${module.id}"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Text(
                        text = module.code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyanBright,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = "Bab ${summary.chapterNumber} · Modul ${summary.moduleIndexInCurriculum} dari ${summary.totalModules}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = module.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 24.sp
            )

            val overviewDesc = module.description.ifBlank { module.subtitle }
            if (overviewDesc.isNotBlank()) {
                Text(
                    text = overviewDesc,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OverviewMetaChip(text = "${summary.totalNodes} materi", borderColor = CyanPrimary)
                OverviewMetaChip(text = "${summary.totalQuizzes} kuis", borderColor = CyanPrimary)
                if (summary.hasSimulation) {
                    val simLabel = if (summary.totalLabCount > 0) {
                        "Simulasi dicoba: ${summary.triedLabCount}/${summary.totalLabCount}"
                    } else {
                        "Ada simulasi"
                    }
                    OverviewMetaChip(text = simLabel, borderColor = AmberWarning, textColor = AmberWarning)
                }
            }

            if (summary.levels.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.levels.forEach { lvl ->
                        val frac = if (lvl.totalNodes > 0) lvl.doneNodes.toFloat() / lvl.totalNodes else 0f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = lvl.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.weight(0.28f)
                            )
                            LinearProgressIndicator(
                                progress = { frac },
                                modifier = Modifier
                                    .weight(0.42f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (lvl.doneNodes >= lvl.totalNodes && lvl.totalNodes > 0) EmeraldSuccess else CyanBright,
                                trackColor = CardSlate
                            )
                            Text(
                                text = "${lvl.doneNodes}/${lvl.totalNodes} · ${lvl.quizCount} kuis",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.weight(0.30f)
                            )
                        }
                    }
                }
            }

            val prereqText = if (summary.crossModulePrerequisites.isEmpty()) {
                "Prasyarat: modul pertama / tanpa prasyarat."
            } else if (!summary.isRecommended) {
                "Disarankan: selesaikan ${summary.crossModulePrerequisites.joinToString(", ")} ≥ 60% lebih dulu (prasyarat lembut)."
            } else {
                "Prasyarat: ${summary.crossModulePrerequisites.joinToString(", ")}."
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (!summary.isRecommended) AmberWarning.copy(alpha = 0.12f) else DeepObsidian,
                border = BorderStroke(
                    1.dp,
                    if (!summary.isRecommended) AmberWarning.copy(alpha = 0.55f) else CardSlateBorder
                )
            ) {
                Text(
                    text = prereqText,
                    fontSize = 12.sp,
                    color = if (!summary.isRecommended) AmberWarning else TextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            if (summary.rewardItemName != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldDark.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "Hadiah modul: lencana ${module.code} + ${summary.rewardItemName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldSuccess,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            val primaryCtaText = when {
                summary.isComplete -> "Ulas modul"
                summary.isNotStarted && summary.currentNode != null ->
                    "Mulai: ${NodeDisplay.displayTitle(summary.currentNode)}"
                summary.currentNode != null ->
                    "Lanjutkan: ${NodeDisplay.displayTitle(summary.currentNode)}"
                else -> "Buka jalur modul"
            }

            Button(
                onClick = onContinuePath,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("module_overview_continue"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text(
                    text = primaryCtaText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepObsidian,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenAllNodes,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardSlateBorder)
                ) {
                    Text(
                        text = "Semua materi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanBright
                    )
                }
                OutlinedButton(
                    onClick = onOpenFlashcards,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardSlateBorder)
                ) {
                    Text(
                        text = "Flashcards",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanBright
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewMetaChip(
    text: String,
    borderColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color = CyanBright
) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = DeepObsidian,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f))
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
