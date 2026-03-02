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
 * 巨人杀手词条 - 玩家最大生命值越高，受到的伤害越高
 *
 * 机制：
 * 1. 检测目标玩家的最大生命值
 * 2. 超过基础生命值(20)的部分会增加受到的伤害
 * 3. 设计理念：惩罚堆血玩家，鼓励技术流战斗
 */
public class GiantSlayerAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "giant_slayer");

    /** 基础生命值（原版20生命=10心） */
    private static final float BASE_HEALTH = 20.0f;

    /** 每点额外生命增加的伤害百分比 */
    private static final float DAMAGE_PER_HEALTH = 0.02f;  // 2%每点

    /** 最大伤害加成 */
    private static final float MAX_DAMAGE_BONUS = 2.0f;  // 最多+200%

    public GiantSlayerAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            70,     // 中等权重
            3.0f    // 难度3以上
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 获取玩家最大生命值
        float maxHealth = player.getMaxHealth();

        // 只有超过基础生命值的部分才计算
        if (maxHealth <= BASE_HEALTH) {
            return damage;
        }

        // 计算额外生命值
        float extraHealth = maxHealth - BASE_HEALTH;

        // 计算伤害加成
        float tierMultiplier = 1.0f + (tier * 0.1f);
        float damageBonus = extraHealth * DAMAGE_PER_HEALTH * tierMultiplier;

        // 检查圣盾勋章饰品反制（giantslayer类型）
        float giantReduction = com.adversity.item.bauble.BaubleHelper.getReduction(player, "giantslayer");
        if (giantReduction > 0) {
            damageBonus *= (1.0f - giantReduction);
        }

        // 限制最大加成
        damageBonus = Math.min(damageBonus, MAX_DAMAGE_BONUS);

        return damage * (1.0f + damageBonus);
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
