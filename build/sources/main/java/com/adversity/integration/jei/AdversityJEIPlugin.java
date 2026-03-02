package com.adversity.integration.jei;

import com.adversity.Adversity;
import com.adversity.block.BlockRegistry;
import com.adversity.sanctuary.ritual.Rite;
import com.adversity.sanctuary.ritual.RitualManager;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 集成插件
 * 显示圣所仪式配方
 */
@JEIPlugin
public class AdversityJEIPlugin implements IModPlugin {

    public static final String RITUAL_UID = Adversity.MODID + ":ritual";

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        // 注册仪式配方类别
        registry.addRecipeCategories(
                new RitualRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        Adversity.LOGGER.info("JEI: Registered ritual recipe category");
    }

    @Override
    public void register(IModRegistry registry) {
        // 注册仪式配方
        List<RitualRecipeWrapper> recipes = new ArrayList<>();
        for (Rite rite : RitualManager.getAllRites()) {
            recipes.add(new RitualRecipeWrapper(rite));
        }
        registry.addRecipes(recipes, RITUAL_UID);

        // 设置催化剂（圣所祭坛）
        if (BlockRegistry.SANCTUARY_ALTAR != null) {
            registry.addRecipeCatalyst(new ItemStack(BlockRegistry.SANCTUARY_ALTAR), RITUAL_UID);
        }

        Adversity.LOGGER.info("JEI: Registered {} ritual recipes", recipes.size());
    }
}
