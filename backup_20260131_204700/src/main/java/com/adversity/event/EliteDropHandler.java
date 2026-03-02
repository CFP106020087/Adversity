package com.adversity.event;

import com.adversity.Adversity;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.config.AdversityConfig;
import com.adversity.item.ItemEntropy;
import com.adversity.item.ItemRegistry;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * 精英怪物掉落处理器
 *
 * 掉落设计理念：
 * - 掉落率和数量与等级正相关
 * - 高等级熵能只从高等级精英获取
 * - 玩家击杀才有掉落（防止自动化农场）
 * - 掉落位置在怪物身上，增强反馈感
 *
 * 掉落表：
 * T1-T4: 熵能碎片 (Entropy Shard) - 1~3个，100%概率
 * T5-T7: 熵能结晶 (Entropy Crystal) - 1~2个，50%-80%概率 + 碎片
 * T8-T10: 熵能核心 (Entropy Core) - 1个，30%-60%概率 + 结晶 + 碎片
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class EliteDropHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onEliteDeath(LivingDeathEvent event) {
        // 只在服务端处理
        if (event.getEntity().world.isRemote) return;

        // 只处理 EntityLiving
        if (!(event.getEntity() instanceof EntityLiving)) return;
        EntityLiving entity = (EntityLiving) event.getEntity();

        // 获取能力数据
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        if (cap == null || cap.getTier() <= 0) return;

        // 检查模组物品掉落是否启用
        if (!AdversityConfig.lootSettings.enableModItemDrops) return;

        // 检查是否被玩家击杀
        DamageSource source = event.getSource();
        if (!isPlayerKill(source)) return;

        int tier = cap.getTier();
        World world = entity.world;
        BlockPos pos = entity.getPosition();

        // 根据等级决定掉落
        processDrops(world, pos, tier, cap.getDifficultyLevel());

        Adversity.LOGGER.debug("Elite T{} dropped loot at {}", tier, pos);
    }

    /**
     * 经验加成 - 精英怪物掉落更多经验
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!AdversityConfig.lootSettings.enableBonusXp) return;

        if (!(event.getEntity() instanceof EntityLiving)) return;
        EntityLiving entity = (EntityLiving) event.getEntity();

        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        if (cap == null || cap.getTier() <= 0) return;

        int tier = cap.getTier();
        int originalXp = event.getDroppedExperience();

        // 计算经验倍率
        double xpMultiplier = 1.0 + (tier * AdversityConfig.lootSettings.xpMultiplierPerTier);
        xpMultiplier = Math.min(xpMultiplier, AdversityConfig.lootSettings.maxXpMultiplier);

        int newXp = (int) (originalXp * xpMultiplier);
        event.setDroppedExperience(newXp);

        if (tier >= 3) {
            Adversity.LOGGER.debug("Elite T{} XP bonus: {} -> {} ({}x)",
                tier, originalXp, newXp, String.format("%.1f", xpMultiplier));
        }
    }

    /**
     * 战利品加成 - 精英怪物掉落更多原版物品
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!AdversityConfig.lootSettings.enableExtraLoot) return;

        if (!(event.getEntity() instanceof EntityLiving)) return;
        EntityLiving entity = (EntityLiving) event.getEntity();

        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        if (cap == null || cap.getTier() <= 0) return;

        int tier = cap.getTier();

        // 计算战利品倍率
        double lootMultiplier = 1.0 + (tier * AdversityConfig.lootSettings.lootMultiplierPerTier);
        lootMultiplier = Math.min(lootMultiplier, AdversityConfig.lootSettings.maxLootMultiplier);

        // 对每个掉落物应用倍率
        for (EntityItem drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (!stack.isEmpty() && stack.getCount() > 0) {
                // 计算新数量（使用随机加成来增加变化性）
                int originalCount = stack.getCount();
                int extraCount = 0;

                // 基础加成
                double extraChance = (lootMultiplier - 1.0) * originalCount;

                // 整数部分
                extraCount += (int) extraChance;

                // 小数部分有几率额外+1
                double fractional = extraChance - (int) extraChance;
                if (RANDOM.nextDouble() < fractional) {
                    extraCount++;
                }

                if (extraCount > 0) {
                    stack.setCount(originalCount + extraCount);
                }
            }
        }
    }

    /**
     * 检查是否为玩家击杀
     */
    private static boolean isPlayerKill(DamageSource source) {
        if (source == null) return false;

        // 直接伤害来源是玩家
        if (source.getTrueSource() instanceof EntityPlayer) {
            return true;
        }

        // 间接伤害（如箭矢）的真正来源是玩家
        if (source.getImmediateSource() != null &&
            source.getTrueSource() instanceof EntityPlayer) {
            return true;
        }

        return false;
    }

    /**
     * 处理掉落物 - 使用新的熵能系统
     */
    private static void processDrops(World world, BlockPos pos, int tier, float difficulty) {
        // T1-T10 都掉落基础熵能（熵能碎片）
        dropEntropyShard(world, pos, tier);

        // T5+ 掉落中级熵能（熵能结晶）
        if (tier >= 5) {
            dropEntropyCrystal(world, pos, tier);
        }

        // T8+ 掉落高级熵能（熵能核心）
        if (tier >= 8) {
            dropEntropyCore(world, pos, tier);
        }

        // 处理自定义掉落 (CraftTweaker)
        for (com.adversity.loot.EliteLootEntry entry : com.adversity.loot.EliteLootManager.getDropsForTier(tier)) {
            if (RANDOM.nextFloat() < entry.getChance()) {
                spawnItemDrop(world, pos, entry.getStack());
            }
        }
    }

    /**
     * 掉落熵能碎片
     * T1: 1个 (100%)
     * T2: 1-2个 (100%)
     * T3: 2个 (100%)
     * T4: 2-3个 (100%)
     * T5+: 3个 (100%)
     */
    private static void dropEntropyShard(World world, BlockPos pos, int tier) {
        if (ItemRegistry.ENTROPY == null)
            return;

        int count;
        switch (tier) {
            case 1:
                count = 1;
                break;
            case 2:
                count = 1 + RANDOM.nextInt(2);
                break;
            case 3:
                count = 2;
                break;
            case 4:
                count = 2 + RANDOM.nextInt(2);
                break;
            default:
                count = 3;
                break;
        }

        spawnItemDrop(world, pos, ItemEntropy.createShard(count));
    }

    /**
     * 掉落熵能结晶
     * T5: 1个 (50%)
     * T6: 1个 (65%)
     * T7: 1-2个 (80%)
     * T8+: 2个 (100%)
     */
    private static void dropEntropyCrystal(World world, BlockPos pos, int tier) {
        if (ItemRegistry.ENTROPY == null)
            return;

        double chance;
        int count;

        switch (tier) {
            case 5:
                chance = 0.50;
                count = 1;
                break;
            case 6:
                chance = 0.65;
                count = 1;
                break;
            case 7:
                chance = 0.80;
                count = 1 + RANDOM.nextInt(2);
                break;
            default: // T8+
                chance = 1.0;
                count = 2;
                break;
        }

        if (RANDOM.nextDouble() < chance) {
            spawnItemDrop(world, pos, ItemEntropy.createCrystal(count));
        }
    }

    /**
     * 掉落熵能核心
     * T8: 1个 (30%)
     * T9: 1个 (45%)
     * T10: 1-2个 (60%)
     */
    private static void dropEntropyCore(World world, BlockPos pos, int tier) {
        if (ItemRegistry.ENTROPY == null)
            return;

        double chance;
        int count;

        switch (tier) {
            case 8:
                chance = 0.30;
                count = 1;
                break;
            case 9:
                chance = 0.45;
                count = 1;
                break;
            case 10:
                chance = 0.60;
                count = 1 + RANDOM.nextInt(2);
                break;
            default:
                return;
        }

        if (RANDOM.nextDouble() < chance) {
            spawnItemDrop(world, pos, ItemEntropy.createCore(count));
        }
    }

    /**
     * 在世界中生成掉落物
     */
    private static void spawnItemDrop(World world, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() <= 0)
            return;

        // 添加随机偏移，让掉落物更自然
        double x = pos.getX() + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.5;

        EntityItem entityItem = new EntityItem(world, x, y, z, stack);

        // 添加少量上抛速度，视觉效果更好
        entityItem.motionY = 0.2 + RANDOM.nextDouble() * 0.1;
        entityItem.motionX = (RANDOM.nextDouble() - 0.5) * 0.1;
        entityItem.motionZ = (RANDOM.nextDouble() - 0.5) * 0.1;

        // 设置无法拾取延迟（短暂，防止立即吸取）
        entityItem.setPickupDelay(10);

        world.spawnEntity(entityItem);
    }
}

