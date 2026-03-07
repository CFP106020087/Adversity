package com.adversity.sanctuary.inventory;

import com.adversity.sanctuary.TileEntitySanctuary;
import com.adversity.sanctuary.ritual.RitualManager;
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
    private final SlotRitualInput[] ritualSlots = new SlotRitualInput[TileEntitySanctuary.MAX_RITUAL_SLOTS];
    private final SlotOutput outputSlot;

    // 活躍槽位數 (由 maxInputCount 決定)
    private int activeInputSlots = 0;

    // 同步字段
    private int lastFuel = -1;
    private int lastMaxFuel = -1;
    private int lastMode = -1;
    private int lastTier = -1;
    private int lastActivated = -1;

    // GUI 座標常量
    private static final int CENTER_X = 98;
    private static final int CENTER_Y = 78;

    // 玩家背包起始 slot index
    private final int playerInvStart;

    public ContainerSanctuary(InventoryPlayer playerInv, TileEntitySanctuary te) {
        this.te = te;

        // Slot 0: 燃料槽 (位于GUI左侧)
        this.addSlotToContainer(new SlotFuel(te, TileEntitySanctuary.SLOT_FUEL, 8, 72));

        // Slot 1: Output 永遠在同心圓中心
        this.outputSlot = new SlotOutput(te, TileEntitySanctuary.SLOT_OUTPUT, -999, -999);
        this.addSlotToContainer(outputSlot);

        // Slot 2-101: 100 個 ritual input slots (默認 inactive, 螢幕外)
        for (int i = 0; i < TileEntitySanctuary.MAX_RITUAL_SLOTS; i++) {
            SlotRitualInput slot = new SlotRitualInput(te,
                    TileEntitySanctuary.SLOT_RITUAL_START + i, -999, -999);
            ritualSlots[i] = slot;
            this.addSlotToContainer(slot);
        }

        // 記錄玩家背包起始位置
        this.playerInvStart = this.inventorySlots.size();

        // 綁定玩家背包（下移以適應更大的儀式區域）
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 148 + i * 18));
            }
        }

        // 綁定快捷欄
        for (int k = 0; k < 9; ++k) {
            this.addSlotToContainer(new Slot(playerInv, k, 8 + k * 18, 206));
        }

        // 動態啟用槽位
        activateSlots(RitualManager.getMaxInputCount());
    }

    /**
     * 動態啟用/停用 ritual input slots + output slot
     * maxInputs = 0 → 全部螢幕外（包含 output）
     * maxInputs > 0 → output 在中心，input 環繞
     */
    public void activateSlots(int maxInputs) {
        maxInputs = Math.min(maxInputs, TileEntitySanctuary.MAX_RITUAL_SLOTS);
        this.activeInputSlots = maxInputs;

        if (maxInputs == 0) {
            // 空配置：output 也在螢幕外
            outputSlot.xPos = -999;
            outputSlot.yPos = -999;
            for (SlotRitualInput slot : ritualSlots) {
                slot.disable();
            }
            return;
        }

        // Output slot 在中心
        outputSlot.xPos = CENTER_X - 9;
        outputSlot.yPos = CENTER_Y - 9;

        // 計算 input slots 的同心圓位置
        int[][] positions = calculateConcentricLayout(maxInputs);

        for (int i = 0; i < TileEntitySanctuary.MAX_RITUAL_SLOTS; i++) {
            if (i < maxInputs) {
                ritualSlots[i].enable(positions[i][0], positions[i][1]);
            } else {
                ritualSlots[i].disable();
            }
        }
    }

    /**
     * 同心圓排版計算（Output 在中心，此計算僅 input 位置）
     */
    private static int[][] calculateConcentricLayout(int inputSlotCount) {
        if (inputSlotCount == 0) {
            return new int[0][2];
        }

        if (inputSlotCount == 1) {
            return new int[][] { { CENTER_X - 30 - 9, CENTER_Y - 9 } };
        }

        // 內圈最多 8 個
        int innerCount = Math.min(inputSlotCount, 8);
        int outerCount = inputSlotCount - innerCount;
        int innerRadius = 28;
        int outerRadius = 48;

        int[][] positions = new int[inputSlotCount][2];

        // 內圈
        for (int i = 0; i < innerCount; i++) {
            double angle = 2 * Math.PI * i / innerCount - Math.PI / 2;
            positions[i][0] = CENTER_X + (int) (innerRadius * Math.cos(angle)) - 9;
            positions[i][1] = CENTER_Y + (int) (innerRadius * Math.sin(angle)) - 9;
        }

        // 外圈（overflow）
        for (int i = 0; i < outerCount; i++) {
            double angle = 2 * Math.PI * i / outerCount - Math.PI / 2;
            positions[innerCount + i][0] = CENTER_X + (int) (outerRadius * Math.cos(angle)) - 9;
            positions[innerCount + i][1] = CENTER_Y + (int) (outerRadius * Math.sin(angle)) - 9;
        }

        return positions;
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

            int playerInvEnd = playerInvStart + 36;

            if (index < playerInvStart) {
                // 從聖所 → 玩家背包
                if (!this.mergeItemStack(itemstack1, playerInvStart, playerInvEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 從玩家背包 → 聖所
                if (TileEntitySanctuary.isValidFuel(itemstack1)) {
                    // 燃料槽 (index 0)
                    if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (activeInputSlots > 0) {
                    // 放入 active ritual input slots (index 2 ~ 2+activeInputSlots)
                    if (!this.mergeItemStack(itemstack1, 2, 2 + activeInputSlots, false)) {
                        // 背包內移動
                        int hotbarStart = playerInvStart + 27;
                        if (index < hotbarStart) {
                            if (!this.mergeItemStack(itemstack1, hotbarStart, playerInvEnd, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            if (!this.mergeItemStack(itemstack1, playerInvStart, hotbarStart, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                } else {
                    return ItemStack.EMPTY;
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

    public int getActiveInputSlots() {
        return activeInputSlots;
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
     * 輸出槽位 - 只能取出，不能放入
     */
    public static class SlotOutput extends Slot {
        public SlotOutput(TileEntitySanctuary te, int index, int x, int y) {
            super(te, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }
    }
}
