package com.example.ui.character

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.CharacterAccentPalette
import com.example.data.CharacterCatalog
import com.example.data.CharacterCustomizationState
import com.example.data.MedicalAvatar
import com.example.ui.theme.CharAccentCyan
import com.example.ui.theme.CharAccentMint
import com.example.ui.theme.CharActionDarkText
import com.example.ui.theme.CharActionOrange
import com.example.ui.theme.CharBorderSlate
import com.example.ui.theme.CharCardSurface
import com.example.ui.theme.CharStageBase
import com.example.ui.theme.CharStageDeep
import com.example.ui.theme.CharTextPrimary
import com.example.ui.theme.CharTextSecondary
import java.io.File
import java.io.FileOutputStream

private fun expressionLabel(exp: Expression): String = when (exp) {
    Expression.NETRAL -> "Netral"
    Expression.SENANG -> "Senang"
    Expression.BERPIKIR -> "Berpikir"
    Expression.WASPADA -> "Waspada"
    Expression.BANGGA -> "Bangga"
    Expression.SEDIH -> "Sedih"
}

private enum class StudioCategory(val label: String) {
    TEMA("Tema ID"),
    EKSPRESI("Ekspresi"),
    LATAR("Latar Studio"),
    PANGKAT("Pangkat")
}

@Composable
private fun StudioTogglePill(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) selectedColor else CharCardSurface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) selectedColor else CharBorderSlate
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = CharActionDarkText,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (selected) CharActionDarkText else CharTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Dialog Kartu Status Shift Nano (dibuka saat menekan & menahan avatar Nano / Long Press).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NanoShiftStatusDialog(
    avatarLabel: String,
    specialistRole: String,
    userName: String,
    completedCount: Int,
    totalNodesCount: Int,
    studyStreakDays: Int,
    shiftMood: Int,
    favoriteSequence: String,
    activeExpression: Expression,
    onSelectExpression: (Expression) -> Unit,
    onDismiss: () -> Unit
) {
    val masteryPct = if (totalNodesCount > 0) (completedCount * 100) / totalNodesCount else 0
    val displayName = userName.ifBlank { avatarLabel }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CharStageBase),
            border = BorderStroke(1.5.dp, CharAccentCyan.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = CharAccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Kartu Status Shift Nano",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CharTextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = CharTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CharCardSurface,
                    border = BorderStroke(1.dp, CharBorderSlate)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$displayName · $specialistRole",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharAccentCyan
                        )
                        Text(
                            text = "Sekuens Favorit: $favoriteSequence",
                            fontSize = 11.sp,
                            color = CharTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Penguasaan Materi: $completedCount/$totalNodesCount ($masteryPct%)",
                                fontSize = 11.sp,
                                color = CharTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (masteryPct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(99.dp)),
                            color = CharAccentMint,
                            trackColor = CharStageDeep
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = CharStageDeep
                            ) {
                                Text(
                                    text = "Streak: $studyStreakDays hari",
                                    fontSize = 11.sp,
                                    color = if (studyStreakDays > 0) CharAccentMint else CharActionOrange,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = CharStageDeep
                            ) {
                                Text(
                                    text = "Mood Shift: $shiftMood ♥",
                                    fontSize = 11.sp,
                                    color = CharAccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Geser untuk Ubah Ekspresi Visor:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharTextPrimary
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(Expression.entries) { exp ->
                        StudioTogglePill(
                            label = expressionLabel(exp),
                            selected = exp == activeExpression,
                            selectedColor = CharAccentCyan,
                            onClick = { onSelectExpression(exp) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mode Foto / Studio Kartu Profil Radiografer:
 * Menggunakan kategori toggle yang digeser (LazyRow) dan pilihan hanya muncul ketika kategori ditekan
 * sehingga tampilan ringkas dan tidak bertumpuk.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NanoSnapshotStudioDialog(
    workingState: CharacterCustomizationState,
    currentAvatar: MedicalAvatar,
    specialistRole: String,
    favoriteSequence: String,
    userName: String,
    accentPalette: CharacterAccentPalette,
    completedCount: Int,
    totalNodesCount: Int,
    onDismiss: () -> Unit,
    onExported: (String) -> Unit
) {
    val context = LocalContext.current
    var studioExpression by remember { mutableStateOf(Expression.BANGGA) }
    var studioBgId by remember { mutableStateOf(workingState.backgroundId) }
    val masteryPct = if (totalNodesCount > 0) (completedCount * 100) / totalNodesCount else 0
    val defaultRank = when {
        currentAvatar.id == "dosen" -> "Dosen Pembimbing"
        masteryPct >= 75 -> "Ahli Fisika MR"
        masteryPct >= 35 -> "Radiografer Klinis"
        else -> "Mahasiswa MRI"
    }
    val rankOptions = listOf(
        "Mahasiswa MRI",
        "Radiografer Klinis",
        "Ahli Fisika MR",
        "Dosen Pembimbing",
        "Kepala Fisikawan 7T"
    )
    var selectedRank by remember { mutableStateOf(defaultRank) }
    val idThemes = listOf("Hologram", "Gelap", "Film X-Ray")
    var selectedIdTheme by remember { mutableStateOf("Hologram") }

    // Kategori aktif yang ditekan pengguna (null = tertutup, ditekan baru muncul pilihannya)
    var expandedCategory by remember { mutableStateOf<StudioCategory?>(null) }

    val studioLoadout = remember(workingState, studioBgId) {
        workingState.copy(backgroundId = studioBgId)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CharStageBase),
            border = BorderStroke(1.5.dp, accentPalette.primaryColor.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Studio Kartu Profil Radiografer",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CharTextPrimary
                        )
                        Text(
                            text = "Ketuk kategori di bawah untuk mengubah tampilan kartu ID",
                            fontSize = 11.sp,
                            color = CharTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Studio",
                            tint = CharTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Pratinjau Kartu ID Radiografer
                val themeBorder = when (selectedIdTheme) {
                    "Hologram" -> Color(0xFF3CCDEE)
                    "Film X-Ray" -> Color(0xFF94A3B8)
                    else -> accentPalette.primaryColor
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.24f)
                        .clip(RoundedCornerShape(18.dp))
                        .border(2.dp, themeBorder, RoundedCornerShape(18.dp))
                ) {
                    StageBackgroundCanvas(
                        backgroundId = studioBgId,
                        accentPalette = accentPalette,
                        glowPulse = 0.95f,
                        rfWavePhase = 0.35f,
                        isWaveActive = selectedIdTheme == "Hologram"
                    )
                    if (selectedIdTheme == "Hologram") {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            Color(0x331FB6E8),
                                            Color(0x1119E6C3),
                                            Color(0x28FFD166),
                                            Color(0x221FB6E8)
                                        )
                                    )
                                )
                        )
                    } else if (selectedIdTheme == "Film X-Ray") {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x66020617))
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CharStageDeep.copy(alpha = 0.88f),
                                border = BorderStroke(1.dp, themeBorder.copy(alpha = 0.85f))
                            ) {
                                Text(
                                    text = "ID · $selectedRank",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentPalette.glowColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CharStageDeep.copy(alpha = 0.82f)
                            ) {
                                Text(
                                    text = "MRI EDU · $masteryPct%",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharTextPrimary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        AvatarView(
                            role = currentAvatar.id,
                            equipped = studioLoadout,
                            expression = studioExpression,
                            modifier = Modifier.size(134.dp)
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = CharStageDeep.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, themeBorder.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = userName.ifBlank { currentAvatar.label },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CharTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$specialistRole · $favoriteSequence",
                                    fontSize = 9.5.sp,
                                    color = CharTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Baris Toggle Kategori yang Bisa Digeser (Slide) — ditekan baru muncul pilihannya
                val currentBgName = CharacterCatalog.getItem(studioBgId)?.name ?: "Ruang Kontrol"
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(StudioCategory.entries) { cat ->
                        val isExpanded = expandedCategory == cat
                        val summaryValue = when (cat) {
                            StudioCategory.TEMA -> selectedIdTheme
                            StudioCategory.EKSPRESI -> expressionLabel(studioExpression)
                            StudioCategory.LATAR -> currentBgName
                            StudioCategory.PANGKAT -> selectedRank
                        }
                        Surface(
                            onClick = {
                                expandedCategory = if (isExpanded) null else cat
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isExpanded) accentPalette.primaryColor else CharCardSurface,
                            border = BorderStroke(
                                width = if (isExpanded) 1.6.dp else 1.dp,
                                color = if (isExpanded) accentPalette.primaryColor else CharBorderSlate
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = cat.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpanded) CharActionDarkText.copy(alpha = 0.78f) else CharTextSecondary
                                    )
                                    Text(
                                        text = summaryValue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isExpanded) CharActionDarkText else CharTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = if (isExpanded) CharActionDarkText else CharAccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Panel Opsi Geser (LazyRow) yang hanya muncul saat kategori di atas ditekan
                AnimatedVisibility(visible = expandedCategory != null) {
                    val activeCat = expandedCategory
                    if (activeCat != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CharStageDeep.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, accentPalette.primaryColor.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Geser untuk memilih ${activeCat.label}:",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentPalette.glowColor
                                    )
                                    Text(
                                        text = "Tutup ▲",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CharTextSecondary,
                                        modifier = Modifier.clickable { expandedCategory = null }
                                    )
                                }

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    when (activeCat) {
                                        StudioCategory.TEMA -> {
                                            items(idThemes) { theme ->
                                                StudioTogglePill(
                                                    label = theme,
                                                    selected = theme == selectedIdTheme,
                                                    selectedColor = accentPalette.primaryColor,
                                                    onClick = { selectedIdTheme = theme }
                                                )
                                            }
                                        }
                                        StudioCategory.EKSPRESI -> {
                                            items(Expression.entries) { exp ->
                                                StudioTogglePill(
                                                    label = expressionLabel(exp),
                                                    selected = exp == studioExpression,
                                                    selectedColor = accentPalette.primaryColor,
                                                    onClick = { studioExpression = exp }
                                                )
                                            }
                                        }
                                        StudioCategory.LATAR -> {
                                            items(CharacterCatalog.backgroundItems) { bg ->
                                                StudioTogglePill(
                                                    label = bg.name,
                                                    selected = bg.id == studioBgId,
                                                    selectedColor = accentPalette.primaryColor,
                                                    onClick = { studioBgId = bg.id }
                                                )
                                            }
                                        }
                                        StudioCategory.PANGKAT -> {
                                            items(rankOptions) { rank ->
                                                StudioTogglePill(
                                                    label = rank,
                                                    selected = rank == selectedRank,
                                                    selectedColor = CharAccentMint,
                                                    onClick = { selectedRank = rank }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        shareAndSaveProfileCard(
                            context = context,
                            equipped = studioLoadout,
                            expression = studioExpression,
                            displayName = userName.ifBlank { currentAvatar.label },
                            specialistRole = specialistRole,
                            favoriteSequence = favoriteSequence,
                            rankBadge = selectedRank,
                            masteryPct = masteryPct,
                            themeName = selectedIdTheme,
                            accentColorArgb = accentPalette.primaryColor.toArgb()
                        )
                        onExported("Kartu profil ${currentAvatar.label} ($selectedRank) siap dibagikan!")
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentPalette.primaryColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = CharActionDarkText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simpan & Bagikan Kartu PNG",
                        color = CharActionDarkText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private fun shareAndSaveProfileCard(
    context: Context,
    equipped: CharacterCustomizationState,
    expression: Expression,
    displayName: String,
    specialistRole: String,
    favoriteSequence: String,
    rankBadge: String,
    masteryPct: Int,
    themeName: String,
    accentColorArgb: Int
) {
    val width = 800
    val height = 960
    val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(outBitmap)

    canvas.drawColor(0xFF071A2B.toInt())

    // Gambar Latar
    val bgRes = CharacterCatalog.getItem(equipped.backgroundId)?.drawableRes ?: R.drawable.bg_control_room
    BitmapFactory.decodeResource(context.resources, bgRes)?.let { bgBmp ->
        val scale = maxOf(width.toFloat() / bgBmp.width, height.toFloat() / bgBmp.height)
        val drawW = bgBmp.width * scale
        val drawH = bgBmp.height * scale
        val left = (width - drawW) / 2f
        val top = height - drawH
        canvas.drawBitmap(bgBmp, null, RectF(left, top, left + drawW, top + drawH), Paint(Paint.FILTER_BITMAP_FLAG))
        bgBmp.recycle()
    }

    // Vignette sesuai tema kartu ID
    val overlayColor = when (themeName) {
        "Film X-Ray" -> 0xA8020617.toInt()
        "Hologram" -> 0x550A253B
        else -> 0x7A071A2B
    }
    val overlayPaint = Paint().apply { color = overlayColor }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

    val frameColor = when (themeName) {
        "Film X-Ray" -> 0xFF94A3B8.toInt()
        "Hologram" -> 0xFF3CCDEE.toInt()
        else -> accentColorArgb
    }

    // Bingkai Kartu
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = frameColor
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawRoundRect(RectF(16f, 16f, width - 16f, height - 16f), 36f, 36f, borderPaint)

    // Panggung Elips
    val ovalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCC0C2640.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawOval(RectF(170f, 700f, 630f, 748f), ovalPaint)
    canvas.drawOval(RectF(170f, 700f, 630f, 748f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = frameColor
        style = Paint.Style.STROKE
        strokeWidth = 3f
    })

    // Tumpuk seluruh layer PNG Nano sesuai zOrder
    val layers = AvatarLayerRegistry.buildActiveLayers(equipped, expression)
    val avatarSide = 520f
    val avatarLeft = (width - avatarSide) / 2f
    val avatarTop = 210f
    val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    layers.forEach { layer ->
        val resId = layer.drawableRes ?: return@forEach
        BitmapFactory.decodeResource(context.resources, resId)?.let { bmp ->
            val layerSize = avatarSide * layer.anchor.sizeFraction
            val lx = avatarLeft + (avatarSide * layer.anchor.x) - (layerSize / 2f)
            val ly = avatarTop + (avatarSide * layer.anchor.y) - (layerSize / 2f)
            canvas.drawBitmap(bmp, null, RectF(lx, ly, lx + layerSize, ly + layerSize), bmpPaint)
            bmp.recycle()
        }
    }

    // Header Badge Pangkat & Progres
    val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE60B2239.toInt() }
    canvas.drawRoundRect(RectF(42f, 44f, 410f, 100f), 18f, 18f, badgeBg)
    val textBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF22D3EE.toInt()
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(rankBadge.take(22), 62f, 81f, textBadge)

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 24f
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("MRI EDU · $masteryPct%", width - 48f, 82f, pctPaint)

    // Footer Identitas & Sekuens
    canvas.drawRoundRect(RectF(42f, 780f, width - 42f, height - 42f), 26f, 26f, badgeBg)
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(displayName.take(28), width / 2f, 840f, titlePaint)
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFA5C4DF.toInt()
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(specialistRole.take(44), width / 2f, 878f, subPaint)
    canvas.drawText("Sekuens: $favoriteSequence".take(48), width / 2f, 906f, subPaint)

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "nano_mri_card_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MRI-Learn")
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    outBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }

        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(dir, "nano_profile_card.png")
        FileOutputStream(file).use { out ->
            outBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_TEXT, "Kartu Radiografer MRI EDU — $displayName ($rankBadge)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Kartu Profil Nano"))
    } catch (_: Exception) {
        Toast.makeText(context, "Kartu profil berhasil disiapkan.", Toast.LENGTH_SHORT).show()
    } finally {
        outBitmap.recycle()
    }
}
