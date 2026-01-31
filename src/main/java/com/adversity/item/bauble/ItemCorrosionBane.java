package com.adversity.item.bauble;

import com.adversity.Adversity;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 腐蚀克星 - 防止凋零(Withering)词条伤害
 * 
 * 圣所内：完全免疫凋零词条
 * 圣所外：50%减少腐蚀层叠速度
 * 特殊效果：满层时反弹50%腐蚀伤害给攻击者
 */
public class ItemCorrosionBane extends AbstractWardBauble {

    private static final float REFLECT_DAMAGE_PERCENT = 0.5f;

    public ItemCorrosionBane() {
        super("corrosion_bane", "withering");
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
     * 当腐蚀层数满时调用，反弹伤害
     * @param player 玩家
     * @param source 攻击者
     * @param damageAmount 原本要造成的伤害
     */
    public void onCorrosionBurst(EntityPlayer player, @Nullable EntityLivingBase source, float damageAmount) {
        if (source != null && source.isEntityAlive()) {
            float reflectDamage = damageAmount * REFLECT_DAMAGE_PERCENT;
            source.attackEntityFrom(DamageSource.causePlayerDamage(player), reflectDamage);
            
            // 音效
            player.world.playSound(null, source.posX, source.posY, source.posZ,
                SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.PLAYERS, 1.0f, 0.8f);
            
            Adversity.LOGGER.debug("Corrosion Bane reflected {} damage to attacker", reflectDamage);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.corrosion_bane.special", 
            (int)(REFLECT_DAMAGE_PERCENT * 100)));
    }
}
