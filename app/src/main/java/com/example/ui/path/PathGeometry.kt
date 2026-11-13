package com.example.ui.path

/**
 * Pola posisi horizontal zig-zag (0f..1f) untuk node pada Peta Belajar:
 * tengah (0.50f), kanan (0.72f), tengah (0.50f), kiri (0.28f), berulang setiap 4 langkah.
 */
object PathGeometry {
    const val CENTER_X = 0.50f
    const val RIGHT_X = 0.72f
    const val LEFT_X = 0.28f

    fun xFractionFor(index: Int): Float {
        val mod = ((index % 4) + 4) % 4
        return when (mod) {
            1 -> RIGHT_X
            3 -> LEFT_X
            else -> CENTER_X
        }
    }
}

fun xFractionFor(index: Int): Float = PathGeometry.xFractionFor(index)
