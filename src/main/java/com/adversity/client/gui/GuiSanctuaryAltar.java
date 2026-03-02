package com.adversity.client.gui;

import com.adversity.network.PacketHandler;
import com.adversity.network.PacketSanctuaryAction;
import com.adversity.network.PacketSwitchSanctuaryGui;
import com.adversity.sanctuary.TileEntitySanctuary;
import com.adversity.sanctuary.inventory.ContainerSanctuary;
import com.adversity.sanctuary.ritual.Rite;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

/**
 * 圣所仪式GUI - 专注于仪式功能
 * 控制面板功能移至 GuiSanctuaryControl
 */
public class GuiSanctuaryAltar extends GuiContainer {

    private static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation(
            "textures/gui/container/generic_54.png");

    private final TileEntitySanctuary te;

    private GuiButton switchButton; // 切换到控制面板
    private GuiButton ritualButton; // 执行仪式

    public GuiSanctuaryAltar(InventoryPlayer playerInv, TileEntitySanctuary te) {
        super(new ContainerSanctuary(playerInv, te));
        this.te = te;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();

        // 右上角切换按钮 -> 切换到控制面板
        this.buttonList.add(switchButton = new GuiButton(4, this.guiLeft + xSize - 22, this.guiTop + 4,
                18, 14, "⇆"));

        // 执行仪式按钮
        this.buttonList.add(ritualButton = new GuiButton(3, this.guiLeft + 80, this.guiTop + 52, 50, 14,
                I18n.format("adversity.gui.ritual")));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        // 仪式按钮状态
        Rite matchingRite = te.getMatchingRite();
        ritualButton.enabled = te.isActivated() && matchingRite != null
                && te.getFuel() >= matchingRite.getEntropyCost()
                && te.getStackInSlot(2).isEmpty();
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
                // 切换到控制面板GUI
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

        // ========== 绘制主背景 ==========
        drawRect(guiX, guiY, guiX + xSize, guiY + ySize, 0xFFC6C6C6);

        // 3D边框
        drawHorizontalLine(guiX, guiX + xSize - 1, guiY, 0xFFFFFFFF);
        drawVerticalLine(guiX, guiY, guiY + ySize - 1, 0xFFFFFFFF);
        drawHorizontalLine(guiX, guiX + xSize - 1, guiY + ySize - 1, 0xFF555555);
        drawVerticalLine(guiX + xSize - 1, guiY, guiY + ySize - 1, 0xFF555555);

        // ========== 左侧燃料区域 ==========
        int fuelAreaX = guiX + 7;
        int fuelAreaY = guiY + 17;

        // 燃料条背景
        drawRect(fuelAreaX, fuelAreaY, fuelAreaX + 18, fuelAreaY + 52, 0xFF555555);
        drawRect(fuelAreaX + 1, fuelAreaY + 1, fuelAreaX + 17, fuelAreaY + 51, 0xFF373737);

        // 燃料条填充
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

        // 燃料槽位
        drawSlotBackground(fuelAreaX, fuelAreaY + 54);

        // ========== 中间仪式区域 ==========
        int ritualAreaX = guiX + 28;
        int ritualAreaY = guiY + 17;

        // 仪式区域背景
        drawRect(ritualAreaX, ritualAreaY, ritualAreaX + 140, ritualAreaY + 50, 0xFF555555);
        drawRect(ritualAreaX + 1, ritualAreaY + 1, ritualAreaX + 139, ritualAreaY + 49, 0xFF2D2D2D);

        // 仪式输入槽位 (slot 1)
        drawSlotBackground(guiX + 45, guiY + 34);

        // 箭头区域
        int arrowX = guiX + 70;
        int arrowY = guiY + 38;
        for (int i = 0; i < 16; i++) {
            drawRect(arrowX + i, arrowY + 2, arrowX + 1 + i, arrowY + 4, 0xFFFFFFFF);
        }
        // 箭头尖
        drawRect(arrowX + 14, arrowY + 1, arrowX + 15, arrowY + 5, 0xFFFFFFFF);
        drawRect(arrowX + 15, arrowY + 2, arrowX + 16, arrowY + 4, 0xFFFFFFFF);

        // 仪式输出槽位 (slot 2)
        drawSlotBackground(guiX + 95, guiY + 34);

        // ========== 绘制玩家背包 ==========
        this.mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
        this.drawTexturedModalRect(guiX + 7, guiY + 84, 7, 17, 162, 54);
        this.drawTexturedModalRect(guiX + 7, guiY + 142, 7, 17, 162, 18);
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
        // 标题
        String title = I18n.format("adversity.gui.sanctuary_ritual");
        this.fontRenderer.drawString(title, 8, 6, 0x404040);

        // 仪式区域标签
        this.fontRenderer.drawString(I18n.format("adversity.gui.ritual_area"), 30, 20, 0xAAAAAA);

        // 显示匹配的仪式信息
        Rite matchingRite = te.getMatchingRite();
        if (matchingRite != null) {
            String cost = I18n.format("adversity.gui.cost") + ": " + matchingRite.getEntropyCost();
            int costColor = te.getFuel() >= matchingRite.getEntropyCost() ? 0x55FF55 : 0xFF5555;
            this.fontRenderer.drawString(cost, 80, 68, costColor);
        }

        // 燃料条上方显示燃料值 (悬停时显示tooltip)
        int fuelLocalX = 7;
        int fuelLocalY = 17;
        int relMouseX = mouseX - this.guiLeft;
        int relMouseY = mouseY - this.guiTop;
        if (relMouseX >= fuelLocalX && relMouseX <= fuelLocalX + 18 &&
                relMouseY >= fuelLocalY && relMouseY <= fuelLocalY + 52) {
            java.util.List<String> tooltip = new java.util.ArrayList<>();
            tooltip.add("\u00A76\u29C9 \u71B5\u80FD\u71C3\u6599"); // §6⧉ 熵能燃料
            tooltip.add("\u00A77" + te.getFuel() + " / " + te.getMaxFuel());
            if (te.getMaxFuel() > 0) {
                int pct = (int) (te.getFuelPercentage() * 100);
                tooltip.add("\u00A7" + (pct > 50 ? "a" : pct > 20 ? "e" : "c") + pct + "%");
            }
            this.drawHoveringText(tooltip, relMouseX, relMouseY);
        }

        // 玩家背包标签
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 0x404040);
    }
}
