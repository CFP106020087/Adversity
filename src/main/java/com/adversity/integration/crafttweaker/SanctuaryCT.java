package com.adversity.integration.crafttweaker;

import com.adversity.Adversity;
import com.adversity.sanctuary.StageGatingRegistry;
import com.adversity.sanctuary.ritual.Rite;
import com.adversity.sanctuary.ritual.RitualCategory;
import com.adversity.sanctuary.ritual.RitualManager;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CraftTweaker integration for Adversity Sanctuary rituals and stage gating
 *
 * ZenScript usage:
 * import mods.adversity.Sanctuary;
 *
 * // Basic ritual
 * Sanctuary.addRitual("my_ritual", <input>, <output>, cost, "reqStage",
 * "rewardStage");
 *
 * // Command ritual with cooldown
 * Sanctuary.addCommandRitual("my_cmd", <input>, cost, "reqStage", "/command");
 * Sanctuary.addCooldownCommandRitual("my_cmd", <input>, cost, "reqStage",
 * "/cmd", 6000);
 *
 * // Multi-input ritual
 * Sanctuary.addMultiInputRitual("fusion", [<input1>, <input2>], <output>, cost,
 * "reqStage", "rewardStage", "crafting");
 *
 * // Stage gating
 * Sanctuary.setItemStage(<item>, "stage");
 * Sanctuary.setDimensionStage(1, "scholar");
 * Sanctuary.setEntityStage("minecraft:wither", "warden");
 * Sanctuary.setBiomeStage("minecraft:hell", "scholar");
 */
@ZenRegister
@ZenClass("mods.adversity.Sanctuary")
public class SanctuaryCT {

    // ==================== 仪式管理 ====================

    @ZenMethod
    public static void addRitual(String name, IItemStack input, IItemStack output, int entropyCost,
            String requiredStage, String rewardStage) {
        RitualManager.registerRite(name,
            CraftTweakerMC.getItemStack(input),
            CraftTweakerMC.getItemStack(output),
                entropyCost, requiredStage, rewardStage);
        CraftTweakerAPI.logInfo("[Adversity] Registered ritual: " + name);
    }

    @ZenMethod
    public static void addRitualWithCommand(String name, IItemStack input, IItemStack output, int entropyCost,
            String requiredStage, String rewardStage, String command) {
        RitualManager.registerRite(name,
                CraftTweakerMC.getItemStack(input),
                CraftTweakerMC.getItemStack(output),
                entropyCost, requiredStage, rewardStage, command);
        CraftTweakerAPI.logInfo("[Adversity] Registered ritual with command: " + name);
    }

    @ZenMethod
    public static void addCommandRitual(String name, IItemStack input, int entropyCost,
            String requiredStage, String command) {
        RitualManager.registerCommandRite(name,
                CraftTweakerMC.getItemStack(input),
                entropyCost, requiredStage, command);
        CraftTweakerAPI.logInfo("[Adversity] Registered command ritual: " + name);
    }

    /**
     * 添加带冷却的命令仪式
     */
    @ZenMethod
    public static void addCooldownCommandRitual(String name, IItemStack input, int entropyCost,
            String requiredStage, String command, int cooldownTicks) {
        RitualManager.registerCommandRite(name,
                CraftTweakerMC.getItemStack(input),
                entropyCost, requiredStage, command, cooldownTicks);
        CraftTweakerAPI
                .logInfo("[Adversity] Registered cooldown command ritual: " + name + " (cd=" + cooldownTicks + "t)");
    }

    /**
     * 添加多输入仪式
     */
    @ZenMethod
    public static void addMultiInputRitual(String name, IItemStack[] inputs, IItemStack output,
            int entropyCost, String requiredStage, String rewardStage, String category) {
        List<ItemStack> inputList = new ArrayList<>();
        for (IItemStack in : inputs) {
            inputList.add(CraftTweakerMC.getItemStack(in));
        }
        RitualManager.registerMultiInputRite(name, inputList,
                CraftTweakerMC.getItemStack(output),
                entropyCost, requiredStage, rewardStage, category);
        CraftTweakerAPI
                .logInfo("[Adversity] Registered multi-input ritual: " + name + " (" + inputs.length + " inputs)");
    }

    @ZenMethod
    public static void removeRitual(String ritualId) {
        if (RitualManager.removeRite(ritualId)) {
            CraftTweakerAPI.logInfo("[Adversity] Removed ritual: " + ritualId);
        } else {
            CraftTweakerAPI.logWarning("[Adversity] Ritual not found: " + ritualId);
        }
    }

    @ZenMethod
    public static void removeAllRituals() {
        int count = RitualManager.removeAllRites();
        CraftTweakerAPI.logInfo("[Adversity] Removed all " + count + " rituals");
    }

    @ZenMethod
    public static int getRitualCount() {
        return RitualManager.getRiteCount();
    }

    // ==================== 物品门控 ====================

    @ZenMethod
    public static void setItemStage(IItemStack item, String stage) {
        ItemStack stack = CraftTweakerMC.getItemStack(item);
        if (!stack.isEmpty()) {
            StageGatingRegistry.setItemStage(stack.getItem(), stage);
            CraftTweakerAPI
                    .logInfo("[Adversity] Set item stage: " + stack.getItem().getRegistryName() + " -> " + stage);
        }
    }

    @ZenMethod
    public static void setItemStages(IItemStack[] items, String stage) {
        for (IItemStack item : items) {
            setItemStage(item, stage);
        }
    }

    @ZenMethod
    public static void setEnchantmentStage(String enchantmentId, String stage) {
        ResourceLocation loc = new ResourceLocation(enchantmentId);
        StageGatingRegistry.setEnchantmentStage(loc, stage);
        CraftTweakerAPI.logInfo("[Adversity] Set enchantment stage: " + enchantmentId + " -> " + stage);
    }

    @ZenMethod
    public static void removeItemStage(IItemStack item) {
        ItemStack stack = CraftTweakerMC.getItemStack(item);
        if (!stack.isEmpty() && stack.getItem().getRegistryName() != null) {
            StageGatingRegistry.removeItemStage(stack.getItem().getRegistryName());
            CraftTweakerAPI.logInfo("[Adversity] Removed item stage: " + stack.getItem().getRegistryName());
        }
    }

    @ZenMethod
    public static void removeEnchantmentStage(String enchantmentId) {
        ResourceLocation loc = new ResourceLocation(enchantmentId);
        StageGatingRegistry.removeEnchantmentStage(loc);
        CraftTweakerAPI.logInfo("[Adversity] Removed enchantment stage: " + enchantmentId);
    }

    // ==================== 维度门控 ====================

    @ZenMethod
    public static void setDimensionStage(int dimensionId, String stage) {
        StageGatingRegistry.setDimensionStage(dimensionId, stage);
        CraftTweakerAPI.logInfo("[Adversity] Set dimension stage: dim" + dimensionId + " -> " + stage);
    }

    @ZenMethod
    public static void removeDimensionStage(int dimensionId) {
        StageGatingRegistry.removeDimensionStage(dimensionId);
        CraftTweakerAPI.logInfo("[Adversity] Removed dimension stage: dim" + dimensionId);
    }

    // ==================== 实体门控 ====================

    @ZenMethod
    public static void setEntityStage(String entityId, String stage) {
        StageGatingRegistry.setEntityStage(entityId, stage);
        CraftTweakerAPI.logInfo("[Adversity] Set entity stage: " + entityId + " -> " + stage);
    }

    @ZenMethod
    public static void removeEntityStage(String entityId) {
        StageGatingRegistry.removeEntityStage(new ResourceLocation(entityId));
        CraftTweakerAPI.logInfo("[Adversity] Removed entity stage: " + entityId);
    }

    // ==================== 群系门控 ====================

    @ZenMethod
    public static void setBiomeStage(String biomeId, String stage) {
        StageGatingRegistry.setBiomeStage(biomeId, stage);
        CraftTweakerAPI.logInfo("[Adversity] Set biome stage: " + biomeId + " -> " + stage);
    }

    @ZenMethod
    public static void removeBiomeStage(String biomeId) {
        StageGatingRegistry.removeBiomeStage(new ResourceLocation(biomeId));
        CraftTweakerAPI.logInfo("[Adversity] Removed biome stage: " + biomeId);
    }
}
