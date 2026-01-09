package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;

/**
 * 反转词条 - 伤害反转机制
 *
 * 机制：
 * 1. 对该生物造成的伤害越低，实际伤害越高
 * 2. 对该生物造成的伤害越高，实际伤害越低
 * 3. 设计理念：迫使玩家使用弱武器，颠覆常规策略
 *
 * 公式：实际伤害 = 基准伤害 * (2 - 原始伤害/基准伤害)
 * 当原始伤害 = 基准伤害时，实际伤害 = 基准伤害
 * 当原始伤害 < 基准伤害时，实际伤害 > 基准伤害
 * 当原始伤害 > 基准伤害时，实际伤害 < 基准伤害
 */
public class ReversalAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "reversal");

    /** 基准伤害值 */
    private static final float BASE_DAMAGE = 8.0f;

    /** 最小伤害 */
    private static final float MIN_DAMAGE = 1.0f;

    /** 最大伤害 */
    private static final float MAX_DAMAGE = 30.0f;

    public ReversalAffix() {
        super(
            ID,
            AffixType.DEFENSIVE,
            50,     // 中等权重
            5.0f    // 难度5以上
        );
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        if (damage <= 0) return damage;

        int tier = getTier(entity);

        // 计算基准伤害（随tier提升）
        float baseDamage = BASE_DAMAGE + (tier * 0.5f);

        // 应用反转公式
        // 实际伤害 = 基准伤害 * (2 - 原始伤害/基准伤害)
        float ratio = damage / baseDamage;
        float reversedDamage = baseDamage * (2.0f - ratio);

        // 限制伤害范围
        reversedDamage = Math.max(MIN_DAMAGE, reversedDamage);
        reversedDamage = Math.min(MAX_DAMAGE, reversedDamage);

        return reversedDamage;
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
