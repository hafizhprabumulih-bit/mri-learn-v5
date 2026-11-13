package com.example

import com.example.data.MriRepository
import com.example.data.local.QuizAttemptEntity
import com.example.util.XpCalculator
import com.example.util.XpRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XpCalculatorTest {

    @Test
    fun calculatesReadNodesQuizAndCompleteModuleBonus() {
        val m01 = MriRepository.modules.first { it.id == "M01" }
        val m01Codes = m01.nodes.map { it.code }.toSet()

        val attempts = listOf(
            QuizAttemptEntity(1, "M01_Q1", "M01.T1.1", "M01", 0, true, 1000L),
            // Percobaan benar kedua untuk quizId yang sama tidak boleh menggandakan XP
            QuizAttemptEntity(2, "M01_Q1", "M01.T1.1", "M01", 0, true, 2000L),
            QuizAttemptEntity(3, "M01_Q2", "M01.T1.2", "M01", 1, false, 3000L),
            QuizAttemptEntity(4, "M01_Q3", "M01.T1.3", "M01", 2, true, 4000L)
        )

        val summary = XpCalculator.calculate(
            completedCodes = m01Codes + setOf("ORPHAN.NODE.99"),
            skippedCodes = emptySet(),
            quizAttempts = attempts,
            modules = MriRepository.modules
        )

        assertEquals(m01.nodes.size, summary.readNodesCount)
        assertEquals(m01.nodes.size * XpRules.XP_PER_READ_NODE, summary.readNodesXp)
        assertEquals(2, summary.firstCorrectQuizCount)
        assertEquals(2 * XpRules.XP_PER_FIRST_CORRECT_QUIZ, summary.quizXp)
        assertEquals(1, summary.completedModulesCount)
        assertEquals(XpRules.XP_PER_COMPLETE_MODULE_BONUS, summary.moduleBonusXp)
        assertEquals(
            summary.readNodesXp + summary.quizXp + summary.moduleBonusXp,
            summary.totalXp
        )
    }

    @Test
    fun skippedNodesGiveZeroXpAndExcludeModuleBonusUntilRead() {
        val m02 = MriRepository.modules.first { it.id == "M02" }
        val allM02Codes = m02.nodes.map { it.code }.toSet()
        val skipped = setOf(m02.nodes.first().code)

        val withSkipped = XpCalculator.calculate(
            completedCodes = allM02Codes,
            skippedCodes = skipped,
            quizAttempts = emptyList(),
            modules = MriRepository.modules
        )

        assertEquals(m02.nodes.size - 1, withSkipped.readNodesCount)
        assertEquals(0, withSkipped.completedModulesCount)
        assertEquals((m02.nodes.size - 1) * XpRules.XP_PER_READ_NODE, withSkipped.totalXp)

        // Setelah simpul dilewati dibaca ulang (bukan SKIP_TEST), XP simpul + bonus modul langsung masuk
        val afterRead = XpCalculator.calculate(
            completedCodes = allM02Codes,
            skippedCodes = emptySet(),
            quizAttempts = emptyList(),
            modules = MriRepository.modules
        )
        assertEquals(1, afterRead.completedModulesCount)
        assertEquals(
            m02.nodes.size * XpRules.XP_PER_READ_NODE + XpRules.XP_PER_COMPLETE_MODULE_BONUS,
            afterRead.totalXp
        )
    }

    @Test
    fun uncompletingNodeReducesXpAndRemovesModuleBonus() {
        val m02 = MriRepository.modules.first { it.id == "M02" }
        val allM02Codes = m02.nodes.map { it.code }.toSet()

        val full = XpCalculator.calculate(allM02Codes, emptySet(), emptyList(), MriRepository.modules)
        val reduced = XpCalculator.calculate(
            allM02Codes - m02.nodes.last().code,
            emptySet(),
            emptyList(),
            MriRepository.modules
        )

        assertEquals(
            XpRules.XP_PER_READ_NODE + XpRules.XP_PER_COMPLETE_MODULE_BONUS,
            full.totalXp - reduced.totalXp
        )
    }

    @Test
    fun nextUnlockPreviewFindsNearestCharacterRewardProgress() {
        val preview = XpCalculator.nextUnlockPreview(
            completedCodes = setOf("M05.T1.1", "M05.T1.2"),
            modules = MriRepository.modules,
            preferredModuleId = "M05"
        )
        assertNotNull(preview)
        assertEquals("M05", preview?.moduleCode)
        assertTrue((preview?.completedInModule ?: 0) >= 2)
    }

    @Test
    fun nextUnlockPreviewAggregatesSplitChapterModulesAndMatchesBranchPreference() {
        val preview = XpCalculator.nextUnlockPreview(
            completedCodes = setOf("M07A.T1.1", "M07B.T2.1"),
            modules = MriRepository.modules,
            preferredModuleId = "M07A"
        )
        assertNotNull(preview)
        assertEquals("M07", preview?.moduleCode)
        assertEquals(2, preview?.completedInModule)
    }
}
