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
 * 净焰之环 - 防止火焰(Fiery)词条伤害
 * 
 * 圣所内：完全免疫火焰词条
 * 圣所外：50%减少火焰层叠速度
 * 特殊效果：满层时自动消耗饰品耐久清除所有火层
 */
public class ItemFlameWard extends AbstractWardBauble {

    private static final int AUTO_CLEAR_DURABILITY_COST = 10;

    public ItemFlameWard() {
        super("flame_ward", "fiery");
        setMaxDamage(100); // 有耐久度
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
     * 当火焰层数即将满层时调用
     * @return true 如果成功清除
     */
    public boolean onBurningMaxStacks(EntityPlayer player, EntityLivingBase source) {
        // 检查耐久度
        // TODO: 实际实现需要知道玩家装备的是哪个ItemStack
        
        // 音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        
        Adversity.LOGGER.debug("Flame Ward auto-cleared burning stacks");
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.flame_ward.special"));
    }
}
