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
 * 迅捷词条 - 增加移动速度
 *
 * 效果：怪物移动速度提升，更难逃跑
 * 速度加成随等级提升
 */
public class HasteAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "haste");

    /** 属性修改器UUID */
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("7e0292f2-9434-48d5-a29f-9583af7df27f");

    /** 基础速度加成 */
    private static final double BASE_SPEED_BONUS = 0.15;  // +15%

    /** 每等级额外速度加成 */
    private static final double SPEED_PER_TIER = 0.05;  // +5%

    public HasteAffix() {
        super(
            ID,
            AffixType.UTILITY,
            90,     // 较高权重（常见词条）
            1.0f    // 难度1以上出现
        );
    }

    @Override
    public void onApply(EntityLiving entity, IAffixData data) {
        int tier = getTier(entity);

        // 计算速度加成
        double speedBonus = BASE_SPEED_BONUS + (tier * SPEED_PER_TIER);
        speedBonus = Math.min(speedBonus, 0.5);  // 最多+50%

        // 应用速度修改器
        IAttributeInstance speedAttr = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            // 移除已存在的修改器（防止重复）
            speedAttr.removeModifier(SPEED_MODIFIER_UUID);

            // 添加新的修改器
            AttributeModifier modifier = new AttributeModifier(
                SPEED_MODIFIER_UUID,
                "Adversity Haste",
                speedBonus,
                2  // 乘法运算
            );
            speedAttr.applyModifier(modifier);
        }
    }

    @Override
    public void onRemove(EntityLiving entity, IAffixData data) {
        // 移除速度修改器
        IAttributeInstance speedAttr = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_UUID);
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
