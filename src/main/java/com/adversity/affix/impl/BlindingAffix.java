package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;

import java.util.Random;

/**
 * 致盲词条 - 攻击时有几率使目标失明
 *
 * 效果：攻击时有几率给予目标失明效果
 * 几率和持续时间随等级提升
 */
public class BlindingAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "blinding");

    /** 基础致盲几率 */
    private static final float BASE_CHANCE = 0.25f;  // 25%

    /** 每等级增加的几率 */
    private static final float CHANCE_PER_TIER = 0.05f;  // +5%

    /** 基础致盲时间（tick） */
    private static final int BASE_DURATION = 40;  // 2秒

    /** 每等级增加的时间（tick） */
    private static final int DURATION_PER_TIER = 20;  // +1秒

    private static final Random RANDOM = new Random();

    public BlindingAffix() {
        super(
            ID,
            AffixType.UTILITY,
            80,     // 中等权重
            2.0f    // 难度2以上出现
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        int tier = getTier(attacker);

        // 计算致盲几率
        float blindChance = BASE_CHANCE + (tier * CHANCE_PER_TIER);
        blindChance = Math.min(blindChance, 0.6f);  // 最多60%

        // 随机判定
        if (RANDOM.nextFloat() < blindChance) {
            // 计算持续时间
            int duration = BASE_DURATION + (tier * DURATION_PER_TIER);

            // 应用失明效果
            target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, duration, 0));

            // 播放音效
            if (!attacker.world.isRemote) {
                attacker.world.playSound(
                    null,
                    target.posX, target.posY, target.posZ,
                    SoundEvents.ENTITY_SQUID_SQUIRT,
                    SoundCategory.HOSTILE,
                    0.5f,
                    0.8f
                );
            }
        }

        return damage;
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
