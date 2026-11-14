package com.example.util.lesson

data class AttemptRecord(
    val quizId: String,
    val isCorrect: Boolean,
    val answeredAt: Long
)

object MasteryStars {

    /**
     * Menghitung bintang penguasaan simpul (1..3):
     * - Minimal 1 bintang ketika pelajaran selesai.
     * - +1 untuk setiap item penilaian (Story / Check) yang dijawab benar pada percobaan pertama
     *   (diurutkan berdasarkan answeredAt paling awal).
     * - Jika hanya ada 1 item penilaian, maksimum adalah 2 bintang.
     * - Jika tidak ada item penilaian, hasilnya 1 bintang.
     */
    fun stars(attempts: List<AttemptRecord>, assessmentIds: List<String>): Int {
        val uniqueIds = assessmentIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (uniqueIds.isEmpty()) return 1

        var firstTryCorrectCount = 0
        for (itemId in uniqueIds) {
            val itemAttempts = attempts.filter { it.quizId == itemId }
            val firstAttempt = itemAttempts.minByOrNull { it.answeredAt }
            if (firstAttempt != null && firstAttempt.isCorrect) {
                firstTryCorrectCount++
            }
        }

        val raw = 1 + firstTryCorrectCount
        val cap = if (uniqueIds.size == 1) 2 else 3
        return raw.coerceIn(1, cap)
    }

    fun starCaption(stars: Int, hasStory: Boolean, hasCheck: Boolean): String {
        return when {
            stars >= 3 && hasStory && !hasCheck -> "3 bintang: kedua soal cerita benar di percobaan pertama"
            stars >= 3 -> "3 bintang: soal cerita & cek ringkasan benar di percobaan pertama"
            stars == 2 && hasStory && !hasCheck -> "2 bintang: soal cerita benar di percobaan pertama"
            stars == 2 && !hasStory && hasCheck -> "2 bintang: cek ringkasan benar di percobaan pertama"
            stars == 2 && hasStory && hasCheck -> "2 bintang: salah satu soal evaluasi benar di percobaan pertama"
            stars == 2 -> "2 bintang: evaluasi benar di percobaan pertama"
            else -> "1 bintang: pelajaran selesai"
        }
    }

    /**
     * Mengembalikan true bila ada item penilaian yang pada percobaan TERAKHIRNYA masih salah
     * (misalnya salah 2 kali berturut-turut), sehingga otomatis disarankan masuk Ulas Hari Ini.
     */
    fun needsReview(attempts: List<AttemptRecord>, assessmentIds: List<String>): Boolean {
        val uniqueIds = assessmentIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (uniqueIds.isEmpty()) return false

        for (itemId in uniqueIds) {
            val itemAttempts = attempts.filter { it.quizId == itemId }
            val latestAttempt = itemAttempts.maxByOrNull { it.answeredAt }
            if (latestAttempt != null && !latestAttempt.isCorrect) {
                return true
            }
        }
        return false
    }
}
