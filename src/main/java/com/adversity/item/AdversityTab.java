package com.adversity.item;

import com.adversity.Adversity;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Adversity 模组的创造模式物品栏
 */
public class AdversityTab extends CreativeTabs {

    public static final AdversityTab INSTANCE = new AdversityTab();

    private AdversityTab() {
        super(Adversity.MODID);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack createIcon() {
        return new ItemStack(ItemRegistry.AFFIX_ESSENCE);
    }

    @Override
    public String getTranslationKey() {
        return "itemGroup." + Adversity.MODID;
    }
}
