package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 圣盾勋章 - 团队专精
 * 
 * 通用效果:
 * - 8格内队友受伤-15%
 * - 被攻击时队友获知
 * - 复活队友后双方+20%攻击30秒
 * 
 * 词条抵抗: 巨人杀手、破盾、强欲
 */
public class ItemAegisMedal extends AbstractWardBauble {

    public ItemAegisMedal() {
        super("aegis_medal", "giantslayer");
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
        tooltip.add(I18n.format("adversity.bauble.aegis_medal.effect1")); // 队友保护
        tooltip.add(I18n.format("adversity.bauble.aegis_medal.effect2")); // 攻击警告
        tooltip.add(I18n.format("adversity.bauble.aegis_medal.effect3")); // 复活增益
    }
}
