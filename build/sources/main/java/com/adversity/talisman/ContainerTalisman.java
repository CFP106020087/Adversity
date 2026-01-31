package com.adversity.talisman;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.talisman.ITalismanCapability;
import com.adversity.item.bauble.AbstractWardBauble;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 护符盒容器
 * 提供 9 个护符槽位和玩家背包
 */
public class ContainerTalisman extends Container {

    private final ITalismanCapability talismanCap;

    public ContainerTalisman(InventoryPlayer playerInv, ITalismanCapability talismanCap) {
        this.talismanCap = talismanCap;

        IItemHandler handler = talismanCap.getHandler();

        // 护符槽位 (3x3 布局, 居中显示)
        // 起始位置: x=62, y=17 (居中于 176x166 GUI)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                int x = 62 + col * 18;
                int y = 17 + row * 18;
                this.addSlotToContainer(new SlotTalisman(handler, slotIndex, x, y));
            }
        }

        // 玩家背包 (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 玩家快捷栏 (1x9)
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            result = stackInSlot.copy();

            // 从护符槽 (0-8) 移出到玩家背包
            if (index < 9) {
                if (!this.mergeItemStack(stackInSlot, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从玩家背包移入护符槽
            else if (stackInSlot.getItem() instanceof AbstractWardBauble) {
                if (!this.mergeItemStack(stackInSlot, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // 背包 <-> 快捷栏
            else if (index < 36) {
                if (!this.mergeItemStack(stackInSlot, 36, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.mergeItemStack(stackInSlot, 9, 36, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return result;
    }

    /**
     * 护符槽位 - 只接受 AbstractWardBauble
     */
    private static class SlotTalisman extends SlotItemHandler {
        public SlotTalisman(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack.getItem() instanceof AbstractWardBauble;
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }
    }
}
