package com.adversity.client.gui;

import com.adversity.network.PacketHandler;
import com.adversity.network.PacketSanctuaryAction;
import com.adversity.network.PacketSwitchSanctuaryGui;
import com.adversity.sanctuary.TileEntitySanctuary;
import com.adversity.sanctuary.inventory.ContainerSanctuaryControl;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

/**
 * 圣所控制面板GUI - 包含模式切换、升级、激活功能
 * 与仪式GUI分开，通过切换按钮互相跳转
 */
public class GuiSanctuaryControl extends GuiContainer {

    private static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation(
            "textures/gui/container/generic_54.png");

    private final TileEntitySanctuary te;

    private GuiButton switchButton;  // 切换到仪式GUI
    private GuiButton modeButton;
    private GuiButton upgradeButton;
    private GuiButton activateButton;

    public GuiSanctuaryControl(InventoryPlayer playerInv, TileEntitySanctuary te) {
        super(new ContainerSanctuaryControl(playerInv, te));
        this.te = te;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();

        // 右上角切换按钮 -> 切换到仪式GUI
        this.buttonList.add(switchButton = new GuiButton(4, this.guiLeft + xSize - 22, this.guiTop + 4, 
                18, 14, "⇆"));

        // 控制按钮区
        int buttonX = this.guiLeft + 30;
        int buttonY = this.guiTop + 30;
        int buttonWidth = 116;
        int buttonHeight = 20;

        // 模式切换按钮
        this.buttonList.add(modeButton = new GuiButton(0, buttonX, buttonY, buttonWidth, buttonHeight, getModeName()));

        // 升级按钮
        this.buttonList.add(upgradeButton = new GuiButton(1, buttonX, buttonY + 24, buttonWidth, buttonHeight,
                I18n.format("adversity.gui.upgrade")));

        // 激活按钮
        this.buttonList.add(activateButton = new GuiButton(2, buttonX, buttonY + 48, buttonWidth, buttonHeight,
                I18n.format("adversity.gui.activate")));
    }

    private String getModeName() {
        return I18n.format("adversity.sanctuary.mode." + te.getMode().name().toLowerCase());
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        modeButton.displayString = getModeName();
        modeButton.enabled = te.isActivated();

        upgradeButton.enabled = te.isActivated() && te.getTier() < 5;
        upgradeButton.displayString = "T" + te.getTier() + " → T" + Math.min(te.getTier() + 1, 5);

        activateButton.visible = !te.isActivated();
        activateButton.enabled = !te.isActivated();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                PacketHandler.INSTANCE.sendToServer(new PacketSanctuaryAction(te.getPos(),
                        PacketSanctuaryAction.Action.TOGGLE_MODE));
                break;
            case 1:
                PacketHandler.INSTANCE.sendToServer(new PacketSanctuaryAction(te.getPos(),
                        PacketSanctuaryAction.Action.UPGRADE));
                break;
            case 2:
                PacketHandler.INSTANCE.sendToServer(new PacketSanctuaryAction(te.getPos(),
                        PacketSanctuaryAction.Action.ACTIVATE));
                break;
            case 4:
                // 切换到仪式GUI
                PacketHandler.INSTANCE.sendToServer(new PacketSwitchSanctuaryGui(te.getPos(), 
                        AdversityGuiHandler.GUI_SANCTUARY));
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

        // ========== 中间信息区域 ==========
        int infoAreaX = guiX + 28;
        int infoAreaY = guiY + 17;
        
        drawRect(infoAreaX, infoAreaY, infoAreaX + 140, infoAreaY + 8, 0xFF555555);
        drawRect(infoAreaX + 1, infoAreaY + 1, infoAreaX + 139, infoAreaY + 7, 0xFF3D3D3D);

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
        String title = I18n.format("adversity.gui.sanctuary_control");
        this.fontRenderer.drawString(title, 8, 6, 0x404040);

        // 玩家背包标签
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 0x404040);
    }
}
