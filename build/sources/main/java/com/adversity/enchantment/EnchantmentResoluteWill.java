package com.adversity.enchantment;

import com.adversity.Adversity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * 坚定意志附魔 - 免疫Purifier（净化者）的药水窃取
 * 
 * 等级 I: 完全免疫药水效果被移除
 */
public class EnchantmentResoluteWill extends Enchantment {

    private static final EntityEquipmentSlot[] ARMOR_SLOTS = {
        EntityEquipmentSlot.HEAD,
        EntityEquipmentSlot.CHEST,
        EntityEquipmentSlot.LEGS,
        EntityEquipmentSlot.FEET
    };

    public EnchantmentResoluteWill() {
        super(Rarity.RARE, EnumEnchantmentType.ARMOR, ARMOR_SLOTS);
        setRegistryName(Adversity.MODID, "resolute_will");
        setName(Adversity.MODID + ".resolute_will");
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
        return 25;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return 40;
    }

    /**
     * 检查玩家是否有坚定意志保护
     */
    public static boolean hasProtection(Iterable<ItemStack> armorSlots) {
        for (ItemStack stack : armorSlots) {
            if (!stack.isEmpty()) {
                int level = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                    EnchantmentRegistry.RESOLUTE_WILL, stack);
                if (level > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
