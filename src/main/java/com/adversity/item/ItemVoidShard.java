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
 * 虚空碎片 - 高级材料
 * 由 T8-T10 精英怪物掉落
 * 用于合成高级装备和永久封印物品
 */
public class ItemVoidShard extends Item {

    public ItemVoidShard() {
        setRegistryName(Adversity.MODID, "void_shard");
        setTranslationKey(Adversity.MODID + ".void_shard");
        setCreativeTab(AdversityTab.INSTANCE);
        setMaxStackSize(64);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("\u00a7d" + I18n.format("item.adversity.void_shard.tooltip"));
        tooltip.add("\u00a78" + I18n.format("item.adversity.void_shard.source"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
