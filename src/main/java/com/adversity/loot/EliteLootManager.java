package com.adversity.loot;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.item.ItemStack;

import java.util.Collection;
import java.util.Collections;

public class EliteLootManager {

    // Tier -> Drops
    private static final Multimap<Integer, EliteLootEntry> CUSTOM_LOOT = ArrayListMultimap.create();

    public static void addDrop(int tier, ItemStack stack, float chance) {
        CUSTOM_LOOT.put(tier, new EliteLootEntry(stack, chance));
    }

    public static Collection<EliteLootEntry> getDropsForTier(int tier) {
        return CUSTOM_LOOT.get(tier);
    }
    
    public static void clear() {
        CUSTOM_LOOT.clear();
    }
}
