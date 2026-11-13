package com.example

import com.example.util.MriMinigameEngine
import com.example.util.TapAccuracy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MriMinigameEngineTest {

    @Test
    fun `screening bank has balanced safe and unsafe items`() {
        val safeCount = MriMinigameEngine.screeningBank.count { it.allowIntoZone4 }
        val unsafeCount = MriMinigameEngine.screeningBank.count { !it.allowIntoZone4 }
        assertTrue(safeCount >= 6)
        assertTrue(unsafeCount >= 6)
        assertTrue(MriMinigameEngine.screeningBank.all { it.reason.isNotBlank() })
    }

    @Test
    fun `larmor tap evaluates perfect good and miss thresholds`() {
        assertEquals(TapAccuracy.PERFECT, MriMinigameEngine.evaluateLarmorTap(0.55f, 0.55f))
        assertEquals(TapAccuracy.PERFECT, MriMinigameEngine.evaluateLarmorTap(0.60f, 0.55f))
        assertEquals(TapAccuracy.GOOD, MriMinigameEngine.evaluateLarmorTap(0.66f, 0.55f))
        assertEquals(TapAccuracy.MISS, MriMinigameEngine.evaluateLarmorTap(0.80f, 0.55f))
    }

    @Test
    fun `contrast artifact bank has valid correctIndex bounds`() {
        assertTrue(MriMinigameEngine.contrastArtifactBank.size >= 12)
        MriMinigameEngine.contrastArtifactBank.forEach { q ->
            assertTrue(q.correctIndex in q.options.indices)
            assertTrue(q.explanation.isNotBlank())
        }
    }

    @Test
    fun `combo points scale and cap cleanly`() {
        assertEquals(0, MriMinigameEngine.computePoints(0, 5))
        assertEquals(140, MriMinigameEngine.computePoints(100, 2))
        assertEquals(220, MriMinigameEngine.computePoints(100, 10))
    }
}
