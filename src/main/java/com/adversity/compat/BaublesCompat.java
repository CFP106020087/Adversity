package com.adversity.compat;

import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 获取Baubles槽位数量
     */
    public static int getSlotCount(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        return handler != null ? handler.getSlots() : 0;
    }

    /**
     * 获取指定槽位的饰品
     */
    public static ItemStack getStackInSlot(EntityPlayer player, int slot) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null || slot < 0 || slot >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }
        return handler.getStackInSlot(slot);
    }

    /**
     * 设置指定槽位的饰品
     */
    public static void setStackInSlot(EntityPlayer player, int slot, ItemStack stack) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler != null && slot >= 0 && slot < handler.getSlots()) {
            handler.setStackInSlot(slot, stack);
        }
    }

    /**
     * 检查饰品是否可以卸下
     */
    public static boolean canUnequip(ItemStack stack, EntityPlayer player) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof IBauble) {
            return ((IBauble) stack.getItem()).canUnequip(stack, player);
        }
        return true;  // 不是IBauble实现的物品默认可卸下
    }

    /**
     * 获取所有可封印的饰品槽位（非空且可卸下）
     */
    public static List<Integer> getAvailableSlotsForSeal(EntityPlayer player) {
        List<Integer> slots = new ArrayList<>();
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) {
            return slots;
        }

        int slotCount = handler.getSlots();
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && canUnequip(stack, player)) {
                slots.add(i);
            }
        }
        return slots;
    }

    /**
     * 获取槽位类型名称（动态，支持额外饰品栏位模组）
     * 格式: BAUBLE_<slot_index>
     */
    public static String getSlotTypeName(int slot) {
        // 使用统一格式，不硬编码槽位类型
        // 支持任意数量的饰品槽位（包括额外饰品栏位模组添加的）
        return "BAUBLE_" + slot;
    }
}
