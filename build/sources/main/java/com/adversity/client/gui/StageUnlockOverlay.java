package com.adversity.client.gui;

import com.adversity.Adversity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@SideOnly(Side.CLIENT)
public class StageUnlockOverlay {

    private static String activeStage = null;
    private static int animationTicks = 0;
    private static final int MAX_TICKS = 160; // 延長時間以展示宏大的退場過渡
    private static final Random rand = new Random();
    private static final List<Particle2D> particles = new ArrayList<>();

    // 觸發動畫
    public static void triggerUnlock(String stageName) {
        activeStage = stageName;
        animationTicks = 0;
        particles.clear();

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != null && mc.player != null) {
            // 降低音調 (Pitch) 讓 Warden 與 Champion 聽起來更加厚重、史詩
            float pitch = (stageName.equals("warden") || stageName.equals("champion")) ? 0.6F : 1.0F;
            mc.world.playSound(mc.player.posX, mc.player.posY, mc.player.posZ,
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0F, pitch, false);
        }

        Adversity.LOGGER.info("Triggered EPIC GS unlock overlay for stage: " + stageName);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && activeStage != null) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world != null && !mc.isGamePaused()) {
                animationTicks++;
                updateParticles();

                if (animationTicks > MAX_TICKS) {
                    activeStage = null;
                    particles.clear();
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || activeStage == null) {
            return;
        }

        ScaledResolution res = event.getResolution();
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();
        float partialTicks = event.getPartialTicks();
        float t = animationTicks + partialTicks;

        GlStateManager.pushMatrix();

        // ---------------- 1. 物理螢幕震動系統 ----------------
        float shakeX = 0, shakeY = 0;
        if ("warden".equals(activeStage)) {
            if (t >= 20 && t < 45) { // 砸地瞬間引發毀滅級震動
                float intensity = 30f * (1.0f - (t - 20) / 25f);
                shakeX = (rand.nextFloat() - 0.5f) * intensity;
                shakeY = (rand.nextFloat() - 0.5f) * intensity;
            }
        } else if ("champion".equals(activeStage)) {
            if (t > 15 && t < 150) { // 深淵常駐狂暴微震
                float pulse = (float) Math.sin(t * 0.4f) * 0.5f + 0.5f;
                float intensity = 4f + pulse * 6f;
                shakeX = (rand.nextFloat() - 0.5f) * intensity;
                shakeY = (rand.nextFloat() - 0.5f) * intensity;
            }
        }

        // 為了確保文字清晰度，震動位移採用整數
        if (shakeX != 0 || shakeY != 0) {
            GlStateManager.translate((int) shakeX, (int) shakeY, 0);
        }

        // ---------------- 2. 初始化史詩渲染狀態 ----------------
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.disableAlpha(); // 停用 Alpha Test，確保半透明邊緣平滑過渡

        // 全域生命週期 Alpha (淡入淡出)
        float globalAlpha = 1.0f;
        if (t < 20)
            globalAlpha = t / 20.0f;
        else if (t > MAX_TICKS - 30)
            globalAlpha = (MAX_TICKS - t) / 30.0f;
        globalAlpha = Math.max(0, Math.min(1, globalAlpha));

        // 根據不同階段渲染
        switch (activeStage) {
            case "awakened":
                renderAwakened(width, height, globalAlpha, partialTicks, t);
                break;
            case "scholar":
                renderScholar(width, height, globalAlpha, partialTicks, t);
                break;
            case "warden":
                renderWarden(width, height, globalAlpha, partialTicks, t);
                break;
            case "champion":
                renderChampion(width, height, globalAlpha, partialTicks, t);
                break;
            default:
                renderDefault(width, height, globalAlpha, partialTicks, t);
                break;
        }

        // 恢復正常狀態
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        setAdditive(false);
        GlStateManager.popMatrix();
    }

    // ==================== 動畫 1: Awakened (自然爆發 / 復甦狂潮) ====================
    private void renderAwakened(int width, int height, float globalAlpha, float partialTicks, float t) {
        float cx = width / 2f, cy = height / 2f;
        float r = 0.2f, g = 1.0f, b = 0.4f;

        setAdditive(false);
        renderVignette(width, height, 0.2f, 0.0f, 0.1f, 0.0f, globalAlpha * 0.8f);

        setAdditive(true);
        for (int i = 0; i < 3; i++) {
            float pulseT = t - i * 20;
            if (pulseT > 0 && pulseT < 60) {
                float radius = pulseT * (Math.max(width, height) * 0.025f);
                float alpha = (1.0f - pulseT / 60f) * globalAlpha * 0.6f;
                renderRing(cx, cy, Math.max(0, radius - 40), radius, r, g, b, 0, alpha);
                renderRing(cx, cy, radius, radius + 40, r, g, b, alpha, 0);
            }
        }

        if (animationTicks < 120) {
            for (int i = 0; i < 8; i++) {
                float angle = rand.nextFloat() * (float) Math.PI * 2;
                float dist = rand.nextFloat() * width * 0.45f;
                spawnParticle(cx + (float) Math.cos(angle) * dist, cy + (float) Math.sin(angle) * dist + 50,
                        (float) Math.cos(angle) * 3f + (rand.nextFloat() - 0.5f) * 3f,
                        -2f - rand.nextFloat() * 5f,
                        r, g, b, 2f + rand.nextFloat() * 3f, 80, 1);
            }
        }
        renderParticles(globalAlpha, partialTicks);

        float textScale = 2.5f, yOffset = 0f; // 基礎放大到 2.5 倍
        if (t < 20) {
            textScale = (t / 20f) * 2.5f;
            yOffset = 20 - t;
        } else if (t < 50) {
            float p = (t - 20) / 30f;
            // 彈跳的幅度也跟著基礎大小稍微增加
            textScale = 2.5f + (float) (Math.sin(p * Math.PI * 3) * (1.0f - p) * 0.5f);
        }
        renderText("adversity.stage.awakened", width, height, textScale, yOffset, globalAlpha, 0x55FF55, false);
    }

    // ==================== 動畫 2: Scholar (宇宙真理 / 奧術矩陣) ====================
    private void renderScholar(int width, int height, float globalAlpha, float partialTicks, float t) {
        float cx = width / 2f, cy = height / 2f;

        setAdditive(false);
        renderVignette(width, height, 0.0f, 0.0f, 0.05f, 0.15f, globalAlpha * 0.95f);

        setAdditive(true);
        float maxRadius = Math.max(width, height) * 0.85f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 0);
        float scale = Math.min(1.0f, t / 40f);
        GlStateManager.scale(scale, scale, 1.0f);

        GlStateManager.pushMatrix();
        GlStateManager.rotate(t * 0.8f, 0, 0, 1);
        renderMagicCircle(maxRadius, 0.1f, 0.4f, 1.0f, globalAlpha * 0.3f, 12, true);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.rotate(-t * 1.5f, 0, 0, 1);
        renderMagicCircle(maxRadius * 0.5f, 1.0f, 0.8f, 0.2f, globalAlpha * 0.5f, 8, true);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.rotate(t * 3.5f, 0, 0, 1);
        renderMagicCircle(maxRadius * 0.25f, 0.2f, 0.8f, 1.0f, globalAlpha * 0.8f, 6, false);
        GlStateManager.popMatrix();
        GlStateManager.popMatrix();

        if (animationTicks < 50) {
            float angle = rand.nextFloat() * (float) Math.PI * 2;
            float px = cx + (float) Math.cos(angle) * maxRadius * 1.2f;
            float py = cy + (float) Math.sin(angle) * maxRadius * 1.2f;
            spawnParticle(px, py, (cx - px) * 0.015f, (cy - py) * 0.015f, 0.2f, 0.8f, 1.0f, 2f + rand.nextFloat() * 2f,
                    50, 2);
        }

        if (t > 35 && t < 65) {
            float flash = 1.0f - Math.abs(t - 45) / 15f;
            renderQuad(0, 0, width, height, 0.8f, 0.9f, 1.0f, flash * globalAlpha * 0.85f);
        }

        renderParticles(globalAlpha, partialTicks);

        float textAlpha = (t > 30) ? Math.min(1.0f, (t - 30) / 20f) * globalAlpha : 0f;
        // 基礎 2.5 倍，呼吸的起伏也稍微加強到 0.08
        float textScale = 2.5f + (float) Math.sin(t * 0.05) * 0.08f;
        if (textAlpha > 0) {
            renderText("adversity.stage.scholar", width, height, textScale, 0f, textAlpha, 0x44AAFF, false);
        }
    }

    // ==================== 動畫 3: Warden (絕對壁壘 / 大地重擊) ====================
    private void renderWarden(int width, int height, float globalAlpha, float partialTicks, float t) {
        float cx = width / 2f, cy = height / 2f;
        float r = 1.0f, g = 0.6f, b = 0.0f;

        setAdditive(false);
        if (t < 20) {
            renderQuad(0, 0, width, height, 0f, 0f, 0f, (t / 20f) * globalAlpha * 0.9f);
        } else {
            renderVignette(width, height, 0.2f, 0.1f, 0.05f, 0.0f, globalAlpha * 0.85f);
        }

        setAdditive(true);
        if (t >= 20) {
            if (t < 40) {
                float flash = 1.0f - (t - 20) / 20f;
                renderQuad(0, 0, width, height, r, g, 0f, flash * globalAlpha * 0.7f);
            }

            float shieldAlpha = Math.max(0, 1.0f - (t - 20) / 80f) * globalAlpha * 0.5f;
            renderFullHexGrid(width, height, r, g, b, shieldAlpha, t);

            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, 0);
            float hexS = 1.0f + (t - 20) * 0.005f;
            GlStateManager.scale(hexS, hexS, 1.0f);
            renderMagicCircle(Math.min(width, height) * 0.35f, r, g, b, shieldAlpha * 1.5f, 6, false);
            GlStateManager.popMatrix();
        }

        if (animationTicks == 20) {
            for (int i = 0; i < 150; i++) {
                float angle = rand.nextFloat() * (float) Math.PI * 2;
                float speed = 10f + rand.nextFloat() * 45f;
                spawnParticle(cx, cy, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed - 5f,
                        r, 0.4f + rand.nextFloat() * 0.3f, 0f, 3f + rand.nextFloat() * 5f, 50 + rand.nextInt(30), 3);
            }
        }
        renderParticles(globalAlpha, partialTicks);

        float textScale = 3.0f; // 砸到地上後的最終大小是 3.0 倍
        float textAlpha = globalAlpha;
        if (t < 20) {
            float p = t / 20f;
            // 從極致巨大的 15.0 倍外太空視角極速壓縮下來
            textScale = 3.0f + (float) Math.pow(1.0f - p, 3) * 12.0f;
            textAlpha = p * globalAlpha;
        } else if (t < 28) {
            // 反作用力的擠壓變形也要跟著放大
            textScale = 3.0f - (float) Math.sin((t - 20) / 8f * Math.PI) * 0.4f;
        }
        renderText("adversity.stage.warden", width, height, textScale, 0f, textAlpha, 0xFF9900, false);
    }

    // ==================== 動畫 4: Champion (深淵狂怒 / 煉獄地獄) ====================
    private void renderChampion(int width, int height, float globalAlpha, float partialTicks, float t) {
        setAdditive(false);
        renderVignette(width, height, 0.1f, 0.5f, 0.0f, 0.0f, globalAlpha * 0.95f);

        setAdditive(true);
        float pulse = (float) Math.sin(t * 0.5f) * 0.5f + 0.5f;
        renderQuad(0, 0, width, height, 1.0f, 0.0f, 0.0f, pulse * globalAlpha * 0.15f);

        // 生成火焰餘燼 (Fire Embers)
        if (animationTicks < 130) {
            for (int i = 0; i < 10; i++) {
                float px = rand.nextFloat() * width;
                float py = height + 20;
                float vx = (rand.nextFloat() - 0.5f) * 6f;
                float vy = -10f - rand.nextFloat() * 15f; // 火焰上升
                // 初始為明亮的黃橘色
                spawnParticle(px, py, vx, vy, 1.0f, 0.6f + rand.nextFloat() * 0.4f, 0.0f, 4f + rand.nextFloat() * 4f,
                        40, 4);
            }
        }
        renderParticles(globalAlpha, partialTicks);

        float progress = Math.min(1.0f, t / 60f);
        int cR = 255;
        int cG = (int) ((1.0f - progress) * 200);
        int cB = (int) ((1.0f - progress) * 100);
        int hexColor = (cR << 16) | (cG << 8) | cB;
        // 基礎來到 3.5 倍，隨著心跳震動的幅度也加強
        float textScale = 3.5f + (float) Math.sin(t * 0.8f) * 0.15f;

        renderText("adversity.stage.champion", width, height, textScale, 0f, globalAlpha, hexColor, true);
    }

    private void renderDefault(int width, int height, float globalAlpha, float partialTicks, float t) {
        setAdditive(true);
        renderQuad(0, 0, width, height, 0.5f, 0.8f, 1.0f, globalAlpha * 0.2f);
        // 預設放大到 2.0 倍
        renderText("adversity.stage." + activeStage, width, height, 2.0f, 0f, globalAlpha, 0xFFFFFF, false);
    }

    // ==================== 工具與進階幾何渲染 ====================

    private void setAdditive(boolean additive) {
        if (additive)
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        else
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    private void renderText(String langKey, int width, int height, float scale, float yOffset, float alpha,
            int hexColor, boolean isGlitch) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;

        // 【修復 Format Error】: 安全且正確地使用 I18n 格式化
        String stageName = I18n.format(langKey);
        String text;
        if (I18n.hasKey("adversity.stage.unlock")) {
            // 正確寫法：把翻譯好的 stageName 傳入 %s 的參數中
            text = I18n.format("adversity.stage.unlock", stageName);
        } else {
            // 防呆機制
            text = "Unlocked: " + stageName;
        }

        int textWidth = font.getStringWidth(text);

        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2f, height / 2f + yOffset, 0);
        GlStateManager.scale(scale, scale, 1.0f);

        // 【修復字體模糊】: 座標必須為整數，避免小數點導致的像素撕裂
        int x = -textWidth / 2;
        int y = -4;

        GlStateManager.enableTexture2D();
        setAdditive(false); // 確保文字本體不被 Additive 疊加模糊

        int alphaBits = (int) (alpha * 255) << 24;

        if (isGlitch) {
            // 保留 Champion 的錯位重影，但偏移量改用整數
            int offset = (rand.nextInt(3) - 1) * 2;
            int rCol = 0xFF0000 | ((int) (alpha * 150) << 24);
            int bCol = 0x0000FF | ((int) (alpha * 150) << 24);
            font.drawString(text, x - offset, y, rCol, false);
            font.drawString(text, x + offset, y, bCol, false);
        }

        // 捨棄原本迴圈畫 4 次的便宜陰影，直接用原生自帶陰影，既省效能又最銳利
        font.drawStringWithShadow(text, x, y, (hexColor & 0x00FFFFFF) | alphaBits);

        GlStateManager.disableTexture2D();
        GlStateManager.popMatrix();
    }

    private void renderQuad(float x, float y, float w, float h, float r, float g, float b, float a) {
        if (a <= 0)
            return;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y + h, 0).color(r, g, b, a).endVertex();
        buffer.pos(x + w, y + h, 0).color(r, g, b, a).endVertex();
        buffer.pos(x + w, y, 0).color(r, g, b, a).endVertex();
        buffer.pos(x, y, 0).color(r, g, b, a).endVertex();
        tessellator.draw();
    }

    private void renderVignette(int width, int height, float innerRadius, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        float cx = width / 2f, cy = height / 2f;
        float maxRadius = (float) Math.sqrt(cx * cx + cy * cy) * 1.2f;
        float radiusStart = Math.min(width, height) * innerRadius;

        buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int j = 0; j <= 32; j++) {
            double angle = (j / 32.0) * Math.PI * 2;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            buffer.pos(cx + cos * radiusStart, cy + sin * radiusStart, 0).color(r, g, b, 0.0f).endVertex();
            buffer.pos(cx + cos * maxRadius, cy + sin * maxRadius, 0).color(r, g, b, alpha).endVertex();
        }
        tessellator.draw();
    }

    private void renderRing(float cx, float cy, float innerRadius, float outerRadius, float r, float g, float b,
            float alphaInner, float alphaOuter) {
        if (outerRadius <= 0)
            return;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 32; i++) {
            double angle = (i / 32.0) * Math.PI * 2;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            buffer.pos(cx + cos * innerRadius, cy + sin * innerRadius, 0).color(r, g, b, alphaInner).endVertex();
            buffer.pos(cx + cos * outerRadius, cy + sin * outerRadius, 0).color(r, g, b, alphaOuter).endVertex();
        }
        tessellator.draw();
    }

    private void renderMagicCircle(float radius, float r, float g, float b, float alpha, int points, boolean complex) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GL11.glLineWidth(2.0f);

        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 64; i++) {
            double angle = (i / 64.0) * Math.PI * 2;
            buffer.pos(Math.cos(angle) * radius, Math.sin(angle) * radius, 0).color(r, g, b, alpha).endVertex();
        }
        tessellator.draw();

        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= points; i++) {
            double angle = (i / (float) points) * Math.PI * 2;
            buffer.pos(Math.cos(angle) * radius, Math.sin(angle) * radius, 0).color(r, g, b, alpha).endVertex();
        }
        tessellator.draw();

        if (complex) {
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < points; i++) {
                double a1 = (i / (float) points) * Math.PI * 2;
                double a2 = ((i + points / 2) / (float) points) * Math.PI * 2;
                buffer.pos(Math.cos(a1) * radius, Math.sin(a1) * radius, 0).color(r, g, b, alpha * 0.5f).endVertex();
                buffer.pos(Math.cos(a2) * radius, Math.sin(a2) * radius, 0).color(r, g, b, alpha * 0.5f).endVertex();
            }
            tessellator.draw();
        }
        GL11.glLineWidth(1.0f);
    }

    private void renderFullHexGrid(float w, float h, float r, float g, float b, float alpha, float t) {
        float hexSize = 40.0f;
        float rowHeight = hexSize * 1.5f;
        float colWidth = hexSize * (float) Math.sqrt(3);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GL11.glLineWidth(2.0f);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        int rows = (int) (h / rowHeight) + 2;
        int cols = (int) (w / colWidth) + 2;

        for (int row = -1; row <= rows; row++) {
            for (int col = -1; col <= cols; col++) {
                float cx = col * colWidth + (row % 2 != 0 ? colWidth / 2 : 0);
                float cy = row * rowHeight;
                float dist = (float) Math.sqrt(Math.pow(cx - w / 2, 2) + Math.pow(cy - h / 2, 2));

                float wave = (float) Math.sin(t * 0.2f - dist * 0.015f);
                float localAlpha = alpha * Math.max(0, wave);
                if (localAlpha <= 0.01f)
                    continue;

                for (int i = 0; i < 6; i++) {
                    double angle1 = (i / 6.0) * Math.PI * 2 + Math.PI / 6;
                    double angle2 = ((i + 1) / 6.0) * Math.PI * 2 + Math.PI / 6;
                    buffer.pos(cx + Math.cos(angle1) * hexSize, cy + Math.sin(angle1) * hexSize, 0)
                            .color(r, g, b, localAlpha).endVertex();
                    buffer.pos(cx + Math.cos(angle2) * hexSize, cy + Math.sin(angle2) * hexSize, 0)
                            .color(r, g, b, localAlpha).endVertex();
                }
            }
        }
        tessellator.draw();
        GL11.glLineWidth(1.0f);
    }

    // ==================== 物理引擎級粒子系統 ====================

    private static class Particle2D {
        float x, y, prevX, prevY, vx, vy, r, g, b, size;
        int life, maxLife, type;
    }

    private void spawnParticle(float x, float y, float vx, float vy, float r, float g, float b, float size, int life,
            int type) {
        Particle2D p = new Particle2D();
        p.x = x;
        p.y = y;
        p.prevX = x;
        p.prevY = y;
        p.vx = vx;
        p.vy = vy;
        p.r = r;
        p.g = g;
        p.b = b;
        p.size = size;
        p.life = life;
        p.maxLife = life;
        p.type = type;
        particles.add(p);
    }

    private void updateParticles() {
        Iterator<Particle2D> it = particles.iterator();
        while (it.hasNext()) {
            Particle2D p = it.next();
            p.prevX = p.x;
            p.prevY = p.y;
            p.x += p.vx;
            p.y += p.vy;

            if (p.type == 1) {
                p.vx *= 0.95f;
                p.vy *= 0.95f;
                p.vy -= 0.05f;
                p.x += Math.sin(p.life * 0.1f) * 0.5f;
            } else if (p.type == 2) {
                p.vx *= 1.08f;
                p.vy *= 1.08f;
            } else if (p.type == 3) {
                p.vx *= 0.92f;
                p.vy += 0.8f;
            } else if (p.type == 4) {
                // 火焰粒子物理更新
                p.vx += (rand.nextFloat() - 0.5f) * 0.8f; // 火苗左右飄動
                p.vy -= 0.1f;
                p.size *= 0.96f; // 燃燒殆盡慢慢變小
                p.g -= 0.025f; // 綠色衰減，從橘黃轉為純紅
                if (p.g < 0)
                    p.g = 0;
            }

            p.life--;
            if (p.life <= 0 || p.size <= 0.5f)
                it.remove();
        }
    }

    private void renderParticles(float globalAlpha, float partialTicks) {
        if (particles.isEmpty())
            return;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        setAdditive(true);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        for (Particle2D p : particles) {
            float alpha = ((float) p.life / p.maxLife) * globalAlpha;
            float px = p.prevX + (p.x - p.prevX) * partialTicks;
            float py = p.prevY + (p.y - p.prevY) * partialTicks;

            // 【修復】: Awakened (Type 1) 是飄動的螢火蟲，和 Champion (Type 4) 一樣不需要動態模糊！
            if (p.type == 1 || p.type == 4) {
                float w = p.size;
                buffer.pos(px - w, py - w, 0).color(p.r, p.g, p.b, alpha).endVertex();
                buffer.pos(px - w, py + w, 0).color(p.r, p.g, p.b, alpha).endVertex();
                buffer.pos(px + w, py + w, 0).color(p.r, p.g, p.b, alpha).endVertex();
                buffer.pos(px + w, py - w, 0).color(p.r, p.g, p.b, alpha).endVertex();
            } else {
                // 原本的動態模糊殘影 (Scholar Type 2, Warden Type 3)
                float vMag = (float) Math.sqrt(p.vx * p.vx + p.vy * p.vy);
                float dirX = (vMag > 0.001f) ? (p.vx / vMag) : 1.0f;
                float dirY = (vMag > 0.001f) ? (p.vy / vMag) : 0.0f;
                float perpX = -dirY;
                float perpY = dirX;

                float w = p.size;
                float h = p.size + vMag * 2.5f;

                float hx1 = px + perpX * w;
                float hy1 = py + perpY * w;
                float hx2 = px - perpX * w;
                float hy2 = py - perpY * w;
                float tx1 = px - dirX * h + perpX * w;
                float ty1 = py - dirY * h + perpY * w;
                float tx2 = px - dirX * h - perpX * w;
                float ty2 = py - dirY * h - perpY * w;

                buffer.pos(hx1, hy1, 0).color(p.r, p.g, p.b, alpha).endVertex();
                buffer.pos(hx2, hy2, 0).color(p.r, p.g, p.b, alpha).endVertex();
                buffer.pos(tx2, ty2, 0).color(p.r, p.g, p.b, 0.0f).endVertex();
                buffer.pos(tx1, ty1, 0).color(p.r, p.g, p.b, 0.0f).endVertex();
            }
        }
        tessellator.draw();
    }
}