package com.adversity.mixin;

import com.adversity.client.gui.GatingHudRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家背包 2×2 合成區 — GS badge + ❌ 提示
 */
@Mixin(value = GuiInventory.class, remap = false)
public class MixinGuiInventory {

    @Inject(method = { "drawGuiContainerForegroundLayer", "func_146979_b" }, at = @At("TAIL"))
    private void adversity$renderGatingHud(int mouseX, int mouseY, CallbackInfo ci) {
        GuiContainer gui = (GuiContainer) (Object) this;
        List<String> blocked = adversity$getBlockedCraftingItems(gui);
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

    private List<String> adversity$getBlockedCraftingItems(GuiContainer gui) {
        List<String> blocked = new ArrayList<>();
        // 2×2 output = slot 0, inputs = slot 1-4
        Slot outputSlot = gui.inventorySlots.getSlot(0);
        if (outputSlot.getHasStack())
            return blocked;

        boolean hasInput = false;
        for (int i = 1; i <= 4; i++) {
            if (gui.inventorySlots.getSlot(i).getHasStack()) {
                hasInput = true;
                break;
            }
        }
        if (!hasInput)
            return blocked;

        blocked.add(I18n.format("adversity.gui.craft_may_require_stage"));
        return blocked;
    }
}
