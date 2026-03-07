package com.adversity.client.gui;

import com.adversity.network.PacketHandler;
import com.adversity.network.PacketSaveEnchantGating;
import com.adversity.sanctuary.StageGatingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.*;

/**
 * 附魔門控配置 GUI
 *
 * 功能：
 * - 列出所有附魔（可滾動）
 * - 每個附魔旁有勾選框（toggle）
 * - 頂部：黑名單/白名單切換 + 階段輸入
 * - 底部：保存按鈕
 */
@SideOnly(Side.CLIENT)
public class GuiEnchantGatingTool extends GuiScreen {

    // 附魔列表
    private List<EnchantEntry> entries = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 14;
    private static final int ROW_HEIGHT = 14;

    // 配置
    private boolean useBlacklist = true;
    private String stage = "scholar";

    // UI 元素
    private GuiButton btnMode;
    private GuiButton btnSave;
    private GuiButton btnSelectAll;
    private GuiButton btnSelectNone;
    private GuiTextField txtStage;

    // 面板位置
    private int panelX, panelY, panelW, panelH;

    public static void open() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiEnchantGatingTool());
    }

    @Override
    public void initGui() {
        // 載入現有配置
        useBlacklist = StageGatingRegistry.isEnchantGatingBlacklist();
        stage = StageGatingRegistry.getEnchantGatingStage();

        // 構建附魔列表
        entries.clear();
        Set<ResourceLocation> currentList = StageGatingRegistry.getEnchantGatingList();
        for (Enchantment ench : ForgeRegistries.ENCHANTMENTS) {
            if (ench.getRegistryName() == null) continue;
            boolean selected = currentList.contains(ench.getRegistryName());
            entries.add(new EnchantEntry(ench, selected));
        }
        // 按名稱排序
        entries.sort(Comparator.comparing(e -> I18n.format(e.enchantment.getName())));

        // 面板大小
        panelW = 300;
        panelH = 50 + VISIBLE_ROWS * ROW_HEIGHT + 40;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        // 按鈕
        int btnY = panelY + 4;
        btnMode = new GuiButton(0, panelX + 4, btnY, 100, 18,
                useBlacklist ? "§c黑名單模式" : "§a白名單模式");
        btnSave = new GuiButton(1, panelX + panelW - 74, panelY + panelH - 24, 70, 20,
                "§a💾 保存");
        btnSelectAll = new GuiButton(2, panelX + panelW - 150, panelY + panelH - 24, 35, 20, "§b全選");
        btnSelectNone = new GuiButton(3, panelX + panelW - 112, panelY + panelH - 24, 35, 20, "§7清除");

        buttonList.add(btnMode);
        buttonList.add(btnSave);
        buttonList.add(btnSelectAll);
        buttonList.add(btnSelectNone);

        // 階段文本框
        txtStage = new GuiTextField(10, fontRenderer, panelX + 110, btnY + 1, 80, 16);
        txtStage.setText(stage);
        txtStage.setMaxStringLength(32);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // 面板背景
        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, 0xDD000000);
        // 邊框
        drawRect(panelX, panelY, panelX + panelW, panelY + 1, 0xFF666666);
        drawRect(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF666666);
        drawRect(panelX, panelY, panelX + 1, panelY + panelH, 0xFF666666);
        drawRect(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF666666);

        // 標題
        fontRenderer.drawStringWithShadow("§l附魔門控配置工具",
                panelX + panelW / 2 - fontRenderer.getStringWidth("附魔門控配置工具") / 2, panelY + 6, 0xFFFFAA);

        // 階段標籤
        fontRenderer.drawStringWithShadow("§7階段:", panelX + 110, panelY + 26, 0xFFFFFF);
        txtStage.drawTextBox();

        // 模式說明
        String modeDesc = useBlacklist ? "§8(列表內被gate)" : "§8(列表內放行)";
        fontRenderer.drawStringWithShadow(modeDesc, panelX + 4, panelY + 26, 0xFFFFFF);

        // 附魔列表區域
        int listY = panelY + 46;
        int listH = VISIBLE_ROWS * ROW_HEIGHT;

        // 列表邊框
        drawRect(panelX + 4, listY - 1, panelX + panelW - 4, listY + listH + 1, 0xFF333333);
        drawRect(panelX + 5, listY, panelX + panelW - 5, listY + listH, 0xFF111111);

        // 渲染可見條目
        for (int i = 0; i < VISIBLE_ROWS && i + scrollOffset < entries.size(); i++) {
            EnchantEntry entry = entries.get(i + scrollOffset);
            int y = listY + i * ROW_HEIGHT;

            // 交替背景
            if (i % 2 == 0) {
                drawRect(panelX + 5, y, panelX + panelW - 5, y + ROW_HEIGHT, 0x20FFFFFF);
            }

            // 勾選框
            String check = entry.selected ? "§a☑" : "§7☐";
            fontRenderer.drawStringWithShadow(check, panelX + 8, y + 3, 0xFFFFFF);

            // 附魔名
            String name = I18n.format(entry.enchantment.getName());
            fontRenderer.drawStringWithShadow(entry.selected ? "§f" + name : "§8" + name,
                    panelX + 22, y + 3, 0xFFFFFF);

            // 註冊名
            String regName = entry.enchantment.getRegistryName().toString();
            int regX = panelX + panelW - 8 - fontRenderer.getStringWidth(regName);
            fontRenderer.drawStringWithShadow("§8" + regName, regX, y + 3, 0xFFFFFF);
        }

        // 滾動提示
        if (entries.size() > VISIBLE_ROWS) {
            int total = entries.size();
            int shown = Math.min(VISIBLE_ROWS, total - scrollOffset);
            String scrollInfo = "§8" + (scrollOffset + 1) + "-" + (scrollOffset + shown) + " / " + total;
            fontRenderer.drawStringWithShadow(scrollInfo, panelX + 8, panelY + panelH - 20, 0xFFFFFF);
        }

        // 選中計數
        long selectedCount = entries.stream().filter(e -> e.selected).count();
        fontRenderer.drawStringWithShadow("§e已選: " + selectedCount, panelX + 8, panelY + panelH - 10, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            // 切換模式
            useBlacklist = !useBlacklist;
            btnMode.displayString = useBlacklist ? "§c黑名單模式" : "§a白名單模式";
        } else if (button.id == 1) {
            // 保存
            stage = txtStage.getText().trim();
            if (stage.isEmpty()) stage = "scholar";

            List<String> selected = new ArrayList<>();
            for (EnchantEntry entry : entries) {
                if (entry.selected) {
                    selected.add(entry.enchantment.getRegistryName().toString());
                }
            }

            PacketHandler.INSTANCE.sendToServer(
                    new PacketSaveEnchantGating(stage, useBlacklist, selected.toArray(new String[0])));

            // UI 回饋
            Minecraft.getMinecraft().player.sendChatMessage(
                    "§a[Adversity] 附魔門控已保存: " + selected.size() + " 條目, "
                    + (useBlacklist ? "黑名單" : "白名單") + ", 階段=" + stage);
            mc.displayGuiScreen(null);
        } else if (button.id == 2) {
            // 全選
            for (EnchantEntry e : entries) e.selected = true;
        } else if (button.id == 3) {
            // 清除
            for (EnchantEntry e : entries) e.selected = false;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        txtStage.mouseClicked(mouseX, mouseY, mouseButton);

        // 點擊附魔列表 toggle
        int listY = panelY + 46;
        if (mouseX >= panelX + 5 && mouseX <= panelX + panelW - 5
                && mouseY >= listY && mouseY < listY + VISIBLE_ROWS * ROW_HEIGHT) {
            int row = (mouseY - listY) / ROW_HEIGHT;
            int idx = row + scrollOffset;
            if (idx >= 0 && idx < entries.size()) {
                entries.get(idx).selected = !entries.get(idx).selected;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        txtStage.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == 1) { // ESC
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = org.lwjgl.input.Mouse.getEventDWheel();
        if (scroll != 0) {
            if (scroll > 0) scrollOffset = Math.max(0, scrollOffset - 3);
            else scrollOffset = Math.min(Math.max(0, entries.size() - VISIBLE_ROWS), scrollOffset + 3);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static class EnchantEntry {
        final Enchantment enchantment;
        boolean selected;

        EnchantEntry(Enchantment enchantment, boolean selected) {
            this.enchantment = enchantment;
            this.selected = selected;
        }
    }
}
