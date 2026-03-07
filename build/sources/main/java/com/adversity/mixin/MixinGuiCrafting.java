package com.adversity.mixin;

import com.adversity.client.gui.GatingHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作台 3×3 GUI — GS badge + ❌ 提示
 */
@Mixin(value = GuiCrafting.class, remap = false)
public class MixinGuiCrafting {

    @Inject(method = { "drawGuiContainerForegroundLayer", "func_146979_b" }, at = @At("TAIL"))
    private void adversity$renderGatingHud(int mouseX, int mouseY, CallbackInfo ci) {
        GuiContainer gui = (GuiContainer) (Object) this;
        List<String> blocked = adversity$getBlockedCraftingItems();
        GatingHudRenderer.setBlockedTitle("adversity.gui.blocked_crafting");
        GatingHudRenderer.setBlockedItems(blocked);
        GatingHudRenderer.renderGsHud(gui.mc.fontRenderer, gui.getXSize());
    }

    @Inject(method = { "mouseClicked", "func_73864_a" }, at = @At("HEAD"), cancellable = true)
    private void adversity$handleHudClick(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        GuiContainer gui = (GuiContainer) (Object) this;
        int relX = mouseX - gui.getGuiLeft();
        int relY = mouseY - gui.getGuiTop();
        if (GatingHudRenderer.handleClick(relX, relY)) {
            ci.cancel();
        }
    }

    /**
     * 檢查合成格中是否存在被 gate 的配方
     */
    private List<String> adversity$getBlockedCraftingItems() {
        List<String> blocked = new ArrayList<>();
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null)
            return blocked;

        GuiContainer gui = (GuiContainer) (Object) this;
        // output slot = slot 0
        Slot outputSlot = gui.inventorySlots.getSlot(0);
        if (outputSlot.getHasStack())
            return blocked; // 有 output → 沒被 gate

        // 檢查是否有 input
        boolean hasInput = false;
        for (int i = 1; i <= 9; i++) {
            if (i < gui.inventorySlots.inventorySlots.size()) {
                Slot slot = gui.inventorySlots.getSlot(i);
                if (slot.getHasStack()) {
                    hasInput = true;
                    break;
                }
            }
        }
        if (!hasInput)
            return blocked;

        // 有 input 但 output 空 → 可能被 gate（也可能配方不存在）
        // 嘗試以 CraftingManager 查原始配方判斷
        // 這裡簡化處理：遍歷所有 stage gated items 提示
        // 實際上 MixinCraftingManager 已清空 output，無法直接知道是哪個配方
        // 顯示通用提示
        blocked.add(I18n.format("adversity.gui.craft_may_require_stage"));
        return blocked;
    }
}
