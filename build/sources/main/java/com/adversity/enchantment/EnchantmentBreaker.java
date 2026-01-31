package com.adversity.enchantment;

import com.adversity.Adversity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * 破咒者附魔 - 对防御词条怪物造成额外伤害
 * 
 * 等级 I: +50%伤害 vs Hero/Divine/OuterGod
 * 等级 II: +100%伤害 vs Hero/Divine/OuterGod
 * 等级 III: 真实伤害 vs Hero/Divine/OuterGod（无视其伤害减免）
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
     * 获取伤害倍率
     */
    public static float getDamageMultiplier(int level) {
        switch (level) {
            case 1: return 1.5f;  // +50%
            case 2: return 2.0f;  // +100%
            case 3: return 2.0f;  // 100%但作为真实伤害
            default: return 1.0f;
        }
    }

    /**
     * 检查是否使用真实伤害（等级3）
     */
    public static boolean usesTrueDamage(int level) {
        return level >= 3;
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
