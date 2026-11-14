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

        val initialChunks = LessonChunker.chunk(
            detailFull = node.detailFull,
            summaryShort = node.summaryShort
        )
        val rawHints = if (nodeExtras?.nanoHintsReviewed == true || allowDrafts) {
            nodeExtras?.nanoHints.orEmpty()
        } else {
            emptyList()
        }

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
        val chunkHintPairs = mergeChunksToLimit(
            chunks = initialChunks,
            hints = rawHints,
            maxCount = maxExplainAllowed
        )

        val steps = mutableListOf<LessonStep>()
        steps.add(LessonStep.Opening(title = openingTitle, goal = openingGoal))

        val totalExplain = chunkHintPairs.size
        chunkHintPairs.forEachIndexed { idx, (chunk, hint) ->
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
            steps.add(
                LessonStep.Interactive(
                    contentId = "$normCode:interactive",
                    isDraft = !interactiveContent.reviewed
                )
            )
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

    private fun mergeChunksToLimit(
        chunks: List<LessonChunk>,
        hints: List<String>,
        maxCount: Int
    ): List<Pair<LessonChunk, String?>> {
        val list = chunks.mapIndexed { idx, chunk ->
            chunk to hints.getOrNull(idx)?.trim()?.takeIf { it.isNotEmpty() }
        }.toMutableList()

        // Jika masih ada nanoHints di luar panjang awal chunks, tempelkan ke segmen terakhir agar tidak terbuang
        if (hints.size > list.size && list.isNotEmpty()) {
            val extraHint = hints.drop(list.size).firstOrNull { it.isNotBlank() }?.trim()
            val lastIdx = list.lastIndex
            if (extraHint != null && list[lastIdx].second == null) {
                list[lastIdx] = list[lastIdx].first to extraHint
            }
        }

        while (list.size > maxCount && list.size > 1) {
            var bestIdx = 0
            var bestLen = Int.MAX_VALUE
            for (i in 0 until list.size - 1) {
                val ca = list[i].first
                val cb = list[i + 1].first
                val len = (ca.leadIn?.length ?: 0) + ca.text.length +
                    (cb.leadIn?.length ?: 0) + cb.text.length
                if (len < bestLen) {
                    bestLen = len
                    bestIdx = i
                }
            }
            val (a, hintA) = list[bestIdx]
            val (b, hintB) = list[bestIdx + 1]
            val bCombined = if (!b.leadIn.isNullOrBlank()) "${b.leadIn} ${b.text}".trim() else b.text
            val mergedChunk = LessonChunk(
                leadIn = a.leadIn,
                text = "${a.text} $bCombined".trim()
            )
            list[bestIdx] = mergedChunk to (hintA ?: hintB)
            list.removeAt(bestIdx + 1)
        }
        return list
    }
}
