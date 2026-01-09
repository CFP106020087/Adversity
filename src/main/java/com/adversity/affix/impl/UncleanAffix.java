package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 不洁词条 - 攻击时概率给予随机负面效果
 *
 * 机制：
 * 1. 每次攻击有概率给予玩家随机负面效果
 * 2. 效果可以叠加（增加等级或延长时间）
 * 3. tier越高，概率越高，效果越强
 * 4. 支持模组添加的负面药水效果
 * 5. 设计理念：不可预测的威胁，增加紧张感
 */
public class UncleanAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "unclean");

    /** 基础触发概率 */
    private static final float BASE_CHANCE = 0.3f;  // 30%

    /** 基础效果持续时间（tick） */
    private static final int BASE_DURATION = 100;  // 5秒

    /** 缓存的负面效果列表（包含模组效果） */
    private static List<Potion> cachedNegativeEffects = null;

    private static final Random RANDOM = new Random();

    public UncleanAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            80,     // 较高权重
            2.0f    // 难度2以上
        );
    }

    /**
     * 获取所有负面药水效果（包含模组效果）
     * 延迟初始化，确保模组效果已注册
     */
    private static List<Potion> getNegativeEffects() {
        if (cachedNegativeEffects == null) {
            cachedNegativeEffects = new ArrayList<>();

            // 遍历所有注册的药水效果
            for (Potion potion : ForgeRegistries.POTIONS) {
                // 检查是否为负面效果
                if (potion != null && isBadEffect(potion)) {
                    cachedNegativeEffects.add(potion);
                }
            }

            Adversity.LOGGER.info("Unclean Affix: Found {} negative potion effects (including modded)",
                cachedNegativeEffects.size());
        }
        return cachedNegativeEffects;
    }

    /**
     * 判断药水效果是否为负面效果
     */
    private static boolean isBadEffect(Potion potion) {
        // 使用Potion的isBadEffect方法
        if (potion.isBadEffect()) {
            return true;
        }

        // 额外检查一些可能没有正确标记的效果
        ResourceLocation id = potion.getRegistryName();
        if (id != null) {
            String name = id.toString().toLowerCase();
            // 排除一些不应该施加的效果
            if (name.contains("instant") ||     // 瞬间伤害等
                name.contains("death") ||       // 死亡相关
                name.contains("kill") ||        // 击杀相关
                name.contains("creative") ||    // 创造模式相关
                name.contains("invulner")) {    // 无敌相关
                return false;
            }
        }

        return false;
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 计算触发概率
        float chance = BASE_CHANCE + (tier * 0.05f);  // 每tier增加5%

        if (RANDOM.nextFloat() < chance) {
            applyRandomEffect(player, tier);
        }

        // 高tier有概率施加多个效果
        if (tier >= 5 && RANDOM.nextFloat() < 0.3f) {
            applyRandomEffect(player, tier);
        }
        if (tier >= 8 && RANDOM.nextFloat() < 0.2f) {
            applyRandomEffect(player, tier);
        }

        return damage;
    }

    /**
     * 应用随机负面效果
     */
    private void applyRandomEffect(EntityPlayer player, int tier) {
        List<Potion> effects = getNegativeEffects();
        if (effects.isEmpty()) return;

        // 随机选择效果
        Potion effect = effects.get(RANDOM.nextInt(effects.size()));

        // 计算效果参数
        int duration = BASE_DURATION + (tier * 40);  // 每tier增加2秒
        int amplifier = calculateAmplifier(tier);

        // 检查玩家是否已有该效果
        PotionEffect existingEffect = player.getActivePotionEffect(effect);
        if (existingEffect != null) {
            // 叠加效果：延长时间或增加等级
            if (RANDOM.nextBoolean()) {
                // 延长时间
                duration = existingEffect.getDuration() + duration / 2;
            } else {
                // 增加等级（有上限）
                amplifier = Math.min(existingEffect.getAmplifier() + 1, 4);
                duration = Math.max(existingEffect.getDuration(), duration);
            }
        }

        // 应用效果
        try {
            player.addPotionEffect(new PotionEffect(effect, duration, amplifier));
        } catch (Exception e) {
            // 某些模组效果可能有问题，忽略错误
            Adversity.LOGGER.debug("Failed to apply potion effect: {}", effect.getRegistryName());
        }

        // 播放效果
        playEffects(player);
    }

    /**
     * 计算效果等级
     */
    private int calculateAmplifier(int tier) {
        // 基础等级0，tier越高越有可能获得更高等级
        if (tier >= 8 && RANDOM.nextFloat() < 0.2f) {
            return 2;  // 等级III
        } else if (tier >= 5 && RANDOM.nextFloat() < 0.3f) {
            return 1;  // 等级II
        }
        return 0;  // 等级I
    }

    /**
     * 播放效果
     */
    private void playEffects(EntityPlayer player) {
        if (player.world.isRemote) return;

        // 音效
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE,
            SoundCategory.HOSTILE,
            0.5f,
            0.8f
        );

        // 粒子
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.SPELL_MOB,
                player.posX, player.posY + 1, player.posZ,
                20,
                0.5, 0.5, 0.5,
                0.0
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }

    /**
     * 清除缓存（用于重载配置）
     */
    public static void clearCache() {
        cachedNegativeEffects = null;
    }
}
