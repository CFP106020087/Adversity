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

        // 检查攻击者是否有反制饰品（雷霆指环 - holy / 破甲符文 - armor_reduction）
        if (source.getTrueSource() instanceof net.minecraft.entity.player.EntityPlayer) {
            net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) source
                    .getTrueSource();
            float holyReduction = com.adversity.item.bauble.BaubleHelper.getReduction(player, "holy");
            float armorReduction = com.adversity.item.bauble.BaubleHelper.getReduction(player, "armor_reduction");
            float pierceReduction = Math.max(holyReduction, armorReduction);
            if (pierceReduction >= 1.0f) {
                return damage; // 完全穿透，不应用立方根
            }
            if (pierceReduction > 0) {
                // 部分穿透：在原始伤害和立方根伤害之间插值
                float reducedDamage = (float) Math.cbrt(damage);
                reducedDamage = Math.max(reducedDamage, MIN_DAMAGE);
                return reducedDamage + (damage - reducedDamage) * pierceReduction;
            }
        }

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
