package com.adversity.item;

import com.adversity.Adversity;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 词条水晶 - 中级材料
 * 由 T5-T7 精英怪物掉落
 * 用于合成中级压制物品和饰品
 */
public class ItemAffixCrystal extends Item {

    public ItemAffixCrystal() {
        setRegistryName(Adversity.MODID, "affix_crystal");
        setUnlocalizedName(Adversity.MODID + ".affix_crystal");
        setCreativeTab(AdversityTab.INSTANCE);
        setMaxStackSize(64);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("\u00a7b" + I18n.format("item.adversity.affix_crystal.tooltip"));
        tooltip.add("\u00a78" + I18n.format("item.adversity.affix_crystal.source"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true; // 闪光效果
    }
}
