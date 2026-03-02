package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 雷霆指环 - 雷电专精
 * 
 * 通用效果:
 * - 攻击10%触发闪电
 * - 雨天攻击+20%
 * - 免疫闪电伤害
 * 
 * 词条抵抗: 神圣、飞升、外神
 */
public class ItemThunderRing extends AbstractWardBauble {

    public ItemThunderRing() {
        super("thunder_ring", "holy");
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
        tooltip.add(I18n.format("adversity.bauble.thunder_ring.effect1")); // 闪电触发
        tooltip.add(I18n.format("adversity.bauble.thunder_ring.effect2")); // 雨天增强
        tooltip.add(I18n.format("adversity.bauble.thunder_ring.effect3")); // 闪电免疫
    }
}
