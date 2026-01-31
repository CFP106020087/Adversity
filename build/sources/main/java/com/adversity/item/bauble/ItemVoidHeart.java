package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 虚空之心 - 末地专精
 * 
 * 通用效果:
 * - 末影珍珠无伤害
 * - 末地攻击+25%/移速+20%
 * - 虚空边缘3秒无敌(5分CD)
 * 
 * 词条抵抗: 传送、空间扭曲、湮灭
 */
public class ItemVoidHeart extends AbstractWardBauble {

    public ItemVoidHeart() {
        super("void_heart", "teleport");
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
        tooltip.add(I18n.format("adversity.bauble.void_heart.effect1")); // 珍珠无伤
        tooltip.add(I18n.format("adversity.bauble.void_heart.effect2")); // 末地增强
        tooltip.add(I18n.format("adversity.bauble.void_heart.effect3")); // 虚空保护
    }
}
