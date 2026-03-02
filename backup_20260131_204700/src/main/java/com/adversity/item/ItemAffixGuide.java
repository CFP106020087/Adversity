package com.adversity.item;

import com.adversity.Adversity;
import com.adversity.client.gui.GuiAffixGuide;
import com.adversity.client.gui.GuiAdversityGuide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 词条指南 - 模组手册物品
 * 右键打开词条指南GUI
 * Shift+右键打开综合生存指南
 */
public class ItemAffixGuide extends Item {

    public ItemAffixGuide() {
        setRegistryName(Adversity.MODID, "affix_guide");
        setTranslationKey(Adversity.MODID + ".affix_guide");
        setCreativeTab(AdversityTab.INSTANCE);
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);

        if (worldIn.isRemote) {
            // 客户端打开GUI
            if (playerIn.isSneaking()) {
                // Shift+右键: 打开综合生存指南
                openSurvivalGuide();
            } else {
                // 普通右键: 打开词条指南
                openAffixGuide();
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @SideOnly(Side.CLIENT)
    private void openAffixGuide() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiAffixGuide());
    }

    @SideOnly(Side.CLIENT)
    private void openSurvivalGuide() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiAdversityGuide());
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("\u00a77" + I18n.format("item.adversity.affix_guide.tooltip"));
        tooltip.add("\u00a78" + I18n.format("item.adversity.affix_guide.hint"));
        tooltip.add("\u00a76" + I18n.format("item.adversity.affix_guide.hint_shift"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true;  // 附魔光效表示这是重要物品
    }
}
