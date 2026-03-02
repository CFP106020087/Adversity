package com.adversity.command;

import com.adversity.sanctuary.SanctuaryGenerator;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 圣所生成测试指令
 *
 * 用法:
 *   /sanctuary <tier>     - 在当前位置生成指定等级的圣所 (1-4)
 *   /sanctuary 1          - 生成T1圣所 (9x9)
 *   /sanctuary 2          - 生成T2圣所 (13x13)
 *   /sanctuary 3          - 生成T3圣所 (17x17)
 *   /sanctuary 4          - 生成T4圣所 (25x25)
 */
public class CommandSanctuary extends CommandBase {

    @Override
    public String getName() {
        return "sanctuary";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/sanctuary <tier:1-4>";
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
            throw new CommandException("Usage: /sanctuary <tier:1-4>");
        }

        int tier = parseInt(args[0], 1, 4);

        BlockPos pos = player.getPosition();

        // 生成圣所
        SanctuaryGenerator.generateSanctuaryAtPosition(player.world, pos, tier);

        sender.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "[Adversity] 已在 " + 
            TextFormatting.YELLOW + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + 
            TextFormatting.GREEN + " 生成 T" + tier + " 圣所"
        ));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("1", "2", "3", "4"));
        }
        return Collections.emptyList();
    }
}
