package com.example.ui.notes

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import com.example.ui.components.rememberRestorableScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.StudyNote
import com.example.ui.character.AvatarView
import com.example.ui.character.Expression
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberDark
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.NotesPdfExporter
import com.example.util.rememberMriHaptics
import com.example.viewmodel.MriUiState
import com.example.viewmodel.MriViewModel
import java.time.LocalDate

@Composable
fun NotesScreen(
    state: MriUiState,
    viewModel: MriViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberMriHaptics()
    val categories = listOf("SEMUA", "Protokol Klinis", "Fisika Sekuens", "Mitigasi Artefak", "Keselamatan MR", "Catatan Bebas")
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var exportStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val createPdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: throw java.io.IOException("Aliran penyimpanan PDF tujuan tidak tersedia.")
                stream.use { outputStream ->
                    NotesPdfExporter.writeNotesPdf(state.notesList, state.userName, outputStream)
                }
                exportStatusMessage = "Catatan berhasil disimpan sebagai file PDF siap cetak."
            }.onFailure { err ->
                exportStatusMessage = "Gagal menyimpan file PDF (${err.localizedMessage ?: "akses ditolak"})."
            }
        } else {
            exportStatusMessage = "Pemilihan lokasi simpan PDF dibatalkan."
        }
    }

    Box(modifier = modifier.fillMaxSize().background(DeepObsidian)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. PINNED TOP HEADER: Judul + Tombol Cetak / Export Catatan (PDF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Catatan Belajar MRI",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.notesList.size} catatan tersimpan • Siap cetak PDF",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = {
                        exportStatusMessage = null
                        showExportDialog = true
                    },
                    modifier = Modifier.testTag("export_notes_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DarkNavySlate,
                        contentColor = CyanBright
                    ),
                    border = BorderStroke(1.dp, CyanPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Cetak / Export Catatan ke PDF",
                        tint = CyanBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cetak PDF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (exportStatusMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_status_banner"),
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldDark,
                    border = BorderStroke(1.dp, EmeraldSuccess)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = exportStatusMessage.orEmpty(),
                            fontSize = 11.sp,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. PINNED FILTER KATEGORI: Tetap di atas agar mudah berganti kategori
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = state.selectedNoteCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptics.lightToggle()
                            viewModel.setNoteCategory(cat)
                        },
                        label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary,
                            selectedLabelColor = DeepObsidian,
                            containerColor = DarkNavySlate,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) CyanBright else CardSlateBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. SCROLLABLE NOTES CONTENT: Hanya daftar catatan ini yang bergerak saat di-swipe!
            // B-19: Cocokkan kategori secara akurat (serta dukung alias lama "Fisika" -> "Fisika Sekuens", dsb.)
            val filteredNotes = if (state.selectedNoteCategory == "SEMUA") {
                state.notesList
            } else {
                val targetCat = state.selectedNoteCategory.trim()
                state.notesList.filter { note ->
                    val noteCat = note.category.trim()
                    noteCat.equals(targetCat, ignoreCase = true) ||
                        (targetCat.equals("Fisika Sekuens", ignoreCase = true) && noteCat.equals("Fisika", ignoreCase = true)) ||
                        (targetCat.equals("Protokol Klinis", ignoreCase = true) && noteCat.equals("Protokol", ignoreCase = true)) ||
                        (targetCat.equals("Mitigasi Artefak", ignoreCase = true) && noteCat.equals("Artefak", ignoreCase = true)) ||
                        (targetCat.equals("Keselamatan MR", ignoreCase = true) && noteCat.equals("Keselamatan", ignoreCase = true))
                }
            }

            val notesScrollState = rememberRestorableScrollState("tab_catatan", viewModel)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(notesScrollState)
            ) {
                if (filteredNotes.isEmpty()) {
                    Surface(
                        onClick = {
                            haptics.lightToggle()
                            viewModel.openNoteDialog(null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .testTag("empty_create_note_card"),
                        shape = RoundedCornerShape(16.dp),
                        color = DarkNavySlate,
                        border = BorderStroke(1.2.dp, CyanPrimary.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AvatarView(
                                role = state.characterCustomization.avatarId,
                                equipped = state.characterCustomization,
                                expression = Expression.BERPIKIR,
                                modifier = Modifier.size(84.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum ada catatan pada kategori ini",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Ketuk kartu ini untuk langsung membuat catatan baru.",
                                fontSize = 11.5.sp,
                                color = CyanBright,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            onClick = {
                                haptics.lightToggle()
                                viewModel.openNoteDialog(null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .testTag("create_note_card"),
                            shape = RoundedCornerShape(16.dp),
                            color = CyanDark.copy(alpha = 0.35f),
                            border = BorderStroke(1.2.dp, CyanPrimary.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CyanPrimary
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Buat catatan baru",
                                            tint = DeepObsidian,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Buat Catatan Baru",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Ketuk kartu ini untuk membuka tempat catatan kosong layar penuh",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        filteredNotes.forEach { note ->
                            NoteCard(
                                note = note,
                                onEdit = {
                                    haptics.lightToggle()
                                    viewModel.openNoteDialog(note)
                                },
                                onDelete = {
                                    haptics.warningBuzz()
                                    viewModel.deleteNote(note.id)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(88.dp))
            }
        }

        // Tombol Floating "+ Tulis Catatan" di atas footer/bottom bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable {
                    haptics.lightToggle()
                    viewModel.openNoteDialog(null)
                }
                .testTag("add_note_fab"),
            shape = RoundedCornerShape(24.dp),
            color = CyanPrimary,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tulis Catatan",
                    tint = DeepObsidian,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Tulis Catatan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepObsidian
                )
            }
        }

        // Dialog Tempat Catatan Kosong (Form Tulis / Edit Catatan)
        if (state.showNoteDialog) {
            NoteEditorDialog(
                initialNote = state.editingNote,
                onDismiss = { viewModel.closeNoteDialog() },
                onSave = { id, title, content, cat ->
                    viewModel.saveNote(id, title, content, cat)
                }
            )
        }

        // Dialog Cetak & Export Catatan ke Format PDF
        if (showExportDialog) {
            val pdfPreviewText = remember(state.notesList, state.userName) {
                NotesPdfExporter.buildPlainPreviewText(state.notesList, state.userName)
            }
            ExportNotesPdfDialog(
                notesCount = state.notesList.size,
                pdfPreviewText = pdfPreviewText,
                statusMessage = exportStatusMessage,
                onPrintPdf = {
                    NotesPdfExporter.printNotesPdf(context, state.notesList, state.userName)
                        .onSuccess {
                            exportStatusMessage = "Dialog cetak / simpan PDF sistem dibuka."
                        }
                        .onFailure { err ->
                            exportStatusMessage = "Gagal membuka cetak PDF (${err.localizedMessage ?: "layanan tidak tersedia"})."
                        }
                },
                onSavePdfFile = {
                    val fileName = "catatan_belajar_mri_${LocalDate.now()}.pdf"
                    runCatching {
                        createPdfDocumentLauncher.launch(fileName)
                    }.onFailure { err ->
                        exportStatusMessage = "Gagal memilih lokasi PDF (${err.localizedMessage ?: "tidak tersedia"})."
                    }
                },
                onSharePdf = {
                    NotesPdfExporter.shareNotesPdf(context, state.notesList, state.userName)
                        .onSuccess {
                            exportStatusMessage = "File PDF siap dibagikan."
                        }
                        .onFailure { err ->
                            exportStatusMessage = "Gagal membagikan PDF (${err.localizedMessage ?: "tidak tersedia"})."
                        }
                },
                onDismiss = { showExportDialog = false }
            )
        }
    }
}


@Composable
fun NoteCard(
    note: StudyNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavySlate),
        border = BorderStroke(1.dp, CardSlateBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyanDark,
                    border = BorderStroke(1.dp, CyanPrimary)
                ) {
                    Text(
                        text = note.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanBright,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = note.dateString,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(imageVector = Icons.Default.EditNote, contentDescription = "Edit catatan", tint = CyanBright, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Hapus catatan", tint = RoseDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.content,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun NoteEditorDialog(
    initialNote: StudyNote?,
    onDismiss: () -> Unit,
    onSave: (id: String?, title: String, content: String, category: String) -> Unit
) {
    val haptics = rememberMriHaptics()
    val normalizedInitialCat = when (initialNote?.category?.trim()?.lowercase()) {
        "fisika", "fisika sekuens" -> "Fisika Sekuens"
        "protokol", "klinis", "protokol klinis" -> "Protokol Klinis"
        "artefak", "mitigasi artefak" -> "Mitigasi Artefak"
        "keselamatan", "safety", "keselamatan mr" -> "Keselamatan MR"
        "catatan bebas" -> "Catatan Bebas"
        else -> initialNote?.category ?: "Catatan Bebas"
    }
    var title by rememberSaveable(initialNote?.id) { mutableStateOf(initialNote?.title ?: "") }
    var content by rememberSaveable(initialNote?.id) { mutableStateOf(initialNote?.content ?: "") }
    var category by rememberSaveable(initialNote?.id) { mutableStateOf(normalizedInitialCat) }
    val canSave = title.isNotBlank() || content.isNotBlank()

    val categories = listOf("Catatan Bebas", "Protokol Klinis", "Fisika Sekuens", "Mitigasi Artefak", "Keselamatan MR")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("note_editor_fullscreen"),
            color = DarkNavySlate
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (initialNote != null) "Edit Catatan" else "Tulis Catatan Kosong Baru",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tuliskan ringkasan materi, catatan dokter, atau parameter konsol",
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = {
                            haptics.lightToggle()
                            onDismiss()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup editor catatan",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Judul
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Catatan", fontSize = 11.5.sp) },
                    placeholder = { Text("Contoh: Tips Refokusing Spin Echo 180°", fontSize = 11.5.sp, color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DeepObsidian,
                        unfocusedContainerColor = DeepObsidian,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CardSlateBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pilihan Kategori
                Text(text = "Kategori Catatan:", fontSize = 11.5.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Surface(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptics.lightToggle()
                                    category = cat
                                },
                            color = if (isSelected) CyanPrimary else DeepObsidian,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) CyanBright else CardSlateBorder)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DeepObsidian else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tempat Catatan Kosong Full-Screen (mengisi ruang vertikal secara luas)
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Tempat Catatan Kosong", fontSize = 11.5.sp) },
                    placeholder = {
                        Text(
                            text = "Ketik catatan Anda di sini...\n\nContoh:\n- TR pendek (<600 ms) menghasilkan kontras T1w\n- STIR menggunakan TI ~150ms untuk supresi lemak\n- Pastikan pasien bebas logam sebelum Zona IV",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 18.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .defaultMinSize(minHeight = 240.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DeepObsidian,
                        unfocusedContainerColor = DeepObsidian,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CardSlateBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tombol Simpan & Batal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptics.lightToggle()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, CardSlateBorder)
                    ) {
                        Text("Batal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            haptics.mediumAction()
                            onSave(initialNote?.id, title, content, category)
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            disabledContainerColor = CardSlate
                        )
                    ) {
                        Text(
                            text = "Simpan",
                            fontSize = 13.sp,
                            color = if (canSave) DeepObsidian else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportNotesPdfDialog(
    notesCount: Int,
    pdfPreviewText: String,
    statusMessage: String?,
    onPrintPdf: () -> Unit,
    onSavePdfFile: () -> Unit,
    onSharePdf: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("export_notes_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkNavySlate),
            border = BorderStroke(1.dp, CyanPrimary)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("back_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali dari opsi cetak PDF",
                            tint = TextPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cetak Catatan (PDF)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Dokumen A4 siap cetak • $notesCount catatan",
                            fontSize = 11.sp,
                            color = CyanBright
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AmberDark,
                        border = BorderStroke(1.dp, AmberAccent)
                    ) {
                        Text(
                            text = ".PDF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AmberAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Cetak langsung ke printer/Save as PDF, simpan file .pdf ke penyimpanan perangkat, atau bagikan dokumen PDF catatan belajar MRI Anda.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pratinjau Tata Letak Dokumen PDF
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("export_pdf_preview"),
                    shape = RoundedCornerShape(12.dp),
                    color = DeepObsidian,
                    border = BorderStroke(1.dp, CardSlateBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = pdfPreviewText,
                            fontSize = 10.5.sp,
                            color = TextPrimary,
                            lineHeight = 15.sp
                        )
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldDark,
                        border = BorderStroke(1.dp, EmeraldSuccess)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = statusMessage,
                                fontSize = 10.sp,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tombol Utama: Cetak / Print PDF
                Button(
                    onClick = onPrintPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("print_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = DeepObsidian
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cetak / Print PDF",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Baris Aksi Sekunder: Simpan .pdf & Bagikan PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSavePdfFile,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("download_pdf_file_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CardSlate,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, CardSlateBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = CyanBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Simpan PDF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = onSharePdf,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("share_pdf_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CardSlate,
                            contentColor = AmberAccent
                        ),
                        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bagikan PDF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("close_export_dialog_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, CardSlateBorder)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kembali", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

