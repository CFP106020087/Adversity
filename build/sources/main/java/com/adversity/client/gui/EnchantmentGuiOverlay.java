package com.adversity.client.gui;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.progression.ProgressionEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 附魔台 / 鐵砧 GUI 左上角 GS HUD 繪製
 *
 * 在附魔台和鐵砧 GUI 的左上角顯示當前玩家的 Game Stage。
 * 色碼與圖標與 DifficultyHUD 保持一致。
 */
@SideOnly(Side.CLIENT)
public class EnchantmentGuiOverlay {

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiEnchantment)
                && !(event.getGui() instanceof GuiRepair)) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null)
            return;

        IAdversityCapability.IProgression cap = player.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
        int tier = ProgressionEventHandler.getHighestTier(player);

        String stageText;
        switch (tier) {
            case 4:
                stageText = "§c✦ " + I18n.format("adversity.stage.champion");
                break;
            case 3:
                stageText = "§6✦ " + I18n.format("adversity.stage.warden");
                break;
            case 2:
                stageText = "§e✦ " + I18n.format("adversity.stage.scholar");
                break;
            case 1:
                stageText = "§a✦ " + I18n.format("adversity.stage.awakened");
                break;
            default:
                stageText = "§8✦ " + I18n.format("adversity.stage.none");
                break;
        }

        // 構建自定義階段行（獨立行，顯示在下方）
        String customLine = "";
        if (cap != null) {
            StringBuilder custom = new StringBuilder();
            for (String stage : cap.getStages()) {
                if (!"awakened".equals(stage) && !"scholar".equals(stage)
                        && !"warden".equals(stage) && !"champion".equals(stage)) {
                    if (custom.length() > 0)
                        custom.append("§7, ");
                    custom.append("§b✦ §b").append(stage);
                }
            }
            if (custom.length() > 0) {
                customLine = custom.toString();
            }
        }

        String prefix = "§7" + I18n.format("adversity.hud.stage") + " ";
        String display = prefix + stageText;

        FontRenderer fr = mc.fontRenderer;
        // 繪製在 GUI 左上角 — 第一行: 預定義階段
        fr.drawStringWithShadow(display, 4, 4, 0xFFFFFF);
        // 第二行: 自定義階段（如有）
        if (!customLine.isEmpty()) {
            fr.drawStringWithShadow(customLine, 4, 14, 0xFFFFFF);
        }
    }
}
