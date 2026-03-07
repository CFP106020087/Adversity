package com.adversity.sanctuary;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.*;

/**
 * 階段門控注冊表
 *
 * Item gate 支持多層疊加（Set<String>），有序 stage 自動 merge 取最高。
 * 有序 stages: awakened(1) < scholar(2) < warden(3) < champion(4)
 * 自定義 stages（如 "bone_collector"）不參與 merge，永遠獨立保留。
 */
public class StageGatingRegistry {

    // 有序階段表
    private static final Map<String, Integer> ORDERED_STAGES = new LinkedHashMap<>();
    static {
        ORDERED_STAGES.put("awakened", 1);
        ORDERED_STAGES.put("scholar", 2);
        ORDERED_STAGES.put("warden", 3);
        ORDERED_STAGES.put("champion", 4);
    }

    // Set-based: 每個物品可有多個 stage requirement（AND 關係）
    private static final Map<ResourceLocation, Set<String>> ITEM_STAGE_REQUIREMENTS = new HashMap<>();
    private static final Map<ResourceLocation, String> ENCHANTMENT_STAGE_REQUIREMENTS = new HashMap<>();

    // CRT 覆蓋的聖所啟動物品（null = 使用 config 預設）
    private static List<String> crtActivationItems = null;

    public static void setActivationItems(List<String> items) {
        crtActivationItems = items;
    }

    public static List<String> getActivationItems() {
        return crtActivationItems;
    }

    public static boolean hasActivationItemOverride() {
        return crtActivationItems != null;
    }

    // ==================== 附魔門控黑白名單 ====================
    private static boolean enchantGatingEnabled = false;
    private static String enchantGatingStage = "scholar";
    private static boolean enchantGatingUseBlacklist = true;
    private static final Set<ResourceLocation> enchantGatingList = new HashSet<>();

    public static void setEnchantmentGating(boolean enabled, String stage, boolean useBlacklist,
            Set<ResourceLocation> list) {
        enchantGatingEnabled = enabled;
        enchantGatingStage = stage;
        enchantGatingUseBlacklist = useBlacklist;
        enchantGatingList.clear();
        enchantGatingList.addAll(list);
    }

    public static boolean isEnchantGatingEnabled() {
        return enchantGatingEnabled;
    }

    public static String getEnchantGatingStage() {
        return enchantGatingStage;
    }

    public static boolean isEnchantGatingBlacklist() {
        return enchantGatingUseBlacklist;
    }

    public static Set<ResourceLocation> getEnchantGatingList() {
        return enchantGatingList;
    }

    /**
     * 檢查附魔是否被黑白名單系統門控（不考慮玩家階段）
     * 
     * @return true = 此附魔需要 stage 驗證
     */
    public static boolean isEnchantmentGated(Enchantment ench) {
        if (!enchantGatingEnabled || ench == null || ench.getRegistryName() == null)
            return false;
        boolean inList = enchantGatingList.contains(ench.getRegistryName());
        return enchantGatingUseBlacklist ? inList : !inList;
    }

    private static final Map<Integer, String> DIMENSION_STAGE_REQUIREMENTS = new HashMap<>();
    private static final Map<ResourceLocation, String> ENTITY_STAGE_REQUIREMENTS = new HashMap<>();
    private static final Map<ResourceLocation, String> BIOME_STAGE_REQUIREMENTS = new HashMap<>();

    // ==================== Item Gating (Set-based, auto-merge) ====================

    /**
     * 追加一個 stage requirement。
     * 有序 stage 自動 merge：多個有序 stage 只保留最高。
     * 自定義 stage 永遠獨立保留。
     */
    public static void setItemStage(Item item, String stage) {
        if (item != null && item.getRegistryName() != null) {
            setItemStage(item.getRegistryName(), stage);
        }
    }

    public static void setItemStage(ResourceLocation itemId, String stage) {
        if (itemId == null || stage == null)
            return;
        stage = stage.toLowerCase();

        Set<String> stages = ITEM_STAGE_REQUIREMENTS.computeIfAbsent(itemId, k -> new LinkedHashSet<>());

        Integer newOrder = ORDERED_STAGES.get(stage);
        if (newOrder != null) {
            // 有序 stage → 移除所有同等或更低的有序 stage
            final int order = newOrder;
            stages.removeIf(s -> {
                Integer existingOrder = ORDERED_STAGES.get(s);
                return existingOrder != null && existingOrder <= order;
            });
            // 檢查是否已有更高的有序 stage
            boolean hasHigher = stages.stream()
                    .anyMatch(s -> ORDERED_STAGES.getOrDefault(s, 0) > order);
            if (!hasHigher) {
                stages.add(stage);
            }
        } else {
            // 自定義 stage → 直接追加
            stages.add(stage);
        }
    }

    /**
     * 查詢物品的所有 stage requirements（可能多個）。
     * 返回 null 表示無 gate。
     */
    public static Set<String> getItemStageRequirements(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        if (item.getRegistryName() == null) return null;
        Set<String> stages = ITEM_STAGE_REQUIREMENTS.get(item.getRegistryName());
        return (stages != null && !stages.isEmpty()) ? stages : null;
    }

    /**
     * 向後兼容：返回第一個 stage（或唯一的 stage）。
     */
    public static String getItemStageRequirement(ItemStack stack) {
        Set<String> stages = getItemStageRequirements(stack);
        return stages != null ? stages.iterator().next() : null;
    }

    public static String getItemStageRequirement(ResourceLocation itemId) {
        Set<String> stages = ITEM_STAGE_REQUIREMENTS.get(itemId);
        return (stages != null && !stages.isEmpty()) ? stages.iterator().next() : null;
    }

    /**
     * 返回當前已註冊 gate 的物品數量（用於性能優化短路）。
     */
    public static int getItemStageCount() {
        return ITEM_STAGE_REQUIREMENTS.size();
    }

    public static void clearItemStage(Item item) {
        if (item != null && item.getRegistryName() != null) {
            ITEM_STAGE_REQUIREMENTS.remove(item.getRegistryName());
        }
    }

    public static void clearItemStage(ResourceLocation itemId) {
        ITEM_STAGE_REQUIREMENTS.remove(itemId);
    }

    public static void removeItemStage(ResourceLocation itemId) {
        ITEM_STAGE_REQUIREMENTS.remove(itemId);
    }

    public static void clearItemStages() {
        ITEM_STAGE_REQUIREMENTS.clear();
    }

    // ==================== Enchantment Gating ====================

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

    // ==================== Dimension Gating ====================

    public static void setDimensionStage(int dimensionId, String stage) {
        DIMENSION_STAGE_REQUIREMENTS.put(dimensionId, stage.toLowerCase());
    }

    public static String getDimensionStageRequirement(int dimensionId) {
        return DIMENSION_STAGE_REQUIREMENTS.get(dimensionId);
    }

    public static void removeDimensionStage(int dimensionId) {
        DIMENSION_STAGE_REQUIREMENTS.remove(dimensionId);
    }

    // ==================== Entity Gating ====================

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

    // ==================== Biome Gating ====================

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

    // ==================== Utility ====================

    /**
     * 檢查玩家是否滿足物品的所有 stage requirements
     */
    public static boolean playerMeetsItemRequirements(
            com.adversity.capability.IAdversityCapability.IProgression progression,
            ItemStack stack) {
        Set<String> stages = getItemStageRequirements(stack);
        if (stages == null)
            return true;
        if (progression == null)
            return false;
        for (String stage : stages) {
            if (!progression.hasStage(stage))
                return false;
        }
        return true;
    }

    /**
     * 獲取物品的未滿足 stage requirements 列表（用於 GUI 提示）
     */
    public static List<String> getMissingStages(
            com.adversity.capability.IAdversityCapability.IProgression progression,
            ItemStack stack) {
        Set<String> stages = getItemStageRequirements(stack);
        if (stages == null)
            return Collections.emptyList();
        List<String> missing = new ArrayList<>();
        for (String stage : stages) {
            if (progression == null || !progression.hasStage(stage)) {
                missing.add(stage);
            }
        }
        return missing;
    }

    public static boolean hasAnyData() {
        return !ITEM_STAGE_REQUIREMENTS.isEmpty()
                || !ENCHANTMENT_STAGE_REQUIREMENTS.isEmpty()
                || !DIMENSION_STAGE_REQUIREMENTS.isEmpty()
                || !ENTITY_STAGE_REQUIREMENTS.isEmpty()
                || !BIOME_STAGE_REQUIREMENTS.isEmpty();
    }

    public static void clearAll() {
        ITEM_STAGE_REQUIREMENTS.clear();
        ENCHANTMENT_STAGE_REQUIREMENTS.clear();
        DIMENSION_STAGE_REQUIREMENTS.clear();
        ENTITY_STAGE_REQUIREMENTS.clear();
        BIOME_STAGE_REQUIREMENTS.clear();
    }
}
