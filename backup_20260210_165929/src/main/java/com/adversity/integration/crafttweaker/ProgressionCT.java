package com.adversity.integration.crafttweaker;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.progression.ProgressionEventHandler;
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
 * Progression.hasStage(player, "awakened"); // 检查阶段
 * Progression.addStage(player, "custom_stage"); // 添加阶段
 * Progression.removeStage(player, "old_stage"); // 移除阶段
 * Progression.getTier(player); // 获取等级 (0-4)
 * Progression.getStages(player); // 获取所有阶段
 */
@ZenRegister
@ZenClass("mods.adversity.Progression")
public class ProgressionCT {

    /**
     * 检查玩家是否有指定阶段
     */
    @ZenMethod
    public static boolean hasStage(IPlayer player, String stage) {
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        if (mcPlayer == null)
            return false;

        IAdversityCapability.IProgression cap = mcPlayer.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
        return cap != null && cap.hasStage(stage);
    }

    /**
     * 为玩家添加阶段
     */
    @ZenMethod
    public static boolean addStage(IPlayer player, String stage) {
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        if (mcPlayer == null)
            return false;

        IAdversityCapability.IProgression cap = mcPlayer.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
        if (cap != null && !cap.hasStage(stage)) {
            cap.addStage(stage);
            if (mcPlayer instanceof EntityPlayerMP) {
                ProgressionEventHandler.syncToClient((EntityPlayerMP) mcPlayer);
            }
            return true;
        }
        return false;
    }

    /**
     * 从玩家移除阶段
     */
    @ZenMethod
    public static boolean removeStage(IPlayer player, String stage) {
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        if (mcPlayer == null)
            return false;

        IAdversityCapability.IProgression cap = mcPlayer.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
        if (cap != null && cap.hasStage(stage)) {
            cap.removeStage(stage);
            if (mcPlayer instanceof EntityPlayerMP) {
                ProgressionEventHandler.syncToClient((EntityPlayerMP) mcPlayer);
            }
            return true;
        }
        return false;
    }

    /**
     * 获取玩家当前最高等级 (0-4)
     * 0=Uninitiated, 1=Awakened, 2=Scholar, 3=Warden, 4=Champion
     */
    @ZenMethod
    public static int getTier(IPlayer player) {
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        if (mcPlayer == null)
            return 0;

        return ProgressionEventHandler.getHighestTier(mcPlayer);
    }

    /**
     * 获取玩家所有阶段（逗号分隔字符串）
     */
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

    /**
     * 清除玩家所有阶段
     */
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
}
