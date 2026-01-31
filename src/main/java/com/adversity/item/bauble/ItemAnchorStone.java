package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 锚石项链 - 防止重力(Gravity)拉扯
 * 
 * 圣所内：完全免疫重力拉扯
 * 圣所外：50%减少拉扯力度
 * Trade-off：-10%移动速度
 */
public class ItemAnchorStone extends AbstractWardBauble {

    public static final float MOVEMENT_PENALTY = 0.1f; // 10%减速

    public ItemAnchorStone() {
        super("anchor_stone", "gravity");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.anchor_stone.tradeoff", 
            (int)(MOVEMENT_PENALTY * 100)));
    }
}
