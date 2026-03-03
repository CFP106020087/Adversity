package com.adversity.command;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IPlayerDifficulty;
import com.adversity.difficulty.DifficultyManager;
import com.adversity.difficulty.GlobalDifficultyData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 全局/个人难度管理命令 (仅OP)
 *
 * 全局用法:
 * /advdiff - 查看全局难度状态
 * /advdiff add <值> - 增加全局偏移
 * /advdiff sub <值> - 减少全局偏移
 * /advdiff set <值> - 设置全局偏移
 * /advdiff lock <值> - 锁定全局难度
 * /advdiff unlock - 解锁全局难度
 * /advdiff cap <值> - 设置全局上限
 * /advdiff multiplier <值> - 设置全局倍率
 * /advdiff reset - 重置全局
 * /advdiff enable/disable - 启用/禁用全局
 *
 * 个人用法 (加玩家名前缀):
 * /advdiff <玩家> add <值> - 增加该玩家个人偏移
 * /advdiff <玩家> sub <值> - 减少该玩家个人偏移
 * /advdiff <玩家> set <值> - 设置该玩家个人偏移
 * /advdiff <玩家> lock <值> - 锁定该玩家个人难度
 * /advdiff <玩家> unlock - 解锁该玩家个人难度
 * /advdiff <玩家> cap <值> - 设置该玩家个人上限
 * /advdiff <玩家> multiplier <值> - 设置该玩家个人倍率
 * /advdiff <玩家> reset - 重置该玩家个人设置
 * /advdiff <玩家> enable/disable - 启用/禁用该玩家难度
 * /advdiff <玩家> - 查看该玩家个人难度状态
 *
 * 最终难度 = (基础难度 × 全局倍率 + 全局偏移 + 个人偏移) × 玩家倍率
 */
public class CommandDifficulty extends CommandBase {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "add", "sub", "set", "lock", "unlock", "cap", "multiplier", "reset", "enable", "disable"
    );

    @Override
    public String getName() {
        return "advdiff";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("advdifficulty", "advd");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/advdiff [玩家] <add|sub|set|lock|unlock|cap|multiplier|reset|enable|disable> [值]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        World world = sender.getEntityWorld();
        GlobalDifficultyData globalData = GlobalDifficultyData.get(world);

        // 无参数 - 显示全局状态
        if (args.length == 0) {
            showGlobalStatus(sender, world, globalData);
            return;
        }

        String firstArg = args[0].toLowerCase();
        boolean isSubcommand = SUBCOMMANDS.contains(firstArg);

        if (isSubcommand) {
            // 全局难度操作
            executeGlobal(sender, globalData, args);
        } else {
            // 第一个参数是玩家名 -> 个人难度操作
            EntityPlayerMP target = getPlayer(server, sender, args[0]);
            IPlayerDifficulty cap = CapabilityHandler.getPlayerDifficulty(target);
            if (cap == null) {
                throw new CommandException("无法获取 " + target.getName() + " 的难度数据");
            }

            if (args.length < 2) {
                // /advdiff <player> -> 显示该玩家个人状态
                showPersonalStatus(sender, target, cap, world, globalData);
                return;
            }

            String subcommand = args[1].toLowerCase();
            executePersonal(sender, target, cap, subcommand, args, 2);
        }
    }

    // ==================== 全局操作 ====================

    private void executeGlobal(ICommandSender sender, GlobalDifficultyData data,
            String[] args) throws CommandException {
        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "add": {
                requireArg(args, 1, "/advdiff add <值>");
                float val = (float) parseDouble(args[1]);
                data.addGlobalOffset(val);
                sendSuccess(sender, "全局偏移增加了 " + TextFormatting.YELLOW + formatFloat(val) +
                        TextFormatting.GREEN + "，当前: " + TextFormatting.WHITE + formatFloat(data.getGlobalOffset()));
                break;
            }
            case "sub": {
                requireArg(args, 1, "/advdiff sub <值>");
                float val = (float) parseDouble(args[1]);
                data.addGlobalOffset(-val);
                sendSuccess(sender, "全局偏移减少了 " + TextFormatting.YELLOW + formatFloat(val) +
                        TextFormatting.GREEN + "，当前: " + TextFormatting.WHITE + formatFloat(data.getGlobalOffset()));
                break;
            }
            case "set": {
                requireArg(args, 1, "/advdiff set <值>");
                float val = (float) parseDouble(args[1]);
                data.setGlobalOffset(val);
                sendSuccess(sender, "全局偏移设为 " + TextFormatting.WHITE + formatFloat(val));
                break;
            }
            case "lock": {
                requireArg(args, 1, "/advdiff lock <难度值>");
                float val = (float) parseDouble(args[1], 0, 1000);
                data.setDifficultyLock(val);
                sendSuccess(sender, "全局难度已锁定为 " + TextFormatting.GOLD + formatFloat(val));
                break;
            }
            case "unlock":
                data.clearDifficultyLock();
                sendSuccess(sender, "全局难度锁定已解除");
                break;
            case "cap": {
                requireArg(args, 1, "/advdiff cap <最大值> (0=无上限)");
                float val = (float) parseDouble(args[1], 0, 1000);
                data.setMaxDifficultyCap(val);
                sendSuccess(sender, val > 0
                        ? "全局上限设为 " + TextFormatting.GOLD + formatFloat(val)
                        : "全局上限已移除");
                break;
            }
            case "multiplier": {
                requireArg(args, 1, "/advdiff multiplier <倍率>");
                float val = (float) parseDouble(args[1], 0, 100);
                data.setGlobalMultiplier(val);
                sendSuccess(sender, "全局倍率设为 " + TextFormatting.WHITE + formatFloat(val) + "x");
                break;
            }
            case "reset":
                data.setGlobalOffset(0);
                data.clearDifficultyLock();
                data.setMaxDifficultyCap(0);
                data.setGlobalMultiplier(1.0f);
                data.setEnabled(true);
                sendSuccess(sender, "所有全局设置已重置");
                break;
            case "enable":
                data.setEnabled(true);
                sendSuccess(sender, "全局难度系统 " + TextFormatting.GREEN + "已启用");
                break;
            case "disable":
                data.setEnabled(false);
                sendSuccess(sender, "全局难度系统 " + TextFormatting.RED + "已禁用");
                break;
            default:
                throw new CommandException("未知子命令: " + subcommand);
        }
    }

    // ==================== 个人操作 ====================

    private void executePersonal(ICommandSender sender, EntityPlayerMP target,
            IPlayerDifficulty cap, String subcommand,
            String[] args, int valueIndex) throws CommandException {
        String name = target.getName();
        String prefix = TextFormatting.AQUA + "[" + name + "] " + TextFormatting.GREEN;
        String playerMessage = ""; // 给目标玩家的通知内容

        switch (subcommand) {
            case "add": {
                requireArg(args, valueIndex, "/advdiff <玩家> add <值>");
                float val = (float) parseDouble(args[valueIndex]);
                cap.addPersonalOffset(val);
                playerMessage = "个人偏移 +" + formatFloat(val) + "，当前: " + formatFloat(cap.getPersonalOffset());
                sendSuccess(sender, prefix + "个人偏移增加了 " + TextFormatting.YELLOW + formatFloat(val) +
                        TextFormatting.GREEN + "，当前: " + TextFormatting.WHITE + formatFloat(cap.getPersonalOffset()));
                break;
            }
            case "sub": {
                requireArg(args, valueIndex, "/advdiff <玩家> sub <值>");
                float val = (float) parseDouble(args[valueIndex]);
                cap.addPersonalOffset(-val);
                playerMessage = "个人偏移 -" + formatFloat(val) + "，当前: " + formatFloat(cap.getPersonalOffset());
                sendSuccess(sender, prefix + "个人偏移减少了 " + TextFormatting.YELLOW + formatFloat(val) +
                        TextFormatting.GREEN + "，当前: " + TextFormatting.WHITE + formatFloat(cap.getPersonalOffset()));
                break;
            }
            case "set": {
                requireArg(args, valueIndex, "/advdiff <玩家> set <值>");
                float val = (float) parseDouble(args[valueIndex]);
                cap.setPersonalOffset(val);
                playerMessage = "个人偏移设为 " + formatFloat(val);
                sendSuccess(sender, prefix + "个人偏移设为 " + TextFormatting.WHITE + formatFloat(val));
                break;
            }
            case "lock": {
                requireArg(args, valueIndex, "/advdiff <玩家> lock <值>");
                float val = (float) parseDouble(args[valueIndex], 0, 1000);
                cap.setPersonalLock(val);
                playerMessage = "个人难度已锁定为 " + formatFloat(val);
                sendSuccess(sender, prefix + "个人难度已锁定为 " + TextFormatting.GOLD + formatFloat(val));
                break;
            }
            case "unlock":
                cap.clearPersonalLock();
                playerMessage = "个人难度锁定已解除";
                sendSuccess(sender, prefix + "个人难度锁定已解除");
                break;
            case "cap": {
                requireArg(args, valueIndex, "/advdiff <玩家> cap <最大值> (0=无上限)");
                float val = (float) parseDouble(args[valueIndex], 0, 1000);
                cap.setPersonalCap(val);
                playerMessage = val > 0 ? "个人上限设为 " + formatFloat(val) : "个人上限已移除";
                sendSuccess(sender, prefix + (val > 0
                        ? "个人上限设为 " + TextFormatting.GOLD + formatFloat(val)
                        : "个人上限已移除"));
                break;
            }
            case "multiplier": {
                requireArg(args, valueIndex, "/advdiff <玩家> multiplier <倍率>");
                float val = (float) parseDouble(args[valueIndex], 0, 6);
                cap.setDifficultyMultiplier(val);
                playerMessage = "个人倍率设为 " + formatFloat(val) + "x";
                sendSuccess(sender, prefix + "个人倍率设为 " + TextFormatting.WHITE + formatFloat(val) + "x");
                break;
            }
            case "reset":
                cap.setPersonalOffset(0);
                cap.clearPersonalLock();
                cap.setPersonalCap(0);
                cap.setDifficultyMultiplier(1.0f);
                cap.setDifficultyDisabled(false);
                playerMessage = "所有个人难度设置已重置";
                sendSuccess(sender, prefix + "所有个人难度设置已重置");
                break;
            case "enable":
                cap.setDifficultyDisabled(false);
                playerMessage = "个人难度已启用";
                sendSuccess(sender, prefix + "个人难度 " + TextFormatting.GREEN + "已启用");
                break;
            case "disable":
                cap.setDifficultyDisabled(true);
                playerMessage = "个人难度已禁用";
                sendSuccess(sender, prefix + "个人难度 " + TextFormatting.RED + "已禁用");
                break;
            default:
                throw new CommandException("未知子命令: " + subcommand + "。使用: " + String.join(", ", SUBCOMMANDS));
        }

        // 通知目标玩家
        if (sender != target) {
            target.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "[Adversity] " + "你的难度被设置为: " + playerMessage));
        }

        // 同步到客户端
        syncPlayerDifficulty(target, cap);
    }

    // ==================== 状态显示 ====================

    private void showGlobalStatus(ICommandSender sender, World world, GlobalDifficultyData data) {
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Adversity 全局难度状态 ==="));

        String statusStr = data.isEnabled()
                ? TextFormatting.GREEN + "已启用"
            : TextFormatting.RED + "已禁用";
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "系统状态: " + statusStr));
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "全局偏移: " +
                TextFormatting.WHITE + formatFloat(data.getGlobalOffset())));
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "全局倍率: " +
            TextFormatting.WHITE + formatFloat(data.getGlobalMultiplier()) + "x"));

        if (data.isDifficultyLocked()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "难度锁定: " +
                TextFormatting.GOLD + "锁定为 " + formatFloat(data.getDifficultyLock())));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "难度锁定: " +
                TextFormatting.GRAY + "未锁定"));
        }

        if (data.getMaxDifficultyCap() > 0) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "难度上限: " +
                TextFormatting.RED + formatFloat(data.getMaxDifficultyCap())));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "难度上限: " +
                TextFormatting.GRAY + "无"));
        }

        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            float baseDifficulty = DifficultyManager.calculateDifficulty(world, player.getPosition(), player);
            float modifiedDifficulty = data.applyGlobalModifiers(baseDifficulty);
            float finalDifficulty = DifficultyManager.calculateDifficultyAt(world, player.getPosition(), player);

            sender.sendMessage(new TextComponentString(""));
            sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "--- 你所在位置的难度 ---"));
            sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "基础难度: " +
                TextFormatting.WHITE + formatFloat(baseDifficulty)));
            sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "全局修正后: " +
                TextFormatting.WHITE + formatFloat(modifiedDifficulty)));
            sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "最终难度: " +
                TextFormatting.YELLOW + formatFloat(finalDifficulty)));
        }
    }

    private void showPersonalStatus(ICommandSender sender, EntityPlayerMP target,
            IPlayerDifficulty cap, World world, GlobalDifficultyData globalData) {
        sender.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "=== " + target.getName() + " 的个人难度 ==="));

        String statusStr = cap.isDifficultyDisabled()
                ? TextFormatting.RED + "已禁用"
                : TextFormatting.GREEN + "已启用";
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "状态: " + statusStr));
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "个人倍率: " +
                TextFormatting.WHITE + formatFloat(cap.getDifficultyMultiplier()) + "x"));
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "个人偏移: " +
                TextFormatting.WHITE + formatFloat(cap.getPersonalOffset())));

        if (cap.getPersonalLock() >= 0) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "个人锁定: " +
                    TextFormatting.GOLD + "锁定为 " + formatFloat(cap.getPersonalLock())));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "个人锁定: " +
                    TextFormatting.GRAY + "未锁定"));
        }

        if (cap.getPersonalCap() > 0) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "个人上限: " +
                    TextFormatting.RED + formatFloat(cap.getPersonalCap())));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "个人上限: " +
                    TextFormatting.GRAY + "无"));
        }
    }

    // ==================== 辅助方法 ====================

    private void requireArg(String[] args, int index, String usage) throws CommandException {
        if (args.length <= index) {
            throw new CommandException("用法: " + usage);
        }
    }

    private void syncPlayerDifficulty(EntityPlayerMP target, IPlayerDifficulty cap) {
        com.adversity.network.PacketHandler.INSTANCE.sendTo(
                new com.adversity.network.PacketSyncPlayerDifficulty(cap), target);
    }

    private void sendSuccess(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[Adversity] " + message));
    }

    private String formatFloat(float value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format("%.2f", value);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
            String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            // 子命令 + 玩家名
            List<String> completions = new ArrayList<>(SUBCOMMANDS);
            completions.addAll(Arrays.asList(server.getOnlinePlayerNames()));
            return getListOfStringsMatchingLastWord(args, completions);
        }

        String firstArg = args[0].toLowerCase();
        if (SUBCOMMANDS.contains(firstArg)) {
            // 全局模式：无额外补全
            return Collections.emptyList();
        }

        // 个人模式: /advdiff <player> <subcommand>
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }

        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        if (index == 0 && args.length > 0) {
            return !SUBCOMMANDS.contains(args[0].toLowerCase());
        }
        return false;
    }
}
