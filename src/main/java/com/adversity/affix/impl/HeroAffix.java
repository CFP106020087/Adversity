package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.affix.IAffix;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;

/**
 * 英雄词条 - 受到的伤害变为1/2次方（平方根）
 *
 * 机制：
 * 1. 怪物受到的伤害应用sqrt函数
 * 2. 例如：100伤害 → sqrt(100) = 10伤害
 * 3. 设计理念：高伤害被大幅削弱，低伤害基本不变
 *
 * 伤害转换示例：
 * - 4伤害 → 2伤害
 * - 16伤害 → 4伤害
 * - 100伤害 → 10伤害
 * - 400伤害 → 20伤害
 *
 * 与神明、外神词条互斥，可通过飞升词条进化
 */
public class HeroAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "hero");

    /** 最小伤害保底 */
    private static final float MIN_DAMAGE = 1.0f;

    public HeroAffix() {
        super(
            ID,
            AffixType.DEFENSIVE,
            30,      // 低权重（强力词条）
            6.0f     // 难度6以上
        );
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        if (damage <= 0) return damage;

        // 检查攻击者是否有反制饰品（破甲符文 - armor_reduction）
        if (source.getTrueSource() instanceof net.minecraft.entity.player.EntityPlayer) {
            net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) source
                    .getTrueSource();
            float armorReduction = com.adversity.item.bauble.BaubleHelper.getReduction(player, "armor_reduction");
            if (armorReduction >= 1.0f) {
                return damage; // 完全穿透
            }
            if (armorReduction > 0) {
                float reducedDamage = (float) Math.sqrt(damage);
                reducedDamage = Math.max(reducedDamage, MIN_DAMAGE);
                return reducedDamage + (damage - reducedDamage) * armorReduction;
            }
        }

        // 应用平方根
        float reducedDamage = (float) Math.sqrt(damage);

        // 保证最小伤害
        return Math.max(reducedDamage, MIN_DAMAGE);
    }

    @Override
    public boolean isCompatibleWith(IAffix other) {
        // 与神明、外神词条互斥
        ResourceLocation otherId = other.getId();
        if (otherId.equals(DivineAffix.ID) || otherId.equals(OuterGodAffix.ID)) {
            return false;
        }
        return super.isCompatibleWith(other);
    }
}
