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
 * 结界护符 - 反制范围光环系统
 *
 * 反制词条：烈焰(光环)、致盲、恐惧、引力、魔免、共生
 * 机制：减少所有范围光环效果
 */
public class ItemBarrierWard extends AbstractWardBauble {

    public ItemBarrierWard() {
        super("barrier_ward", "aura");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;  // 圣所内完全免疫光环
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f;  // 圣所外减少50%光环效果
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn,
            List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.barrier_ward.desc1"));
        tooltip.add(I18n.format("adversity.bauble.barrier_ward.desc2"));
    }
}
