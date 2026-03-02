package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * 蜕皮词条 - 致命伤时触发复活
 *
 * 机制：
 * 1. 受到致命伤害时有25%概率触发蜕皮
 * 2. 恢复30%最大血量
 * 3. 清除所有debuff
 * 4. 获得短暂速度加成
 * 5. 每只怪物仅触发一次
 * 6. 设计理念：给怪物"第二条命"
 */
public class SheddingAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "shedding");

    /** 触发概率 */
    private static final float TRIGGER_CHANCE = 0.25f; // 25%

    /** 恢复血量比例 */
    private static final float HEAL_RATIO = 0.3f; // 30%

    /** 速度加成持续时间 (tick) */
    private static final int SPEED_DURATION = 100; // 5秒

    private static final Random RANDOM = new Random();

    public SheddingAffix() {
        super(
                ID,
                AffixType.DEFENSIVE,
                50, // 较低权重（强力词条）
                5.0f // 难度5以上出现
        );
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        // 检查是否已经触发过蜕皮
        if (data.getCustomInt("hasShed") > 0) {
            return damage;
        }

        // 检查是否会致死
        if (entity.getHealth() - damage > 0) {
            return damage;
        }

        int tier = getTier(entity);

        // 计算触发概率
        float chance = TRIGGER_CHANCE + (tier * 0.03f);
        chance = Math.min(chance, 0.5f); // 最多50%

        // 检查攻击者是否有反制饰品（锚定之心 - 反制闪避/蜕皮）
        if (source.getTrueSource() instanceof net.minecraft.entity.player.EntityPlayer) {
            net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) source
                    .getTrueSource();
            if (com.adversity.item.bauble.BaubleHelper.tryBlockEffect(player, "evasion", entity)) {
                return damage; // 蜕皮被阻止
            }
        }

        // 随机判定
        if (RANDOM.nextFloat() < chance) {
            // 触发蜕皮
            triggerShedding(entity, data, tier);

            // 减少伤害使其存活
            return entity.getHealth() - 1.0f;
        }

        return damage;
    }

    /**
     * 触发蜕皮效果
     */
    private void triggerShedding(EntityLiving entity, IAffixData data, int tier) {
        // 标记已蜕皮
        data.setCustomInt("hasShed", 1);

        // 恢复血量
        float healAmount = entity.getMaxHealth() * (HEAL_RATIO + tier * 0.02f);
        entity.setHealth(Math.min(entity.getHealth() + healAmount, entity.getMaxHealth()));

        // 清除所有负面效果
        entity.clearActivePotions();

        // 获得速度加成
        entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, SPEED_DURATION, 1));

        // 播放蜕皮效果
        playShedEffect(entity);
    }

    /**
     * 播放蜕皮效果
     */
    private void playShedEffect(EntityLiving entity) {
        if (entity.world.isRemote)
            return;

        // 音效
        entity.world.playSound(
                null,
                entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_SLIME_SQUISH,
                SoundCategory.HOSTILE,
                1.0f,
                0.8f);

        // 粒子效果
        if (entity.world instanceof WorldServer) {
            // 蜕皮碎片
            ((WorldServer) entity.world).spawnParticle(
                    EnumParticleTypes.SLIME,
                    entity.posX, entity.posY + entity.height / 2, entity.posZ,
                    30,
                    0.5, 0.5, 0.5,
                    0.1);
            // 治愈效果
            ((WorldServer) entity.world).spawnParticle(
                    EnumParticleTypes.HEART,
                    entity.posX, entity.posY + entity.height + 0.5, entity.posZ,
                    5,
                    0.3, 0.2, 0.3,
                    0.0);
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
