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
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.WorldServer;

/**
 * 凋零词条 - 攻击时施加凋零效果
 *
 * 效果：攻击时给予目标凋零效果（持续伤害）
 * 凋零等级和持续时间随怪物等级提升
 */
public class WitheringAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "withering");

    /** 基础凋零时间（tick） */
    private static final int BASE_DURATION = 60;  // 3秒

    /** 每等级增加的时间（tick） */
    private static final int DURATION_PER_TIER = 20;  // +1秒

    public WitheringAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            70,     // 中等权重
            5.0f    // 难度5以上出现（较强力）
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        int tier = getTier(attacker);

        // 计算凋零等级（0-based，所以 amplifier 0 = Wither I）
        // T5-T6: Wither I, T7-T8: Wither II, T9-T10: Wither III
        int amplifier = Math.min((tier - 5) / 2, 2);

        // 计算持续时间
        int duration = BASE_DURATION + (tier * DURATION_PER_TIER);

        // 应用凋零效果
        target.addPotionEffect(new PotionEffect(MobEffects.WITHER, duration, amplifier));

        // 生成粒子和音效
        if (!attacker.world.isRemote) {
            attacker.world.playSound(
                null,
                target.posX, target.posY, target.posZ,
                SoundEvents.ENTITY_WITHER_AMBIENT,
                SoundCategory.HOSTILE,
                0.3f,
                1.5f
            );

            if (attacker.world instanceof WorldServer) {
                ((WorldServer) attacker.world).spawnParticle(
                    EnumParticleTypes.SMOKE_NORMAL,
                    target.posX, target.posY + target.height / 2, target.posZ,
                    8,  // 数量
                    0.3, 0.3, 0.3,  // 偏移
                    0.02  // 速度
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
