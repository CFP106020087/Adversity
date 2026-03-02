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
 * 锚定之心 - 反制闪避/复活系统
 *
 * 反制词条：传送、蜕皮
 * 机制：阻止敌人闪避和复活
 */
public class ItemAnchorHeart extends AbstractWardBauble {

    public ItemAnchorHeart() {
        super("anchor_heart", "evasion");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;  // 圣所内完全阻止闪避/复活
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f;  // 圣所外50%阻止
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn,
            List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.anchor_heart.desc1"));
        tooltip.add(I18n.format("adversity.bauble.anchor_heart.desc2"));
    }
}
