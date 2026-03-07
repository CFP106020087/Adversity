package com.adversity.client.gui;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.progression.ProgressionEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * 統一 GS 門控 HUD 渲染器
 *
 * 在工作台、附魔台、鐵砧、玩家背包 GUI 的右上角繪製：
 * - GS badge（當前階段圖標 + 名稱）
 * - ❌ 按鈕（有被 gate 的項目時）
 * - 可展開 detail panel（點擊 ❌ 後顯示被封鎖的項目清單）
 */
@SideOnly(Side.CLIENT)
public class GatingHudRenderer {

    // Detail panel 展開狀態
    private static boolean detailOpen = false;

    // 被 gate 的項目列表（由各 Mixin 設定）
    private static final List<String> blockedItems = new ArrayList<>();

    // Detail panel 標題（由各 Mixin 設定，不同 GUI 類型不同）
    private static String blockedTitle = "";

    // ❌ 按鈕區域（相對 GUI 座標）
    private static int badgeX, badgeY, badgeW, badgeH;

    /**
     * 設定當前被封鎖的項目（由 Mixin 在 foreground 繪製前調用）
     */
    public static void setBlockedItems(List<String> items) {
        blockedItems.clear();
        if (items != null) {
            blockedItems.addAll(items);
        }
    }

    /**
     * 設定 detail panel 標題（各 GUI 類型不同）
     */
    public static void setBlockedTitle(String titleKey) {
        blockedTitle = I18n.format(titleKey);
    }

    /**
     * 在 GUI foreground layer 繪製 GS badge + ❌
     *
     * @param xSize GUI 寬度（用於定位右上角）
     */
    public static void renderGsHud(FontRenderer fr, int xSize) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null)
            return;

        // 確保 GL 狀態正確（部分 GUI 在 foregroundLayer 尾部修改了狀態）
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int tier = ProgressionEventHandler.getHighestTier(player);

        // 選擇顏色和圖標
        String stageText;
        int color;
        switch (tier) {
            case 4:
                stageText = "§c✦ " + I18n.format("adversity.stage.champion");
                color = 0xFF5555;
                break;
            case 3:
                stageText = "§6✦ " + I18n.format("adversity.stage.warden");
                color = 0xFFAA00;
                break;
            case 2:
                stageText = "§e✦ " + I18n.format("adversity.stage.scholar");
                color = 0xFFFF55;
                break;
            case 1:
                stageText = "§a✦ " + I18n.format("adversity.stage.awakened");
                color = 0x55FF55;
                break;
            default:
                stageText = "§8✦ " + I18n.format("adversity.stage.none");
                color = 0x555555;
                break;
        }

        // 自定義階段
        IAdversityCapability.IProgression cap = player.getCapability(
                CapabilityHandler.PROGRESSION_CAPABILITY, null);
        String customLine = "";
        if (cap != null) {
            StringBuilder custom = new StringBuilder();
            for (String stage : cap.getStages()) {
                if (!"awakened".equals(stage) && !"scholar".equals(stage)
                        && !"warden".equals(stage) && !"champion".equals(stage)) {
                    if (custom.length() > 0)
                        custom.append("§7, ");
                    custom.append("§b").append(stage);
                }
            }
            if (custom.length() > 0) {
                customLine = custom.toString();
            }
        }

        // --- 繪製 GS badge（GUI 上方）---
        int textWidth = fr.getStringWidth(stageText.replaceAll("§.", ""));
        badgeX = xSize - textWidth - 20;
        badgeY = -(customLine.isEmpty() ? 14 : 24);
        badgeW = textWidth + 16;
        badgeH = customLine.isEmpty() ? 12 : 22;

        // badge 背景
        drawRectAlpha(badgeX - 2, badgeY - 2, badgeX + badgeW, badgeY + badgeH, 0x80000000);
        fr.drawStringWithShadow(stageText, badgeX, badgeY, 0xFFFFFF);

        if (!customLine.isEmpty()) {
            fr.drawStringWithShadow(customLine, badgeX, badgeY + 10, 0xFFFFFF);
        }

        // --- ❌ 按鈕（有 blocked items 時）---
        if (!blockedItems.isEmpty()) {
            String xMark = "§c❌";
            String countStr = "§c" + blockedItems.size();
            int countW = fr.getStringWidth(String.valueOf(blockedItems.size()));
            int xMarkX = badgeX - 16;
            int countX = xMarkX - countW - 2;

            // 深色背景確保在淺色 GUI 上可見
            drawRectAlpha(countX - 2, badgeY - 2, xMarkX + 12, badgeY + 12, 0xCC000000);
            fr.drawStringWithShadow(xMark, xMarkX, badgeY, 0xFFFFFF);
            fr.drawStringWithShadow(countStr, countX, badgeY, 0xFFFFFF);
        }

        // --- Detail panel（展開時）---
        if (detailOpen && !blockedItems.isEmpty()) {
            renderDetailPanel(fr, xSize);
        }
    }

    /**
     * 繪製被封鎖項目的詳情面板（在 GUI 右側外部渲染，避免覆蓋 GUI 內容）
     */
    private static void renderDetailPanel(FontRenderer fr, int xSize) {
        int lineHeight = 10;
        int maxVisible = 16; // 最多顯示 16 行，防止超出螢幕
        int visibleCount = Math.min(blockedItems.size(), maxVisible);
        int panelW = 160;
        int panelH = 14 + visibleCount * lineHeight + 4;

        // 面板位置：GUI 左側外部
        int panelX = -panelW - 4;
        int panelY = -2;

        // 面板背景
        drawRectAlpha(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC000000);

        // 邊框
        drawRectAlpha(panelX, panelY, panelX + panelW, panelY + 1, 0xFF555555);
        drawRectAlpha(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF555555);
        drawRectAlpha(panelX, panelY, panelX + 1, panelY + panelH, 0xFF555555);
        drawRectAlpha(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF555555);

        // 標題
        fr.drawStringWithShadow("§c§l" + blockedTitle, panelX + 4, panelY + 3, 0xFFFFFF);

        // 項目列表
        int y = panelY + 14;
        for (int i = 0; i < visibleCount; i++) {
            fr.drawStringWithShadow("§c✖ §7" + blockedItems.get(i), panelX + 4, y, 0xFFFFFF);
            y += lineHeight;
        }

        // 省略提示
        if (blockedItems.size() > maxVisible) {
            fr.drawStringWithShadow("§8... +" + (blockedItems.size() - maxVisible), panelX + 4, y, 0xFFFFFF);
        }
    }

    /**
     * 處理滑鼠點擊（toggle detail panel）
     *
     * @param mouseX 相對 GUI 左上角的 X
     * @param mouseY 相對 GUI 左上角的 Y
     * @return true 如果點擊被消費
     */
    public static boolean handleClick(int mouseX, int mouseY) {
        if (blockedItems.isEmpty()) {
            detailOpen = false;
            return false;
        }

        // 點擊 ❌ 區域
        int xMarkX = badgeX - 14;
        if (mouseX >= xMarkX - 4 && mouseX <= badgeX + badgeW
                && mouseY >= badgeY - 2 && mouseY <= badgeY + badgeH) {
            detailOpen = !detailOpen;
            return true;
        }

        // 點擊 detail panel 外部 → 關閉
        if (detailOpen) {
            detailOpen = false;
        }

        return false;
    }

    /**
     * GUI 關閉時重置狀態
     */
    public static void reset() {
        detailOpen = false;
        blockedItems.clear();
    }

    /**
     * 帶 alpha 的矩形繪製
     */
    private static void drawRectAlpha(int left, int top, int right, int bottom, int color) {
        net.minecraft.client.gui.Gui.drawRect(left, top, right, bottom, color);
    }
}
