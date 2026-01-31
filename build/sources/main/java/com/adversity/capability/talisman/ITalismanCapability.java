package com.adversity.capability.talisman;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;

/**
 * 护符盒能力接口
 * 存储玩家的 9 个守护护符槽
 */
public interface ITalismanCapability {

    /**
     * 获取护符槽位数量
     */
    int getSlotCount();

    /**
     * 获取物品处理器
     */
    IItemHandler getHandler();

    /**
     * 获取指定槽位的物品
     */
    ItemStack getStackInSlot(int slot);

    /**
     * 设置指定槽位的物品
     */
    void setStackInSlot(int slot, ItemStack stack);

    /**
     * 序列化到 NBT
     */
    NBTTagCompound serializeNBT();

    /**
     * 从 NBT 反序列化
     */
    void deserializeNBT(NBTTagCompound nbt);
}
