package com.adversity.compat;

import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.BaublesCapabilities;
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
     * 检查饰品是否可以被词条系统封印
     * 
     * 注意：此方法专门用于词条系统，不同于玩家手动卸下饰品的检查。
     * 如果物品实现了 IBauble 接口或通过 Capability 提供了 IBauble，
     * 且 canUnequip() 返回 false（无论任何游戏模式），则不允许封印。
     */
    public static boolean canUnequip(ItemStack stack, EntityPlayer player) {
        if (stack.isEmpty()) {
            return false;
        }

        // 首先检查直接实现 IBauble 接口的情况
        if (stack.getItem() instanceof IBauble) {
            IBauble bauble = (IBauble) stack.getItem();
            // 对于词条封印，我们需要检查物品的"固有"可卸下属性
            // 而不是依赖于游戏模式。通过模拟非创造模式玩家来检查。
            if (!bauble.canUnequip(stack, player)) {
                return false; // 物品明确禁止卸下
            }
        }

        // 然后检查通过 Capability 提供 IBauble 的情况
        IBauble capBauble = stack.getCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null);
        if (capBauble != null) {
            if (!capBauble.canUnequip(stack, player)) {
                return false; // Capability 明确禁止卸下
            }
        }

        // 如果没有任何 IBauble 实现禁止卸下，则允许封印
        return true;
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
