package com.adversity.integration.crafttweaker;

import com.adversity.sanctuary.ritual.Rite;
import com.adversity.sanctuary.ritual.RitualManager;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.adversity.Sanctuary")
public class SanctuaryCT {

    @ZenMethod
    public static void addRitual(String name, IItemStack input, IItemStack output, int entropyCost, String requiredStage, String rewardStage) {
        RitualManager.registerRite(
            name,
            CraftTweakerMC.getItemStack(input),
            CraftTweakerMC.getItemStack(output),
            entropyCost,
            requiredStage,
            rewardStage
        );
        // CraftTweakerAPI.logInfo("Registered Adversity Ritual: " + name); 
    }
}
