package com.adversity.sanctuary;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class StageGatingRegistry {

    private static final Map<ResourceLocation, String> ITEM_STAGE_REQUIREMENTS = new HashMap<>();
    private static final Map<ResourceLocation, String> ENCHANTMENT_STAGE_REQUIREMENTS = new HashMap<>();
    private static final Map<Integer, String> DIMENSION_STAGE_REQUIREMENTS = new HashMap<>();
    private static final Map<ResourceLocation, String> ENTITY_STAGE_REQUIREMENTS = new HashMap<>();
    private static final Map<ResourceLocation, String> BIOME_STAGE_REQUIREMENTS = new HashMap<>();

    public static void setItemStage(Item item, String stage) {
        if (item != null && item.getRegistryName() != null) {
            ITEM_STAGE_REQUIREMENTS.put(item.getRegistryName(), stage.toLowerCase());
        }
    }

    public static void setItemStage(ResourceLocation itemId, String stage) {
        if (itemId != null && stage != null) {
            ITEM_STAGE_REQUIREMENTS.put(itemId, stage.toLowerCase());
        }
    }

    public static String getItemStageRequirement(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        if (item.getRegistryName() == null) return null;
        return ITEM_STAGE_REQUIREMENTS.get(item.getRegistryName());
    }

    public static String getItemStageRequirement(ResourceLocation itemId) {
        return ITEM_STAGE_REQUIREMENTS.get(itemId);
    }

    public static void removeItemStage(ResourceLocation itemId) {
        ITEM_STAGE_REQUIREMENTS.remove(itemId);
    }

    public static void clearItemStages() {
        ITEM_STAGE_REQUIREMENTS.clear();
    }

    public static int getItemStageCount() {
        return ITEM_STAGE_REQUIREMENTS.size();
    }

    public static void setEnchantmentStage(Enchantment enchantment, String stage) {
        if (enchantment != null && enchantment.getRegistryName() != null) {
            ENCHANTMENT_STAGE_REQUIREMENTS.put(enchantment.getRegistryName(), stage.toLowerCase());
        }
    }

    public static void setEnchantmentStage(ResourceLocation enchantId, String stage) {
        if (enchantId != null && stage != null) {
            ENCHANTMENT_STAGE_REQUIREMENTS.put(enchantId, stage.toLowerCase());
        }
    }

    public static String getEnchantmentStageRequirement(Enchantment enchantment) {
        if (enchantment == null || enchantment.getRegistryName() == null) return null;
        return ENCHANTMENT_STAGE_REQUIREMENTS.get(enchantment.getRegistryName());
    }

    public static String getEnchantmentStageRequirement(ResourceLocation enchantId) {
        return ENCHANTMENT_STAGE_REQUIREMENTS.get(enchantId);
    }

    public static void removeEnchantmentStage(ResourceLocation enchantId) {
        ENCHANTMENT_STAGE_REQUIREMENTS.remove(enchantId);
    }

    public static void clearEnchantmentStages() {
        ENCHANTMENT_STAGE_REQUIREMENTS.clear();
    }

    public static int getEnchantmentStageCount() {
        return ENCHANTMENT_STAGE_REQUIREMENTS.size();
    }

    public static void setDimensionStage(int dimensionId, String stage) {
        DIMENSION_STAGE_REQUIREMENTS.put(dimensionId, stage.toLowerCase());
    }

    public static String getDimensionStageRequirement(int dimensionId) {
        return DIMENSION_STAGE_REQUIREMENTS.get(dimensionId);
    }

    public static void removeDimensionStage(int dimensionId) {
        DIMENSION_STAGE_REQUIREMENTS.remove(dimensionId);
    }

    public static void setEntityStage(ResourceLocation entityId, String stage) {
        if (entityId != null && stage != null) {
            ENTITY_STAGE_REQUIREMENTS.put(entityId, stage.toLowerCase());
        }
    }

    public static void setEntityStage(String entityId, String stage) {
        setEntityStage(new ResourceLocation(entityId), stage);
    }

    public static String getEntityStageRequirement(ResourceLocation entityId) {
        return ENTITY_STAGE_REQUIREMENTS.get(entityId);
    }

    public static void removeEntityStage(ResourceLocation entityId) {
        ENTITY_STAGE_REQUIREMENTS.remove(entityId);
    }

    public static void setBiomeStage(ResourceLocation biomeId, String stage) {
        if (biomeId != null && stage != null) {
            BIOME_STAGE_REQUIREMENTS.put(biomeId, stage.toLowerCase());
        }
    }

    public static void setBiomeStage(String biomeId, String stage) {
        setBiomeStage(new ResourceLocation(biomeId), stage);
    }

    public static String getBiomeStageRequirement(ResourceLocation biomeId) {
        return BIOME_STAGE_REQUIREMENTS.get(biomeId);
    }

    public static void removeBiomeStage(ResourceLocation biomeId) {
        BIOME_STAGE_REQUIREMENTS.remove(biomeId);
    }

    public static void clearAll() {
        ITEM_STAGE_REQUIREMENTS.clear();
        ENCHANTMENT_STAGE_REQUIREMENTS.clear();
        DIMENSION_STAGE_REQUIREMENTS.clear();
        ENTITY_STAGE_REQUIREMENTS.clear();
        BIOME_STAGE_REQUIREMENTS.clear();
    }
}
