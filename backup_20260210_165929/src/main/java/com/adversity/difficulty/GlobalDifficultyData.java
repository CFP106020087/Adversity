package com.adversity.difficulty;

import com.adversity.Adversity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * 全局难度偏移管理器
 * 存储全局/区域难度偏移，用于OP手动调整世界难度
 * 
 * 设计理念：
 * - 全局偏移(globalOffset): 影响整个世界的难度基准
 * - 难度锁定(difficultyLock): 锁定为固定难度值
 * - 最大难度上限(maxDifficultyCap): 限制难度最大值
 */
public class GlobalDifficultyData extends WorldSavedData {

    private static final String DATA_NAME = Adversity.MODID + "_global_difficulty";

    // 全局难度偏移（加法）
    private float globalOffset = 0.0f;

    // 难度锁定值（-1表示不锁定）
    private float difficultyLock = -1.0f;

    // 最大难度上限（0表示无上限）
    private float maxDifficultyCap = 0.0f;

    // 全局难度倍率（乘法，1.0为标准）
    private float globalMultiplier = 1.0f;

    // 是否启用全局难度系统
    private boolean enabled = true;

    // 是否永久禁用 (通过 /adversity disable 设置，不可逆)
    private boolean permanentlyDisabled = false;

    public GlobalDifficultyData() {
        super(DATA_NAME);
    }

    public GlobalDifficultyData(String name) {
        super(name);
    }

    /**
     * 获取世界的全局难度数据
     */
    public static GlobalDifficultyData get(World world) {
        MapStorage storage = world.getMapStorage();
        if (storage == null) {
            return new GlobalDifficultyData();
        }

        GlobalDifficultyData data = (GlobalDifficultyData) storage.getOrLoadData(
                GlobalDifficultyData.class, DATA_NAME);

        if (data == null) {
            data = new GlobalDifficultyData();
            storage.setData(DATA_NAME, data);
        }

        return data;
    }

    // ==================== 难度修正接口 ====================

    /**
     * 应用全局难度修正
     * 
     * @param baseDifficulty 原始计算的难度值
     * @return 修正后的难度值
     */
    public float applyGlobalModifiers(float baseDifficulty) {
        if (!enabled) {
            return baseDifficulty;
        }

        // 如果锁定，直接返回锁定值
        if (difficultyLock >= 0) {
            return difficultyLock;
        }

        // 应用倍率
        float modified = baseDifficulty * globalMultiplier;

        // 应用偏移
        modified += globalOffset;

        // 确保非负
        modified = Math.max(0, modified);

        // 应用上限
        if (maxDifficultyCap > 0 && modified > maxDifficultyCap) {
            modified = maxDifficultyCap;
        }

        return modified;
    }

    // ==================== Getters & Setters ====================

    public float getGlobalOffset() {
        return globalOffset;
    }

    public void setGlobalOffset(float offset) {
        this.globalOffset = offset;
        markDirty();
    }

    public void addGlobalOffset(float delta) {
        this.globalOffset += delta;
        markDirty();
    }

    public float getDifficultyLock() {
        return difficultyLock;
    }

    public void setDifficultyLock(float lock) {
        this.difficultyLock = lock;
        markDirty();
    }

    public void clearDifficultyLock() {
        this.difficultyLock = -1.0f;
        markDirty();
    }

    public boolean isDifficultyLocked() {
        return difficultyLock >= 0;
    }

    public float getMaxDifficultyCap() {
        return maxDifficultyCap;
    }

    public void setMaxDifficultyCap(float cap) {
        this.maxDifficultyCap = Math.max(0, cap);
        markDirty();
    }

    public float getGlobalMultiplier() {
        return globalMultiplier;
    }

    public void setGlobalMultiplier(float multiplier) {
        this.globalMultiplier = Math.max(0, multiplier);
        markDirty();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        markDirty();
    }

    public boolean isPermanentlyDisabled() {
        return permanentlyDisabled;
    }

    public void setPermanentlyDisabled(boolean disabled) {
        this.permanentlyDisabled = disabled;
        if (disabled) {
            // 永久禁用时，锁定难度到0
            this.difficultyLock = 0.0f;
        }
        markDirty();
    }

    // ==================== NBT 序列化 ====================

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.globalOffset = nbt.getFloat("globalOffset");
        this.difficultyLock = nbt.getFloat("difficultyLock");
        this.maxDifficultyCap = nbt.getFloat("maxDifficultyCap");
        this.globalMultiplier = nbt.hasKey("globalMultiplier") ? nbt.getFloat("globalMultiplier") : 1.0f;
        this.enabled = !nbt.hasKey("enabled") || nbt.getBoolean("enabled");
        this.permanentlyDisabled = nbt.getBoolean("permanentlyDisabled");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setFloat("globalOffset", globalOffset);
        nbt.setFloat("difficultyLock", difficultyLock);
        nbt.setFloat("maxDifficultyCap", maxDifficultyCap);
        nbt.setFloat("globalMultiplier", globalMultiplier);
        nbt.setBoolean("enabled", enabled);
        nbt.setBoolean("permanentlyDisabled", permanentlyDisabled);
        return nbt;
    }
}
