package com.adversity.client.gui;

import com.adversity.Adversity;
import com.adversity.affix.AffixRegistry;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffix;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 词条指南GUI - 展示所有词条信息
 */
@SideOnly(Side.CLIENT)
public class GuiAffixGuide extends GuiScreen {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(Adversity.MODID,
            "textures/gui/guide_background.png");

    // GUI尺寸
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;

    // 内容区域
    private static final int CONTENT_X = 20;
    private static final int CONTENT_Y = 35;
    private static final int CONTENT_WIDTH = 216;
    private static final int CONTENT_HEIGHT = 145;

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
        buttonList.add(new GuiButton(0, guiLeft + GUI_WIDTH - 25, guiTop + 5, 20, 20, "X"));

        // 添加分类按钮
        int btnX = guiLeft + 10;
        int btnY = guiTop + GUI_HEIGHT + 5;
        int btnWidth = 50;

        // 全部按钮
        GuiButton allBtn = new GuiButton(10, btnX, btnY, btnWidth, 20, I18n.format("gui.adversity.guide.all"));
        buttonList.add(allBtn);
        categoryButtons.add(allBtn);
        btnX += btnWidth + 5;

        // 各类型按钮
        int id = 11;
        for (AffixType type : AffixType.values()) {
            String name = I18n.format("gui.adversity.guide.type." + type.name().toLowerCase());
            GuiButton btn = new GuiButton(id++, btnX, btnY, btnWidth, 20, name);
            buttonList.add(btn);
            categoryButtons.add(btn);
            btnX += btnWidth + 5;
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
        int totalHeight = displayedAffixes.size() * 55; // 每个词条55像素高
        maxScroll = Math.max(0, totalHeight - CONTENT_HEIGHT);
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 绘制半透明背景
        drawDefaultBackground();

        // 绘制GUI背景
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xDD1a1a2e);

        // 绘制边框
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + 2, 0xFF6a4c93);
        drawRect(guiLeft, guiTop + GUI_HEIGHT - 2, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF6a4c93);
        drawRect(guiLeft, guiTop, guiLeft + 2, guiTop + GUI_HEIGHT, 0xFF6a4c93);
        drawRect(guiLeft + GUI_WIDTH - 2, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF6a4c93);

        // 绘制标题
        String title = I18n.format("gui.adversity.guide.title");
        int titleWidth = fontRenderer.getStringWidth(title);
        fontRenderer.drawStringWithShadow(title, guiLeft + (GUI_WIDTH - titleWidth) / 2, guiTop + 10, 0xFFd4af37);

        // 绘制副标题
        String subtitle = I18n.format("gui.adversity.guide.subtitle", displayedAffixes.size());
        fontRenderer.drawString(subtitle, guiLeft + CONTENT_X, guiTop + 25, 0xAAAAAA);

        // 设置裁剪区域
        GlStateManager.pushMatrix();

        // 绘制词条列表
        int contentTop = guiTop + CONTENT_Y;
        int y = contentTop - scrollOffset;

        for (IAffix affix : displayedAffixes) {
            if (y + 55 > contentTop && y < contentTop + CONTENT_HEIGHT) {
                drawAffixEntry(affix, guiLeft + CONTENT_X, y, mouseX, mouseY);
            }
            y += 55;
        }

        GlStateManager.popMatrix();

        // 绘制滚动条
        if (maxScroll > 0) {
            int scrollBarX = guiLeft + GUI_WIDTH - 10;
            int scrollBarY = guiTop + CONTENT_Y;
            int scrollBarHeight = CONTENT_HEIGHT;

            // 滚动条背景
            drawRect(scrollBarX, scrollBarY, scrollBarX + 6, scrollBarY + scrollBarHeight, 0x44FFFFFF);

            // 滚动条滑块
            int thumbHeight = Math.max(20, scrollBarHeight * CONTENT_HEIGHT / (maxScroll + CONTENT_HEIGHT));
            int thumbY = scrollBarY + (scrollBarHeight - thumbHeight) * scrollOffset / maxScroll;
            drawRect(scrollBarX, thumbY, scrollBarX + 6, thumbY + thumbHeight, 0xAAFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawAffixEntry(IAffix affix, int x, int y, int mouseX, int mouseY) {
        // 背景
        boolean hovered = mouseX >= x && mouseX < x + CONTENT_WIDTH - 15 &&
                mouseY >= y && mouseY < y + 52;
        int bgColor = hovered ? 0x44FFFFFF : 0x22FFFFFF;
        drawRect(x, y, x + CONTENT_WIDTH - 15, y + 52, bgColor);

        // 先计算难度要求的宽度（用于限制名称长度）
        int reservedWidth = 0;
        String diffReq = "";
        if (affix.getMinDifficulty() > 0) {
            diffReq = I18n.format("gui.adversity.guide.min_diff",
                    String.format("%.1f", affix.getMinDifficulty()));
            reservedWidth = fontRenderer.getStringWidth(diffReq) + 10;
        }

        // 词条名称 (带颜色，限制长度避免重叠)
        String typeColor = getTypeColor(affix.getType());
        String nameKey = "affix.adversity." + affix.getId().getPath() + ".name";
        String name = I18n.format(nameKey);
        // 如果翻译不存在，使用 ID
        if (name.equals(nameKey)) {
            name = affix.getId().getPath();
        }
        // 限制名称长度
        int maxNameWidth = CONTENT_WIDTH - 30 - reservedWidth;
        if (fontRenderer.getStringWidth(name) > maxNameWidth) {
            name = fontRenderer.trimStringToWidth(name, maxNameWidth - 10) + "...";
        }
        fontRenderer.drawStringWithShadow(typeColor + name, x + 5, y + 4, 0xFFFFFF);

        // 难度要求（右上角）
        if (reservedWidth > 0) {
            int reqWidth = fontRenderer.getStringWidth(diffReq);
            fontRenderer.drawString(diffReq, x + CONTENT_WIDTH - 20 - reqWidth, y + 4, 0xFFAA00);
        }

        // 类型标签
        String typeLabel = "\u00a78[" + I18n.format("gui.adversity.guide.type." + affix.getType().name().toLowerCase())
                + "]";
        fontRenderer.drawString(typeLabel, x + 5, y + 16, 0x888888);

        // 描述
        String descKey = "affix.adversity." + affix.getId().getPath() + ".desc";
        String desc = I18n.format(descKey);
        // 如果翻译不存在，显示默认描述
        if (desc.equals(descKey)) {
            desc = "No description available";
        }
        if (fontRenderer.getStringWidth(desc) > CONTENT_WIDTH - 25) {
            desc = fontRenderer.trimStringToWidth(desc, CONTENT_WIDTH - 30) + "...";
        }
        fontRenderer.drawString(desc, x + 5, y + 30, 0xCCCCCC);
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

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            // 关闭
            mc.displayGuiScreen(null);
        } else if (button.id == 10) {
            // 全部
            selectedCategory = null;
            scrollOffset = 0;
            refreshAffixList();
        } else if (button.id >= 11 && button.id < 11 + AffixType.values().length) {
            // 分类筛选
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
            scrollOffset -= scroll / 4;
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
