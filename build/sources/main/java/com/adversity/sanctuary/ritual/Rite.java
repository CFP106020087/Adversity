package com.adversity.sanctuary.ritual;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 圣所仪式定义
 * 支持单/多输入物品、命令执行、冷却、分类
 */
public class Rite {

    private final ResourceLocation id;
    private final List<ItemStack> inputs;
    private final ItemStack output;
    private final int entropyCost;
    private final String requiredStage;
    private final String rewardStage;
    private final String command;
    private final RitualCategory category;
    private final int cooldownTicks;

    // ==================== 旧版兼容构造器 ====================

    public Rite(ResourceLocation id, ItemStack input, ItemStack output, int entropyCost,
            String requiredStage, String rewardStage) {
        this(id, input, output, entropyCost, requiredStage, rewardStage, null);
    }

    public Rite(ResourceLocation id, ItemStack input, ItemStack output, int entropyCost,
            String requiredStage, String rewardStage, String command) {
        this(id, Collections.singletonList(input.copy()), output, entropyCost,
                requiredStage, rewardStage, command, guessCategory(rewardStage, command), 0);
    }

    // ==================== 完整构造器 ====================

    public Rite(ResourceLocation id, List<ItemStack> inputs, ItemStack output, int entropyCost,
            String requiredStage, String rewardStage, String command,
            RitualCategory category, int cooldownTicks) {
        this.id = id;
        this.inputs = new ArrayList<>();
        for (ItemStack s : inputs) {
            if (!s.isEmpty())
                this.inputs.add(s.copy());
        }
        this.output = output.isEmpty() ? ItemStack.EMPTY : output.copy();
        this.entropyCost = entropyCost;
        this.requiredStage = requiredStage;
        this.rewardStage = rewardStage;
        this.command = command;
        this.category = category;
        this.cooldownTicks = cooldownTicks;
    }

    private static RitualCategory guessCategory(String rewardStage, String command) {
        if (rewardStage != null && !rewardStage.isEmpty())
            return RitualCategory.STAGE_UNLOCK;
        if (command != null && !command.isEmpty()) {
            if (command.contains("advdiff"))
                return RitualCategory.DIFFICULTY;
            return RitualCategory.UTILITY;
        }
        return RitualCategory.CRAFTING;
    }

    // ==================== Getter ====================

    public ResourceLocation getId() {
        return id;
    }

    /** 兼容旧 API：返回第一个输入 */
    public ItemStack getInput() {
        return inputs.isEmpty() ? ItemStack.EMPTY : inputs.get(0);
    }

    public List<ItemStack> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    public boolean isMultiInput() {
        return inputs.size() > 1;
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

    public RitualCategory getCategory() {
        return category;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public boolean hasCooldown() {
        return cooldownTicks > 0;
    }

    public boolean hasCommand() {
        return command != null && !command.isEmpty();
    }

    /**
     * 执行仪式命令（静默执行，不广播）
     */
    public boolean executeCommand(MinecraftServer server, EntityPlayerMP player) {
        if (!hasCommand() || server == null)
            return false;

        String cmd = command
                .replace("{player}", player.getName())
                .replace("{x}", String.valueOf((int) player.posX))
                .replace("{y}", String.valueOf((int) player.posY))
                .replace("{z}", String.valueOf((int) player.posZ))
                .replace("{dim}", String.valueOf(player.dimension));

        try {
            server.getCommandManager().executeCommand(server, cmd);
            return true;
        } catch (Exception e) {
            com.adversity.Adversity.LOGGER.warn("Failed to execute ritual command: {}", cmd, e);
            return false;
        }
    }

    /**
     * 检查单输入是否匹配
     */
    public boolean matches(ItemStack stack) {
        if (isMultiInput())
            return false;
        ItemStack required = getInput();
        return !stack.isEmpty() && stack.isItemEqual(required) && stack.getCount() >= required.getCount();
    }

    /**
     * 检查多输入是否匹配
     */
    public boolean matchesMulti(List<ItemStack> available) {
        if (!isMultiInput())
            return false;
        for (ItemStack required : inputs) {
            boolean found = false;
            for (ItemStack avail : available) {
                if (!avail.isEmpty() && avail.isItemEqual(required) && avail.getCount() >= required.getCount()) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }
        return true;
    }
}
