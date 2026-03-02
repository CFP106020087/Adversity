package com.adversity.enchantment;

import com.adversity.Adversity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * 熵能亲和附魔 - 增加熵能掉落
 * 
 * 等级 I: +25%熵能掉落
 * 等级 II: +50%熵能掉落
 */
public class EnchantmentEntropyAffinity extends Enchantment {

    private static final EntityEquipmentSlot[] ARMOR_SLOTS = {
        EntityEquipmentSlot.HEAD,
        EntityEquipmentSlot.CHEST,
        EntityEquipmentSlot.LEGS,
        EntityEquipmentSlot.FEET
    };

    public EnchantmentEntropyAffinity() {
        super(Rarity.UNCOMMON, EnumEnchantmentType.ARMOR, ARMOR_SLOTS);
        setRegistryName(Adversity.MODID, "entropy_affinity");
        setName(Adversity.MODID + ".entropy_affinity");
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }

    @Override
    public int getMinEnchantability(int level) {
        return 10 + (level - 1) * 8;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 15;
    }

    /**
     * 获取熵能掉落加成 (如1.25表示+25%)
     */
    public static float getEntropyBonus(int level) {
        switch (level) {
            case 1: return 1.25f;
            case 2: return 1.50f;
            default: return 1.0f;
        }
    }

    /**
     * 计算玩家总熵能加成
     */
    public static float getTotalEntropyBonus(Iterable<ItemStack> armorSlots) {
        float totalBonus = 1.0f;
        for (ItemStack stack : armorSlots) {
            if (!stack.isEmpty()) {
                int level = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                    EnchantmentRegistry.ENTROPY_AFFINITY, stack);
                if (level > 0) {
                    totalBonus *= getEntropyBonus(level);
                }
            }
        }
        return totalBonus;
    }
}
