package com.example

import com.example.data.MriQuizQuestion
import com.example.data.MriRepository
import com.example.data.local.QuizAttemptEntity
import com.example.util.LevelQuizCompletionState
import com.example.util.LevelQuizStatus
import com.example.util.NodeStatus
import com.example.util.PathNodeState
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelQuizStatusTest {

    private fun sampleQuestions(count: Int): List<MriQuizQuestion> =
        (1..count).map { idx ->
            MriQuizQuestion(
                id = "Q_T1_$idx",
                moduleId = "M01",
                nodeCode = "M01.T1.$idx",
                level = 1,
                question = "Pertanyaan $idx",
                options = listOf("A", "B", "C", "D"),
                correctIndex = 0,
                explanation = "Penjelasan $idx"
            )
        }

    private fun sampleNodeStates(done: Boolean): List<PathNodeState> {
        val node = MriRepository.modules.first().nodes.first()
        return listOf(
            PathNodeState(
                node = node,
                status = if (done) NodeStatus.DONE else NodeStatus.OPEN,
                softPrerequisiteHint = null
            )
        )
    }

    @Test
    fun softLocksLeaveUnfinishedLevelQuizOpen() {
        val summary = LevelQuizStatus.compute(
            level = 1,
            levelQuizzes = sampleQuestions(10),
            levelNodeStates = sampleNodeStates(done = false),
            attempts = emptyList(),
            strictLocks = false
        )
        assertEquals(LevelQuizCompletionState.TERBUKA, summary.status)
        assertEquals(NodeStatus.OPEN, summary.nodeStatus)
    }

    @Test
    fun strictLocksKeepUnfinishedLevelQuizLocked() {
        val summary = LevelQuizStatus.compute(
            level = 1,
            levelQuizzes = sampleQuestions(10),
            levelNodeStates = sampleNodeStates(done = false),
            attempts = emptyList(),
            strictLocks = true
        )
        assertEquals(LevelQuizCompletionState.TERKUNCI, summary.status)
        assertEquals(NodeStatus.LOCKED, summary.nodeStatus)
    }

    @Test
    fun passingSeventyPercentMarksLevelQuizDone() {
        val questions = sampleQuestions(10)
        val attempts = questions.mapIndexed { index, q ->
            QuizAttemptEntity(
                quizId = q.id,
                nodeCode = q.nodeCode,
                selectedIndex = if (index < 7) 0 else 1,
                isCorrect = index < 7,
                answeredAt = 1000L + index
            )
        }
        val summary = LevelQuizStatus.compute(
            level = 1,
            levelQuizzes = questions,
            levelNodeStates = sampleNodeStates(done = true),
            attempts = attempts,
            strictLocks = false
        )
        assertEquals(7, summary.latestCorrectCount)
        assertEquals(LevelQuizCompletionState.SELESAI, summary.status)
        assertEquals(NodeStatus.DONE, summary.nodeStatus)
    }

    @Test
    fun latestAttemptOverridesEarlierCorrectAnswer() {
        val questions = sampleQuestions(4)
        val attempts = listOf(
            QuizAttemptEntity(quizId = "Q_T1_1", nodeCode = "M01.T1.1", selectedIndex = 0, isCorrect = true, answeredAt = 10L),
            QuizAttemptEntity(quizId = "Q_T1_2", nodeCode = "M01.T1.2", selectedIndex = 0, isCorrect = true, answeredAt = 10L),
            QuizAttemptEntity(quizId = "Q_T1_3", nodeCode = "M01.T1.3", selectedIndex = 0, isCorrect = true, answeredAt = 10L),
            // Jawaban terbaru untuk Q_T1_1 salah -> skor menjadi 2/4 (50% < 70%)
            QuizAttemptEntity(quizId = "Q_T1_1", nodeCode = "M01.T1.1", selectedIndex = 1, isCorrect = false, answeredAt = 20L)
        )
        val summary = LevelQuizStatus.compute(
            level = 1,
            levelQuizzes = questions,
            levelNodeStates = sampleNodeStates(done = true),
            attempts = attempts,
            strictLocks = false
        )
        assertEquals(2, summary.latestCorrectCount)
        assertEquals(LevelQuizCompletionState.TERBUKA, summary.status)
    }
}
