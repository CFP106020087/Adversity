package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.WorldServer;

/**
 * 吸血词条 - 造成伤害时回复生命
 *
 * 效果：每次攻击回复造成伤害的一定百分比的生命值
 * 吸血比例随等级提升
 */
public class VampiricAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "vampiric");

    /** 基础吸血比例 */
    private static final float BASE_LIFESTEAL = 0.15f;  // 15%

    /** 每等级增加的吸血比例 */
    private static final float LIFESTEAL_PER_TIER = 0.03f;  // +3%

    public VampiricAffix() {
        super(
            ID,
            AffixType.DEFENSIVE,  // 防御型（增加生存能力）
            80,     // 略低权重
            3.0f    // 难度3以上才出现
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        int tier = getTier(attacker);

        // 计算吸血比例
        float lifestealRatio = BASE_LIFESTEAL + (tier * LIFESTEAL_PER_TIER);
        lifestealRatio = Math.min(lifestealRatio, 0.5f);  // 最多50%

        // 计算回复量
        float healAmount = damage * lifestealRatio;

        // 回复生命
        if (healAmount > 0) {
            attacker.heal(healAmount);

            // 生成粒子效果
            if (!attacker.world.isRemote && attacker.world instanceof WorldServer) {
                ((WorldServer) attacker.world).spawnParticle(
                    EnumParticleTypes.DAMAGE_INDICATOR,
                    attacker.posX, attacker.posY + attacker.height * 0.5, attacker.posZ,
                    3,  // 数量
                    0.2, 0.2, 0.2,  // 偏移
                    0.0  // 速度
                );
            }

            // 偶尔播放音效（不要太频繁）
            if (!attacker.world.isRemote && attacker.world.rand.nextFloat() < 0.3f) {
                attacker.world.playSound(
                    null,
                    attacker.posX, attacker.posY, attacker.posZ,
                    SoundEvents.ENTITY_GENERIC_DRINK,
                    SoundCategory.HOSTILE,
                    0.3f,
                    0.8f
                );
            }
        }

        return damage;
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
