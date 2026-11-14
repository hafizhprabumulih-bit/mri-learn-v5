package com.example

import com.example.data.StudyNote
import com.example.util.NotesPdfExporter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesPdfExporterTest {

    @Test
    fun `wrapTextLines wraps long paragraphs within limit without dropping words`() {
        val longText = "Frekuensi presesi Larmor proton hidrogen berbanding lurus dengan kuat medan magnet utama B0."
        val lines = NotesPdfExporter.wrapTextLines(longText, maxCharsPerLine = 32)
        assertTrue(lines.size >= 3)
        assertTrue(lines.all { it.length <= 32 })
        assertTrue(lines.joinToString(" ").contains("Larmor"))
        assertTrue(lines.joinToString(" ").contains("B0."))
    }

    @Test
    fun `buildPlainPreviewText formats notes cleanly for PDF preview instead of JSON`() {
        val notes = listOf(
            StudyNote(
                id = "n1",
                title = "Ringkasan TR dan TE",
                content = "T1w memakai TR pendek dan TE pendek.",
                category = "Fisika Sekuens",
                dateString = "2026-11-14"
            )
        )
        val preview = NotesPdfExporter.buildPlainPreviewText(notes, "Radiografer")
        assertTrue(preview.contains("PDF"))
        assertTrue(preview.contains("Ringkasan TR dan TE"))
        assertFalse(preview.trimStart().startsWith("{"))
    }
}
