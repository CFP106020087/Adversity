package com.adversity.mixin;

import com.adversity.client.gui.SealedSlotOverlayRenderer;
import com.adversity.curse.PermanentCurseManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin on vanilla Slot to prevent items from being placed into or merged into
 * sealed slots.
 *
 * Two injection points:
 * 1. isItemValid → false: blocks empty-slot placement (mergeItemStack 2nd pass,
 * PICKUP, QUICK_CRAFT)
 * 2. getSlotStackLimit → 0: blocks merge-into-existing (mergeItemStack 1st pass
 * uses this no-arg version)
 *
 * NOTE: Do NOT hook getItemStackLimit(ItemStack) — that method is used in
 * PICKUP's merge math,
 * and returning 0 causes k2 = 0 - count = NEGATIVE → shrink(negative)
 * duplicates items,
 * grow(negative) deletes items.
 */
@Mixin(value = Slot.class, remap = false)
public abstract class MixinSlot {

    @Shadow(aliases = "field_75222_d")
    public int slotNumber;

    @Shadow(aliases = "field_75224_c")
    public net.minecraft.inventory.IInventory inventory;

    @Shadow
    public abstract int getSlotIndex();

    /**
     * Block item placement into empty sealed slots.
     * Called by: mergeItemStack 2nd loop, slotClick PICKUP (empty slot),
     * QUICK_CRAFT stage 1+2
     */
    @Inject(method = { "isItemValid", "func_75214_a" }, at = @At("HEAD"), cancellable = true)
    private void adversity$blockSealedSlotPlacement(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (adversity$isThisSlotSealed()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Block item extraction from sealed slots.
     * PICKUP_ALL (double-click) scans all slots using canTakeStack — without this,
     * items get pulled from sealed slots into cursor.
     * Also covers THROW (Q on hovered slot in GUI).
     */
    @Inject(method = { "canTakeStack", "func_82869_a" }, at = @At("HEAD"), cancellable = true)
    private void adversity$blockSealedSlotExtraction(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (adversity$isThisSlotSealed()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Block item merging into existing stacks in sealed slots.
     * mergeItemStack 1st loop uses: Math.min(slot.getSlotStackLimit(),
     * stack.getMaxStackSize())
     * getSlotStackLimit() is the NO-ARG version (func_75219_a), returns 64 by
     * default.
     * Returning 0 makes maxSize=0, so j <= 0 is false and no merge occurs.
     *
     * IMPORTANT: This is NOT the same as getItemStackLimit(ItemStack) which is
     * Forge-added.
     */
    @Inject(method = { "getSlotStackLimit", "func_75219_a" }, at = @At("HEAD"), cancellable = true)
    private void adversity$blockSealedSlotMerge(CallbackInfoReturnable<Integer> cir) {
        if (adversity$isThisSlotSealed()) {
            cir.setReturnValue(0);
        }
    }

    /**
     * Shared sealed-slot check
     */
    private boolean adversity$isThisSlotSealed() {
        try {
            if (!(this.inventory instanceof InventoryPlayer)) {
                return false;
            }

            int slotIndex = getSlotIndex();
            if (slotIndex < 0 || slotIndex >= 36) {
                return false;
            }

            InventoryPlayer playerInv = (InventoryPlayer) this.inventory;
            EntityPlayer player = playerInv.player;

            int sealedCount;
            if (player.world.isRemote) {
                sealedCount = SealedSlotOverlayRenderer.ClientCurseCache.getSealedSlots();
            } else {
                PermanentCurseManager manager = PermanentCurseManager.get(player.world);
                sealedCount = manager.getBlackCoffinSealed(player);
            }

            if (sealedCount <= 0) {
                return false;
            }

            if (slotIndex >= 9 && slotIndex < 36) {
                return (slotIndex - 9) < sealedCount;
            } else if (slotIndex >= 0 && slotIndex < 9) {
                if (sealedCount <= 27)
                    return false;
                return slotIndex < (sealedCount - 27);
            }
        } catch (Exception e) {
            // Don't break slot validation
        }
        return false;
    }
}
