package com.adversity.sanctuary.ritual;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * 圣所仪式定义
 */
public class Rite {

    private final ResourceLocation id;
    private final ItemStack input;
    private final ItemStack output;
    private final int entropyCost;
    private final String requiredStage;
    private final String rewardStage;

    public Rite(ResourceLocation id, ItemStack input, ItemStack output, int entropyCost, String requiredStage, String rewardStage) {
        this.id = id;
        this.input = input;
        this.output = output;
        this.entropyCost = entropyCost;
        this.requiredStage = requiredStage;
        this.rewardStage = rewardStage;
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
    
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.isItemEqual(input) && stack.getCount() >= input.getCount();
    }
}
