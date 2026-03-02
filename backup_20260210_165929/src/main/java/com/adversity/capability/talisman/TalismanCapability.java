package com.adversity.capability.talisman;

import com.adversity.item.bauble.AbstractWardBauble;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 护符盒能力实现
 * 提供 9 个专用槽位存放守护护符
 */
public class TalismanCapability implements ITalismanCapability {

    public static final int SLOT_COUNT = 9;

    private final ItemStackHandler handler;

    public TalismanCapability() {
        this.handler = new ItemStackHandler(SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                // 只接受 AbstractWardBauble 类型的物品
                return stack.getItem() instanceof AbstractWardBauble;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1; // 每个槽位只能放 1 个
            }
        };
    }

    @Override
    public int getSlotCount() {
        return SLOT_COUNT;
    }

    @Override
    public IItemHandler getHandler() {
        return handler;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return handler.getStackInSlot(slot);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < SLOT_COUNT) {
            handler.setStackInSlot(slot, stack);
        }
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("items", handler.serializeNBT());
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("items")) {
            handler.deserializeNBT(nbt.getCompoundTag("items"));
        }
    }
}
