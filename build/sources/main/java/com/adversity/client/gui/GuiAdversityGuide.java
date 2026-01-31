package com.adversity.client.gui;

import com.adversity.Adversity;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 综合指南GUI - 包含圣所、饰品、附魔等机制说明
 */
@SideOnly(Side.CLIENT)
public class GuiAdversityGuide extends GuiScreen {

    // GUI尺寸
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 220;

    // 内容区域
    private static final int CONTENT_X = 15;
    private static final int CONTENT_Y = 45;
    private static final int CONTENT_WIDTH = 250;
    private static final int CONTENT_HEIGHT = 160;

    // 当前页签
    private int currentTab = 0;
    private static final String[] TAB_NAMES = { "overview", "sanctuary", "baubles", "enchants", "rituals",
            "progression", "tips" };

    // 滚动
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private List<String> contentLines = new ArrayList<>();

    // GUI位置
    private int guiLeft;
    private int guiTop;

    @Override
    public void initGui() {
        super.initGui();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        buttonList.clear();

        // 关闭按钮
        buttonList.add(new GuiButton(0, guiLeft + GUI_WIDTH - 20, guiTop + 5, 15, 15, "X"));

        // 页签按钮
        int tabY = guiTop + 25;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            String tabName = I18n.format("guide.adversity.tab." + TAB_NAMES[i]);
            buttonList.add(new GuiButton(10 + i, guiLeft + 5 + i * 38, tabY, 36, 14, tabName));
        }

        refreshContent();
    }

    private void refreshContent() {
        contentLines.clear();
        scrollOffset = 0;

        switch (currentTab) {
            case 0:
                addOverviewContent();
                break;
            case 1:
                addSanctuaryContent();
                break;
            case 2:
                addBaublesContent();
                break;
            case 3:
                addEnchantmentsContent();
                break;
            case 4:
                addRitualsContent();
                break;
            case 5:
                addProgressionContent();
                break;
            case 6:
                addTipsContent();
                break;
        }

        // 计算最大滚动
        int totalHeight = contentLines.size() * 11;
        maxScroll = Math.max(0, totalHeight - CONTENT_HEIGHT);
    }

    private void addRitualsContent() {
        addTitle("guide.adversity.rituals.title");
        addLine("");
        addWrappedText("guide.adversity.rituals.intro");
        addLine("");

        addSubtitle("guide.adversity.rituals.how_to");
        addWrappedText("guide.adversity.rituals.how_to.desc");
        addLine("");

        addSubtitle("guide.adversity.rituals.list");
        addBullet("guide.adversity.rituals.cleanse");
        addBullet("guide.adversity.rituals.awakening");
        // ...
    }

    private void addProgressionContent() {
        addTitle("guide.adversity.progression.title");
        addLine("");
        addWrappedText("guide.adversity.progression.intro");
        addLine("");

        addSubtitle("guide.adversity.progression.stages");
        addBullet("guide.adversity.progression.stage.0");
        addBullet("guide.adversity.progression.stage.1");
        addBullet("guide.adversity.progression.stage.2");
        addBullet("guide.adversity.progression.stage.3");
        addBullet("guide.adversity.progression.stage.4");
    }

    private void addOverviewContent() {
        addTitle("guide.adversity.overview.title");
        addLine("");
        addWrappedText("guide.adversity.overview.intro");
        addLine("");
        addSubtitle("guide.adversity.overview.difficulty");
        addWrappedText("guide.adversity.overview.difficulty.desc");
        addLine("");
        addSubtitle("guide.adversity.overview.elites");
        addWrappedText("guide.adversity.overview.elites.desc");
        addLine("");
        addSubtitle("guide.adversity.overview.affixes");
        addWrappedText("guide.adversity.overview.affixes.desc");
        addLine("");
        addSubtitle("guide.adversity.overview.counterplay");
        addWrappedText("guide.adversity.overview.counterplay.desc");
    }

    private void addSanctuaryContent() {
        addTitle("guide.adversity.sanctuary.title");
        addLine("");
        addWrappedText("guide.adversity.sanctuary.intro");
        addLine("");

        addSubtitle("guide.adversity.sanctuary.natural");
        addWrappedText("guide.adversity.sanctuary.natural.desc");
        addBullet("guide.adversity.sanctuary.natural.find");
        addBullet("guide.adversity.sanctuary.natural.activate");
        addBullet("guide.adversity.sanctuary.natural.effect");
        addLine("");

        addSubtitle("guide.adversity.sanctuary.artificial");
        addWrappedText("guide.adversity.sanctuary.artificial.desc");
        addBullet("guide.adversity.sanctuary.artificial.build");
        addBullet("guide.adversity.sanctuary.artificial.materials");
        addBullet("guide.adversity.sanctuary.artificial.effect");
        addLine("");

        addSubtitle("guide.adversity.sanctuary.entropy");
        addWrappedText("guide.adversity.sanctuary.entropy.desc");
        addBullet("guide.adversity.sanctuary.entropy.shard");
        addBullet("guide.adversity.sanctuary.entropy.crystal");
        addBullet("guide.adversity.sanctuary.entropy.core");
    }

    private void addBaublesContent() {
        addTitle("guide.adversity.baubles.title");
        addLine("");
        addWrappedText("guide.adversity.baubles.intro");
        addLine("");

        addSubtitle("guide.adversity.baubles.equipment");
        addBullet("guide.adversity.baubles.soul_chain");
        addBullet("guide.adversity.baubles.spatial_anchor");
        addBullet("guide.adversity.baubles.enchant_guardian");
        addLine("");

        addSubtitle("guide.adversity.baubles.elemental");
        addBullet("guide.adversity.baubles.flame_ward");
        addBullet("guide.adversity.baubles.frost_ward");
        addBullet("guide.adversity.baubles.corrosion_bane");
        addLine("");

        addSubtitle("guide.adversity.baubles.special");
        addBullet("guide.adversity.baubles.guardian_heart");
        addBullet("guide.adversity.baubles.anchor_stone");
        addBullet("guide.adversity.baubles.clarity_lens");
        addBullet("guide.adversity.baubles.courage_charm");
        addLine("");

        addSubtitle("guide.adversity.baubles.sanctuary_bonus");
        addWrappedText("guide.adversity.baubles.sanctuary_bonus.desc");
    }

    private void addEnchantmentsContent() {
        addTitle("guide.adversity.enchants.title");
        addLine("");
        addWrappedText("guide.adversity.enchants.intro");
        addLine("");

        addSubtitle("guide.adversity.enchants.list");
        addBullet("guide.adversity.enchants.soulbound");
        addBullet("guide.adversity.enchants.breaker");
        addBullet("guide.adversity.enchants.entropy_affinity");
        addBullet("guide.adversity.enchants.purifying_touch");
        addBullet("guide.adversity.enchants.resolute_will");
    }

    private void addTipsContent() {
        addTitle("guide.adversity.tips.title");
        addLine("");

        addSubtitle("guide.adversity.tips.beginner");
        addBullet("guide.adversity.tips.beginner.1");
        addBullet("guide.adversity.tips.beginner.2");
        addBullet("guide.adversity.tips.beginner.3");
        addLine("");

        addSubtitle("guide.adversity.tips.advanced");
        addBullet("guide.adversity.tips.advanced.1");
        addBullet("guide.adversity.tips.advanced.2");
        addBullet("guide.adversity.tips.advanced.3");
        addLine("");

        addSubtitle("guide.adversity.tips.danger");
        addBullet("guide.adversity.tips.danger.1");
        addBullet("guide.adversity.tips.danger.2");
    }

    // === 内容辅助方法 ===

    private void addTitle(String key) {
        contentLines.add("§6§l" + I18n.format(key));
    }

    private void addSubtitle(String key) {
        contentLines.add("§e" + I18n.format(key));
    }

    private void addLine(String text) {
        contentLines.add(text);
    }

    private void addBullet(String key) {
        contentLines.add("§7• " + I18n.format(key));
    }

    private void addWrappedText(String key) {
        String text = I18n.format(key);
        List<String> wrapped = fontRenderer.listFormattedStringToWidth(text, CONTENT_WIDTH - 10);
        for (String line : wrapped) {
            contentLines.add("§7" + line);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
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
        String title = I18n.format("guide.adversity.title");
        int titleWidth = fontRenderer.getStringWidth(title);
        fontRenderer.drawStringWithShadow(title, guiLeft + (GUI_WIDTH - titleWidth) / 2, guiTop + 8, 0xFFd4af37);

        // 高亮当前页签
        for (int i = 0; i < TAB_NAMES.length; i++) {
            GuiButton btn = buttonList.get(1 + i);
            if (i == currentTab) {
                drawRect(btn.x - 1, btn.y - 1, btn.x + btn.width + 1, btn.y + btn.height + 1, 0xFF6a4c93);
            }
        }

        // 绘制内容
        int contentTop = guiTop + CONTENT_Y;
        int y = contentTop - scrollOffset;

        GlStateManager.pushMatrix();
        // 简单裁剪 - 只绘制可见区域
        for (String line : contentLines) {
            if (y >= contentTop - 11 && y < contentTop + CONTENT_HEIGHT) {
                fontRenderer.drawString(line, guiLeft + CONTENT_X, y, 0xFFFFFF);
            }
            y += 11;
        }
        GlStateManager.popMatrix();

        // 绘制滚动条
        if (maxScroll > 0) {
            int scrollBarX = guiLeft + GUI_WIDTH - 10;
            int scrollBarY = guiTop + CONTENT_Y;
            int scrollBarHeight = CONTENT_HEIGHT;

            drawRect(scrollBarX, scrollBarY, scrollBarX + 6, scrollBarY + scrollBarHeight, 0x44FFFFFF);

            int thumbHeight = Math.max(20, scrollBarHeight * CONTENT_HEIGHT / (maxScroll + CONTENT_HEIGHT));
            int thumbY = scrollBarY + (scrollBarHeight - thumbHeight) * scrollOffset / maxScroll;
            drawRect(scrollBarX, thumbY, scrollBarX + 6, thumbY + thumbHeight, 0xAAFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.displayGuiScreen(null);
        } else if (button.id >= 10 && button.id < 10 + TAB_NAMES.length) {
            currentTab = button.id - 10;
            refreshContent();
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
