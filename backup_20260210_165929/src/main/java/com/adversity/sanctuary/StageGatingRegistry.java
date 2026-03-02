package com.adversity.sanctuary;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 阶段门控注册表
 * 存储物品和附魔的阶段要求
 * 
 * 用于限制玩家在圣所范围内的合成和附魔能力
 */
public class StageGatingRegistry {

    // 物品ID -> 阶段要求
    private static final Map<ResourceLocation, String> ITEM_STAGE_REQUIREMENTS = new HashMap<>();
    
    // 附魔ID -> 阶段要求
    private static final Map<ResourceLocation, String> ENCHANTMENT_STAGE_REQUIREMENTS = new HashMap<>();

    /**
     * 设置物品的阶段要求
     * @param item 物品
     * @param stage 需要的阶段 (awakened/scholar/warden/champion)
     */
    public static void setItemStage(Item item, String stage) {
        if (item != null && item.getRegistryName() != null) {
            ITEM_STAGE_REQUIREMENTS.put(item.getRegistryName(), stage.toLowerCase());
        }
    }

    /**
     * 设置物品的阶段要求 (通过ResourceLocation)
     */
    public static void setItemStage(ResourceLocation itemId, String stage) {
        if (itemId != null && stage != null) {
            ITEM_STAGE_REQUIREMENTS.put(itemId, stage.toLowerCase());
        }
    }

    /**
     * 设置附魔的阶段要求
     * @param enchantment 附魔
     * @param stage 需要的阶段
     */
    public static void setEnchantmentStage(Enchantment enchantment, String stage) {
        if (enchantment != null && enchantment.getRegistryName() != null) {
            ENCHANTMENT_STAGE_REQUIREMENTS.put(enchantment.getRegistryName(), stage.toLowerCase());
        }
    }

    /**
     * 设置附魔的阶段要求 (通过ResourceLocation)
     */
    public static void setEnchantmentStage(ResourceLocation enchantId, String stage) {
        if (enchantId != null && stage != null) {
            ENCHANTMENT_STAGE_REQUIREMENTS.put(enchantId, stage.toLowerCase());
        }
    }

    /**
     * 获取物品的阶段要求
     * @return 阶段名称，如果没有要求则返回 null
     */
    public static String getItemStageRequirement(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        if (item.getRegistryName() == null) return null;
        return ITEM_STAGE_REQUIREMENTS.get(item.getRegistryName());
    }

    /**
     * 获取物品的阶段要求 (通过ResourceLocation)
     */
    public static String getItemStageRequirement(ResourceLocation itemId) {
        return ITEM_STAGE_REQUIREMENTS.get(itemId);
    }

    /**
     * 获取附魔的阶段要求
     */
    public static String getEnchantmentStageRequirement(Enchantment enchantment) {
        if (enchantment == null || enchantment.getRegistryName() == null) return null;
        return ENCHANTMENT_STAGE_REQUIREMENTS.get(enchantment.getRegistryName());
    }

    /**
     * 获取附魔的阶段要求 (通过ResourceLocation)
     */
    public static String getEnchantmentStageRequirement(ResourceLocation enchantId) {
        return ENCHANTMENT_STAGE_REQUIREMENTS.get(enchantId);
    }

    /**
     * 移除物品的阶段要求
     */
    public static void removeItemStage(ResourceLocation itemId) {
        ITEM_STAGE_REQUIREMENTS.remove(itemId);
    }

    /**
     * 移除附魔的阶段要求
     */
    public static void removeEnchantmentStage(ResourceLocation enchantId) {
        ENCHANTMENT_STAGE_REQUIREMENTS.remove(enchantId);
    }

    /**
     * 清除所有物品阶段要求
     */
    public static void clearItemStages() {
        ITEM_STAGE_REQUIREMENTS.clear();
    }

    /**
     * 清除所有附魔阶段要求
     */
    public static void clearEnchantmentStages() {
        ENCHANTMENT_STAGE_REQUIREMENTS.clear();
    }

    /**
     * 获取已注册的物品阶段要求数量
     */
    public static int getItemStageCount() {
        return ITEM_STAGE_REQUIREMENTS.size();
    }

    /**
     * 获取已注册的附魔阶段要求数量
     */
    public static int getEnchantmentStageCount() {
        return ENCHANTMENT_STAGE_REQUIREMENTS.size();
    }
}
