package com.example.util.lesson

import com.example.data.LegacyNodeCodes
import com.example.data.MriNode
import com.example.data.MriQuizQuestion
import com.example.data.lesson.LessonExtras
import com.example.util.NodeDisplay

const val LESSON_FLOW_ENABLED: Boolean = true

data class LessonPlan(
    val nodeCode: String,
    val steps: List<LessonStep>
) {
    val segmentCount: Int
        get() = steps.count { it !is LessonStep.Complete }
}

object LessonPlanBuilder {

    fun build(
        node: MriNode,
        quizzes: List<MriQuizQuestion>,
        extras: LessonExtras? = null,
        allowDrafts: Boolean = false
    ): LessonPlan {
        val normCode = LegacyNodeCodes.normalize(node.code)
        val nodeExtras = extras?.forNode(normCode)

        val openingTitle = NodeDisplay.displayTitle(node)
        val openingGoal = node.learningGoal
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: node.summaryShort.trim().ifEmpty { openingTitle }

        var chunks = LessonChunker.chunk(
            detailFull = node.detailFull,
            summaryShort = node.summaryShort
        )
        val nanoHints = nodeExtras?.nanoHints.orEmpty()

        val hasAnalogy = node.analogi.trim().isNotEmpty()
        val interactiveContent = nodeExtras?.interactive?.takeIf { it.reviewed || allowDrafts }
        val activeStories = nodeExtras?.stories
            .orEmpty()
            .filter { it.reviewed || allowDrafts }
            .take(2)
        val storyIds = activeStories.mapTo(HashSet()) { it.id }

        val checkQuiz = quizzes.firstOrNull { q ->
            LegacyNodeCodes.normalize(q.nodeCode) == normCode && q.id !in storyIds
        }

        // Hitung jumlah langkah non-Explain dan non-Complete:
        // Opening (1) + Analogy (0..1) + Interactive (0..1) + Stories (0..2) + Check (0..1)
        val fixedNonExplainCount = 1 +
            (if (hasAnalogy) 1 else 0) +
            (if (interactiveContent != null) 1 else 0) +
            activeStories.size +
            (if (checkQuiz != null) 1 else 0)

        val maxExplainAllowed = (10 - fixedNonExplainCount).coerceAtLeast(1)
        chunks = mergeChunksToLimit(chunks, maxExplainAllowed)

        val steps = mutableListOf<LessonStep>()
        steps.add(LessonStep.Opening(title = openingTitle, goal = openingGoal))

        val totalExplain = chunks.size
        chunks.forEachIndexed { idx, chunk ->
            val hint = nanoHints.getOrNull(idx)?.takeIf { it.isNotBlank() }
            steps.add(
                LessonStep.Explain(
                    chunk = chunk,
                    explainIndex = idx + 1,
                    explainTotal = totalExplain,
                    nanoHint = hint
                )
            )
        }

        if (hasAnalogy) {
            steps.add(LessonStep.Analogy(text = node.analogi.trim()))
        }

        if (interactiveContent != null) {
            steps.add(LessonStep.Interactive(contentId = "$normCode:interactive"))
        }

        activeStories.forEach { story ->
            steps.add(
                LessonStep.Story(
                    storyId = story.id,
                    isDraft = !story.reviewed
                )
            )
        }

        if (checkQuiz != null) {
            steps.add(LessonStep.Check(quizId = checkQuiz.id))
        }

        steps.add(LessonStep.Complete)

        return LessonPlan(
            nodeCode = normCode,
            steps = steps
        )
    }

    private fun mergeChunksToLimit(chunks: List<LessonChunk>, maxCount: Int): List<LessonChunk> {
        val list = chunks.toMutableList()
        while (list.size > maxCount && list.size > 1) {
            var bestIdx = 0
            var bestLen = Int.MAX_VALUE
            for (i in 0 until list.size - 1) {
                val len = (list[i].leadIn?.length ?: 0) + list[i].text.length +
                    (list[i + 1].leadIn?.length ?: 0) + list[i + 1].text.length
                if (len < bestLen) {
                    bestLen = len
                    bestIdx = i
                }
            }
            val a = list[bestIdx]
            val b = list[bestIdx + 1]
            val bCombined = if (!b.leadIn.isNullOrBlank()) "${b.leadIn} ${b.text}".trim() else b.text
            list[bestIdx] = LessonChunk(
                leadIn = a.leadIn,
                text = "${a.text} $bCombined".trim()
            )
            list.removeAt(bestIdx + 1)
        }
        return list
    }
}
