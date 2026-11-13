package com.example.util

import com.example.data.MriNode

/**
 * Pemformat label simpul untuk teks yang dilihat pengguna (murni, tanpa dependensi Android).
 * Kode panjang (mis. `M01.T1.1`) tetap dipakai untuk penyimpanan, pencarian, dan catatan.
 *
 * Pola kode yang ada di data: `Mxx.TN`, `Mxx.TN.N`, `Mxx.TN.NA`, `Mxx.TN.N.N`, dan `Mxx.TN.R`
 * (simpul ringkasan tingkat).
 */
object NodeDisplay {
    private val modulePart = Regex("^M\\d+[A-Za-z]?$")
    private val levelPart = Regex("^T\\d+(?:\\.[0-9A-Za-z]+)*$")
    private val levelSummary = Regex("^T(\\d+)\\.R$", RegexOption.IGNORE_CASE)

    // Awalan kode di depan judul, mis. "T1.1 ", "T2.10.5 ", "T1.4 " (untuk kode T1.4A), "T1.1 · ".
    private val titlePrefix = Regex("^T\\d+(?:\\.\\d+)+[A-Za-z]?(?:\\s*[·:\\-–—]\\s*|\\s+)")

    /** `M01.T1.1` -> `T1.1`, `M01.T0` -> `T0`, `M01.T1.R` -> `Ringkasan T1`; kode lain dikembalikan apa adanya. */
    fun shortCode(code: String): String {
        val trimmed = code.trim()
        val dot = trimmed.indexOf('.')
        if (dot <= 0 || dot == trimmed.lastIndex) return trimmed
        val module = trimmed.substring(0, dot)
        val rest = trimmed.substring(dot + 1)
        if (!modulePart.matches(module) || !levelPart.matches(rest)) return trimmed
        levelSummary.matchEntire(rest)?.let { return "Ringkasan T${it.groupValues[1]}" }
        return rest
    }

    /** Judul simpul tanpa awalan kode (mis. "T1.1 Peran MRI ..." -> "Peran MRI ..."). */
    fun displayTitle(title: String): String {
        val trimmed = title.trim()
        val stripped = titlePrefix.replaceFirst(trimmed, "").trim()
        return stripped.ifBlank { trimmed }
    }

    fun displayTitle(node: MriNode): String = displayTitle(node.title)
}
