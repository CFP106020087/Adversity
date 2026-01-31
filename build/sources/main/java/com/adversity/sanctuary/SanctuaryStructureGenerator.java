package com.adversity.sanctuary;

import com.adversity.Adversity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 圣所结构生成器
 * 根据等级生成不同规模的圣所建筑结构
 */
public class SanctuaryStructureGenerator {

    // 主要建筑材料
    private static final IBlockState STONE_BRICK = Blocks.STONEBRICK.getDefaultState();
    private static final IBlockState CHISELED_STONE = Blocks.STONEBRICK.getStateFromMeta(3); // 錾制石砖
    private static final IBlockState CRACKED_STONE = Blocks.STONEBRICK.getStateFromMeta(2); // 裂石砖
    private static final IBlockState QUARTZ_BLOCK = Blocks.QUARTZ_BLOCK.getDefaultState();
    private static final IBlockState QUARTZ_PILLAR = Blocks.QUARTZ_BLOCK.getStateFromMeta(2);
    private static final IBlockState GLOWSTONE = Blocks.GLOWSTONE.getDefaultState();
    private static final IBlockState SEA_LANTERN = Blocks.SEA_LANTERN.getDefaultState();
    private static final IBlockState GOLD_BLOCK = Blocks.GOLD_BLOCK.getDefaultState();
    private static final IBlockState DIAMOND_BLOCK = Blocks.DIAMOND_BLOCK.getDefaultState();
    private static final IBlockState EMERALD_BLOCK = Blocks.EMERALD_BLOCK.getDefaultState();

    /**
     * 根据等级生成或升级圣所结构
     * 
     * @param world 世界
     * @param altarPos 祭坛位置
     * @param newTier 新等级 (1-5)
     */
    public static void generateStructure(World world, BlockPos altarPos, int newTier) {
        if (world.isRemote) return;
        
        Adversity.LOGGER.info("Generating sanctuary structure at {} for tier {}", altarPos, newTier);
        
        switch (newTier) {
            case 1:
                // Tier 1: 基础平台 (3x3)
                generateTier1(world, altarPos);
                break;
            case 2:
                // Tier 2: 扩展平台 (5x5) + 4根柱子
                generateTier2(world, altarPos);
                break;
            case 3:
                // Tier 3: 中型神殿 (7x7) + 8根柱子 + 光源
                generateTier3(world, altarPos);
                break;
            case 4:
                // Tier 4: 大型神殿 (9x9) + 外墙 + 入口拱门
                generateTier4(world, altarPos);
                break;
            case 5:
                // Tier 5: 完整圣殿 (11x11) + 穹顶 + 宝石装饰
                generateTier5(world, altarPos);
                break;
        }
    }

    /**
     * Tier 1: 基础石砖平台 (3x3)
     */
    private static void generateTier1(World world, BlockPos altarPos) {
        BlockPos floorCenter = altarPos.down();
        
        // 3x3 地板
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                safeSetBlock(world, floorCenter.add(x, 0, z), STONE_BRICK);
            }
        }
        
        // 四角放置錾制石砖
        safeSetBlock(world, floorCenter.add(-1, 0, -1), CHISELED_STONE);
        safeSetBlock(world, floorCenter.add(1, 0, -1), CHISELED_STONE);
        safeSetBlock(world, floorCenter.add(-1, 0, 1), CHISELED_STONE);
        safeSetBlock(world, floorCenter.add(1, 0, 1), CHISELED_STONE);
    }

    /**
     * Tier 2: 扩展平台 (5x5) + 4根石柱
     */
    private static void generateTier2(World world, BlockPos altarPos) {
        BlockPos floorCenter = altarPos.down();
        
        // 5x5 地板
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                safeSetBlock(world, floorCenter.add(x, 0, z), STONE_BRICK);
            }
        }
        
        // 四角錾制石砖
        safeSetBlock(world, floorCenter.add(-2, 0, -2), CHISELED_STONE);
        safeSetBlock(world, floorCenter.add(2, 0, -2), CHISELED_STONE);
        safeSetBlock(world, floorCenter.add(-2, 0, 2), CHISELED_STONE);
        safeSetBlock(world, floorCenter.add(2, 0, 2), CHISELED_STONE);
        
        // 4根柱子 (高3格)
        int[][] pillarOffsets = {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}};
        for (int[] offset : pillarOffsets) {
            for (int y = 1; y <= 3; y++) {
                safeSetBlock(world, altarPos.add(offset[0], y, offset[1]), STONE_BRICK);
            }
            // 柱顶装饰
            safeSetBlock(world, altarPos.add(offset[0], 3, offset[1]), CHISELED_STONE);
        }
    }

    /**
     * Tier 3: 中型神殿 (7x7) + 8根柱子 + 光源
     */
    private static void generateTier3(World world, BlockPos altarPos) {
        BlockPos floorCenter = altarPos.down();
        
        // 7x7 地板 (石英)
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                safeSetBlock(world, floorCenter.add(x, 0, z), QUARTZ_BLOCK);
            }
        }
        
        // 边缘錾制石砖
        for (int i = -3; i <= 3; i++) {
            safeSetBlock(world, floorCenter.add(i, 0, -3), CHISELED_STONE);
            safeSetBlock(world, floorCenter.add(i, 0, 3), CHISELED_STONE);
            safeSetBlock(world, floorCenter.add(-3, 0, i), CHISELED_STONE);
            safeSetBlock(world, floorCenter.add(3, 0, i), CHISELED_STONE);
        }
        
        // 8根石英柱 (高4格)
        int[][] pillarOffsets = {
            {-3, -3}, {3, -3}, {-3, 3}, {3, 3},  // 角
            {0, -3}, {0, 3}, {-3, 0}, {3, 0}      // 边中点
        };
        for (int[] offset : pillarOffsets) {
            for (int y = 1; y <= 4; y++) {
                safeSetBlock(world, altarPos.add(offset[0], y, offset[1]), QUARTZ_PILLAR);
            }
        }
        
        // 角柱顶部荧石光源
        safeSetBlock(world, altarPos.add(-3, 5, -3), GLOWSTONE);
        safeSetBlock(world, altarPos.add(3, 5, -3), GLOWSTONE);
        safeSetBlock(world, altarPos.add(-3, 5, 3), GLOWSTONE);
        safeSetBlock(world, altarPos.add(3, 5, 3), GLOWSTONE);
    }

    /**
     * Tier 4: 大型神殿 (9x9) + 外墙 + 入口
     */
    private static void generateTier4(World world, BlockPos altarPos) {
        BlockPos floorCenter = altarPos.down();
        
        // 9x9 地板
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                safeSetBlock(world, floorCenter.add(x, 0, z), QUARTZ_BLOCK);
            }
        }
        
        // 中心十字金色装饰
        for (int i = -1; i <= 1; i++) {
            safeSetBlock(world, floorCenter.add(i, 0, 0), GOLD_BLOCK);
            safeSetBlock(world, floorCenter.add(0, 0, i), GOLD_BLOCK);
        }
        
        // 外墙 (不包括入口)
        for (int i = -4; i <= 4; i++) {
            // 北墙和南墙
            for (int y = 1; y <= 3; y++) {
                if (Math.abs(i) > 1) { // 留入口
                    safeSetBlock(world, altarPos.add(i, y, -4), STONE_BRICK);
                    safeSetBlock(world, altarPos.add(i, y, 4), STONE_BRICK);
                }
            }
            // 东墙和西墙 (完整)
            for (int y = 1; y <= 3; y++) {
                safeSetBlock(world, altarPos.add(-4, y, i), STONE_BRICK);
                safeSetBlock(world, altarPos.add(4, y, i), STONE_BRICK);
            }
        }
        
        // 角柱 (高5格)
        int[][] corners = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}};
        for (int[] c : corners) {
            for (int y = 1; y <= 5; y++) {
                safeSetBlock(world, altarPos.add(c[0], y, c[1]), QUARTZ_PILLAR);
            }
            safeSetBlock(world, altarPos.add(c[0], 6, c[1]), SEA_LANTERN);
        }
    }

    /**
     * Tier 5: 完整圣殿 (11x11) + 穹顶 + 宝石装饰
     */
    private static void generateTier5(World world, BlockPos altarPos) {
        BlockPos floorCenter = altarPos.down();
        
        // 11x11 地板
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                safeSetBlock(world, floorCenter.add(x, 0, z), QUARTZ_BLOCK);
            }
        }
        
        // 中心十字钻石+绿宝石装饰
        safeSetBlock(world, floorCenter, DIAMOND_BLOCK);
        for (int i = -2; i <= 2; i++) {
            if (i != 0) {
                safeSetBlock(world, floorCenter.add(i, 0, 0), EMERALD_BLOCK);
                safeSetBlock(world, floorCenter.add(0, 0, i), EMERALD_BLOCK);
            }
        }
        
        // 外墙
        for (int i = -5; i <= 5; i++) {
            for (int y = 1; y <= 4; y++) {
                // 留入口
                if (Math.abs(i) > 2) {
                    safeSetBlock(world, altarPos.add(i, y, -5), STONE_BRICK);
                    safeSetBlock(world, altarPos.add(i, y, 5), STONE_BRICK);
                }
                safeSetBlock(world, altarPos.add(-5, y, i), STONE_BRICK);
                safeSetBlock(world, altarPos.add(5, y, i), STONE_BRICK);
            }
        }
        
        // 穹顶 (简化版 - 阶梯状)
        // 第一层屋顶
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                safeSetBlock(world, altarPos.add(x, 5, z), STONE_BRICK);
            }
        }
        // 第二层
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                safeSetBlock(world, altarPos.add(x, 6, z), QUARTZ_BLOCK);
            }
        }
        // 第三层
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                safeSetBlock(world, altarPos.add(x, 7, z), QUARTZ_BLOCK);
            }
        }
        // 顶部光源
        safeSetBlock(world, altarPos.add(0, 8, 0), SEA_LANTERN);
        safeSetBlock(world, altarPos.add(0, 7, 0), GLOWSTONE);
        
        // 角塔
        int[][] corners = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}};
        for (int[] c : corners) {
            for (int y = 1; y <= 7; y++) {
                safeSetBlock(world, altarPos.add(c[0], y, c[1]), QUARTZ_PILLAR);
            }
            safeSetBlock(world, altarPos.add(c[0], 8, c[1]), SEA_LANTERN);
        }
    }

    /**
     * 安全设置方块 (不替换玩家放置的方块或祭坛本身)
     */
    private static void safeSetBlock(World world, BlockPos pos, IBlockState state) {
        IBlockState current = world.getBlockState(pos);
        Block currentBlock = current.getBlock();
        
        // 不替换祭坛、空气以外的用户方块
        if (currentBlock == Blocks.AIR || 
            currentBlock == Blocks.GRASS || 
            currentBlock == Blocks.DIRT ||
            currentBlock == Blocks.STONE ||
            currentBlock == Blocks.COBBLESTONE ||
            currentBlock == Blocks.GRAVEL ||
            currentBlock == Blocks.SAND ||
            currentBlock == Blocks.WATER ||
            currentBlock == Blocks.FLOWING_WATER ||
            currentBlock == Blocks.TALLGRASS ||
            currentBlock == Blocks.DEADBUSH ||
            currentBlock == Blocks.SNOW_LAYER ||
            // 允许覆盖我们自己放置的建筑材料
            currentBlock == Blocks.STONEBRICK ||
            currentBlock == Blocks.QUARTZ_BLOCK ||
            currentBlock == Blocks.GLOWSTONE ||
            currentBlock == Blocks.SEA_LANTERN ||
            currentBlock == Blocks.GOLD_BLOCK ||
            currentBlock == Blocks.DIAMOND_BLOCK ||
            currentBlock == Blocks.EMERALD_BLOCK) {
            
            world.setBlockState(pos, state, 2);
        }
    }
}
