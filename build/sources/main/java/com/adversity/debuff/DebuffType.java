package com.adversity.debuff;

import com.adversity.client.visual.VisualEffectType;

/**
 * Debuff类型枚举
 * 定义所有自定义debuff效果及其属性
 */
public enum DebuffType {

    /**
     * 黑暗 - 致盲词条施加
     * 效果：视野逐渐收缩，满层时严重影响视野和攻击
     */
    DARKNESS("darkness", 10, 60, VisualEffectType.BLINDING, true) {
        @Override
        public float getEffectStrength(int stacks) {
            // 每层增加10%黑暗程度
            return stacks * 0.1f;
        }
    },

    /**
     * 冻结 - 冰霜词条施加
     * 效果：移动速度降低，满层时短暂完全冻结
     */
    FROST("frost", 8, 40, VisualEffectType.FROZEN, true) {
        @Override
        public float getEffectStrength(int stacks) {
            // 每层降低12.5%移动速度
            return stacks * 0.125f;
        }
    },

    /**
     * 腐蚀 - 凋零词条施加
     * 效果：护甲降低，持续受到伤害
     */
    CORROSION("corrosion", 6, 80, VisualEffectType.DECAY, false) {
        @Override
        public float getEffectStrength(int stacks) {
            // 每层降低约16.7%护甲
            return stacks * 0.167f;
        }
    },

    /**
     * 恐惧 - 恐惧词条施加
     * 效果：被迫后退，视野压迫
     */
    FEAR("fear", 5, 100, VisualEffectType.HORROR, true) {
        @Override
        public float getEffectStrength(int stacks) {
            // 每层20%恐惧强度
            return stacks * 0.2f;
        }
    },

    /**
     * 灼烧 - 烈焰词条施加
     * 效果：持续火焰伤害，满层时爆发
     */
    BURNING("burning", 5, 0, VisualEffectType.BURNING, false) {
        @Override
        public float getEffectStrength(int stacks) {
            // 每层20%灼烧强度
            return stacks * 0.2f;
        }
    },

    /**
     * 血债 - 吸血词条施加
     * 效果：被标记的玩家受到更多吸血
     */
    BLOOD_DEBT("blood_debt", 4, 0, VisualEffectType.BLOOD_MARK, false) {
        @Override
        public float getEffectStrength(int stacks) {
            // 每层25%额外吸血
            return stacks * 0.25f;
        }
    },

    /**
     * 引力锁定 - 引力词条施加
     * 效果：被引力场锁定，拉力增强
     */
    GRAVITY_LOCK("gravity_lock", 3, 0, VisualEffectType.GRAVITY_DISTORT, false) {
        @Override
        public float getEffectStrength(int stacks) {
            // 每层约33%额外拉力
            return stacks * 0.333f;
        }
    },

    /**
     * 虚空凝视 - 高级词条施加
     * 效果：被虚空盯上，持续受到穿透伤害
     */
    VOID_MARK("void_mark", 3, 120, VisualEffectType.VOID_GAZE, true) {
        @Override
        public float getEffectStrength(int stacks) {
            return stacks * 0.333f;
        }
    };

    private final String id;
    private final int maxStacks;
    private final int decayInterval;  // 多少tick减少1层（0表示不自然衰减）
    private final VisualEffectType visualEffect;
    private final boolean hasVisualEffect;

    DebuffType(String id, int maxStacks, int decayInterval, VisualEffectType visualEffect, boolean hasVisualEffect) {
        this.id = id;
        this.maxStacks = maxStacks;
        this.decayInterval = decayInterval;
        this.visualEffect = visualEffect;
        this.hasVisualEffect = hasVisualEffect;
    }

    public String getId() {
        return id;
    }

    public int getMaxStacks() {
        return maxStacks;
    }

    public int getDecayInterval() {
        return decayInterval;
    }

    public VisualEffectType getVisualEffect() {
        return visualEffect;
    }

    public boolean hasVisualEffect() {
        return hasVisualEffect;
    }

    /**
     * 获取当前层数对应的效果强度（0.0-1.0）
     */
    public abstract float getEffectStrength(int stacks);

    public static DebuffType fromId(String id) {
        for (DebuffType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
