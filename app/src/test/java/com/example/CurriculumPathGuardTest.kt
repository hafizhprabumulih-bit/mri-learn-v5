package com.example

import com.example.data.LegacyNodeCodes
import com.example.data.MriRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tes audit kurikulum (JVM murni) untuk memverifikasi fakta data sebelum membangun Peta Belajar.
 */
class CurriculumPathGuardTest {

    @Test
    fun totalModulesAndNodesMatchExpected() {
        val modules = MriRepository.modules
        val totalNodes = modules.sumOf { it.nodes.size }
        assertEquals("Total modul harus 18", 18, modules.size)
        assertEquals("Total simpul harus 186", 186, totalNodes)
    }

    @Test
    fun sortOrderPerModuleIsUniqueAndAscending() {
        val rawViolatingModules = mutableListOf<String>()

        MriRepository.modules.forEach { module ->
            val rawOrders = module.nodes.map { it.sortOrder }
            val isRawUniqueAndAscending = rawOrders.distinct().size == rawOrders.size &&
                rawOrders.zipWithNext().all { (a, b) -> a < b }
            if (!isRawUniqueAndAscending) {
                rawViolatingModules += module.id
            }

            val effectiveOrders = module.nodes.mapIndexed { idx, node ->
                if (node.sortOrder != 0) node.sortOrder else idx + 1
            }
            assertEquals(
                "Effective sortOrder pada modul ${module.id} harus unik",
                effectiveOrders.size,
                effectiveOrders.distinct().size
            )
            assertTrue(
                "Effective sortOrder pada modul ${module.id} harus berurutan menaik",
                effectiveOrders.zipWithNext().all { (a, b) -> a < b }
            )
        }

        println("Modul dengan raw MriNode.sortOrder belum diisi/tidak menaik (${rawViolatingModules.size}): $rawViolatingModules")
        assertEquals(EXPECTED_MODULES_WITH_UNSET_RAW_SORT_ORDER, rawViolatingModules)
    }

    @Test
    fun nodeLevelsAreWithinZeroToThree() {
        val invalidNodes = MriRepository.modules
            .flatMap { it.nodes }
            .filter { it.level !in 0..3 }
            .map { "${it.code} (level=${it.level})" }

        assertTrue("Semua simpul harus memiliki level 0..3, ditemukan: $invalidNodes", invalidNodes.isEmpty())
    }

    @Test
    fun prerequisitesResolveOrMatchKnownLegacyAnomalies() {
        val moduleIds = MriRepository.modules.map { it.id }.toSet()
        val nodeCodes = MriRepository.modules.flatMap { it.nodes }.map { it.code }.toSet()

        fun canResolveSingle(moduleId: String, rawValue: String): Boolean {
            val trimmed = rawValue.trim()
            if (trimmed.isEmpty()) return true
            val normalized = LegacyNodeCodes.normalize(trimmed)
            if (normalized in moduleIds) return true
            if (normalized in nodeCodes) return true
            val relativeNormalized = LegacyNodeCodes.normalize("$moduleId.$normalized")
            return relativeNormalized in nodeCodes
        }

        val unresolvedSingle = mutableListOf<String>()
        val unresolvedTokens = mutableListOf<String>()

        MriRepository.modules.forEach { module ->
            module.nodes.forEach { node ->
                val rawPrereq = node.prerequisite?.trim().orEmpty()
                if (rawPrereq.isNotEmpty()) {
                    if (!canResolveSingle(module.id, rawPrereq)) {
                        unresolvedSingle += "${node.code} -> $rawPrereq"
                    }
                    rawPrereq.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .forEach { token ->
                            if (!canResolveSingle(module.id, token)) {
                                unresolvedTokens += "${node.code} -> $token"
                            }
                        }
                }
            }
        }

        println("Prasyarat tidak terselesaikan sebagai token tunggal (${unresolvedSingle.size}):")
        unresolvedSingle.forEach { println("  - $it") }
        println("Token prasyarat tidak terselesaikan setelah pemisahan koma (${unresolvedTokens.size}):")
        unresolvedTokens.forEach { println("  - $it") }

        assertEquals(EXPECTED_NON_SINGLE_PREREQUISITES, unresolvedSingle)
        assertEquals(EXPECTED_UNRESOLVED_PREREQUISITE_TOKENS, unresolvedTokens)
    }

    @Test
    fun reportNodesWithoutLabType() {
        val allNodes = MriRepository.modules.flatMap { it.nodes }
        val withoutLab = allNodes.filter { it.labType.isNullOrBlank() }.map { it.code }
        println("Simpul tanpa labType (${withoutLab.size} dari ${allNodes.size}): $withoutLab")
        assertTrue(allNodes.isNotEmpty())
    }

    @Test
    fun reportQuizCountsAndNodesWithoutQuiz() {
        val quizzes = MriRepository.quizzes
        val countsByModule = quizzes.groupingBy { it.moduleId }.eachCount()
        println("Total kuis: ${quizzes.size}. Jumlah kuis per modul:")
        MriRepository.modules.forEach { module ->
            println("  - ${module.id}: ${countsByModule[module.id] ?: 0}")
        }

        val quizNodeCodes = quizzes.map { LegacyNodeCodes.normalize(it.nodeCode) }.toSet()
        val nodesWithoutQuiz = MriRepository.modules
            .flatMap { it.nodes }
            .map { it.code }
            .filter { LegacyNodeCodes.normalize(it) !in quizNodeCodes }

        println("Simpul tanpa kuis sama sekali (${nodesWithoutQuiz.size}): $nodesWithoutQuiz")
        assertTrue(quizzes.isNotEmpty())
    }

    @Test
    fun nodeCodesAreGloballyUnique() {
        val allCodes = MriRepository.modules.flatMap { it.nodes }.map { it.code }
        val duplicates = allCodes.groupingBy { it }.eachCount().filter { it.value > 1 }
        assertTrue("Tidak boleh ada kode simpul duplikat, ditemukan: $duplicates", duplicates.isEmpty())
    }

    companion object {
        // Modul yang mengandalkan default MriNode.sortOrder = 0 (diisi idx + 1 oleh toCurriculumNodeEntities).
        private val EXPECTED_MODULES_WITH_UNSET_RAW_SORT_ORDER = listOf(
            "M01", "M02", "M03A", "M03B", "M03C", "M04A", "M04B", "M04C", "M09"
        )

        // Daftar prasyarat lama yang tidak berbentuk kode tunggal (gabungan koma atau teks bebas).
        private val EXPECTED_NON_SINGLE_PREREQUISITES = listOf(
            "M05.T3.2 -> Opsional",
            "M06A.T1.1 -> M01, M04A",
            "M06A.T2.3 -> M05.T3.2, M06A.T2.1",
            "M06A.T2.5 -> M04A.T1.5, M06A.T2.1",
            "M06A.T2.6 -> M06A.T2.4, M06A.T2.5",
            "M06A.T3.1 -> M05.T2.4, M06A.T2.5",
            "M06B.T1.1 -> M01 · Dasar MRI",
            "M06B.T3.1 -> M05.T2.4, M06B.T1.1",
            "M06B.T3.2 -> M05.T3.1, M06B.T2.3",
            "M07A.T2.5 -> M07A.T1.2, M07A.T2.1",
            "M07B.T2.3 -> M07B.T2.1, M05.T2.5",
            "M07B.T3.2 -> M07B.T2.7, M07B.T3.1",
            "M07C.T2.5 -> M07C.T2.4, M07B.T2.5",
            "M07C.T3.2 -> M07C.T2.4, M06B.T3.2",
            "M07C.T3.4 -> M07C.T2.2, M07C.T2.3",
            "M08A.T1.2 -> M04B.T2.5, M04B.T2.6",
            "M08A.T1.3 -> M04A, M04B",
            "M08A.T2.3 -> M08A.T1.4, M03A",
            "M08A.T3.4 -> M08A.T1.2, M08A.T2.1",
            "M08B.T1.1 -> M03A, M04A",
            "M08B.T2.2 -> M08B.T1.1, M03A",
            "M08B.T2.4 -> M08B.T2.3, M06A.T2.5",
            "M08B.T2.5 -> M08B.T1.2, M07C.T2.2",
            "M08B.T2.6 -> M08B.T2.4, M08B.T2.5",
            "M08B.T3.2 -> M08B.T2.4, M08B.T3.1",
            "M08C.T2.1 -> M06B.T2.2, M04A",
            "M08C.T2.3 -> M05.T2.4, M06B.T2.3",
            "M08C.T2.5 -> M06A.T2.1, M06B.T2.4",
            "M08C.T2.6 -> M03A, M05.T1.3",
            "M08C.T3.3 -> M08A, M08B, M08C.T2.1"
        )

        // Token prasyarat yang memang berupa teks deskriptif (bukan id modul/kode simpul).
        private val EXPECTED_UNRESOLVED_PREREQUISITE_TOKENS = listOf(
            "M05.T3.2 -> Opsional",
            "M06B.T1.1 -> M01 · Dasar MRI"
        )
    }
}
