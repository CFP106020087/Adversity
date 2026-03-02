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
 * 外神词条 - 受到的伤害变为1/4次方（四次根）
 *
 * 机制：
 * 1. 怪物受到的伤害应用pow(x, 0.25)函数
 * 2. 例如：256伤害 → pow(256, 0.25) = 4伤害
 * 3. 设计理念：终极防御词条，几乎免疫爆发伤害
 *
 * 伤害转换示例：
 * - 16伤害 → 2伤害
 * - 81伤害 → 3伤害
 * - 256伤害 → 4伤害
 * - 10000伤害 → 10伤害
 *
 * 与英雄、神明词条互斥
 * 只能通过飞升词条获得
 */
public class OuterGodAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "outer_god");

    /** 最小伤害保底 */
    private static final float MIN_DAMAGE = 1.0f;

    public OuterGodAffix() {
        super(
            ID,
            AffixType.DEFENSIVE,
            5,       // 极低权重（最强词条）
            10.0f    // 难度10（一般不自然生成，只能通过飞升获得）
        );
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        if (damage <= 0) return damage;

        // 应用四次根
        float reducedDamage = (float) Math.pow(damage, 0.25);

        // 保证最小伤害
        return Math.max(reducedDamage, MIN_DAMAGE);
    }

    @Override
    public boolean isCompatibleWith(IAffix other) {
        // 与英雄、神明词条互斥
        ResourceLocation otherId = other.getId();
        if (otherId.equals(HeroAffix.ID) || otherId.equals(DivineAffix.ID)) {
            return false;
        }
        return super.isCompatibleWith(other);
    }
}
