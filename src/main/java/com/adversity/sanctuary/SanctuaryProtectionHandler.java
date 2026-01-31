package com.adversity.sanctuary;

import com.adversity.block.BlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 圣所保护处理器
 * 
 * 阻止玩家破坏天然圣所结构中的方块
 */
public class SanctuaryProtectionHandler {

    /** 圣所保护半径 */
    private static final int PROTECTION_RADIUS = 4;

    /**
     * 阻止破坏圣所祭坛方块
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        Block block = event.getState().getBlock();

        // 直接保护圣所祭坛
        if (block == BlockRegistry.SANCTUARY_ALTAR) {
            event.setCanceled(true);
            return;
        }

        // 检查是否在某个天然圣所的保护范围内
        if (isInNaturalSanctuaryStructure(world, pos)) {
            // 石英系方块在圣所结构内不可破坏
            if (isProtectedBlock(block)) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 阻止爆炸破坏圣所结构
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExplosion(net.minecraftforge.event.world.ExplosionEvent.Detonate event) {
        World world = event.getWorld();

        event.getAffectedBlocks().removeIf(pos -> {
            Block block = world.getBlockState(pos).getBlock();

            // 保护圣所祭坛
            if (block == BlockRegistry.SANCTUARY_ALTAR) {
                return true;
            }

            // 保护圣所结构内的石英块
            if (isInNaturalSanctuaryStructure(world, pos) && isProtectedBlock(block)) {
                return true;
            }

            return false;
        });
    }

    /**
     * 检查位置是否在天然圣所结构内
     */
    private boolean isInNaturalSanctuaryStructure(World world, BlockPos pos) {
        // 搜索附近是否有圣所祭坛
        for (int dx = -PROTECTION_RADIUS; dx <= PROTECTION_RADIUS; dx++) {
            for (int dy = -2; dy <= 5; dy++) {
                for (int dz = -PROTECTION_RADIUS; dz <= PROTECTION_RADIUS; dz++) {
                    BlockPos checkPos = pos.add(dx, dy, dz);
                    Block block = world.getBlockState(checkPos).getBlock();
                    if (block == BlockRegistry.SANCTUARY_ALTAR) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查方块是否是应被保护的圣所结构方块
     */
    private boolean isProtectedBlock(Block block) {
        return block == Blocks.QUARTZ_BLOCK ||
                block == Blocks.QUARTZ_STAIRS ||
                block == Blocks.STONE_SLAB || // Includes quartz slab via metadata
                block == Blocks.SEA_LANTERN ||
                block == BlockRegistry.SANCTUARY_ALTAR;
    }
}
