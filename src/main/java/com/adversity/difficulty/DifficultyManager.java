package com.adversity.difficulty;

import com.adversity.Adversity;
import com.adversity.affix.AffixData;
import com.adversity.affix.AffixRegistry;
import com.adversity.affix.IAffix;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.capability.IPlayerDifficulty;
import com.adversity.config.AdversityConfig;
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

/**
 * 难度管理器 - 核心难度计算和应用逻辑
 */
public class DifficultyManager {

    private static final List<IDifficultyProvider> PROVIDERS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    private static boolean initialized = false;

    /**
     * 初始化难度管理器
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // 注册默认难度提供者
        registerProvider(new DistanceDifficultyProvider());
        registerProvider(new TimeDifficultyProvider());

        Adversity.LOGGER.info("Difficulty Manager initialized with {} providers", PROVIDERS.size());
    }

    /**
     * 注册难度提供者
     */
    public static void registerProvider(IDifficultyProvider provider) {
        PROVIDERS.add(provider);
        Adversity.LOGGER.debug("Registered difficulty provider: {}", provider.getId());
    }

    /**
     * 计算指定位置的综合难度
     * 使用加权总和，让距离和时间难度能够叠加
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
     * 应用玩家的难度倍率
     */
    private static float applyPlayerMultiplier(float baseDifficulty, @Nullable EntityPlayer player) {
        if (player == null) {
            return baseDifficulty;
        }

        IPlayerDifficulty playerDiff = CapabilityHandler.getPlayerDifficulty(player);
        if (playerDiff == null) {
            return baseDifficulty;
        }

        return baseDifficulty * playerDiff.getDifficultyMultiplier();
    }

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
            if (playerDiff != null) {
                // 玩家禁用了难度系统
                if (playerDiff.isDifficultyDisabled()) {
                    cap.setProcessed(true);
                    return;
                }
            }
        }

        World world = entity.world;
        BlockPos pos = entity.getPosition();

        // 计算基础难度
        float baseDifficulty = calculateDifficulty(world, pos, nearestPlayer);

        // 应用玩家的难度倍率
        float difficulty = applyPlayerMultiplier(baseDifficulty, nearestPlayer);
        cap.setDifficultyLevel(difficulty);

        // 计算精英概率
        double eliteChance = Math.min(
            AdversityConfig.difficulty.eliteChance + difficulty * AdversityConfig.difficulty.eliteChancePerDifficulty,
            AdversityConfig.difficulty.maxEliteChance
        );

        // 检查是否成为精英（难度 >= 2.0 时才有机会）
        int tier = 0;
        if (difficulty >= 2.0f && RANDOM.nextDouble() < eliteChance) {
            tier = calculateTier(difficulty);
        }
        cap.setTier(tier);

        // 计算减伤（所有怪物都有基础减伤，精英更高）
        float damageReduction = (float) Math.min(
            difficulty * AdversityConfig.difficulty.damageReductionPerDifficulty,
            AdversityConfig.difficulty.maxDamageReduction
        );
        cap.setDamageReduction(damageReduction);

        // 只有精英才应用属性修正和词条
        if (tier > 0) {
            // 应用属性修正
            applyStatModifiers(entity, cap, difficulty);

            // 应用词条
            applyAffixes(entity, cap, difficulty, tier);
        }

        // 标记已处理
        cap.setProcessed(true);

        // 如果有词条，设置快速检查标记（用于 tick 优化）
        if (cap.getAffixCount() > 0) {
            entity.getEntityData().setBoolean("adversity.hasAffixes", true);
        }

        // 只输出精英怪物日志（减少日志量）
        if (tier > 0) {
            Adversity.LOGGER.info("[Adversity] ELITE {} at ({}, {}, {}) | diff={} | tier={} | hp={}x | dmg={}x | dr={}% | affixes={}",
                entity.getName(),
                (int) entity.posX, (int) entity.posY, (int) entity.posZ,
                String.format("%.2f", difficulty), tier,
                String.format("%.2f", cap.getHealthMultiplier()),
                String.format("%.2f", cap.getDamageMultiplier()),
                String.format("%.0f", cap.getDamageReduction() * 100),
                cap.getAffixCount());
        }

        // 同步数据到客户端
        syncToClients(entity, cap);
    }

    /**
     * 同步实体数据到客户端
     */
    public static void syncToClients(EntityLiving entity, IAdversityCapability cap) {
        if (entity.world.isRemote) {
            return; // 只在服务端同步
        }

        // 只同步有等级的怪物
        if (cap.getTier() <= 0) {
            return;
        }

        // 收集词条ID
        List<ResourceLocation> affixIds = new ArrayList<>();
        for (AffixData data : cap.getAllAffixData()) {
            affixIds.add(data.getAffix().getId());
        }

        // 创建同步包
        PacketSyncAdversity packet = new PacketSyncAdversity(
            entity.getEntityId(),
            cap.getTier(),
            cap.getDifficultyLevel(),
            cap.getHealthMultiplier(),
            cap.getDamageMultiplier(),
            affixIds
        );

        // 发送给实体附近的所有玩家
        PacketHandler.INSTANCE.sendToAllTracking(packet, entity);
    }

    /**
     * 根据难度计算等级 (10级系统)
     * 难度区间更细分，适应高难度模组包
     */
    private static int calculateTier(float difficulty) {
        if (difficulty < 2.0f) return 0;       // 普通 (无词条)
        if (difficulty < 3.0f) return 1;       // T1 - 精英
        if (difficulty < 4.5f) return 2;       // T2 - 稀有
        if (difficulty < 6.0f) return 3;       // T3 - 精锐
        if (difficulty < 8.0f) return 4;       // T4 - 史诗
        if (difficulty < 10.0f) return 5;      // T5 - 传说
        if (difficulty < 13.0f) return 6;      // T6 - 神话
        if (difficulty < 16.0f) return 7;      // T7 - 远古
        if (difficulty < 20.0f) return 8;      // T8 - 虚空
        if (difficulty < 25.0f) return 9;      // T9 - 深渊
        return 10;                              // T10 - 终焉
    }

    /**
     * 应用属性修正
     */
    private static void applyStatModifiers(EntityLiving entity, IAdversityCapability cap, float difficulty) {
        // 从配置读取倍率参数
        float healthMult = 1.0f + difficulty * (float) AdversityConfig.difficulty.healthMultiplierPerDifficulty;
        float damageMult = 1.0f + difficulty * (float) AdversityConfig.difficulty.damageMultiplierPerDifficulty;
        float armorBonus = (float) Math.min(
            difficulty * AdversityConfig.difficulty.armorPerDifficulty,
            AdversityConfig.difficulty.maxArmorBonus
        );

        cap.setHealthMultiplier(healthMult);
        cap.setDamageMultiplier(damageMult);

        // 应用生命值
        IAttributeInstance healthAttr = entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (healthAttr != null) {
            double baseHealth = healthAttr.getBaseValue();
            healthAttr.setBaseValue(baseHealth * healthMult);
            entity.setHealth(entity.getMaxHealth()); // 恢复满血
        }

        // 应用攻击力
        IAttributeInstance damageAttr = entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            double baseDamage = damageAttr.getBaseValue();
            damageAttr.setBaseValue(baseDamage * damageMult);
        }

        // 应用盔甲值加成
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
        if (tier <= 0) {
            return; // 普通怪物没有词条
        }

        // 计算词条数量
        int affixCount = calculateAffixCount(tier);
        if (affixCount <= 0) {
            return;
        }

        // 获取可用词条
        List<IAffix> availableAffixes = new ArrayList<>();
        for (IAffix affix : AffixRegistry.getAllAffixes()) {
            if (affix.getMinDifficulty() <= difficulty && affix.canApplyTo(entity)) {
                availableAffixes.add(affix);
            }
        }

        if (availableAffixes.isEmpty()) {
            return;
        }

        // 随机选择词条
        List<IAffix> selectedAffixes = selectAffixes(availableAffixes, affixCount);

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
     * 根据等级计算词条数量 (10级系统)
     */
    private static int calculateAffixCount(int tier) {
        switch (tier) {
            case 1: return 1;                          // T1: 1
            case 2: return 1 + RANDOM.nextInt(2);      // T2: 1-2
            case 3: return 2;                          // T3: 2
            case 4: return 2 + RANDOM.nextInt(2);      // T4: 2-3
            case 5: return 3;                          // T5: 3
            case 6: return 3 + RANDOM.nextInt(2);      // T6: 3-4
            case 7: return 4;                          // T7: 4
            case 8: return 4 + RANDOM.nextInt(2);      // T8: 4-5
            case 9: return 5;                          // T9: 5
            case 10: return 5 + RANDOM.nextInt(2);     // T10: 5-6
            default: return 0;
        }
    }

    /**
     * 加权随机选择词条
     */
    private static List<IAffix> selectAffixes(List<IAffix> available, int count) {
        List<IAffix> selected = new ArrayList<>();
        List<IAffix> pool = new ArrayList<>(available);

        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            // 计算总权重
            int totalWeight = 0;
            for (IAffix affix : pool) {
                totalWeight += affix.getWeight();
            }

            if (totalWeight <= 0) break;

            // 随机选择
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

                // 从池中移除不兼容的词条
                final IAffix finalChosen = chosen;
                pool.removeIf(a -> a.equals(finalChosen) || !a.isCompatibleWith(finalChosen));
            }
        }

        return selected;
    }
}
