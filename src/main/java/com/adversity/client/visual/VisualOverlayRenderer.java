package com.adversity.client.visual;

import net.minecraft.client.Minecraft;
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
import java.util.Random;

/**
 * 视觉效果覆盖层渲染器 - 高品质版本
 * 使用多层渲染、动画和精细的视觉效果
 */
@SideOnly(Side.CLIENT)
public class VisualOverlayRenderer {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // 冰霜效果的持久化数据（用于蔓延动画）
    private static float[] frostCrawlProgress = new float[32];
    private static float[] frostCrawlTarget = new float[32];
    private static long lastFrostUpdate = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && mc.world != null && !mc.isGamePaused()) {
            VisualEffectManager.tick();
            updateFrostAnimation();
        }
    }

    /**
     * 更新冰霜蔓延动画
     */
    private void updateFrostAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrostUpdate < 50) return;
        lastFrostUpdate = currentTime;

        VisualEffectManager.EffectData frozenData = VisualEffectManager.getActiveEffects().get(VisualEffectType.FROZEN);
        float targetIntensity = frozenData != null ? frozenData.getDisplayIntensity() : 0;

        Random rand = new Random(42);
        for (int i = 0; i < frostCrawlProgress.length; i++) {
            // 更新目标
            if (targetIntensity > 0) {
                float baseTarget = targetIntensity * (0.6f + rand.nextFloat() * 0.4f);
                frostCrawlTarget[i] = baseTarget;
            } else {
                frostCrawlTarget[i] = 0;
            }

            // 平滑过渡
            float diff = frostCrawlTarget[i] - frostCrawlProgress[i];
            float speed = targetIntensity > frostCrawlProgress[i] ? 0.03f : 0.08f;
            frostCrawlProgress[i] += diff * speed;
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
        GlStateManager.disableAlpha();

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

        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    // ==================== 冰冻效果 - 极寒 ====================

    /**
     * 冰冻效果 - 高品质冰霜覆盖
     * 多层渲染：边缘冰霜 + 冰晶纹理 + 屏幕色调 + 呼吸雾气
     */
    private void renderFrozenEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        // 第1层：全屏冷色调（非常轻微）
        renderFrostTint(width, height, intensity);

        // 第2层：边缘冰霜蔓延效果
        renderFrostEdgeCrawl(width, height, intensity, tick);

        // 第3层：冰晶图案
        renderIceCrystals(width, height, intensity, tick);

        // 第4层：细腻的冰霜颗粒
        renderFrostParticles(width, height, intensity, tick);

        // 第5层：呼吸雾气效果（屏幕边缘的白雾）
        renderBreathFog(width, height, intensity, tick);

        // 第6层：冰裂纹
        if (intensity > 0.5f) {
            renderIceCracks(width, height, intensity, tick);
        }
    }

    /**
     * 冷色调覆盖
     */
    private void renderFrostTint(int width, int height, float intensity) {
        float alpha = intensity * 0.08f;
        drawRect(0, 0, width, height, 0.75f, 0.88f, 1.0f, alpha);
    }

    /**
     * 边缘冰霜蔓延 - 从四边向内蔓延的冰霜
     */
    private void renderFrostEdgeCrawl(int width, int height, float intensity, long tick) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 每条边8个冰霜区块
        int segmentsPerSide = 8;
        float shimmer = (float)(Math.sin(tick * 0.15) * 0.1 + 0.9);

        // 顶部边缘
        for (int i = 0; i < segmentsPerSide; i++) {
            float progress = frostCrawlProgress[i] * shimmer;
            if (progress < 0.01f) continue;

            float x1 = width * i / (float)segmentsPerSide;
            float x2 = width * (i + 1) / (float)segmentsPerSide;
            float maxY = height * 0.35f * progress;

            renderFrostGradientQuad(buffer, tessellator,
                x1, 0, x2, maxY,
                0.85f, 0.93f, 1.0f,
                intensity * 0.7f, 0, true);
        }

        // 底部边缘
        for (int i = 0; i < segmentsPerSide; i++) {
            float progress = frostCrawlProgress[i + 8] * shimmer;
            if (progress < 0.01f) continue;

            float x1 = width * i / (float)segmentsPerSide;
            float x2 = width * (i + 1) / (float)segmentsPerSide;
            float minY = height - height * 0.35f * progress;

            renderFrostGradientQuad(buffer, tessellator,
                x1, minY, x2, height,
                0.85f, 0.93f, 1.0f,
                0, intensity * 0.7f, true);
        }

        // 左边缘
        for (int i = 0; i < segmentsPerSide; i++) {
            float progress = frostCrawlProgress[i + 16] * shimmer;
            if (progress < 0.01f) continue;

            float y1 = height * i / (float)segmentsPerSide;
            float y2 = height * (i + 1) / (float)segmentsPerSide;
            float maxX = width * 0.25f * progress;

            renderFrostGradientQuad(buffer, tessellator,
                0, y1, maxX, y2,
                0.85f, 0.93f, 1.0f,
                intensity * 0.7f, 0, false);
        }

        // 右边缘
        for (int i = 0; i < segmentsPerSide; i++) {
            float progress = frostCrawlProgress[i + 24] * shimmer;
            if (progress < 0.01f) continue;

            float y1 = height * i / (float)segmentsPerSide;
            float y2 = height * (i + 1) / (float)segmentsPerSide;
            float minX = width - width * 0.25f * progress;

            renderFrostGradientQuad(buffer, tessellator,
                minX, y1, width, y2,
                0.85f, 0.93f, 1.0f,
                0, intensity * 0.7f, false);
        }
    }

    /**
     * 渲染冰霜渐变矩形
     */
    private void renderFrostGradientQuad(BufferBuilder buffer, Tessellator tessellator,
                                          float x1, float y1, float x2, float y2,
                                          float r, float g, float b,
                                          float alphaStart, float alphaEnd, boolean vertical) {
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        if (vertical) {
            buffer.pos(x1, y1, 0).color(r, g, b, alphaStart).endVertex();
            buffer.pos(x2, y1, 0).color(r, g, b, alphaStart).endVertex();
            buffer.pos(x2, y2, 0).color(r, g, b, alphaEnd).endVertex();
            buffer.pos(x1, y2, 0).color(r, g, b, alphaEnd).endVertex();
        } else {
            buffer.pos(x1, y1, 0).color(r, g, b, alphaStart).endVertex();
            buffer.pos(x2, y1, 0).color(r, g, b, alphaEnd).endVertex();
            buffer.pos(x2, y2, 0).color(r, g, b, alphaEnd).endVertex();
            buffer.pos(x1, y2, 0).color(r, g, b, alphaStart).endVertex();
        }

        tessellator.draw();
    }

    /**
     * 冰晶图案 - 六边形冰晶从边缘生长
     */
    private void renderIceCrystals(int width, int height, float intensity, long tick) {
        if (intensity < 0.2f) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        Random rand = new Random(12345);
        int numCrystals = (int)(20 * intensity);

        for (int i = 0; i < numCrystals; i++) {
            // 从边缘生成
            int side = i % 4;
            float baseX, baseY;
            float growAngle;

            switch (side) {
                case 0: // 顶部
                    baseX = rand.nextFloat() * width;
                    baseY = rand.nextFloat() * 40;
                    growAngle = (float)(Math.PI / 2 + (rand.nextFloat() - 0.5) * 0.8);
                    break;
                case 1: // 底部
                    baseX = rand.nextFloat() * width;
                    baseY = height - rand.nextFloat() * 40;
                    growAngle = (float)(-Math.PI / 2 + (rand.nextFloat() - 0.5) * 0.8);
                    break;
                case 2: // 左边
                    baseX = rand.nextFloat() * 40;
                    baseY = rand.nextFloat() * height;
                    growAngle = (float)((rand.nextFloat() - 0.5) * 0.8);
                    break;
                default: // 右边
                    baseX = width - rand.nextFloat() * 40;
                    baseY = rand.nextFloat() * height;
                    growAngle = (float)(Math.PI + (rand.nextFloat() - 0.5) * 0.8);
                    break;
            }

            // 动画生长
            float growProgress = (float)(Math.sin((tick + i * 17) * 0.03) * 0.3 + 0.7);
            float crystalSize = (15 + rand.nextFloat() * 35) * intensity * growProgress;

            renderSingleIceCrystal(buffer, tessellator, baseX, baseY, crystalSize, growAngle,
                intensity * (0.4f + rand.nextFloat() * 0.3f), tick + i * 10);
        }
    }

    /**
     * 渲染单个冰晶（分叉树状结构）
     */
    private void renderSingleIceCrystal(BufferBuilder buffer, Tessellator tessellator,
                                         float x, float y, float size, float angle,
                                         float alpha, long tick) {
        // 主干
        float endX = x + (float)Math.cos(angle) * size;
        float endY = y + (float)Math.sin(angle) * size;

        float shimmer = (float)(Math.sin(tick * 0.2) * 0.15 + 0.85);
        float a = alpha * shimmer;

        // 绘制主干（三角形使其有厚度）
        float perpX = (float)Math.cos(angle + Math.PI / 2) * 2;
        float perpY = (float)Math.sin(angle + Math.PI / 2) * 2;

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);

        // 亮色冰晶
        buffer.pos(x - perpX, y - perpY, 0).color(0.9f, 0.95f, 1.0f, a).endVertex();
        buffer.pos(x + perpX, y + perpY, 0).color(0.9f, 0.95f, 1.0f, a).endVertex();
        buffer.pos(endX, endY, 0).color(0.7f, 0.85f, 1.0f, a * 0.5f).endVertex();

        tessellator.draw();

        // 分支（递归深度1）
        if (size > 10) {
            float branchSize = size * 0.5f;
            float branchAngle1 = angle + 0.5f;
            float branchAngle2 = angle - 0.5f;
            float midX = x + (float)Math.cos(angle) * size * 0.5f;
            float midY = y + (float)Math.sin(angle) * size * 0.5f;

            // 分支1
            float branch1EndX = midX + (float)Math.cos(branchAngle1) * branchSize;
            float branch1EndY = midY + (float)Math.sin(branchAngle1) * branchSize;

            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(midX, midY, 0).color(0.85f, 0.92f, 1.0f, a * 0.7f).endVertex();
            buffer.pos(branch1EndX, branch1EndY, 0).color(0.7f, 0.85f, 1.0f, a * 0.3f).endVertex();
            tessellator.draw();

            // 分支2
            float branch2EndX = midX + (float)Math.cos(branchAngle2) * branchSize;
            float branch2EndY = midY + (float)Math.sin(branchAngle2) * branchSize;

            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(midX, midY, 0).color(0.85f, 0.92f, 1.0f, a * 0.7f).endVertex();
            buffer.pos(branch2EndX, branch2EndY, 0).color(0.7f, 0.85f, 1.0f, a * 0.3f).endVertex();
            tessellator.draw();
        }
    }

    /**
     * 细腻的冰霜颗粒
     */
    private void renderFrostParticles(int width, int height, float intensity, long tick) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        Random rand = new Random(tick / 3);
        int numParticles = (int)(100 * intensity);

        for (int i = 0; i < numParticles; i++) {
            // 主要集中在边缘
            float edgeBias = rand.nextFloat();
            float x, y;

            if (edgeBias < 0.7f) {
                // 边缘区域
                int side = rand.nextInt(4);
                switch (side) {
                    case 0:
                        x = rand.nextFloat() * width;
                        y = rand.nextFloat() * height * 0.15f;
                        break;
                    case 1:
                        x = rand.nextFloat() * width;
                        y = height - rand.nextFloat() * height * 0.15f;
                        break;
                    case 2:
                        x = rand.nextFloat() * width * 0.15f;
                        y = rand.nextFloat() * height;
                        break;
                    default:
                        x = width - rand.nextFloat() * width * 0.15f;
                        y = rand.nextFloat() * height;
                        break;
                }
            } else {
                x = rand.nextFloat() * width;
                y = rand.nextFloat() * height;
            }

            float size = 1 + rand.nextFloat() * 2;
            float sparkle = (float)(Math.sin((tick + i * 7) * 0.3) * 0.3 + 0.7);
            float a = intensity * 0.4f * sparkle * (edgeBias < 0.7f ? 1.0f : 0.3f);

            // 白色/淡蓝色随机
            float colorVariant = rand.nextFloat();
            float cr = 0.85f + colorVariant * 0.15f;
            float cg = 0.92f + colorVariant * 0.08f;
            float cb = 1.0f;

            buffer.pos(x, y, 0).color(cr, cg, cb, a).endVertex();
            buffer.pos(x + size, y, 0).color(cr, cg, cb, a).endVertex();
            buffer.pos(x + size, y + size, 0).color(cr, cg, cb, a).endVertex();
            buffer.pos(x, y + size, 0).color(cr, cg, cb, a).endVertex();
        }

        tessellator.draw();
    }

    /**
     * 呼吸雾气效果
     */
    private void renderBreathFog(int width, int height, float intensity, long tick) {
        if (intensity < 0.3f) return;

        float breathPhase = (float)(Math.sin(tick * 0.08) * 0.5 + 0.5);
        float fogIntensity = intensity * breathPhase * 0.25f;

        // 底部白雾（模拟呼吸）
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        float fogHeight = height * 0.12f * (0.8f + breathPhase * 0.4f);

        buffer.pos(0, height - fogHeight, 0).color(0.95f, 0.97f, 1.0f, 0f).endVertex();
        buffer.pos(width, height - fogHeight, 0).color(0.95f, 0.97f, 1.0f, 0f).endVertex();
        buffer.pos(width, height, 0).color(0.95f, 0.97f, 1.0f, fogIntensity).endVertex();
        buffer.pos(0, height, 0).color(0.95f, 0.97f, 1.0f, fogIntensity).endVertex();

        tessellator.draw();
    }

    /**
     * 冰裂纹效果（高强度时）
     */
    private void renderIceCracks(int width, int height, float intensity, long tick) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        Random rand = new Random(7777);
        int numCracks = (int)((intensity - 0.5f) * 20);

        for (int i = 0; i < numCracks; i++) {
            // 裂纹从角落延伸
            int corner = i % 4;
            float startX, startY;

            switch (corner) {
                case 0: startX = 0; startY = 0; break;
                case 1: startX = width; startY = 0; break;
                case 2: startX = 0; startY = height; break;
                default: startX = width; startY = height; break;
            }

            // 随机偏移
            startX += (rand.nextFloat() - 0.5f) * 60;
            startY += (rand.nextFloat() - 0.5f) * 60;

            renderCrackLine(buffer, tessellator, startX, startY,
                width / 2f, height / 2f,
                rand, intensity * 0.6f, tick + i * 20);
        }
    }

    /**
     * 渲染单条裂纹
     */
    private void renderCrackLine(BufferBuilder buffer, Tessellator tessellator,
                                  float startX, float startY, float targetX, float targetY,
                                  Random rand, float alpha, long tick) {
        float dx = targetX - startX;
        float dy = targetY - startY;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        float crackLength = Math.min(length * 0.4f, 150);

        int segments = 8;
        float currentX = startX;
        float currentY = startY;

        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            float progress = (float)i / segments;
            float segmentAlpha = alpha * (1.0f - progress * 0.7f);

            // 添加锯齿偏移
            float offsetX = (rand.nextFloat() - 0.5f) * 15;
            float offsetY = (rand.nextFloat() - 0.5f) * 15;

            float x = startX + (dx / length) * crackLength * progress + offsetX;
            float y = startY + (dy / length) * crackLength * progress + offsetY;

            // 闪烁效果
            float flicker = (float)(Math.sin((tick + i * 10) * 0.2) * 0.2 + 0.8);

            buffer.pos(x, y, 0).color(0.7f, 0.85f, 1.0f, segmentAlpha * flicker).endVertex();
        }

        tessellator.draw();
    }

    // ==================== 其他效果 ====================

    /**
     * 致盲效果 - 屏幕逐渐被黑暗吞噬
     */
    private void renderBlindingEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        float breathe = (float)(Math.sin(tick * 0.08) * 0.1 + 0.9);
        float viewRadius = (1.0f - intensity * 0.85f) * breathe;

        renderRadialGradient(width, height, viewRadius, 0.0f, 0.0f, 0.0f, intensity * 0.98f);
        renderEdgeNoise(width, height, tick, 0.0f, 0.0f, 0.0f, intensity * 0.3f);
    }

    /**
     * 恐惧效果 - 心跳脉冲
     */
    private void renderHorrorEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        double heartbeat = Math.sin(tick * 0.15) * 0.5 + 0.5;
        heartbeat = Math.pow(heartbeat, 0.5);
        float pulse = (float)(heartbeat * 0.4 + 0.6);
        float breathe = (float)(Math.sin(tick * 0.05) * 0.15 + 0.85);

        float vignetteSize = 0.3f + (1.0f - intensity) * 0.4f;
        vignetteSize *= breathe;

        renderVignette(width, height, vignetteSize, 0.0f, 0.0f, 0.0f, intensity * pulse * 0.85f);

        if (heartbeat > 0.7) {
            float redPulse = (float)((heartbeat - 0.7) / 0.3) * 0.15f * intensity;
            renderVignette(width, height, vignetteSize * 0.8f, 0.3f, 0.0f, 0.0f, redPulse);
        }
    }

    /**
     * 腐蚀效果
     */
    private void renderDecayEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        float spread = (float)(Math.sin(tick * 0.03) * 0.1 + 0.5);
        renderVignette(width, height, spread, 0.2f, 0.0f, 0.15f, intensity * 0.7f);
        renderDecayParticles(width, height, tick, intensity);
    }

    /**
     * 灼烧效果
     */
    private void renderBurningEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        float flicker1 = (float)(Math.sin(tick * 0.3) * 0.15 + 0.85);
        float flicker2 = (float)(Math.sin(tick * 0.47 + 1.5) * 0.1 + 0.9);

        renderVignette(width, height, 0.6f, 1.0f, 0.4f, 0.0f, intensity * 0.4f * flicker1);
        renderVignette(width, height, 0.45f, 1.0f, 0.2f, 0.0f, intensity * 0.3f * flicker2);
        renderHeatWaves(width, height, tick, intensity);
    }

    /**
     * 虚空凝视效果
     */
    private void renderVoidGazeEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        float pulse = (float)(Math.sin(tick * 0.1) * 0.2 + 0.8);
        renderVignette(width, height, 0.4f, 0.15f, 0.0f, 0.25f, intensity * 0.75f * pulse);
        renderVoidTendrils(width, height, tick, intensity);
        drawRect(0, 0, width, height, 0.1f, 0.0f, 0.15f, intensity * 0.12f);
    }

    /**
     * 引力扭曲效果
     */
    private void renderGravityDistortEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        float pulse = (float)(Math.sin(tick * 0.15) * 0.2 + 0.8);
        renderVignette(width, height, 0.7f, 0.1f, 0.0f, 0.1f, intensity * 0.25f * pulse);
        renderDistortionRings(width, height, tick, intensity);
    }

    /**
     * 吸血标记效果
     */
    private void renderBloodMarkEffect(int width, int height, VisualEffectManager.EffectData data, float partialTicks) {
        float intensity = data.getDisplayIntensity();
        long tick = VisualEffectManager.getGlobalTick();

        float pulse = (float)(Math.sin(tick * 0.2) * 0.3 + 0.7);
        renderVignette(width, height, 0.5f, 0.8f, 0.0f, 0.0f, intensity * 0.5f * pulse);
        renderBloodDrips(width, height, tick, intensity);
    }

    // ==================== 辅助渲染方法 ====================

    private void drawRect(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, 0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, 0).color(r, g, b, a).endVertex();
        tessellator.draw();
    }

    private void renderRadialGradient(int width, int height, float centerRadius, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int cx = width / 2;
        int cy = height / 2;
        float maxRadius = (float)Math.sqrt(cx * cx + cy * cy);

        int segments = 24;
        for (int i = 0; i < segments; i++) {
            float innerRatio = (float)i / segments;
            float outerRatio = (float)(i + 1) / segments;

            float innerAlpha = calculateRadialAlpha(innerRatio, centerRadius, alpha);
            float outerAlpha = calculateRadialAlpha(outerRatio, centerRadius, alpha);

            float innerR = maxRadius * innerRatio;
            float outerR = maxRadius * outerRatio;

            buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int j = 0; j <= 48; j++) {
                double angle = (j / 48.0) * Math.PI * 2;
                float cos = (float)Math.cos(angle);
                float sin = (float)Math.sin(angle);

                buffer.pos(cx + cos * innerR, cy + sin * innerR, 0).color(r, g, b, innerAlpha).endVertex();
                buffer.pos(cx + cos * outerR, cy + sin * outerR, 0).color(r, g, b, outerAlpha).endVertex();
            }
            tessellator.draw();
        }
    }

    private float calculateRadialAlpha(float ratio, float centerRadius, float maxAlpha) {
        if (ratio < centerRadius) return 0.0f;
        float normalized = (ratio - centerRadius) / (1.0f - centerRadius);
        return normalized * normalized * maxAlpha;
    }

    private void renderVignette(int width, int height, float size, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int cx = width / 2;
        int cy = height / 2;
        float radius = Math.min(width, height) * size;
        float maxRadius = (float)Math.sqrt(cx * cx + cy * cy);

        int segments = 16;
        for (int i = 0; i < segments; i++) {
            float innerRatio = radius + (maxRadius - radius) * (float)i / segments;
            float outerRatio = radius + (maxRadius - radius) * (float)(i + 1) / segments;

            float innerAlpha = alpha * (float)i / segments;
            float outerAlpha = alpha * (float)(i + 1) / segments;

            buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int j = 0; j <= 32; j++) {
                double angle = (j / 32.0) * Math.PI * 2;
                float cos = (float)Math.cos(angle);
                float sin = (float)Math.sin(angle);

                buffer.pos(cx + cos * innerRatio, cy + sin * innerRatio, 0).color(r, g, b, innerAlpha).endVertex();
                buffer.pos(cx + cos * outerRatio, cy + sin * outerRatio, 0).color(r, g, b, outerAlpha).endVertex();
            }
            tessellator.draw();
        }
    }

    private void renderEdgeNoise(int width, int height, long tick, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        Random rand = new Random(tick / 3);
        for (int i = 0; i < 40; i++) {
            int side = rand.nextInt(4);
            int x = side < 2 ? rand.nextInt(width) : (side == 2 ? rand.nextInt(40) : width - rand.nextInt(40));
            int y = side >= 2 ? rand.nextInt(height) : (side == 0 ? rand.nextInt(40) : height - rand.nextInt(40));
            int size = rand.nextInt(8) + 3;
            float a = alpha * (0.3f + rand.nextFloat() * 0.7f);

            buffer.pos(x, y, 0).color(r, g, b, a).endVertex();
            buffer.pos(x + size, y, 0).color(r, g, b, a).endVertex();
            buffer.pos(x + size, y + size, 0).color(r, g, b, a).endVertex();
            buffer.pos(x, y + size, 0).color(r, g, b, a).endVertex();
        }
        tessellator.draw();
    }

    private void renderDecayParticles(int width, int height, long tick, float intensity) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        Random rand = new Random(tick / 5);
        for (int i = 0; i < 25; i++) {
            int side = rand.nextInt(4);
            int x = side < 2 ? rand.nextInt(width) : (side == 2 ? rand.nextInt(50) : width - rand.nextInt(50));
            int y = side >= 2 ? rand.nextInt(height) : (side == 0 ? rand.nextInt(50) : height - rand.nextInt(50));
            int size = rand.nextInt(6) + 2;

            float cr = 0.1f + rand.nextFloat() * 0.15f;
            float cg = rand.nextFloat() * 0.12f;
            float cb = 0.08f + rand.nextFloat() * 0.1f;
            float a = intensity * 0.5f * (0.5f + rand.nextFloat() * 0.5f);

            buffer.pos(x, y, 0).color(cr, cg, cb, a).endVertex();
            buffer.pos(x + size, y, 0).color(cr, cg, cb, a).endVertex();
            buffer.pos(x + size, y + size, 0).color(cr, cg, cb, a).endVertex();
            buffer.pos(x, y + size, 0).color(cr, cg, cb, a).endVertex();
        }
        tessellator.draw();
    }

    private void renderHeatWaves(int width, int height, long tick, float intensity) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < 5; i++) {
            float phase = ((tick * 0.15f + i * 25) % 120) / 120.0f;
            int y = (int)(phase * height);
            int waveHeight = 2 + (int)(Math.sin(tick * 0.2 + i) * 1.5);
            float a = intensity * 0.12f * (0.5f + (float)Math.sin(tick * 0.25 + i) * 0.3f);

            buffer.pos(0, y, 0).color(1.0f, 0.5f, 0.1f, a).endVertex();
            buffer.pos(60, y, 0).color(1.0f, 0.5f, 0.1f, 0f).endVertex();
            buffer.pos(60, y + waveHeight, 0).color(1.0f, 0.5f, 0.1f, 0f).endVertex();
            buffer.pos(0, y + waveHeight, 0).color(1.0f, 0.5f, 0.1f, a).endVertex();

            buffer.pos(width - 60, y, 0).color(1.0f, 0.5f, 0.1f, 0f).endVertex();
            buffer.pos(width, y, 0).color(1.0f, 0.5f, 0.1f, a).endVertex();
            buffer.pos(width, y + waveHeight, 0).color(1.0f, 0.5f, 0.1f, a).endVertex();
            buffer.pos(width - 60, y + waveHeight, 0).color(1.0f, 0.5f, 0.1f, 0f).endVertex();
        }
        tessellator.draw();
    }

    private void renderVoidTendrils(int width, int height, long tick, float intensity) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int cx = width / 2;
        int cy = height / 2;
        float rotation = tick * 0.015f;

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 10; i++) {
            float angle = rotation + (float)(i * Math.PI * 2 / 10);
            float length = 40 + (float)Math.sin(tick * 0.08 + i) * 25;
            float startDist = 80;
            float a = intensity * 0.35f * (0.5f + (float)Math.sin(tick * 0.12 + i) * 0.3f);

            buffer.pos(cx + Math.cos(angle) * startDist, cy + Math.sin(angle) * startDist, 0)
                .color(0.25f, 0.0f, 0.35f, a).endVertex();
            buffer.pos(cx + Math.cos(angle) * (startDist + length), cy + Math.sin(angle) * (startDist + length), 0)
                .color(0.1f, 0.0f, 0.15f, a * 0.3f).endVertex();
        }
        tessellator.draw();
    }

    private void renderDistortionRings(int width, int height, long tick, float intensity) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int cx = width / 2;
        int cy = height / 2;

        for (int ring = 0; ring < 3; ring++) {
            float animProgress = ((tick + ring * 30) % 80) / 80.0f;
            float radius = 60 + animProgress * 120;
            float a = intensity * 0.2f * (1.0f - animProgress);

            buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < 32; i++) {
                double angle = i * Math.PI * 2 / 32;
                buffer.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0)
                    .color(0.3f, 0.0f, 0.3f, a).endVertex();
            }
            tessellator.draw();
        }
    }

    private void renderBloodDrips(int width, int height, long tick, float intensity) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);

        Random rand = new Random(tick / 12);
        for (int i = 0; i < 6; i++) {
            int x = 20 + rand.nextInt(width - 40);
            float dropProgress = ((tick + i * 25) % 90) / 90.0f;
            int y = (int)(dropProgress * 70);
            int dropSize = 3 + rand.nextInt(3);
            float a = intensity * 0.55f * (1.0f - dropProgress * 0.4f);

            buffer.pos(x, y + dropSize * 2, 0).color(0.55f, 0.0f, 0.0f, a).endVertex();
            buffer.pos(x - dropSize, y, 0).color(0.7f, 0.0f, 0.0f, a * 0.7f).endVertex();
            buffer.pos(x + dropSize, y, 0).color(0.7f, 0.0f, 0.0f, a * 0.7f).endVertex();
        }
        tessellator.draw();
    }
}
