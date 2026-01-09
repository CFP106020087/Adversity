package com.adversity.client.gui;

import com.adversity.client.TierColorSystem;
import com.adversity.config.AdversityConfig;
import com.adversity.difficulty.DifficultyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 难度HUD - 在屏幕角落显示当前区域难度
 */
@SideOnly(Side.CLIENT)
public class DifficultyHUD {

    // HUD位置偏移
    private static final int MARGIN_X = 5;
    private static final int MARGIN_Y = 5;

    // 缓存的难度值（避免每帧重新计算）
    private static float cachedDifficulty = 0;
    private static long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 20;  // 每20tick更新一次

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        // 只在TEXT阶段渲染（避免与其他HUD冲突）
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            return;
        }

        // 如果HUD被禁用，不显示
        if (!AdversityConfig.clientSettings.enableDifficultyHUD) {
            return;
        }

        // 如果在GUI中或F3开启，不显示
        if (mc.gameSettings.showDebugInfo) {
            return;
        }

        // 更新难度缓存
        updateDifficultyCache(mc);

        // 渲染HUD
        renderDifficultyHUD(mc, event.getResolution());
    }

    /**
     * 更新难度缓存
     */
    private void updateDifficultyCache(Minecraft mc) {
        long currentTime = mc.world.getTotalWorldTime();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            return;
        }
        lastUpdateTime = currentTime;

        EntityPlayer player = mc.player;
        BlockPos pos = player.getPosition();

        // 计算当前位置的难度
        cachedDifficulty = DifficultyManager.calculateDifficulty(
            mc.world,
            pos,
            player
        );

        // 应用玩家倍率
        cachedDifficulty = DifficultyManager.applyPlayerMultiplier(cachedDifficulty, player);
    }

    /**
     * 渲染难度HUD
     */
    private void renderDifficultyHUD(Minecraft mc, ScaledResolution resolution) {
        FontRenderer font = mc.fontRenderer;

        // 获取难度信息
        String diffLabel = TierColorSystem.getDifficultyLabel(cachedDifficulty);
        int diffColor = TierColorSystem.getDifficultyColor(cachedDifficulty);

        // 构建显示文本
        String line1 = "§7区域难度";
        String line2 = String.format("§f%.1f §7- %s%s",
            cachedDifficulty,
            TierColorSystem.getDifficultyTextColor(cachedDifficulty),
            diffLabel
        );

        // 计算位置（左上角）
        int x = MARGIN_X;
        int y = MARGIN_Y;

        // 渲染背景
        int bgWidth = Math.max(font.getStringWidth(line1), font.getStringWidth(line2)) + 8;
        int bgHeight = 22;
        drawRect(x - 2, y - 2, x + bgWidth, y + bgHeight, 0x80000000);

        // 渲染难度条
        int barWidth = bgWidth - 4;
        int barHeight = 3;
        int barY = y + 16;

        // 背景条
        drawRect(x, barY, x + barWidth, barY + barHeight, 0xFF333333);

        // 难度条（根据难度填充，最大100）
        float fillPercent = Math.min(1, cachedDifficulty / 100f);
        int fillWidth = (int) (barWidth * fillPercent);
        if (fillWidth > 0) {
            drawRect(x, barY, x + fillWidth, barY + barHeight, 0xFF000000 | diffColor);
        }

        // 渲染文字
        font.drawStringWithShadow(line1, x + 2, y, 0xAAAAAA);
        font.drawStringWithShadow(line2, x + 2, y + 8, 0xFFFFFF);
    }

    /**
     * 绘制矩形
     */
    private void drawRect(int left, int top, int right, int bottom, int color) {
        if (left > right) {
            int temp = left;
            left = right;
            right = temp;
        }
        if (top > bottom) {
            int temp = top;
            top = bottom;
            bottom = temp;
        }

        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
        GlStateManager.color(red, green, blue, alpha);

        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION);
        buffer.pos(left, bottom, 0.0D).endVertex();
        buffer.pos(right, bottom, 0.0D).endVertex();
        buffer.pos(right, top, 0.0D).endVertex();
        buffer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
