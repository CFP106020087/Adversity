package com.adversity.item.bauble;

import com.adversity.Adversity;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 附魔守护者 - 防止消魔(Disenchant)效果
 * 
 * 圣所内：完全免疫消魔
 * 圣所外：50%阻止消魔
 * 特殊效果：被触发时恢复一定经验值
 */
public class ItemEnchantGuardian extends AbstractWardBauble {

    private static final int XP_RESTORE = 50; // 恢复的经验点数

    public ItemEnchantGuardian() {
        super("enchant_guardian", "disenchant");
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
    public void onEffectTriggered(EntityPlayer player, @Nullable EntityLivingBase source, boolean blocked) {
        if (blocked) {
            // 恢复经验
            player.addExperience(XP_RESTORE);

            // 音效
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.5f);

            Adversity.LOGGER.debug("Enchant Guardian restored {} XP to player", XP_RESTORE);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.enchant_guardian.special", XP_RESTORE));
    }
}
