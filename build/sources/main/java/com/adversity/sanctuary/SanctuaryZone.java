package com.adversity.sanctuary;

import com.adversity.config.AdversityConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * 圣所区域数据
 */
public class SanctuaryZone {

    public final int dimension;
    public final BlockPos center;
    public final SanctuaryType type;
    public int radius; // Changed from final to allow upgrades
    public int tier; // Changed from final to allow upgrades (仅天然圣所使用, 1-5)

    // 燃料系统
    public int fuel;
    public int maxFuel;

    // 圣所模式
    public SanctuaryMode mode = SanctuaryMode.SAFE; // 默认为庇护模式

    // 基础效果强度 (由Type+Tier决定)
    private final float baseEliteReduction;
    private final float baseBaubleMultiplier;

    /**
     * 创建天然圣所
     */
    public static SanctuaryZone createNatural(int dimension, BlockPos center, int tier) {
        int radius = getNaturalRadius(tier);
        int maxFuel = AdversityConfig.sanctuarySettings.naturalFuelCapacity;
        // 初始燃料为最大值的20%-50%随机
        int initialFuel = maxFuel / 5 + new java.util.Random().nextInt(maxFuel * 3 / 10 + 1);
        return new SanctuaryZone(
                dimension, center, SanctuaryType.NATURAL, radius, tier,
                initialFuel,
                maxFuel,
                getNaturalEliteReduction(tier),
                1.0f // 天然圣所饰品100%效果
        );
    }

    /**
     * 创建人造圣所
     */
    public static SanctuaryZone createArtificial(int dimension, BlockPos center) {
        return new SanctuaryZone(
                dimension, center, SanctuaryType.ARTIFICIAL,
                AdversityConfig.sanctuarySettings.artificialRadius, 0,
                AdversityConfig.sanctuarySettings.artificialFuelCapacity,
                AdversityConfig.sanctuarySettings.artificialFuelCapacity,
                0.3f, // 仅30%精英减少
                0.5f // 饰品50%效果
        );
    }

    private SanctuaryZone(int dimension, BlockPos center, SanctuaryType type,
            int radius, int tier, int fuel, int maxFuel,
            float baseEliteReduction, float baseBaubleMultiplier) {
        this.dimension = dimension;
        this.center = center;
        this.type = type;
        this.radius = radius;
        this.tier = tier;
        this.fuel = fuel;
        this.maxFuel = maxFuel;
        this.baseEliteReduction = baseEliteReduction;
        this.baseBaubleMultiplier = baseBaubleMultiplier;
    }

    /**
     * 获取当前精英生成减少率
     */
    public float getEliteReduction() {
        if (!isActive())
            return 0.0f;

        switch (mode) {
            case SAFE:
                return 1.0f; // 庇护模式：完全阻止精英生成
            case FARM:
                return baseEliteReduction * 0.3f; // 狩猎模式：大幅降低减少率（允许精英生成）
            case EASE:
                return baseEliteReduction; // 压制模式：使用基础减少率
            default:
                return 1.0f;
        }
    }

    /**
     * 获取饰品效果倍率
     */
    public float getBaubleEffectMultiplier() {
        if (!isActive())
            return 0.5f; // 非激活状态=圣所外效果
        return baseBaubleMultiplier;
    }

    /**
     * 获取当前燃料消耗倍率（从配置读取）
     * SAFE最贵 > EASE中等 > FARM最便宜
     * 高等级消耗更重: 基础倍率 × (1 + (tier-1) × tierMultiplier)
     */
    public double getFuelUsageMultiplier() {
        double modeMultiplier;
        switch (mode) {
            case SAFE:
                modeMultiplier = AdversityConfig.sanctuarySettings.safeModeFuelMultiplier;
                break;
            case EASE:
                modeMultiplier = AdversityConfig.sanctuarySettings.easeModeFuelMultiplier;
                break;
            case FARM:
                modeMultiplier = AdversityConfig.sanctuarySettings.farmModeFuelMultiplier;
                break;
            default:
                modeMultiplier = 1.0;
        }
        // 等级越高消耗越重
        double tierMultiplier = 1.0 + (tier - 1) * AdversityConfig.sanctuarySettings.tierFuelMultiplier;
        return modeMultiplier * tierMultiplier;
    }

    /**
     * 获取当前难度修正倍率 (仅EASE模式有效)
     * 返回 1.0 (无修正) 或更低值 (降低难度)
     */
    public double getDifficultyMultiplier() {
        if (!isActive())
            return 1.0;

        if (mode == SanctuaryMode.EASE) {
            return AdversityConfig.sanctuarySettings.easeModeDifficultyMultiplier;
        }
        return 1.0;
    }

    /**
     * 获取允许生成的最大等级 (仅FARM模式有效)
     * 返回 0 表示无限制
     */
    public int getMaxAllowedTier() {
        if (!isActive())
            return 0;

        if (mode == SanctuaryMode.FARM) {
            return 4; // 限制为Epic级别 (T4)
        }
        return 0;
    }

    /**
     * 获取词条伤害压制比例
     * 圣所内词条造成的伤害会被削减
     * 
     * @return 0.0 = 无压制, 0.5 = 50%伤害压制
     */
    public float getAffixDamageReduction() {
        if (!isActive())
            return 0.0f;

        // 根据圣所等级计算压制比例
        // T1: 30%, T2: 35%, T3: 40%, T4: 45%, T5: 50%
        float baseReduction = 0.25f + (tier * 0.05f);
        baseReduction = Math.min(baseReduction, 0.5f); // 最高50%

        switch (mode) {
            case SAFE:
                return baseReduction; // 安全模式：完整压制
            case EASE:
                return baseReduction * 1.2f; // 压制模式：额外20%压制
            case FARM:
                return baseReduction * 0.3f; // 狩猎模式：压制大幅降低
            default:
                return baseReduction;
        }
    }

    /**
     * 获取天然圣所半径
     * T1=1600, T2=2000, T3=2400, T4=2800, T5=3200
     */
    private static int getNaturalRadius(int tier) {
        return AdversityConfig.sanctuarySettings.naturalBaseRadius +
                (tier - 1) * AdversityConfig.sanctuarySettings.naturalRadiusPerTier;
    }

    /**
     * 获取天然圣所精英减少率
     */
    private static float getNaturalEliteReduction(int tier) {
        switch (tier) {
            case 1:
                return 0.5f;
            case 2:
                return 0.75f;
            case 3:
                return 0.9f;
            case 4:
                return 1.0f;
            default:
                return 0.5f;
        }
    }

    /**
     * 检查位置是否在圣所范围内（圆柱形，忽略Y轴）
     */
    public boolean contains(BlockPos pos) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        return (dx * dx + dz * dz) <= (radius * radius);
    }

    /**
     * 圣所是否有燃料运作中
     */
    public boolean isActive() {
        return fuel > 0;
    }

    /**
     * 获取燃料百分比 (0.0-1.0)
     */
    public float getFuelPercentage() {
        return maxFuel > 0 ? (float) fuel / maxFuel : 0;
    }

    public void setMode(SanctuaryMode mode) {
        this.mode = mode;
    }

    // ==================== NBT 序列化 ====================

    public NBTTagCompound toNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("dimension", dimension);
        nbt.setInteger("x", center.getX());
        nbt.setInteger("y", center.getY());
        nbt.setInteger("z", center.getZ());
        nbt.setString("type", type.name());
        nbt.setInteger("radius", radius);
        nbt.setInteger("tier", tier);
        nbt.setInteger("fuel", fuel);
        nbt.setInteger("maxFuel", maxFuel);
        nbt.setFloat("eliteReduction", baseEliteReduction);
        nbt.setFloat("baubleMultiplier", baseBaubleMultiplier);
        nbt.setString("mode", mode.name()); // 保存模式
        return nbt;
    }

    public static SanctuaryZone fromNBT(NBTTagCompound nbt) {
        try {
            int dimension = nbt.getInteger("dimension");
            BlockPos center = new BlockPos(
                    nbt.getInteger("x"),
                    nbt.getInteger("y"),
                    nbt.getInteger("z"));
            SanctuaryType type = SanctuaryType.valueOf(nbt.getString("type"));
            int radius = nbt.getInteger("radius");
            int tier = nbt.getInteger("tier");
            int fuel = nbt.getInteger("fuel");
            int maxFuel = nbt.getInteger("maxFuel");
            float eliteReduction = nbt.getFloat("eliteReduction");
            float baubleMultiplier = nbt.getFloat("baubleMultiplier");

            SanctuaryZone zone = new SanctuaryZone(
                    dimension, center, type, radius, tier,
                    fuel, maxFuel, eliteReduction, baubleMultiplier);

            if (nbt.hasKey("mode")) {
                zone.setMode(SanctuaryMode.fromString(nbt.getString("mode")));
            }

            return zone;
        } catch (Exception e) {
            return null;
        }
    }
}
