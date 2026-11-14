package com.example

import com.example.data.MriModule
import com.example.data.MriNode
import com.example.data.local.QuizAttemptEntity
import com.example.util.SpacedRepetition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedRepetitionTest {

    private val sampleModules = listOf(
        MriModule(
            id = "M01",
            code = "M01",
            title = "Fisika Dasar",
            subtitle = "Dasar",
            description = "Desc",
            levelBadge = "T1",
            iconName = "science",
            nodes = listOf(
                MriNode(
                    code = "M01.T1.1",
                    title = "Spin Hidrogen",
                    level = 1,
                    summaryShort = "Ringkas",
                    detailFull = "Panjang",
                    analogi = "Analogi"
                ),
                MriNode(
                    code = "M01.T1.2",
                    title = "Frekuensi Larmor",
                    level = 1,
                    summaryShort = "Ringkas",
                    detailFull = "Panjang",
                    analogi = "Analogi"
                )
            )
        )
    )

    @Test
    fun `prioritizes low accuracy and stale attempts`() {
        val dayMs = 86_400_000L
        val now = 100L * dayMs
        val attempts = listOf(
            QuizAttemptEntity(
                quizId = "q1",
                nodeCode = "M01.T1.1",
                moduleId = "M01",
                selectedIndex = 0,
                isCorrect = false,
                answeredAt = now - dayMs
            ),
            QuizAttemptEntity(
                quizId = "q2",
                nodeCode = "M01.T1.2",
                moduleId = "M01",
                selectedIndex = 0,
                isCorrect = true,
                answeredAt = now - (5 * dayMs)
            )
        )

        val due = SpacedRepetition.selectDueReviews(attempts, sampleModules, nowMillis = now, maxItems = 3)
        assertEquals(2, due.size)
        assertEquals("M01.T1.1", due[0].nodeCode)
        assertEquals(0, due[0].accuracyPercent)
        assertEquals("M01.T1.2", due[1].nodeCode)
        assertEquals(5, due[1].daysSinceLastAttempt)
    }

    @Test
    fun `excludes recent high accuracy attempts`() {
        val dayMs = 86_400_000L
        val now = 100L * dayMs
        val attempts = listOf(
            QuizAttemptEntity(
                quizId = "q1",
                nodeCode = "M01.T1.1",
                moduleId = "M01",
                selectedIndex = 0,
                isCorrect = true,
                answeredAt = now - dayMs
            )
        )

        val due = SpacedRepetition.selectDueReviews(attempts, sampleModules, nowMillis = now)
        assertTrue(due.isEmpty())
    }

    @Test
    fun `respects manual and excluded review overrides and latest story attempt mastery`() {
        val dayMs = 86_400_000L
        val now = 100L * dayMs

        // story:M01.T1.1:1 salah di t=100 lalu benar di t=200 (retry dalam pelajaran):
        // status terbaru per quizId = benar -> tidak boleh dikunci sebagai akurasi 50% gagal
        val retryPassedAttempts = listOf(
            QuizAttemptEntity(
                quizId = "story:M01.T1.1:1",
                nodeCode = "M01.T1.1",
                moduleId = "M01",
                selectedIndex = 1,
                isCorrect = false,
                answeredAt = now - 2000L
            ),
            QuizAttemptEntity(
                quizId = "story:M01.T1.1:1",
                nodeCode = "M01.T1.1",
                moduleId = "M01",
                selectedIndex = 0,
                isCorrect = true,
                answeredAt = now - 1000L
            )
        )
        assertTrue(
            SpacedRepetition.selectDueReviews(
                attempts = retryPassedAttempts,
                modules = sampleModules,
                nowMillis = now
            ).isEmpty()
        )

        // Manual override tetap masuk walau tanpa error / tanpa riwayat kuis
        val withManual = SpacedRepetition.selectDueReviews(
            attempts = retryPassedAttempts,
            modules = sampleModules,
            nowMillis = now,
            manualReviewNodes = setOf("M01.T1.2")
        )
        assertEquals(listOf("M01.T1.2"), withManual.map { it.nodeCode })

        // Salah 2x pada story (terbaru salah) -> otomatis masuk, kecuali dikecualikan manual
        val failedTwiceAttempts = listOf(
            QuizAttemptEntity(
                quizId = "story:M01.T1.1:1",
                nodeCode = "M01.T1.1",
                moduleId = "M01",
                selectedIndex = 1,
                isCorrect = false,
                answeredAt = now - 2000L
            ),
            QuizAttemptEntity(
                quizId = "story:M01.T1.1:1",
                nodeCode = "M01.T1.1",
                moduleId = "M01",
                selectedIndex = 2,
                isCorrect = false,
                answeredAt = now - 1000L
            )
        )
        val autoFlagged = SpacedRepetition.selectDueReviews(
            attempts = failedTwiceAttempts,
            modules = sampleModules,
            nowMillis = now
        )
        assertEquals(listOf("M01.T1.1"), autoFlagged.map { it.nodeCode })

        val excludedAuto = SpacedRepetition.selectDueReviews(
            attempts = failedTwiceAttempts,
            modules = sampleModules,
            nowMillis = now,
            manualReviewNodes = setOf("M01.T1.1"),
            excludedReviewNodes = setOf("M01.T1.1")
        )
        assertTrue(excludedAuto.isEmpty())

        assertFalse(
            SpacedRepetition.isNodeQueuedForReview(
                nodeCode = "M01.T1.1",
                manualReviewNodes = setOf("M01.T1.1"),
                excludedReviewNodes = setOf("M01.T1.1"),
                autoNeedsReview = true
            )
        )
        assertTrue(
            SpacedRepetition.isNodeQueuedForReview(
                nodeCode = "M01.T1.2",
                manualReviewNodes = setOf("M01.T1.2"),
                excludedReviewNodes = emptySet(),
                autoNeedsReview = false
            )
        )
        assertTrue(
            SpacedRepetition.isNodeQueuedForReview(
                nodeCode = "M01.T1.2",
                manualReviewNodes = emptySet(),
                excludedReviewNodes = emptySet(),
                autoNeedsReview = true
            )
        )
    }
}
