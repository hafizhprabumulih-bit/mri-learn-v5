package com.example.util

import com.example.data.MriModule

data class HomeModuleNodeState(
    val module: MriModule,
    val globalIndex: Int,
    val done: Int,
    val total: Int,
    val status: NodeStatus,
    val recommended: Boolean,
    val previousModuleCode: String?
)

data class HomeChapterGroup(
    val chapterKey: String,
    val chapterTitle: String,
    val chapterSubtitle: String,
    val modules: List<HomeModuleNodeState>
)

/**
 * Perencana murni untuk mengelompokkan 18 modul menjadi 9 bab berurutan pada peta Beranda
 * serta menandai 1 modul target terawal sebagai `CURRENT`.
 */
object HomePathPlanner {

    fun chapterKeyOf(moduleCode: String): String {
        val digits = moduleCode.drop(1).takeWhile { it.isDigit() }
        return if (digits.isNotBlank()) "M$digits" else moduleCode
    }

    fun plan(
        modules: List<MriModule>,
        completedCodes: Set<String>
    ): List<HomeChapterGroup> {
        if (modules.isEmpty()) return emptyList()

        val modulePaths = PathStateResolver.resolveAll(
            modules = modules,
            completedCodes = completedCodes
        )

        var assignedCurrent = false
        val flatStates = ArrayList<HomeModuleNodeState>(modulePaths.size)

        modulePaths.forEachIndexed { index, pathState ->
            val mod = pathState.module
            val done = pathState.progress.done
            val total = pathState.progress.total
            val isComplete = total > 0 && done >= total
            val recommended = PathStateResolver.isModuleRecommended(
                modules = modules,
                completedCodes = completedCodes,
                moduleId = mod.id
            )
            val prevCode = if (index > 0) modulePaths[index - 1].module.code else null

            val status = when {
                isComplete -> NodeStatus.DONE
                !assignedCurrent -> {
                    assignedCurrent = true
                    NodeStatus.CURRENT
                }
                else -> NodeStatus.OPEN
            }

            flatStates.add(
                HomeModuleNodeState(
                    module = mod,
                    globalIndex = index,
                    done = done,
                    total = total,
                    status = status,
                    recommended = recommended,
                    previousModuleCode = prevCode
                )
            )
        }

        val groupsByChapter = LinkedHashMap<String, MutableList<HomeModuleNodeState>>()
        flatStates.forEach { state ->
            val key = chapterKeyOf(state.module.code)
            groupsByChapter.getOrPut(key) { mutableListOf() }.add(state)
        }

        return groupsByChapter.entries.map { (chapterKey, chapterModules) ->
            val (title, subtitle) = chapterHeaderFor(chapterKey, chapterModules)
            HomeChapterGroup(
                chapterKey = chapterKey,
                chapterTitle = title,
                chapterSubtitle = subtitle,
                modules = chapterModules.toList()
            )
        }
    }

    private fun chapterHeaderFor(
        chapterKey: String,
        modules: List<HomeModuleNodeState>
    ): Pair<String, String> {
        val chapterNumber = chapterKey.drop(1).toIntOrNull()
        val numPrefix = if (chapterNumber != null) "Bab $chapterNumber" else chapterKey

        val title = if (modules.size == 1) {
            "$numPrefix · ${modules.first().module.title}"
        } else {
            when (chapterKey) {
                "M03" -> "$numPrefix · Sekuens & Parameter Kontras"
                "M04" -> "$numPrefix · Pembentukan Citra & K-space"
                "M06" -> "$numPrefix · Koil Gradien & RF"
                "M07" -> "$numPrefix · Keselamatan MRI"
                "M08" -> "$numPrefix · Artefak & Kontrol Kualitas"
                else -> "$numPrefix · ${modules.first().module.title.replace("Ruang-k", "K-space").replace("ruang-k", "K-space")}"
            }
        }

        val totalDone = modules.sumOf { it.done }
        val totalNodes = modules.sumOf { it.total }
        val codesSummary = if (modules.size > 1) {
            modules.joinToString(" · ") { it.module.code } + " • "
        } else {
            ""
        }
        val subtitle = "${codesSummary}${totalDone}/${totalNodes} materi selesai"
        return title to subtitle
    }

    fun isDimmed(modState: HomeModuleNodeState): Boolean =
        !modState.recommended && modState.status != NodeStatus.DONE

    fun levelSummary(levelBadge: String): String {
        val normalized = levelBadge.lowercase()
        return when {
            normalized.contains("t1") && normalized.contains("t3") -> "Dasar → Lanjut"
            normalized.contains("t1") && normalized.contains("t2") -> "Dasar → Menengah"
            normalized.contains("t2") && normalized.contains("t3") -> "Menengah → Lanjut"
            normalized.contains("t3") -> "Lanjut"
            normalized.contains("t2") -> "Menengah"
            normalized.contains("t1") -> "Dasar"
            else -> levelBadge.ifBlank { "Kurikulum MRI" }
        }
    }

    fun moduleSubtitle(modState: HomeModuleNodeState): String {
        return "${levelSummary(modState.module.levelBadge)} · ${modState.total} simpul"
    }
}
