package com.adversity.item.bauble;

import com.adversity.Adversity;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 灵魂锁链 - 防止装备被枷锁(Shackle)封印
 * 
 * 圣所内：完全免疫枷锁
 * 圣所外：50%几率阻止
 * 特殊效果：成功阻止时，反向眩晕攻击者2秒
 */
public class ItemSoulChain extends AbstractWardBauble {

    public ItemSoulChain() {
        super("soul_chain", "shackle");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f; // 完全免疫
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f; // 50%阻止率
    }

    @Override
    public void onEffectTriggered(EntityPlayer player, @Nullable EntityLivingBase source, boolean blocked) {
        if (blocked && source != null) {
            // 成功阻止时，反向眩晕攻击者
            source.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 5)); // 2秒极度减速
            source.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 40, 0)); // 2秒失明
            
            // 音效
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.5f);
            
            Adversity.LOGGER.debug("Soul Chain reflected Shackle effect back to attacker");
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.soul_chain.special"));
    }
}
