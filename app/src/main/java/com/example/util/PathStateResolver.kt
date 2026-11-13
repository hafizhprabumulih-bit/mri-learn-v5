package com.example.util

import com.example.data.LegacyNodeCodes
import com.example.data.MriModule
import com.example.data.MriNode

enum class NodeStatus {
    DONE,
    SKIPPED,
    CURRENT,
    OPEN,
    LOCKED
}

/**
 * Sakelar ketat/longgar penguncian simpul pada Peta Belajar.
 * Ketika `true`, hanya simpul `DONE`, `CURRENT`, dan `OPEN` yang dapat langsung dipelajari;
 * simpul `LOCKED` menampilkan lembar prasyarat.
 * Saat ini `false` (prasyarat lembut): semua simpul bisa dibuka, urutan hanya disarankan lewat
 * status `CURRENT` dan petunjuk `softPrerequisiteHint`. Tes lompat tetap ditawarkan untuk simpul
 * `OPEN` yang berada setelah simpul aktif.
 */
const val PATH_STRICT_LOCKS: Boolean = false

data class PathNodeState(
    val node: MriNode,
    val status: NodeStatus,
    val softPrerequisiteHint: String?
)

data class ModuleProgress(
    val moduleId: String,
    val done: Int,
    val total: Int
) {
    val fraction: Float
        get() = if (total <= 0) 0f else (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

data class ModulePathState(
    val module: MriModule,
    val nodes: List<PathNodeState>,
    val progress: ModuleProgress
)

/**
 * Resolver murni (tanpa dependensi Android/Room) untuk menghitung status setiap simpul dan modul
 * pada Peta Belajar dari progres tersimpan.
 */
object PathStateResolver {
    private const val RECOMMENDED_THRESHOLD_NUMERATOR = 3
    private const val RECOMMENDED_THRESHOLD_DENOMINATOR = 5 // 3/5 = 60%

    fun resolveModule(
        module: MriModule,
        allModules: List<MriModule>,
        completedCodes: Set<String>,
        strictLocks: Boolean = true,
        skippedCodes: Set<String> = emptySet()
    ): ModulePathState {
        val normalizedCompleted = LegacyNodeCodes.normalizeAll(completedCodes)
        val normalizedSkipped = LegacyNodeCodes.normalizeAll(skippedCodes)
        val modulesById = allModules.associateBy { it.id }
        val nodeToModuleId = buildNodeToModuleMap(allModules)
        val moduleCompletionCache = HashMap<String, Boolean>()

        return resolveSingleModule(
            module = module,
            modulesById = modulesById,
            nodeToModuleId = nodeToModuleId,
            normalizedCompleted = normalizedCompleted,
            normalizedSkipped = normalizedSkipped,
            strictLocks = strictLocks,
            moduleCompletionCache = moduleCompletionCache
        )
    }

    fun resolveAll(
        modules: List<MriModule>,
        completedCodes: Set<String>,
        strictLocks: Boolean = true,
        skippedCodes: Set<String> = emptySet()
    ): List<ModulePathState> {
        val normalizedCompleted = LegacyNodeCodes.normalizeAll(completedCodes)
        val normalizedSkipped = LegacyNodeCodes.normalizeAll(skippedCodes)
        val orderedModules = orderModules(modules)
        val modulesById = orderedModules.associateBy { it.id }
        val nodeToModuleId = buildNodeToModuleMap(orderedModules)
        val moduleCompletionCache = HashMap<String, Boolean>()

        return orderedModules.map { module ->
            resolveSingleModule(
                module = module,
                modulesById = modulesById,
                nodeToModuleId = nodeToModuleId,
                normalizedCompleted = normalizedCompleted,
                normalizedSkipped = normalizedSkipped,
                strictLocks = strictLocks,
                moduleCompletionCache = moduleCompletionCache
            )
        }
    }

    fun orderNodesPublic(nodes: List<MriNode>): List<MriNode> =
        orderNodesWithEffectiveSort(nodes).map { it.node }

    fun currentNodeOf(state: ModulePathState): PathNodeState? =
        state.nodes.firstOrNull { it.status == NodeStatus.CURRENT }

    /** Simpul setelah [nodeCode] menurut urutan peta, atau `null` bila terakhir / tidak ditemukan. */
    fun nextNodeOf(state: ModulePathState, nodeCode: String): PathNodeState? {
        val idx = indexOfNode(state, nodeCode)
        return if (idx >= 0 && idx + 1 < state.nodes.size) state.nodes[idx + 1] else null
    }

    /** Posisi 1-based [nodeCode] dan total simpul modul; posisi 0 bila tidak ditemukan. */
    fun positionOf(state: ModulePathState, nodeCode: String): Pair<Int, Int> {
        val idx = indexOfNode(state, nodeCode)
        return Pair(if (idx >= 0) idx + 1 else 0, state.nodes.size)
    }

    private fun indexOfNode(state: ModulePathState, nodeCode: String): Int {
        val target = LegacyNodeCodes.normalize(nodeCode.trim())
        if (target.isBlank()) return -1
        return state.nodes.indexOfFirst { LegacyNodeCodes.normalize(it.node.code) == target }
    }

    fun isModuleRecommended(
        modules: List<MriModule>,
        completedCodes: Set<String>,
        moduleId: String
    ): Boolean {
        val orderedModules = orderModules(modules)
        val targetIndex = orderedModules.indexOfFirst { it.id == moduleId }
        if (targetIndex < 0) return false
        if (targetIndex == 0) return true

        val normalizedCompleted = LegacyNodeCodes.normalizeAll(completedCodes)
        val previousModule = orderedModules[targetIndex - 1]
        return isModuleAtLeast60Percent(previousModule, normalizedCompleted)
    }

    private fun resolveSingleModule(
        module: MriModule,
        modulesById: Map<String, MriModule>,
        nodeToModuleId: Map<String, String>,
        normalizedCompleted: Set<String>,
        normalizedSkipped: Set<String>,
        strictLocks: Boolean,
        moduleCompletionCache: MutableMap<String, Boolean>
    ): ModulePathState {
        val orderedWithOrder = orderNodesWithEffectiveSort(module.nodes)
        val total = orderedWithOrder.size
        val isDoneFlags = BooleanArray(total) { idx ->
            LegacyNodeCodes.normalize(orderedWithOrder[idx].node.code) in normalizedCompleted
        }

        // Precompute max sortOrder of any completed node so aturan (b) works in O(N).
        var maxCompletedOrder = Int.MIN_VALUE
        for (i in 0 until total) {
            if (isDoneFlags[i]) {
                val order = orderedWithOrder[i].effectiveOrder
                if (order > maxCompletedOrder) {
                    maxCompletedOrder = order
                }
            }
        }

        var doneCount = 0
        var assignedCurrent = false
        val resolvedNodes = ArrayList<PathNodeState>(total)
        val ownNodeCodes = orderedWithOrder.mapTo(HashSet(total)) {
            LegacyNodeCodes.normalize(it.node.code)
        }

        for (i in 0 until total) {
            val item = orderedWithOrder[i]
            val node = item.node
            val normalizedCode = LegacyNodeCodes.normalize(node.code)
            val isDone = isDoneFlags[i]
            val status = if (isDone) {
                doneCount++
                if (normalizedCode in normalizedSkipped) NodeStatus.SKIPPED else NodeStatus.DONE
            } else {
                val unlocked = if (!strictLocks) {
                    true
                } else {
                    val isFirst = i == 0
                    val isPreviousDone = i > 0 && isDoneFlags[i - 1]
                    val hasCompletedLaterOrder = maxCompletedOrder > item.effectiveOrder
                    isFirst || isPreviousDone || hasCompletedLaterOrder
                }

                if (!unlocked) {
                    NodeStatus.LOCKED
                } else if (!assignedCurrent) {
                    assignedCurrent = true
                    NodeStatus.CURRENT
                } else {
                    NodeStatus.OPEN
                }
            }

            val hint = resolveSoftPrerequisiteHint(
                module = module,
                node = node,
                modulesById = modulesById,
                nodeToModuleId = nodeToModuleId,
                ownNodeCodes = ownNodeCodes,
                normalizedCompleted = normalizedCompleted,
                moduleCompletionCache = moduleCompletionCache
            )

            resolvedNodes.add(
                PathNodeState(
                    node = node,
                    status = status,
                    softPrerequisiteHint = hint
                )
            )
        }

        return ModulePathState(
            module = module,
            nodes = resolvedNodes,
            progress = ModuleProgress(
                moduleId = module.id,
                done = doneCount,
                total = total
            )
        )
    }

    private fun resolveSoftPrerequisiteHint(
        module: MriModule,
        node: MriNode,
        modulesById: Map<String, MriModule>,
        nodeToModuleId: Map<String, String>,
        ownNodeCodes: Set<String>,
        normalizedCompleted: Set<String>,
        moduleCompletionCache: MutableMap<String, Boolean>
    ): String? {
        val raw = node.prerequisite?.trim().orEmpty()
        if (raw.isEmpty()) return null

        val unmetLabels = LinkedHashSet<String>()
        raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { rawToken ->
                val cleanToken = rawToken.substringBefore("·").trim()
                if (cleanToken.isEmpty() || cleanToken.equals("Opsional", ignoreCase = true)) {
                    return@forEach
                }
                val normalizedToken = LegacyNodeCodes.normalize(cleanToken)

                val targetModule = modulesById[normalizedToken]
                if (targetModule != null) {
                    if (targetModule.id != module.id) {
                        val met = moduleCompletionCache.getOrPut(targetModule.id) {
                            isModuleAtLeast60Percent(targetModule, normalizedCompleted)
                        }
                        if (!met) {
                            unmetLabels.add(targetModule.code)
                        }
                    }
                    return@forEach
                }

                val targetCode = when {
                    normalizedToken in nodeToModuleId || normalizedToken in ownNodeCodes -> normalizedToken
                    else -> {
                        val relativeCode = LegacyNodeCodes.normalize("${module.id}.$normalizedToken")
                        if (relativeCode in nodeToModuleId || relativeCode in ownNodeCodes) relativeCode else null
                    }
                }

                if (targetCode != null && targetCode !in normalizedCompleted) {
                    unmetLabels.add(targetCode)
                }
            }

        if (unmetLabels.isEmpty()) return null
        return "Disarankan: selesaikan ${unmetLabels.joinToString(", ")} dulu"
    }

    private fun isModuleAtLeast60Percent(
        module: MriModule,
        normalizedCompleted: Set<String>
    ): Boolean {
        val total = module.nodes.size
        if (total <= 0) return false
        val done = module.nodes.count { LegacyNodeCodes.normalize(it.code) in normalizedCompleted }
        return done * RECOMMENDED_THRESHOLD_DENOMINATOR >= total * RECOMMENDED_THRESHOLD_NUMERATOR
    }

    private fun orderModules(modules: List<MriModule>): List<MriModule> =
        modules.withIndex()
            .sortedWith(
                compareBy(
                    { if (it.value.sortOrder != 0) it.value.sortOrder else it.index + 1 },
                    { it.index }
                )
            )
            .map { it.value }

    private fun orderNodesWithEffectiveSort(nodes: List<MriNode>): List<OrderedNode> =
        nodes.withIndex()
            .map { (idx, node) ->
                OrderedNode(
                    node = node,
                    effectiveOrder = if (node.sortOrder != 0) node.sortOrder else idx + 1,
                    originalIndex = idx
                )
            }
            .sortedWith(compareBy({ it.effectiveOrder }, { it.originalIndex }))

    private fun buildNodeToModuleMap(modules: List<MriModule>): Map<String, String> {
        val map = HashMap<String, String>()
        for (mod in modules) {
            for (node in mod.nodes) {
                map[LegacyNodeCodes.normalize(node.code)] = mod.id
            }
        }
        return map
    }

    private data class OrderedNode(
        val node: MriNode,
        val effectiveOrder: Int,
        val originalIndex: Int
    )
}
