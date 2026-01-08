package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.affix.IAffix;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;

/**
 * 冰霜词条 - 攻击时减速目标
 *
 * 效果：攻击时给予目标缓慢效果，等级和持续时间随难度增加
 * 与烈焰词条不兼容（冰火互斥）
 */
public class FrostyAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "frosty");

    private static final int BASE_DURATION_TICKS = 60;  // 3秒
    private static final int DURATION_PER_TIER = 20;    // 每等级+1秒

    public FrostyAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            100,    // 标准权重
            0.0f    // 无难度限制
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        int tier = getTier(attacker);

        // 计算减速等级 (0-based, 所以 tier 1-3 = Slowness I, 4-6 = II, 7+ = III)
        int amplifier = Math.min((tier - 1) / 3, 2);

        // 计算持续时间
        int duration = BASE_DURATION_TICKS + (tier * DURATION_PER_TIER);

        // 应用缓慢效果
        target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, duration, amplifier));

        // 播放冰冻音效
        if (!attacker.world.isRemote) {
            attacker.world.playSound(
                null,
                target.posX, target.posY, target.posZ,
                SoundEvents.BLOCK_GLASS_BREAK,
                SoundCategory.HOSTILE,
                0.5f,
                1.5f
            );
        }

        return damage;
    }

    @Override
    public boolean isCompatibleWith(IAffix other) {
        // 与烈焰词条不兼容
        if (other.getId().equals(FieryAffix.ID)) {
            return false;
        }
        return super.isCompatibleWith(other);
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
