package com.adversity.client.gui;

import com.adversity.network.PacketHandler;
import com.adversity.network.PacketSanctuaryAction;
import com.adversity.sanctuary.TileEntitySanctuary;
import com.adversity.sanctuary.inventory.ContainerSanctuary;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

/**
 * 圣所祭坛GUI - 纯代码绘制版本
 * 使用Minecraft标准GUI元素确保像素级对齐
 */
public class GuiSanctuaryAltar extends GuiContainer {

    // 使用标准背包贴图
    private static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation(
            "textures/gui/container/generic_54.png");

    private final TileEntitySanctuary te;

    private GuiButton modeButton;
    private GuiButton upgradeButton;
    private GuiButton activateButton;

    public GuiSanctuaryAltar(InventoryPlayer playerInv, TileEntitySanctuary te) {
        super(new ContainerSanctuary(playerInv, te));
        this.te = te;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();

        // 按钮放在右侧
        int buttonX = this.guiLeft + 100;
        int buttonY = this.guiTop + 18;
        int buttonWidth = 68;
        int buttonHeight = 16;

        // 模式切换按钮
        this.buttonList.add(modeButton = new GuiButton(0, buttonX, buttonY, buttonWidth, buttonHeight, getModeName()));

        // 升级按钮
        this.buttonList.add(upgradeButton = new GuiButton(1, buttonX, buttonY + 20, buttonWidth, buttonHeight,
                I18n.format("adversity.gui.upgrade")));

        // 激活按钮
        this.buttonList.add(activateButton = new GuiButton(2, buttonX, buttonY + 40, buttonWidth, buttonHeight,
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
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int guiX = this.guiLeft;
        int guiY = this.guiTop;

        // ========== 绘制主背景 ==========
        // 深色背景
        drawRect(guiX, guiY, guiX + xSize, guiY + ySize, 0xFFC6C6C6);

        // 3D边框效果
        drawHorizontalLine(guiX, guiX + xSize - 1, guiY, 0xFFFFFFFF);
        drawVerticalLine(guiX, guiY, guiY + ySize - 1, 0xFFFFFFFF);
        drawHorizontalLine(guiX, guiX + xSize - 1, guiY + ySize - 1, 0xFF555555);
        drawVerticalLine(guiX + xSize - 1, guiY, guiY + ySize - 1, 0xFF555555);

        // ========== 左侧燃料区域 ==========
        int fuelAreaX = guiX + 7;
        int fuelAreaY = guiY + 17;

        // 燃料条背景 (深色凹槽)
        drawRect(fuelAreaX, fuelAreaY, fuelAreaX + 18, fuelAreaY + 52, 0xFF555555);
        drawRect(fuelAreaX + 1, fuelAreaY + 1, fuelAreaX + 17, fuelAreaY + 51, 0xFF373737);

        // 燃料条填充 (渐变蓝色)
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

        // 燃料槽位 (标准18x18槽位外观)
        int slotX = fuelAreaX;
        int slotY = fuelAreaY + 54;
        drawSlotBackground(slotX, slotY);

        // ========== 中间信息面板 ==========
        int infoX = guiX + 28;
        int infoY = guiY + 17;
        int infoWidth = 68;
        int infoHeight = 60;

        // 凹陷面板背景
        drawRect(infoX, infoY, infoX + infoWidth, infoY + infoHeight, 0xFF555555);
        drawRect(infoX + 1, infoY + 1, infoX + infoWidth - 1, infoY + infoHeight - 1, 0xFF2D2D2D);

        // ========== 绘制玩家背包区域 ==========
        // 使用标准背包贴图的槽位部分
        this.mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
        // 绘制3行背包 (从y=84开始)
        this.drawTexturedModalRect(guiX + 7, guiY + 84, 7, 17, 162, 54);
        // 绘制快捷栏
        this.drawTexturedModalRect(guiX + 7, guiY + 142, 7, 17, 162, 18);
    }

    /**
     * 绘制标准槽位背景
     */
    private void drawSlotBackground(int x, int y) {
        // 外框 (凹陷效果)
        drawRect(x, y, x + 18, y + 18, 0xFF555555);
        // 内部
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        // 高光边
        drawHorizontalLine(x + 1, x + 16, y + 1, 0xFF373737);
        drawVerticalLine(x + 1, y + 1, y + 16, 0xFF373737);
        // 阴影边
        drawHorizontalLine(x + 1, x + 16, y + 16, 0xFFFFFFFF);
        drawVerticalLine(x + 16, y + 1, y + 16, 0xFFFFFFFF);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题
        String title = I18n.format("tile.adversity.sanctuary_altar.name");
        this.fontRenderer.drawString(title, 8, 6, 0x404040);

        // ========== 中间面板文字 ==========
        int infoX = 32;
        int infoY = 20;

        // 状态
        String status = te.isActivated() ? I18n.format("adversity.sanctuary.active")
                : I18n.format("adversity.sanctuary.inactive");
        int statusColor = te.isActivated() ? 0x55FF55 : 0xFF5555;
        this.fontRenderer.drawString(status, infoX, infoY, statusColor);

        // 燃料
        String fuelLabel = I18n.format("adversity.gui.fuel_slot") + ":";
        this.fontRenderer.drawString(fuelLabel, infoX, infoY + 12, 0xAAAAAA);

        String fuelValue = te.getFuel() + "/" + te.getMaxFuel();
        this.fontRenderer.drawString(fuelValue, infoX, infoY + 22, 0xFFFFFF);

        // 等级
        String tierLabel = "Tier: " + te.getTier();
        this.fontRenderer.drawString(tierLabel, infoX, infoY + 36, 0xFFD700);

        // 模式
        String modeLabel = I18n.format("adversity.sanctuary.mode." + te.getMode().name().toLowerCase());
        this.fontRenderer.drawString(modeLabel, infoX, infoY + 48, 0x00BFFF);

        // 玩家背包标签
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 0x404040);
    }
}
