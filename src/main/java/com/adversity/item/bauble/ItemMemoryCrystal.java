package com.adversity.item.bauble;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 记忆水晶 - 对抗遗忘(Oblivion)词条
 * 
 * 遗忘词条：攻击扣除玩家经验值
 * 此饰品：保护经验不被扣除
 * 
 * 圣所内：完全保护经验
 * 圣所外：保护50%经验
 */
public class ItemMemoryCrystal extends AbstractWardBauble {

    public ItemMemoryCrystal() {
        super("memory_crystal", "oblivion");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f; // 完全保护
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f; // 50%保护
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.memory_crystal.effect"));
    }
}
