package com.adversity.asm;

import com.adversity.curse.PermanentCurseManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Hook for Container.slotClick
 * Called by ASM-injected code to check for sealed slots
 */
public class SlotClickHook {

    /**
     * Called at the beginning of Container.slotClick
     * @return ItemStack.EMPTY to cancel the click, null to continue normally
     */
    public static ItemStack onSlotClick(Container container, int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        // Only process on server side
        if (player.world.isRemote) {
            return null;
        }

        // Ignore invalid slot IDs
        if (slotId < 0) {
            return null;
        }

        // Get the slot being clicked
        if (slotId >= container.inventorySlots.size()) {
            return null;
        }

        Slot slot = container.inventorySlots.get(slotId);
        if (slot == null || slot.inventory != player.inventory) {
            return null;
        }

        // Get the slot index in player's inventory
        int slotIndex = slot.getSlotIndex();

        // Check if this slot is sealed by Black Coffin
        if (isSlotSealed(player, slotIndex)) {
            // Play deny sound
            player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.BLOCK_NOTE_BASS,
                net.minecraft.util.SoundCategory.PLAYERS,
                0.3f, 0.5f
            );

            // Return EMPTY to cancel the click
            return ItemStack.EMPTY;
        }

        // Also check if we're trying to shift-click INTO a sealed slot
        if (clickType == ClickType.QUICK_MOVE && !slot.getStack().isEmpty()) {
            // For shift-click, we need to check target slots
            // This is handled by the tick-based cleanup, so we allow it here
            // The item will be immediately moved out if it ends up in a sealed slot
        }

        // Continue with normal slotClick
        return null;
    }

    /**
     * Check if a slot index is sealed by Black Coffin
     * 先封主背包(9-35)，再封快捷栏(0-8)
     */
    private static boolean isSlotSealed(EntityPlayer player, int slotIndex) {
        try {
            PermanentCurseManager manager = PermanentCurseManager.get(player.world);
            int sealedCount = manager.getBlackCoffinSealed(player);

            if (sealedCount <= 0) {
                return false;
            }

            // 主背包有27个槽位(9-35)，快捷栏有9个槽位(0-8)
            if (slotIndex >= 9 && slotIndex < 36) {
                // 主背包槽位：先封印
                // slotIndex 9 对应第1个封印，slotIndex 35 对应第27个封印
                int sealOrder = slotIndex - 9;  // 0-26
                return sealOrder < sealedCount;
            } else if (slotIndex >= 0 && slotIndex < 9) {
                // 快捷栏槽位：后封印（在主背包全部封印之后）
                // 需要超过27个封印才开始封快捷栏
                if (sealedCount <= 27) return false;
                int hotbarSealed = sealedCount - 27;  // 快捷栏已封印数
                return slotIndex < hotbarSealed;
            }
            return false;

        } catch (Exception e) {
            // If there's any error (e.g., manager not available), don't block
            return false;
        }
    }
}
