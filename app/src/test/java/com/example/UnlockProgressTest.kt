package com.example

import com.example.data.CharacterCatalog
import com.example.data.CharacterCategory
import com.example.data.CharacterItem
import com.example.data.MriRepository
import com.example.data.UnlockRequirement
import com.example.util.ItemUnlockState
import com.example.util.UnlockProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockProgressTest {

    private fun gatedItem(id: String, moduleId: String, requiredNodes: Int): CharacterItem =
        CharacterItem(
            id = id,
            name = "Item $id",
            category = CharacterCategory.RAMBUT,
            description = "Deskripsi",
            unlockRequirement = UnlockRequirement(
                requiredModuleId = moduleId,
                requiredNodesCount = requiredNodes
            )
        )

    @Test
    fun disabledLocksKeepAllItemsUnlockedWithSubtleRewardHint() {
        val item = gatedItem("test_m02", "M02", 5)
        val state = UnlockProgress.evaluate(
            item = item,
            modules = MriRepository.modules,
            completedNodeCodes = emptySet(),
            enforceCurriculumLocks = false
        )

        assertTrue(state is ItemUnlockState.Unlocked)
        assertEquals("Hadiah modul M02", (state as ItemUnlockState.Unlocked).rewardHint)
        assertNull(
            UnlockProgress.closestLockedItem(
                items = listOf(item),
                modules = MriRepository.modules,
                completedNodeCodes = emptySet(),
                enforceCurriculumLocks = false
            )
        )
    }

    @Test
    fun enabledLocksComputeProgressAndClosestLockedTip() {
        val m02 = MriRepository.modules.first { it.id == "M02" }
        val completedThree = m02.nodes.take(3).map { it.code }.toSet()
        val itemM02 = gatedItem("item_m02", "M02", 5)
        val itemM01 = gatedItem("item_m01", "M01", 10)

        val lockedM02 = UnlockProgress.evaluate(
            item = itemM02,
            modules = MriRepository.modules,
            completedNodeCodes = completedThree,
            enforceCurriculumLocks = true
        )
        assertTrue(lockedM02 is ItemUnlockState.Locked)
        lockedM02 as ItemUnlockState.Locked
        assertEquals(3, lockedM02.completedNodes)
        assertEquals(5, lockedM02.requiredNodes)
        assertEquals(2, lockedM02.remainingNodes)
        assertEquals("Selesaikan M02", lockedM02.requirementLabel)

        val tip = UnlockProgress.closestLockedItem(
            items = listOf(itemM01, itemM02),
            modules = MriRepository.modules,
            completedNodeCodes = completedThree,
            enforceCurriculumLocks = true
        )
        assertEquals("item_m02", tip?.item?.id)
    }

    @Test
    fun grandfatheredEquippedOrOwnedItemStaysUnlockedWhenLocksEnabled() {
        val item = gatedItem("grandfathered_item", "M02", 5)
        val equippedState = UnlockProgress.evaluate(
            item = item,
            modules = MriRepository.modules,
            completedNodeCodes = emptySet(),
            equippedItemIds = setOf("grandfathered_item"),
            enforceCurriculumLocks = true
        )
        val ownedState = UnlockProgress.evaluate(
            item = item,
            modules = MriRepository.modules,
            completedNodeCodes = emptySet(),
            ownedItemIds = setOf("grandfathered_item"),
            enforceCurriculumLocks = true
        )

        assertTrue(equippedState is ItemUnlockState.Unlocked)
        assertTrue(ownedState is ItemUnlockState.Unlocked)
        assertFalseLockedFlagUnchanged()
    }

    private fun assertFalseLockedFlagUnchanged() {
        assertEquals(false, CharacterCatalog.ENFORCE_CURRICULUM_LOCKS)
    }
}
