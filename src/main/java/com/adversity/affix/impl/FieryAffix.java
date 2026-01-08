package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;

/**
 * 烈焰词条 - 攻击时点燃目标
 *
 * 参考自 Infernal Mobs 的 Fiery 和 Champions 的 Molten
 * 效果：攻击时使目标燃烧，燃烧时间随难度增加
 */
public class FieryAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "fiery");

    /** 基础燃烧时间（秒） */
    private static final int BASE_BURN_SECONDS = 3;

    /** 每难度等级增加的燃烧时间（秒） */
    private static final int BURN_SECONDS_PER_TIER = 1;

    public FieryAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,  // 攻击型词条
            100,                   // 权重：100（标准权重）
            0.0f                   // 最低难度：0（可以在任何难度出现）
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        // 计算燃烧时间：基础时间 + 每等级额外时间
        int tier = data.getTier();
        int burnSeconds = BASE_BURN_SECONDS + (tier * BURN_SECONDS_PER_TIER);

        // 点燃目标（参数是 tick 数，1秒=20tick）
        target.setFire(burnSeconds);

        // 播放火焰音效
        if (!attacker.world.isRemote) {
            attacker.world.playSound(
                null,
                target.posX, target.posY, target.posZ,
                SoundEvents.ENTITY_BLAZE_SHOOT,
                SoundCategory.HOSTILE,
                0.5f,
                1.0f + (attacker.world.rand.nextFloat() - 0.5f) * 0.2f
            );
        }

        return damage; // 不修改伤害值
    }

    @Override
    public void onApply(EntityLiving entity, IAffixData data) {
        // 烈焰怪物自身免疫火焰
        entity.setEntityInvulnerable(false); // 确保不是无敌
        // 注意：在 Minecraft 1.12.2 中，我们可以通过属性或 NBT 来实现火焰免疫
        // 但简单起见，我们在 onTick 中处理
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 烈焰怪物自身灭火（免疫火焰伤害）
        if (entity.isBurning()) {
            entity.extinguish();
        }
    }
}
