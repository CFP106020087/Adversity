package com.adversity.compat;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Baubles兼容层 - 仅在Baubles加载时使用
 *
 * 注意：此类只能在确认Baubles已加载后调用
 */
public class BaublesCompat {

    /**
     * 计算玩家装备的Baubles饰品数量
     */
    public static int countEquippedBaubles(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) {
            return 0;
        }

        int count = 0;
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
