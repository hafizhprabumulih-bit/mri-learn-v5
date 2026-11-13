package com.example

import com.example.data.MriRepository
import com.example.util.SkipTestBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipTestBuilderTest {

    @Test
    fun buildsUpToThreeQuestionsFromSkippedNodesWhenAvailable() {
        val m01 = MriRepository.modules.first { it.id == "M01" }
        // Melompat ke simpul kelima di M01 melewati beberapa simpul yang memiliki kuis
        val target = m01.nodes[5].code
        val plan = SkipTestBuilder.build(
            targetNodeCode = target,
            module = m01,
            completedCodes = emptySet(),
            allQuizzes = MriRepository.quizzes,
            shuffleOptions = false
        )

        assertTrue(plan.canOfferSkipTest)
        assertTrue(plan.questions.size in 2..3)
        assertEquals(2, plan.requiredCorrect)
        val skippedCodes = plan.nodesToSkip.map { it.code }.toSet()
        assertTrue(plan.questions.all { it.nodeCode in skippedCodes })
    }

    @Test
    fun hidesSkipOptionWhenFewerThanTwoQuestionsExistInSkippedSlice() {
        val m01 = MriRepository.modules.first { it.id == "M01" }
        // Target adalah simpul kedua -> hanya simpul pertama (.T0) yang dilewati
        val target = m01.nodes[1].code
        val plan = SkipTestBuilder.build(
            targetNodeCode = target,
            module = m01,
            completedCodes = emptySet(),
            allQuizzes = MriRepository.quizzes
        )

        if (plan.questions.size < 2) {
            assertFalse(plan.canOfferSkipTest)
        } else {
            assertTrue(plan.canOfferSkipTest)
        }
    }

    @Test
    fun passingThresholdMatchesTwoThirdsRule() {
        assertEquals(2, SkipTestBuilder.requiredCorrectCount(3))
        assertEquals(2, SkipTestBuilder.requiredCorrectCount(2))
        assertFalse(SkipTestBuilder.isPassing(1, 3))
        assertTrue(SkipTestBuilder.isPassing(2, 3))
        assertTrue(SkipTestBuilder.isPassing(3, 3))
        assertFalse(SkipTestBuilder.isPassing(1, 2))
        assertTrue(SkipTestBuilder.isPassing(2, 2))
    }

    @Test
    fun excludesAlreadyCompletedNodesFromSkipList() {
        val m01 = MriRepository.modules.first { it.id == "M01" }
        val target = m01.nodes[4].code
        val completed = setOf(m01.nodes[0].code, m01.nodes[1].code)
        val plan = SkipTestBuilder.build(
            targetNodeCode = target,
            module = m01,
            completedCodes = completed,
            allQuizzes = MriRepository.quizzes
        )

        assertTrue(plan.nodesToSkip.none { it.code in completed })
    }
}
