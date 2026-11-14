package com.example.util.lesson

enum class QuestionPhase {
    UNANSWERED,
    RETRY_ALLOWED,
    RESOLVED_CORRECT,
    RESOLVED_WRONG_TWICE
}

data class LessonState(
    val plan: LessonPlan,
    val index: Int = 0,
    val attemptsByItem: Map<String, List<Int>> = emptyMap(),
    val questionPhaseByItem: Map<String, QuestionPhase> = emptyMap(),
    val awaitingSecondSelection: Set<String> = emptySet(),
    val maxVisitedIndex: Int = 0,
    val finished: Boolean = false
) {
    val currentStep: LessonStep
        get() = plan.steps.getOrElse(index) { LessonStep.Complete }

    fun phaseFor(itemId: String): QuestionPhase =
        questionPhaseByItem[itemId] ?: QuestionPhase.UNANSWERED

    fun isQuestionResolved(itemId: String): Boolean {
        val phase = phaseFor(itemId)
        return phase == QuestionPhase.RESOLVED_CORRECT || phase == QuestionPhase.RESOLVED_WRONG_TWICE
    }
}

sealed interface LessonEvent {
    data object Next : LessonEvent
    data object Back : LessonEvent
    data class Answer(
        val itemId: String,
        val optionIndex: Int,
        val correctIndex: Int
    ) : LessonEvent
    data class Retry(val itemId: String) : LessonEvent
    data class RestoreIndex(val savedIndex: Int) : LessonEvent
}

object LessonReducer {

    fun reduce(state: LessonState, event: LessonEvent): LessonState {
        return when (event) {
            is LessonEvent.Answer -> handleAnswer(state, event)
            is LessonEvent.Retry -> handleRetry(state, event.itemId)
            is LessonEvent.Next -> handleNext(state)
            is LessonEvent.Back -> handleBack(state)
            is LessonEvent.RestoreIndex -> handleRestoreIndex(state, event.savedIndex)
        }
    }

    private fun handleAnswer(state: LessonState, event: LessonEvent.Answer): LessonState {
        val currentPhase = state.phaseFor(event.itemId)
        if (currentPhase == QuestionPhase.RESOLVED_CORRECT ||
            currentPhase == QuestionPhase.RESOLVED_WRONG_TWICE
        ) {
            return state
        }
        // Jika baru salah sekali dan belum menekan "Coba lagi", abaikan ketukan opsi baru
        if (currentPhase == QuestionPhase.RETRY_ALLOWED &&
            event.itemId !in state.awaitingSecondSelection
        ) {
            return state
        }

        val prevAttempts = state.attemptsByItem[event.itemId].orEmpty()
        if (prevAttempts.size >= 2) return state

        val updatedAttempts = prevAttempts + event.optionIndex
        val isCorrect = event.optionIndex == event.correctIndex
        val nextPhase = when {
            isCorrect -> QuestionPhase.RESOLVED_CORRECT
            updatedAttempts.size == 1 -> QuestionPhase.RETRY_ALLOWED
            else -> QuestionPhase.RESOLVED_WRONG_TWICE
        }

        return state.copy(
            attemptsByItem = state.attemptsByItem + (event.itemId to updatedAttempts),
            questionPhaseByItem = state.questionPhaseByItem + (event.itemId to nextPhase),
            awaitingSecondSelection = state.awaitingSecondSelection - event.itemId
        )
    }

    private fun handleRetry(state: LessonState, itemId: String): LessonState {
        if (state.phaseFor(itemId) != QuestionPhase.RETRY_ALLOWED) return state
        return state.copy(
            awaitingSecondSelection = state.awaitingSecondSelection + itemId
        )
    }

    private fun handleNext(state: LessonState): LessonState {
        if (state.finished) return state
        val step = state.currentStep
        when (step) {
            is LessonStep.Story -> {
                if (!state.isQuestionResolved(step.storyId)) return state
            }
            is LessonStep.Check -> {
                if (!state.isQuestionResolved(step.quizId)) return state
            }
            is LessonStep.Complete -> {
                return state.copy(finished = true)
            }
            else -> Unit
        }

        val nextIndex = (state.index + 1).coerceAtMost(state.plan.steps.lastIndex)
        val nextStep = state.plan.steps[nextIndex]
        val isComplete = nextStep is LessonStep.Complete
        return state.copy(
            index = nextIndex,
            maxVisitedIndex = maxOf(state.maxVisitedIndex, nextIndex),
            finished = isComplete
        )
    }

    private fun handleBack(state: LessonState): LessonState {
        if (state.index <= 0) return state
        var candidate = state.index - 1
        while (candidate >= 0) {
            val step = state.plan.steps[candidate]
            val isAnsweredQuestion = when (step) {
                is LessonStep.Story -> state.attemptsByItem[step.storyId].orEmpty().isNotEmpty()
                is LessonStep.Check -> state.attemptsByItem[step.quizId].orEmpty().isNotEmpty()
                else -> false
            }
            if (!isAnsweredQuestion) {
                return state.copy(
                    index = candidate,
                    finished = false
                )
            }
            candidate--
        }
        return state
    }

    private fun handleRestoreIndex(state: LessonState, savedIndex: Int): LessonState {
        val maxNonComplete = (state.plan.steps.lastIndex - 1).coerceAtLeast(0)
        val clamped = savedIndex.coerceIn(0, maxNonComplete)
        return state.copy(
            index = clamped,
            maxVisitedIndex = maxOf(state.maxVisitedIndex, clamped),
            finished = false
        )
    }
}
