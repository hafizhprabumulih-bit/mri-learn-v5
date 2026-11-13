package com.example

import com.example.data.MriModule
import com.example.data.MriNode
import com.example.data.MriRepository
import com.example.util.NodeDisplay
import com.example.util.NodeStatus
import com.example.util.PathStateResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeDisplayAndPositionTest {

    private fun node(code: String, sortOrder: Int = 0) = MriNode(
        code = code,
        level = 1,
        title = "Judul $code",
        summaryShort = "Ringkasan $code",
        detailFull = "Detail $code",
        analogi = "Analogi $code",
        sortOrder = sortOrder
    )

    private fun module(nodes: List<MriNode>) = MriModule(
        id = "M01",
        code = "M01",
        title = "Modul M01",
        subtitle = "Sub",
        description = "Desk",
        levelBadge = "Dasar",
        iconName = "science",
        nodes = nodes
    )

    @Test
    fun shortCodeFollowsRealCodePatterns() {
        assertEquals("T1.1", NodeDisplay.shortCode("M01.T1.1"))
        assertEquals("T0", NodeDisplay.shortCode("M01.T0"))
        assertEquals("Ringkasan T1", NodeDisplay.shortCode("M01.T1.R"))
        assertEquals("T1.4A", NodeDisplay.shortCode("M01.T1.4A"))
        assertEquals("T2.10.5", NodeDisplay.shortCode("M04B.T2.10.5"))
        assertEquals("T1.1", NodeDisplay.shortCode("M03A.T1.1"))
    }

    @Test
    fun shortCodeReturnsUnknownCodesUnchanged() {
        assertEquals("FLASHCARD_REVIEW", NodeDisplay.shortCode("FLASHCARD_REVIEW"))
        assertEquals("M01", NodeDisplay.shortCode("M01"))
        assertEquals("abc.def", NodeDisplay.shortCode("abc.def"))
        assertEquals("M01.", NodeDisplay.shortCode("M01."))
    }

    @Test
    fun displayTitleStripsLeadingCodePrefixOnly() {
        assertEquals("Peran MRI dalam praktik klinis", NodeDisplay.displayTitle("T1.1 Peran MRI dalam praktik klinis"))
        assertEquals("Fakta 6: Dimensi ruang-k", NodeDisplay.displayTitle("T2.10.5 Fakta 6: Dimensi ruang-k"))
        assertEquals("T2* pada gradient echo", NodeDisplay.displayTitle("T2.3 T2* pada gradient echo"))
        assertEquals("Ringkasan Tingkat 1", NodeDisplay.displayTitle("Ringkasan Tingkat 1"))
        assertEquals("Relaksasi T1 (Spin-Lattice)", NodeDisplay.displayTitle("Relaksasi T1 (Spin-Lattice)"))
        assertEquals("T1.1", NodeDisplay.displayTitle("T1.1"))
    }

    @Test
    fun everyCurriculumNodeHasNonBlankDisplayLabels() {
        MriRepository.modules.flatMap { it.nodes }.forEach { n ->
            assertTrue("Judul tampilan kosong: ${n.code}", NodeDisplay.displayTitle(n).isNotBlank())
            val short = NodeDisplay.shortCode(n.code)
            assertFalse("Kode pendek masih memuat modul: ${n.code} -> $short", short.startsWith("M"))
        }
    }

    @Test
    fun nextNodeAndPositionFollowPathOrder() {
        val mod = module(
            listOf(
                node("M01.T1.2", sortOrder = 3),
                node("M01.T0", sortOrder = 1),
                node("M01.T1.1", sortOrder = 2)
            )
        )
        val state = PathStateResolver.resolveModule(mod, listOf(mod), emptySet(), strictLocks = false)

        assertEquals("M01.T1.1", PathStateResolver.nextNodeOf(state, "M01.T0")?.node?.code)
        assertEquals("M01.T1.2", PathStateResolver.nextNodeOf(state, "M01.T1.1")?.node?.code)
        assertNull(PathStateResolver.nextNodeOf(state, "M01.T1.2"))
        assertNull(PathStateResolver.nextNodeOf(state, "M09.T9.9"))

        assertEquals(Pair(1, 3), PathStateResolver.positionOf(state, "M01.T0"))
        assertEquals(Pair(3, 3), PathStateResolver.positionOf(state, "M01.T1.2"))
        assertEquals(Pair(0, 3), PathStateResolver.positionOf(state, "tidak-ada"))
    }

    @Test
    fun softLocksNeverProduceLockedNodes() {
        val mod = module(listOf(node("M01.T0", 1), node("M01.T1.1", 2), node("M01.T1.2", 3)))
        val state = PathStateResolver.resolveModule(mod, listOf(mod), emptySet(), strictLocks = false)
        assertTrue(state.nodes.none { it.status == NodeStatus.LOCKED })
        assertEquals(NodeStatus.CURRENT, state.nodes.first().status)
    }
}
