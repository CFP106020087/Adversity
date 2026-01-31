package com.adversity.sanctuary.block;

import com.adversity.Adversity;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/**
 * 圣所石 - 不可破坏的结构方块
 */
public class BlockSanctuaryStone extends Block {

    public BlockSanctuaryStone() {
        super(Material.ROCK);
        setRegistryName(Adversity.MODID, "sanctuary_stone");
        setTranslationKey(Adversity.MODID + ".sanctuary_stone");
        setBlockUnbreakable(); // 不可破坏
        setResistance(6000000.0F); // 抗爆
        setLightLevel(0.2f); // 微光
        setCreativeTab(null); // 不在创造模式显示
    }
}
