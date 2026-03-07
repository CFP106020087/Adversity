package com.adversity.item;

import com.adversity.Adversity;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/**
 * 創造模式附魔門控配置工具
 *
 * 右鍵打開附魔門控 GUI，可選擇黑白名單模式、
 * 要求階段、和要門控的附魔列表，然後保存為 JSON + ZS。
 */
public class ItemEnchantGatingTool extends Item {

    public ItemEnchantGatingTool() {
        setRegistryName(Adversity.MODID, "enchant_gating_tool");
        setTranslationKey(Adversity.MODID + ".enchant_gating_tool");
        setCreativeTab(CreativeTabs.TOOLS);
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        if (worldIn.isRemote && playerIn.isCreative()) {
            // 客戶端打開 GUI
            com.adversity.client.gui.GuiEnchantGatingTool.open();
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
    }
}
