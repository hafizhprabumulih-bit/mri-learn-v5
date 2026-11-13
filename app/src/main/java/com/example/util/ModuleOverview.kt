package com.example.util

import com.example.data.CharacterCatalog
import com.example.data.LegacyNodeCodes
import com.example.data.MriModule
import com.example.data.MriNode
import com.example.data.MriQuizQuestion
import com.example.data.UnlockRequirement

data class LevelOverview(
    val level: Int,
    val label: String,
    val doneNodes: Int,
    val totalNodes: Int,
    val quizCount: Int
)

data class ModuleOverviewSummary(
    val module: MriModule,
    val chapterNumber: Int,
    val moduleIndexInCurriculum: Int,
    val totalModules: Int,
    val totalNodes: Int,
    val completedNodes: Int,
    val totalQuizzes: Int,
    val hasSimulation: Boolean,
    val triedLabCount: Int = 0,
    val totalLabCount: Int = 0,
    val levels: List<LevelOverview>,
    val crossModulePrerequisites: List<String>,
    val isRecommended: Boolean,
    val currentNode: MriNode?,
    val isComplete: Boolean,
    val isNotStarted: Boolean,
    val rewardItemName: String?
)

/**
 * Perhitungan murni ringkasan modul untuk bottom sheet pratinjau di Beranda.
 */
object ModuleOverview {

    fun compute(
        module: MriModule,
        allModules: List<MriModule>,
        completedCodes: Set<String>,
        quizzes: List<MriQuizQuestion>,
        enforceCurriculumLocks: Boolean = CharacterCatalog.ENFORCE_CURRICULUM_LOCKS,
        triedLabNodes: Set<String> = emptySet()
    ): ModuleOverviewSummary {
        val normalizedCompleted = LegacyNodeCodes.normalizeAll(completedCodes)
        val pathState = PathStateResolver.resolveModule(
            module = module,
            allModules = allModules,
            completedCodes = normalizedCompleted,
            strictLocks = PATH_STRICT_LOCKS
        )
        val moduleQuizzes = quizzes.filter { it.moduleId == module.id }

        val levels = (1..3).mapNotNull { lvl ->
            val nodesInLevel = module.nodes.filter { it.level == lvl }
            val quizzesInLevel = moduleQuizzes.count { it.level == lvl }
            if (nodesInLevel.isEmpty() && quizzesInLevel == 0) {
                null
            } else {
                val doneInLevel = nodesInLevel.count {
                    LegacyNodeCodes.normalize(it.code) in normalizedCompleted
                }
                val label = when (lvl) {
                    1 -> "Dasar"
                    2 -> "Menengah"
                    else -> "Lanjut"
                }
                LevelOverview(
                    level = lvl,
                    label = label,
                    doneNodes = doneInLevel,
                    totalNodes = nodesInLevel.size,
                    quizCount = quizzesInLevel
                )
            }
        }

        val moduleCodeSet = allModules.map { it.code.uppercase() }.toSet()
        val crossModulePrereqs = LinkedHashSet<String>()
        module.nodes.forEach { node ->
            node.prerequisites.forEach { rawPrereq ->
                val targetModule = extractCrossModuleCode(rawPrereq, module.code, moduleCodeSet)
                if (targetModule != null) {
                    crossModulePrereqs.add(targetModule)
                }
            }
        }

        val chapterNumber = HomePathPlanner.chapterKeyOf(module.code)
            .drop(1)
            .toIntOrNull()
            ?: 1
        val moduleIndex = (allModules.indexOfFirst { it.id == module.id }.takeIf { it >= 0 } ?: 0) + 1
        val totalNodes = pathState.progress.total
        val completedNodes = pathState.progress.done
        val isRecommended = PathStateResolver.isModuleRecommended(
            modules = allModules,
            completedCodes = normalizedCompleted,
            moduleId = module.id
        )
        val currentNode = PathStateResolver.currentNodeOf(pathState)?.node

        val rewardItemName = if (enforceCurriculumLocks) {
            CharacterCatalog.allItems.firstOrNull { item ->
                val req = item.unlockRequirement
                req.requiredModuleId?.equals(module.id, ignoreCase = true) == true ||
                    req.requiredModuleId?.equals(module.code, ignoreCase = true) == true
            }?.name
        } else {
            null
        }

        val labNodes = module.nodes.filter { !it.labType.isNullOrBlank() }
        val normalizedTried = LegacyNodeCodes.normalizeAll(triedLabNodes)
        val triedLabCount = labNodes.count { LegacyNodeCodes.normalize(it.code) in normalizedTried }

        return ModuleOverviewSummary(
            module = module,
            chapterNumber = chapterNumber,
            moduleIndexInCurriculum = moduleIndex,
            totalModules = allModules.size.coerceAtLeast(1),
            totalNodes = totalNodes,
            completedNodes = completedNodes,
            totalQuizzes = moduleQuizzes.size,
            hasSimulation = labNodes.isNotEmpty(),
            triedLabCount = triedLabCount,
            totalLabCount = labNodes.size,
            levels = levels,
            crossModulePrerequisites = crossModulePrereqs.toList(),
            isRecommended = isRecommended,
            currentNode = currentNode,
            isComplete = totalNodes > 0 && completedNodes >= totalNodes,
            isNotStarted = completedNodes == 0,
            rewardItemName = rewardItemName
        )
    }

    internal fun extractCrossModuleCode(
        rawPrerequisite: String,
        currentModuleCode: String,
        knownModuleCodes: Set<String>
    ): String? {
        val trimmed = rawPrerequisite.trim().uppercase()
        if (trimmed.isBlank()) return null

        val candidate = when {
            trimmed.startsWith("M") && trimmed.contains(".") -> trimmed.substringBefore(".")
            trimmed.startsWith("M") -> trimmed
            else -> return null
        }

        if (candidate.equals(currentModuleCode, ignoreCase = true)) return null
        return if (candidate in knownModuleCodes) candidate else candidate
    }
}
