package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

data class HapticWaveformSpec(
    val timingsMs: LongArray,
    val amplitudes: IntArray
) {
    val totalDurationMs: Long
        get() = timingsMs.sum()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HapticWaveformSpec) return false
        return timingsMs.contentEquals(other.timingsMs) && amplitudes.contentEquals(other.amplitudes)
    }

    override fun hashCode(): Int {
        var result = timingsMs.contentHashCode()
        result = 31 * result + amplitudes.contentHashCode()
        return result
    }
}

enum class MriHapticPattern {
    LIGHT_TOGGLE,
    MEDIUM_ACTION,
    WARNING_BUZZ,
    COMPLETION_BUZZ
}

/**
 * Pengelola pola getar haptik terpusat untuk interaksi Belajar MRI:
 * - LIGHT_TOGGLE: ketukan ringan (14 ms) saat menekan tombol 3D modul, chip, atau toggle.
 * - MEDIUM_ACTION: umpan balik mantap (26 ms) untuk aksi simpan atau tembak RF.
 * - WARNING_BUZZ: getar ganda saat terjadi kesalahan skrining/proyektil.
 * - COMPLETION_BUZZ: getar selebrasi bertingkat saat menuntaskan kuis atau pelajaran modul.
 */
object MriHaptics {

    fun specFor(pattern: MriHapticPattern): HapticWaveformSpec = when (pattern) {
        MriHapticPattern.LIGHT_TOGGLE -> HapticWaveformSpec(
            timingsMs = longArrayOf(0L, 14L),
            amplitudes = intArrayOf(0, 72)
        )
        MriHapticPattern.MEDIUM_ACTION -> HapticWaveformSpec(
            timingsMs = longArrayOf(0L, 26L),
            amplitudes = intArrayOf(0, 140)
        )
        MriHapticPattern.WARNING_BUZZ -> HapticWaveformSpec(
            timingsMs = longArrayOf(0L, 34L, 45L, 52L),
            amplitudes = intArrayOf(0, 165, 0, 210)
        )
        MriHapticPattern.COMPLETION_BUZZ -> HapticWaveformSpec(
            timingsMs = longArrayOf(0L, 45L, 55L, 65L, 50L, 95L),
            amplitudes = intArrayOf(0, 145, 0, 195, 0, 255)
        )
    }

    fun perform(
        context: Context?,
        fallbackHaptic: HapticFeedback?,
        pattern: MriHapticPattern
    ) {
        val spec = specFor(pattern)
        val vibrated = runCatching {
            val vibrator = resolveVibrator(context) ?: return@runCatching false
            if (!vibrator.hasVibrator()) return@runCatching false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (spec.timingsMs.size <= 2) {
                    val dur = spec.timingsMs.lastOrNull()?.coerceAtLeast(10L) ?: 16L
                    val amp = spec.amplitudes.lastOrNull()?.coerceIn(1, 255) ?: VibrationEffect.DEFAULT_AMPLITUDE
                    VibrationEffect.createOneShot(dur, amp)
                } else {
                    VibrationEffect.createWaveform(spec.timingsMs, spec.amplitudes, -1)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(spec.timingsMs, -1)
            }
            true
        }.getOrDefault(false)

        if (!vibrated) {
            val type = when (pattern) {
                MriHapticPattern.LIGHT_TOGGLE -> HapticFeedbackType.TextHandleMove
                MriHapticPattern.MEDIUM_ACTION,
                MriHapticPattern.WARNING_BUZZ,
                MriHapticPattern.COMPLETION_BUZZ -> HapticFeedbackType.LongPress
            }
            fallbackHaptic?.performHapticFeedback(type)
        }
    }

    private fun resolveVibrator(context: Context?): Vibrator? {
        if (context == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}

class MriHapticsController(
    private val context: Context,
    private val fallback: HapticFeedback
) {
    fun lightToggle() = MriHaptics.perform(context, fallback, MriHapticPattern.LIGHT_TOGGLE)
    fun mediumAction() = MriHaptics.perform(context, fallback, MriHapticPattern.MEDIUM_ACTION)
    fun warningBuzz() = MriHaptics.perform(context, fallback, MriHapticPattern.WARNING_BUZZ)
    fun completionBuzz() = MriHaptics.perform(context, fallback, MriHapticPattern.COMPLETION_BUZZ)
}

@Composable
fun rememberMriHaptics(): MriHapticsController {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    return remember(context, haptic) {
        MriHapticsController(context = context, fallback = haptic)
    }
}
