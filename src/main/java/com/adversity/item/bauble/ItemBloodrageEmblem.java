package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 血怒徽章 - 吸血专精
 * 
 * 通用效果:
 * - 攻击吸血5%
 * - 血量越低攻速越快(最高+30%)
 * - 击杀回复半心
 * 
 * 词条抵抗: 吸血、渴血、死神
 */
public class ItemBloodrageEmblem extends AbstractWardBauble {

    public ItemBloodrageEmblem() {
        super("bloodrage_emblem", "lifesteal");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.6f;
    }

    @Override
    public void onEffectTriggered(EntityPlayer player, @Nullable EntityLivingBase source, boolean blocked) {
        // 效果由BaubleEffectHandler处理
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.bloodrage_emblem.effect1")); // 吸血5%
        tooltip.add(I18n.format("adversity.bauble.bloodrage_emblem.effect2")); // 低血加攻速
        tooltip.add(I18n.format("adversity.bauble.bloodrage_emblem.effect3")); // 击杀回血
    }
}
