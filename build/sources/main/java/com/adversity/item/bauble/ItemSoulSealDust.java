package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 封魂之尘 - 对抗飞升(Ascension)词条
 * 
 * 飞升词条：死亡时30%+几率复活并升阶
 * 此饰品：降低复活几率
 * 
 * 圣所内：完全阻止复活
 * 圣所外：降低50%复活几率
 */
public class ItemSoulSealDust extends AbstractWardBauble {

    public ItemSoulSealDust() {
        super("soul_seal_dust", "ascension");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f; // 完全阻止
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f; // 50%
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.soul_seal_dust.effect"));
    }
}
