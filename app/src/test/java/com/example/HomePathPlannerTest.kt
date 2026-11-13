package com.example

import com.example.data.MriRepository
import com.example.util.HomePathPlanner
import com.example.util.NodeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePathPlannerTest {

    @Test
    fun groupsEighteenModulesIntoNineChapters() {
        val chapters = HomePathPlanner.plan(MriRepository.modules, emptySet())
        assertEquals(9, chapters.size)
        assertEquals(18, chapters.sumOf { it.modules.size })
        assertEquals("M01", chapters.first().chapterKey)
        assertEquals("M09", chapters.last().chapterKey)
    }

    @Test
    fun assignsExactlyOneCurrentModuleAtStartAndNeverLocksModules() {
        val chapters = HomePathPlanner.plan(MriRepository.modules, emptySet())
        val allStates = chapters.flatMap { it.modules }

        val currentCount = allStates.count { it.status == NodeStatus.CURRENT }
        assertEquals(1, currentCount)
        assertEquals("M01", allStates.first { it.status == NodeStatus.CURRENT }.module.id)
        assertTrue(allStates.none { it.status == NodeStatus.LOCKED })
    }

    @Test
    fun completedFirstModuleAdvancesCurrentToSecondAndDimsLaterUnrecommended() {
        val m01Codes = MriRepository.modules.first { it.id == "M01" }.nodes.map { it.code }.toSet()
        val chapters = HomePathPlanner.plan(MriRepository.modules, m01Codes)
        val allStates = chapters.flatMap { it.modules }

        val m01State = allStates.first { it.module.id == "M01" }
        val m02State = allStates.first { it.module.id == "M02" }
        val m03aState = allStates.first { it.module.id == "M03A" }

        assertEquals(NodeStatus.DONE, m01State.status)
        assertEquals(NodeStatus.CURRENT, m02State.status)
        assertTrue(m02State.recommended)
        assertEquals(NodeStatus.OPEN, m03aState.status)
        assertFalse(m03aState.recommended)
        assertTrue(HomePathPlanner.isDimmed(m03aState))
        assertFalse(HomePathPlanner.moduleSubtitle(m03aState).contains("Disarankan"))
        assertTrue(HomePathPlanner.moduleSubtitle(m03aState).contains("simpul"))
        assertTrue(HomePathPlanner.moduleSubtitle(m02State).contains("simpul"))
    }
}
