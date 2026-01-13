package com.adversity.command;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IPlayerDifficulty;
import com.adversity.capability.IPlayerDifficulty.ScalingMode;
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
import java.util.Locale;

/**
 * 逆境难度调整指令
 *
 * 用法 (玩家调整自己，无需权限):
 *   /adversity                                    - 查看自己的难度状态
 *   /adversity set <0.0-6.0>                     - 设置自己的难度倍率
 *   /adversity preset <预设>                     - 使用预设
 *   /adversity disable                           - 禁用自己的难度系统
 *   /adversity enable                            - 启用自己的难度系统
 *   /adversity reset                             - 重置自己的难度
 *   /adversity health <模式>                     - 设置血量增长模式
 *   /adversity damage <模式>                     - 设置伤害增长模式
 *
 * 用法 (OP调整他人):
 *   /adversity <玩家名> set <0.0-6.0>            - 设置指定玩家的难度倍率
 *   /adversity <玩家名> preset <预设>            - 为指定玩家使用预设
 *   /adversity <玩家名> disable                  - 为指定玩家禁用难度
 *   /adversity <玩家名> enable                   - 为指定玩家启用难度
 *   /adversity <玩家名> reset                    - 重置指定玩家的难度
 *   /adversity <玩家名> health <模式>            - 设置指定玩家血量增长模式
 *   /adversity <玩家名> damage <模式>            - 设置指定玩家伤害增长模式
 *
 * 增长模式:
 *   default      - 使用服务器配置
 *   linear       - 线性增长 (base + diff × rate)
 *   exponential  - 指数增长 (base × e^(diff × rate))
 *   compound     - 复合增长 (base × (1 + rate)^diff)
 *   polynomial   - 多项式增长 (base + diff^power × rate)
 *   logarithmic  - 对数增长 (base + ln(1+diff) × rate)
 *   sigmoid      - S型曲线 (平滑过渡到最大值)
 */
public class CommandAdversity extends CommandBase {

    private static final List<String> SUBCOMMANDS = Arrays.asList("set", "preset", "disable", "enable", "reset", "health", "damage");
    private static final List<String> PRESETS = Arrays.asList("peaceful", "easy", "normal", "hard", "nightmare");
    private static final List<String> SCALING_MODES = Arrays.asList("default", "linear", "exponential", "compound", "polynomial", "logarithmic", "sigmoid");

    @Override
    public String getName() {
        return "adversity";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        if (sender instanceof EntityPlayer) {
            return "/adversity - 查看难度\n" +
                   "/adversity <set|preset|health|damage|...> [值] - 调整自己\n" +
                   "/adversity <玩家> <set|preset|...> [值] - 调整他人(OP)";
        }
        return "/adversity <player> <set|preset|disable|enable|reset|health|damage> [value]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        // 无参数 - 玩家查看自己的难度状态
        if (args.length == 0) {
            if (!(sender instanceof EntityPlayer)) {
                throw new CommandException("Usage: /adversity <player> <set|preset|disable|enable|reset|health|damage> [value]");
            }
            EntityPlayer player = (EntityPlayer) sender;
            IPlayerDifficulty cap = CapabilityHandler.getPlayerDifficulty(player);
            if (cap == null) {
                throw new CommandException("Failed to get difficulty data");
            }
            showStatus(player, player, cap);
            return;
        }

        // 判断第一个参数是子命令还是玩家名
        String firstArg = args[0].toLowerCase();
        boolean isSubcommand = SUBCOMMANDS.contains(firstArg);

        if (isSubcommand) {
            // 玩家调整自己的设置（无需管理员权限）
            if (!(sender instanceof EntityPlayer)) {
                throw new CommandException("Console must specify a player: /adversity <player> ...");
            }
            EntityPlayer player = (EntityPlayer) sender;
            IPlayerDifficulty cap = CapabilityHandler.getPlayerDifficulty(player);
            if (cap == null) {
                throw new CommandException("Failed to get difficulty data");
            }
            executeSubcommand(sender, player, cap, args, 0, false);
        } else {
            // 调整他人 - 需要 OP 权限
            if (!hasOpPermission(server, sender)) {
                throw new CommandException("修改他人设置需要管理员权限");
            }

            if (args.length < 2) {
                throw new CommandException("Usage: /adversity <player> <set|preset|disable|enable|reset|health|damage> [value]");
            }

            EntityPlayerMP targetPlayer = getPlayer(server, sender, args[0]);
            IPlayerDifficulty cap = CapabilityHandler.getPlayerDifficulty(targetPlayer);

            if (cap == null) {
                throw new CommandException("Failed to get difficulty data for " + targetPlayer.getName());
            }

            executeSubcommand(sender, targetPlayer, cap, args, 1, true);
        }
    }

    /**
     * 执行子命令
     * @param sender 命令发送者
     * @param target 目标玩家
     * @param cap 目标玩家的能力数据
     * @param args 参数数组
     * @param subcommandIndex 子命令在参数中的索引
     * @param isAdmin 是否管理员操作
     */
    private void executeSubcommand(ICommandSender sender, EntityPlayer target, IPlayerDifficulty cap,
                                   String[] args, int subcommandIndex, boolean isAdmin) throws CommandException {
        String subcommand = args[subcommandIndex].toLowerCase();
        int valueIndex = subcommandIndex + 1;

        switch (subcommand) {
            case "set":
                if (args.length <= valueIndex) {
                    throw new CommandException("Usage: /adversity " + (isAdmin ? "<player> " : "") + "set <0.0-6.0>");
                }
                float multiplier = (float) parseDouble(args[valueIndex], 0.0, 6.0);
                cap.setDifficultyMultiplier(multiplier);
                notifySuccess(sender, target, "难度倍率设为 " + formatMultiplier(multiplier), isAdmin);
                break;

            case "preset":
                if (args.length <= valueIndex) {
                    throw new CommandException("Usage: /adversity " + (isAdmin ? "<player> " : "") + "preset <peaceful|easy|normal|hard|nightmare>");
                }
                applyPreset(sender, target, cap, args[valueIndex].toLowerCase(), isAdmin);
                break;

            case "disable":
                cap.setDifficultyDisabled(true);
                notifySuccess(sender, target, "Adversity系统 " + TextFormatting.RED + "已禁用", isAdmin);
                break;

            case "enable":
                cap.setDifficultyDisabled(false);
                notifySuccess(sender, target, "Adversity系统 " + TextFormatting.GREEN + "已启用", isAdmin);
                break;

            case "reset":
                cap.setDifficultyMultiplier(1.0f);
                cap.setDifficultyDisabled(false);
                cap.resetKillCount();
                cap.setHealthScalingMode(ScalingMode.DEFAULT);
                cap.setDamageScalingMode(ScalingMode.DEFAULT);
                notifySuccess(sender, target, "难度已重置为默认值", isAdmin);
                break;

            case "health":
                if (args.length <= valueIndex) {
                    throw new CommandException("Usage: /adversity " + (isAdmin ? "<player> " : "") + "health <" + String.join("|", SCALING_MODES) + ">");
                }
                ScalingMode healthMode = parseScalingMode(args[valueIndex]);
                cap.setHealthScalingMode(healthMode);
                notifySuccess(sender, target, "血量增长模式设为 " + TextFormatting.AQUA + healthMode.name(), isAdmin);
                break;

            case "damage":
                if (args.length <= valueIndex) {
                    throw new CommandException("Usage: /adversity " + (isAdmin ? "<player> " : "") + "damage <" + String.join("|", SCALING_MODES) + ">");
                }
                ScalingMode damageMode = parseScalingMode(args[valueIndex]);
                cap.setDamageScalingMode(damageMode);
                notifySuccess(sender, target, "伤害增长模式设为 " + TextFormatting.RED + damageMode.name(), isAdmin);
                break;

            default:
                throw new CommandException("Unknown subcommand: " + subcommand + ". Use: set, preset, disable, enable, reset, health, damage");
        }
    }

    /**
     * 解析缩放模式
     */
    private ScalingMode parseScalingMode(String input) throws CommandException {
        try {
            return ScalingMode.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CommandException("Invalid scaling mode: " + input + ". Valid modes: " + String.join(", ", SCALING_MODES));
        }
    }

    /**
     * 检查是否有 OP 权限
     */
    private boolean hasOpPermission(MinecraftServer server, ICommandSender sender) {
        if (!(sender instanceof EntityPlayer)) {
            return true; // 控制台始终有权限
        }
        return server.getPlayerList().canSendCommands(((EntityPlayer) sender).getGameProfile());
    }

    private void showStatus(ICommandSender sender, EntityPlayer target, IPlayerDifficulty cap) {
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== " + target.getName() + " 的逆境难度 ==="));

        String status = cap.isDifficultyDisabled()
            ? TextFormatting.RED + "已禁用"
            : TextFormatting.GREEN + "已启用";
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "状态: " + status));

        String multiplierStr = formatMultiplier(cap.getDifficultyMultiplier());
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "难度倍率: " + TextFormatting.WHITE + multiplierStr));

        String presetName = getPresetName(cap.getDifficultyMultiplier());
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "预设: " + TextFormatting.WHITE + presetName));

        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "击杀数: " + TextFormatting.WHITE + cap.getKillCount()));

        // 显示缩放模式
        String healthMode = cap.getHealthScalingMode() == ScalingMode.DEFAULT
            ? "服务器默认" : cap.getHealthScalingMode().name();
        String damageMode = cap.getDamageScalingMode() == ScalingMode.DEFAULT
            ? "服务器默认" : cap.getDamageScalingMode().name();

        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "血量增长: " + TextFormatting.AQUA + healthMode));
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "伤害增长: " + TextFormatting.RED + damageMode));
    }

    private void applyPreset(ICommandSender sender, EntityPlayer target, IPlayerDifficulty cap,
                            String preset, boolean isAdmin) throws CommandException {
        float multiplier;
        String displayName;

        switch (preset) {
            case "peaceful":
                multiplier = 0.0f;
                displayName = TextFormatting.AQUA + "和平";
                cap.setDifficultyDisabled(true);
                break;
            case "easy":
                multiplier = 0.5f;
                displayName = TextFormatting.GREEN + "简单";
                cap.setDifficultyDisabled(false);
                break;
            case "normal":
                multiplier = 1.0f;
                displayName = TextFormatting.YELLOW + "普通";
                cap.setDifficultyDisabled(false);
                break;
            case "hard":
                multiplier = 1.5f;
                displayName = TextFormatting.RED + "困难";
                cap.setDifficultyDisabled(false);
                break;
            case "nightmare":
                multiplier = 2.0f;
                displayName = TextFormatting.DARK_RED + "噩梦";
                cap.setDifficultyDisabled(false);
                break;
            default:
                throw new CommandException("Unknown preset: " + preset + ". Available: peaceful, easy, normal, hard, nightmare");
        }

        cap.setDifficultyMultiplier(multiplier);
        notifySuccess(sender, target, "难度设为 " + displayName, isAdmin);
    }

    private String formatMultiplier(float multiplier) {
        if (multiplier == 0.0f) return "0.0x (和平)";
        if (multiplier == 0.5f) return "0.5x (简单)";
        if (multiplier == 1.0f) return "1.0x (普通)";
        if (multiplier == 1.5f) return "1.5x (困难)";
        if (multiplier == 2.0f) return "2.0x (噩梦)";
        return String.format("%.1fx", multiplier);
    }

    private String getPresetName(float multiplier) {
        if (multiplier == 0.0f) return "和平";
        if (multiplier <= 0.5f) return "简单";
        if (multiplier <= 1.0f) return "普通";
        if (multiplier <= 1.5f) return "困难";
        return "噩梦";
    }

    /**
     * 通知操作成功
     */
    private void notifySuccess(ICommandSender sender, EntityPlayer target, String message, boolean isAdmin) {
        // 通知执行者
        String prefix = isAdmin ? "[Adversity] " + target.getName() + ": " : "[Adversity] ";
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + prefix + message));

        // 如果是管理员操作且执行者不是目标玩家，也通知目标玩家
        if (isAdmin && sender != target) {
            target.sendMessage(new TextComponentString(TextFormatting.GREEN + "[Adversity] " + message));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            // 第一个参数：可以是子命令（调整自己）或玩家名（调整他人）
            List<String> completions = new java.util.ArrayList<>(SUBCOMMANDS);
            // 只有OP才显示玩家名补全
            if (hasOpPermission(server, sender)) {
                completions.addAll(Arrays.asList(server.getOnlinePlayerNames()));
            }
            return getListOfStringsMatchingLastWord(args, completions);
        }

        String firstArg = args[0].toLowerCase();
        boolean isSubcommand = SUBCOMMANDS.contains(firstArg);

        if (isSubcommand) {
            // 玩家调整自己：/adversity <subcommand> [value]
            if (args.length == 2) {
                if ("preset".equals(firstArg)) {
                    return getListOfStringsMatchingLastWord(args, PRESETS);
                }
                if ("health".equals(firstArg) || "damage".equals(firstArg)) {
                    return getListOfStringsMatchingLastWord(args, SCALING_MODES);
                }
            }
        } else {
            // 调整他人：/adversity <player> <subcommand> [value]
            if (args.length == 2) {
                return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
            }
            if (args.length == 3) {
                String subcommand = args[1].toLowerCase();
                if ("preset".equals(subcommand)) {
                    return getListOfStringsMatchingLastWord(args, PRESETS);
                }
                if ("health".equals(subcommand) || "damage".equals(subcommand)) {
                    return getListOfStringsMatchingLastWord(args, SCALING_MODES);
                }
            }
        }

        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        // 只有当第一个参数不是子命令时，才把 index 0 当作用户名
        if (index == 0 && args.length > 0) {
            return !SUBCOMMANDS.contains(args[0].toLowerCase());
        }
        return false;
    }
}
