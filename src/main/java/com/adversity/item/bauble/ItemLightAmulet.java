package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 轻装护身符 - 对抗强欲(Avarice)和贪婪(Greed)词条
 * 
 * 强欲：背包槽位占用越多受伤越多
 * 贪婪：饰品越多受伤越多
 * 此饰品：降低这些惩罚
 * 
 * 圣所内：完全免疫惩罚
 * 圣所外：减少50%惩罚
 */
public class ItemLightAmulet extends AbstractWardBauble {

    public ItemLightAmulet() {
        super("light_amulet", "avarice"); // 主要对抗强欲
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f;
    }

    /**
     * 额外检查是否也对抗贪婪词条
     */
    public boolean countersGreed() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.light_amulet.effect"));
        tooltip.add(I18n.format("adversity.bauble.light_amulet.also_counters"));
    }
}
