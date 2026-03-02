package com.adversity.integration.jei;

import com.adversity.sanctuary.ritual.Rite;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import java.util.Collections;

/**
 * 仪式配方包装器
 */
public class RitualRecipeWrapper implements IRecipeWrapper {

    private final Rite rite;

    public RitualRecipeWrapper(Rite rite) {
        this.rite = rite;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        // 输入物品
        ingredients.setInput(VanillaTypes.ITEM, rite.getInput());
        
        // 输出物品
        if (!rite.getOutput().isEmpty()) {
            ingredients.setOutput(VanillaTypes.ITEM, rite.getOutput());
        }
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        FontRenderer font = minecraft.fontRenderer;
        
        // 显示熵能消耗
        String costText = I18n.format("jei.adversity.ritual.cost", rite.getEntropyCost());
        font.drawString(costText, 35, 5, 0x333333);
        
        // 显示前置阶段要求
        if (rite.getRequiredStage() != null) {
            String reqText = I18n.format("jei.adversity.ritual.requires", rite.getRequiredStage());
            font.drawString(reqText, 35, 40, 0x666666);
        }
        
        // 显示解锁阶段奖励
        if (rite.getRewardStage() != null) {
            String rewardText = I18n.format("jei.adversity.ritual.unlocks", rite.getRewardStage());
            font.drawString(rewardText, 35, 40, 0x228B22);
        }
    }

    public Rite getRite() {
        return rite;
    }
}
