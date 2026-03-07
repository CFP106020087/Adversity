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
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ContainerRepair;

/**
 * 难度HUD - 在屏幕角落显示当前区域难度
 */
@SideOnly(Side.CLIENT)
public class DifficultyHUD {

    // HUD位置枚举
    public enum HudPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    // 缓存的难度值（避免每帧重新计算）
    private static float cachedDifficulty = 0;
    private static String cachedStageDisplay = "";
    private static String cachedCustomStages = "";
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
            resetCache();
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
     * 重置缓存（切換世界時調用，防止殘留舊資料）
     */
    public static void resetCache() {
        cachedDifficulty = 0;
        cachedStageDisplay = "";
        cachedCustomStages = "";
        lastUpdateTime = 0;
    }

    /**
     * 更新难度缓存
     */
    private void updateDifficultyCache(Minecraft mc) {
        long currentTime = mc.world.getTotalWorldTime();
        // 世界切換偵測：新世界 time < 上次記錄 → 強制重置
        if (currentTime < lastUpdateTime) {
            resetCache();
        }
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

        // 更新 GS 缓存
        IAdversityCapability.IProgression cap = player.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
        if (cap != null) {
            String prefix = "§7" + I18n.format("adversity.hud.stage") + " ";
            if (cap.hasStage("champion")) {
                cachedStageDisplay = prefix + "§c✦ §c" + I18n.format("adversity.stage.champion");
            } else if (cap.hasStage("warden")) {
                cachedStageDisplay = prefix + "§6✦ §6" + I18n.format("adversity.stage.warden");
            } else if (cap.hasStage("scholar")) {
                cachedStageDisplay = prefix + "§e✦ §e" + I18n.format("adversity.stage.scholar");
            } else if (cap.hasStage("awakened")) {
                cachedStageDisplay = prefix + "§a✦ §a" + I18n.format("adversity.stage.awakened");
            } else {
                cachedStageDisplay = prefix + "§8✦ §8" + I18n.format("adversity.stage.none");
            }

            // 收集自定义阶段（非预定义的四个）
            cachedCustomStages = "";
            StringBuilder custom = new StringBuilder();
            for (String stage : cap.getStages()) {
                if (!"awakened".equals(stage) && !"scholar".equals(stage)
                        && !"warden".equals(stage) && !"champion".equals(stage)) {
                    if (custom.length() > 0)
                        custom.append("§7, ");
                    custom.append("§b✦ §b").append(stage);
                }
            }
            if (custom.length() > 0) {
                cachedCustomStages = custom.toString();
            }
        }
    }

    /**
     * 解析HUD位置配置
     */
    private HudPosition parseHudPosition() {
        String pos = AdversityConfig.clientSettings.hudPosition.toUpperCase();
        try {
            return HudPosition.valueOf(pos);
        } catch (IllegalArgumentException e) {
            return HudPosition.TOP_LEFT;
        }
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
        String line3 = cachedStageDisplay;

        // 获取配置
        HudPosition position = parseHudPosition();
        int offsetX = AdversityConfig.clientSettings.hudOffsetX;
        int offsetY = AdversityConfig.clientSettings.hudOffsetY;

        // 自定义阶段行
        String line4 = cachedCustomStages;
        boolean hasCustom = !line4.isEmpty();

        // 计算HUD尺寸
        int bgWidth = Math.max(font.getStringWidth(line1),
                Math.max(font.getStringWidth(line2),
                        Math.max(font.getStringWidth(line3),
                                hasCustom ? font.getStringWidth(line4) : 0)))
                + 8;
        int bgHeight = hasCustom ? 42 : 32;

        // 根据位置配置计算坐标
        int screenWidth = resolution.getScaledWidth();
        int screenHeight = resolution.getScaledHeight();
        int x, y;

        switch (position) {
            case TOP_RIGHT:
                x = screenWidth - bgWidth - offsetX;
                y = offsetY;
                break;
            case BOTTOM_LEFT:
                x = offsetX;
                y = screenHeight - bgHeight - offsetY;
                break;
            case BOTTOM_RIGHT:
                x = screenWidth - bgWidth - offsetX;
                y = screenHeight - bgHeight - offsetY;
                break;
            case TOP_LEFT:
            default:
                x = offsetX;
                y = offsetY;
                break;
        }

        // 渲染背景
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
        font.drawStringWithShadow(line3, x + 2, y + 21, 0xFFFFFF);
        if (hasCustom) {
            font.drawStringWithShadow(line4, x + 2, y + 31, 0xFFFFFF);
        }
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
