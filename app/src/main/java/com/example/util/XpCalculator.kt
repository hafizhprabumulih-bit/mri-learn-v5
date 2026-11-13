package com.example.util

import com.example.data.CharacterCatalog
import com.example.data.CharacterItem
import com.example.data.LegacyNodeCodes
import com.example.data.MriModule
import com.example.data.MriNode
import com.example.data.local.QuizAttemptEntity

object XpRules {
    const val XP_PER_READ_NODE = 10
    const val XP_PER_FIRST_CORRECT_QUIZ = 5
    const val XP_PER_COMPLETE_MODULE_BONUS = 50
    const val XP_PER_SKIPPED_NODE = 0
}

data class XpSummary(
    val totalXp: Int,
    val readNodesCount: Int,
    val readNodesXp: Int,
    val firstCorrectQuizCount: Int,
    val quizXp: Int,
    val completedModulesCount: Int,
    val moduleBonusXp: Int
)

data class CharacterUnlockProgress(
    val item: CharacterItem,
    val moduleCode: String,
    val moduleTitle: String,
    val completedInModule: Int,
    val requiredCount: Int
) {
    val fraction: Float
        get() = if (requiredCount <= 0) 1f else (completedInModule.toFloat() / requiredCount.toFloat()).coerceIn(0f, 1f)

    val isReady: Boolean
        get() = completedInModule >= requiredCount
}

/**
 * Kalkulator XP murni (tanpa kolom database baru). Seluruh XP diturunkan langsung
 * dari progres simpul yang benar-benar dibaca, kuis benar unik pertama, dan modul tuntas 100%.
 */
object XpCalculator {
    const val XP_MODULE_BONUS = XpRules.XP_PER_COMPLETE_MODULE_BONUS
    const val XP_PER_CORRECT_QUIZ = XpRules.XP_PER_FIRST_CORRECT_QUIZ

    fun calculate(
        completedCodes: Set<String>,
        skippedCodes: Set<String>,
        quizAttempts: List<QuizAttemptEntity>,
        modules: List<MriModule>
    ): XpSummary {
        val validNodeCodes = modules.flatMapTo(HashSet()) { module ->
            module.nodes.map { LegacyNodeCodes.normalize(it.code) }
        }
        val normalizedCompleted = LegacyNodeCodes.normalizeAll(completedCodes)
        val normalizedSkipped = LegacyNodeCodes.normalizeAll(skippedCodes)

        val readValidCodes = (normalizedCompleted - normalizedSkipped).filterTo(HashSet()) {
            it in validNodeCodes
        }

        val readNodesCount = readValidCodes.size
        val readNodesXp = readNodesCount * XpRules.XP_PER_READ_NODE

        val uniqueCorrectQuizIds = quizAttempts
            .asSequence()
            .filter { it.isCorrect && it.quizId.isNotBlank() }
            .mapTo(HashSet()) { it.quizId }
        val firstCorrectQuizCount = uniqueCorrectQuizIds.size
        val quizXp = firstCorrectQuizCount * XpRules.XP_PER_FIRST_CORRECT_QUIZ

        val completedModulesCount = modules.count { module ->
            module.nodes.isNotEmpty() && module.nodes.all { node ->
                LegacyNodeCodes.normalize(node.code) in readValidCodes
            }
        }
        val moduleBonusXp = completedModulesCount * XpRules.XP_PER_COMPLETE_MODULE_BONUS

        return XpSummary(
            totalXp = readNodesXp + quizXp + moduleBonusXp,
            readNodesCount = readNodesCount,
            readNodesXp = readNodesXp,
            firstCorrectQuizCount = firstCorrectQuizCount,
            quizXp = quizXp,
            completedModulesCount = completedModulesCount,
            moduleBonusXp = moduleBonusXp
        )
    }

    /**
     * Menghitung tambahan XP ketika `node` baru saja diselesaikan lewat membaca (`READ`)
     * dengan asumsi sebelumnya belum pernah selesai sebagai `READ`.
     */
    fun earnedForNodeCompletion(
        node: MriNode,
        module: MriModule,
        completedCodesBefore: Set<String>,
        skippedCodesBefore: Set<String>
    ): Int {
        val normalizedTarget = LegacyNodeCodes.normalize(node.code)
        val normalizedCompletedBefore = LegacyNodeCodes.normalizeAll(completedCodesBefore)
        val normalizedSkippedBefore = LegacyNodeCodes.normalizeAll(skippedCodesBefore)

        val wasAlreadyRead = normalizedTarget in (normalizedCompletedBefore - normalizedSkippedBefore)
        if (wasAlreadyRead) return 0

        val readCodesBefore = normalizedCompletedBefore - normalizedSkippedBefore
        val readCodesAfter = readCodesBefore + normalizedTarget

        val moduleWasComplete = module.nodes.isNotEmpty() && module.nodes.all {
            LegacyNodeCodes.normalize(it.code) in readCodesBefore
        }
        val moduleNowComplete = module.nodes.isNotEmpty() && module.nodes.all {
            LegacyNodeCodes.normalize(it.code) in readCodesAfter
        }
        val bonus = if (!moduleWasComplete && moduleNowComplete) {
            XpRules.XP_PER_COMPLETE_MODULE_BONUS
        } else {
            0
        }
        return XpRules.XP_PER_READ_NODE + bonus
    }

    /**
     * Memilih 1 pratinjau hadiah karakter paling relevan berdasarkan progress modul saat ini
     * tanpa mengubah status `ENFORCE_CURRICULUM_LOCKS` di katalog.
     */
    fun nextUnlockPreview(
        completedCodes: Set<String>,
        modules: List<MriModule>,
        preferredModuleId: String? = null,
        items: List<CharacterItem> = CharacterCatalog.allItems
    ): CharacterUnlockProgress? {
        val normalizedCompleted = LegacyNodeCodes.normalizeAll(completedCodes)
        val candidates = items.mapNotNull { item ->
            val req = item.unlockRequirement
            val reqModId = req.requiredModuleId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (req.requiredNodesCount <= 0) return@mapNotNull null

            val matchingModules = modules.filter {
                it.id.equals(reqModId, ignoreCase = true) || it.code.equals(reqModId, ignoreCase = true)
            }.ifEmpty {
                modules.filter {
                    it.id.startsWith(reqModId, ignoreCase = true) ||
                        it.code.startsWith(reqModId, ignoreCase = true)
                }
            }
            val primaryMod = matchingModules.firstOrNull()
            val doneInModule = matchingModules.sumOf { mod ->
                mod.nodes.count {
                    LegacyNodeCodes.normalize(it.code) in normalizedCompleted
                }
            }
            val resolvedCode = if (matchingModules.size == 1) {
                primaryMod?.code ?: reqModId.uppercase()
            } else {
                reqModId.uppercase()
            }
            val resolvedTitle = req.requiredModuleTitle
                ?: (if (matchingModules.size == 1) primaryMod?.title else reqModId.uppercase())
                ?: reqModId.uppercase()

            CharacterUnlockProgress(
                item = item,
                moduleCode = resolvedCode,
                moduleTitle = resolvedTitle,
                completedInModule = doneInModule.coerceAtMost(req.requiredNodesCount),
                requiredCount = req.requiredNodesCount
            )
        }

        if (candidates.isEmpty()) return null

        val preferredIncomplete = if (!preferredModuleId.isNullOrBlank()) {
            candidates.filter {
                !it.isReady && (
                    it.moduleCode.equals(preferredModuleId, ignoreCase = true) ||
                        preferredModuleId.startsWith(it.moduleCode, ignoreCase = true)
                    )
            }.minByOrNull { it.requiredCount - it.completedInModule }
        } else {
            null
        }
        if (preferredIncomplete != null) return preferredIncomplete

        val nearestIncomplete = candidates
            .filter { !it.isReady }
            .minByOrNull { it.requiredCount - it.completedInModule }

        return nearestIncomplete ?: candidates.lastOrNull()
    }
}
