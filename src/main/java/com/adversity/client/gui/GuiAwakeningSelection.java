package com.adversity.client.gui;

import com.adversity.network.PacketAwakeningSelection;
import com.adversity.network.PacketHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 覺醒儀式效果選擇 GUI
 *
 * 顯示可選正面效果列表（本地化名稱），玩家選擇 N 個。
 * 選擇完成後發送 PacketAwakeningSelection 到服務端。
 */
@SideOnly(Side.CLIENT)
public class GuiAwakeningSelection extends GuiScreen {

    private final String[] potionIds;
    private final int selectCount;
    private final int durationTicks;

    private final Set<Integer> selectedIndices = new HashSet<>();
    private final List<String> localizedNames = new ArrayList<>();

    private GuiButton confirmButton;
    private int scrollOffset = 0;
    private static final int ENTRY_HEIGHT = 22;
    private static final int LIST_X = 30;
    private static final int LIST_WIDTH = 200;
    private int listTop;
    private int listBottom;
    private int visibleEntries;

    public GuiAwakeningSelection(String[] potionIds, int selectCount, int durationTicks) {
        this.potionIds = potionIds;
        this.selectCount = selectCount;
        this.durationTicks = durationTicks;
    }

    @Override
    public void initGui() {
        super.initGui();

        // 預先本地化所有藥水名稱
        localizedNames.clear();
        for (String id : potionIds) {
            Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(id));
            if (potion != null) {
                localizedNames.add(I18n.format(potion.getName()));
            } else {
                localizedNames.add(id);
            }
        }

        listTop = 40;
        listBottom = height - 50;
        visibleEntries = (listBottom - listTop) / ENTRY_HEIGHT;

        int durationSec = durationTicks / 20;
        String confirmText = I18n.format("adversity.gui.awakening.confirm",
                selectedIndices.size(), selectCount, durationSec);
        confirmButton = new GuiButton(0, width / 2 - 60, height - 40, 120, 20, confirmText);
        confirmButton.enabled = false;
        this.buttonList.add(confirmButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // 標題
        String title = I18n.format("adversity.gui.awakening.title");
        drawCenteredString(fontRenderer, title, width / 2, 15, 0xFFFFFF);

        String subtitle = I18n.format("adversity.gui.awakening.subtitle",
                selectCount, durationTicks / 20);
        drawCenteredString(fontRenderer, subtitle, width / 2, 27, 0xAAAAAA);

        // 繪製列表
        int listX = (width - LIST_WIDTH) / 2;

        for (int i = 0; i < visibleEntries && (i + scrollOffset) < potionIds.length; i++) {
            int idx = i + scrollOffset;
            int y = listTop + i * ENTRY_HEIGHT;

            boolean selected = selectedIndices.contains(idx);
            boolean hovered = mouseX >= listX && mouseX <= listX + LIST_WIDTH
                    && mouseY >= y && mouseY < y + ENTRY_HEIGHT;

            // 背景
            int bgColor = selected ? 0x6000AA00 : (hovered ? 0x40FFFFFF : 0x20FFFFFF);
            drawRect(listX, y, listX + LIST_WIDTH, y + ENTRY_HEIGHT - 2, bgColor);

            // 左邊選擇標記
            String marker = selected ? "\u2714 " : "  ";

            // 藥水名稱（本地化）
            String name = localizedNames.get(idx);
            int textColor = selected ? 0x55FF55 : 0xFFFFFF;
            fontRenderer.drawStringWithShadow(marker + name, listX + 5, y + 6, textColor);

            // 右邊 registry name（小字）
            String regName = potionIds[idx];
            int regColor = 0x888888;
            int regWidth = fontRenderer.getStringWidth(regName);
            if (regWidth < LIST_WIDTH - 80) {
                fontRenderer.drawString(regName, listX + LIST_WIDTH - regWidth - 5, y + 7, regColor);
            }
        }

        // 滾動條指示
        if (potionIds.length > visibleEntries) {
            int barHeight = (int)((float)visibleEntries / potionIds.length * (listBottom - listTop));
            int barY = listTop + (int)((float)scrollOffset / potionIds.length * (listBottom - listTop));
            drawRect(listX + LIST_WIDTH + 2, barY, listX + LIST_WIDTH + 5, barY + barHeight, 0x80FFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) return;

        int listX = (width - LIST_WIDTH) / 2;

        for (int i = 0; i < visibleEntries && (i + scrollOffset) < potionIds.length; i++) {
            int idx = i + scrollOffset;
            int y = listTop + i * ENTRY_HEIGHT;

            if (mouseX >= listX && mouseX <= listX + LIST_WIDTH && mouseY >= y && mouseY < y + ENTRY_HEIGHT) {
                if (selectedIndices.contains(idx)) {
                    selectedIndices.remove(idx);
                } else if (selectedIndices.size() < selectCount) {
                    selectedIndices.add(idx);
                }
                updateConfirmButton();
                break;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            scrollOffset -= Integer.signum(scroll);
            scrollOffset = Math.max(0, Math.min(scrollOffset, potionIds.length - visibleEntries));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0 && selectedIndices.size() == selectCount) {
            // 收集選中的藥水 ID
            String[] selected = new String[selectCount];
            int i = 0;
            for (int idx : selectedIndices) {
                selected[i++] = potionIds[idx];
            }

            // 發送到服務端
            PacketHandler.INSTANCE.sendToServer(
                    new PacketAwakeningSelection(selected, durationTicks));

            mc.displayGuiScreen(null);
        }
    }

    private void updateConfirmButton() {
        confirmButton.enabled = (selectedIndices.size() == selectCount);
        int durationSec = durationTicks / 20;
        confirmButton.displayString = I18n.format("adversity.gui.awakening.confirm",
                selectedIndices.size(), selectCount, durationSec);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
