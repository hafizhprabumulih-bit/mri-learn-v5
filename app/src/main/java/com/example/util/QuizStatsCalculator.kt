package com.example.util

import com.example.data.MriQuizQuestion
import com.example.data.local.QuizAttemptEntity

data class CatalogQuizStats(
    val scopedAttempts: List<QuizAttemptEntity>,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val accuracyPct: Int,
    val stillWrongAttempts: List<QuizAttemptEntity>
)

object QuizStatsCalculator {
    /**
     * Menghitung statistik kuis katalog secara aman dengan memfilter hanya quizId yang ada
     * di katalog kurikulum (mengabaikan attempt `story:*` dari Mode Pelajaran Bertahap).
     */
    fun compute(
        allQuizzes: List<MriQuizQuestion>,
        attempts: List<QuizAttemptEntity>,
        selectedModuleFilter: String = "ALL"
    ): CatalogQuizStats {
        val catalogQuizIds = allQuizzes.mapTo(HashSet()) { it.id }
        val catalogAttempts = attempts.filter { it.quizId in catalogQuizIds }
        val scoped = if (selectedModuleFilter == "ALL" || selectedModuleFilter == "SEMUA") {
            catalogAttempts
        } else {
            catalogAttempts.filter { attempt ->
                attempt.moduleId == selectedModuleFilter ||
                    (selectedModuleFilter.length <= 3 && attempt.moduleId.startsWith(selectedModuleFilter)) ||
                    attempt.nodeCode.startsWith(selectedModuleFilter)
            }
        }
        val total = scoped.size
        val correct = scoped.count { it.isCorrect }
        val accuracy = if (total > 0) (correct * 100) / total else 0
        val stillWrong = scoped
            .groupBy { it.quizId }
            .values
            .mapNotNull { group -> group.maxByOrNull { it.answeredAt } }
            .filter { !it.isCorrect }

        return CatalogQuizStats(
            scopedAttempts = scoped,
            totalAttempts = total,
            correctAttempts = correct,
            accuracyPct = accuracy,
            stillWrongAttempts = stillWrong
        )
    }
}
