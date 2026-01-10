package com.adversity.client.render;

import com.adversity.affix.AffixRegistry;
import com.adversity.affix.IAffix;
import com.adversity.client.ClientAdversityCache;
import com.adversity.client.TierColorSystem;
import com.adversity.config.AdversityConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 精英怪物信息渲染器
 * 支持大体型怪物动态缩放
 */
@SideOnly(Side.CLIENT)
public class EntityInfoRenderer {

    // 基础渲染参数
    private static final float BASE_SCALE = 0.025f;
    private static final float MIN_SCALE = 0.02f;
    private static final float MAX_SCALE = 0.08f;

    // 血条基础尺寸
    private static final float BASE_HEALTH_BAR_WIDTH = 80.0f;
    private static final float BASE_HEALTH_BAR_HEIGHT = 6.0f;

    // 词条图标尺寸
    private static final float AFFIX_ICON_SIZE = 12.0f;
    private static final float AFFIX_ICON_SPACING = 2.0f;

    /**
     * 渲染实体信息（等级、血条、词条）
     * @param entity 目标实体
     * @param data 缓存的实体数据
     * @param x 渲染X偏移
     * @param y 渲染Y偏移
     * @param z 渲染Z偏移
     */
    public static void render(EntityLiving entity, ClientAdversityCache.CachedEntityData data,
                               double x, double y, double z) {
        if (!AdversityConfig.clientSettings.enableMobTierDisplay) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fontRenderer = mc.fontRenderer;

        // 计算实体尺寸系数（用于缩放UI）
        float entityScale = calculateEntityScale(entity);
        float renderScale = BASE_SCALE * entityScale;
        renderScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, renderScale));

        // 计算UI宽度（根据实体宽度）
        float uiWidth = Math.max(BASE_HEALTH_BAR_WIDTH, entity.width * 40);
        float healthBarHeight = BASE_HEALTH_BAR_HEIGHT * entityScale;
        healthBarHeight = Math.max(4.0f, Math.min(12.0f, healthBarHeight));

        // 计算渲染位置（在实体上方，根据实体大小调整）
        float heightOffset = entity.height + 0.3f + (entityScale - 1.0f) * 0.5f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + heightOffset, z);
        GlStateManager.glNormal3f(0.0f, 1.0f, 0.0f);

        // 面向玩家
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1)
            * mc.getRenderManager().playerViewX, 1.0f, 0.0f, 0.0f);

        // 应用缩放
        GlStateManager.scale(-renderScale, -renderScale, renderScale);

        // 渲染状态设置
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );

        // 渲染顺序：血条 -> 等级名 -> 词条
        float currentY = 0;

        // 1. 渲染血条
        GlStateManager.disableTexture2D();
        currentY = renderHealthBar(entity, data, uiWidth, healthBarHeight, currentY);
        GlStateManager.enableTexture2D();

        // 2. 渲染等级名称
        currentY = renderTierName(fontRenderer, data, currentY);

        // 3. 渲染词条
        if (AdversityConfig.clientSettings.enableAffixIcons) {
            renderAffixes(fontRenderer, data, currentY);
        }

        // 恢复渲染状态
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    /**
     * 计算实体缩放系数
     * 大型怪物使用更大的UI
     */
    private static float calculateEntityScale(EntityLiving entity) {
        // 使用实体的最大尺寸（宽度和高度）
        float maxDimension = Math.max(entity.width, entity.height);

        // 基础实体（1x1）系数为1.0
        // 大型实体（如末影龙）需要更大的UI
        if (maxDimension <= 1.0f) {
            return 1.0f;
        } else if (maxDimension <= 2.0f) {
            return 1.0f + (maxDimension - 1.0f) * 0.3f;
        } else if (maxDimension <= 4.0f) {
            return 1.3f + (maxDimension - 2.0f) * 0.4f;
        } else {
            // 超大型怪物
            return 2.1f + (maxDimension - 4.0f) * 0.2f;
        }
    }

    /**
     * 渲染血条
     * @return 下一个元素的Y位置
     */
    private static float renderHealthBar(EntityLiving entity, ClientAdversityCache.CachedEntityData data,
                                          float width, float height, float yOffset) {
        float healthPercent = entity.getHealth() / entity.getMaxHealth();
        healthPercent = Math.max(0, Math.min(1, healthPercent));

        float halfWidth = width / 2;
        float padding = 2.0f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 外框（黑色描边）
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(-halfWidth - padding, yOffset - padding, 0).color(0, 0, 0, 200).endVertex();
        buffer.pos(-halfWidth - padding, yOffset + height + padding, 0).color(0, 0, 0, 200).endVertex();
        buffer.pos(halfWidth + padding, yOffset + height + padding, 0).color(0, 0, 0, 200).endVertex();
        buffer.pos(halfWidth + padding, yOffset - padding, 0).color(0, 0, 0, 200).endVertex();
        tessellator.draw();

        // 血条背景（深色）
        int[] bgColor = getDarkenedColor(TierColorSystem.getHealthBarColor(data.tier), 0.3f);
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(-halfWidth, yOffset, 0).color(bgColor[0], bgColor[1], bgColor[2], 180).endVertex();
        buffer.pos(-halfWidth, yOffset + height, 0).color(bgColor[0], bgColor[1], bgColor[2], 180).endVertex();
        buffer.pos(halfWidth, yOffset + height, 0).color(bgColor[0], bgColor[1], bgColor[2], 180).endVertex();
        buffer.pos(halfWidth, yOffset, 0).color(bgColor[0], bgColor[1], bgColor[2], 180).endVertex();
        tessellator.draw();

        // 当前血量（渐变效果）
        int[] color = TierColorSystem.getHealthBarColor(data.tier);
        int[] lightColor = getLightenedColor(color, 1.3f);
        float currentWidth = width * healthPercent;

        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        // 上部分亮色
        buffer.pos(-halfWidth, yOffset, 0).color(lightColor[0], lightColor[1], lightColor[2], 230).endVertex();
        buffer.pos(-halfWidth, yOffset + height * 0.4f, 0).color(color[0], color[1], color[2], 230).endVertex();
        buffer.pos(-halfWidth + currentWidth, yOffset + height * 0.4f, 0).color(color[0], color[1], color[2], 230).endVertex();
        buffer.pos(-halfWidth + currentWidth, yOffset, 0).color(lightColor[0], lightColor[1], lightColor[2], 230).endVertex();
        tessellator.draw();

        // 下部分正常色
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(-halfWidth, yOffset + height * 0.4f, 0).color(color[0], color[1], color[2], 230).endVertex();
        buffer.pos(-halfWidth, yOffset + height, 0).color(bgColor[0], bgColor[1], bgColor[2], 230).endVertex();
        buffer.pos(-halfWidth + currentWidth, yOffset + height, 0).color(bgColor[0], bgColor[1], bgColor[2], 230).endVertex();
        buffer.pos(-halfWidth + currentWidth, yOffset + height * 0.4f, 0).color(color[0], color[1], color[2], 230).endVertex();
        tessellator.draw();

        // 血量百分比文字（在血条内部）
        GlStateManager.enableTexture2D();
        Minecraft mc = Minecraft.getMinecraft();
        String healthText = String.format("%.0f/%.0f", entity.getHealth(), entity.getMaxHealth());
        int textWidth = mc.fontRenderer.getStringWidth(healthText);
        float textScale = Math.min(1.0f, (height - 2) / 8.0f);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, yOffset + height / 2 - 4 * textScale, 0);
        GlStateManager.scale(textScale, textScale, 1);
        mc.fontRenderer.drawString(healthText, -textWidth / 2, 0, 0xFFFFFF, true);
        GlStateManager.popMatrix();
        GlStateManager.disableTexture2D();

        return yOffset + height + 4;
    }

    /**
     * 渲染等级名称
     * @return 下一个元素的Y位置
     */
    private static float renderTierName(FontRenderer fontRenderer, ClientAdversityCache.CachedEntityData data, float yOffset) {
        TextFormatting tierColor = TierColorSystem.getTextColor(data.tier);
        String tierName = getTierName(data.tier);

        // 使用装饰符号
        String prefix = getDecoratorPrefix(data.tier);
        String suffix = getDecoratorSuffix(data.tier);
        String tierStr = tierColor + prefix + " " + tierName + " " + suffix;

        int tierWidth = fontRenderer.getStringWidth(tierStr);

        // 绘制带阴影的文本
        fontRenderer.drawString(tierStr, -tierWidth / 2, (int)yOffset, 0xFFFFFF, true);

        return yOffset + 12;
    }

    /**
     * 渲染词条列表
     */
    private static void renderAffixes(FontRenderer fontRenderer, ClientAdversityCache.CachedEntityData data, float yOffset) {
        List<ResourceLocation> affixIds = data.affixIds;
        if (affixIds.isEmpty()) return;

        // 计算总宽度
        int totalWidth = 0;
        for (ResourceLocation id : affixIds) {
            IAffix affix = AffixRegistry.getAffix(id);
            if (affix != null) {
                totalWidth += fontRenderer.getStringWidth(affix.getDisplayName()) + 8;
            }
        }
        totalWidth -= 8; // 去掉最后一个间距

        // 居中绘制
        float currentX = -totalWidth / 2.0f;

        for (ResourceLocation id : affixIds) {
            IAffix affix = AffixRegistry.getAffix(id);
            if (affix != null) {
                TextFormatting color = getAffixColor(affix);
                String text = color + affix.getDisplayName();
                fontRenderer.drawString(text, (int)currentX, (int)yOffset, 0xFFFFFF, true);
                currentX += fontRenderer.getStringWidth(text) + 8;
            }
        }
    }

    /**
     * 获取等级名称
     */
    private static String getTierName(int tier) {
        if (tier <= 0 || tier > 10) {
            return "T" + tier;
        }
        String key = "adversity.tier." + tier;
        String translated = I18n.format(key);
        if (translated.equals(key)) {
            return getDefaultTierName(tier);
        }
        return translated;
    }

    /**
     * 默认等级名称
     */
    private static String getDefaultTierName(int tier) {
        switch (tier) {
            case 1: return "Elite";
            case 2: return "Rare";
            case 3: return "Veteran";
            case 4: return "Epic";
            case 5: return "Legendary";
            case 6: return "Mythic";
            case 7: return "Ancient";
            case 8: return "Void";
            case 9: return "Abyssal";
            case 10: return "Terminus";
            default: return "T" + tier;
        }
    }

    /**
     * 获取等级装饰前缀
     */
    private static String getDecoratorPrefix(int tier) {
        if (tier <= 3) return "★";
        if (tier <= 6) return "✦";
        if (tier <= 8) return "❖";
        return "☆";
    }

    /**
     * 获取等级装饰后缀
     */
    private static String getDecoratorSuffix(int tier) {
        if (tier <= 3) return "★";
        if (tier <= 6) return "✦";
        if (tier <= 8) return "❖";
        return "☆";
    }

    /**
     * 根据词条类型获取颜色
     */
    private static TextFormatting getAffixColor(IAffix affix) {
        switch (affix.getType()) {
            case OFFENSIVE:
                return TextFormatting.RED;
            case DEFENSIVE:
                return TextFormatting.AQUA;
            case UTILITY:
                return TextFormatting.YELLOW;
            case SPECIAL:
                return TextFormatting.LIGHT_PURPLE;
            default:
                return TextFormatting.GRAY;
        }
    }

    /**
     * 变暗颜色
     */
    private static int[] getDarkenedColor(int[] color, float factor) {
        return new int[] {
            (int)(color[0] * factor),
            (int)(color[1] * factor),
            (int)(color[2] * factor)
        };
    }

    /**
     * 变亮颜色
     */
    private static int[] getLightenedColor(int[] color, float factor) {
        return new int[] {
            Math.min(255, (int)(color[0] * factor)),
            Math.min(255, (int)(color[1] * factor)),
            Math.min(255, (int)(color[2] * factor))
        };
    }
}
