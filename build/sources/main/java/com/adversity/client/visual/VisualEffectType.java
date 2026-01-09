package com.adversity.client.visual;

/**
 * 视觉效果类型枚举
 * 定义所有可以通过屏幕覆盖层渲染的视觉效果
 */
public enum VisualEffectType {

    /**
     * 致盲效果 - 屏幕周围逐渐变暗，中心保留小范围视野
     * 动态收缩的黑色边缘
     */
    BLINDING(0, "blinding", 0.0f, 0.0f, 0.0f, 0.95f),

    /**
     * 恐惧效果 - 黑色呼吸脉冲，屏幕边缘有压迫感
     * 带有心跳般的节奏
     */
    HORROR(1, "horror", 0.0f, 0.0f, 0.0f, 0.7f),

    /**
     * 冰冻效果 - 屏幕边缘出现冰晶纹理，中心变蓝
     */
    FROZEN(2, "frozen", 0.6f, 0.85f, 1.0f, 0.4f),

    /**
     * 腐蚀效果 - 屏幕边缘有腐烂/凋零的纹理，偏紫黑色
     */
    DECAY(3, "decay", 0.2f, 0.0f, 0.15f, 0.5f),

    /**
     * 灼烧效果 - 屏幕边缘火焰红光，热浪扭曲
     */
    BURNING(4, "burning", 1.0f, 0.3f, 0.0f, 0.35f),

    /**
     * 虚空凝视 - 深紫色边缘，中心有扭曲效果
     */
    VOID_GAZE(5, "void_gaze", 0.15f, 0.0f, 0.2f, 0.6f),

    /**
     * 引力扭曲 - 画面边缘轻微扭曲变形
     */
    GRAVITY_DISTORT(6, "gravity_distort", 0.1f, 0.0f, 0.1f, 0.25f),

    /**
     * 吸血标记 - 血红色边缘闪烁
     */
    BLOOD_MARK(7, "blood_mark", 0.8f, 0.0f, 0.0f, 0.4f);

    private final int id;
    private final String name;
    private final float red;
    private final float green;
    private final float blue;
    private final float baseAlpha;

    VisualEffectType(int id, String name, float red, float green, float blue, float baseAlpha) {
        this.id = id;
        this.name = name;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.baseAlpha = baseAlpha;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public float getRed() {
        return red;
    }

    public float getGreen() {
        return green;
    }

    public float getBlue() {
        return blue;
    }

    public float getBaseAlpha() {
        return baseAlpha;
    }

    public static VisualEffectType fromId(int id) {
        for (VisualEffectType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }

    public static VisualEffectType fromName(String name) {
        for (VisualEffectType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
