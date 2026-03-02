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
import net.minecraft.util.ResourceLocation;

/**
 * 破盾词条 - 玩家吸收值（黄心）越高，受到的伤害越高
 *
 * 机制：
 * 1. 检测目标玩家的吸收值（黄色心）
 * 2. 吸收值越高，受到的伤害加成越大
 * 3. 设计理念：针对金苹果/吸收药水流派
 * 4. 伤害加成比例较高，因为吸收值本身就是额外防护
 */
public class ShieldBreakerAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "shield_breaker");

    /** 每点吸收值增加的伤害百分比 */
    private static final float DAMAGE_PER_ABSORPTION = 0.08f;  // 8%每点

    /** 最大伤害加成 */
    private static final float MAX_DAMAGE_BONUS = 2.5f;  // 最多+250%

    public ShieldBreakerAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            75,     // 中等权重
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

        // 获取玩家吸收值
        float absorptionAmount = player.getAbsorptionAmount();

        // 无吸收值不触发
        if (absorptionAmount <= 0) {
            return damage;
        }

        // 计算伤害加成
        float tierMultiplier = 1.0f + (tier * 0.15f);  // 每tier增加15%效果
        float damageBonus = absorptionAmount * DAMAGE_PER_ABSORPTION * tierMultiplier;

        // 限制最大加成
        damageBonus = Math.min(damageBonus, MAX_DAMAGE_BONUS);

        return damage * (1.0f + damageBonus);
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
