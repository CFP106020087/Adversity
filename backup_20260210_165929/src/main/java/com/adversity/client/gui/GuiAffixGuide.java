package com.adversity.client.gui;

import com.adversity.Adversity;
import com.adversity.affix.AffixRegistry;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffix;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 词条指南GUI - 展示所有词条信息
 * 使用 glScissor 实现正确的内容裁剪
 */
@SideOnly(Side.CLIENT)
public class GuiAffixGuide extends GuiScreen {

    // GUI尺寸
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 220;

    // 标题区域高度
    private static final int HEADER_HEIGHT = 30;

    // 内容区域 (相对于GUI左上角)
    private static final int CONTENT_X = 15;
    private static final int CONTENT_Y = 35; // 标题下方
    private static final int CONTENT_WIDTH = 250;
    private static final int CONTENT_HEIGHT = 170;

    // 分类按钮
    private List<GuiButton> categoryButtons = new ArrayList<>();
    private AffixType selectedCategory = null; // null = 显示全部

    // 滚动
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // 缓存的词条列表
    private List<IAffix> displayedAffixes = new ArrayList<>();

    private int guiLeft;
    private int guiTop;

    @Override
    public void initGui() {
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        buttonList.clear();
        categoryButtons.clear();

        // 添加关闭按钮
        buttonList.add(new GuiButton(0, guiLeft + GUI_WIDTH - 22, guiTop + 5, 17, 17, "X"));

        // 添加分类按钮 (移到GUI内部顶部)
        int btnX = guiLeft + 10;
        int btnY = guiTop + GUI_HEIGHT + 5; // 在GUI外部下方
        int btnWidth = 45;

        // 全部按钮
        GuiButton allBtn = new GuiButton(10, btnX, btnY, btnWidth, 16, I18n.format("gui.adversity.guide.all"));
        buttonList.add(allBtn);
        categoryButtons.add(allBtn);
        btnX += btnWidth + 3;

        // 各类型按钮
        int id = 11;
        for (AffixType type : AffixType.values()) {
            String name = I18n.format("gui.adversity.guide.type." + type.name().toLowerCase());
            GuiButton btn = new GuiButton(id++, btnX, btnY, btnWidth, 16, name);
            buttonList.add(btn);
            categoryButtons.add(btn);
            btnX += btnWidth + 3;
        }

        refreshAffixList();
    }

    private void refreshAffixList() {
        displayedAffixes.clear();

        for (IAffix affix : AffixRegistry.getAllAffixes()) {
            if (selectedCategory == null || affix.getType() == selectedCategory) {
                displayedAffixes.add(affix);
            }
        }

        // 计算最大滚动
        int totalHeight = displayedAffixes.size() * 75; // 每个词条75像素高
        maxScroll = Math.max(0, totalHeight - CONTENT_HEIGHT);
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 1. 绘制全屏遮罩
        drawDefaultBackground();

        // 2. 绘制GUI主背景
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xEE1a1a2e);

        // 3. 绘制边框
        int borderColor = 0xFF6a4c93;
        drawHorizontalLine(guiLeft, guiLeft + GUI_WIDTH - 1, guiTop, borderColor);
        drawHorizontalLine(guiLeft, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, borderColor);
        drawVerticalLine(guiLeft, guiTop, guiTop + GUI_HEIGHT, borderColor);
        drawVerticalLine(guiLeft + GUI_WIDTH - 1, guiTop, guiTop + GUI_HEIGHT, borderColor);

        // 4. 绘制标题 (在内容区上方，不会被裁剪)
        String title = I18n.format("gui.adversity.guide.title");
        drawCenteredString(fontRenderer, title, guiLeft + GUI_WIDTH / 2, guiTop + 8, 0xFFd4af37);

        // 5. 绘制副标题
        String subtitle = I18n.format("gui.adversity.guide.subtitle", displayedAffixes.size());
        fontRenderer.drawString(subtitle, guiLeft + CONTENT_X, guiTop + 22, 0x888888);

        // 6. 绘制内容区域 (使用 Scissor 裁剪)
        int contentLeft = guiLeft + CONTENT_X;
        int contentTop = guiTop + CONTENT_Y;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        scissor(contentLeft, contentTop, CONTENT_WIDTH - 12, CONTENT_HEIGHT); // -12 给滚动条留空间

        int y = contentTop - scrollOffset;
        for (IAffix affix : displayedAffixes) {
            // 只绘制可见的条目
            if (y + 75 > contentTop - 10 && y < contentTop + CONTENT_HEIGHT + 10) {
                drawAffixEntry(affix, contentLeft, y, mouseX, mouseY);
            }
            y += 75;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // 7. 绘制滚动条 (在裁剪区域外)
        if (maxScroll > 0) {
            int scrollBarX = guiLeft + GUI_WIDTH - 12;
            int scrollBarY = contentTop;
            int scrollBarHeight = CONTENT_HEIGHT;

            // 轨道
            drawRect(scrollBarX, scrollBarY, scrollBarX + 6, scrollBarY + scrollBarHeight, 0x40000000);

            // 滑块
            int thumbHeight = Math.max(20, scrollBarHeight * CONTENT_HEIGHT / (maxScroll + CONTENT_HEIGHT));
            int thumbY = scrollBarY + (scrollBarHeight - thumbHeight) * scrollOffset / maxScroll;
            drawRect(scrollBarX, thumbY, scrollBarX + 6, thumbY + thumbHeight, 0xAA808080);
        }

        // 8. 绘制按钮
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * 设置剪裁区域 (注意 OpenGL 坐标系是左下角原点)
     */
    private void scissor(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        int screenHeight = mc.displayHeight;
        GL11.glScissor(x * scale, screenHeight - (y + height) * scale, width * scale, height * scale);
    }

    private void drawAffixEntry(IAffix affix, int x, int y, int mouseX, int mouseY) {
        int entryWidth = CONTENT_WIDTH - 20;
        int entryHeight = 72;

        // 背景
        boolean hovered = mouseX >= x && mouseX < x + entryWidth &&
                mouseY >= y && mouseY < y + entryHeight;
        int bgColor = hovered ? 0x44FFFFFF : 0x22FFFFFF;
        drawRect(x, y, x + entryWidth, y + entryHeight, bgColor);

        // 左边框颜色条
        int typeColor = getTypeColorInt(affix.getType());
        drawRect(x, y, x + 3, y + entryHeight, typeColor);

        // === 第一行: 名称 + 难度 ===
        // 难度要求（右上角）
        String diffReq = "";
        int diffWidth = 0;
        if (affix.getMinDifficulty() > 0) {
            diffReq = I18n.format("gui.adversity.guide.min_diff",
                    String.format("%.1f", affix.getMinDifficulty()));
            diffWidth = fontRenderer.getStringWidth(diffReq);
            fontRenderer.drawString(diffReq, x + entryWidth - diffWidth - 5, y + 5, 0xFFAA00);
        }

        // 词条名称 (带颜色)
        String typeColorCode = getTypeColor(affix.getType());
        String nameKey = "affix.adversity." + affix.getId().getPath() + ".name";
        String name = I18n.format(nameKey);
        if (name.equals(nameKey)) {
            name = affix.getId().getPath();
        }
        // 限制名称长度
        int maxNameWidth = entryWidth - diffWidth - 20;
        if (fontRenderer.getStringWidth(name) > maxNameWidth) {
            name = fontRenderer.trimStringToWidth(name, maxNameWidth - 10) + "...";
        }
        fontRenderer.drawStringWithShadow(typeColorCode + name, x + 8, y + 5, 0xFFFFFF);

        // === 第二行: 类型标签 ===
        String typeLabel = "\u00a78[" + I18n.format("gui.adversity.guide.type." + affix.getType().name().toLowerCase())
                + "]";
        fontRenderer.drawString(typeLabel, x + 8, y + 18, 0x888888);

        // === 第三行+: 描述 (多行) ===
        String descKey = "affix.adversity." + affix.getId().getPath() + ".desc";
        String desc = I18n.format(descKey);
        if (desc.equals(descKey)) {
            desc = "No description available";
        }

        int descWidth = entryWidth - 15;
        List<String> descLines = fontRenderer.listFormattedStringToWidth(desc, descWidth);
        int lineY = y + 32;
        int maxLines = 3;
        for (int i = 0; i < Math.min(descLines.size(), maxLines); i++) {
            String line = descLines.get(i);
            if (i == maxLines - 1 && descLines.size() > maxLines) {
                line = fontRenderer.trimStringToWidth(line, descWidth - 15) + "...";
            }
            fontRenderer.drawString(line, x + 8, lineY, 0xBBBBBB);
            lineY += 11;
        }

        // 悬停时显示完整描述
        if (hovered && descLines.size() > maxLines) {
            drawHoveringText(descLines, mouseX, mouseY);
        }
    }

    private String getTypeColor(AffixType type) {
        switch (type) {
            case OFFENSIVE:
                return "\u00a7c"; // 红色
            case DEFENSIVE:
                return "\u00a79"; // 蓝色
            case UTILITY:
                return "\u00a7a"; // 绿色
            case SPECIAL:
                return "\u00a7d"; // 紫色
            default:
                return "\u00a7f";
        }
    }

    private int getTypeColorInt(AffixType type) {
        switch (type) {
            case OFFENSIVE:
                return 0xFFFF5555; // 红色
            case DEFENSIVE:
                return 0xFF5555FF; // 蓝色
            case UTILITY:
                return 0xFF55FF55; // 绿色
            case SPECIAL:
                return 0xFFFF55FF; // 紫色
            default:
                return 0xFFFFFFFF;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.displayGuiScreen(null);
        } else if (button.id == 10) {
            selectedCategory = null;
            scrollOffset = 0;
            refreshAffixList();
        } else if (button.id >= 11 && button.id < 11 + AffixType.values().length) {
            selectedCategory = AffixType.values()[button.id - 11];
            scrollOffset = 0;
            refreshAffixList();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            scrollOffset -= Integer.signum(scroll) * 20;
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

