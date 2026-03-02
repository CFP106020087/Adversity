package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 免疫徽章 - 对抗破伤风(Tetanus)词条
 * 
 * 破伤风词条：累计5次命中后扣50%当前生命
 * 此饰品：降低命中计数速度，延长触发间隔
 * 
 * 圣所内：命中计数减半（10次才触发）
 * 圣所外：命中计数减30%（约7次触发）
 */
public class ItemImmunityBadge extends AbstractWardBauble {

    public ItemImmunityBadge() {
        super("immunity_badge", "tetanus");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f; // 计数减半
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.6f; // 计数减30%
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.immunity_badge.effect"));
    }
}
