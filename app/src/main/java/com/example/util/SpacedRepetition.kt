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
 * berdasarkan riwayat percobaan kuis (`quiz_attempts`, menggunakan status terbaru per `quizId`
 * agar percobaan ke-2 yang berhasil memperbaiki kesalahan awal tidak menjatuhkan akurasi),
 * ditambah simpul yang ditandai manual oleh pengguna (`manualReviewNodes`) dan mengecualikan
 * simpul yang dimatikan manual (`excludedReviewNodes`).
 */
object SpacedRepetition {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun isNodeQueuedForReview(
        nodeCode: String,
        manualReviewNodes: Set<String>,
        excludedReviewNodes: Set<String>,
        autoNeedsReview: Boolean
    ): Boolean {
        val norm = LegacyNodeCodes.normalize(nodeCode)
        val normManual = LegacyNodeCodes.normalizeAll(manualReviewNodes)
        val normExcluded = LegacyNodeCodes.normalizeAll(excludedReviewNodes)
        return when {
            norm in normExcluded -> false
            norm in normManual -> true
            else -> autoNeedsReview
        }
    }

    fun selectDueReviews(
        attempts: List<QuizAttemptEntity>,
        modules: List<MriModule>,
        nowMillis: Long = System.currentTimeMillis(),
        maxItems: Int = 3,
        manualReviewNodes: Set<String> = emptySet(),
        excludedReviewNodes: Set<String> = emptySet()
    ): List<ReviewCandidate> {
        if (maxItems <= 0) return emptyList()

        val normManual = LegacyNodeCodes.normalizeAll(manualReviewNodes)
        val normExcluded = LegacyNodeCodes.normalizeAll(excludedReviewNodes)
        if (attempts.isEmpty() && (normManual - normExcluded).isEmpty()) return emptyList()

        val nodeLookup = buildMap {
            modules.forEach { module ->
                module.nodes.forEach { node ->
                    put(LegacyNodeCodes.normalize(node.code), module.id to node.title)
                }
            }
        }

        val groupedAttempts = attempts
            .asSequence()
            .filter { it.nodeCode.isNotBlank() }
            .groupBy { LegacyNodeCodes.normalize(it.nodeCode) }

        val candidateCodes = (groupedAttempts.keys + normManual) - normExcluded

        val candidates = candidateCodes
            .asSequence()
            .mapNotNull { normalizedCode ->
                val group = groupedAttempts[normalizedCode].orEmpty()
                val isManuallyAdded = normalizedCode in normManual

                // Ambil status percobaan TERBARU per quizId sehingga kesalahan ke-1 yang diperbaiki
                // di percobaan retry ke-2 tidak terus memicu Ulas Hari Ini secara salah.
                val latestByQuiz = group
                    .groupBy { it.quizId.ifBlank { "_id_${it.id}" } }
                    .values
                    .mapNotNull { quizGroup -> quizGroup.maxByOrNull { it.answeredAt } }

                val lookup = nodeLookup[normalizedCode]
                if (lookup == null && group.isEmpty()) return@mapNotNull null

                val moduleId = lookup?.first
                    ?: group.firstOrNull()?.moduleId?.ifBlank { null }
                    ?: normalizedCode.substringBefore(".", missingDelimiterValue = "")
                val title = lookup?.second ?: normalizedCode

                if (latestByQuiz.isEmpty()) {
                    if (!isManuallyAdded) return@mapNotNull null
                    return@mapNotNull ReviewCandidate(
                        nodeCode = normalizedCode,
                        moduleId = moduleId,
                        title = title,
                        accuracyPercent = 100,
                        attemptCount = 0,
                        daysSinceLastAttempt = 0,
                        reasonLabel = "Ditandai untuk diulang",
                        priorityScore = 65.0
                    ) to 0L
                }

                val correctCount = latestByQuiz.count { it.isCorrect }
                val accuracyPercent = (correctCount * 100) / latestByQuiz.size
                val lastAnsweredAt = group.maxOf { it.answeredAt }
                val daysSince = ((nowMillis - lastAnsweredAt).coerceAtLeast(0L) / MILLIS_PER_DAY).toInt()
                val isAutoDue = accuracyPercent < 80 || daysSince >= 3

                if (!isAutoDue && !isManuallyAdded) {
                    return@mapNotNull null
                }

                val reasonLabel = when {
                    accuracyPercent < 60 -> "Akurasi $accuracyPercent%"
                    accuracyPercent < 80 -> "Perlu penguatan ($accuracyPercent%)"
                    daysSince >= 3 -> "Belum diulang $daysSince hari"
                    else -> "Ditandai untuk diulang"
                }
                val manualBoost = if (isManuallyAdded && !isAutoDue) 65.0 else 0.0
                val priorityScore = (100 - accuracyPercent) * 2.0 +
                    daysSince * 5.0 +
                    (if (accuracyPercent < 60) 30.0 else 0.0) +
                    manualBoost

                ReviewCandidate(
                    nodeCode = normalizedCode,
                    moduleId = moduleId,
                    title = title,
                    accuracyPercent = accuracyPercent,
                    attemptCount = latestByQuiz.size,
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
