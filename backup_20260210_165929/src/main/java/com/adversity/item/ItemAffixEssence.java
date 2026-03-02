package com.adversity.item;

import com.adversity.Adversity;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 词条残渣 - 基础材料
 * 由 T1-T4 精英怪物掉落
 * 用于合成基础压制物品
 */
public class ItemAffixEssence extends Item {

    public ItemAffixEssence() {
        setRegistryName(Adversity.MODID, "affix_essence");
        setTranslationKey(Adversity.MODID + ".affix_essence");
        setCreativeTab(AdversityTab.INSTANCE);
        setMaxStackSize(64);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("\u00a77" + I18n.format("item.adversity.affix_essence.tooltip"));
        tooltip.add("\u00a78" + I18n.format("item.adversity.affix_essence.source"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return false;
    }
}
