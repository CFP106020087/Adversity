package com.adversity.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import fermiumbooter.FermiumRegistryAPI;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Adversity ASM Loading Plugin
 * Used to inject hooks into Container.slotClick for sealed slot blocking
 * Also registers FermiumBooter mixins for enchantment/crafting restrictions
 */
@IFMLLoadingPlugin.Name("AdversityCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({ "com.adversity.asm", "com.adversity.mixin" })
@IFMLLoadingPlugin.SortingIndex(1001)
public class AdversityLoadingPlugin implements IFMLLoadingPlugin {

    static {
        System.out.println("[Adversity] AdversityLoadingPlugin static init");
        try {
            // Early mixin - 原版 Minecraft 类 (ContainerEnchantment 等)
            FermiumRegistryAPI.enqueueMixin(false, "adversity.early.mixins.json");
            System.out.println("[Adversity] Early mixins queued via FermiumBooter");

            // Late mixin - 第三方 Mod 类 (JEI RecipeRegistry 等)
            FermiumRegistryAPI.enqueueMixin(true, "adversity.late.mixins.json");
            System.out.println("[Adversity] Late mixins queued via FermiumBooter");
        } catch (Throwable e) {
            System.err.println("[Adversity] FermiumBooter registration failed: " + e);
            e.printStackTrace();
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {}; // Sealed slot blocking handled by MixinSlot
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
