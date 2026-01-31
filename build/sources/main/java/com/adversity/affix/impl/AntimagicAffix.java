package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔免词条 - 魔法/药水免疫
 *
 * 机制：
 * 1. 免疫所有魔法伤害
 * 2. 6格范围内玩家正面药水效果失效
 * 3. 光环效果每秒检测
 * 4. 设计理念：针对魔法/药水流派
 */
public class AntimagicAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "antimagic");

    /** 魔法压制光环范围 */
    private static final double AURA_RANGE = 6.0;

    /** 光环检测间隔 (tick) */
    private static final int AURA_INTERVAL = 20; // 1秒

    public AntimagicAffix() {
        super(
                ID,
                AffixType.DEFENSIVE,
                35, // 较低权重（强力词条）
                7.0f // 难度7以上出现
        );
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        // 检查是否为魔法伤害
        if (source.isMagicDamage()) {
            // 完全免疫魔法伤害
            return 0;
        }

        return damage;
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 每秒检测一次光环
        if (data.getTickCount() % AURA_INTERVAL != 0) {
            return;
        }

        int tier = getTier(entity);
        double range = AURA_RANGE + (tier * 0.5);

        // 查找范围内的玩家
        List<EntityPlayer> nearbyPlayers = findNearbyPlayers(entity, range);

        for (EntityPlayer player : nearbyPlayers) {
            // 检查饰品反制（结界护符 - 抑制光环效果）
            float auraReduction = com.adversity.item.bauble.BaubleHelper.getEffectStrength(player, "aura");
            if (auraReduction >= 1.0f) {
                continue; // 完全免疫魔法压制
            }

            suppressPositivePotions(player, tier, auraReduction);
        }
    }

    /**
     * 查找附近的玩家
     */
    private List<EntityPlayer> findNearbyPlayers(EntityLiving entity, double range) {
        AxisAlignedBB searchBox = entity.getEntityBoundingBox().grow(range);

        return entity.world.getEntitiesWithinAABB(
                EntityPlayer.class,
                searchBox,
                player -> player != null && player.isEntityAlive());
    }

    /**
     * 压制玩家的正面药水效果
     * 
     * @param player        目标玩家
     * @param tier          词条等级
     * @param auraReduction 来自饰品的减免比例 (0.0-1.0)
     */
    private void suppressPositivePotions(EntityPlayer player, int tier, float auraReduction) {
        // 收集需要移除的正面效果
        List<Potion> toRemove = new ArrayList<>();

        for (PotionEffect effect : player.getActivePotionEffects()) {
            Potion potion = effect.getPotion();
            if (potion != null && !potion.isBadEffect()) {
                // 根据反制概率决定是否移除
                if (auraReduction <= 0 || com.adversity.item.bauble.BaubleHelper.RANDOM.nextFloat() >= auraReduction) {
                    toRemove.add(potion);
                }
            }
        }

        // 移除正面效果（按tier决定移除数量）
        int maxRemove = Math.min(1 + tier / 3, toRemove.size());
        for (int i = 0; i < maxRemove; i++) {
            player.removePotionEffect(toRemove.get(i));
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
