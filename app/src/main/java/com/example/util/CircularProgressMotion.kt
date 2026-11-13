package com.example.util

/**
 * Penghitung sudut putar & sapuan (sweep) murni untuk indikator progres lingkaran
 * sehingga lingkaran berputar proporsional sesuai progres yang dicapai.
 */
object CircularProgressMotion {

    fun normalizeProgress(done: Int, total: Int): Float {
        if (total <= 0) return 0f
        return (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Menghitung sudut awal busur (derajat) yang berputar seiring bertambahnya progres
     * ditambah offset animasi halus saat progres berada di tengah (in-progress).
     */
    fun calculateStartAngleDeg(
        progress: Float,
        continuousSpinDeg: Float = 0f,
        isInProgress: Boolean = false
    ): Float {
        val clamped = progress.coerceIn(0f, 1f)
        val spinOffset = if (isInProgress) continuousSpinDeg * 0.25f else 0f
        return -90f + clamped * 270f + spinOffset
    }

    /**
     * Menghitung besar sudut sapuan busur progres (derajat) agar 0% tetap tampak
     * sebagai penanda titik awal kecil dan 100% menutup lingkaran penuh.
     */
    fun calculateSweepAngleDeg(progress: Float, total: Int): Float {
        if (total <= 0) return 0f
        val clamped = progress.coerceIn(0f, 1f)
        if (clamped <= 0f) return 18f
        if (clamped >= 1f) return 360f
        return (clamped * 360f).coerceAtLeast(24f)
    }
}
