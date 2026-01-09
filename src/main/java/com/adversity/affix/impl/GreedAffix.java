package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

/**
 * 贪婪词条 - 饰品越多，受到的伤害越高
 *
 * 机制：
 * 1. 检测目标玩家装备的Baubles饰品数量
 * 2. 饰品越多，玩家受到的伤害越高
 * 3. 设计理念：高风险高回报，强力饰品带来代价
 */
public class GreedAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "greed");

    /** 每个饰品增加的伤害百分比 */
    private static final float DAMAGE_PER_BAUBLE = 0.15f;  // 15%每个饰品

    /** 最大伤害加成 */
    private static final float MAX_DAMAGE_BONUS = 1.0f;  // 最多+100%

    /** Baubles是否可用 */
    private static Boolean baublesLoaded = null;

    public GreedAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            60,     // 中等权重
            3.0f    // 难度3以上
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        // 检查Baubles是否可用
        if (!isBaublesLoaded()) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 计算装备的饰品数量 (通过兼容层，延迟加载)
        int baubleCount = countBaubles(player);

        if (baubleCount <= 0) {
            return damage;
        }

        // 计算伤害加成
        float tierMultiplier = 1.0f + (tier * 0.05f);
        float damageBonus = baubleCount * DAMAGE_PER_BAUBLE * tierMultiplier;

        // 限制最大加成
        damageBonus = Math.min(damageBonus, MAX_DAMAGE_BONUS);

        float finalDamage = damage * (1.0f + damageBonus);

        Adversity.LOGGER.debug("Greed affix: {} baubles, damage {} -> {} (+{}%)",
            baubleCount, damage, finalDamage, (int)(damageBonus * 100));

        return finalDamage;
    }

    /**
     * 检查Baubles是否已加载
     */
    private static boolean isBaublesLoaded() {
        if (baublesLoaded == null) {
            baublesLoaded = Loader.isModLoaded("baubles");
        }
        return baublesLoaded;
    }

    /**
     * 通过兼容层计算饰品数量（延迟加载BaublesCompat类）
     */
    private static int countBaubles(EntityPlayer player) {
        // 只有在确认Baubles加载后才加载BaublesCompat类
        // 这样避免在没有Baubles时类加载失败
        return com.adversity.compat.BaublesCompat.countEquippedBaubles(player);
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
