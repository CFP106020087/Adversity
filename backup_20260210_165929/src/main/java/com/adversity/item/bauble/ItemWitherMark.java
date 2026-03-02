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
 * 凋零印记 - 反制吸血/回复系统
 *
 * 反制词条：吸血、再生
 * 机制：减少敌人的回复效果
 */
public class ItemWitherMark extends AbstractWardBauble {

    public ItemWitherMark() {
        super("wither_mark", "healing");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;  // 圣所内完全禁止回复
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f;  // 圣所外减少50%回复
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn,
            List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.wither_mark.desc1"));
        tooltip.add(I18n.format("adversity.bauble.wither_mark.desc2"));
    }
}
