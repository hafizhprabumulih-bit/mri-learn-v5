package com.example.util

import com.example.data.LegacyNodeCodes
import com.example.data.MriModule
import com.example.data.MriNode
import com.example.data.QuizQuestion

data class SkipTestPlan(
    val targetNodeCode: String,
    val nodesToSkip: List<MriNode>,
    val questions: List<QuizQuestion>,
    val requiredCorrect: Int
) {
    val canOfferSkipTest: Boolean
        get() = nodesToSkip.isNotEmpty() && questions.size >= MIN_QUESTIONS

    companion object {
        const val MIN_QUESTIONS = 2
        const val MAX_QUESTIONS = 3

        fun empty(targetNodeCode: String): SkipTestPlan = SkipTestPlan(
            targetNodeCode = targetNodeCode,
            nodesToSkip = emptyList(),
            questions = emptyList(),
            requiredCorrect = 0
        )
    }
}

/**
 * Penyusun tes singkat untuk fitur "Lompat dengan tes singkat" saat pengguna mengetuk
 * simpul terkunci pada Peta Modul.
 */
object SkipTestBuilder {

    fun requiredCorrectCount(totalQuestions: Int): Int {
        if (totalQuestions <= 0) return 0
        // Pembulatan ke atas dari 2/3 * totalQuestions: 2 soal -> 2, 3 soal -> 2
        return (totalQuestions * 2 + 2) / 3
    }

    fun isPassing(correctCount: Int, totalQuestions: Int): Boolean {
        return totalQuestions > 0 && correctCount >= requiredCorrectCount(totalQuestions)
    }

    fun build(
        targetNodeCode: String,
        module: MriModule,
        completedCodes: Set<String>,
        allQuizzes: List<QuizQuestion>,
        shuffleOptions: Boolean = true
    ): SkipTestPlan {
        val normalizedTarget = LegacyNodeCodes.normalize(targetNodeCode)
        val normalizedCompleted = LegacyNodeCodes.normalizeAll(completedCodes)
        val orderedNodes = PathStateResolver.orderNodesPublic(module.nodes)

        val targetIndex = orderedNodes.indexOfFirst {
            LegacyNodeCodes.normalize(it.code) == normalizedTarget
        }
        if (targetIndex <= 0) {
            return SkipTestPlan.empty(targetNodeCode)
        }

        val nodesToSkip = orderedNodes
            .subList(0, targetIndex)
            .filter { LegacyNodeCodes.normalize(it.code) !in normalizedCompleted }

        if (nodesToSkip.isEmpty()) {
            return SkipTestPlan.empty(targetNodeCode)
        }

        val skipNodeCodeOrder = nodesToSkip.map { LegacyNodeCodes.normalize(it.code) }
        val quizzesByNodeCode = allQuizzes
            .asSequence()
            .filter { q ->
                val qNorm = LegacyNodeCodes.normalize(q.nodeCode)
                qNorm in skipNodeCodeOrder
            }
            .groupBy { LegacyNodeCodes.normalize(it.nodeCode) }

        // Ambil round-robin per simpul yang akan dilewati (prioritaskan variasi antar simpul)
        val selected = mutableListOf<QuizQuestion>()
        var round = 0
        while (selected.size < SkipTestPlan.MAX_QUESTIONS) {
            var addedInRound = false
            for (code in skipNodeCodeOrder) {
                val bucket = quizzesByNodeCode[code].orEmpty()
                if (round < bucket.size) {
                    val q = bucket[round]
                    selected.add(if (shuffleOptions) QuizShuffle.shuffled(q) else q)
                    addedInRound = true
                    if (selected.size >= SkipTestPlan.MAX_QUESTIONS) break
                }
            }
            if (!addedInRound) break
            round++
        }

        return SkipTestPlan(
            targetNodeCode = targetNodeCode,
            nodesToSkip = nodesToSkip,
            questions = selected,
            requiredCorrect = requiredCorrectCount(selected.size)
        )
    }
}
