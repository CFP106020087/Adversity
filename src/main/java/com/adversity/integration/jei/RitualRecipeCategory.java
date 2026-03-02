package com.adversity.integration.jei;

import com.adversity.Adversity;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

/**
 * 仪式配方JEI类别
 */
public class RitualRecipeCategory implements IRecipeCategory<RitualRecipeWrapper> {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    private final IDrawable background;
    private final IDrawable icon;
    private final String localizedName;

    public RitualRecipeCategory(IGuiHelper guiHelper) {
        // 背景纹理 (使用自定义或默认)
        ResourceLocation bgLocation = new ResourceLocation(Adversity.MODID, "textures/gui/jei_ritual.png");
        this.background = guiHelper.createDrawable(bgLocation, 0, 0, 116, 54);
        
        // 图标 (使用熵能核心)
        this.icon = guiHelper.createDrawable(
            new ResourceLocation(Adversity.MODID, "textures/items/entropy_core.png"),
            0, 0, 16, 16, 16, 16
        );
        
        this.localizedName = I18n.format("jei.adversity.ritual.title");
    }

    @Override
    public String getUid() {
        return AdversityJEIPlugin.RITUAL_UID;
    }

    @Override
    public String getTitle() {
        return localizedName;
    }

    @Override
    public String getModName() {
        return Adversity.NAME;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, RitualRecipeWrapper recipeWrapper, IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();

        // 输入槽位 (左侧)
        itemStacks.init(INPUT_SLOT, true, 8, 18);
        // 输出槽位 (右侧)
        itemStacks.init(OUTPUT_SLOT, false, 90, 18);

        // 设置物品
        itemStacks.set(ingredients);
    }
}
