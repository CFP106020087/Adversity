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
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;

/**
 * 反射词条 - 受到伤害时反弹部分伤害给攻击者
 *
 * 效果：受到近战伤害时，对攻击者造成反弹伤害
 * 反弹比例随等级提升
 */
public class ReflectiveAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "reflective");

    /** 基础反弹比例 */
    private static final float BASE_REFLECT = 0.15f;  // 15%

    /** 每等级增加的反弹比例 */
    private static final float REFLECT_PER_TIER = 0.05f;  // +5%

    /** 反弹伤害上限倍数（相对于怪物最大生命值） */
    private static final float MAX_REFLECT_RATIO = 0.1f;

    public ReflectiveAffix() {
        super(
            ID,
            AffixType.DEFENSIVE,
            70,     // 中等权重
            4.0f    // 难度4以上才出现
        );
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        // 不反弹荆棘伤害（防止无限反弹循环）
        if (source.getDamageType().equals("thorns")) {
            return damage;
        }

        // 只反弹来自生物的伤害
        if (source.getTrueSource() instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();

            // 不反弹自己的伤害（防止自我伤害循环）
            if (attacker == entity) {
                return damage;
            }

            // 不反弹其他拥有反射词条的怪物的伤害（防止怪物间无限反弹）
            if (attacker instanceof EntityLiving) {
                IAdversityCapability attackerCap = CapabilityHandler.getCapability((EntityLiving) attacker);
                if (attackerCap != null && attackerCap.hasAffix(this)) {
                    return damage;
                }
            }

            int tier = getTier(entity);

            // 检查玩家是否有反制饰品（真伤水晶 - 反制伤害转换）
            if (attacker instanceof net.minecraft.entity.player.EntityPlayer) {
                net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) attacker;
                float reduction = com.adversity.item.bauble.BaubleHelper.getEffectStrength(player, "damage_conversion");
                if (reduction >= 1.0f) {
                    return damage; // 完全免疫反弹
                }
            }

            // 计算反弹比例
            float reflectRatio = BASE_REFLECT + (tier * REFLECT_PER_TIER);
            reflectRatio = Math.min(reflectRatio, 0.5f);  // 最多50%

            // 计算反弹伤害
            float reflectDamage = damage * reflectRatio;


            // 限制单次反弹伤害上限
            float maxReflect = entity.getMaxHealth() * MAX_REFLECT_RATIO;
            reflectDamage = Math.min(reflectDamage, maxReflect);

            // 造成反弹伤害（使用荆棘伤害类型）
            if (reflectDamage > 0) {
                attacker.attackEntityFrom(DamageSource.causeThornsDamage(entity), reflectDamage);

                // 播放音效
                if (!entity.world.isRemote) {
                    entity.world.playSound(
                        null,
                        entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENCHANT_THORNS_HIT,
                        SoundCategory.HOSTILE,
                        0.5f,
                        1.0f
                    );
                }
            }
        }

        return damage;  // 不减少受到的伤害
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
