package com.example.util

import com.example.data.MriQuizQuestion
import com.example.data.local.QuizAttemptEntity

enum class LevelQuizCompletionState {
    TERKUNCI,
    TERBUKA,
    SELESAI
}

data class LevelQuizSummary(
    val level: Int,
    val questionCount: Int,
    val latestCorrectCount: Int,
    val status: LevelQuizCompletionState
) {
    val nodeStatus: NodeStatus
        get() = when (status) {
            LevelQuizCompletionState.SELESAI -> NodeStatus.DONE
            LevelQuizCompletionState.TERBUKA -> NodeStatus.OPEN
            LevelQuizCompletionState.TERKUNCI -> NodeStatus.LOCKED
        }
}

/**
 * Menghitung status Kuis Tingkat (1..3) secara murni dari riwayat percobaan kuis
 * tanpa mengubah skema Room.
 */
object LevelQuizStatus {
    const val PASSING_PERCENT = 70

    fun compute(
        level: Int,
        levelQuizzes: List<MriQuizQuestion>,
        levelNodeStates: List<PathNodeState>,
        attempts: List<QuizAttemptEntity>,
        strictLocks: Boolean = PATH_STRICT_LOCKS
    ): LevelQuizSummary {
        val questionIds = levelQuizzes.map { it.id }.toSet()
        if (questionIds.isEmpty()) {
            return LevelQuizSummary(
                level = level,
                questionCount = 0,
                latestCorrectCount = 0,
                status = LevelQuizCompletionState.TERKUNCI
            )
        }

        val latestByQuiz = LinkedHashMap<String, QuizAttemptEntity>()
        attempts.forEach { attempt ->
            if (attempt.quizId in questionIds) {
                val existing = latestByQuiz[attempt.quizId]
                if (existing == null || attempt.answeredAt >= existing.answeredAt) {
                    latestByQuiz[attempt.quizId] = attempt
                }
            }
        }

        val latestCorrect = latestByQuiz.values.count { it.isCorrect }
        val passed = latestCorrect * 100 >= questionIds.size * PASSING_PERCENT
        val allNodesFinished = levelNodeStates.isNotEmpty() && levelNodeStates.all {
            it.status == NodeStatus.DONE || it.status == NodeStatus.SKIPPED
        }

        val status = when {
            passed -> LevelQuizCompletionState.SELESAI
            !strictLocks || allNodesFinished -> LevelQuizCompletionState.TERBUKA
            else -> LevelQuizCompletionState.TERKUNCI
        }

        return LevelQuizSummary(
            level = level,
            questionCount = questionIds.size,
            latestCorrectCount = latestCorrect,
            status = status
        )
    }
}
