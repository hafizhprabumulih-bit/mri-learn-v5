package com.example

import com.example.data.MriQuizQuestion
import com.example.data.MriRepository
import com.example.util.QuizSessionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizSessionResultTest {

    private fun makeQuizzes(nodeCodes: List<String>): List<MriQuizQuestion> =
        nodeCodes.mapIndexed { idx, code ->
            MriQuizQuestion(
                id = "Q_$idx",
                moduleId = "M01",
                nodeCode = code,
                level = 1,
                question = "Soal $idx",
                options = listOf("A", "B", "C", "D"),
                correctIndex = 0,
                explanation = "Pembahasan $idx"
            )
        }

    @Test
    fun allCorrectProducesThreeStarsAndEmptyReviewList() {
        val quizzes = makeQuizzes(listOf("M01.T1.1", "M01.T1.2", "M01.T1.3", "M01.T1.4"))
        val answers = quizzes.associate { it.id to true }
        val result = QuizSessionResult.compute(answers, quizzes, MriRepository.modules)

        assertEquals(4, result.correctCount)
        assertEquals(0, result.wrongCount)
        assertEquals(100, result.percentage)
        assertTrue(result.passed)
        assertEquals(3, result.stars)
        assertTrue(result.reviewNodes.isEmpty())
        assertTrue(result.wrongQuizIds.isEmpty())
    }

    @Test
    fun allWrongProducesZeroStarsAndNotPassed() {
        val quizzes = makeQuizzes(listOf("M01.T1.1", "M01.T1.2"))
        val answers = quizzes.associate { it.id to false }
        val result = QuizSessionResult.compute(answers, quizzes, MriRepository.modules)

        assertEquals(0, result.correctCount)
        assertEquals(2, result.wrongCount)
        assertEquals(0, result.percentage)
        assertFalse(result.passed)
        assertEquals(0, result.stars)
        assertEquals(2, result.reviewNodes.size)
    }

    @Test
    fun exactlySeventyPercentPassesWithOneStar() {
        val quizzes = makeQuizzes((1..10).map { "M01.T1.1" })
        val answers = quizzes.mapIndexed { i, q -> q.id to (i < 7) }.toMap()
        val result = QuizSessionResult.compute(answers, quizzes, MriRepository.modules)

        assertEquals(70, result.percentage)
        assertTrue(result.passed)
        assertEquals(1, result.stars)
        assertEquals(3, result.wrongQuizIds.size)
        // Tiga soal salah pada simpul yang sama dikelompokkan menjadi 1 baris
        assertEquals(1, result.reviewNodes.size)
        assertEquals(3, result.reviewNodes.first().wrongCount)
    }

    @Test
    fun blankNodeCodeIsOmittedFromReviewNodesButCountedInWrongQuizIds() {
        val quizzes = makeQuizzes(listOf("", "M01.T1.2", "M01.T1.1", "M01.T1.1"))
        val answers = mapOf(
            "Q_0" to false,
            "Q_1" to false,
            "Q_2" to false,
            "Q_3" to false
        )
        val result = QuizSessionResult.compute(answers, quizzes, MriRepository.modules)

        assertEquals(4, result.wrongQuizIds.size)
        assertEquals(2, result.reviewNodes.size)
        assertEquals("M01.T1.1", result.reviewNodes[0].nodeCode)
        assertEquals(2, result.reviewNodes[0].wrongCount)
        assertEquals("M01.T1.2", result.reviewNodes[1].nodeCode)
        assertEquals(1, result.reviewNodes[1].wrongCount)
    }
}
