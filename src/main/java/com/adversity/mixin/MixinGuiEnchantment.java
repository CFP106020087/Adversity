package com.adversity.mixin;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.gui.GatingHudRenderer;
import com.adversity.sanctuary.StageGatingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;

import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 附魔台 GUI — GS badge + 被封鎖附魔列表
 */
@Mixin(value = GuiEnchantment.class, remap = false)
public class MixinGuiEnchantment {

    @Inject(method = { "drawGuiContainerForegroundLayer", "func_146979_b" }, at = @At("TAIL"))
    private void adversity$renderGatingHud(int mouseX, int mouseY, CallbackInfo ci) {
        GuiContainer gui = (GuiContainer) (Object) this;
        List<String> blocked = adversity$getBlockedEnchantments();
        GatingHudRenderer.setBlockedTitle("adversity.gui.blocked_enchants");
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

    private List<String> adversity$getBlockedEnchantments() {
        List<String> blocked = new ArrayList<>();
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null)
            return blocked;

        IAdversityCapability.IProgression cap = player.getCapability(
                CapabilityHandler.PROGRESSION_CAPABILITY, null);

        for (Enchantment ench : ForgeRegistries.ENCHANTMENTS) {
            // 黑白名單系統
            if (StageGatingRegistry.isEnchantGatingEnabled()) {
                if (StageGatingRegistry.isEnchantmentGated(ench)) {
                    String stage = StageGatingRegistry.getEnchantGatingStage();
                    if (cap == null || !cap.hasStage(stage)) {
                        String name = I18n.format(ench.getName());
                        blocked.add(name + " §8(" + I18n.format("adversity.gui.requires") + " " + stage + ")");
                    }
                }
                continue; // 啟用時完全接管，不再查逐條
            }
            // 逐條系統
            String stage = StageGatingRegistry.getEnchantmentStageRequirement(ench);
            if (stage != null && (cap == null || !cap.hasStage(stage))) {
                String name = I18n.format(ench.getName());
                blocked.add(name + " §8(" + I18n.format("adversity.gui.requires") + " " + stage + ")");
            }
        }
        return blocked;
    }
}
