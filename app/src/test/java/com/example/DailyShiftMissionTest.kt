package com.example

import com.example.util.DailyShiftMission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyShiftMissionTest {

    @Test
    fun `partial daily progress awards individual task xp`() {
        val summary = DailyShiftMission.evaluate(nodesToday = 1, quizzesToday = 3, labsToday = 0)
        assertEquals(1, summary.completedCount)
        assertEquals(3, summary.totalCount)
        assertFalse(summary.isAllComplete)
        assertEquals(15, summary.bonusXpEarned)
        assertTrue(summary.tasks[0].isDone)
        assertFalse(summary.tasks[1].isDone)
        assertEquals(3, summary.tasks[1].current)
    }

    @Test
    fun `completing all tasks grants full completion bonus`() {
        val summary = DailyShiftMission.evaluate(nodesToday = 2, quizzesToday = 8, labsToday = 1)
        assertEquals(3, summary.completedCount)
        assertTrue(summary.isAllComplete)
        assertEquals(80, summary.bonusXpEarned)
    }
}
