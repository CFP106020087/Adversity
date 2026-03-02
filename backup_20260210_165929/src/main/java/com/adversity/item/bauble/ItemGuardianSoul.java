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
 * 守护者之魂 - 反制装备封印系统
 *
 * 反制词条：枷锁、褫夺、黑棺、祛魔
 * 机制：减少封印几率和时间
 */
public class ItemGuardianSoul extends AbstractWardBauble {

    public ItemGuardianSoul() {
        super("guardian_soul", "equipment_seal");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;  // 圣所内完全免疫封印
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f;  // 圣所外减少50%封印几率/时间
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn,
            List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.guardian_soul.desc1"));
        tooltip.add(I18n.format("adversity.bauble.guardian_soul.desc2"));
    }
}
