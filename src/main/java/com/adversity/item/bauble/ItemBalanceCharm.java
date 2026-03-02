package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 平衡护符 - 对抗逆转(Reversal)词条
 * 
 * 逆转词条使低伤害变高、高伤害变低
 * 此饰品使伤害计算正常化
 * 
 * 圣所内：完全无效化逆转效果
 * 圣所外：50%减少逆转影响
 */
public class ItemBalanceCharm extends AbstractWardBauble {

    public ItemBalanceCharm() {
        super("balance_charm", "reversal");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f; // 完全免疫
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f; // 50%效果
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.balance_charm.effect"));
    }
}
