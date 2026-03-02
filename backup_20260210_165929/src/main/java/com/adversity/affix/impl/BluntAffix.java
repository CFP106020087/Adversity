package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * 钝击词条 - 玩家护甲越高，受到的伤害越高
 *
 * 机制：
 * 1. 检测目标玩家的护甲值
 * 2. 护甲值越高，受到的伤害加成越大
 * 3. 设计理念：反坦克设计，鼓励轻装战斗
 * 4. 伤害加成有上限，避免全身附魔钻石甲变成自杀
 */
public class BluntAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "blunt");

    /** 每点护甲增加的伤害百分比 */
    private static final float DAMAGE_PER_ARMOR = 0.04f;  // 4%每点

    /** 最大伤害加成 */
    private static final float MAX_DAMAGE_BONUS = 1.5f;  // 最多+150%

    /** 护甲阈值（低于此值不触发效果） */
    private static final int ARMOR_THRESHOLD = 4;

    public BluntAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            80,     // 中等权重
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

        // 获取玩家护甲值
        double armorValue = player.getEntityAttribute(SharedMonsterAttributes.ARMOR).getAttributeValue();

        // 低护甲不触发
        if (armorValue <= ARMOR_THRESHOLD) {
            return damage;
        }

        // 计算有效护甲（超过阈值的部分）
        double effectiveArmor = armorValue - ARMOR_THRESHOLD;

        // 计算伤害加成（每点护甲4%，随tier提升）
        float tierMultiplier = 1.0f + (tier * 0.1f);
        float damageBonus = (float) (effectiveArmor * DAMAGE_PER_ARMOR * tierMultiplier);

        // 限制最大加成
        damageBonus = Math.min(damageBonus, MAX_DAMAGE_BONUS);

        return damage * (1.0f + damageBonus);
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
