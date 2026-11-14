package com.example.data.local

import android.util.Log
import androidx.room.withTransaction
import com.example.data.LegacyNodeCodes

/** Idempotent: aman dijalankan berulang. Semua perubahan dalam satu transaksi. */
suspend fun MriDatabase.normalizeLegacyNodeCodes() {
    withTransaction {
        val progressDao = nodeProgressDao()
        val attemptDao = quizAttemptDao()
        val noteDao = studyNoteDao()
        val dayDao = studyDayDao()
        for ((oldCode, newCode) in LegacyNodeCodes.MAP) {
            val newModule = LegacyNodeCodes.moduleIdOf(newCode)
            val old = progressDao.getByNodeCode(oldCode)
            if (old != null) {
                val existing = progressDao.getByNodeCode(newCode)
                if (existing == null) {
                    progressDao.insert(
                        NodeProgressEntity(
                            nodeCode = newCode,
                            completedAt = old.completedAt,
                            moduleId = newModule,
                            source = old.source
                        )
                    )
                } else if (existing.source != "READ" && old.source == "READ") {
                    progressDao.insert(
                        existing.copy(
                            completedAt = old.completedAt,
                            moduleId = newModule,
                            source = old.source
                        )
                    )
                }
                progressDao.delete(oldCode)
            }
            attemptDao.renameNode(oldCode, newCode, newModule)
            noteDao.renameNode(oldCode, newCode, newModule)
            dayDao.renameLastNode(oldCode, newCode, newModule)
        }
    }
    val leftover = nodeProgressDao().getAll().filter { it.nodeCode.startsWith("M03.") }
    if (leftover.isNotEmpty()) {
        Log.w("LegacyCodes", "Kode M03.* tanpa peta: ${leftover.map { it.nodeCode }}")
    }
}
