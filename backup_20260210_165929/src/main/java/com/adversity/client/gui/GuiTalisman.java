package com.adversity.client.gui;

import com.adversity.talisman.ContainerTalisman;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

/**
 * 护符盒 GUI - Betweenlands 魔法书册风格
 * 使用代码绘制实现深紫色神秘主题
 */
public class GuiTalisman extends GuiContainer {

    // 标准背包贴图用于槽位
    private static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation(
            "textures/gui/container/generic_54.png");

    // 护符盒的3x3槽位在Container中的布局
    // 起始位置: x=62, y=17 (相对于guiLeft/guiTop)

    public GuiTalisman(ContainerTalisman container) {
        super(container);
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int guiX = this.guiLeft;
        int guiY = this.guiTop;

        // ========== 主背景 - 深紫色神秘风格 ==========
        // 外层边框 (金色)
        drawRect(guiX - 2, guiY - 2, guiX + xSize + 2, guiY + ySize + 2, 0xFFB8860B);

        // 主背景 (深紫色)
        drawRect(guiX, guiY, guiX + xSize, guiY + ySize, 0xFF1A0A1F);

        // 内框装饰 (暗紫色渐变边框)
        drawRect(guiX + 2, guiY + 2, guiX + xSize - 2, guiY + ySize - 2, 0xFF2D1436);
        drawRect(guiX + 4, guiY + 4, guiX + xSize - 4, guiY + ySize - 4, 0xFF1A0A1F);

        // 边框高光
        drawHorizontalLine(guiX + 2, guiX + xSize - 3, guiY + 2, 0xFF6B3A7A);
        drawVerticalLine(guiX + 2, guiY + 2, guiY + ySize - 3, 0xFF6B3A7A);
        drawHorizontalLine(guiX + 2, guiX + xSize - 3, guiY + ySize - 3, 0xFF0D0510);
        drawVerticalLine(guiX + xSize - 3, guiY + 2, guiY + ySize - 3, 0xFF0D0510);

        // ========== 护符槽区域 - 魔法光环 ==========
        int talismanAreaX = guiX + 53;
        int talismanAreaY = guiY + 12;
        int areaWidth = 70;
        int areaHeight = 62;

        // 魔法光环背景 (蓝紫色渐变)
        drawRect(talismanAreaX, talismanAreaY, talismanAreaX + areaWidth, talismanAreaY + areaHeight, 0xFF0A0020);
        drawRect(talismanAreaX + 2, talismanAreaY + 2, talismanAreaX + areaWidth - 2, talismanAreaY + areaHeight - 2,
                0xFF150835);

        // 神秘符文边框
        drawHorizontalLine(talismanAreaX, talismanAreaX + areaWidth - 1, talismanAreaY, 0xFF4169E1);
        drawHorizontalLine(talismanAreaX, talismanAreaX + areaWidth - 1, talismanAreaY + areaHeight - 1, 0xFF4169E1);
        drawVerticalLine(talismanAreaX, talismanAreaY, talismanAreaY + areaHeight - 1, 0xFF4169E1);
        drawVerticalLine(talismanAreaX + areaWidth - 1, talismanAreaY, talismanAreaY + areaHeight - 1, 0xFF4169E1);

        // 角落装饰 (小方块)
        drawRect(talismanAreaX - 2, talismanAreaY - 2, talismanAreaX + 2, talismanAreaY + 2, 0xFFB8860B);
        drawRect(talismanAreaX + areaWidth - 2, talismanAreaY - 2, talismanAreaX + areaWidth + 2, talismanAreaY + 2,
                0xFFB8860B);
        drawRect(talismanAreaX - 2, talismanAreaY + areaHeight - 2, talismanAreaX + 2, talismanAreaY + areaHeight + 2,
                0xFFB8860B);
        drawRect(talismanAreaX + areaWidth - 2, talismanAreaY + areaHeight - 2, talismanAreaX + areaWidth + 2,
                talismanAreaY + areaHeight + 2, 0xFFB8860B);

        // ========== 绘制9个护符槽 (3x3) ==========
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = guiX + 61 + col * 18;
                int slotY = guiY + 16 + row * 18;
                drawMagicSlotBackground(slotX, slotY);
            }
        }

        // ========== 玩家背包区域 ==========
        // 背包背景 (略微透明的深色)
        drawRect(guiX + 6, guiY + 82, guiX + 170, guiY + 162, 0xAA2D1436);

        // 槽位边框
        drawHorizontalLine(guiX + 6, guiX + 169, guiY + 82, 0xFF6B3A7A);
        drawHorizontalLine(guiX + 6, guiX + 169, guiY + 161, 0xFF0D0510);

        // 使用标准背包贴图的槽位
        this.mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
        // 绘制3行背包 (从y=84开始)
        this.drawTexturedModalRect(guiX + 7, guiY + 84, 7, 17, 162, 54);
        // 绘制快捷栏
        this.drawTexturedModalRect(guiX + 7, guiY + 142, 7, 17, 162, 18);
    }

    /**
     * 绘制魔法风格槽位背景
     */
    private void drawMagicSlotBackground(int x, int y) {
        // 外框 (深蓝紫色)
        drawRect(x, y, x + 18, y + 18, 0xFF2F1A4A);
        // 内部 (更深的紫色)
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF1A0A2E);
        // 内凹效果
        drawHorizontalLine(x + 1, x + 16, y + 1, 0xFF0A0515);
        drawVerticalLine(x + 1, y + 1, y + 16, 0xFF0A0515);
        // 底部高光
        drawHorizontalLine(x + 1, x + 16, y + 16, 0xFF4B2A6B);
        drawVerticalLine(x + 16, y + 1, y + 16, 0xFF4B2A6B);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题 (金色)
        String title = I18n.format("adversity.gui.talisman.title");
        int titleWidth = this.fontRenderer.getStringWidth(title);
        this.fontRenderer.drawStringWithShadow(title, (this.xSize - titleWidth) / 2, -10, 0xFFD700);

        // 玩家背包标签 (淡紫色)
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 0xAA88AA);
    }
}
