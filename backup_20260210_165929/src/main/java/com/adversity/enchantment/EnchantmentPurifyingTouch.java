package com.adversity.enchantment;

import com.adversity.Adversity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * 净化之触附魔 - 缩短封印(Shackle/Divest/Disenchant)效果持续时间
 * 
 * 等级 I: 封印持续时间-30%
 * 等级 II: 封印持续时间-50%
 * 等级 III: 封印持续时间-70%
 */
public class EnchantmentPurifyingTouch extends Enchantment {

    public EnchantmentPurifyingTouch() {
        super(Rarity.RARE, EnumEnchantmentType.ARMOR,
                new EntityEquipmentSlot[] {
                        EntityEquipmentSlot.HEAD,
                        EntityEquipmentSlot.CHEST,
                        EntityEquipmentSlot.LEGS,
                        EntityEquipmentSlot.FEET
                });
        setRegistryName(Adversity.MODID, "purifying_touch");
        setName(Adversity.MODID + ".purifying_touch");
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinEnchantability(int level) {
        return 15 + (level - 1) * 10;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 20;
    }

    /**
     * 获取封印持续时间减免倍率 (0.7表示-30%)
     */
    public static float getSealDurationMultiplier(int level) {
        switch (level) {
            case 1:
                return 0.70f; // -30%
            case 2:
                return 0.50f; // -50%
            case 3:
                return 0.30f; // -70%
            default:
                return 1.0f;
        }
    }

    /**
     * 计算玩家总封印时间减免
     */
    public static float getTotalSealReduction(Iterable<ItemStack> armorSlots) {
        int maxLevel = 0;
        for (ItemStack stack : armorSlots) {
            if (!stack.isEmpty()) {
                int level = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                        EnchantmentRegistry.PURIFYING_TOUCH, stack);
                if (level > maxLevel) {
                    maxLevel = level;
                }
            }
        }
        return getSealDurationMultiplier(maxLevel);
    }

    /**
     * 获取物品的净化之触等级
     */
    public static int getEnchantmentLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
            EnchantmentRegistry.PURIFYING_TOUCH, stack);
    }
}
