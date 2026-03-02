package com.adversity.sanctuary.inventory;

import com.adversity.sanctuary.TileEntitySanctuary;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ContainerSanctuary extends Container {

    private final TileEntitySanctuary te;

    // 同步字段
    private int lastFuel = -1;
    private int lastMaxFuel = -1;
    private int lastMode = -1;
    private int lastTier = -1;
    private int lastActivated = -1;

    public ContainerSanctuary(InventoryPlayer playerInv, TileEntitySanctuary te) {
        this.te = te;

        // 燃料槽位 (位于GUI左侧燃料条下方)
        this.addSlotToContainer(new SlotFuel(te, 0, 8, 72));

        // 仪式输入槽位 (GUI中央左侧，与GUI绘制对齐)
        this.addSlotToContainer(new Slot(te, 1, 46, 35));

        // 仪式输出槽位 (GUI中央右侧，只能取出，与GUI绘制对齐)
        this.addSlotToContainer(new SlotOutput(te, 2, 96, 35));

        // 绑定玩家背包 (slot 3-29)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 绑定玩家快捷栏 (slot 30-38)
        for (int k = 0; k < 9; ++k) {
            this.addSlotToContainer(new Slot(playerInv, k, 8 + k * 18, 142));
        }
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendAllWindowProperties(this, te);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        for (IContainerListener listener : this.listeners) {
            if (lastFuel != te.getField(0)) {
                listener.sendWindowProperty(this, 0, te.getField(0));
            }
            if (lastMaxFuel != te.getField(1)) {
                listener.sendWindowProperty(this, 1, te.getField(1));
            }
            if (lastMode != te.getField(2)) {
                listener.sendWindowProperty(this, 2, te.getField(2));
            }
            if (lastTier != te.getField(3)) {
                listener.sendWindowProperty(this, 3, te.getField(3));
            }
            if (lastActivated != te.getField(4)) {
                listener.sendWindowProperty(this, 4, te.getField(4));
            }
        }

        lastFuel = te.getField(0);
        lastMaxFuel = te.getField(1);
        lastMode = te.getField(2);
        lastTier = te.getField(3);
        lastActivated = te.getField(4);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        te.setField(id, data);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return !playerIn.isDead && playerIn.getDistanceSq(te.getPos()) <= 64.0;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            // 槽位0=燃料, 1=仪式输入, 2=仪式输出, 3-29=背包, 30-38=快捷栏
            if (index < 3) {
                // 从圣所槽位移出到玩家背包
                if (!this.mergeItemStack(itemstack1, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移入
                if (TileEntitySanctuary.isValidFuel(itemstack1)) {
                    // 先尝试放入燃料槽
                    if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // 尝试放入仪式输入槽
                    if (!this.mergeItemStack(itemstack1, 1, 2, false)) {
                        // 如果仪式槽满了，在背包内移动
                        if (index < 30) {
                            // 主背包到快捷栏
                            if (!this.mergeItemStack(itemstack1, 30, 39, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            // 快捷栏到主背包
                            if (!this.mergeItemStack(itemstack1, 3, 30, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }

    public TileEntitySanctuary getTileEntity() {
        return te;
    }

    /**
     * 燃料槽位 - 只接受有效燃料
     */
    public static class SlotFuel extends Slot {
        public SlotFuel(TileEntitySanctuary te, int index, int x, int y) {
            super(te, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return TileEntitySanctuary.isValidFuel(stack);
        }
    }

    /**
     * 输出槽位 - 只能取出，不能放入
     */
    public static class SlotOutput extends Slot {
        public SlotOutput(TileEntitySanctuary te, int index, int x, int y) {
            super(te, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false; // 不允许放入
        }
    }
}
