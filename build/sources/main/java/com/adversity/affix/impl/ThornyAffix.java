package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.debuff.DebuffType;
import com.adversity.debuff.PlayerDebuffManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

/**
 * 荆刺词条 - 被近战攻击时叠加刺伤层数
 *
 * 机制：
 * 1. 被玩家近战攻击时，对攻击者叠加"刺伤"debuff
 * 2. 每层刺伤使攻击者受到攻击时额外+10%伤害
 * 3. 满5层时触发"刺穿"，对攻击者造成真实伤害
 * 4. 设计理念：惩罚连续近战，鼓励远程/切换目标
 */
public class ThornyAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "thorny");

    /** 每层刺伤增加的伤害百分比 */
    private static final float DAMAGE_PER_STACK = 0.1f; // 10%

    /** 满层时的刺穿伤害 */
    private static final float PIERCE_DAMAGE = 4.0f; // 2心

    /** 最大层数 */
    private static final int MAX_STACKS = 5;

    public ThornyAffix() {
        super(
                ID,
                AffixType.DEFENSIVE,
                70, // 中等权重
                3.0f // 难度3以上出现
        );
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        // 只响应玩家的近战攻击
        if (!(source.getTrueSource() instanceof EntityPlayer)) {
            return damage;
        }

        // 检查是否为近战伤害（非弹射物）
        if (source.isProjectile() || source.isMagicDamage()) {
            return damage;
        }

        EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
        int tier = getTier(entity);

        // 检查饰品反制（净化徽章 - 抑制叠层）
        float reduction = com.adversity.item.bauble.BaubleHelper.getEffectStrength(attacker, "stack_system");
        if (reduction >= 1.0f) {
            return damage; // 完全免疫刺伤叠加
        }

        // 根据反制概率决定是否叠加
        if (reduction > 0 && com.adversity.item.bauble.BaubleHelper.RANDOM.nextFloat() < reduction) {
            return damage; // 成功抵抗
        }

        // 获取当前刺伤层数
        int currentStacks = PlayerDebuffManager.getStacks(attacker, DebuffType.THORNS);
        int newStacks = Math.min(currentStacks + 1, MAX_STACKS);

        // 叠加刺伤
        PlayerDebuffManager.addStacks(attacker, DebuffType.THORNS, 1, 200, entity.getEntityId());

        // 满层触发刺穿
        if (newStacks >= MAX_STACKS) {
            triggerPierce(attacker, entity, tier);
            // 重置层数
            PlayerDebuffManager.removeDebuff(attacker, DebuffType.THORNS);
        }

        // 播放荆刺效果
        playThornEffect(entity);

        return damage;
    }

    /**
     * 触发刺穿效果
     */
    private void triggerPierce(EntityPlayer target, EntityLiving source, int tier) {
        // 计算刺穿伤害
        float pierceDamage = PIERCE_DAMAGE + (tier * 0.5f);

        // 造成真实伤害
        target.attackEntityFrom(
                new DamageSource("adversity.pierce").setDamageBypassesArmor(),
                pierceDamage);

        // 播放刺穿效果
        if (!target.world.isRemote) {
            target.world.playSound(
                    null,
                    target.posX, target.posY, target.posZ,
                    SoundEvents.ENTITY_PLAYER_HURT,
                    SoundCategory.HOSTILE,
                    1.0f,
                    0.5f);

            if (target.world instanceof WorldServer) {
                ((WorldServer) target.world).spawnParticle(
                        EnumParticleTypes.CRIT,
                        target.posX, target.posY + target.height / 2, target.posZ,
                        20,
                        0.5, 0.5, 0.5,
                        0.1);
            }
        }
    }

    /**
     * 播放荆刺效果
     */
    private void playThornEffect(EntityLiving entity) {
        if (!entity.world.isRemote && entity.world instanceof WorldServer) {
            ((WorldServer) entity.world).spawnParticle(
                    EnumParticleTypes.CRIT_MAGIC,
                    entity.posX, entity.posY + entity.height / 2, entity.posZ,
                    5,
                    0.3, 0.3, 0.3,
                    0.05);
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
