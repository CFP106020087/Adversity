package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.stats.StatList;
import net.minecraft.util.ResourceLocation;

import java.util.UUID;

/**
 * 死神词条 - 玩家死亡次数越少，怪物越强
 *
 * 机制：
 * 1. 检测攻击目标（玩家）的死亡次数统计
 * 2. 死亡次数越少，怪物攻击力和移动速度越高
 * 3. 死亡次数超过10次后，效果逐渐减弱
 * 4. 设计理念：惩罚高技术玩家，给新手喘息空间
 */
public class ReaperAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "reaper");

    /** 速度加成修改器UUID */
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("5f3a8c2d-7e91-4b6f-a8c3-2d5e7f9a1b3c");

    /** 最大伤害加成（0次死亡时） */
    private static final float MAX_DAMAGE_BONUS = 2.0f;  // +200% 伤害

    /** 最大速度加成（0次死亡时） */
    private static final double MAX_SPEED_BONUS = 0.5;  // +50% 速度

    /** 死亡次数阈值（超过此值后效果减弱） */
    private static final int DEATH_THRESHOLD = 10;

    /** 死亡次数上限（超过此值效果消失） */
    private static final int DEATH_CAP = 30;

    public ReaperAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            60,     // 较低权重，因为效果强力
            3.0f    // 难度3以上才会出现
        );
    }

    @Override
    public void onApply(EntityLiving entity, IAffixData data) {
        // 初始应用速度加成（基于周围玩家平均死亡次数或默认值）
        updateSpeedModifier(entity, 0);
    }

    @Override
    public void onRemove(EntityLiving entity, IAffixData data) {
        // 移除速度修改器
        IAttributeInstance speedAttr = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            AttributeModifier modifier = speedAttr.getModifier(SPEED_MODIFIER_UUID);
            if (modifier != null) {
                speedAttr.removeModifier(modifier);
            }
        }
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 获取玩家死亡次数
        int deathCount = player.getStatFile().readStat(StatList.DEATHS);

        // 计算伤害倍率
        float damageMultiplier = calculateDamageMultiplier(deathCount, tier);

        // 更新怪物速度（基于当前目标）
        updateSpeedModifier(attacker, deathCount);

        // 应用伤害加成
        return damage * damageMultiplier;
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 每2秒检查一次周围玩家，更新速度
        if (data.getTickCount() % 40 == 0) {
            EntityPlayer nearestPlayer = entity.world.getClosestPlayerToEntity(entity, 16.0);
            if (nearestPlayer != null) {
                int deathCount = nearestPlayer.getStatFile().readStat(StatList.DEATHS);
                updateSpeedModifier(entity, deathCount);
            }
        }
    }

    /**
     * 计算伤害倍率
     * 0次死亡 = 最大加成
     * DEATH_THRESHOLD次死亡 = 一半加成
     * DEATH_CAP次死亡 = 无加成
     */
    private float calculateDamageMultiplier(int deathCount, int tier) {
        if (deathCount >= DEATH_CAP) {
            return 1.0f;  // 死亡太多次，效果消失
        }

        float tierBonus = 1.0f + (tier * 0.1f);  // 每级增加10%效果

        if (deathCount <= 0) {
            // 0次死亡，最大加成
            return 1.0f + (MAX_DAMAGE_BONUS * tierBonus);
        } else if (deathCount <= DEATH_THRESHOLD) {
            // 线性衰减区间
            float ratio = 1.0f - ((float) deathCount / DEATH_THRESHOLD);
            return 1.0f + (MAX_DAMAGE_BONUS * ratio * tierBonus);
        } else {
            // 缓慢衰减区间
            float ratio = 1.0f - ((float) (deathCount - DEATH_THRESHOLD) / (DEATH_CAP - DEATH_THRESHOLD));
            return 1.0f + (MAX_DAMAGE_BONUS * 0.5f * ratio * tierBonus);
        }
    }

    /**
     * 更新速度修改器
     */
    private void updateSpeedModifier(EntityLiving entity, int deathCount) {
        IAttributeInstance speedAttr = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        // 移除旧修改器
        AttributeModifier oldModifier = speedAttr.getModifier(SPEED_MODIFIER_UUID);
        if (oldModifier != null) {
            speedAttr.removeModifier(oldModifier);
        }

        // 计算新速度加成
        double speedBonus = calculateSpeedBonus(deathCount);
        if (speedBonus > 0) {
            AttributeModifier newModifier = new AttributeModifier(
                SPEED_MODIFIER_UUID,
                "Reaper speed bonus",
                speedBonus,
                2  // 乘法运算
            );
            speedAttr.applyModifier(newModifier);
        }
    }

    /**
     * 计算速度加成
     */
    private double calculateSpeedBonus(int deathCount) {
        if (deathCount >= DEATH_CAP) {
            return 0;
        }

        if (deathCount <= 0) {
            return MAX_SPEED_BONUS;
        } else if (deathCount <= DEATH_THRESHOLD) {
            float ratio = 1.0f - ((float) deathCount / DEATH_THRESHOLD);
            return MAX_SPEED_BONUS * ratio;
        } else {
            float ratio = 1.0f - ((float) (deathCount - DEATH_THRESHOLD) / (DEATH_CAP - DEATH_THRESHOLD));
            return MAX_SPEED_BONUS * 0.3 * ratio;
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
