package com.adversity.enchantment;

import com.adversity.Adversity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * 灵魂锁定附魔 - 防止装备被封印
 * 
 * 等级 I: 33%阻止封印
 * 等级 II: 66%阻止封印
 * 等级 III: 100%阻止封印（完全免疫）
 */
public class EnchantmentSoulbound extends Enchantment {

    private static final EntityEquipmentSlot[] ALL_SLOTS = {
        EntityEquipmentSlot.HEAD,
        EntityEquipmentSlot.CHEST,
        EntityEquipmentSlot.LEGS,
        EntityEquipmentSlot.FEET,
        EntityEquipmentSlot.MAINHAND,
        EntityEquipmentSlot.OFFHAND
    };

    public EnchantmentSoulbound() {
        super(Rarity.RARE, EnumEnchantmentType.ALL, ALL_SLOTS);
        setRegistryName(Adversity.MODID, "soulbound");
        setName(Adversity.MODID + ".soulbound");
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
     * 获取封印阻止率 (0.0-1.0)
     */
    public static float getSealBlockChance(int level) {
        switch (level) {
            case 1: return 0.33f;
            case 2: return 0.66f;
            case 3: return 1.0f;
            default: return 0.0f;
        }
    }

    /**
     * 检查物品是否应该阻止封印
     */
    public static boolean shouldBlockSeal(ItemStack stack) {
        int level = getEnchantmentLevel(stack);
        if (level <= 0) return false;
        
        float chance = getSealBlockChance(level);
        if (chance >= 1.0f) return true;
        
        return Math.random() < chance;
    }

    /**
     * 获取物品的灵魂锁定等级
     */
    public static int getEnchantmentLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
            EnchantmentRegistry.SOULBOUND, stack);
    }
}
