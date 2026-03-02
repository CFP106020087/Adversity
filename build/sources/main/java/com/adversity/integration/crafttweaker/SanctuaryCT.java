package com.adversity.integration.crafttweaker;

import com.adversity.Adversity;
import com.adversity.sanctuary.ritual.Rite;
import com.adversity.sanctuary.ritual.RitualManager;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker integration for Adversity Sanctuary rituals
 * 
 * ZenScript usage:
 * import mods.adversity.Sanctuary;
 * 
 * // Add ritual
 * Sanctuary.addRitual("my_ritual", <input>, <output>, cost, "reqStage",
 * "rewardStage");
 * 
 * // Add command ritual
 * Sanctuary.addCommandRitual("my_cmd", <input>, cost, "reqStage", "/command");
 * 
 * // Remove ritual
 * Sanctuary.removeRitual("adversity:ritual_id");
 * 
 * // Remove all rituals
 * Sanctuary.removeAllRituals();
 */
@ZenRegister
@ZenClass("mods.adversity.Sanctuary")
public class SanctuaryCT {

    /**
     * 添加普通仪式
     */
    @ZenMethod
    public static void addRitual(String name, IItemStack input, IItemStack output, int entropyCost,
            String requiredStage, String rewardStage) {
        RitualManager.registerRite(
            name,
            CraftTweakerMC.getItemStack(input),
            CraftTweakerMC.getItemStack(output),
            entropyCost,
            requiredStage,
            rewardStage
        );
        CraftTweakerAPI.logInfo("[Adversity] Registered ritual: " + name);
    }

    /**
     * 添加带命令的仪式
     */
    @ZenMethod
    public static void addRitualWithCommand(String name, IItemStack input, IItemStack output, int entropyCost,
            String requiredStage, String rewardStage, String command) {
        RitualManager.registerRite(
                name,
                CraftTweakerMC.getItemStack(input),
                CraftTweakerMC.getItemStack(output),
                entropyCost,
                requiredStage,
                rewardStage,
                command);
        CraftTweakerAPI.logInfo("[Adversity] Registered ritual with command: " + name);
    }

    /**
     * 添加纯命令仪式（无物品输出）
     */
    @ZenMethod
    public static void addCommandRitual(String name, IItemStack input, int entropyCost,
            String requiredStage, String command) {
        RitualManager.registerCommandRite(
                name,
                CraftTweakerMC.getItemStack(input),
                entropyCost,
                requiredStage,
                command);
        CraftTweakerAPI.logInfo("[Adversity] Registered command ritual: " + name);
    }

    /**
     * 移除指定仪式
     */
    @ZenMethod
    public static void removeRitual(String ritualId) {
        if (RitualManager.removeRite(ritualId)) {
            CraftTweakerAPI.logInfo("[Adversity] Removed ritual: " + ritualId);
        } else {
            CraftTweakerAPI.logWarning("[Adversity] Ritual not found: " + ritualId);
        }
    }

    /**
     * 移除所有仪式
     */
    @ZenMethod
    public static void removeAllRituals() {
        int count = RitualManager.removeAllRites();
        CraftTweakerAPI.logInfo("[Adversity] Removed all " + count + " rituals");
    }

    /**
     * 获取已注册仪式数量
     */
    @ZenMethod
    public static int getRitualCount() {
        return RitualManager.getRiteCount();
    }

    // ==================== 阶段门控 API ====================

    /**
     * 设置物品的合成阶段要求
     * 在圣所范围内，只有达到指定阶段的玩家才能合成此物品
     * 
     * @param item  需要限制的物品
     * @param stage 要求的阶段 (awakened/scholar/warden/champion)
     */
    @ZenMethod
    public static void setItemStage(IItemStack item, String stage) {
        net.minecraft.item.ItemStack stack = CraftTweakerMC.getItemStack(item);
        if (!stack.isEmpty()) {
            com.adversity.sanctuary.StageGatingRegistry.setItemStage(stack.getItem(), stage);
            CraftTweakerAPI.logInfo("[Adversity] Set item stage requirement: " +
                    stack.getItem().getRegistryName() + " -> " + stage);
        }
    }

    /**
     * 设置附魔的获取阶段要求
     * 在圣所范围内附魔时，只有达到指定阶段的玩家才能获得此附魔
     * 
     * @param enchantmentId 附魔ID (如 "adversity:soulbound")
     * @param stage         要求的阶段
     */
    @ZenMethod
    public static void setEnchantmentStage(String enchantmentId, String stage) {
        net.minecraft.util.ResourceLocation loc = new net.minecraft.util.ResourceLocation(enchantmentId);
        com.adversity.sanctuary.StageGatingRegistry.setEnchantmentStage(loc, stage);
        CraftTweakerAPI.logInfo("[Adversity] Set enchantment stage requirement: " + enchantmentId + " -> " + stage);
    }

    /**
     * 批量设置多个物品的合成阶段要求
     */
    @ZenMethod
    public static void setItemStages(IItemStack[] items, String stage) {
        for (IItemStack item : items) {
            setItemStage(item, stage);
        }
    }

    /**
     * 移除物品的阶段要求
     */
    @ZenMethod
    public static void removeItemStage(IItemStack item) {
        net.minecraft.item.ItemStack stack = CraftTweakerMC.getItemStack(item);
        if (!stack.isEmpty() && stack.getItem().getRegistryName() != null) {
            com.adversity.sanctuary.StageGatingRegistry.removeItemStage(stack.getItem().getRegistryName());
            CraftTweakerAPI.logInfo("[Adversity] Removed item stage requirement: " +
                    stack.getItem().getRegistryName());
        }
    }

    /**
     * 移除附魔的阶段要求
     */
    @ZenMethod
    public static void removeEnchantmentStage(String enchantmentId) {
        net.minecraft.util.ResourceLocation loc = new net.minecraft.util.ResourceLocation(enchantmentId);
        com.adversity.sanctuary.StageGatingRegistry.removeEnchantmentStage(loc);
        CraftTweakerAPI.logInfo("[Adversity] Removed enchantment stage requirement: " + enchantmentId);
    }
}
