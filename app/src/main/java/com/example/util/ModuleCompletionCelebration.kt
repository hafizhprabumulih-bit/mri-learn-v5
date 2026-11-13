package com.example.util

import com.example.data.CharacterCatalog
import com.example.data.MriModule
import com.example.data.MriQuizQuestion
import com.example.data.UnlockRequirement
import com.example.data.local.QuizAttemptEntity

/**
 * Helper murni untuk mendeteksi modul baru tuntas dan menyusun data layar Modul Tuntas.
 */
object ModuleCompletionCelebration {

    fun moduleJustCompleted(
        before: ModuleProgress,
        after: ModuleProgress
    ): Boolean = after.total > 0 && before.done < before.total && after.done >= after.total

    fun shouldShow(
        moduleId: String,
        before: ModuleProgress,
        after: ModuleProgress,
        alreadyCelebratedModuleIds: Set<String>
    ): Boolean = moduleJustCompleted(before, after) && moduleId !in alreadyCelebratedModuleIds

    fun findNextRecommendedModule(
        currentModuleId: String,
        allModules: List<MriModule>,
        completedCodes: Set<String>
    ): MriModule? {
        if (allModules.isEmpty()) return null
        val pathStates = PathStateResolver.resolveAll(allModules, completedCodes)
        val currentIdx = pathStates.indexOfFirst { it.module.id == currentModuleId }
        val orderedCandidates = if (currentIdx >= 0) {
            pathStates.drop(currentIdx + 1) + pathStates.take(currentIdx)
        } else {
            pathStates
        }
        return orderedCandidates.firstOrNull { candidate ->
            candidate.progress.total > 0 &&
                candidate.progress.done < candidate.progress.total &&
                PathStateResolver.isModuleRecommended(
                    modules = allModules,
                    completedCodes = completedCodes,
                    moduleId = candidate.module.id
                )
        }?.module
    }

    fun moduleQuizMasteryPercent(
        moduleId: String,
        quizzes: List<MriQuizQuestion>,
        attempts: List<QuizAttemptEntity>
    ): Int {
        val moduleQuizzes = quizzes.filter { it.moduleId == moduleId }
        if (moduleQuizzes.isEmpty()) return 0
        val quizIds = moduleQuizzes.map { it.id }.toSet()
        val latestByQuiz = LinkedHashMap<String, QuizAttemptEntity>()
        attempts.forEach { attempt ->
            if (attempt.quizId in quizIds) {
                val prev = latestByQuiz[attempt.quizId]
                if (prev == null || attempt.answeredAt >= prev.answeredAt) {
                    latestByQuiz[attempt.quizId] = attempt
                }
            }
        }
        val correct = latestByQuiz.values.count { it.isCorrect }
        return (correct * 100) / moduleQuizzes.size
    }

    fun unlockedCharacterItemName(
        moduleId: String,
        enforceCurriculumLocks: Boolean = CharacterCatalog.ENFORCE_CURRICULUM_LOCKS
    ): String? {
        if (!enforceCurriculumLocks) return null
        return CharacterCatalog.allItems.firstOrNull { item ->
            item.unlockRequirement.requiredModuleId?.equals(moduleId, ignoreCase = true) == true
        }?.name
    }
}
