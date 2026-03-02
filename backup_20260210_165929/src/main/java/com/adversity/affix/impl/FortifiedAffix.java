package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.ResourceLocation;

import java.util.UUID;

/**
 * 铁壁词条 - 血量越低护甲越高
 *
 * 机制：
 * 1. 50%血量以下触发
 * 2. 每缺1%血量增加1.5%护甲
 * 3. 30%血量以下额外增加护甲
 * 4. 最高+150%护甲
 * 5. 设计理念：让低血怪物更难击杀
 */
public class FortifiedAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "fortified");

    /** 属性修改器UUID */
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("8f3a9c2d-7e91-4b6f-a8c3-2d5e7f9a2b4c");

    /** 触发阈值 (50%血量以下) */
    private static final float TRIGGER_THRESHOLD = 0.5f;

    /** 强化阈值 (30%血量以下) */
    private static final float ENHANCED_THRESHOLD = 0.3f;

    /** 基础护甲加成比例 (每缺1%血量) */
    private static final float ARMOR_PER_PERCENT = 0.015f;  // 1.5%

    /** 强化护甲加成比例 (30%以下额外) */
    private static final float ENHANCED_ARMOR_PER_PERCENT = 0.03f;  // 3%

    /** 最大护甲加成 */
    private static final float MAX_ARMOR_BONUS = 1.5f;  // 150%

    public FortifiedAffix() {
        super(
            ID,
            AffixType.DEFENSIVE,
            75,     // 中等权重
            2.0f    // 难度2以上出现
        );
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 每10tick更新一次护甲 (0.5秒)
        if (data.getTickCount() % 10 != 0) {
            return;
        }

        float healthPercent = entity.getHealth() / entity.getMaxHealth();
        int tier = getTier(entity);

        // 计算护甲加成
        float armorBonus = calculateArmorBonus(healthPercent, tier);

        // 更新护甲修改器
        updateArmorModifier(entity, armorBonus);
    }

    /**
     * 计算护甲加成
     */
    private float calculateArmorBonus(float healthPercent, int tier) {
        // 血量高于阈值，无加成
        if (healthPercent >= TRIGGER_THRESHOLD) {
            return 0f;
        }

        float tierMultiplier = 1.0f + (tier * 0.1f);
        float bonus = 0f;

        // 50%-30%: 基础加成
        if (healthPercent >= ENHANCED_THRESHOLD) {
            float missingPercent = TRIGGER_THRESHOLD - healthPercent;
            bonus = missingPercent * ARMOR_PER_PERCENT * 100;  // 转换为百分比
        } else {
            // 30%以下: 基础加成 + 强化加成
            float baseBonus = (TRIGGER_THRESHOLD - ENHANCED_THRESHOLD) * ARMOR_PER_PERCENT * 100;
            float enhancedMissingPercent = ENHANCED_THRESHOLD - healthPercent;
            float enhancedBonus = enhancedMissingPercent * ENHANCED_ARMOR_PER_PERCENT * 100;
            bonus = baseBonus + enhancedBonus;
        }

        // 应用tier加成并限制最大值
        bonus = bonus * tierMultiplier;
        return Math.min(bonus / 100f, MAX_ARMOR_BONUS);  // 转回小数
    }

    /**
     * 更新护甲修改器
     */
    private void updateArmorModifier(EntityLiving entity, float armorBonus) {
        IAttributeInstance armorAttr = entity.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armorAttr == null) return;

        // 移除旧修改器
        AttributeModifier oldModifier = armorAttr.getModifier(ARMOR_MODIFIER_UUID);
        if (oldModifier != null) {
            armorAttr.removeModifier(oldModifier);
        }

        // 应用新修改器（如果有加成）
        if (armorBonus > 0) {
            AttributeModifier newModifier = new AttributeModifier(
                ARMOR_MODIFIER_UUID,
                "Fortified armor bonus",
                armorBonus,
                2  // 乘法运算
            );
            armorAttr.applyModifier(newModifier);
        }
    }

    @Override
    public void onRemove(EntityLiving entity, IAffixData data) {
        // 移除护甲修改器
        IAttributeInstance armorAttr = entity.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(ARMOR_MODIFIER_UUID);
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
