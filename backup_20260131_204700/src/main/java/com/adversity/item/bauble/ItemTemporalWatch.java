package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 时光怀表 - 时间专精
 * 
 * 通用效果:
 * - 负面效果持续时间-40%
 * - 药水效果持续+30%
 * - 饥饿消耗-25%
 * 
 * 词条抵抗: 迅捷、英雄、遗忘
 */
public class ItemTemporalWatch extends AbstractWardBauble {

    public ItemTemporalWatch() {
        super("temporal_watch", "swift");
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
        tooltip.add(I18n.format("adversity.bauble.temporal_watch.effect1")); // debuff时间减少
        tooltip.add(I18n.format("adversity.bauble.temporal_watch.effect2")); // buff时间延长
        tooltip.add(I18n.format("adversity.bauble.temporal_watch.effect3")); // 饥饿减少
    }
}
