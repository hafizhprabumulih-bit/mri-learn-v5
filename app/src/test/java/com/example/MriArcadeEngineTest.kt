package com.example

import com.example.util.DefenseItemCategory
import com.example.util.DefenseProjectile
import com.example.util.MagnetDefenseState
import com.example.util.MriArcadeEngine
import com.example.util.PulseBeatKind
import com.example.util.RhythmBeatNote
import com.example.util.SpinEchoRhythmState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MriArcadeEngineTest {

    @Test
    fun `deflecting unsafe projectile increases score and combo`() {
        val state = MagnetDefenseState(
            items = listOf(
                DefenseProjectile(
                    id = 1,
                    label = "Gunting Baja",
                    emoji = "✂️",
                    category = DefenseItemCategory.MR_UNSAFE,
                    x = 0.2f,
                    y = 0.2f,
                    vx = 0.1f,
                    vy = 0.1f
                )
            )
        )
        val next = MriArcadeEngine.tapDefenseProjectile(state, 1)
        assertTrue(next.items.isEmpty())
        assertEquals(1, next.combo)
        assertEquals(1, next.deflectedCount)
        assertTrue(next.score >= 120)
    }

    @Test
    fun `unsafe projectile reaching five gauss reduces lives`() {
        val state = MagnetDefenseState(
            items = listOf(
                DefenseProjectile(
                    id = 1,
                    label = "Tabung O₂",
                    emoji = "🧯",
                    category = DefenseItemCategory.MR_UNSAFE,
                    x = 0.5f,
                    y = 0.5f,
                    vx = 0f,
                    vy = 0f
                )
            ),
            lives = 3
        )
        val next = MriArcadeEngine.stepMagnetDefense(state, 0.016f)
        assertEquals(2, next.lives)
        assertEquals(0, next.combo)
    }

    @Test
    fun `spin echo rhythm awards perfect hit near target`() {
        val state = SpinEchoRhythmState(
            notes = listOf(RhythmBeatNote(id = 7, kind = PulseBeatKind.RF_90, progress = 0.99f))
        )
        val next = MriArcadeEngine.tapRhythmLane(state)
        assertTrue(next.notes.isEmpty())
        assertEquals(1, next.combo)
        assertTrue(next.score >= 135)
        assertTrue(next.lastJudgment.startsWith("PERFECT"))
    }
}
