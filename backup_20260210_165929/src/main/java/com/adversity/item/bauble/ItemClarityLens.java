package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 澄明之眼 - 防止致盲(Blinding)效果
 * 
 * 圣所内：完全免疫致盲
 * 圣所外：50%减少致盲持续时间
 * Trade-off：周围5格内怪物获得你的视野（可能更精准追踪你）
 */
public class ItemClarityLens extends AbstractWardBauble {

    public static final int VISION_SHARE_RADIUS = 5;

    public ItemClarityLens() {
        super("clarity_lens", "blinding");
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
        tooltip.add(I18n.format("adversity.bauble.clarity_lens.tradeoff", VISION_SHARE_RADIUS));
    }
}
