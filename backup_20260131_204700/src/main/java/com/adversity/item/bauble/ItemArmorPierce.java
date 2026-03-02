package com.adversity.item.bauble;

import com.adversity.Adversity;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 破甲符文 - 反制护甲/减伤系统
 *
 * 反制词条：铁壁、硬化、英雄、神明、外神
 * 机制：穿透敌人额外护甲和减伤
 */
public class ItemArmorPierce extends AbstractWardBauble {

    public ItemArmorPierce() {
        super("armor_pierce", "armor_reduction");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;  // 圣所内完全穿透
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f;  // 圣所外50%穿透
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn,
            List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.armor_pierce.desc1"));
        tooltip.add(I18n.format("adversity.bauble.armor_pierce.desc2"));
    }
}
