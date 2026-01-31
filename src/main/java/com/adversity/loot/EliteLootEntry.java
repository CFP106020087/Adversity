package com.adversity.loot;

import net.minecraft.item.ItemStack;

public class EliteLootEntry {
    private final ItemStack stack;
    private final float chance;

    public EliteLootEntry(ItemStack stack, float chance) {
        this.stack = stack;
        this.chance = chance;
    }

    public ItemStack getStack() {
        return stack.copy(); // Return copy to prevent modification
    }

    public float getChance() {
        return chance;
    }
}
