package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
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

        // 检查攻击者是否有平衡护符反制
        if (source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source.getTrueSource();
            float reduction = com.adversity.item.bauble.BaubleHelper.getReduction(player, "reversal");
            if (reduction >= 1.0f) {
                // 完全反制，伤害不变
                return damage;
            } else if (reduction > 0) {
                // 部分反制，减弱逆转效果
                return applyPartialReversal(entity, damage, reduction);
            }
        }

        int tier = getTier(entity);

        // 计算基准伤害（随tier提升）
        float baseDamage = BASE_DAMAGE + (tier * 0.5f);

        // 应用反转公式
        float ratio = damage / baseDamage;
        float reversedDamage = baseDamage * (2.0f - ratio);

        // 限制伤害范围
        reversedDamage = Math.max(MIN_DAMAGE, reversedDamage);
        reversedDamage = Math.min(MAX_DAMAGE, reversedDamage);

        return reversedDamage;
    }

    private float applyPartialReversal(EntityLiving entity, float damage, float reduction) {
        int tier = getTier(entity);
        float baseDamage = BASE_DAMAGE + (tier * 0.5f);
        float ratio = damage / baseDamage;
        float reversedDamage = baseDamage * (2.0f - ratio);
        reversedDamage = Math.max(MIN_DAMAGE, reversedDamage);
        reversedDamage = Math.min(MAX_DAMAGE, reversedDamage);
        // 按反制比例混合原始伤害和逆转伤害
        return damage * reduction + reversedDamage * (1.0f - reduction);
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
