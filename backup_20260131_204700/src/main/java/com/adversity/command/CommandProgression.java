package com.adversity.command;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.progression.ProgressionEventHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 进度管理命令
 *
 * 用法:
 * /progression list - 列出当前阶段
 * /progression add <stage> - 添加阶段
 * /progression remove <stage> - 移除阶段
 * /progression clear - 清除所有阶段
 * /progression tier - 显示当前最高等级
 */
public class CommandProgression extends CommandBase {

    private static final List<String> SUBCOMMANDS = Arrays.asList("list", "add", "remove", "clear", "tier");
    private static final List<String> STAGES = Arrays.asList(
            "awakened", "scholar", "warden", "champion");

    @Override
    public String getName() {
        return "progression";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/progression <list|add|remove|clear|tier> [stage]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP权限
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            throw new CommandException("This command can only be used by players");
        }

        EntityPlayer player = (EntityPlayer) sender;

        if (args.length < 1) {
            throw new CommandException("Usage: " + getUsage(sender));
        }

        String subCommand = args[0].toLowerCase();
        IAdversityCapability.IProgression cap = player.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);

        if (cap == null) {
            throw new CommandException("Progression capability not found!");
        }

        switch (subCommand) {
            case "list":
                Set<String> stages = cap.getStages();
                if (stages.isEmpty()) {
                    sender.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "[Adversity] " + TextFormatting.GRAY + "No stages unlocked."));
                } else {
                    sender.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "[Adversity] " + TextFormatting.GREEN + "Stages: " +
                                    TextFormatting.WHITE + String.join(", ", stages)));
                }
                break;

            case "add":
                if (args.length < 2) {
                    throw new CommandException("Usage: /progression add <stage>");
                }
                String stageToAdd = args[1].toLowerCase();
                if (!cap.hasStage(stageToAdd)) {
                    cap.addStage(stageToAdd);
                    sender.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "[Adversity] Added stage: " + TextFormatting.GOLD + stageToAdd));
                    if (player instanceof EntityPlayerMP) {
                        ProgressionEventHandler.syncToClient((EntityPlayerMP) player);
                    }
                } else {
                    sender.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "[Adversity] Stage already exists: " + stageToAdd));
                }
                break;

            case "remove":
                if (args.length < 2) {
                    throw new CommandException("Usage: /progression remove <stage>");
                }
                String stageToRemove = args[1].toLowerCase();
                if (cap.hasStage(stageToRemove)) {
                    cap.removeStage(stageToRemove);
                    sender.sendMessage(new TextComponentString(
                            TextFormatting.RED + "[Adversity] Removed stage: " + stageToRemove));
                    if (player instanceof EntityPlayerMP) {
                        ProgressionEventHandler.syncToClient((EntityPlayerMP) player);
                    }
                } else {
                    sender.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "[Adversity] Stage not found: " + stageToRemove));
                }
                break;

            case "clear":
                cap.clear();
                sender.sendMessage(new TextComponentString(
                        TextFormatting.RED + "[Adversity] All stages cleared."));
                if (player instanceof EntityPlayerMP) {
                    ProgressionEventHandler.syncToClient((EntityPlayerMP) player);
                }
                break;

            case "tier":
                int tier = ProgressionEventHandler.getHighestTier(player);
                String tierName;
                switch (tier) {
                    case 1:
                        tierName = "Awakened";
                        break;
                    case 2:
                        tierName = "Scholar";
                        break;
                    case 3:
                        tierName = "Warden";
                        break;
                    case 4:
                        tierName = "Champion";
                        break;
                    default:
                        tierName = "Uninitiated";
                        break;
                }
                sender.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "[Adversity] Current Tier: " +
                                TextFormatting.GOLD + tier + " (" + tierName + ")"));
                break;

            default:
                throw new CommandException("Unknown subcommand: " + subCommand);
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        if (args.length == 2 && (args[0].equals("add") || args[0].equals("remove"))) {
            return getListOfStringsMatchingLastWord(args, STAGES);
        }
        return Collections.emptyList();
    }
}
