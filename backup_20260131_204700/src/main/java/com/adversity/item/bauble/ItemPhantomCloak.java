package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 幽灵斗篷 - 隐匿专精
 * 
 * 通用效果:
 * - 潜行时完全隐身
 * - 背刺伤害+40%
 * - 仇恨降低50%
 * 
 * 词条抵抗: 分裂(不触发)、反射(免疫)、英雄(不被发现)
 */
public class ItemPhantomCloak extends AbstractWardBauble {

    public ItemPhantomCloak() {
        super("phantom_cloak", "split");
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
        tooltip.add(I18n.format("adversity.bauble.phantom_cloak.effect1")); // 潜行隐身
        tooltip.add(I18n.format("adversity.bauble.phantom_cloak.effect2")); // 背刺伤害
        tooltip.add(I18n.format("adversity.bauble.phantom_cloak.effect3")); // 仇恨降低
    }
}
