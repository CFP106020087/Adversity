package com.adversity.item.bauble;

import com.adversity.Adversity;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 净化徽章 - 反制叠层系统
 *
 * 反制词条：烈焰、冰霜、凋零、致盲、恐惧、吸血、荆刺
 * 机制：减少所有负面叠层的增长速度
 */
public class ItemPurifyBadge extends AbstractWardBauble {

    public ItemPurifyBadge() {
        super("purify_badge", "stack_system");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;  // 圣所内完全免疫叠层增长
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f;  // 圣所外减少50%叠层增长
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn,
            List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.purify_badge.desc1"));
        tooltip.add(I18n.format("adversity.bauble.purify_badge.desc2"));
    }
}
