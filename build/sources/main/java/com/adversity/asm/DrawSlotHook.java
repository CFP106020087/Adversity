package com.adversity.asm;

import com.adversity.client.gui.SealedSlotOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Hook for GuiContainer.drawSlot
 * Called by ASM-injected code to check if slot rendering should be skipped
 */
@SideOnly(Side.CLIENT)
public class DrawSlotHook {

    /**
     * Check if a slot's item rendering should be skipped (sealed by Black Coffin)
     * @param slot The slot being rendered
     * @return true to skip rendering, false to render normally
     */
    public static boolean shouldSkipSlot(Slot slot) {
        if (slot == null) return false;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return false;

        // Check if this slot belongs to player's inventory
        if (slot.inventory != player.inventory) {
            return false;
        }

        // Get sealed slot count from client cache
        int sealedCount = SealedSlotOverlayRenderer.ClientCurseCache.getSealedSlots();
        if (sealedCount <= 0) return false;

        // Check if this slot index is sealed
        int slotIndex = slot.getSlotIndex();
        return isSlotSealedByCount(slotIndex, sealedCount);
    }

    /**
     * Check if a slot index is sealed given the sealed count
     * 先封主背包(9-35)，再封快捷栏(0-8)
     */
    private static boolean isSlotSealedByCount(int slotIndex, int sealedCount) {
        if (sealedCount <= 0) return false;

        // 主背包有27个槽位(9-35)，快捷栏有9个槽位(0-8)
        if (slotIndex >= 9 && slotIndex < 36) {
            // 主背包槽位：先封印
            int sealOrder = slotIndex - 9;  // 0-26
            return sealOrder < sealedCount;
        } else if (slotIndex >= 0 && slotIndex < 9) {
            // 快捷栏槽位：后封印（在主背包全部封印之后）
            if (sealedCount <= 27) return false;
            int hotbarSealed = sealedCount - 27;
            return slotIndex < hotbarSealed;
        }
        return false;
    }
}
