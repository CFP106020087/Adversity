package com.adversity.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Adversity ASM Loading Plugin
 * Used to inject hooks into Container.slotClick for sealed slot blocking
 */
@IFMLLoadingPlugin.Name("AdversityCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"com.adversity.asm"})
@IFMLLoadingPlugin.SortingIndex(1001)
public class AdversityLoadingPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                "com.adversity.asm.ContainerTransformer",
                "com.adversity.asm.GuiContainerTransformer" // Client-side slot rendering block
        };
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
