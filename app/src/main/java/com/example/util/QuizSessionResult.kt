package com.example.util

import com.example.data.LegacyNodeCodes
import com.example.data.MriModule
import com.example.data.MriQuizQuestion

data class QuizReviewNodeItem(
    val nodeCode: String,
    val nodeTitle: String,
    val wrongCount: Int
)

data class QuizSessionSummary(
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val percentage: Int,
    val passed: Boolean,
    val stars: Int,
    val wrongQuizIds: List<String>,
    val reviewNodes: List<QuizReviewNodeItem>,
    val earnedXp: Int
)

/**
 * Perhitungan murni hasil sesi kuis (skor, bintang, dan daftar simpul "Perlu diulang").
 */
object QuizSessionResult {
    const val PASSING_PERCENT = 70

    fun compute(
        sessionAnswers: Map<String, Boolean>,
        sessionQuizzes: List<MriQuizQuestion>,
        modules: List<MriModule>
    ): QuizSessionSummary {
        val total = sessionQuizzes.size
        if (total == 0) {
            return QuizSessionSummary(
                totalQuestions = 0,
                correctCount = 0,
                wrongCount = 0,
                percentage = 0,
                passed = false,
                stars = 0,
                wrongQuizIds = emptyList(),
                reviewNodes = emptyList(),
                earnedXp = 0
            )
        }

        val correctCount = sessionQuizzes.count { sessionAnswers[it.id] == true }
        val wrongQuizzes = sessionQuizzes.filter { sessionAnswers[it.id] == false }
        val wrongCount = total - correctCount
        val percentage = (correctCount * 100) / total
        val passed = percentage >= PASSING_PERCENT
        val stars = when {
            percentage >= 100 -> 3
            percentage >= 85 -> 2
            percentage >= PASSING_PERCENT -> 1
            else -> 0
        }

        val nodeOrderMap = HashMap<String, Int>()
        val nodeTitleMap = HashMap<String, String>()
        var orderCounter = 0
        modules.forEach { mod ->
            mod.nodes.forEach { node ->
                val norm = LegacyNodeCodes.normalize(node.code)
                nodeOrderMap[norm] = orderCounter++
                nodeTitleMap[norm] = NodeDisplay.displayTitle(node)
            }
        }

        val groupedWrong = LinkedHashMap<String, Int>()
        wrongQuizzes.forEach { quiz ->
            val norm = LegacyNodeCodes.normalize(quiz.nodeCode)
            if (norm.isNotBlank()) {
                groupedWrong[norm] = (groupedWrong[norm] ?: 0) + 1
            }
        }

        val reviewNodes = groupedWrong.entries
            .sortedBy { (normCode, _) -> nodeOrderMap[normCode] ?: Int.MAX_VALUE }
            .map { (normCode, count) ->
                QuizReviewNodeItem(
                    nodeCode = normCode,
                    nodeTitle = nodeTitleMap[normCode] ?: normCode,
                    wrongCount = count
                )
            }

        return QuizSessionSummary(
            totalQuestions = total,
            correctCount = correctCount,
            wrongCount = wrongCount,
            percentage = percentage,
            passed = passed,
            stars = stars,
            wrongQuizIds = wrongQuizzes.map { it.id },
            reviewNodes = reviewNodes,
            earnedXp = correctCount * XpCalculator.XP_PER_CORRECT_QUIZ
        )
    }
}
