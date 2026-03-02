package com.adversity.integration.crafttweaker;

import com.adversity.loot.EliteLootManager;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.adversity.EliteLoot")
public class EliteLootCT {

    @ZenMethod
    public static void addDrop(int tier, IItemStack stack, float chance) {
        if (stack == null) return;
        EliteLootManager.addDrop(
            tier,
            CraftTweakerMC.getItemStack(stack),
            chance
        );
    }
    
    // 也可以添加个清理方法
    @ZenMethod
    public static void clear() {
        EliteLootManager.clear();
    }
}
