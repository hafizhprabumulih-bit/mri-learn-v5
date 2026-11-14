package com.example.data.lesson

import com.example.data.LegacyNodeCodes
import com.example.data.MriRepository
import org.junit.Assert.assertEquals
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
                    "ID story '${story.id}' harus diawali 'story:$normKey:'",
                    story.id.startsWith("story:$normKey:")
                )
                assertTrue(
                    "Jumlah opsi pada ${story.id} harus 2..4",
                    story.options.size in 2..4
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
                    "Interactive steps pada $normKey harus berjumlah 2..6",
                    interactive.steps.size in 2..6
                )
                interactive.steps.forEach { step ->
                    assertTrue(step.text.isNotBlank())
                    step.prediction?.let { pred ->
                        assertTrue(pred.question.isNotBlank())
                        assertTrue(pred.options.size in 2..4)
                        assertTrue(pred.correctIndex in pred.options.indices)
                    }
                }
            }
        }
    }
}
