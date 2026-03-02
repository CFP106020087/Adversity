package com.adversity.difficulty;

import com.adversity.Adversity;
import com.adversity.affix.AffixData;
import com.adversity.affix.AffixRegistry;
import com.adversity.affix.IAffix;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.capability.IPlayerDifficulty;
import com.adversity.config.AdversityConfig;
import com.adversity.effect.SuppressionManager;
import com.adversity.sanctuary.SanctuaryManager;
import com.adversity.sanctuary.SanctuaryZone;
import com.adversity.network.PacketHandler;
import com.adversity.network.PacketSyncAdversity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 难度管理器 - 核心难度计算和应用逻辑
 *
 * 重构版本：支持多种缩放模式以适应不同模组包生态
 * - 轻量包：线性增长，原版体验
 * - 中型包：复合增长，平滑曲线
 * - 重型包：多项式/指数增长，极端数值
 */
public class DifficultyManager {

    private static final List<IDifficultyProvider> PROVIDERS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    private static boolean initialized = false;

    // 缓存的缩放模式（避免每次调用都解析字符串）
    private static ScalingFormula.ScalingMode healthMode;
    private static ScalingFormula.ScalingMode damageMode;
    private static ScalingFormula.ScalingMode armorMode;
    private static ScalingFormula.ScalingMode drMode;

    /**
     * 初始化难度管理器
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // 注册默认难度提供者
        registerProvider(new DistanceDifficultyProvider());
        registerProvider(new TimeDifficultyProvider());

        // 解析并缓存缩放模式
        refreshScalingModes();

        Adversity.LOGGER.info("Difficulty Manager initialized with {} providers", PROVIDERS.size());
        logScalingConfig();
    }

    /**
     * 刷新缩放模式缓存（配置变更时调用）
     */
    public static void refreshScalingModes() {
        healthMode = ScalingFormula.parseMode(AdversityConfig.statScaling.healthScalingMode);
        damageMode = ScalingFormula.parseMode(AdversityConfig.statScaling.damageScalingMode);
        armorMode = ScalingFormula.parseMode(AdversityConfig.statScaling.armorScalingMode);
        drMode = ScalingFormula.parseMode(AdversityConfig.statScaling.damageReductionScalingMode);
    }

    /**
     * 输出当前缩放配置到日志（用于调试）
     */
    private static void logScalingConfig() {
        Adversity.LOGGER.info("=== Adversity Scaling Configuration ===");
        Adversity.LOGGER.info("Health: {} (rate={}, max={})",
            healthMode,
            AdversityConfig.statScaling.healthRate,
            AdversityConfig.statScaling.healthMax);
        Adversity.LOGGER.info("Damage: {} (rate={}, max={})",
            damageMode,
            AdversityConfig.statScaling.damageRate,
            AdversityConfig.statScaling.damageMax);
        Adversity.LOGGER.info("Armor: {} (rate={}, max={})",
            armorMode,
            AdversityConfig.statScaling.armorRate,
            AdversityConfig.statScaling.armorMax);
        Adversity.LOGGER.info("DR: {} (rate={}, max={})",
            drMode,
            AdversityConfig.statScaling.damageReductionRate,
            AdversityConfig.statScaling.damageReductionMax);

        // 输出示例数值
        Adversity.LOGGER.info("--- Sample Values (Difficulty 10, 20, 50) ---");
        for (int d : new int[]{10, 20, 50}) {
            double hp = calculateHealthMultiplier(d);
            double dmg = calculateDamageMultiplier(d);
            double armor = calculateArmorBonus(d);
            double dr = calculateDamageReduction(d);
            Adversity.LOGGER.info("Diff {}: HP={}x, DMG={}x, Armor=+{}, DR={}%",
                d,
                String.format("%.1f", hp),
                String.format("%.1f", dmg),
                String.format("%.1f", armor),
                String.format("%.1f", dr * 100));
        }
    }

    /**
     * 注册难度提供者
     */
    public static void registerProvider(IDifficultyProvider provider) {
        PROVIDERS.add(provider);
        Adversity.LOGGER.debug("Registered difficulty provider: {}", provider.getId());
    }

    // ==================== 难度计算 ====================

    /**
     * 计算指定位置的综合难度（使用加权总和）
     */
    public static float calculateDifficulty(World world, BlockPos pos, @Nullable EntityPlayer nearestPlayer) {
        if (PROVIDERS.isEmpty()) {
            return 0f;
        }

        float totalDifficulty = 0f;

        for (IDifficultyProvider provider : PROVIDERS) {
            if (provider.isApplicable(world, pos, nearestPlayer)) {
                float weight = provider.getWeight();
                float difficulty = provider.calculateDifficulty(world, pos, nearestPlayer);
                totalDifficulty += difficulty * weight;
            }
        }

        return totalDifficulty;
    }

    /**
     * 公开的难度计算接口（包含全局修正和玩家倍率）
     */
    public static float calculateDifficultyAt(World world, BlockPos pos, @Nullable EntityPlayer player) {
        float baseDifficulty = calculateDifficulty(world, pos, player);

        // 应用全局难度修正
        GlobalDifficultyData globalData = GlobalDifficultyData.get(world);
        float modifiedDifficulty = globalData.applyGlobalModifiers(baseDifficulty);

        // 最后应用玩家个人倍率
        return applyPlayerMultiplier(modifiedDifficulty, player);
    }

    /**
     * 检查位置是否被压制
     */
    public static boolean isLocationSuppressed(World world, BlockPos pos) {
        return SuppressionManager.isSuppressed(world.provider.getDimension(), pos);
    }

    /**
     * 应用玩家的难度倍率
     */
    public static float applyPlayerMultiplier(float baseDifficulty, @Nullable EntityPlayer player) {
        if (player == null) {
            return baseDifficulty;
        }

        IPlayerDifficulty playerDiff = CapabilityHandler.getPlayerDifficulty(player);
        if (playerDiff == null) {
            return baseDifficulty;
        }

        return baseDifficulty * playerDiff.getDifficultyMultiplier();
    }

    // ==================== 属性缩放计算 ====================

    /**
     * 计算生命值倍率（使用服务器配置）
     */
    public static double calculateHealthMultiplier(float difficulty) {
        return calculateHealthMultiplier(difficulty, null);
    }

    /**
     * 计算生命值倍率（支持玩家个人设置）
     */
    public static double calculateHealthMultiplier(float difficulty, @Nullable EntityPlayer player) {
        ScalingFormula.ScalingMode mode = healthMode;

        // 检查玩家是否有自定义缩放模式
        if (player != null) {
            IPlayerDifficulty playerDiff = CapabilityHandler.getPlayerDifficulty(player);
            if (playerDiff != null) {
                IPlayerDifficulty.ScalingMode playerMode = playerDiff.getHealthScalingMode();
                if (playerMode != IPlayerDifficulty.ScalingMode.DEFAULT) {
                    mode = convertScalingMode(playerMode);
                }
            }
        }

        return ScalingFormula.calculate(
            mode,
            AdversityConfig.statScaling.healthBase,
            difficulty,
            AdversityConfig.statScaling.healthRate,
            AdversityConfig.statScaling.healthPower,
            AdversityConfig.statScaling.healthMax
        );
    }

    /**
     * 计算攻击力倍率（使用服务器配置）
     */
    public static double calculateDamageMultiplier(float difficulty) {
        return calculateDamageMultiplier(difficulty, null);
    }

    /**
     * 计算攻击力倍率（支持玩家个人设置）
     */
    public static double calculateDamageMultiplier(float difficulty, @Nullable EntityPlayer player) {
        ScalingFormula.ScalingMode mode = damageMode;

        // 检查玩家是否有自定义缩放模式
        if (player != null) {
            IPlayerDifficulty playerDiff = CapabilityHandler.getPlayerDifficulty(player);
            if (playerDiff != null) {
                IPlayerDifficulty.ScalingMode playerMode = playerDiff.getDamageScalingMode();
                if (playerMode != IPlayerDifficulty.ScalingMode.DEFAULT) {
                    mode = convertScalingMode(playerMode);
                }
            }
        }

        return ScalingFormula.calculate(
            mode,
            AdversityConfig.statScaling.damageBase,
            difficulty,
            AdversityConfig.statScaling.damageRate,
            AdversityConfig.statScaling.damagePower,
            AdversityConfig.statScaling.damageMax
        );
    }

    /**
     * 将玩家缩放模式转换为公式缩放模式
     */
    private static ScalingFormula.ScalingMode convertScalingMode(IPlayerDifficulty.ScalingMode playerMode) {
        switch (playerMode) {
            case LINEAR:
                return ScalingFormula.ScalingMode.LINEAR;
            case EXPONENTIAL:
                return ScalingFormula.ScalingMode.EXPONENTIAL;
            case COMPOUND:
                return ScalingFormula.ScalingMode.COMPOUND;
            case POLYNOMIAL:
                return ScalingFormula.ScalingMode.POLYNOMIAL;
            case LOGARITHMIC:
                return ScalingFormula.ScalingMode.LOGARITHMIC;
            case SIGMOID:
                return ScalingFormula.ScalingMode.SIGMOID;
            default:
                return healthMode; // 默认回退到服务器配置
        }
    }

    /**
     * 计算盔甲加成
     */
    public static double calculateArmorBonus(float difficulty) {
        return ScalingFormula.calculate(
            armorMode,
            AdversityConfig.statScaling.armorBase,
            difficulty,
            AdversityConfig.statScaling.armorRate,
            AdversityConfig.statScaling.armorPower,
            AdversityConfig.statScaling.armorMax
        );
    }

    /**
     * 计算减伤比例
     */
    public static double calculateDamageReduction(float difficulty) {
        // 减伤使用特殊处理：确保不超过上限
        double dr = ScalingFormula.calculate(
            drMode,
            AdversityConfig.statScaling.damageReductionBase,
            difficulty,
            AdversityConfig.statScaling.damageReductionRate,
            AdversityConfig.statScaling.damageReductionPower,
            AdversityConfig.statScaling.damageReductionMax
        );
        return Math.min(dr, AdversityConfig.statScaling.damageReductionMax);
    }

    // ==================== 等级计算 ====================

    /**
     * 根据难度计算等级（使用配置的阈值）
     */
    public static int calculateTier(float difficulty) {
        double[] thresholds = AdversityConfig.eliteSettings.tierThresholds;
        if (thresholds == null || thresholds.length == 0) {
            return 0;
        }

        // 从高到低检查阈值
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (difficulty >= thresholds[i]) {
                return i + 1; // T1-T10
            }
        }

        return 0; // 低于最低阈值
    }

    /**
     * 根据等级计算词条数量（使用配置）
     */
    public static int calculateAffixCount(int tier) {
        if (tier <= 0) return 0;

        int[] counts = AdversityConfig.eliteSettings.affixCountPerTier;
        if (counts == null || counts.length == 0) {
            return 1;
        }

        int index = Math.min(tier - 1, counts.length - 1);
        int baseCount = counts[index];

        // 偶数等级有几率额外 +1
        if (tier % 2 == 0 && RANDOM.nextBoolean()) {
            baseCount++;
        }

        return baseCount;
    }

    // ==================== 实体处理 ====================

    /**
     * 处理生成的实体，应用难度和词条
     */
    public static void processSpawnedEntity(EntityLiving entity, @Nullable EntityPlayer nearestPlayer) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        if (cap == null || cap.isProcessed()) {
            return;
        }

        // 检查玩家的个人难度设置
        if (nearestPlayer != null) {
            IPlayerDifficulty playerDiff = CapabilityHandler.getPlayerDifficulty(nearestPlayer);
            if (playerDiff != null && playerDiff.isDifficultyDisabled()) {
                cap.setProcessed(true);
                return;
            }
        }

        World world = entity.world;
        BlockPos pos = entity.getPosition();

        // 计算难度
        float baseDifficulty = calculateDifficulty(world, pos, nearestPlayer);
        float difficulty = applyPlayerMultiplier(baseDifficulty, nearestPlayer);

        // 应用圣所难度倍率 (EASE模式降低难度)
        double sanctuaryDiffMul = SanctuaryManager.getDifficultyMultiplier(world, pos);
        if (sanctuaryDiffMul != 1.0) {
            difficulty = (float) (difficulty * sanctuaryDiffMul);
        }

        cap.setDifficultyLevel(difficulty);

        // 检查区域压制
        boolean suppressed = SuppressionManager.isSuppressed(world.provider.getDimension(), pos);

        // 检查圣所区域压制
        float sanctuaryReduction = SanctuaryManager.getEliteReduction(world, pos);

        // 检查是否在精英黑名单中
        boolean eliteBlacklisted = AdversityConfig.isEliteBlacklisted(entity);

        // 检查是否有强制等级
        int forcedTier = AdversityConfig.getForcedTier(entity);

        // 计算精英概率
        double eliteChance = Math.min(
            AdversityConfig.eliteSettings.eliteChance +
                difficulty * AdversityConfig.eliteSettings.eliteChancePerDifficulty,
            AdversityConfig.eliteSettings.maxEliteChance
        );

        // 检查是否成为精英
        int tier = 0;
        if (eliteBlacklisted) {
            // 在黑名单中，永远不会成为精英
            tier = 0;
        } else if (forcedTier > 0) {
            // 强制指定等级
            tier = forcedTier;
        } else {
            // 正常随机检查
            double minDiff = AdversityConfig.eliteSettings.minDifficultyForElite;
            // 应用圣所减少效果到精英概率
            double effectiveEliteChance = eliteChance * (1.0 - sanctuaryReduction);
            if (!suppressed && difficulty >= minDiff && RANDOM.nextDouble() < effectiveEliteChance) {
                tier = calculateTier(difficulty);
            }
        }

        // 应用圣所精英等级上限 (FARM模式限制 ≤ T4)
        int maxTier = SanctuaryManager.getMaxAllowedTier(world, pos);
        if (maxTier > 0 && tier > maxTier) {
            tier = maxTier;
        }

        cap.setTier(tier);

        // 计算并存储减伤（所有怪物）
        float damageReduction = (float) calculateDamageReduction(difficulty);
        cap.setDamageReduction(damageReduction);

        // 只有精英才应用属性修正和词条
        if (tier > 0) {
            applyStatModifiers(entity, cap, difficulty, nearestPlayer);
            applyAffixes(entity, cap, difficulty, tier);
        }

        // 标记已处理
        cap.setProcessed(true);

        // 设置快速检查标记
        if (cap.getAffixCount() > 0) {
            entity.getEntityData().setBoolean("adversity.hasAffixes", true);
        }

        // 精英日志
        if (tier > 0) {
            Adversity.LOGGER.debug("[Adversity] ELITE {} at ({}, {}, {}) | diff={} | tier={} | hp={}x | dmg={}x | armor=+{} | dr={}% | affixes={}",
                entity.getName(),
                (int) entity.posX, (int) entity.posY, (int) entity.posZ,
                String.format("%.2f", difficulty), tier,
                String.format("%.2f", cap.getHealthMultiplier()),
                String.format("%.2f", cap.getDamageMultiplier()),
                String.format("%.1f", calculateArmorBonus(difficulty)),
                String.format("%.0f", cap.getDamageReduction() * 100),
                cap.getAffixCount());
        }

        // 同步到客户端
        syncToClients(entity, cap);
    }

    /**
     * 应用属性修正
     */
    private static void applyStatModifiers(EntityLiving entity, IAdversityCapability cap, float difficulty,
                                          @Nullable EntityPlayer nearestPlayer) {
        // 计算各项属性（使用玩家的个人缩放设置）
        double healthMult = calculateHealthMultiplier(difficulty, nearestPlayer);
        double damageMult = calculateDamageMultiplier(difficulty, nearestPlayer);
        double armorBonus = calculateArmorBonus(difficulty);

        cap.setHealthMultiplier((float) healthMult);
        cap.setDamageMultiplier((float) damageMult);

        // 应用生命值
        IAttributeInstance healthAttr = entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (healthAttr != null) {
            double baseHealth = healthAttr.getBaseValue();
            double newHealth = baseHealth * healthMult;

            // 防止溢出 Float.MAX_VALUE
            if (newHealth > Float.MAX_VALUE) {
                newHealth = Float.MAX_VALUE;
            }

            healthAttr.setBaseValue(newHealth);
            entity.setHealth(entity.getMaxHealth());
        }

        // 应用攻击力
        IAttributeInstance damageAttr = entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            double baseDamage = damageAttr.getBaseValue();
            double newDamage = baseDamage * damageMult;

            if (newDamage > Float.MAX_VALUE) {
                newDamage = Float.MAX_VALUE;
            }

            damageAttr.setBaseValue(newDamage);
        }

        // 应用盔甲值
        IAttributeInstance armorAttr = entity.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armorAttr != null && armorBonus > 0) {
            double baseArmor = armorAttr.getBaseValue();
            armorAttr.setBaseValue(baseArmor + armorBonus);
        }
    }

    /**
     * 应用词条
     */
    private static void applyAffixes(EntityLiving entity, IAdversityCapability cap, float difficulty, int tier) {
        if (tier <= 0) return;

        int affixCount = calculateAffixCount(tier);
        if (affixCount <= 0) return;

        // 首先应用强制词条
        Set<ResourceLocation> forcedAffixIds = AdversityConfig.getForcedAffixesForEntity(entity);
        int forcedCount = 0;
        for (ResourceLocation forcedId : forcedAffixIds) {
            // 强制词条也可以被禁用
            if (AdversityConfig.isAffixDisabled(forcedId)) {
                continue;
            }
            IAffix forcedAffix = AffixRegistry.getAffix(forcedId);
            if (forcedAffix != null && forcedAffix.canApplyTo(entity)) {
                if (cap.addAffix(forcedAffix)) {
                    AffixData data = cap.getAffixData(forcedAffix);
                    if (data != null) {
                        forcedAffix.onApply(entity, data);
                    }
                    forcedCount++;
                }
            }
        }

        // 计算剩余的随机词条数量
        int remainingCount = Math.max(0, affixCount - forcedCount);
        if (remainingCount <= 0) return;

        // 获取可用词条（排除禁用的、该实体的黑名单词条、和已应用的强制词条）
        List<IAffix> availableAffixes = new ArrayList<>();
        for (IAffix affix : AffixRegistry.getAllAffixes()) {
            // 检查词条是否被全局禁用
            if (AdversityConfig.isAffixDisabled(affix.getId())) {
                continue;
            }
            // 检查词条是否被该实体类型屏蔽
            if (AdversityConfig.isAffixBlockedForEntity(affix.getId(), entity)) {
                continue;
            }
            // 跳过已经应用的强制词条
            if (forcedAffixIds.contains(affix.getId())) {
                continue;
            }
            // 检查词条的Tier限制
            int minTier = affix.getMinTier();
            int maxTier = affix.getMaxTier();
            if (minTier > 0 && tier < minTier) {
                continue; // Tier太低，跳过高级词条
            }
            if (maxTier > 0 && tier > maxTier) {
                continue; // Tier太高，跳过低级词条
            }
            // 检查配置中的词条等级限制
            if (!com.adversity.util.AffixTierHelper.isAllowedForTier(affix.getId(), tier)) {
                continue;
            }
            // 检查词条的难度和实体要求
            if (affix.getMinDifficulty() <= difficulty && affix.canApplyTo(entity)) {
                availableAffixes.add(affix);
            }
        }

        if (availableAffixes.isEmpty()) return;

        // 随机选择词条
        List<IAffix> selectedAffixes = selectAffixes(availableAffixes, remainingCount);

        // 应用词条
        for (IAffix affix : selectedAffixes) {
            if (cap.addAffix(affix)) {
                AffixData data = cap.getAffixData(affix);
                if (data != null) {
                    affix.onApply(entity, data);
                }
            }
        }
    }

    /**
     * 加权随机选择词条（支持词条依赖）
     */
    private static List<IAffix> selectAffixes(List<IAffix> available, int count) {
        List<IAffix> selected = new ArrayList<>();
        java.util.Set<ResourceLocation> selectedIds = new java.util.HashSet<>();

        // 分离有前置要求和无前置要求的词条
        List<IAffix> pool = new ArrayList<>();
        List<IAffix> dependentAffixes = new ArrayList<>();

        for (IAffix affix : available) {
            java.util.Set<ResourceLocation> required = affix.getRequiredAffixes();
            if (required.isEmpty()) {
                pool.add(affix);
            } else {
                dependentAffixes.add(affix);
            }
        }

        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            int totalWeight = 0;
            for (IAffix affix : pool) {
                totalWeight += affix.getWeight();
            }

            if (totalWeight <= 0) break;

            int roll = RANDOM.nextInt(totalWeight);
            int current = 0;
            IAffix chosen = null;

            for (IAffix affix : pool) {
                current += affix.getWeight();
                if (roll < current) {
                    chosen = affix;
                    break;
                }
            }

            if (chosen != null) {
                selected.add(chosen);
                selectedIds.add(chosen.getId());
                final IAffix finalChosen = chosen;
                pool.removeIf(a -> a.equals(finalChosen) || !a.isCompatibleWith(finalChosen));

                // 检查是否有依赖词条现在可以加入池中
                java.util.Iterator<IAffix> it = dependentAffixes.iterator();
                while (it.hasNext()) {
                    IAffix dep = it.next();
                    java.util.Set<ResourceLocation> required = dep.getRequiredAffixes();
                    // 检查是否有任何前置词条已被选中
                    boolean requirementMet = false;
                    for (ResourceLocation req : required) {
                        if (selectedIds.contains(req)) {
                            requirementMet = true;
                            break;
                        }
                    }
                    if (requirementMet && dep.isCompatibleWith(finalChosen)) {
                        pool.add(dep);
                        it.remove();
                    }
                }
            }
        }

        return selected;
    }

    /**
     * 同步实体数据到客户端
     */
    public static void syncToClients(EntityLiving entity, IAdversityCapability cap) {
        if (entity.world.isRemote) return;
        if (cap.getTier() <= 0) return;

        List<ResourceLocation> affixIds = new ArrayList<>();
        for (AffixData data : cap.getAllAffixData()) {
            affixIds.add(data.getAffix().getId());
        }

        PacketSyncAdversity packet = new PacketSyncAdversity(
            entity.getEntityId(),
            cap.getTier(),
            cap.getDifficultyLevel(),
            cap.getHealthMultiplier(),
            cap.getDamageMultiplier(),
            affixIds
        );

        PacketHandler.INSTANCE.sendToAllTracking(packet, entity);
    }
}
