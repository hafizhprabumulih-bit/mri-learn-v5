package com.example

import com.example.ui.components.MriGlobalAssetCatalog
import com.example.ui.components.MriGlobalAssetKey
import com.example.util.MriMinigameEngine
import com.example.util.TapAccuracy
import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MriMinigameEngineTest {

    @Test
    fun `screening bank has balanced safe and unsafe items with asset keys`() {
        val safeCount = MriMinigameEngine.screeningBank.count { it.allowIntoZone4 }
        val unsafeCount = MriMinigameEngine.screeningBank.count { !it.allowIntoZone4 }
        assertTrue(safeCount >= 6)
        assertTrue(unsafeCount >= 6)
        assertTrue(MriMinigameEngine.screeningBank.all { it.reason.isNotBlank() && it.assetKey.isNotBlank() })
        MriMinigameEngine.screeningBank.forEach { item ->
            val resolved = MriGlobalAssetCatalog.resolveKey(item.assetKey)
            assertNotEquals("Item ${item.id} should resolve dedicated asset", MriGlobalAssetKey.FALLBACK_MRI, resolved)
        }
    }

    @Test
    fun `larmor tap evaluates perfect good and miss thresholds`() {
        assertEquals(TapAccuracy.PERFECT, MriMinigameEngine.evaluateLarmorTap(0.55f, 0.55f))
        assertEquals(TapAccuracy.PERFECT, MriMinigameEngine.evaluateLarmorTap(0.60f, 0.55f))
        assertEquals(TapAccuracy.GOOD, MriMinigameEngine.evaluateLarmorTap(0.66f, 0.55f))
        assertEquals(TapAccuracy.MISS, MriMinigameEngine.evaluateLarmorTap(0.80f, 0.55f))
    }

    @Test
    fun `larmor phase and needle remain bounded and continuous when combo rises`() {
        var phase = 0.65f
        val needleBefore = MriMinigameEngine.needleFromPhase(phase)
        phase = MriMinigameEngine.advanceLarmorPhase(phase, deltaSec = 0.016f, speedMultiplier = 1.2f, combo = 5)
        val needleAfter = MriMinigameEngine.needleFromPhase(phase)

        assertTrue(needleBefore in 0.06f..0.94f)
        assertTrue(needleAfter in 0.06f..0.94f)
        assertTrue("Needle should move smoothly without sudden jump on combo increase", abs(needleAfter - needleBefore) < 0.08f)
    }

    @Test
    fun `buildNonRepeatingDeck avoids repeating previousLast at index zero`() {
        val base = listOf("A", "B", "C", "D", "E")
        repeat(10) { seed ->
            val deck = MriMinigameEngine.buildNonRepeatingDeck(base, previousLast = "C", random = Random(seed))
            assertEquals(base.size, deck.size)
            assertNotEquals("C", deck.first())
        }
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
