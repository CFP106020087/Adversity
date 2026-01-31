package com.adversity.enchantment;

import com.adversity.Adversity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * 净化之触附魔 - 攻击有几率临时移除怪物词条
 * 
 * 等级 I: 5%几率移除1个词条30秒
 */
public class EnchantmentPurifyingTouch extends Enchantment {

    public static final float PROC_CHANCE = 0.05f;
    public static final int SUPPRESS_DURATION = 600; // 30秒 in ticks

    public EnchantmentPurifyingTouch() {
        super(Rarity.VERY_RARE, EnumEnchantmentType.WEAPON, 
            new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND});
        setRegistryName(Adversity.MODID, "purifying_touch");
        setName(Adversity.MODID + ".purifying_touch");
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getMinEnchantability(int level) {
        return 30;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return 50;
    }

    /**
     * 检查是否触发净化效果
     */
    public static boolean shouldTrigger() {
        return Math.random() < PROC_CHANCE;
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
