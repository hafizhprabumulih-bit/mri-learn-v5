package com.example.ui.path

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MriModule
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlateBorder
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ModuleCompletedScreen(
    module: MriModule,
    doneNodes: Int,
    totalNodes: Int,
    quizMasteryPercent: Int,
    moduleBonusXp: Int,
    unlockedItemName: String?,
    nextRecommendedModule: MriModule?,
    onEquipItem: () -> Unit,
    onContinueNextModule: (MriModule) -> Unit,
    onReturnToPath: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onReturnToPath)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepObsidian, DarkNavySlate, DeepObsidian)
                )
            )
            .testTag("module_completed_screen_${module.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Lencana emas modul yang digambar dengan Compose
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .background(AmberWarning.copy(alpha = 0.16f))
                        .border(3.dp, AmberWarning, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = module.code,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberWarning
                        )
                    }
                }

                Text(
                    text = "MODUL TUNTAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AmberWarning,
                    letterSpacing = 1.4.sp
                )
                Text(
                    text = "Lencana ${module.code} diraih!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = module.title,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModuleStatBox(
                        label = "SIMPUL",
                        value = "$doneNodes/$totalNodes",
                        valueColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    ModuleStatBox(
                        label = "KUIS",
                        value = "$quizMasteryPercent%",
                        valueColor = CyanBright,
                        modifier = Modifier.weight(1f)
                    )
                    ModuleStatBox(
                        label = "BONUS XP",
                        value = "+$moduleBonusXp",
                        valueColor = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (unlockedItemName != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = EmeraldDark.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ITEM KARAKTER TERBUKA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldSuccess,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = unlockedItemName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            TextButton(
                                onClick = onEquipItem,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(
                                    text = "Pasang",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanBright
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkNavySlate,
                    border = BorderStroke(1.dp, CardSlateBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (nextRecommendedModule != null) {
                                "DISARANKAN BERIKUTNYA"
                            } else {
                                "KURIKULUM TUNTAS"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextSecondary,
                            letterSpacing = 0.9.sp
                        )
                        if (nextRecommendedModule != null) {
                            Text(
                                text = "${nextRecommendedModule.code} · ${nextRecommendedModule.title}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${nextRecommendedModule.nodes.size} simpul · melanjutkan jalur kurikulum",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = "Selamat! Seluruh modul kurikulum MRI telah tuntas.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (nextRecommendedModule != null) {
                    Button(
                        onClick = { onContinueNextModule(nextRecommendedModule) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("module_completed_next"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text(
                            text = "Lanjut ke ${nextRecommendedModule.code}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepObsidian
                        )
                    }
                }
                TextButton(
                    onClick = onReturnToPath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("module_completed_return_path")
                ) {
                    Text(
                        text = "Kembali ke peta",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleStatBox(
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
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
        }
    }
}
