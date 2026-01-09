package com.adversity.client.visual;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.EnumMap;
import java.util.Map;

/**
 * 客户端视觉效果管理器
 * 跟踪当前激活的所有视觉效果及其强度/持续时间
 */
@SideOnly(Side.CLIENT)
public class VisualEffectManager {

    /**
     * 存储每种效果的数据
     */
    private static final Map<VisualEffectType, EffectData> activeEffects = new EnumMap<>(VisualEffectType.class);

    /**
     * 全局时间计数器（用于动画）
     */
    private static long globalTick = 0;

    /**
     * 添加或更新视觉效果
     *
     * @param type 效果类型
     * @param duration 持续时间（tick）
     * @param intensity 强度 (0.0 - 1.0)
     * @param sourceEntityId 来源实体ID（用于多来源叠加）
     */
    public static void addEffect(VisualEffectType type, int duration, float intensity, int sourceEntityId) {
        if (type == null) return;

        EffectData existing = activeEffects.get(type);
        if (existing != null) {
            // 如果已存在，取较长持续时间和较高强度
            existing.duration = Math.max(existing.duration, duration);
            existing.intensity = Math.max(existing.intensity, intensity);
            existing.sourceEntityId = sourceEntityId;
        } else {
            activeEffects.put(type, new EffectData(duration, intensity, sourceEntityId));
        }
    }

    /**
     * 移除指定效果
     */
    public static void removeEffect(VisualEffectType type) {
        activeEffects.remove(type);
    }

    /**
     * 清除所有效果
     */
    public static void clearAllEffects() {
        activeEffects.clear();
    }

    /**
     * 清除来自特定实体的效果
     */
    public static void clearEffectsFromEntity(int entityId) {
        activeEffects.entrySet().removeIf(entry -> entry.getValue().sourceEntityId == entityId);
    }

    /**
     * 每tick更新（递减持续时间，移除过期效果）
     */
    public static void tick() {
        globalTick++;

        activeEffects.entrySet().removeIf(entry -> {
            EffectData data = entry.getValue();
            data.duration--;

            // 淡出效果
            if (data.duration <= 20) {
                data.fadeProgress = 1.0f - (data.duration / 20.0f);
            }

            return data.duration <= 0;
        });
    }

    /**
     * 获取指定效果的当前数据
     */
    public static EffectData getEffectData(VisualEffectType type) {
        return activeEffects.get(type);
    }

    /**
     * 检查效果是否激活
     */
    public static boolean isEffectActive(VisualEffectType type) {
        return activeEffects.containsKey(type);
    }

    /**
     * 获取所有激活的效果
     */
    public static Map<VisualEffectType, EffectData> getActiveEffects() {
        return activeEffects;
    }

    /**
     * 获取全局时间计数器（用于动画计算）
     */
    public static long getGlobalTick() {
        return globalTick;
    }

    /**
     * 是否有任何效果激活
     */
    public static boolean hasAnyEffect() {
        return !activeEffects.isEmpty();
    }

    /**
     * 效果数据类
     */
    public static class EffectData {
        public int duration;           // 剩余持续时间（tick）
        public float intensity;        // 强度 (0.0 - 1.0)
        public int sourceEntityId;     // 来源实体ID
        public float fadeProgress;     // 淡出进度 (0.0 - 1.0)
        public long startTick;         // 效果开始时间

        public EffectData(int duration, float intensity, int sourceEntityId) {
            this.duration = duration;
            this.intensity = Math.min(1.0f, Math.max(0.0f, intensity));
            this.sourceEntityId = sourceEntityId;
            this.fadeProgress = 0.0f;
            this.startTick = globalTick;
        }

        /**
         * 获取当前显示强度（考虑淡出）
         */
        public float getDisplayIntensity() {
            return intensity * (1.0f - fadeProgress);
        }

        /**
         * 获取动画相位（0.0 - 1.0循环）
         */
        public float getAnimationPhase(float period) {
            long elapsed = globalTick - startTick;
            return (elapsed % (long) period) / period;
        }
    }
}
