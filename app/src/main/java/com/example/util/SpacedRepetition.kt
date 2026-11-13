package com.example.util

import com.example.data.LegacyNodeCodes
import com.example.data.MriModule
import com.example.data.local.QuizAttemptEntity

data class ReviewCandidate(
    val nodeCode: String,
    val moduleId: String,
    val title: String,
    val accuracyPercent: Int,
    val attemptCount: Int,
    val daysSinceLastAttempt: Int,
    val reasonLabel: String,
    val priorityScore: Double
)

/**
 * Menghitung daftar simpul yang perlu diulang ("Ulasan Hari Ini") secara murni
 * berdasarkan riwayat percobaan kuis (`quiz_attempts`).
 */
object SpacedRepetition {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun selectDueReviews(
        attempts: List<QuizAttemptEntity>,
        modules: List<MriModule>,
        nowMillis: Long = System.currentTimeMillis(),
        maxItems: Int = 3
    ): List<ReviewCandidate> {
        if (attempts.isEmpty() || maxItems <= 0) return emptyList()

        val nodeLookup = buildMap {
            modules.forEach { module ->
                module.nodes.forEach { node ->
                    put(LegacyNodeCodes.normalize(node.code), module.id to node.title)
                }
            }
        }

        val candidates = attempts
            .asSequence()
            .filter { it.nodeCode.isNotBlank() }
            .groupBy { LegacyNodeCodes.normalize(it.nodeCode) }
            .mapNotNull { (normalizedCode, group) ->
                val attemptCount = group.size
                if (attemptCount == 0) return@mapNotNull null

                val correctCount = group.count { it.isCorrect }
                val accuracyPercent = (correctCount * 100) / attemptCount
                val lastAnsweredAt = group.maxOf { it.answeredAt }
                val daysSince = ((nowMillis - lastAnsweredAt).coerceAtLeast(0L) / MILLIS_PER_DAY).toInt()

                if (accuracyPercent >= 80 && daysSince < 3) {
                    return@mapNotNull null
                }

                val lookup = nodeLookup[normalizedCode]
                val moduleId = lookup?.first ?: group.first().moduleId.ifBlank {
                    normalizedCode.substringBefore(".", missingDelimiterValue = "")
                }
                val title = lookup?.second ?: normalizedCode
                val reasonLabel = when {
                    accuracyPercent < 60 -> "Akurasi $accuracyPercent%"
                    accuracyPercent < 80 -> "Perlu penguatan ($accuracyPercent%)"
                    else -> "Belum diulang $daysSince hari"
                }
                val priorityScore = (100 - accuracyPercent) * 2.0 +
                    daysSince * 5.0 +
                    if (accuracyPercent < 60) 30.0 else 0.0

                ReviewCandidate(
                    nodeCode = normalizedCode,
                    moduleId = moduleId,
                    title = title,
                    accuracyPercent = accuracyPercent,
                    attemptCount = attemptCount,
                    daysSinceLastAttempt = daysSince,
                    reasonLabel = reasonLabel,
                    priorityScore = priorityScore
                ) to lastAnsweredAt
            }
            .sortedWith(
                compareByDescending<Pair<ReviewCandidate, Long>> { it.first.priorityScore }
                    .thenBy { it.second }
            )
            .take(maxItems)
            .map { it.first }
            .toList()

        return candidates
    }
}
