package com.adversity.difficulty;

/**
 * 数值缩放公式系统
 * 支持多种增长模式以适应不同模组包的数值生态
 *
 * 设计理念：
 * - 轻量包：原版体验，线性增长
 * - 中型包：平稳的指数增长
 * - 重型包：快速的多项式/复合增长
 * - 魔改包：可配置的极端数值
 */
public class ScalingFormula {

    /**
     * 缩放模式枚举
     */
    public enum ScalingMode {
        /**
         * 线性增长: base + difficulty × rate
         * 特点: 增长稳定可预测，适合原版体验
         * 示例(rate=0.15): diff10→2.5x, diff30→5.5x
         */
        LINEAR,

        /**
         * 指数增长: base × e^(difficulty × rate)
         * 特点: 中后期快速增长，适合中型模组包
         * 示例(rate=0.15): diff10→4.5x, diff30→90x
         */
        EXPONENTIAL,

        /**
         * 复合增长: base × (1 + rate)^difficulty
         * 特点: 类似复利，增长曲线平滑
         * 示例(rate=0.15): diff10→4.0x, diff30→66x
         */
        COMPOUND,

        /**
         * 多项式增长: base + difficulty^power × rate
         * 特点: 高难度区间爆发式增长
         * 示例(rate=0.1, power=2): diff10→11x, diff30→91x
         * 示例(rate=0.01, power=3): diff10→11x, diff30→271x
         */
        POLYNOMIAL,

        /**
         * 对数增长: base + ln(1 + difficulty) × rate
         * 特点: 前期快后期慢，适合限制上限
         * 示例(rate=1.0): diff10→3.4x, diff30→4.4x
         */
        LOGARITHMIC,

        /**
         * S曲线增长: base + maxBonus × (1 - e^(-difficulty × rate))
         * 特点: 平滑过渡到上限，无突变
         * 示例(rate=0.1, max=10): diff10→6.3x, diff30→9.5x
         */
        SIGMOID,

        /**
         * 分段线性: 不同难度区间使用不同斜率
         * 特点: 精确控制各阶段增长速度
         * 需要额外配置 breakpoints
         */
        PIECEWISE
    }

    /**
     * 计算缩放后的数值
     *
     * @param mode       缩放模式
     * @param base       基础值 (通常为1.0)
     * @param difficulty 难度值
     * @param rate       增长率
     * @param power      幂次 (仅POLYNOMIAL模式使用)
     * @param maxValue   最大值上限 (0或负数表示无上限)
     * @return 缩放后的乘数
     */
    public static double calculate(ScalingMode mode, double base, double difficulty,
                                   double rate, double power, double maxValue) {
        double result;

        switch (mode) {
            case LINEAR:
                // base + difficulty × rate
                result = base + difficulty * rate;
                break;

            case EXPONENTIAL:
                // base × e^(difficulty × rate)
                result = base * Math.exp(difficulty * rate);
                break;

            case COMPOUND:
                // base × (1 + rate)^difficulty
                result = base * Math.pow(1.0 + rate, difficulty);
                break;

            case POLYNOMIAL:
                // base + difficulty^power × rate
                result = base + Math.pow(difficulty, power) * rate;
                break;

            case LOGARITHMIC:
                // base + ln(1 + difficulty) × rate
                result = base + Math.log(1.0 + difficulty) * rate;
                break;

            case SIGMOID:
                // base + maxBonus × (1 - e^(-difficulty × rate))
                // 这里 maxValue 作为 sigmoid 的渐近上限
                double maxBonus = (maxValue > base) ? (maxValue - base) : rate * 100;
                result = base + maxBonus * (1.0 - Math.exp(-difficulty * rate));
                break;

            case PIECEWISE:
                // 分段线性，使用默认断点
                result = calculatePiecewise(base, difficulty, rate);
                break;

            default:
                result = base + difficulty * rate;
        }

        // 应用上限
        if (maxValue > 0 && result > maxValue) {
            result = maxValue;
        }

        // 确保最小值为 base
        return Math.max(result, base);
    }

    /**
     * 简化版本，使用默认power=2
     */
    public static double calculate(ScalingMode mode, double base, double difficulty,
                                   double rate, double maxValue) {
        return calculate(mode, base, difficulty, rate, 2.0, maxValue);
    }

    /**
     * 分段线性计算
     * 默认断点: 0-5 (×1), 5-15 (×1.5), 15-30 (×2), 30+ (×3)
     */
    private static double calculatePiecewise(double base, double difficulty, double rate) {
        double result = base;

        if (difficulty <= 5) {
            result += difficulty * rate;
        } else if (difficulty <= 15) {
            result += 5 * rate;
            result += (difficulty - 5) * rate * 1.5;
        } else if (difficulty <= 30) {
            result += 5 * rate;
            result += 10 * rate * 1.5;
            result += (difficulty - 15) * rate * 2.0;
        } else {
            result += 5 * rate;
            result += 10 * rate * 1.5;
            result += 15 * rate * 2.0;
            result += (difficulty - 30) * rate * 3.0;
        }

        return result;
    }

    /**
     * 从字符串解析缩放模式
     */
    public static ScalingMode parseMode(String modeName) {
        try {
            return ScalingMode.valueOf(modeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ScalingMode.COMPOUND; // 默认使用复合增长
        }
    }

    /**
     * 获取模式的描述
     */
    public static String getModeDescription(ScalingMode mode) {
        switch (mode) {
            case LINEAR:
                return "Linear (base + diff × rate)";
            case EXPONENTIAL:
                return "Exponential (base × e^(diff × rate))";
            case COMPOUND:
                return "Compound (base × (1+rate)^diff)";
            case POLYNOMIAL:
                return "Polynomial (base + diff^power × rate)";
            case LOGARITHMIC:
                return "Logarithmic (base + ln(1+diff) × rate)";
            case SIGMOID:
                return "Sigmoid (smooth cap)";
            case PIECEWISE:
                return "Piecewise Linear";
            default:
                return "Unknown";
        }
    }

    /**
     * 预览不同难度下的缩放值（用于调试/配置界面）
     */
    public static String generatePreview(ScalingMode mode, double base, double rate,
                                         double power, double maxValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("Scaling Preview (").append(mode).append("):\n");

        int[] testDifficulties = {5, 10, 15, 20, 25, 30, 40, 50};
        for (int diff : testDifficulties) {
            double value = calculate(mode, base, diff, rate, power, maxValue);
            sb.append(String.format("  Diff %2d: %.2fx\n", diff, value));
        }

        return sb.toString();
    }
}
