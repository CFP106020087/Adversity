package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 勇气护符 - 防止恐惧(Horror)击退
 * 
 * 圣所内：完全免疫恐惧击退
 * 圣所外：50%减少恐惧效果
 * Trade-off：受到恐惧攻击时+30%伤害（风险与回报）
 */
public class ItemCourageCharm extends AbstractWardBauble {

    public static final float DAMAGE_INCREASE = 0.3f; // 30%增伤

    public ItemCourageCharm() {
        super("courage_charm", "horror");
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
        tooltip.add(I18n.format("adversity.bauble.courage_charm.tradeoff", 
            (int)(DAMAGE_INCREASE * 100)));
    }
}
