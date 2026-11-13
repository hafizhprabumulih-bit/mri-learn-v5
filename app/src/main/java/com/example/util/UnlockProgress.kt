package com.example.util

import com.example.data.CharacterCatalog
import com.example.data.CharacterItem
import com.example.data.LegacyNodeCodes
import com.example.data.MriModule

sealed interface ItemUnlockState {
    data class Unlocked(
        val rewardHint: String? = null
    ) : ItemUnlockState

    data class Locked(
        val progress: Float,
        val completedNodes: Int,
        val requiredNodes: Int,
        val remainingNodes: Int,
        val moduleId: String,
        val requirementLabel: String
    ) : ItemUnlockState
}

data class ClosestLockedItemTip(
    val item: CharacterItem,
    val state: ItemUnlockState.Locked
)

/**
 * Perhitungan murni status & progres buka-kunci item Ruang Karakter.
 * Mendukung dua kondisi `ENFORCE_CURRICULUM_LOCKS` serta grandfathering item terpasang/dimiliki.
 */
object UnlockProgress {

    fun evaluate(
        item: CharacterItem,
        modules: List<MriModule>,
        completedNodeCodes: Set<String>,
        equippedItemIds: Set<String> = emptySet(),
        ownedItemIds: Set<String> = emptySet(),
        enforceCurriculumLocks: Boolean = CharacterCatalog.ENFORCE_CURRICULUM_LOCKS
    ): ItemUnlockState {
        val req = item.unlockRequirement
        val moduleId = req.requiredModuleId?.trim()?.takeIf { it.isNotBlank() }

        if (!enforceCurriculumLocks) {
            val hint = moduleId?.let { "Hadiah modul ${it.uppercase()}" }
            return ItemUnlockState.Unlocked(rewardHint = hint)
        }

        // Grandfathering: item yang sudah terpasang atau dimiliki sebelum flag aktif tetap terbuka
        if (item.id in equippedItemIds || item.id in ownedItemIds) {
            return ItemUnlockState.Unlocked()
        }

        if (moduleId == null || req.requiredNodesCount <= 0) {
            return ItemUnlockState.Unlocked()
        }

        val normalizedCompleted = LegacyNodeCodes.normalizeAll(completedNodeCodes)
        val completedInModule = modules
            .filter { mod ->
                mod.id.equals(moduleId, ignoreCase = true) ||
                    mod.code.equals(moduleId, ignoreCase = true) ||
                    mod.id.startsWith(moduleId, ignoreCase = true)
            }
            .flatMap { it.nodes }
            .count { LegacyNodeCodes.normalize(it.code) in normalizedCompleted }

        if (completedInModule >= req.requiredNodesCount) {
            return ItemUnlockState.Unlocked()
        }

        val remaining = (req.requiredNodesCount - completedInModule).coerceAtLeast(1)
        val fraction = (completedInModule.toFloat() / req.requiredNodesCount.toFloat()).coerceIn(0f, 1f)
        return ItemUnlockState.Locked(
            progress = fraction,
            completedNodes = completedInModule,
            requiredNodes = req.requiredNodesCount,
            remainingNodes = remaining,
            moduleId = moduleId.uppercase(),
            requirementLabel = "Selesaikan ${moduleId.uppercase()}"
        )
    }

    fun closestLockedItem(
        items: List<CharacterItem>,
        modules: List<MriModule>,
        completedNodeCodes: Set<String>,
        equippedItemIds: Set<String> = emptySet(),
        ownedItemIds: Set<String> = emptySet(),
        enforceCurriculumLocks: Boolean = CharacterCatalog.ENFORCE_CURRICULUM_LOCKS
    ): ClosestLockedItemTip? {
        if (!enforceCurriculumLocks) return null
        return items
            .mapNotNull { item ->
                val state = evaluate(
                    item = item,
                    modules = modules,
                    completedNodeCodes = completedNodeCodes,
                    equippedItemIds = equippedItemIds,
                    ownedItemIds = ownedItemIds,
                    enforceCurriculumLocks = true
                )
                if (state is ItemUnlockState.Locked) {
                    ClosestLockedItemTip(item = item, state = state)
                } else {
                    null
                }
            }
            .minWithOrNull(
                compareBy<ClosestLockedItemTip> { it.state.remainingNodes }
                    .thenByDescending { it.state.progress }
            )
    }
}
