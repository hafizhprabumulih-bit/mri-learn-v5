package com.example

import com.example.data.MriModule
import com.example.data.MriNode
import com.example.data.MriRepository
import com.example.util.ModuleProgress
import com.example.util.NodeStatus
import com.example.util.PathStateResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PathStateResolverTest {

    private fun testNode(
        code: String,
        sortOrder: Int = 0,
        level: Int = 1,
        prerequisite: String? = null
    ) = MriNode(
        code = code,
        level = level,
        title = "Judul $code",
        summaryShort = "Ringkasan $code",
        detailFull = "Detail $code",
        analogi = "Analogi $code",
        prerequisite = prerequisite,
        sortOrder = sortOrder
    )

    private fun testModule(
        id: String,
        nodes: List<MriNode>,
        sortOrder: Int = 0
    ) = MriModule(
        id = id,
        code = id,
        title = "Modul $id",
        subtitle = "Sub $id",
        description = "Desk $id",
        levelBadge = "Dasar",
        iconName = "science",
        nodes = nodes,
        sortOrder = sortOrder
    )

    @Test
    fun emptyProgressMakesOnlyFirstNodeCurrentAndRestLocked() {
        val m01 = testModule(
            id = "M01",
            nodes = listOf(
                testNode("M01.T1.1", sortOrder = 1),
                testNode("M01.T1.2", sortOrder = 2),
                testNode("M01.T1.3", sortOrder = 3)
            )
        )

        val resolved = PathStateResolver.resolveModule(m01, listOf(m01), emptySet())

        assertEquals(listOf(NodeStatus.CURRENT, NodeStatus.LOCKED, NodeStatus.LOCKED), resolved.nodes.map { it.status })
        assertEquals("M01.T1.1", PathStateResolver.currentNodeOf(resolved)?.node?.code)
        assertEquals(0, resolved.progress.done)
        assertEquals(3, resolved.progress.total)
    }

    @Test
    fun completingFirstNodeAdvancesCurrentToSecond() {
        val m01 = testModule(
            id = "M01",
            nodes = listOf(
                testNode("M01.T1.1", sortOrder = 1),
                testNode("M01.T1.2", sortOrder = 2),
                testNode("M01.T1.3", sortOrder = 3)
            )
        )

        val resolved = PathStateResolver.resolveModule(m01, listOf(m01), setOf("M01.T1.1"))

        assertEquals(listOf(NodeStatus.DONE, NodeStatus.CURRENT, NodeStatus.LOCKED), resolved.nodes.map { it.status })
        assertEquals("M01.T1.2", PathStateResolver.currentNodeOf(resolved)?.node?.code)
        assertEquals(1, resolved.progress.done)
    }

    @Test
    fun completedLaterNodeKeepsEarlierAndNextNodeUnlocked() {
        val m01 = testModule(
            id = "M01",
            nodes = listOf(
                testNode("M01.T1.1", sortOrder = 1),
                testNode("M01.T1.2", sortOrder = 2),
                testNode("M01.T1.3", sortOrder = 3),
                testNode("M01.T1.4", sortOrder = 4),
                testNode("M01.T1.5", sortOrder = 5)
            )
        )

        val resolved = PathStateResolver.resolveModule(
            module = m01,
            allModules = listOf(m01),
            completedCodes = setOf("M01.T1.1", "M01.T1.3")
        )

        assertEquals(
            listOf(
                NodeStatus.DONE,
                NodeStatus.CURRENT,
                NodeStatus.DONE,
                NodeStatus.OPEN,
                NodeStatus.LOCKED
            ),
            resolved.nodes.map { it.status }
        )
        assertEquals("M01.T1.2", PathStateResolver.currentNodeOf(resolved)?.node?.code)
    }

    @Test
    fun strictLocksFalseLeavesNoLockedNodesAndPreservesCurrent() {
        val m01 = testModule(
            id = "M01",
            nodes = listOf(
                testNode("M01.T1.1", sortOrder = 1),
                testNode("M01.T1.2", sortOrder = 2),
                testNode("M01.T1.3", sortOrder = 3)
            )
        )

        val resolved = PathStateResolver.resolveModule(
            module = m01,
            allModules = listOf(m01),
            completedCodes = setOf("M01.T1.1"),
            strictLocks = false
        )

        assertEquals(listOf(NodeStatus.DONE, NodeStatus.CURRENT, NodeStatus.OPEN), resolved.nodes.map { it.status })
        assertTrue(resolved.nodes.none { it.status == NodeStatus.LOCKED })
    }

    @Test
    fun legacyNodeCodesAreNormalizedBeforeResolving() {
        // Legacy "M03.T1.1" -> "M03A.T1.1", "M03.T1.2" -> "M03A.T1.3"
        val m03a = MriRepository.modules.first { it.id == "M03A" }
        val resolved = PathStateResolver.resolveModule(
            module = m03a,
            allModules = MriRepository.modules,
            completedCodes = setOf("M03.T1.1", "M03.T1.2")
        )

        val byCode = resolved.nodes.associateBy { it.node.code }
        assertEquals(NodeStatus.DONE, byCode["M03A.T1.1"]?.status)
        assertEquals(NodeStatus.CURRENT, byCode["M03A.T1.2"]?.status)
        assertEquals(NodeStatus.DONE, byCode["M03A.T1.3"]?.status)
        assertEquals(NodeStatus.OPEN, byCode["M03A.T1.4"]?.status)
        assertEquals(2, resolved.progress.done)
    }

    @Test
    fun togglingOffEarlierNodeDoesNotReLockLaterCompletedNode() {
        val m01 = testModule(
            id = "M01",
            nodes = listOf(
                testNode("M01.T1.1", sortOrder = 1),
                testNode("M01.T1.2", sortOrder = 2),
                testNode("M01.T1.3", sortOrder = 3),
                testNode("M01.T1.4", sortOrder = 4)
            )
        )

        // Simpul 2 dibatalkan, sementara simpul 3 masih selesai (bahkan jika 1 juga belum)
        val resolved = PathStateResolver.resolveModule(
            module = m01,
            allModules = listOf(m01),
            completedCodes = setOf("M01.T1.3")
        )

        assertEquals(
            listOf(NodeStatus.CURRENT, NodeStatus.OPEN, NodeStatus.DONE, NodeStatus.OPEN),
            resolved.nodes.map { it.status }
        )
    }

    @Test
    fun unmetModulePrerequisiteProvidesSoftHintWithoutLocking() {
        val m01 = testModule(
            id = "M01",
            nodes = listOf(
                testNode("M01.T1.1", sortOrder = 1),
                testNode("M01.T1.2", sortOrder = 2),
                testNode("M01.T1.3", sortOrder = 3),
                testNode("M01.T1.4", sortOrder = 4),
                testNode("M01.T1.5", sortOrder = 5)
            ),
            sortOrder = 1
        )
        val m02 = testModule(
            id = "M02",
            nodes = listOf(
                testNode("M02.T1.1", sortOrder = 1, prerequisite = "M01")
            ),
            sortOrder = 2
        )

        // M01 baru 2/5 = 40% (< 60%)
        val belowThreshold = PathStateResolver.resolveModule(
            module = m02,
            allModules = listOf(m01, m02),
            completedCodes = setOf("M01.T1.1", "M01.T1.2")
        )
        assertEquals(NodeStatus.CURRENT, belowThreshold.nodes[0].status)
        assertNotNull(belowThreshold.nodes[0].softPrerequisiteHint)
        assertTrue(belowThreshold.nodes[0].softPrerequisiteHint!!.contains("M01"))
        assertFalse(PathStateResolver.isModuleRecommended(listOf(m01, m02), setOf("M01.T1.1", "M01.T1.2"), "M02"))

        // M01 mencapai 3/5 = 60% (>= 60%)
        val atThreshold = PathStateResolver.resolveModule(
            module = m02,
            allModules = listOf(m01, m02),
            completedCodes = setOf("M01.T1.1", "M01.T1.2", "M01.T1.3")
        )
        assertNull(atThreshold.nodes[0].softPrerequisiteHint)
        assertTrue(PathStateResolver.isModuleRecommended(listOf(m01, m02), setOf("M01.T1.1", "M01.T1.2", "M01.T1.3"), "M02"))
    }

    @Test
    fun relativePrerequisiteResolvesWithinModule() {
        val m05 = testModule(
            id = "M05",
            nodes = listOf(
                testNode("M05.T1.1", sortOrder = 1),
                testNode("M05.T2.2", sortOrder = 2),
                testNode("M05.T2.3", sortOrder = 3, prerequisite = "T2.2")
            )
        )

        val unmet = PathStateResolver.resolveModule(
            module = m05,
            allModules = listOf(m05),
            completedCodes = setOf("M05.T1.1"),
            strictLocks = false
        )
        assertEquals("Disarankan: selesaikan M05.T2.2 dulu", unmet.nodes[2].softPrerequisiteHint)

        val met = PathStateResolver.resolveModule(
            module = m05,
            allModules = listOf(m05),
            completedCodes = setOf("M05.T1.1", "M05.T2.2"),
            strictLocks = false
        )
        assertNull(met.nodes[2].softPrerequisiteHint)
    }

    @Test
    fun moduleProgressFractionHandlesZeroTotalSafely() {
        val emptyProgress = ModuleProgress(moduleId = "M00", done = 0, total = 0)
        assertEquals(0f, emptyProgress.fraction, 0.0001f)
        assertFalse(emptyProgress.fraction.isNaN())
    }

    @Test
    fun realRepositoryM01SmokeTest() {
        val m01 = MriRepository.modules.first { it.id == "M01" }
        val resolvedEmpty = PathStateResolver.resolveModule(m01, MriRepository.modules, emptySet())

        assertEquals(23, resolvedEmpty.nodes.size)
        assertEquals(23, resolvedEmpty.progress.total)
        assertEquals(0, resolvedEmpty.progress.done)
        assertEquals(NodeStatus.CURRENT, resolvedEmpty.nodes.first().status)
        assertEquals("M01.T0", resolvedEmpty.nodes.first().node.code)
        assertTrue(resolvedEmpty.nodes.drop(1).all { it.status == NodeStatus.LOCKED })
        assertTrue(PathStateResolver.isModuleRecommended(MriRepository.modules, emptySet(), "M01"))
        assertFalse(PathStateResolver.isModuleRecommended(MriRepository.modules, emptySet(), "M02"))

        val allResolved = PathStateResolver.resolveAll(MriRepository.modules, emptySet())
        assertEquals(18, allResolved.size)
        assertEquals(186, allResolved.sumOf { it.progress.total })
    }
}
