package com.adversity.client.gui;

import com.adversity.network.PacketHandler;
import com.adversity.network.PacketSanctuaryAction;
import com.adversity.network.PacketSwitchSanctuaryGui;
import com.adversity.sanctuary.TileEntitySanctuary;
import com.adversity.sanctuary.inventory.ContainerSanctuary;
import com.adversity.sanctuary.inventory.SlotRitualInput;
import com.adversity.sanctuary.ritual.Rite;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

/**
 * 圣所仪式GUI - 動態繪製同心圓輸入槽位
 */
public class GuiSanctuaryAltar extends GuiContainer {

    private static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation(
            "textures/gui/container/generic_54.png");

    private final TileEntitySanctuary te;
    private final ContainerSanctuary container;

    private GuiButton switchButton;
    private GuiButton ritualButton;

    public GuiSanctuaryAltar(InventoryPlayer playerInv, TileEntitySanctuary te) {
        super(new ContainerSanctuary(playerInv, te));
        this.te = te;
        this.container = (ContainerSanctuary) this.inventorySlots;
        this.xSize = 176;
        this.ySize = 230;
    }

    @Override
    public void initGui() {
        super.initGui();

        // 右上角切換按鈕
        this.buttonList.add(switchButton = new GuiButton(4, this.guiLeft + xSize - 22, this.guiTop + 4,
                18, 14, "⇆"));

        // 執行儀式按鈕 — 在儀式區域下方
        this.buttonList.add(ritualButton = new GuiButton(3,
                this.guiLeft + 55, this.guiTop + 130,
                66, 14,
                I18n.format("adversity.gui.ritual")));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        // 匹配儀式
        Rite matchingRite = te.getMatchingRite();
        boolean canPerform = te.isActivated()
                && matchingRite != null
                && te.getFuel() >= matchingRite.getEntropyCost()
                && te.getStackInSlot(TileEntitySanctuary.SLOT_OUTPUT).isEmpty();
        ritualButton.enabled = canPerform;
        ritualButton.visible = te.isActivated();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 3:
                PacketHandler.INSTANCE.sendToServer(new PacketSanctuaryAction(te.getPos(),
                        PacketSanctuaryAction.Action.PERFORM_RITUAL));
                break;
            case 4:
                PacketHandler.INSTANCE.sendToServer(new PacketSwitchSanctuaryGui(te.getPos(),
                        AdversityGuiHandler.GUI_SANCTUARY_CONTROL));
                break;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int guiX = this.guiLeft;
        int guiY = this.guiTop;

        // ========== 主背景 ==========
        drawRect(guiX, guiY, guiX + xSize, guiY + ySize, 0xFFC6C6C6);

        // 3D 邊框
        drawHorizontalLine(guiX, guiX + xSize - 1, guiY, 0xFFFFFFFF);
        drawVerticalLine(guiX, guiY, guiY + ySize - 1, 0xFFFFFFFF);
        drawHorizontalLine(guiX, guiX + xSize - 1, guiY + ySize - 1, 0xFF555555);
        drawVerticalLine(guiX + xSize - 1, guiY, guiY + ySize - 1, 0xFF555555);

        // ========== 左側燃料區域 ==========
        int fuelAreaX = guiX + 7;
        int fuelAreaY = guiY + 17;

        // 燃料條背景
        drawRect(fuelAreaX, fuelAreaY, fuelAreaX + 18, fuelAreaY + 52, 0xFF555555);
        drawRect(fuelAreaX + 1, fuelAreaY + 1, fuelAreaX + 17, fuelAreaY + 51, 0xFF373737);

        // 燃料條填充
        if (te.getMaxFuel() > 0) {
            float fuelPct = te.getFuelPercentage();
            int barHeight = (int) (fuelPct * 48);
            int barY = fuelAreaY + 2 + (48 - barHeight);

            for (int h = 0; h < barHeight; h++) {
                float ratio = (float) h / 48.0f;
                int r = (int) (0 * (1 - ratio) + 100 * ratio);
                int g = (int) (102 * (1 - ratio) + 200 * ratio);
                int b = (int) (204 * (1 - ratio) + 255 * ratio);
                int color = 0xFF000000 | (r << 16) | (g << 8) | b;
                drawHorizontalLine(fuelAreaX + 2, fuelAreaX + 15, barY + barHeight - h - 1, color);
            }
        }

        // 燃料槽背景
        drawSlotBackground(fuelAreaX, fuelAreaY + 54);

        // ========== 中間儀式區域 ==========
        int ritualAreaX = guiX + 28;
        int ritualAreaY = guiY + 17;

        // 儀式區域背景（加大以容納同心圓）
        drawRect(ritualAreaX, ritualAreaY, ritualAreaX + 140, ritualAreaY + 110, 0xFF555555);
        drawRect(ritualAreaX + 1, ritualAreaY + 1, ritualAreaX + 139, ritualAreaY + 109, 0xFF2D2D2D);

        // ========== 動態繪製所有活躍槽位 ==========
        int activeSlots = container.getActiveInputSlots();

        if (activeSlots > 0) {
            // 輸出槽位（中心位置）
            Slot outputSlot = this.inventorySlots.getSlot(1);
            if (outputSlot.xPos >= 0) {
                drawSlotBackground(guiX + outputSlot.xPos - 1, guiY + outputSlot.yPos - 1);
            }

            // 輸入槽位（動態位置）
            for (int i = 0; i < activeSlots; i++) {
                Slot slot = this.inventorySlots.getSlot(2 + i);
                if (slot instanceof SlotRitualInput) {
                    SlotRitualInput ritualSlot = (SlotRitualInput) slot;
                    if (ritualSlot.isActive() && slot.xPos >= 0) {
                        drawSlotBackground(guiX + slot.xPos - 1, guiY + slot.yPos - 1);
                    }
                }
            }

            // 繪製連接線（從每個 input 到中心 output）
            if (outputSlot.xPos >= 0) {
                int cx = guiX + outputSlot.xPos + 8;
                int cy = guiY + outputSlot.yPos + 8;
                for (int i = 0; i < activeSlots; i++) {
                    Slot slot = this.inventorySlots.getSlot(2 + i);
                    if (slot instanceof SlotRitualInput && ((SlotRitualInput) slot).isActive() && slot.xPos >= 0) {
                        int sx = guiX + slot.xPos + 8;
                        int sy = guiY + slot.yPos + 8;
                        drawDottedLine(sx, sy, cx, cy, 0x60FFFFFF);
                    }
                }
            }
        } else {
            // 空：顯示提示
            String noRituals = I18n.format("adversity.gui.no_rituals");
            this.fontRenderer.drawString(noRituals,
                    ritualAreaX + 70 - fontRenderer.getStringWidth(noRituals) / 2,
                    ritualAreaY + 22, 0x666666);
        }

        // ========== 繪製玩家背包（下移後的位置） ==========
        this.mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
        this.drawTexturedModalRect(guiX + 7, guiY + 148, 7, 17, 162, 54);
        this.drawTexturedModalRect(guiX + 7, guiY + 206, 7, 17, 162, 18);
    }

    /**
     * 繪製虛線連接
     */
    private void drawDottedLine(int x1, int y1, int x2, int y2, int color) {
        double dist = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
        if (dist < 2)
            return;
        int steps = (int) (dist / 4);
        for (int i = 0; i < steps; i += 2) {
            float t1 = i / (float) steps;
            float t2 = Math.min((i + 1) / (float) steps, 1.0f);
            int px1 = (int) (x1 + (x2 - x1) * t1);
            int py1 = (int) (y1 + (y2 - y1) * t1);
            int px2 = (int) (x1 + (x2 - x1) * t2);
            int py2 = (int) (y1 + (y2 - y1) * t2);
            drawRect(Math.min(px1, px2), Math.min(py1, py2),
                    Math.max(px1, px2) + 1, Math.max(py1, py2) + 1, color);
        }
    }

    private void drawSlotBackground(int x, int y) {
        drawRect(x, y, x + 18, y + 18, 0xFF555555);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        drawHorizontalLine(x + 1, x + 16, y + 1, 0xFF373737);
        drawVerticalLine(x + 1, y + 1, y + 16, 0xFF373737);
        drawHorizontalLine(x + 1, x + 16, y + 16, 0xFFFFFFFF);
        drawVerticalLine(x + 16, y + 1, y + 16, 0xFFFFFFFF);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 標題
        String title = I18n.format("adversity.gui.sanctuary_ritual");
        this.fontRenderer.drawString(title, 8, 6, 0x404040);

        // 儀式區域標籤
        this.fontRenderer.drawString(I18n.format("adversity.gui.ritual_area"), 30, 20, 0xAAAAAA);

        // 顯示匹配儀式信息
        Rite matchingRite = te.getMatchingRite();
        if (matchingRite != null) {
            String cost = I18n.format("adversity.gui.cost") + ": " + matchingRite.getEntropyCost();
            int costColor = te.getFuel() >= matchingRite.getEntropyCost() ? 0x55FF55 : 0xFF5555;
            this.fontRenderer.drawString(cost, 80, 58, costColor);
        }

        // 燃料 tooltip
        int fuelLocalX = 7;
        int fuelLocalY = 17;
        int relMouseX = mouseX - this.guiLeft;
        int relMouseY = mouseY - this.guiTop;
        if (relMouseX >= fuelLocalX && relMouseX <= fuelLocalX + 18 &&
                relMouseY >= fuelLocalY && relMouseY <= fuelLocalY + 52) {
            java.util.List<String> tooltip = new java.util.ArrayList<>();
            tooltip.add("\u00A76\u29C9 \u71B5\u80FD\u71C3\u6599");
            tooltip.add("\u00A77" + te.getFuel() + " / " + te.getMaxFuel());
            if (te.getMaxFuel() > 0) {
                int pct = (int) (te.getFuelPercentage() * 100);
                tooltip.add("\u00A7" + (pct > 50 ? "a" : pct > 20 ? "e" : "c") + pct + "%");
            }
            this.drawHoveringText(tooltip, relMouseX, relMouseY);
        }

        // 玩家背包標籤
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 0x404040);
    }
}
