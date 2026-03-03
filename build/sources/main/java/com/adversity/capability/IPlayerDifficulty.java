package com.adversity.capability;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 玩家难度设置接口
 */
public interface IPlayerDifficulty {

    /**
     * 获取玩家的难度倍率 (0.0 - 2.0)
     * 1.0 = 正常, 0.5 = 简单, 1.5 = 困难, 2.0 = 噩梦
     */
    float getDifficultyMultiplier();

    /**
     * 设置难度倍率
     */
    void setDifficultyMultiplier(float multiplier);

    /**
     * 获取玩家是否禁用难度系统
     */
    boolean isDifficultyDisabled();

    /**
     * 设置是否禁用难度系统
     */
    void setDifficultyDisabled(boolean disabled);

    /**
     * 获取玩家的击杀数（用于额外难度计算）
     */
    int getKillCount();

    /**
     * 增加击杀数
     */
    void addKill();

    /**
     * 重置击杀数
     */
    void resetKillCount();

    // ==================== 玩家个人缩放设置 ====================

    /**
     * 缩放模式枚举
     */
    enum ScalingMode {
        DEFAULT,      // 使用服务器配置
        LINEAR,       // 线性增长
        EXPONENTIAL,  // 指数增长
        COMPOUND,     // 复合增长
        POLYNOMIAL,   // 多项式增长
        LOGARITHMIC,  // 对数增长
        SIGMOID       // S型曲线
    }

    /**
     * 获取生命值缩放模式
     */
    ScalingMode getHealthScalingMode();

    /**
     * 设置生命值缩放模式
     */
    void setHealthScalingMode(ScalingMode mode);

    /**
     * 获取伤害缩放模式
     */
    ScalingMode getDamageScalingMode();

    /**
     * 设置伤害缩放模式
     */
    void setDamageScalingMode(ScalingMode mode);

    // ==================== 玩家个人难度偏移/锁定/上限 ====================

    /** 获取玩家个人难度偏移 */
    float getPersonalOffset();

    /** 设置玩家个人难度偏移 */
    void setPersonalOffset(float offset);

    /** 增加玩家个人难度偏移 */
    void addPersonalOffset(float amount);

    /** 获取玩家个人难度锁定值 (-1 = 未锁定) */
    float getPersonalLock();

    /** 锁定玩家个人难度 */
    void setPersonalLock(float value);

    /** 解除玩家个人难度锁定 */
    void clearPersonalLock();

    /** 获取玩家个人难度上限 (0 = 无上限) */
    float getPersonalCap();

    /** 设置玩家个人难度上限 */
    void setPersonalCap(float cap);

    /**
     * 序列化
     */
    NBTTagCompound serializeNBT();

    /**
     * 反序列化
     */
    void deserializeNBT(NBTTagCompound nbt);

    // ==================== 玩家个人游玩时间追踪 ====================

    /**
     * 获取玩家的个人游玩时间（tick）
     * 用于替代全局服务器时间计算难度
     */
    long getPlayTime();

    /**
     * 增加玩家的游玩时间
     */
    void addPlayTime(long ticks);

    /**
     * 设置玩家的游玩时间
     */
    void setPlayTime(long ticks);
}
