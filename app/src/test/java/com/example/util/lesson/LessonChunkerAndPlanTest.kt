package com.example.util.lesson

import com.example.data.LegacyNodeCodes
import com.example.data.MriNode
import com.example.data.MriQuizQuestion
import com.example.data.MriRepository
import com.example.data.lesson.InteractiveContent
import com.example.data.lesson.LessonExtras
import com.example.data.lesson.NodeLessonExtras
import com.example.data.lesson.RevealChainStep
import com.example.data.lesson.StoryQuestion
import com.example.data.local.QuizAttemptEntity
import com.example.util.QuizStatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonChunkerAndPlanTest {

    @Test
    fun chunker_preservesM01T1_1ParenthesesAndModuleCodes() {
        val m01 = MriRepository.modules.first { it.id == "M01" }
        val node = m01.nodes.first { LegacyNodeCodes.normalize(it.code) == "M01.T1.1" }

        val chunks = LessonChunker.chunk(node.detailFull, node.summaryShort)
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.size <= 8)

        val joined = chunks.joinToString(" ") {
            if (it.leadIn != null) "${it.leadIn} ${it.text}" else it.text
        }
        if ("T1.7" in node.detailFull) {
            assertTrue("Harus menjaga rujukan T1.7 utuh", chunks.any { "T1.7" in (it.leadIn.orEmpty() + " " + it.text) })
        }
        assertTrue(joined.isNotBlank())
    }

    @Test
    fun chunker_handlesShortAndVeryLongStringsWithinEightChunks() {
        val shortChunks = LessonChunker.chunk("Teks sangat pendek.", "Ringkasan pendek simpul.")
        assertEquals(1, shortChunks.size)
        assertNull(shortChunks.first().leadIn)
        assertEquals("Ringkasan pendek simpul.", shortChunks.first().text)

        val longParagraphs = (1..14).joinToString("\n\n") { idx ->
            "Kalimat pembuka paragraf $idx. Penjelasan mendalam mengenai presesi Larmor dan medan magnet statis pada paragraf ke-$idx yang cukup panjang untuk menguji batas penggabungan potongan penjelasan bertahap. Kalimat tambahan ke-$idx menjelaskan waktu relaksasi longitudinal dan transversal secara rinci."
        }
        assertTrue(longParagraphs.length >= 2500)
        val longChunks = LessonChunker.chunk(longParagraphs, "Ringkasan panjang")
        assertTrue("Maksimal 8 potongan, aktual=${longChunks.size}", longChunks.size in 1..8)
        longChunks.forEach { chunk ->
            assertTrue(chunk.text.isNotBlank())
        }
    }

    @Test
    fun chunker_doesNotSplitOnAbbreviationsOrDecimalsOrParentheses() {
        val text = "Frekuensi kerja adalah 63,87 MHz pada magnet 1.5 T (mis. untuk pemeriksaan lutut dan otak, lihat T1.7 atau M07A. untuk detail keselamatan). Sinyal kemudian dibaca oleh kumparan penerima."
        val sentences = LessonChunker.splitSentences(text)
        assertEquals(2, sentences.size)
        assertTrue(sentences[0].contains("63,87 MHz"))
        assertTrue(sentences[0].contains("(mis. untuk pemeriksaan lutut dan otak, lihat T1.7 atau M07A. untuk detail keselamatan)"))
    }

    @Test
    fun chunker_singleSentenceParagraphHasNullLeadIn() {
        val singleSentence = "Proton hidrogen di dalam jaringan tubuh berpresesi mengelilingi arah medan magnet utama B0 dengan frekuensi yang sebanding dengan kekuatan medan magnet tersebut."
        val chunks = LessonChunker.chunk(singleSentence, "Ringkasan")
        assertEquals(1, chunks.size)
        assertNull(chunks[0].leadIn)
        assertEquals(singleSentence, chunks[0].text)
    }

    @Test
    fun chunker_runsCleanlyAcrossAll186CurriculumNodes() {
        val allNodes = MriRepository.modules.flatMap { it.nodes }
        assertTrue("Kurikulum harus memiliki minimal 180 simpul", allNodes.size >= 180)

        val chunkCounts = mutableListOf<Pair<String, Int>>()
        val maxChunkLengths = mutableListOf<Pair<String, Int>>()

        for (node in allNodes) {
            val chunks = LessonChunker.chunk(node.detailFull, node.summaryShort)
            assertTrue("Simpul ${node.code} menghasilkan 0 chunk", chunks.isNotEmpty())
            assertTrue("Simpul ${node.code} melebihi 8 chunk: ${chunks.size}", chunks.size <= 8)
            chunks.forEach { c ->
                assertTrue("Chunk kosong pada ${node.code}", c.text.isNotBlank())
            }
            chunkCounts.add(node.code to chunks.size)
            val longest = chunks.maxOf { (it.leadIn?.length ?: 0) + it.text.length }
            maxChunkLengths.add(node.code to longest)
        }

        val topByCount = chunkCounts.sortedByDescending { it.second }.take(5)
        val topByLength = maxChunkLengths.sortedByDescending { it.second }.take(5)
        println("Top 5 simpul dengan jumlah potongan terbanyak: $topByCount")
        println("Top 5 simpul dengan potongan terpanjang (karakter): $topByLength")
    }

    @Test
    fun planBuilder_handlesOptionalStepsAndCapsSegmentsAtTen() {
        val sampleNode = MriNode(
            code = "M01.T0",
            level = 0,
            title = "Tujuan Belajar",
            summaryShort = "Memahami dasar sinyal MRI.",
            detailFull = (1..8).joinToString("\n\n") { i ->
                "Inti bahasan $i. Proton hidrogen di dalam air dan lemak menghasilkan sinyal resonansi magnetik ketika diberi pulsa radiofrekuensi yang tepat pada langkah ke-$i."
            },
            analogi = "Seperti kompas kecil yang berputar serempak.",
            learningGoal = "Memahami rantai dasar sinyal MRI."
        )
        val quizzes = listOf(
            MriQuizQuestion(
                id = "Q_M01_T0_1",
                moduleId = "M01",
                nodeCode = "M01.T0",
                question = "Apa sumber sinyal utama MRI klinis?",
                options = listOf("Hidrogen", "Kalsium", "Besi", "Udara"),
                correctIndex = 0,
                explanation = "Hidrogen melimpah di air dan lemak.",
                levelBadge = "T1",
                level = 1
            )
        )
        val extras = LessonExtras(
            version = 1,
            nodes = mapOf(
                "M01.T0" to NodeLessonExtras(
                    nodeCode = "M01.T0",
                    nanoHints = listOf("Petunjuk 1"),
                    recap = listOf("Poin 1"),
                    stories = listOf(
                        StoryQuestion(
                            id = "story:M01.T0:1",
                            scenario = "Skenario 1",
                            question = "Pertanyaan 1?",
                            options = listOf("A", "B"),
                            correctIndex = 0,
                            explanation = "Penjelasan 1",
                            reviewed = true,
                            reviewedBy = "Pakar",
                            reviewedAt = "2026-11-14"
                        ),
                        StoryQuestion(
                            id = "story:M01.T0:2",
                            scenario = "Skenario 2",
                            question = "Pertanyaan 2?",
                            options = listOf("A", "B"),
                            correctIndex = 1,
                            explanation = "Penjelasan 2",
                            reviewed = true,
                            reviewedBy = "Pakar",
                            reviewedAt = "2026-11-14"
                        )
                    ),
                    interactive = InteractiveContent(
                        title = "Urutan Sinyal",
                        intro = "Pengantar",
                        steps = listOf(
                            RevealChainStep("Langkah 1"),
                            RevealChainStep("Langkah 2")
                        ),
                        reviewed = true
                    )
                )
            )
        )

        val plan = LessonPlanBuilder.build(
            node = sampleNode,
            quizzes = quizzes,
            extras = extras,
            allowDrafts = false
        )
        assertTrue("Segment count harus <= 10, aktual=${plan.segmentCount}", plan.segmentCount <= 10)
        assertTrue(plan.steps.first() is LessonStep.Opening)
        assertTrue(plan.steps.last() is LessonStep.Complete)
        assertTrue(plan.steps.any { it is LessonStep.Analogy })
        assertTrue(plan.steps.any { it is LessonStep.Interactive })
        assertEquals(2, plan.steps.count { it is LessonStep.Story })
        assertEquals(1, plan.steps.count { it is LessonStep.Check })
    }

    @Test
    fun reducer_enforcesTwoAttemptQuestionRulesAndBackNavigation() {
        val node = MriNode(
            code = "M01.T0",
            level = 0,
            title = "Tujuan Belajar",
            summaryShort = "Ringkasan singkat.",
            detailFull = "Paragraf penjelasan pertama yang cukup panjang untuk membentuk satu langkah penjelasan utuh di dalam rencana belajar.",
            analogi = ""
        )
        val quizzes = listOf(
            MriQuizQuestion(
                id = "Q1",
                moduleId = "M01",
                nodeCode = "M01.T0",
                question = "Soal uji?",
                options = listOf("Benar", "Salah 1", "Salah 2"),
                correctIndex = 0,
                explanation = "Penjelasan",
                levelBadge = "T1",
                level = 1
            )
        )
        val plan = LessonPlanBuilder.build(node, quizzes)
        // Steps: 0=Opening, 1=Explain, 2=Check("Q1"), 3=Complete
        var state = LessonState(plan = plan)
        assertEquals(0, state.index)

        // Maju ke Explain (index 1) lalu ke Check (index 2)
        state = LessonReducer.reduce(state, LessonEvent.Next)
        assertEquals(1, state.index)
        state = LessonReducer.reduce(state, LessonEvent.Next)
        assertEquals(2, state.index)

        // Tidak bisa lanjut sebelum menjawab Check
        val blocked = LessonReducer.reduce(state, LessonEvent.Next)
        assertEquals(2, blocked.index)

        // Percobaan 1 salah -> RETRY_ALLOWED
        state = LessonReducer.reduce(state, LessonEvent.Answer("Q1", optionIndex = 1, correctIndex = 0))
        assertEquals(QuestionPhase.RETRY_ALLOWED, state.phaseFor("Q1"))
        assertFalse(state.finished)

        // Ketuk Retry -> pilih jawaban benar di percobaan 2 -> RESOLVED_CORRECT
        state = LessonReducer.reduce(state, LessonEvent.Retry("Q1"))
        state = LessonReducer.reduce(state, LessonEvent.Answer("Q1", optionIndex = 0, correctIndex = 0))
        assertEquals(QuestionPhase.RESOLVED_CORRECT, state.phaseFor("Q1"))

        // Tombol Back dari Check yang sudah dijawab harus kembali ke Explain (index 1)
        val afterBack = LessonReducer.reduce(state, LessonEvent.Back)
        assertEquals(1, afterBack.index)

        // Maju kembali ke Check lalu ke Complete -> finished = true
        state = LessonReducer.reduce(state, LessonEvent.Next)
        assertEquals(3, state.index)
        assertTrue(state.finished)
    }

    @Test
    fun masteryStars_andQuizStatsCalculator_behaveCorrectly() {
        val attempts = listOf(
            // Urutan acak: percobaan kedua (benar, t=200) muncul sebelum percobaan pertama (salah, t=100)
            AttemptRecord(quizId = "story:M01.T0:1", isCorrect = true, answeredAt = 200L),
            AttemptRecord(quizId = "story:M01.T0:1", isCorrect = false, answeredAt = 100L),
            AttemptRecord(quizId = "Q_CHECK_1", isCorrect = true, answeredAt = 150L)
        )
        // story:M01.T0:1 salah pada percobaan pertama (t=100), sedangkan Q_CHECK_1 benar pada percobaan pertama (t=150)
        val stars = MasteryStars.stars(attempts, listOf("story:M01.T0:1", "Q_CHECK_1"))
        assertEquals(2, stars)
        assertFalse("Percobaan terakhir keduanya benar, jadi tidak wajib ulas", MasteryStars.needsReview(attempts, listOf("story:M01.T0:1", "Q_CHECK_1")))

        // Jika percobaan terakhir salah (salah 2x) -> needsReview = true
        val wrongTwice = listOf(
            AttemptRecord(quizId = "Q_CHECK_1", isCorrect = false, answeredAt = 100L),
            AttemptRecord(quizId = "Q_CHECK_1", isCorrect = false, answeredAt = 200L)
        )
        assertEquals(1, MasteryStars.stars(wrongTwice, listOf("Q_CHECK_1")))
        assertTrue(MasteryStars.needsReview(wrongTwice, listOf("Q_CHECK_1")))

        // QuizStatsCalculator harus mengabaikan attempt `story:*`
        val catalogQuizzes = listOf(
            MriQuizQuestion(
                id = "Q_CHECK_1",
                moduleId = "M01",
                nodeCode = "M01.T0",
                question = "Pertanyaan?",
                options = listOf("A", "B"),
                correctIndex = 0,
                explanation = "Penjelasan",
                levelBadge = "T1",
                level = 1
            )
        )
        val dbAttempts = listOf(
            QuizAttemptEntity(id = 1, quizId = "story:M01.T0:1", nodeCode = "M01.T0", moduleId = "M01", selectedIndex = 1, isCorrect = false, answeredAt = 100L),
            QuizAttemptEntity(id = 2, quizId = "Q_CHECK_1", nodeCode = "M01.T0", moduleId = "M01", selectedIndex = 0, isCorrect = true, answeredAt = 200L)
        )
        val stats = QuizStatsCalculator.compute(catalogQuizzes, dbAttempts, "M01")
        assertEquals(1, stats.totalAttempts)
        assertEquals(1, stats.correctAttempts)
        assertEquals(100, stats.accuracyPct)
        assertTrue(stats.stillWrongAttempts.isEmpty())

        // Filter SEMUA dan prefix modul 3 karakter ("M03" -> "M03A"/"M03B") tetap menghitung katalog & mengabaikan story:*
        val m03Quizzes = listOf(
            catalogQuizzes.first().copy(id = "Q_M03A", moduleId = "M03A", nodeCode = "M03A.T1.1"),
            catalogQuizzes.first().copy(id = "Q_M03B", moduleId = "M03B", nodeCode = "M03B.T1.1")
        )
        val m03Attempts = listOf(
            QuizAttemptEntity(id = 10, quizId = "story:M03A.T1.1:1", nodeCode = "M03A.T1.1", moduleId = "M03A", selectedIndex = 1, isCorrect = false, answeredAt = 100L),
            QuizAttemptEntity(id = 11, quizId = "Q_M03A", nodeCode = "M03A.T1.1", moduleId = "M03A", selectedIndex = 0, isCorrect = true, answeredAt = 200L),
            QuizAttemptEntity(id = 12, quizId = "Q_M03B", nodeCode = "M03B.T1.1", moduleId = "M03B", selectedIndex = 1, isCorrect = false, answeredAt = 300L)
        )
        val semuaStats = QuizStatsCalculator.compute(catalogQuizzes + m03Quizzes, dbAttempts + m03Attempts, "SEMUA")
        assertEquals(3, semuaStats.totalAttempts)
        assertEquals(2, semuaStats.correctAttempts)
        val m03Stats = QuizStatsCalculator.compute(m03Quizzes, m03Attempts, "M03")
        assertEquals(2, m03Stats.totalAttempts)
        assertEquals(1, m03Stats.correctAttempts)
        assertEquals(50, m03Stats.accuracyPct)
        assertEquals(listOf("Q_M03B"), m03Stats.stillWrongAttempts.map { it.quizId })
    }

    @Test
    fun chunker_splitsSingleLetterUnitPeriodsAndHandlesUnmatchedParen() {
        val unitText = "Helium cair menjaga kumparan sekitar 9 K. Kabel harus terlindungi sepanjang 10 m. Medan mencapai 10.000 G. Waktu pulih sekitar 64 s. Reduksi kebisingan R. Sinyal diamati berikutnya."
        val sentences = LessonChunker.splitSentences(unitText)
        assertEquals("Kalimat unit 1-huruf harus terpecah menjadi 6: $sentences", 6, sentences.size)

        val unmatchedParenText = "Nilai batas B0 dapat berubah (mis. pada protokol cepat. Operator harus memeriksa ulang SAR sebelum memindai."
        val unmatchedSentences = LessonChunker.splitSentences(unmatchedParenText)
        assertEquals(2, unmatchedSentences.size)
    }

    @Test
    fun planBuilder_gatesNanoHintsByReviewedAndPreservesTrailingHintAndDraftFlag() {
        val sampleNode = MriNode(
            code = "M01.T1.1",
            level = 1,
            title = "Spin Hidrogen",
            summaryShort = "Ringkasan spin.",
            detailFull = (1..8).joinToString("\n\n") { i ->
                "Kalimat pertama paragraf ke-$i sangat jelas. Penjelasan tambahan paragraf ke-$i cukup panjang agar menghasilkan tepat satu potongan."
            },
            analogi = "Analogi kompas."
        )
        val unreviewedExtras = LessonExtras(
            version = 1,
            nodes = mapOf(
                "M01.T1.1" to NodeLessonExtras(
                    nodeCode = "M01.T1.1",
                    nanoHints = List(8) { idx -> if (idx == 7) "Hint ekor terakhir" else "" },
                    nanoHintsReviewed = false,
                    interactive = InteractiveContent(
                        title = "Draf Interaktif",
                        intro = "Pengantar",
                        steps = listOf(RevealChainStep("1"), RevealChainStep("2"), RevealChainStep("3")),
                        reviewed = false
                    )
                )
            )
        )

        val prodPlan = LessonPlanBuilder.build(sampleNode, emptyList(), unreviewedExtras, allowDrafts = false)
        assertTrue(prodPlan.steps.filterIsInstance<LessonStep.Explain>().all { it.nanoHint == null })
        assertTrue(prodPlan.steps.none { it is LessonStep.Interactive })

        val draftPlan = LessonPlanBuilder.build(sampleNode, emptyList(), unreviewedExtras, allowDrafts = true)
        val interactiveStep = draftPlan.steps.filterIsInstance<LessonStep.Interactive>().singleOrNull()
        assertNotNull(interactiveStep)
        assertTrue(interactiveStep!!.isDraft)

        val reviewedExtras = LessonExtras(
            version = 1,
            nodes = mapOf(
                "M01.T1.1" to unreviewedExtras.forNode("M01.T1.1")!!.copy(
                    nanoHintsReviewed = true,
                    interactive = unreviewedExtras.forNode("M01.T1.1")!!.interactive!!.copy(reviewed = true)
                )
            )
        )
        val reviewedPlan = LessonPlanBuilder.build(sampleNode, emptyList(), reviewedExtras, allowDrafts = false)
        val reviewedExplainHints = reviewedPlan.steps.filterIsInstance<LessonStep.Explain>().mapNotNull { it.nanoHint }
        assertTrue("Hint ekor harus ikut terbawa saat mergeChunksToLimit menggabung chunk", reviewedExplainHints.any { "Hint ekor terakhir" in it })
        assertFalse(reviewedPlan.steps.filterIsInstance<LessonStep.Interactive>().single().isDraft)
    }

    @Test
    fun reducer_encodesDecodesRestoresProgressAndAllowsBackFromUnansweredQuestion() {
        val sampleNode = MriNode(
            code = "M01.T0",
            level = 0,
            title = "Tujuan",
            summaryShort = "Ringkasan",
            detailFull = "Paragraf penjelasan cukup panjang untuk membuat satu langkah Explain yang jelas bagi pembelajar.",
            analogi = ""
        )
        val quizzes = listOf(
            MriQuizQuestion(
                id = "Q_SPECIAL=1;A%B",
                moduleId = "M01",
                nodeCode = "M01.T0",
                question = "Soal?",
                options = listOf("Benar", "Salah"),
                correctIndex = 0,
                explanation = "Penjelasan",
                levelBadge = "T1",
                level = 1
            )
        )
        val plan = LessonPlanBuilder.build(sampleNode, quizzes)
        // Steps: 0=Opening, 1=Explain, 2=Check, 3=Complete
        val encoded = LessonReducer.encodeAttempts(mapOf("Q_SPECIAL=1;A%B" to listOf(1, 0)))
        val decoded = LessonReducer.decodeAttempts(encoded)
        assertEquals(listOf(1, 0), decoded["Q_SPECIAL=1;A%B"])

        val restored = LessonReducer.restoreProgress(
            state = LessonState(plan = plan),
            savedIndex = 2,
            encodedAttempts = encoded,
            correctIndexByItem = mapOf("Q_SPECIAL=1;A%B" to 0)
        )
        assertEquals(2, restored.index)
        assertEquals(QuestionPhase.RESOLVED_CORRECT, restored.phaseFor("Q_SPECIAL=1;A%B"))

        // Dari Check yang belum dijawab, LessonEvent.Back harus bisa mundur ke Explain
        val unansweredAtCheck = LessonState(plan = plan, index = 2)
        val backedToExplain = LessonReducer.reduce(unansweredAtCheck, LessonEvent.Back)
        assertEquals(1, backedToExplain.index)

        // Caption bintang 3 tanpa check & 2 dengan story+check
        assertEquals(
            "3 bintang: kedua soal cerita benar di percobaan pertama",
            MasteryStars.starCaption(stars = 3, hasStory = true, hasCheck = false)
        )
        assertEquals(
            "2 bintang: salah satu soal evaluasi benar di percobaan pertama",
            MasteryStars.starCaption(stars = 2, hasStory = true, hasCheck = true)
        )
    }
}
