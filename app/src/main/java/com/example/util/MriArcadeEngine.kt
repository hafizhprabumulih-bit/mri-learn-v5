package com.example.util

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

enum class DefenseItemCategory { MR_UNSAFE, MR_SAFE }

data class DefenseProjectile(
    val id: Int,
    val label: String,
    val emoji: String,
    val category: DefenseItemCategory,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float
)

data class MagnetDefenseState(
    val items: List<DefenseProjectile> = emptyList(),
    val score: Int = 0,
    val combo: Int = 0,
    val lives: Int = 3,
    val wave: Int = 1,
    val elapsedSec: Float = 0f,
    val spawnCooldownSec: Float = 0.25f,
    val nextId: Int = 1,
    val deflectedCount: Int = 0,
    val lastEventMessage: String = "Ketuk benda feromagnetik (merah) sebelum menembus garis 5 Gauss!",
    val lastEventGood: Boolean = true,
    val isGameOver: Boolean = false
) {
    val fieldLabel: String
        get() = when {
            wave >= 3 -> "3,0 Tesla"
            wave == 2 -> "1,5 Tesla"
            else -> "0,5 Tesla"
        }

    val rankTitle: String
        get() = MriArcadeEngine.rankForDefenseScore(score)
}

enum class PulseBeatKind(val label: String) {
    RF_90("90° RF"),
    REFOCUS_180("180° Refocus"),
    ECHO_READ("Baca Echo")
}

data class RhythmBeatNote(
    val id: Int,
    val kind: PulseBeatKind,
    val progress: Float // 0f = atas, 1f = garis target ketuk
)

data class SpinEchoRhythmState(
    val notes: List<RhythmBeatNote> = emptyList(),
    val score: Int = 0,
    val combo: Int = 0,
    val lives: Int = 3,
    val elapsedSec: Float = 0f,
    val spawnTimerSec: Float = 0.25f,
    val nextId: Int = 1,
    val stepIndex: Int = 0,
    val echoAmplitude: Float = 0.5f,
    val lastJudgment: String = "Ketuk tombol pulsa saat indikator tiba di garis target!",
    val isGameOver: Boolean = false
) {
    val isTurboMode: Boolean get() = combo >= 6
    val rankTitle: String get() = MriArcadeEngine.rankForRhythmScore(score)
}

/**
 * Mesin simulasi murni untuk dua game arcade MRI:
 * 1) Nano Magnet Defense (pertahanan garis 5 Gauss)
 * 2) Spin Echo Rhythm (ritme pulsa 90° -> 180° -> Echo)
 */
object MriArcadeEngine {

    const val BORE_CENTER_X = 0.5f
    const val BORE_CENTER_Y = 0.5f
    const val FIVE_GAUSS_RADIUS = 0.16f

    private val unsafeTemplates = listOf(
        "Gunting Baja" to "✂️",
        "Tabung O₂" to "🧯",
        "Kunci Pas" to "🔧",
        "Klip Logam" to "📎",
        "Kursi Roda Besi" to "🦽",
        "Stetoskop Baja" to "🩺",
        "Jam Mekanik" to "⌚",
        "Pinset Feromagnetik" to "镊"
    )
    private val safeTemplates = listOf(
        "Bantal Busa" to "🛏️",
        "Vial Kontras" to "🧪",
        "Gaun Katun" to "👕",
        "Earplug Busa" to "🎧",
        "Phantom ACR" to "🫧"
    )

    // Variasi pola sekuens agar ritme Spin Echo & TSE tidak monoton
    private val rhythmSequencePatterns = listOf(
        // Klasik Spin Echo: 90° -> 180° -> Echo
        PulseBeatKind.RF_90, PulseBeatKind.REFOCUS_180, PulseBeatKind.ECHO_READ,
        // Turbo Spin Echo (TSE / FSE): 90° -> 180° -> Echo -> 180° -> Echo
        PulseBeatKind.RF_90, PulseBeatKind.REFOCUS_180, PulseBeatKind.ECHO_READ,
        PulseBeatKind.REFOCUS_180, PulseBeatKind.ECHO_READ,
        // Inversion Recovery (IR-SE): 180° Inversi -> 90° -> 180° -> Echo
        PulseBeatKind.REFOCUS_180, PulseBeatKind.RF_90, PulseBeatKind.REFOCUS_180, PulseBeatKind.ECHO_READ
    )

    fun stepMagnetDefense(
        state: MagnetDefenseState,
        deltaSec: Float,
        random: Random = Random.Default
    ): MagnetDefenseState {
        if (state.isGameOver || deltaSec <= 0f) return state
        val dt = deltaSec.coerceAtMost(0.05f)
        val newElapsed = state.elapsedSec + dt
        val newWave = (1 + (newElapsed / 15f).toInt()).coerceAtMost(5)
        val speedFactor = 0.14f + (newWave - 1) * 0.032f

        var lives = state.lives
        var combo = state.combo
        var score = state.score
        var lastMsg = state.lastEventMessage
        var lastGood = state.lastEventGood

        val surviving = mutableListOf<DefenseProjectile>()
        for (item in state.items) {
            val dxCenter = BORE_CENTER_X - item.x
            val dyCenter = BORE_CENTER_Y - item.y
            val distNow = hypot(dxCenter, dyCenter).coerceAtLeast(0.001f)
            // Akselerasi gradien spasial B0 + sedikit gerak spiral pada wave tinggi
            val pullBoost = 1f + ((0.45f - distNow).coerceAtLeast(0f) * 0.85f)
            val swirl = if (newWave >= 3 && item.category == DefenseItemCategory.MR_UNSAFE) {
                0.04f * if (item.id % 2 == 0) 1f else -1f
            } else 0f
            val tx = -dyCenter / distNow
            val ty = dxCenter / distNow

            val nx = item.x + (item.vx * pullBoost + tx * swirl) * dt
            val ny = item.y + (item.vy * pullBoost + ty * swirl) * dt
            val dist = hypot(nx - BORE_CENTER_X, ny - BORE_CENTER_Y)
            if (dist <= FIVE_GAUSS_RADIUS) {
                if (item.category == DefenseItemCategory.MR_UNSAFE) {
                    lives = (lives - 1).coerceAtLeast(0)
                    combo = 0
                    lastMsg = "Bahaya proyektil! ${item.label} menembus garis 5 Gauss!"
                    lastGood = false
                } else {
                    score += 45
                    lastMsg = "${item.label} (MR Safe) masuk aman ke K-space scan (+45)"
                    lastGood = true
                }
            } else {
                surviving.add(item.copy(x = nx, y = ny))
            }
        }

        var cooldown = state.spawnCooldownSec - dt
        var nextId = state.nextId
        if (cooldown <= 0f && lives > 0) {
            val spawnCount = if (newWave >= 4 && random.nextFloat() < 0.28f && surviving.size < 5) 2 else 1
            repeat(spawnCount) { idx ->
                val angle = random.nextFloat() * (2f * PI.toFloat()) + idx * 1.1f
                val startX = BORE_CENTER_X + cos(angle) * 0.44f
                val startY = BORE_CENTER_Y + sin(angle) * 0.44f
                val dx = BORE_CENTER_X - startX
                val dy = BORE_CENTER_Y - startY
                val len = hypot(dx, dy).coerceAtLeast(0.001f)
                val isUnsafe = random.nextFloat() < 0.76f
                val template = if (isUnsafe) {
                    unsafeTemplates[random.nextInt(unsafeTemplates.size)]
                } else {
                    safeTemplates[random.nextInt(safeTemplates.size)]
                }
                val speedVar = speedFactor * (0.9f + random.nextFloat() * 0.28f)
                surviving.add(
                    DefenseProjectile(
                        id = nextId++,
                        label = template.first,
                        emoji = template.second,
                        category = if (isUnsafe) DefenseItemCategory.MR_UNSAFE else DefenseItemCategory.MR_SAFE,
                        x = startX,
                        y = startY,
                        vx = (dx / len) * speedVar,
                        vy = (dy / len) * speedVar
                    )
                )
            }
            cooldown = (1.18f - (newWave - 1) * 0.14f).coerceAtLeast(0.48f)
        }

        return state.copy(
            items = surviving,
            score = score,
            combo = combo,
            lives = lives,
            wave = newWave,
            elapsedSec = newElapsed,
            spawnCooldownSec = cooldown,
            nextId = nextId,
            lastEventMessage = lastMsg,
            lastEventGood = lastGood,
            isGameOver = lives <= 0
        )
    }

    fun tapDefenseProjectile(state: MagnetDefenseState, itemId: Int): MagnetDefenseState {
        if (state.isGameOver) return state
        val target = state.items.firstOrNull { it.id == itemId } ?: return state
        val remaining = state.items.filterNot { it.id == itemId }

        return if (target.category == DefenseItemCategory.MR_UNSAFE) {
            val newCombo = state.combo + 1
            val distFromBore = hypot(target.x - BORE_CENTER_X, target.y - BORE_CENTER_Y)
            val closeCallBonus = if (distFromBore <= FIVE_GAUSS_RADIUS + 0.08f) 35 else 0
            val points = 100 + (newCombo * 20).coerceAtMost(140) + closeCallBonus
            val closeTag = if (closeCallBonus > 0) " [Tangkisan Kritis +35!]" else ""
            state.copy(
                items = remaining,
                score = state.score + points,
                combo = newCombo,
                deflectedCount = state.deflectedCount + 1,
                lastEventMessage = "Tangkis ${target.label}! +$points (Combo x$newCombo)$closeTag",
                lastEventGood = true
            )
        } else {
            state.copy(
                items = remaining,
                combo = 0,
                score = (state.score - 30).coerceAtLeast(0),
                lastEventMessage = "${target.label} itu MR Safe, biarkan masuk! (-30)",
                lastEventGood = false
            )
        }
    }

    fun tapDefenseAtNormalized(
        state: MagnetDefenseState,
        normX: Float,
        normY: Float,
        hitRadius: Float = 0.14f
    ): MagnetDefenseState {
        if (state.isGameOver || state.items.isEmpty()) return state
        val closest = state.items.minByOrNull { hypot(it.x - normX, it.y - normY) } ?: return state
        val dist = hypot(closest.x - normX, closest.y - normY)
        return if (dist <= hitRadius) {
            tapDefenseProjectile(state, closest.id)
        } else {
            state
        }
    }

    fun stepSpinEchoRhythm(state: SpinEchoRhythmState, deltaSec: Float): SpinEchoRhythmState {
        if (state.isGameOver || deltaSec <= 0f) return state
        val dt = deltaSec.coerceAtMost(0.05f)
        val newElapsed = state.elapsedSec + dt
        val dynamicBoost = (newElapsed / 45f).coerceAtMost(0.18f)
        val speed = (if (state.isTurboMode) 0.54f else 0.39f) + dynamicBoost

        var lives = state.lives
        var combo = state.combo
        var judgment = state.lastJudgment
        var amp = (state.echoAmplitude - dt * 0.15f).coerceAtLeast(0.2f)

        val kept = mutableListOf<RhythmBeatNote>()
        for (note in state.notes) {
            val nextProg = note.progress + speed * dt
            if (nextProg > 1.22f) {
                lives = (lives - 1).coerceAtLeast(0)
                combo = 0
                amp = 0.2f
                judgment = "Terlewat ${note.kind.label}! Koherensi spin K-space meluruh"
            } else {
                kept.add(note.copy(progress = nextProg))
            }
        }

        var spawnTimer = state.spawnTimerSec - dt
        var nextId = state.nextId
        var stepIdx = state.stepIndex
        if (spawnTimer <= 0f && lives > 0) {
            val kind = rhythmSequencePatterns[stepIdx % rhythmSequencePatterns.size]
            kept.add(RhythmBeatNote(id = nextId++, kind = kind, progress = 0f))
            stepIdx++
            val baseInterval = if (state.isTurboMode) 0.78f else 1.02f
            spawnTimer = when (kind) {
                PulseBeatKind.REFOCUS_180 -> baseInterval * 0.85f
                PulseBeatKind.ECHO_READ -> baseInterval * 1.08f
                PulseBeatKind.RF_90 -> baseInterval
            }
        }

        return state.copy(
            notes = kept,
            lives = lives,
            combo = combo,
            elapsedSec = newElapsed,
            spawnTimerSec = spawnTimer,
            nextId = nextId,
            stepIndex = stepIdx,
            echoAmplitude = amp,
            lastJudgment = judgment,
            isGameOver = lives <= 0
        )
    }

    fun tapRhythmLane(
        state: SpinEchoRhythmState,
        preferredKind: PulseBeatKind? = null
    ): SpinEchoRhythmState {
        if (state.isGameOver || state.notes.isEmpty()) return state
        val pool = if (preferredKind != null) {
            state.notes.filter { it.kind == preferredKind }.ifEmpty { state.notes }
        } else {
            state.notes
        }
        val candidate = pool.minByOrNull { abs(it.progress - 1.0f) } ?: return state
        val dist = abs(candidate.progress - 1.0f)
        val kindMatch = preferredKind == null || candidate.kind == preferredKind

        return if (dist <= 0.28f && kindMatch) {
            val isPerfect = dist <= 0.11f
            val newCombo = state.combo + 1
            val mult = if (newCombo >= 6) 2 else 1
            val base = if (isPerfect) 120 else 80
            val gained = (base + (newCombo * 15).coerceAtMost(120)) * mult
            state.copy(
                notes = state.notes.filterNot { it.id == candidate.id },
                score = state.score + gained,
                combo = newCombo,
                echoAmplitude = if (isPerfect) 1.0f else 0.82f,
                lastJudgment = buildString {
                    append(if (isPerfect) "PERFECT" else "GOOD")
                    append(" ${candidate.kind.label} +$gained")
                    if (newCombo >= 6) append(" (TSE 2x!)")
                }
            )
        } else if (!kindMatch && dist <= 0.32f) {
            state.copy(
                notes = state.notes.filterNot { it.id == candidate.id },
                combo = 0,
                echoAmplitude = 0.25f,
                lastJudgment = "Pulsa kurang tepat! Seharusnya ${candidate.kind.label}"
            )
        } else {
            state.copy(
                combo = 0,
                echoAmplitude = 0.30f,
                lastJudgment = "Tunggu hingga pulsa menyentuh garis target!"
            )
        }
    }

    fun rankForDefenseScore(score: Int): String = when {
        score >= 2200 -> "Master Superkonduktor"
        score >= 1200 -> "Radiografer Kilat"
        score >= 500 -> "Penjaga 5 Gauss"
        else -> "Magang Zona II"
    }

    fun rankForRhythmScore(score: Int): String = when {
        score >= 2400 -> "Master Superkonduktor"
        score >= 1300 -> "Radiografer Kilat"
        score >= 550 -> "Penjaga 5 Gauss"
        else -> "Magang Zona II"
    }
}
