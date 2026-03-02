package com.adversity.client.gui;

import com.adversity.network.PacketHandler;
import com.adversity.network.PacketTeleportToSanctuary;
import com.adversity.sanctuary.SanctuaryZone;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 圣所传送选择GUI
 * 显示所有已激活圣所供玩家选择传送
 */
@SideOnly(Side.CLIENT)
public class GuiSanctuaryTeleport extends GuiScreen {

    private final BlockPos currentAltarPos;
    private final List<SanctuaryEntry> sanctuaryEntries = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int VISIBLE_ENTRIES = 5;
    private static final int ENTRY_HEIGHT = 24;

    // GUI尺寸
    private int guiLeft;
    private int guiTop;
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 180;

    public GuiSanctuaryTeleport(BlockPos currentPos, List<SanctuaryZone> sanctuaries, EntityPlayer player) {
        this.currentAltarPos = currentPos;
        
        // 转换为条目列表
        for (SanctuaryZone zone : sanctuaries) {
            // 排除当前圣所
            if (!zone.center.equals(currentPos) || zone.dimension != player.dimension) {
                double distance = Math.sqrt(player.getDistanceSq(zone.center));
                sanctuaryEntries.add(new SanctuaryEntry(zone, distance));
            }
        }
        
        // 按距离排序
        sanctuaryEntries.sort((a, b) -> Double.compare(a.distance, b.distance));
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        // 关闭按钮
        this.buttonList.add(new GuiButton(0, guiLeft + GUI_WIDTH - 22, guiTop + 4, 18, 14, "X"));

        // 滚动按钮
        this.buttonList.add(new GuiButton(1, guiLeft + GUI_WIDTH - 22, guiTop + 30, 18, 14, "▲"));
        this.buttonList.add(new GuiButton(2, guiLeft + GUI_WIDTH - 22, guiTop + GUI_HEIGHT - 50, 18, 14, "▼"));

        // 传送按钮 (每个条目一个)
        updateTeleportButtons();
    }

    private void updateTeleportButtons() {
        // 移除旧的传送按钮 (ID >= 10)
        buttonList.removeIf(b -> b.id >= 10);

        // 添加新的传送按钮
        for (int i = 0; i < VISIBLE_ENTRIES && i + scrollOffset < sanctuaryEntries.size(); i++) {
            int buttonY = guiTop + 30 + i * ENTRY_HEIGHT;
            this.buttonList.add(new GuiButton(10 + i, guiLeft + 150, buttonY + 4, 40, 16, 
                    I18n.format("adversity.gui.teleport")));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            // 关闭
            this.mc.displayGuiScreen(null);
        } else if (button.id == 1) {
            // 向上滚动
            if (scrollOffset > 0) {
                scrollOffset--;
                updateTeleportButtons();
            }
        } else if (button.id == 2) {
            // 向下滚动
            if (scrollOffset + VISIBLE_ENTRIES < sanctuaryEntries.size()) {
                scrollOffset++;
                updateTeleportButtons();
            }
        } else if (button.id >= 10) {
            // 传送按钮
            int index = button.id - 10 + scrollOffset;
            if (index < sanctuaryEntries.size()) {
                SanctuaryEntry entry = sanctuaryEntries.get(index);
                PacketHandler.INSTANCE.sendToServer(new PacketTeleportToSanctuary(
                        entry.zone.dimension, entry.zone.center));
                this.mc.displayGuiScreen(null);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 绘制暗色背景
        this.drawDefaultBackground();

        // GUI背景
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xDD222222);

        // 边框
        drawHorizontalLine(guiLeft, guiLeft + GUI_WIDTH - 1, guiTop, 0xFF666666);
        drawHorizontalLine(guiLeft, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, 0xFF333333);
        drawVerticalLine(guiLeft, guiTop, guiTop + GUI_HEIGHT - 1, 0xFF666666);
        drawVerticalLine(guiLeft + GUI_WIDTH - 1, guiTop, guiTop + GUI_HEIGHT - 1, 0xFF333333);

        // 标题
        String title = I18n.format("adversity.gui.teleport_title");
        this.fontRenderer.drawString(title, guiLeft + 8, guiTop + 8, 0xFFFFFF);

        // 副标题
        String subtitle = I18n.format("adversity.gui.teleport_subtitle", sanctuaryEntries.size());
        this.fontRenderer.drawString(subtitle, guiLeft + 8, guiTop + 18, 0xAAAAAA);

        // 绘制条目
        GlStateManager.pushMatrix();
        for (int i = 0; i < VISIBLE_ENTRIES && i + scrollOffset < sanctuaryEntries.size(); i++) {
            SanctuaryEntry entry = sanctuaryEntries.get(i + scrollOffset);
            int entryY = guiTop + 30 + i * ENTRY_HEIGHT;

            // 条目背景
            drawRect(guiLeft + 4, entryY, guiLeft + 145, entryY + ENTRY_HEIGHT - 2, 0x44FFFFFF);

            // 维度图标颜色
            int dimColor = getDimensionColor(entry.zone.dimension);
            drawRect(guiLeft + 6, entryY + 2, guiLeft + 12, entryY + ENTRY_HEIGHT - 4, dimColor);

            // 坐标
            String coords = String.format("(%d, %d, %d)", 
                    entry.zone.center.getX(), entry.zone.center.getY(), entry.zone.center.getZ());
            this.fontRenderer.drawString(coords, guiLeft + 16, entryY + 4, 0xFFFFFF);

            // 距离和维度名
            String info = String.format("%.0fm | %s", entry.distance, getDimensionName(entry.zone.dimension));
            this.fontRenderer.drawString(info, guiLeft + 16, entryY + 14, 0x888888);
        }
        GlStateManager.popMatrix();

        // 绘制按钮
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 无圣所提示
        if (sanctuaryEntries.isEmpty()) {
            String noSanctuary = I18n.format("adversity.gui.no_sanctuary");
            int textWidth = this.fontRenderer.getStringWidth(noSanctuary);
            this.fontRenderer.drawString(noSanctuary, 
                    guiLeft + (GUI_WIDTH - textWidth) / 2, guiTop + 80, 0xFFAA00);
        }
    }

    private int getDimensionColor(int dimension) {
        switch (dimension) {
            case 0: return 0xFF55AA55; // 主世界 - 绿色
            case -1: return 0xFFAA5555; // 下界 - 红色
            case 1: return 0xFF9966CC; // 末地 - 紫色
            default: return 0xFF5555AA; // 其他 - 蓝色
        }
    }

    private String getDimensionName(int dimension) {
        switch (dimension) {
            case 0: return I18n.format("adversity.dimension.overworld");
            case -1: return I18n.format("adversity.dimension.nether");
            case 1: return I18n.format("adversity.dimension.end");
            default: return "Dim " + dimension;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * 圣所条目
     */
    private static class SanctuaryEntry {
        final SanctuaryZone zone;
        final double distance;

        SanctuaryEntry(SanctuaryZone zone, double distance) {
            this.zone = zone;
            this.distance = distance;
        }
    }
}
