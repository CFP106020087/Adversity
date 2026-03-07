package com.adversity.mixin;

import com.adversity.Adversity;
import com.adversity.enchantment.EnchantmentGatingHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 鐵砧門控 Mixin
 *
 * 職責：
 * 在 updateRepairOutput 後檢查附魔書上的附魔是否被門控，
 * 若全部被門控則清空輸出，若部分門控則移除被門控的附魔。
 *
 * 所有業務邏輯委託給 EnchantmentGatingHelper。
 */
@Mixin(value = ContainerRepair.class, remap = false)
public abstract class MixinContainerRepair {

    // vanilla fields — 雙名稱 (MCP + SRG)
    @Shadow(aliases = "field_82855_n")
    private EntityPlayer player;

    @Shadow(aliases = "field_82852_f")
    private IInventory outputSlot;

    @Shadow(aliases = "field_82853_g")
    private IInventory inputSlots;

    /**
     * 注入點: updateRepairOutput RETURN
     * 過濾被門控的附魔書附魔
     */
    @Inject(method = { "updateRepairOutput", "func_82848_d" }, at = @At("RETURN"))
    private void adversity$filterAnvilOutput(CallbackInfo ci) {
        try {
            EnchantmentGatingHelper.filterAnvilOutput(outputSlot, inputSlots, player);
        } catch (Exception e) {
            Adversity.LOGGER.warn("[Anvil Mixin] Error in filterAnvilOutput: {}", e.getMessage());
        }
    }
}
