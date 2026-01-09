package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
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

import java.util.*;

/**
 * 净化者词条 - 夺取玩家的正面药水效果
 *
 * 机制：
 * 1. 攻击玩家时有概率夺取一个正面药水效果
 * 2. 夺取的效果会转移到怪物身上
 * 3. 触发后玩家30秒内无法获得新的正面药水效果
 * 4. 设计理念：针对药水流派玩家
 */
public class PurifierAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "purifier");

    /** 基础夺取概率 */
    private static final float BASE_STEAL_CHANCE = 0.25f;  // 25%

    /** 正面药水免疫时间（tick） */
    private static final int IMMUNITY_DURATION = 600;  // 30秒

    /** 存储玩家正面药水免疫结束时间 */
    private static final Map<UUID, Long> POTION_IMMUNITY = new HashMap<>();

    private static final Random RANDOM = new Random();

    public PurifierAffix() {
        super(
            ID,
            AffixType.UTILITY,
            50,     // 中等权重
            4.0f    // 难度4以上
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 计算夺取概率
        float chance = BASE_STEAL_CHANCE + (tier * 0.05f);

        if (RANDOM.nextFloat() < chance) {
            stealPositiveEffect(player, attacker, tier);
        }

        return damage;
    }

    /**
     * 夺取玩家的正面药水效果
     */
    private void stealPositiveEffect(EntityPlayer player, EntityLiving attacker, int tier) {
        // 收集玩家的所有正面效果
        List<PotionEffect> positiveEffects = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getPotion() != null && !effect.getPotion().isBadEffect()) {
                positiveEffects.add(effect);
            }
        }

        if (positiveEffects.isEmpty()) {
            return;  // 没有正面效果可夺取
        }

        // 随机选择一个效果
        PotionEffect stolenEffect = positiveEffects.get(RANDOM.nextInt(positiveEffects.size()));
        Potion potion = stolenEffect.getPotion();

        // 从玩家身上移除
        player.removePotionEffect(potion);

        // 转移到怪物身上（增强版）
        int newDuration = stolenEffect.getDuration();
        int newAmplifier = Math.min(stolenEffect.getAmplifier() + 1, 4);  // 增加一级
        attacker.addPotionEffect(new PotionEffect(potion, newDuration, newAmplifier));

        // 应用正面药水免疫
        applyPotionImmunity(player, tier);

        // 播放效果
        playStealEffects(player, attacker);

        // 发送视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.DECAY, 40, 0.7f, attacker.getEntityId());
    }

    /**
     * 应用正面药水免疫
     */
    private void applyPotionImmunity(EntityPlayer player, int tier) {
        long endTime = player.world.getTotalWorldTime() + IMMUNITY_DURATION + (tier * 100);
        POTION_IMMUNITY.put(player.getUniqueID(), endTime);
    }

    /**
     * 检查玩家是否处于正面药水免疫状态
     */
    public static boolean isPotionImmune(EntityPlayer player) {
        Long endTime = POTION_IMMUNITY.get(player.getUniqueID());
        if (endTime == null) {
            return false;
        }

        if (player.world.getTotalWorldTime() >= endTime) {
            POTION_IMMUNITY.remove(player.getUniqueID());
            return false;
        }

        return true;
    }

    /**
     * 阻止正面药水效果（应该在事件处理器中调用）
     */
    public static boolean shouldBlockPositivePotion(EntityPlayer player, Potion potion) {
        if (potion == null || potion.isBadEffect()) {
            return false;  // 负面效果不阻止
        }
        return isPotionImmune(player);
    }

    /**
     * 播放夺取效果
     */
    private void playStealEffects(EntityPlayer player, EntityLiving attacker) {
        if (player.world.isRemote) return;

        // 音效
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.EVOCATION_ILLAGER_CAST_SPELL,
            SoundCategory.HOSTILE,
            1.0f,
            0.8f
        );

        // 从玩家到怪物的粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;

            // 玩家处的效果消失粒子
            world.spawnParticle(
                EnumParticleTypes.SPELL_MOB,
                player.posX, player.posY + 1, player.posZ,
                20,
                0.5, 0.5, 0.5,
                0.0
            );

            // 怪物处的效果获得粒子
            world.spawnParticle(
                EnumParticleTypes.VILLAGER_HAPPY,
                attacker.posX, attacker.posY + 1, attacker.posZ,
                15,
                0.3, 0.3, 0.3,
                0.0
            );
        }
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 每10秒清理过期的免疫记录
        if (data.getTickCount() % 200 == 0) {
            cleanupImmunity(entity.world.getTotalWorldTime());
        }
    }

    /**
     * 清理过期的免疫记录
     */
    private void cleanupImmunity(long currentTime) {
        Iterator<Map.Entry<UUID, Long>> it = POTION_IMMUNITY.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (currentTime >= entry.getValue()) {
                it.remove();
            }
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
