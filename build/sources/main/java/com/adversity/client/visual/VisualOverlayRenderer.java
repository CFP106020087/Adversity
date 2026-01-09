package com.adversity.client.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Map;

/**
 * 视觉效果覆盖层渲染器
 * 负责在屏幕上渲染各种视觉效果
 */
@SideOnly(Side.CLIENT)
public class VisualOverlayRenderer {

    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && mc.world != null && !mc.isGamePaused()) {
            VisualEffectManager.tick();
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        if (!VisualEffectManager.hasAnyEffect()) {
            return;
        }

        ScaledResolution res = event.getResolution();
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();
        float partialTicks = event.getPartialTicks();

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();

        // 渲染每个激活的效果
        for (Map.Entry<VisualEffectType, VisualEffectManager.EffectData> entry : VisualEffectManager.getActiveEffects().entrySet()) {
            VisualEffectType type = entry.getKey();
            VisualEffectManager.EffectData data = entry.getValue();

            switch (type) {
                case BLINDING:
                    renderBlindingEffect(width, height, data, partialTicks);
                    break;
                case HORROR:
                    renderHorrorEffect(width, height, data, partialTicks);
                    break;
                case FROZEN:
                    renderFrozenEffect(width, height, data, partialTicks);
                    break;
                case DECAY:
                    renderDecayEffect(width, height, data, partialTicks);
                    break;
                case BURNING:
                    renderBurningEffect(width, height, data, partialTicks);
                    break;
                case VOID_GAZE:
                    renderVoidGazeEffect(width, height, data, partialTicks);
                    break;
                case GRAVITY_DISTORT:
                    renderGravityDistortEffect(width, height, data, partialTicks);
                    break;
                case BLOOD_MARK:
                    renderBloodMarkEffect(width, height, data, partialTicks);
                    break;
            }
        }

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 致盲效果 - 屏幕逐渐被黑暗吞噬，只留下中心小范围
     */
    private void renderBlindingEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        // 动态收缩的视野
        float breathe = (float) (Math.sin(tick * 0.08) * 0.1 + 0.9);
        float viewRadius = (1.0f - intensity * 0.85f) * breathe;

        // 渲染径向渐变黑暗
        renderRadialGradient(width, height, viewRadius, 0.0f, 0.0f, 0.0f, intensity * 0.98f);

        // 添加边缘噪声/纹理感
        renderEdgeNoise(width, height, tick, 0.0f, 0.0f, 0.0f, intensity * 0.3f);
    }

    /**
     * 恐惧效果 - 黑色呼吸脉冲，压迫感
     */
    private void renderHorrorEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        // 心跳般的脉冲（快-慢-快节奏）
        double heartbeat = Math.sin(tick * 0.15) * 0.5 + 0.5;
        heartbeat = Math.pow(heartbeat, 0.5);  // 使脉冲更尖锐
        float pulse = (float) (heartbeat * 0.4 + 0.6);

        // 呼吸般的收缩
        float breathe = (float) (Math.sin(tick * 0.05) * 0.15 + 0.85);

        // 渲染脉动的黑色边缘
        float vignetteSize = 0.3f + (1.0f - intensity) * 0.4f;
        vignetteSize *= breathe;

        renderVignette(width, height, vignetteSize, 0.0f, 0.0f, 0.0f, intensity * pulse * 0.85f);

        // 添加轻微的红色闪烁（心跳时）
        if (heartbeat > 0.7) {
            float redPulse = (float) ((heartbeat - 0.7) / 0.3) * 0.15f * intensity;
            renderVignette(width, height, vignetteSize * 0.8f, 0.3f, 0.0f, 0.0f, redPulse);
        }
    }

    /**
     * 冰冻效果 - 蓝色边缘冰晶
     */
    private void renderFrozenEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        // 冰晶闪烁
        float shimmer = (float) (Math.sin(tick * 0.2) * 0.1 + 0.9);

        // 渲染蓝色边缘
        renderVignette(width, height, 0.5f, 0.6f, 0.85f, 1.0f, intensity * 0.5f * shimmer);

        // 添加冰晶纹理效果
        renderFrostPattern(width, height, tick, intensity * 0.6f);

        // 全屏轻微蓝色色调
        renderFullScreen(width, height, 0.7f, 0.9f, 1.0f, intensity * 0.1f);
    }

    /**
     * 腐蚀/凋零效果 - 紫黑色腐烂边缘
     */
    private void renderDecayEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        // 腐烂的蔓延动画
        float spread = (float) (Math.sin(tick * 0.03) * 0.1 + 0.5);

        // 渲染紫黑色边缘
        renderVignette(width, height, spread, 0.2f, 0.0f, 0.15f, intensity * 0.7f);

        // 添加腐蚀粒子/纹理
        renderDecayParticles(width, height, tick, intensity);

        // 绿色凋零条纹
        renderDecayStripes(width, height, tick, intensity * 0.3f);
    }

    /**
     * 灼烧效果 - 火焰红光边缘
     */
    private void renderBurningEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        // 火焰闪烁
        float flicker1 = (float) (Math.sin(tick * 0.3) * 0.15 + 0.85);
        float flicker2 = (float) (Math.sin(tick * 0.47 + 1.5) * 0.1 + 0.9);

        // 渲染火焰边缘
        renderVignette(width, height, 0.6f, 1.0f, 0.4f, 0.0f, intensity * 0.4f * flicker1);
        renderVignette(width, height, 0.45f, 1.0f, 0.2f, 0.0f, intensity * 0.3f * flicker2);

        // 添加热浪扭曲效果（边缘更强）
        renderHeatDistortion(width, height, tick, intensity * 0.5f);
    }

    /**
     * 虚空凝视效果 - 深紫色压迫
     */
    private void renderVoidGazeEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        // 虚空脉动
        float pulse = (float) (Math.sin(tick * 0.1) * 0.2 + 0.8);

        // 渲染深紫色边缘
        renderVignette(width, height, 0.4f, 0.15f, 0.0f, 0.25f, intensity * 0.75f * pulse);

        // 添加扭曲的虚空纹理
        renderVoidPattern(width, height, tick, intensity);

        // 全屏轻微紫色
        renderFullScreen(width, height, 0.1f, 0.0f, 0.15f, intensity * 0.15f);
    }

    /**
     * 引力扭曲效果
     */
    private void renderGravityDistortEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        // 轻微的紫色边缘
        float pulse = (float) (Math.sin(tick * 0.15) * 0.2 + 0.8);
        renderVignette(width, height, 0.7f, 0.1f, 0.0f, 0.1f, intensity * 0.25f * pulse);

        // 扭曲线条效果
        renderDistortionLines(width, height, tick, intensity);
    }

    /**
     * 吸血标记效果 - 血红边缘
     */
    private void renderBloodMarkEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        // 血液脉动
        float pulse = (float) (Math.sin(tick * 0.2) * 0.3 + 0.7);

        // 渲染血红边缘
        renderVignette(width, height, 0.5f, 0.8f, 0.0f, 0.0f, intensity * 0.5f * pulse);

        // 血滴效果
        renderBloodDrops(width, height, tick, intensity);
    }

    // =========== 辅助渲染方法 ===========

    /**
     * 渲染径向渐变（中心透明，边缘不透明）
     */
    private void renderRadialGradient(int width, int height, float centerRadius, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int cx = width / 2;
        int cy = height / 2;
        float maxRadius = (float) Math.sqrt(cx * cx + cy * cy);

        // 绘制多个同心矩形来模拟径向渐变
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            float innerRatio = (float) i / segments;
            float outerRatio = (float) (i + 1) / segments;

            float innerAlpha = calculateRadialAlpha(innerRatio, centerRadius, alpha);
            float outerAlpha = calculateRadialAlpha(outerRatio, centerRadius, alpha);

            float innerR = maxRadius * innerRatio;
            float outerR = maxRadius * outerRatio;

            // 绘制渐变环
            buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int j = 0; j <= 64; j++) {
                double angle = (j / 64.0) * Math.PI * 2;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);

                buffer.pos(cx + cos * innerR, cy + sin * innerR, 0)
                    .color(r, g, b, innerAlpha).endVertex();
                buffer.pos(cx + cos * outerR, cy + sin * outerR, 0)
                    .color(r, g, b, outerAlpha).endVertex();
            }
            tessellator.draw();
        }
    }

    private float calculateRadialAlpha(float ratio, float centerRadius, float maxAlpha) {
        if (ratio < centerRadius) {
            return 0.0f;
        }
        float normalized = (ratio - centerRadius) / (1.0f - centerRadius);
        return normalized * normalized * maxAlpha;  // 二次曲线使边缘更强
    }

    /**
     * 渲染暗角效果
     */
    private void renderVignette(int width, int height, float size, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int cx = width / 2;
        int cy = height / 2;
        float radius = Math.min(width, height) * size;
        float maxRadius = (float) Math.sqrt(cx * cx + cy * cy);

        // 从半径到边缘的渐变
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            float innerRatio = radius + (maxRadius - radius) * (float) i / segments;
            float outerRatio = radius + (maxRadius - radius) * (float) (i + 1) / segments;

            float innerAlpha = alpha * (float) i / segments;
            float outerAlpha = alpha * (float) (i + 1) / segments;

            buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int j = 0; j <= 48; j++) {
                double angle = (j / 48.0) * Math.PI * 2;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);

                buffer.pos(cx + cos * innerRatio, cy + sin * innerRatio, 0)
                    .color(r, g, b, innerAlpha).endVertex();
                buffer.pos(cx + cos * outerRatio, cy + sin * outerRatio, 0)
                    .color(r, g, b, outerAlpha).endVertex();
            }
            tessellator.draw();
        }
    }

    /**
     * 渲染全屏颜色覆盖
     */
    private void renderFullScreen(int width, int height, float r, float g, float b, float alpha) {
        Gui.drawRect(0, 0, width, height, ((int)(alpha * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255));
    }

    /**
     * 渲染边缘噪声纹理
     */
    private void renderEdgeNoise(int width, int height, long tick, float r, float g, float b, float alpha) {
        // 简化版本：绘制多个随机点在边缘
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        java.util.Random rand = new java.util.Random(tick / 3);
        int edgeWidth = 40;

        for (int i = 0; i < 50; i++) {
            int side = rand.nextInt(4);
            int x, y;
            int size = rand.nextInt(10) + 5;

            switch (side) {
                case 0: // top
                    x = rand.nextInt(width);
                    y = rand.nextInt(edgeWidth);
                    break;
                case 1: // bottom
                    x = rand.nextInt(width);
                    y = height - rand.nextInt(edgeWidth);
                    break;
                case 2: // left
                    x = rand.nextInt(edgeWidth);
                    y = rand.nextInt(height);
                    break;
                default: // right
                    x = width - rand.nextInt(edgeWidth);
                    y = rand.nextInt(height);
                    break;
            }

            float a = alpha * (0.3f + rand.nextFloat() * 0.7f);
            buffer.pos(x, y, 0).color(r, g, b, a).endVertex();
            buffer.pos(x + size, y, 0).color(r, g, b, a).endVertex();
            buffer.pos(x + size, y + size, 0).color(r, g, b, a).endVertex();
            buffer.pos(x, y + size, 0).color(r, g, b, a).endVertex();
        }

        tessellator.draw();
    }

    /**
     * 渲染冰霜图案
     */
    private void renderFrostPattern(int width, int height, long tick, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 冰晶条纹从边缘向内延伸
        int numCrystals = 16;
        java.util.Random rand = new java.util.Random(42);  // 固定种子保持一致

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < numCrystals; i++) {
            int side = i % 4;
            float progress = (float) Math.sin((tick + i * 20) * 0.05) * 0.2f + 0.5f;
            float length = 30 + rand.nextFloat() * 50 * progress;
            float baseWidth = 3 + rand.nextFloat() * 5;

            int baseX, baseY;
            float angle;

            switch (side) {
                case 0: // top
                    baseX = (int) (rand.nextFloat() * width);
                    baseY = 0;
                    angle = (float) (Math.PI / 2 + (rand.nextFloat() - 0.5) * 0.5);
                    break;
                case 1: // bottom
                    baseX = (int) (rand.nextFloat() * width);
                    baseY = height;
                    angle = (float) (-Math.PI / 2 + (rand.nextFloat() - 0.5) * 0.5);
                    break;
                case 2: // left
                    baseX = 0;
                    baseY = (int) (rand.nextFloat() * height);
                    angle = (float) ((rand.nextFloat() - 0.5) * 0.5);
                    break;
                default: // right
                    baseX = width;
                    baseY = (int) (rand.nextFloat() * height);
                    angle = (float) (Math.PI + (rand.nextFloat() - 0.5) * 0.5);
                    break;
            }

            float tipX = baseX + (float) Math.cos(angle) * length;
            float tipY = baseY + (float) Math.sin(angle) * length;
            float perpX = (float) Math.cos(angle + Math.PI / 2) * baseWidth;
            float perpY = (float) Math.sin(angle + Math.PI / 2) * baseWidth;

            float a = alpha * (0.5f + rand.nextFloat() * 0.5f);
            buffer.pos(baseX - perpX, baseY - perpY, 0).color(0.7f, 0.9f, 1.0f, a).endVertex();
            buffer.pos(baseX + perpX, baseY + perpY, 0).color(0.7f, 0.9f, 1.0f, a).endVertex();
            buffer.pos(tipX, tipY, 0).color(0.9f, 0.95f, 1.0f, a * 0.3f).endVertex();
        }

        tessellator.draw();
    }

    /**
     * 渲染腐蚀粒子
     */
    private void renderDecayParticles(int width, int height, long tick, float intensity) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        java.util.Random rand = new java.util.Random(tick / 5);
        int edgeWidth = 60;

        for (int i = 0; i < 30; i++) {
            int side = rand.nextInt(4);
            int x, y;
            int size = rand.nextInt(8) + 2;

            switch (side) {
                case 0:
                    x = rand.nextInt(width);
                    y = rand.nextInt(edgeWidth);
                    break;
                case 1:
                    x = rand.nextInt(width);
                    y = height - rand.nextInt(edgeWidth);
                    break;
                case 2:
                    x = rand.nextInt(edgeWidth);
                    y = rand.nextInt(height);
                    break;
                default:
                    x = width - rand.nextInt(edgeWidth);
                    y = rand.nextInt(height);
                    break;
            }

            // 随机紫/黑/绿色
            float cr = 0.1f + rand.nextFloat() * 0.15f;
            float cg = rand.nextFloat() * 0.15f;
            float cb = 0.1f + rand.nextFloat() * 0.1f;
            float a = intensity * 0.4f * (0.5f + rand.nextFloat() * 0.5f);

            buffer.pos(x, y, 0).color(cr, cg, cb, a).endVertex();
            buffer.pos(x + size, y, 0).color(cr, cg, cb, a).endVertex();
            buffer.pos(x + size, y + size, 0).color(cr, cg, cb, a).endVertex();
            buffer.pos(x, y + size, 0).color(cr, cg, cb, a).endVertex();
        }

        tessellator.draw();
    }

    /**
     * 渲染腐蚀条纹
     */
    private void renderDecayStripes(int width, int height, long tick, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        // 垂直条纹，从边缘蔓延
        int numStripes = 8;
        java.util.Random rand = new java.util.Random(123);

        for (int i = 0; i < numStripes; i++) {
            boolean fromTop = rand.nextBoolean();
            int x = (int) (rand.nextFloat() * width);
            int stripeWidth = 2 + rand.nextInt(4);
            float stripeLength = 30 + rand.nextFloat() * 60 * (float) Math.sin((tick + i * 30) * 0.02);

            int y1, y2;
            if (fromTop) {
                y1 = 0;
                y2 = (int) stripeLength;
            } else {
                y1 = height - (int) stripeLength;
                y2 = height;
            }

            float a = alpha * (0.4f + rand.nextFloat() * 0.4f);
            // 深绿色凋零
            buffer.pos(x, y1, 0).color(0.1f, 0.2f, 0.1f, a).endVertex();
            buffer.pos(x + stripeWidth, y1, 0).color(0.1f, 0.2f, 0.1f, a).endVertex();
            buffer.pos(x + stripeWidth, y2, 0).color(0.1f, 0.2f, 0.1f, a * 0.2f).endVertex();
            buffer.pos(x, y2, 0).color(0.1f, 0.2f, 0.1f, a * 0.2f).endVertex();
        }

        tessellator.draw();
    }

    /**
     * 渲染热浪扭曲（简化版 - 用颜色条纹模拟）
     */
    private void renderHeatDistortion(int width, int height, long tick, float intensity) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        // 波动的橙色条纹
        int numWaves = 6;
        for (int i = 0; i < numWaves; i++) {
            float phase = (tick * 0.1f + i * 20) % 100;
            int y = (int) (phase / 100.0f * height);
            int waveHeight = 3 + (int) (Math.sin(tick * 0.2 + i) * 2);

            float a = intensity * 0.1f * (0.5f + (float) Math.sin(tick * 0.3 + i) * 0.3f);

            // 从边缘向中心渐变
            int fadeWidth = 80;
            // 左边
            buffer.pos(0, y, 0).color(1.0f, 0.5f, 0.0f, a).endVertex();
            buffer.pos(fadeWidth, y, 0).color(1.0f, 0.5f, 0.0f, 0.0f).endVertex();
            buffer.pos(fadeWidth, y + waveHeight, 0).color(1.0f, 0.5f, 0.0f, 0.0f).endVertex();
            buffer.pos(0, y + waveHeight, 0).color(1.0f, 0.5f, 0.0f, a).endVertex();

            // 右边
            buffer.pos(width - fadeWidth, y, 0).color(1.0f, 0.5f, 0.0f, 0.0f).endVertex();
            buffer.pos(width, y, 0).color(1.0f, 0.5f, 0.0f, a).endVertex();
            buffer.pos(width, y + waveHeight, 0).color(1.0f, 0.5f, 0.0f, a).endVertex();
            buffer.pos(width - fadeWidth, y + waveHeight, 0).color(1.0f, 0.5f, 0.0f, 0.0f).endVertex();
        }

        tessellator.draw();
    }

    /**
     * 渲染虚空图案
     */
    private void renderVoidPattern(int width, int height, long tick, float intensity) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int cx = width / 2;
        int cy = height / 2;

        // 旋转的虚空线条
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        int numLines = 12;
        float rotation = tick * 0.02f;

        for (int i = 0; i < numLines; i++) {
            float angle = rotation + (float) (i * Math.PI * 2 / numLines);
            float length = 50 + (float) Math.sin(tick * 0.1 + i) * 30;
            float startDist = 100;

            float x1 = cx + (float) Math.cos(angle) * startDist;
            float y1 = cy + (float) Math.sin(angle) * startDist;
            float x2 = cx + (float) Math.cos(angle) * (startDist + length);
            float y2 = cy + (float) Math.sin(angle) * (startDist + length);

            float a = intensity * 0.4f * (0.5f + (float) Math.sin(tick * 0.15 + i) * 0.3f);

            buffer.pos(x1, y1, 0).color(0.3f, 0.0f, 0.4f, a).endVertex();
            buffer.pos(x2, y2, 0).color(0.1f, 0.0f, 0.2f, a * 0.3f).endVertex();
        }

        tessellator.draw();
    }

    /**
     * 渲染引力扭曲线条
     */
    private void renderDistortionLines(int width, int height, long tick, float intensity) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int cx = width / 2;
        int cy = height / 2;

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // 向中心收缩的线条
        int numLines = 16;
        float animProgress = (tick % 60) / 60.0f;

        for (int i = 0; i < numLines; i++) {
            float angle = (float) (i * Math.PI * 2 / numLines);
            float startDist = 150 + animProgress * 100;
            float endDist = 80 + animProgress * 60;

            float x1 = cx + (float) Math.cos(angle) * startDist;
            float y1 = cy + (float) Math.sin(angle) * startDist;
            float x2 = cx + (float) Math.cos(angle) * endDist;
            float y2 = cy + (float) Math.sin(angle) * endDist;

            float a = intensity * 0.3f * (1.0f - animProgress);

            buffer.pos(x1, y1, 0).color(0.2f, 0.0f, 0.2f, a * 0.5f).endVertex();
            buffer.pos(x2, y2, 0).color(0.4f, 0.0f, 0.4f, a).endVertex();
        }

        tessellator.draw();
    }

    /**
     * 渲染血滴效果
     */
    private void renderBloodDrops(int width, int height, long tick, float intensity) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);

        java.util.Random rand = new java.util.Random(tick / 10);
        int numDrops = 8;

        for (int i = 0; i < numDrops; i++) {
            // 从顶部边缘滴落
            int x = rand.nextInt(width);
            float dropProgress = ((tick + i * 30) % 100) / 100.0f;
            int y = (int) (dropProgress * 80);
            int dropSize = 4 + rand.nextInt(4);

            float a = intensity * 0.6f * (1.0f - dropProgress * 0.5f);

            // 三角形血滴
            buffer.pos(x, y, 0).color(0.6f, 0.0f, 0.0f, a).endVertex();
            buffer.pos(x - dropSize, y - dropSize * 2, 0).color(0.8f, 0.0f, 0.0f, a * 0.7f).endVertex();
            buffer.pos(x + dropSize, y - dropSize * 2, 0).color(0.8f, 0.0f, 0.0f, a * 0.7f).endVertex();
        }

        tessellator.draw();
    }
}
