package com.adversity.item.bauble;

import com.adversity.Adversity;
import com.adversity.item.AdversityTab;
import com.adversity.sanctuary.SanctuaryManager;
import com.adversity.sanctuary.SanctuaryZone;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 抽象守护饰品基类
 * 所有反制词条的饰品继承此类
 */
public abstract class AbstractWardBauble extends Item {

    protected final String wardType;

    public AbstractWardBauble(String name, String wardType) {
        this.wardType = wardType;
        setRegistryName(Adversity.MODID, name);
        setTranslationKey(Adversity.MODID + "." + name);
        setMaxStackSize(1);
        setCreativeTab(AdversityTab.INSTANCE);
    }

    /**
     * 获取圣所内的效果强度
     * 
     * @return 1.0 = 完全免疫
     */
    protected abstract float getInSanctuaryStrength();

    /**
     * 获取圣所外的效果强度
     * 
     * @return 0.5 = 50%效果
     */
    protected abstract float getOutsideSanctuaryStrength();

    /**
     * 获取当前效果强度
     */
    public float getEffectStrength(EntityPlayer player) {
        SanctuaryZone zone = SanctuaryManager.getPlayerSanctuary(player);
        if (zone != null && zone.isActive()) {
            // 在圣所内，效果受圣所类型影响
            return getInSanctuaryStrength() * zone.getBaubleEffectMultiplier();
        } else {
            // 圣所外使用基础外部效果
            return getOutsideSanctuaryStrength();
        }
    }

    /**
     * 检查是否完全免疫（效果强度>=1.0）
     */
    public boolean isFullyImmune(EntityPlayer player) {
        return getEffectStrength(player) >= 1.0f;
    }

    /**
     * 获取减少百分比 (0.0-1.0)
     */
    public float getReductionPercent(EntityPlayer player) {
        float strength = getEffectStrength(player);
        return Math.min(1.0f, strength);
    }

    /**
     * 触发特殊效果（子类可覆盖）
     * 
     * @param player  玩家
     * @param source  触发源（通常是攻击者）
     * @param blocked 是否完全阻止了效果
     */
    public void onEffectTriggered(EntityPlayer player, @Nullable EntityLivingBase source, boolean blocked) {
        // 默认不做任何事，子类可覆盖
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn,
            List<String> tooltip, ITooltipFlag flagIn) {
        // 显示守护类型
        tooltip.add(I18n.format("adversity.bauble.ward_type",
                I18n.format("adversity.ward." + wardType)));

        // 圣所内效果
        int inStrength = (int) (getInSanctuaryStrength() * 100);
        tooltip.add(I18n.format("adversity.bauble.in_sanctuary", inStrength + "%"));

        // 圣所外效果
        int outStrength = (int) (getOutsideSanctuaryStrength() * 100);
        tooltip.add(I18n.format("adversity.bauble.outside_sanctuary", outStrength + "%"));

        // 特殊效果描述（子类可覆盖）
        addSpecialDescription(tooltip);
    }

    /**
     * 添加特殊效果描述（子类覆盖）
     */
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        // 默认无
    }
}
