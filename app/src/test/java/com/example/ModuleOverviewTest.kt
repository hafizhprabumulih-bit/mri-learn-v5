package com.example

import com.example.data.MriRepository
import com.example.util.ModuleOverview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleOverviewTest {

    @Test
    fun m01OverviewHas23NodesAndLevelQuizzes10_8_5() {
        val m01 = MriRepository.modules.first { it.id == "M01" }
        val summary = ModuleOverview.compute(
            module = m01,
            allModules = MriRepository.modules,
            completedCodes = emptySet(),
            quizzes = MriRepository.quizzes,
            enforceCurriculumLocks = false
        )

        assertEquals(23, summary.totalNodes)
        assertEquals(0, summary.completedNodes)
        assertEquals(23, summary.totalQuizzes)
        assertTrue(summary.hasSimulation)
        assertTrue(summary.isNotStarted)
        assertFalse(summary.isComplete)
        assertTrue(summary.isRecommended)
        assertNull(summary.rewardItemName)
        assertEquals(listOf(10, 8, 5), summary.levels.map { it.quizCount })
    }

    @Test
    fun m04bOverviewHandlesModuleWithoutLevel1() {
        val m04b = MriRepository.modules.first { it.id == "M04B" }
        val summary = ModuleOverview.compute(
            module = m04b,
            allModules = MriRepository.modules,
            completedCodes = emptySet(),
            quizzes = MriRepository.quizzes,
            enforceCurriculumLocks = false
        )

        assertEquals(listOf(2), summary.levels.map { it.level })
        assertEquals(m04b.nodes.size, summary.totalNodes)
        assertEquals(18, summary.totalQuizzes)
    }

    @Test
    fun fullCompletionMarksCompleteAndNormalizesCrossModulePrereqs() {
        val m01 = MriRepository.modules.first { it.id == "M01" }
        val allM01Codes = m01.nodes.map { it.code }.toSet()
        val summary = ModuleOverview.compute(
            module = m01,
            allModules = MriRepository.modules,
            completedCodes = allM01Codes,
            quizzes = MriRepository.quizzes,
            enforceCurriculumLocks = false
        )

        assertTrue(summary.isComplete)
        assertEquals(23, summary.completedNodes)

        val known = MriRepository.modules.map { it.code.uppercase() }.toSet()
        assertEquals("M01", ModuleOverview.extractCrossModuleCode("M01", "M02", known))
        assertEquals("M05", ModuleOverview.extractCrossModuleCode("M05.T1.1", "M06A", known))
        assertNull(ModuleOverview.extractCrossModuleCode("T2.2", "M06A", known))
        assertNull(ModuleOverview.extractCrossModuleCode("M06A.T1.1", "M06A", known))
    }
}
