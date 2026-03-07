package com.adversity.sanctuary.inventory;

import com.adversity.sanctuary.TileEntitySanctuary;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * 動態儀式輸入槽位
 *
 * 100 個預創建，默認 inactive (xPos=-999, isItemValid=false)。
 * 通過 enable(x,y) / disable() 動態控制。
 */
public class SlotRitualInput extends Slot {

    private boolean active = false;

    public SlotRitualInput(TileEntitySanctuary te, int index, int x, int y) {
        super(te, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return active;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return active;
    }

    @Override
    public int getSlotStackLimit() {
        return active ? 64 : 0;
    }

    public void enable(int x, int y) {
        this.xPos = x;
        this.yPos = y;
        this.active = true;
    }

    public void disable() {
        this.xPos = -999;
        this.yPos = -999;
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }
}
