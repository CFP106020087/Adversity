package com.adversity.item.bauble;

import com.adversity.Adversity;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 真伤水晶 - 反制伤害转换系统
 *
 * 反制词条：反射、适应
 * 机制：伤害无法被转换或吸收
 */
public class ItemTrueCrystal extends AbstractWardBauble {

    public ItemTrueCrystal() {
        super("true_crystal", "damage_conversion");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;  // 圣所内完全锁定真实伤害
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f;  // 圣所外50%锁定
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn,
            List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.true_crystal.desc1"));
        tooltip.add(I18n.format("adversity.bauble.true_crystal.desc2"));
    }
}
