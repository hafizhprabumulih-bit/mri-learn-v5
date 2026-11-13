package com.example.util

data class ShiftTask(
    val id: String,
    val title: String,
    val current: Int,
    val target: Int,
    val xpReward: Int
) {
    val isDone: Boolean get() = current >= target
}

data class DailyShiftSummary(
    val tasks: List<ShiftTask>,
    val completedCount: Int,
    val totalCount: Int,
    val bonusXpEarned: Int,
    val isAllComplete: Boolean
)

/**
 * Misi harian ringan "Shift Nano" yang memantau aktivitas harian
 * (simpul selesai, soal kuis dijawab, dan simulasi lab dicoba).
 */
object DailyShiftMission {
    fun evaluate(
        nodesToday: Int,
        quizzesToday: Int,
        labsToday: Int
    ): DailyShiftSummary {
        val safeNodes = nodesToday.coerceAtLeast(0)
        val safeQuizzes = quizzesToday.coerceAtLeast(0)
        val safeLabs = labsToday.coerceAtLeast(0)

        val tasks = listOf(
            ShiftTask(
                id = "node",
                title = "Selesaikan 1 simpul materi",
                current = safeNodes.coerceAtMost(1),
                target = 1,
                xpReward = 15
            ),
            ShiftTask(
                id = "quiz",
                title = "Kerjakan 5 soal kuis",
                current = safeQuizzes.coerceAtMost(5),
                target = 5,
                xpReward = 25
            ),
            ShiftTask(
                id = "lab",
                title = "Coba 1 simulasi lab",
                current = safeLabs.coerceAtMost(1),
                target = 1,
                xpReward = 20
            )
        )

        val completed = tasks.count { it.isDone }
        val allDone = completed == tasks.size
        val earnedXp = tasks.filter { it.isDone }.sumOf { it.xpReward } + if (allDone) 20 else 0

        return DailyShiftSummary(
            tasks = tasks,
            completedCount = completed,
            totalCount = tasks.size,
            bonusXpEarned = earnedXp,
            isAllComplete = allDone
        )
    }
}
