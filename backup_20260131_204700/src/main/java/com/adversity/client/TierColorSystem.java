package com.adversity.client;

import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Tier颜色系统 - 统一管理所有Tier相关的颜色
 *
 * 设计参考L2Hostility的颜色方案:
 * - 低级怪物使用冷色调（绿、蓝）
 * - 中级怪物使用暖色调（金、橙）
 * - 高级怪物使用深色调（紫、红）
 * - 终焉使用特殊效果色
 */
@SideOnly(Side.CLIENT)
public class TierColorSystem {

    // ==================== 文字颜色 (TextFormatting) ====================

    /**
     * 获取Tier对应的文字颜色
     */
    public static TextFormatting getTextColor(int tier) {
        switch (tier) {
            case 1: return TextFormatting.GREEN;        // 精英 - 绿色
            case 2: return TextFormatting.BLUE;         // 稀有 - 蓝色
            case 3: return TextFormatting.AQUA;         // 精锐 - 青色
            case 4: return TextFormatting.LIGHT_PURPLE; // 史诗 - 淡紫
            case 5: return TextFormatting.GOLD;         // 传说 - 金色
            case 6: return TextFormatting.RED;          // 神话 - 红色
            case 7: return TextFormatting.DARK_PURPLE;  // 远古 - 深紫
            case 8: return TextFormatting.DARK_AQUA;    // 虚空 - 深青
            case 9: return TextFormatting.DARK_RED;     // 深渊 - 深红
            case 10: return TextFormatting.DARK_GRAY;   // 终焉 - 暗灰(带特效)
            default: return TextFormatting.WHITE;
        }
    }

    // ==================== RGB颜色 (0xRRGGBB) ====================

    /**
     * 获取Tier对应的RGB颜色值
     */
    public static int getRGB(int tier) {
        switch (tier) {
            case 1: return 0x55FF55;   // 精英 - 绿色
            case 2: return 0x5555FF;   // 稀有 - 蓝色
            case 3: return 0x55FFFF;   // 精锐 - 青色
            case 4: return 0xFF55FF;   // 史诗 - 紫色
            case 5: return 0xFFAA00;   // 传说 - 金色
            case 6: return 0xFF5555;   // 神话 - 红色
            case 7: return 0xAA00AA;   // 远古 - 深紫
            case 8: return 0x00AAAA;   // 虚空 - 深青
            case 9: return 0xAA0000;   // 深渊 - 深红
            case 10: return 0x550055;  // 终焉 - 暗紫红
            default: return 0xFFFFFF;
        }
    }

    /**
     * 获取Tier对应的RGB分量 [R, G, B]
     */
    public static int[] getRGBComponents(int tier) {
        int rgb = getRGB(tier);
        return new int[] {
            (rgb >> 16) & 0xFF,
            (rgb >> 8) & 0xFF,
            rgb & 0xFF
        };
    }

    // ==================== 血条颜色 ====================

    /**
     * 获取血条颜色 [R, G, B]
     */
    public static int[] getHealthBarColor(int tier) {
        switch (tier) {
            case 1: return new int[]{85, 255, 85};      // 精英 - 绿色
            case 2: return new int[]{85, 85, 255};      // 稀有 - 蓝色
            case 3: return new int[]{85, 255, 255};     // 精锐 - 青色
            case 4: return new int[]{255, 85, 255};     // 史诗 - 紫色
            case 5: return new int[]{255, 170, 0};      // 传说 - 金色
            case 6: return new int[]{255, 85, 85};      // 神话 - 红色
            case 7: return new int[]{170, 0, 170};      // 远古 - 深紫
            case 8: return new int[]{0, 170, 170};      // 虚空 - 深青
            case 9: return new int[]{170, 0, 0};        // 深渊 - 深红
            case 10: return new int[]{85, 0, 85};       // 终焉 - 暗紫
            default: return new int[]{255, 255, 255};
        }
    }

    // ==================== 难度区域颜色 ====================

    /**
     * 根据难度值获取颜色（用于区域难度显示）
     * 难度 0-100 映射到 绿色→黄色→红色→紫色
     */
    public static int getDifficultyColor(float difficulty) {
        if (difficulty <= 0) {
            return 0x55FF55;  // 绿色 - 安全
        } else if (difficulty < 20) {
            // 绿色 → 黄色
            float t = difficulty / 20f;
            return interpolateColor(0x55FF55, 0xFFFF55, t);
        } else if (difficulty < 50) {
            // 黄色 → 橙色
            float t = (difficulty - 20) / 30f;
            return interpolateColor(0xFFFF55, 0xFFAA00, t);
        } else if (difficulty < 80) {
            // 橙色 → 红色
            float t = (difficulty - 50) / 30f;
            return interpolateColor(0xFFAA00, 0xFF5555, t);
        } else {
            // 红色 → 深紫
            float t = Math.min(1, (difficulty - 80) / 20f);
            return interpolateColor(0xFF5555, 0xAA00AA, t);
        }
    }

    /**
     * 获取难度等级描述
     */
    public static String getDifficultyLabel(float difficulty) {
        if (difficulty <= 5) return "安全";
        if (difficulty <= 15) return "普通";
        if (difficulty <= 30) return "危险";
        if (difficulty <= 50) return "致命";
        if (difficulty <= 70) return "噩梦";
        if (difficulty <= 90) return "地狱";
        return "终焉";
    }

    /**
     * 获取难度等级对应的TextFormatting
     */
    public static TextFormatting getDifficultyTextColor(float difficulty) {
        if (difficulty <= 5) return TextFormatting.GREEN;
        if (difficulty <= 15) return TextFormatting.YELLOW;
        if (difficulty <= 30) return TextFormatting.GOLD;
        if (difficulty <= 50) return TextFormatting.RED;
        if (difficulty <= 70) return TextFormatting.DARK_RED;
        if (difficulty <= 90) return TextFormatting.DARK_PURPLE;
        return TextFormatting.LIGHT_PURPLE;
    }

    // ==================== 工具方法 ====================

    /**
     * 颜色插值
     */
    private static int interpolateColor(int color1, int color2, float t) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (r << 16) | (g << 8) | b;
    }

    /**
     * 将RGB转换为带透明度的ARGB
     */
    public static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }
}
