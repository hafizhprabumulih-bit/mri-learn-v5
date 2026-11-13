package com.example

import com.example.data.MriRepository
import com.example.util.ModuleCompletionCelebration
import com.example.util.ModuleProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleCompletionCelebrationTest {

    @Test
    fun detectsFreshModuleCompletionOnlyOnce() {
        val before = ModuleProgress(moduleId = "M01", done = 22, total = 23)
        val after = ModuleProgress(moduleId = "M01", done = 23, total = 23)

        assertTrue(ModuleCompletionCelebration.moduleJustCompleted(before, after))
        assertTrue(ModuleCompletionCelebration.shouldShow("M01", before, after, emptySet()))
        assertFalse(ModuleCompletionCelebration.shouldShow("M01", before, after, setOf("M01")))
    }

    @Test
    fun alreadyCompleteBeforeDoesNotTriggerAgain() {
        val before = ModuleProgress(moduleId = "M01", done = 23, total = 23)
        val after = ModuleProgress(moduleId = "M01", done = 23, total = 23)

        assertFalse(ModuleCompletionCelebration.moduleJustCompleted(before, after))
        assertFalse(ModuleCompletionCelebration.shouldShow("M01", before, after, emptySet()))
    }

    @Test
    fun findsNextRecommendedModuleAndReturnsNullWhenAllDone() {
        val m01Codes = MriRepository.modules.first { it.id == "M01" }.nodes.map { it.code }.toSet()
        val next = ModuleCompletionCelebration.findNextRecommendedModule(
            currentModuleId = "M01",
            allModules = MriRepository.modules,
            completedCodes = m01Codes
        )
        assertEquals("M02", next?.id)

        val allCodes = MriRepository.modules.flatMap { it.nodes }.map { it.code }.toSet()
        val none = ModuleCompletionCelebration.findNextRecommendedModule(
            currentModuleId = "M09",
            allModules = MriRepository.modules,
            completedCodes = allCodes
        )
        assertNull(none)
    }

    @Test
    fun unlockedCharacterItemIsHiddenWhenLocksDisabled() {
        assertNull(ModuleCompletionCelebration.unlockedCharacterItemName("M02", enforceCurriculumLocks = false))
    }

    @Test
    fun readCompletionCelebrationWaitsUntilAllSkippedNodesAreActuallyRead() {
        val m02 = MriRepository.modules.first { it.id == "M02" }
        val allCodes = m02.nodes.map { it.code }
        val skipped = setOf(allCodes[0])
        val readBefore = allCodes.drop(1).dropLast(1).toSet()

        // Menyelesaikan simpul baca terakhir saat masih ada 1 simpul SKIP_TEST:
        // belum berhak memicu selebrasi modul tuntas (+50 XP).
        val lastUnread = allCodes.last()
        assertFalse(
            ModuleCompletionCelebration.shouldShowForReadCompletion(
                moduleId = "M02",
                module = m02,
                completedBefore = readBefore + skipped,
                skippedBefore = skipped,
                justCompletedCode = lastUnread,
                alreadyCelebratedModuleIds = emptySet()
            )
        )

        val progressWhileOneSkipped = ModuleCompletionCelebration.readProgressOf(
            module = m02,
            completedCodes = readBefore + skipped + lastUnread,
            skippedCodes = skipped
        )
        assertEquals(allCodes.size - 1, progressWhileOneSkipped.done)
        assertEquals(allCodes.size, progressWhileOneSkipped.total)

        // Ketika simpul yang tadinya SKIP_TEST akhirnya dibaca -> 100% READ -> selebrasi tampil!
        assertTrue(
            ModuleCompletionCelebration.shouldShowForReadCompletion(
                moduleId = "M02",
                module = m02,
                completedBefore = readBefore + skipped + lastUnread,
                skippedBefore = skipped,
                justCompletedCode = allCodes[0],
                alreadyCelebratedModuleIds = emptySet()
            )
        )
    }
}
