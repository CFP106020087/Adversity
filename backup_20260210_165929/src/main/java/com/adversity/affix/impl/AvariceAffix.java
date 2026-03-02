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
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

/**
 * 强欲词条 - 玩家背包物品越多，怪物伤害越高
 *
 * 机制：
 * 1. 检测目标玩家背包中占用的槽位数量
 * 2. 占用槽位越多，伤害加成越大
 * 3. 不计算堆叠数量，只计算槽位占用
 * 4. 设计理念：惩罚囤积癖玩家，鼓励轻装上阵
 */
public class AvariceAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "avarice");

    /** 每个占用槽位增加的伤害百分比 */
    private static final float DAMAGE_PER_SLOT = 0.03f;  // 3%每槽

    /** 最大伤害加成 */
    private static final float MAX_DAMAGE_BONUS = 1.5f;  // 最多+150%

    /** 开始计算的槽位阈值 */
    private static final int SLOT_THRESHOLD = 9;  // 超过9个槽位才开始计算

    public AvariceAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            75,     // 中等权重
            2.0f    // 难度2以上
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 计算占用的槽位数量
        int occupiedSlots = countOccupiedSlots(player);

        // 低于阈值不触发
        if (occupiedSlots <= SLOT_THRESHOLD) {
            return damage;
        }

        // 计算有效槽位（超过阈值的部分）
        int effectiveSlots = occupiedSlots - SLOT_THRESHOLD;

        // 计算伤害加成
        float tierMultiplier = 1.0f + (tier * 0.1f);
        float damageBonus = effectiveSlots * DAMAGE_PER_SLOT * tierMultiplier;

        // 限制最大加成
        damageBonus = Math.min(damageBonus, MAX_DAMAGE_BONUS);

        // 检查轻装护身符反制
        float lightReduction = com.adversity.item.bauble.BaubleHelper.getReduction(player, "avarice");
        if (lightReduction >= 1.0f) {
            // 完全免疫惩罚
            return damage;
        } else if (lightReduction > 0) {
            // 降低伤害加成
            damageBonus = damageBonus * (1.0f - lightReduction);
        }

        return damage * (1.0f + damageBonus);
    }

    /**
     * 计算玩家背包占用槽位数
     */
    private int countOccupiedSlots(EntityPlayer player) {
        int count = 0;

        // 主背包（36槽：9快捷栏 + 27背包）
        NonNullList<ItemStack> mainInventory = player.inventory.mainInventory;
        for (ItemStack stack : mainInventory) {
            if (!stack.isEmpty()) {
                count++;
            }
        }

        // 装备栏（4槽）
        NonNullList<ItemStack> armorInventory = player.inventory.armorInventory;
        for (ItemStack stack : armorInventory) {
            if (!stack.isEmpty()) {
                count++;
            }
        }

        // 副手（1槽）
        NonNullList<ItemStack> offhandInventory = player.inventory.offHandInventory;
        for (ItemStack stack : offhandInventory) {
            if (!stack.isEmpty()) {
                count++;
            }
        }

        return count;
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
