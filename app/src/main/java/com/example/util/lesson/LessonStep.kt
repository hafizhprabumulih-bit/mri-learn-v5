package com.example.util.lesson

sealed interface LessonStep {
    data class Opening(
        val title: String,
        val goal: String
    ) : LessonStep

    data class Explain(
        val chunk: LessonChunk,
        val explainIndex: Int,
        val explainTotal: Int,
        val nanoHint: String? = null
    ) : LessonStep

    data class Analogy(
        val text: String
    ) : LessonStep

    data class Interactive(
        val contentId: String,
        val isDraft: Boolean = false
    ) : LessonStep

    data class Story(
        val storyId: String,
        val isDraft: Boolean = false
    ) : LessonStep

    data class Check(
        val quizId: String
    ) : LessonStep

    data object Complete : LessonStep
}
