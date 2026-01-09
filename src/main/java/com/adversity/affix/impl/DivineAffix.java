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
 * 神明词条 - 受到的伤害变为1/3次方（立方根）
 *
 * 机制：
 * 1. 怪物受到的伤害应用cbrt函数
 * 2. 例如：125伤害 → cbrt(125) = 5伤害
 * 3. 设计理念：极高伤害也被压制，需要持续输出
 *
 * 伤害转换示例：
 * - 8伤害 → 2伤害
 * - 27伤害 → 3伤害
 * - 125伤害 → 5伤害
 * - 1000伤害 → 10伤害
 *
 * 与英雄、外神词条互斥
 */
public class DivineAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "divine");

    /** 最小伤害保底 */
    private static final float MIN_DAMAGE = 1.0f;

    public DivineAffix() {
        super(
            ID,
            AffixType.DEFENSIVE,
            15,      // 极低权重（非常强力）
            8.0f     // 难度8以上
        );
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        if (damage <= 0) return damage;

        // 应用立方根
        float reducedDamage = (float) Math.cbrt(damage);

        // 保证最小伤害
        return Math.max(reducedDamage, MIN_DAMAGE);
    }

    @Override
    public boolean isCompatibleWith(IAffix other) {
        // 与英雄、外神词条互斥
        ResourceLocation otherId = other.getId();
        if (otherId.equals(HeroAffix.ID) || otherId.equals(OuterGodAffix.ID)) {
            return false;
        }
        return super.isCompatibleWith(other);
    }
}
