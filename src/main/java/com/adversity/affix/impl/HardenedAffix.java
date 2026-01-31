package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

/**
 * 硬化词条 - 免击退+霸体
 *
 * 机制：
 * 1. 完全免疫击退效果
 * 2. 受伤后1秒内获得30%伤害减免（霸体）
 * 3. 霸体有2秒冷却
 * 4. 设计理念：让怪物难以风筝
 */
public class HardenedAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "hardened");

    /** 霸体伤害减免 */
    private static final float DAMAGE_REDUCTION = 0.3f;  // 30%

    /** 霸体持续时间 (tick) */
    private static final int SUPER_ARMOR_DURATION = 20;  // 1秒

    /** 霸体冷却时间 (tick) */
    private static final int SUPER_ARMOR_COOLDOWN = 40;  // 2秒

    public HardenedAffix() {
        super(
            ID,
            AffixType.DEFENSIVE,
            65,     // 中等权重
            4.0f    // 难度4以上出现
        );
    }

    @Override
    public void onApply(EntityLiving entity, IAffixData data) {
        // 设置免击退 - 通过设置knockbackResistance
        entity.getEntityAttribute(
            net.minecraft.entity.SharedMonsterAttributes.KNOCKBACK_RESISTANCE
        ).setBaseValue(1.0);
    }

    @Override
    public void onRemove(EntityLiving entity, IAffixData data) {
        // 恢复击退抗性
        entity.getEntityAttribute(
            net.minecraft.entity.SharedMonsterAttributes.KNOCKBACK_RESISTANCE
        ).setBaseValue(0.0);
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        int tier = getTier(entity);
        float finalDamage = damage;

        // 检查霸体状态
        int superArmorTime = data.getCustomInt("superArmorTime");
        int cooldown = data.getCooldown();

        if (superArmorTime > 0) {
            // 霸体激活中，减少伤害
            float reduction = DAMAGE_REDUCTION + (tier * 0.03f);  // 每tier+3%
            reduction = Math.min(reduction, 0.5f);  // 最多50%
            finalDamage = damage * (1.0f - reduction);

            // 播放霸体效果
            playSuperArmorEffect(entity);
        } else if (cooldown <= 0) {
            // 触发霸体
            data.setCustomInt("superArmorTime", SUPER_ARMOR_DURATION);
            data.setCooldown(SUPER_ARMOR_COOLDOWN + SUPER_ARMOR_DURATION);

            // 第一次触发时也享受减伤
            float reduction = DAMAGE_REDUCTION + (tier * 0.03f);
            reduction = Math.min(reduction, 0.5f);
            finalDamage = damage * (1.0f - reduction);

            playSuperArmorEffect(entity);
        }

        return finalDamage;
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 更新霸体时间
        int superArmorTime = data.getCustomInt("superArmorTime");
        if (superArmorTime > 0) {
            data.setCustomInt("superArmorTime", superArmorTime - 1);
        }

        // 冷却由IAffixData自动管理
    }

    /**
     * 播放霸体效果
     */
    private void playSuperArmorEffect(EntityLiving entity) {
        if (!entity.world.isRemote && entity.world instanceof WorldServer) {
            ((WorldServer) entity.world).spawnParticle(
                EnumParticleTypes.BLOCK_DUST,
                entity.posX, entity.posY + entity.height / 2, entity.posZ,
                10,
                0.3, 0.3, 0.3,
                0.05,
                net.minecraft.init.Blocks.IRON_BLOCK.getDefaultState().getBlock().getMetaFromState(
                    net.minecraft.init.Blocks.IRON_BLOCK.getDefaultState()
                )
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
