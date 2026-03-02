package com.adversity.sanctuary.ritual;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;

/**
 * 圣所仪式定义
 * 支持物品转换和命令执行
 */
public class Rite {

    private final ResourceLocation id;
    private final ItemStack input;
    private final ItemStack output;
    private final int entropyCost;
    private final String requiredStage;
    private final String rewardStage;
    private final String command; // 新增：静默执行的命令

    public Rite(ResourceLocation id, ItemStack input, ItemStack output, int entropyCost, String requiredStage, String rewardStage) {
        this(id, input, output, entropyCost, requiredStage, rewardStage, null);
    }

    public Rite(ResourceLocation id, ItemStack input, ItemStack output, int entropyCost, String requiredStage,
            String rewardStage, String command) {
        this.id = id;
        this.input = input;
        this.output = output;
        this.entropyCost = entropyCost;
        this.requiredStage = requiredStage;
        this.rewardStage = rewardStage;
        this.command = command;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ItemStack getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output;
    }

    public int getEntropyCost() {
        return entropyCost;
    }

    public String getRequiredStage() {
        return requiredStage;
    }

    public String getRewardStage() {
        return rewardStage;
    }
    
    public String getCommand() {
        return command;
    }

    public boolean hasCommand() {
        return command != null && !command.isEmpty();
    }

    /**
     * 执行仪式命令（静默执行，不广播）
     * 
     * @param server Minecraft服务器
     * @param player 执行仪式的玩家
     * @return 是否执行成功
     */
    public boolean executeCommand(MinecraftServer server, EntityPlayerMP player) {
        if (!hasCommand() || server == null)
            return false;

        // 替换命令中的占位符
        String cmd = command
                .replace("{player}", player.getName())
                .replace("{x}", String.valueOf((int) player.posX))
                .replace("{y}", String.valueOf((int) player.posY))
                .replace("{z}", String.valueOf((int) player.posZ))
                .replace("{dim}", String.valueOf(player.dimension));

        // 静默执行命令（使用服务器权限，不广播输出）
        try {
            server.getCommandManager().executeCommand(server, cmd);
            return true;
        } catch (Exception e) {
            com.adversity.Adversity.LOGGER.warn("Failed to execute ritual command: {}", cmd, e);
            return false;
        }
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.isItemEqual(input) && stack.getCount() >= input.getCount();
    }
}

