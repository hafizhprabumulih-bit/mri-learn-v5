package com.example.data.lesson

import com.example.data.LegacyNodeCodes
import com.example.data.MriRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LessonExtrasGuardTest {

    @Test
    fun emptyJson_returnsEmptyLessonExtrasSafely() {
        val parsed = LessonExtrasLoader.parseJson("""{"version":1,"nodes":{}}""")
        assertEquals(1, parsed.version)
        assertTrue(parsed.nodes.isEmpty())
    }

    @Test
    fun assetLessonExtrasJson_satisfiesAllCurriculumAndReviewRules() {
        val candidatePaths = listOf(
            File("src/main/assets/lesson_extras.json"),
            File("app/src/main/assets/lesson_extras.json")
        )
        val assetFile = candidatePaths.firstOrNull { it.exists() }
        assertNotNull("Berkas lesson_extras.json harus tersedia di src/main/assets/", assetFile)

        val jsonText = assetFile!!.readText()
        val extras = LessonExtrasLoader.parseJson(jsonText)

        val validCurriculumCodes = MriRepository.modules
            .flatMap { it.nodes }
            .mapTo(HashSet()) { LegacyNodeCodes.normalize(it.code) }

        val m01t0 = extras.forNode("M01.T0")
        assertNotNull("Entri M01.T0 harus tersedia di lesson_extras.json", m01t0)
        assertFalse("nanoHintsReviewed M01.T0 harus false saat masih draf", m01t0!!.nanoHintsReviewed)
        assertFalse("recapReviewed M01.T0 harus false saat masih draf", m01t0.recapReviewed)

        val seenStoryIds = HashSet<String>()

        for ((nodeKey, nodeExtras) in extras.nodes) {
            val normKey = LegacyNodeCodes.normalize(nodeKey)
            assertTrue(
                "Kode simpul $normKey pada lesson_extras.json tidak ditemukan di kurikulum",
                normKey in validCurriculumCodes
            )

            nodeExtras.nanoHints.forEach { hint ->
                assertTrue(
                    "nanoHint pada $normKey melebihi 90 karakter (${hint.length})",
                    hint.length <= 90
                )
            }

            nodeExtras.stories.forEach { story ->
                assertTrue(
                    "ID story '${story.id}' duplikat lintas simpul",
                    seenStoryIds.add(story.id)
                )
                assertTrue(
                    "ID story '${story.id}' harus diawali 'story:$normKey:'",
                    story.id.startsWith("story:$normKey:")
                )
                assertTrue(
                    "Panjang scenario pada ${story.id} melebihi 400 karakter (${story.scenario.length})",
                    story.scenario.length <= 400
                )
                assertEquals(
                    "Story ${story.id} harus memiliki tepat 4 opsi",
                    4,
                    story.options.size
                )
                assertEquals(
                    "Keempat opsi pada ${story.id} harus unik",
                    4,
                    story.options.map { it.trim() }.distinct().size
                )
                assertTrue(
                    "correctIndex pada ${story.id} di luar batas opsi",
                    story.correctIndex in story.options.indices
                )
                assertTrue(
                    "explanation pada ${story.id} tidak boleh kosong",
                    story.explanation.isNotBlank()
                )
                if (story.reviewed) {
                    assertTrue(
                        "Story ${story.id} bertanda reviewed=true wajib memiliki reviewedBy & reviewedAt",
                        !story.reviewedBy.isNullOrBlank() && !story.reviewedAt.isNullOrBlank()
                    )
                }
            }

            nodeExtras.interactive?.let { interactive ->
                assertEquals("REVEAL_CHAIN", interactive.type)
                assertTrue(
                    "Interactive steps pada $normKey harus berjumlah 3..6",
                    interactive.steps.size in 3..6
                )
                interactive.steps.forEach { step ->
                    assertTrue(step.text.isNotBlank())
                    step.prediction?.let { pred ->
                        assertTrue(pred.question.isNotBlank())
                        assertTrue(pred.options.size in 2..4)
                        assertEquals(
                            "Opsi prediction pada $normKey harus unik",
                            pred.options.size,
                            pred.options.map { it.trim() }.distinct().size
                        )
                        assertTrue(pred.correctIndex in pred.options.indices)
                    }
                }
            }
        }
    }

    @Test
    fun parseJson_skipsJsonNullArrayElementsAndSupportsReviewFlags() {
        val rawJson = """
            {
              "version": 1,
              "nodes": {
                "M01.T0": {
                  "nanoHints": [null, "Tip A", ""],
                  "nanoHintsReviewed": true,
                  "recap": {
                    "items": [null, "Ringkasan A"],
                    "reviewed": true
                  }
                }
              }
            }
        """.trimIndent()
        val parsed = LessonExtrasLoader.parseJson(rawJson)
        val node = parsed.forNode("M01.T0")
        assertNotNull(node)
        assertEquals(listOf("Tip A"), node!!.nanoHints)
        assertTrue(node.nanoHintsReviewed)
        assertEquals(listOf("Ringkasan A"), node.recap)
        assertTrue(node.recapReviewed)
    }
}
