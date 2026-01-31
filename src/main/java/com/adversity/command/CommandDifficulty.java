package com.adversity.command;

import com.adversity.difficulty.DifficultyManager;
import com.adversity.difficulty.GlobalDifficultyData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 全局难度管理命令 (仅OP)
 * 
 * 用法:
 *   /difficulty                         - 查看当前全局难度状态
 *   /difficulty add <值>                - 增加全局难度偏移
 *   /difficulty sub <值>                - 减少全局难度偏移
 *   /difficulty set <值>                - 设置全局难度偏移
 *   /difficulty lock <值>               - 锁定难度为固定值
 *   /difficulty unlock                  - 解除难度锁定
 *   /difficulty cap <值>                - 设置难度上限 (0=无上限)
 *   /difficulty multiplier <值>         - 设置全局难度倍率
 *   /difficulty reset                   - 重置所有全局设置
 *   /difficulty enable                  - 启用全局难度系统
 *   /difficulty disable                 - 禁用全局难度系统
 * 
 * 设计理念:
 *   这个命令管理的是"世界难度"，与 /adversity 管理的"玩家个人难度"分开
 *   最终难度 = (基础难度 × 全局倍率 + 全局偏移) × 玩家倍率
 */
public class CommandDifficulty extends CommandBase {
    
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "add", "sub", "set", "lock", "unlock", "cap", "multiplier", "reset", "enable", "disable"
    );
    
    @Override
    public String getName() {
        return "advdiff";  // adversity difficulty，避免与原版冲突
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("advdifficulty", "advd");
    }
    
    @Override
    public String getUsage(ICommandSender sender) {
        return "/advdiff <add|sub|set|lock|unlock|cap|multiplier|reset|enable|disable> [值]";
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 2;  // 需要OP权限
    }
    
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        World world = sender.getEntityWorld();
        GlobalDifficultyData data = GlobalDifficultyData.get(world);
        
        // 无参数 - 显示状态
        if (args.length == 0) {
            showStatus(sender, world, data);
            return;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "add":
                if (args.length < 2) {
                    throw new CommandException("用法: /advdiff add <值>");
                }
                float addValue = (float) parseDouble(args[1]);
                data.addGlobalOffset(addValue);
                sendSuccess(sender, "全局难度偏移增加了 " + TextFormatting.YELLOW + formatFloat(addValue) + 
                           TextFormatting.GREEN + "，当前偏移: " + TextFormatting.WHITE + formatFloat(data.getGlobalOffset()));
                break;
                
            case "sub":
                if (args.length < 2) {
                    throw new CommandException("用法: /advdiff sub <值>");
                }
                float subValue = (float) parseDouble(args[1]);
                data.addGlobalOffset(-subValue);
                sendSuccess(sender, "全局难度偏移减少了 " + TextFormatting.YELLOW + formatFloat(subValue) + 
                           TextFormatting.GREEN + "，当前偏移: " + TextFormatting.WHITE + formatFloat(data.getGlobalOffset()));
                break;
                
            case "set":
                if (args.length < 2) {
                    throw new CommandException("用法: /advdiff set <值>");
                }
                float setValue = (float) parseDouble(args[1]);
                data.setGlobalOffset(setValue);
                sendSuccess(sender, "全局难度偏移设为 " + TextFormatting.WHITE + formatFloat(setValue));
                break;
                
            case "lock":
                if (args.length < 2) {
                    throw new CommandException("用法: /advdiff lock <难度值>");
                }
                float lockValue = (float) parseDouble(args[1], 0, 1000);
                data.setDifficultyLock(lockValue);
                sendSuccess(sender, "难度已锁定为 " + TextFormatting.GOLD + formatFloat(lockValue));
                break;
                
            case "unlock":
                data.clearDifficultyLock();
                sendSuccess(sender, "难度锁定已解除");
                break;
                
            case "cap":
                if (args.length < 2) {
                    throw new CommandException("用法: /advdiff cap <最大值> (0=无上限)");
                }
                float capValue = (float) parseDouble(args[1], 0, 1000);
                data.setMaxDifficultyCap(capValue);
                if (capValue > 0) {
                    sendSuccess(sender, "难度上限设为 " + TextFormatting.GOLD + formatFloat(capValue));
                } else {
                    sendSuccess(sender, "难度上限已移除");
                }
                break;
                
            case "multiplier":
                if (args.length < 2) {
                    throw new CommandException("用法: /advdiff multiplier <倍率>");
                }
                float multValue = (float) parseDouble(args[1], 0, 100);
                data.setGlobalMultiplier(multValue);
                sendSuccess(sender, "全局难度倍率设为 " + TextFormatting.WHITE + formatFloat(multValue) + "x");
                break;
                
            case "reset":
                data.setGlobalOffset(0);
                data.clearDifficultyLock();
                data.setMaxDifficultyCap(0);
                data.setGlobalMultiplier(1.0f);
                data.setEnabled(true);
                sendSuccess(sender, "所有全局难度设置已重置");
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
                throw new CommandException("未知子命令: " + subcommand + "。使用: " + String.join(", ", SUBCOMMANDS));
        }
    }
    
    /**
     * 显示当前难度状态
     */
    private void showStatus(ICommandSender sender, World world, GlobalDifficultyData data) {
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Adversity 全局难度状态 ==="));
        
        // 系统状态
        String statusStr = data.isEnabled() 
            ? TextFormatting.GREEN + "已启用" 
            : TextFormatting.RED + "已禁用";
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "系统状态: " + statusStr));
        
        // 全局偏移
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "全局偏移: " + 
            TextFormatting.WHITE + formatFloat(data.getGlobalOffset())));
        
        // 全局倍率
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "全局倍率: " + 
            TextFormatting.WHITE + formatFloat(data.getGlobalMultiplier()) + "x"));
        
        // 锁定状态
        if (data.isDifficultyLocked()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "难度锁定: " + 
                TextFormatting.GOLD + "锁定为 " + formatFloat(data.getDifficultyLock())));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "难度锁定: " + 
                TextFormatting.GRAY + "未锁定"));
        }
        
        // 上限
        if (data.getMaxDifficultyCap() > 0) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "难度上限: " + 
                TextFormatting.RED + formatFloat(data.getMaxDifficultyCap())));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "难度上限: " + 
                TextFormatting.GRAY + "无"));
        }
        
        // 如果命令发送者是玩家，显示该玩家位置的当前难度
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
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        return Collections.emptyList();
    }
}
