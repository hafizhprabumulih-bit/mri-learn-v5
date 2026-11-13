package com.example

import com.example.util.DefenseItemCategory
import com.example.util.DefenseProjectile
import com.example.util.MagnetDefenseState
import com.example.util.MriArcadeEngine
import com.example.util.PulseBeatKind
import com.example.util.RhythmBeatNote
import com.example.util.SpinEchoRhythmState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
                    emoji = "GUNTING",
                    category = DefenseItemCategory.MR_UNSAFE,
                    x = 0.2f,
                    y = 0.2f,
                    vx = 0.1f,
                    vy = 0.1f,
                    assetKey = "GUNTING_BAJA"
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
                    emoji = "O2",
                    category = DefenseItemCategory.MR_UNSAFE,
                    x = 0.5f,
                    y = 0.5f,
                    vx = 0f,
                    vy = 0f,
                    assetKey = "TABUNG_O2"
                )
            ),
            lives = 3
        )
        val next = MriArcadeEngine.stepMagnetDefense(state, 0.016f)
        assertEquals(2, next.lives)
        assertEquals(0, next.combo)
    }

    @Test
    fun `tapDefenseAtNormalized prioritizes unsafe item when safe item is nearby`() {
        val safeNear = DefenseProjectile(
            id = 1,
            label = "Bantal Busa",
            emoji = "BANTAL",
            category = DefenseItemCategory.MR_SAFE,
            x = 0.22f,
            y = 0.22f,
            vx = 0f,
            vy = 0f,
            assetKey = "BANTAL_BUSA"
        )
        val unsafeClose = DefenseProjectile(
            id = 2,
            label = "Kunci Pas",
            emoji = "KUNCI",
            category = DefenseItemCategory.MR_UNSAFE,
            x = 0.25f,
            y = 0.24f,
            vx = 0f,
            vy = 0f,
            assetKey = "KUNCI_PAS"
        )
        val state = MagnetDefenseState(items = listOf(safeNear, unsafeClose), score = 100)
        val next = MriArcadeEngine.tapDefenseAtNormalized(state, 0.23f, 0.23f)
        assertEquals(1, next.items.size)
        assertEquals(1, next.items.first().id)
        assertEquals(1, next.deflectedCount)
        assertTrue(next.lastEventGood)
    }

    @Test
    fun `pickOptimalSpawnAngle separates spawn from existing active projectile`() {
        val existing = listOf(
            DefenseProjectile(
                id = 1,
                label = "Klip Logam",
                emoji = "KLIP",
                category = DefenseItemCategory.MR_UNSAFE,
                x = 0.94f,
                y = 0.50f,
                vx = -0.1f,
                vy = 0f,
                assetKey = "KLIP_LOGAM"
            )
        )
        val existingAngle = atan2(0.50f - MriArcadeEngine.BORE_CENTER_Y, 0.94f - MriArcadeEngine.BORE_CENTER_X)
        val picked = MriArcadeEngine.pickOptimalSpawnAngle(existing, Random(42))
        val rawDiff = abs(picked - existingAngle) % (2f * PI.toFloat())
        val angularGap = minOf(rawDiff, 2f * PI.toFloat() - rawDiff)
        assertTrue("Expected well-separated spawn angle, got gap=$angularGap", angularGap >= PI.toFloat() * 0.5f)
    }

    @Test
    fun `spin echo rhythm awards perfect hit near target`() {
        val state = SpinEchoRhythmState(
            notes = listOf(RhythmBeatNote(id = 7, kind = PulseBeatKind.RF_90, progress = 0.99f))
        )
        val next = MriArcadeEngine.tapRhythmLane(state, PulseBeatKind.RF_90)
        assertTrue(next.notes.isEmpty())
        assertEquals(1, next.combo)
        assertTrue(next.score >= 135)
        assertTrue(next.lastJudgment.startsWith("PERFECT"))
    }

    @Test
    fun `tapping empty rhythm lane does not remove note from another lane`() {
        val note = RhythmBeatNote(id = 9, kind = PulseBeatKind.ECHO_READ, progress = 0.96f)
        val state = SpinEchoRhythmState(notes = listOf(note), combo = 3)
        val next = MriArcadeEngine.tapRhythmLane(state, PulseBeatKind.RF_90)
        assertEquals(1, next.notes.size)
        assertEquals(9, next.notes.first().id)
        assertEquals(0, next.combo)
        assertTrue(next.lastJudgment.contains("Salah lajur") || next.lastJudgment.contains("kosong"))
    }

    @Test
    fun `resolveAssetKey returns valid key without CJK fallback characters`() {
        val proj = DefenseProjectile(
            id = 5,
            label = "Pinset Feromagnetik",
            emoji = "PINSET",
            category = DefenseItemCategory.MR_UNSAFE,
            x = 0.2f,
            y = 0.8f,
            vx = 0f,
            vy = 0f
        )
        val key = MriArcadeEngine.resolveAssetKey(proj)
        assertNotNull(key)
        assertEquals("PINSET", key)
    }
}
