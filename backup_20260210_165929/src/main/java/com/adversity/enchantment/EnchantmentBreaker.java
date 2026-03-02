package com.adversity.enchantment;

import com.adversity.Adversity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * 破咒者附魔 - 移除精英词条buff
 * 
 * 等级 I: 10%几率移除一个随机词条
 * 等级 II: 20%几率移除一个随机词条
 * 等级 III: 30%几率移除一个随机词条
 */
public class EnchantmentBreaker extends Enchantment {

    public EnchantmentBreaker() {
        super(Rarity.VERY_RARE, EnumEnchantmentType.WEAPON, 
            new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND});
        setRegistryName(Adversity.MODID, "breaker");
        setName(Adversity.MODID + ".breaker");
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
        return 20 + (level - 1) * 15;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 25;
    }

    /**
     * 获取移除词条的几率
     */
    public static float getRemoveChance(int level) {
        return 0.10f * level; // 10%/20%/30%
    }

    /**
     * 获取物品的破咒者等级
     */
    public static int getEnchantmentLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
            EnchantmentRegistry.BREAKER, stack);
    }
}
