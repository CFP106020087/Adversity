package com.adversity.capability;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 玩家难度设置实现
 */
public class PlayerDifficulty implements IPlayerDifficulty {

    private float difficultyMultiplier = 1.0f;
    private boolean difficultyDisabled = false;
    private int killCount = 0;
    private ScalingMode healthScalingMode = ScalingMode.DEFAULT;
    private ScalingMode damageScalingMode = ScalingMode.DEFAULT;
    private long playTime = 0;
    private float personalOffset = 0.0f;
    private float personalLock = -1.0f; // -1 = 未锁定
    private float personalCap = 0.0f; // 0 = 无上限

    @Override
    public float getDifficultyMultiplier() {
        return difficultyMultiplier;
    }

    @Override
    public void setDifficultyMultiplier(float multiplier) {
        this.difficultyMultiplier = Math.max(0.0f, Math.min(6.0f, multiplier));
    }

    @Override
    public boolean isDifficultyDisabled() {
        return difficultyDisabled;
    }

    @Override
    public void setDifficultyDisabled(boolean disabled) {
        this.difficultyDisabled = disabled;
    }

    @Override
    public int getKillCount() {
        return killCount;
    }

    @Override
    public void addKill() {
        this.killCount++;
    }

    @Override
    public void resetKillCount() {
        this.killCount = 0;
    }

    @Override
    public ScalingMode getHealthScalingMode() {
        return healthScalingMode;
    }

    @Override
    public void setHealthScalingMode(ScalingMode mode) {
        this.healthScalingMode = mode != null ? mode : ScalingMode.DEFAULT;
    }

    @Override
    public ScalingMode getDamageScalingMode() {
        return damageScalingMode;
    }

    @Override
    public void setDamageScalingMode(ScalingMode mode) {
        this.damageScalingMode = mode != null ? mode : ScalingMode.DEFAULT;
    }

    // ==================== 玩家个人游玩时间追踪 ====================

    @Override
    public long getPlayTime() {
        return playTime;
    }

    @Override
    public void addPlayTime(long ticks) {
        this.playTime += ticks;
    }

    @Override
    public void setPlayTime(long ticks) {
        this.playTime = Math.max(0, ticks);
    }

    // ==================== 玩家个人难度偏移/锁定/上限 ====================

    @Override
    public float getPersonalOffset() {
        return personalOffset;
    }

    @Override
    public void setPersonalOffset(float offset) {
        this.personalOffset = offset;
    }

    @Override
    public void addPersonalOffset(float amount) {
        this.personalOffset += amount;
    }

    @Override
    public float getPersonalLock() {
        return personalLock;
    }

    @Override
    public void setPersonalLock(float value) {
        this.personalLock = value;
    }

    @Override
    public void clearPersonalLock() {
        this.personalLock = -1.0f;
    }

    @Override
    public float getPersonalCap() {
        return personalCap;
    }

    @Override
    public void setPersonalCap(float cap) {
        this.personalCap = Math.max(0, cap);
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setFloat("difficultyMultiplier", difficultyMultiplier);
        nbt.setBoolean("difficultyDisabled", difficultyDisabled);
        nbt.setInteger("killCount", killCount);
        nbt.setString("healthScalingMode", healthScalingMode.name());
        nbt.setString("damageScalingMode", damageScalingMode.name());
        nbt.setLong("playTime", playTime);
        nbt.setFloat("personalOffset", personalOffset);
        nbt.setFloat("personalLock", personalLock);
        nbt.setFloat("personalCap", personalCap);
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        this.difficultyMultiplier = nbt.hasKey("difficultyMultiplier") ? nbt.getFloat("difficultyMultiplier") : 1.0f;
        this.difficultyDisabled = nbt.getBoolean("difficultyDisabled");
        this.killCount = nbt.getInteger("killCount");
        this.playTime = nbt.getLong("playTime");
        this.personalOffset = nbt.getFloat("personalOffset");
        this.personalLock = nbt.hasKey("personalLock") ? nbt.getFloat("personalLock") : -1.0f;
        this.personalCap = nbt.getFloat("personalCap");

        // 读取缩放模式，兼容旧数据
        if (nbt.hasKey("healthScalingMode")) {
            try {
                this.healthScalingMode = ScalingMode.valueOf(nbt.getString("healthScalingMode"));
            } catch (IllegalArgumentException e) {
                this.healthScalingMode = ScalingMode.DEFAULT;
            }
        } else {
            this.healthScalingMode = ScalingMode.DEFAULT;
        }

        if (nbt.hasKey("damageScalingMode")) {
            try {
                this.damageScalingMode = ScalingMode.valueOf(nbt.getString("damageScalingMode"));
            } catch (IllegalArgumentException e) {
                this.damageScalingMode = ScalingMode.DEFAULT;
            }
        } else {
            this.damageScalingMode = ScalingMode.DEFAULT;
        }
    }
}
