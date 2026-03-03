package com.adversity.integration.crafttweaker;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.progression.ProgressionEventHandler;
import com.adversity.progression.StageTriggerRegistry;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.player.IPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker 进度系统集成
 *
 * ZenScript 用法:
 * import mods.adversity.Progression;
 *
 * Progression.hasStage(player, "awakened");
 * Progression.addStage(player, "custom_stage");
 * Progression.removeStage(player, "old_stage");
 * Progression.getTier(player);
 * Progression.getStages(player);
 * Progression.clearStages(player);
 *
 * // Auto triggers
 * Progression.addKillTrigger("dragon_slayer", "minecraft:ender_dragon", 1,
 * "champion");
 * Progression.addDifficultyTrigger("hardened", 30.0, "warden");
 * Progression.clearTriggers();
 */
@ZenRegister
@ZenClass("mods.adversity.Progression")
public class ProgressionCT {

    @ZenMethod
    public static boolean hasStage(IPlayer player, String stage) {
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        if (mcPlayer == null)
            return false;
        IAdversityCapability.IProgression cap = mcPlayer.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
        return cap != null && cap.hasStage(stage);
    }

    @ZenMethod
    public static boolean addStage(IPlayer player, String stage) {
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        if (mcPlayer == null)
            return false;
        return ProgressionEventHandler.addStage(mcPlayer, stage);
    }

    @ZenMethod
    public static boolean removeStage(IPlayer player, String stage) {
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        if (mcPlayer == null)
            return false;
        return ProgressionEventHandler.removeStage(mcPlayer, stage);
    }

    @ZenMethod
    public static int getTier(IPlayer player) {
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        if (mcPlayer == null)
            return 0;
        return ProgressionEventHandler.getHighestTier(mcPlayer);
    }

    @ZenMethod
    public static String getStages(IPlayer player) {
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        if (mcPlayer == null)
            return "";
        IAdversityCapability.IProgression cap = mcPlayer.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
        if (cap != null) {
            return String.join(",", cap.getStages());
        }
        return "";
    }

    @ZenMethod
    public static void clearStages(IPlayer player) {
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        if (mcPlayer == null)
            return;
        IAdversityCapability.IProgression cap = mcPlayer.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
        if (cap != null) {
            cap.clear();
            if (mcPlayer instanceof EntityPlayerMP) {
                ProgressionEventHandler.syncToClient((EntityPlayerMP) mcPlayer);
            }
        }
    }

    // ==================== 自动触发器 ====================

    /**
     * 注册击杀触发器
     */
    @ZenMethod
    public static void addKillTrigger(String id, String entityId, int killCount, String stage) {
        StageTriggerRegistry.registerKillTrigger(id, entityId, killCount, stage);
        CraftTweakerAPI.logInfo("[Adversity] Registered kill trigger: " + id);
    }

    /**
     * 注册难度触发器
     */
    @ZenMethod
    public static void addDifficultyTrigger(String id, double minDifficulty, String stage) {
        StageTriggerRegistry.registerDifficultyTrigger(id, minDifficulty, stage);
        CraftTweakerAPI.logInfo("[Adversity] Registered difficulty trigger: " + id);
    }

    /**
     * 清除所有触发器
     */
    @ZenMethod
    public static void clearTriggers() {
        StageTriggerRegistry.clearAll();
        CraftTweakerAPI.logInfo("[Adversity] Cleared all stage triggers");
    }
}
