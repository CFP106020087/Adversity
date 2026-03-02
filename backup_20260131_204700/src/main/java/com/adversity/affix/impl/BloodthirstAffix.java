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
import net.minecraft.util.ResourceLocation;

/**
 * 渴血词条 - 玩家生命值越低，怪物伤害越高
 *
 * 机制：
 * 1. 检测目标玩家的当前生命值与最大生命值的比例
 * 2. 生命值百分比越低，伤害加成越高
 * 3. 50%血以下开始触发，30%以下大幅增强
 * 4. 设计理念：追杀残血，增加紧张感
 */
public class BloodthirstAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "bloodthirst");

    /** 开始触发的生命百分比阈值 */
    private static final float TRIGGER_THRESHOLD = 0.5f;  // 50%以下触发

    /** 强化触发的生命百分比阈值 */
    private static final float ENHANCED_THRESHOLD = 0.3f;  // 30%以下强化

    /** 基础伤害加成（50%血时） */
    private static final float BASE_DAMAGE_BONUS = 0.3f;  // +30%

    /** 最大伤害加成（接近0血时） */
    private static final float MAX_DAMAGE_BONUS = 2.0f;  // +200%

    public BloodthirstAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            85,     // 较高权重
            2.0f    // 难度2以上
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 计算生命百分比
        float healthPercent = player.getHealth() / player.getMaxHealth();

        // 高于阈值不触发
        if (healthPercent >= TRIGGER_THRESHOLD) {
            return damage;
        }

        // 计算伤害加成
        float damageBonus = calculateDamageBonus(healthPercent, tier);

        // 发送血腥视觉效果
        if (healthPercent <= ENHANCED_THRESHOLD) {
            VisualEffectHelper.sendToPlayer(player, VisualEffectType.BLOOD_MARK, 30, 0.6f, attacker.getEntityId());
        }

        return damage * (1.0f + damageBonus);
    }

    /**
     * 计算伤害加成
     * 使用指数曲线，血量越低加成增长越快
     */
    private float calculateDamageBonus(float healthPercent, int tier) {
        float tierMultiplier = 1.0f + (tier * 0.1f);

        // 将血量百分比映射到0-1范围（50%->0, 0%->1）
        float ratio = (TRIGGER_THRESHOLD - healthPercent) / TRIGGER_THRESHOLD;

        // 使用二次曲线加速增长
        float bonus;
        if (healthPercent > ENHANCED_THRESHOLD) {
            // 50%-30%: 线性增长
            bonus = BASE_DAMAGE_BONUS * (ratio / (TRIGGER_THRESHOLD - ENHANCED_THRESHOLD) * TRIGGER_THRESHOLD);
        } else {
            // 30%以下: 加速增长
            float baseBonus = BASE_DAMAGE_BONUS;
            float extraRatio = (ENHANCED_THRESHOLD - healthPercent) / ENHANCED_THRESHOLD;
            bonus = baseBonus + (MAX_DAMAGE_BONUS - baseBonus) * (extraRatio * extraRatio);
        }

        return Math.min(bonus * tierMultiplier, MAX_DAMAGE_BONUS);
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
