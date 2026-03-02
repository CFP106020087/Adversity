package com.adversity.sanctuary;

import com.adversity.Adversity;
import com.adversity.config.AdversityConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * 圣所世界生成器
 * 
 * 在特定坐标生成天然圣所结构：
 * - 每隔1000格的网格交点
 * - 地底石英神殿结构
 * - 祭坛和周围方块不可破坏
 */
public class SanctuaryGenerator implements IWorldGenerator {

    /** 圣所生成间隔 (格) */
    private static final int SPACING = 1000;

    /** 圣所生成偏移 (避免正好在原点) */
    private static final int OFFSET = 500;

    /** 地底生成深度 */
    private static final int UNDERGROUND_Y = 32;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
            IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {

        if (!AdversityConfig.sanctuarySettings.enableSanctuaries) {
            return;
        }

        // 检查维度白名单
        boolean allowed = false;
        for (int dim : AdversityConfig.sanctuarySettings.allowedDimensions) {
            if (world.provider.getDimension() == dim) {
                allowed = true;
                break;
            }
        }

        // 检查维度黑名单 (优先级高于白名单)
        for (int dim : AdversityConfig.sanctuarySettings.dimensionBlacklist) {
            if (world.provider.getDimension() == dim) {
                allowed = false;
                break;
            }
        }

        if (!allowed) {
            return;
        }

        // 检查是否是圣所生成位置
        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;

        // 计算最近的圣所网格点
        int nearestX = Math.round((float) (worldX - OFFSET) / SPACING) * SPACING + OFFSET;
        int nearestZ = Math.round((float) (worldZ - OFFSET) / SPACING) * SPACING + OFFSET;

        // 检查网格点是否在当前区块内
        if (nearestX >= worldX && nearestX < worldX + 16 &&
                nearestZ >= worldZ && nearestZ < worldZ + 16) {

            BlockPos pos = new BlockPos(nearestX, UNDERGROUND_Y, nearestZ);
            // 根据距离计算等级
            double dist = Math.sqrt(pos.getX() * pos.getX() + pos.getZ() * pos.getZ());
            int tier = 1 + (int) (dist / 10000);
            if (tier > 4)
                tier = 4;
            generateSanctuaryAtPosition(world, pos, tier);
            Adversity.LOGGER.info("Generated underground quartz sanctuary at {} in dim {}",
                    pos, world.provider.getDimension());
        }
    }

    /**
     * 生成地底石英神殿结构
     * 
     * 结构设计:
     * - 7x7x7 空腔房间
     * - 石英块地板和墙壁
     * - 石英柱子装饰
     * - 中心圣所祭坛（不可破坏）
     * - 发光石照明
     */
    /**
     * 生成圣所结构
     */
    public static void generateSanctuaryAtPosition(World world, BlockPos center, int tier) {
        new SanctuaryGenerator().generateSphericalSanctuary(world, center, tier);
    }

    /**
     * 生成球形圣所结构
     */
    private void generateSphericalSanctuary(World world, BlockPos center, int tier) {
        // 限制等级范围
        if (tier < 1)
            tier = 1;
        if (tier > 4)
            tier = 4;

        // 尺寸设定 (半径)
        // T1: r=6
        // T2: r=8
        // T3: r=10
        // T4: r=14
        int radius = 4 + tier * 2;
        if (tier == 4)
            radius = 14;

        // 使用装饰性石英方块 (外观美观，通过事件保护不可破坏)
        // Meta: 0=普通, 1=錾制, 2=柱形
        IBlockState floorState = Blocks.QUARTZ_BLOCK.getStateFromMeta(1); // 錾制石英 (地板)
        IBlockState pillarState = Blocks.QUARTZ_BLOCK.getStateFromMeta(2); // 柱形石英 (柱子)
        IBlockState ceilingState = Blocks.QUARTZ_BLOCK.getDefaultState(); // 普通石英 (天花板)

        // 1. 清空球形区域 (挖空地底)
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double distSq = dx * dx + dy * dy + dz * dz;
                    // 稍微留一点边缘
                    if (distSq <= (radius - 1) * (radius - 1)) {
                        world.setBlockState(center.add(dx, dy, dz), Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }

        // 2. 生成地板 (y = -radius/2 处)
        int floorY = -(radius / 2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= (radius - 1) * (radius - 1)) {
                    // 地板 - 錾制石英
                    world.setBlockState(center.add(dx, floorY, dz), floorState, 2);
                    // 天花板装饰 (顶部)
                    if (dx * dx + dz * dz <= (radius / 2) * (radius / 2)) {
                        world.setBlockState(center.add(dx, radius - 1, dz), Blocks.SEA_LANTERN.getDefaultState(), 2);
                    }
                }
            }
        }

        // 3. 生成环形柱子
        int pillarCount = 4 + tier; // T1=5, T4=8
        double angleStep = 2 * Math.PI / pillarCount;
        int pillarRadius = radius - 3;

        for (int i = 0; i < pillarCount; i++) {
            double angle = i * angleStep;
            int px = (int) (Math.cos(angle) * pillarRadius);
            int pz = (int) (Math.sin(angle) * pillarRadius);

            BlockPos pillarBase = center.add(px, floorY, pz);
            // 生成柱子直到顶部
            for (int y = 0; y < radius + (radius / 2); y++) {
                BlockPos p = pillarBase.add(0, y, 0);
                // 仅在球体内部生成
                if (p.distanceSq(center) < (radius - 1) * (radius - 1)) {
                    world.setBlockState(p, pillarState, 2); // 柱形石英
                }
            }
            // 柱子底部发光
            world.setBlockState(pillarBase.up(), Blocks.SEA_LANTERN.getDefaultState(), 2);
        }

        // 4. 放置祭坛 (中心, 地板上方)
        BlockPos altarPos = center.add(0, floorY + 1, 0);
        Block altarBlock = com.adversity.block.BlockRegistry.SANCTUARY_ALTAR;
        if (altarBlock != null) {
            // 底座
            world.setBlockState(center.add(0, floorY, 0), Blocks.OBSIDIAN.getDefaultState(), 2);
            // 祭坛本体
            world.setBlockState(altarPos, altarBlock.getDefaultState(), 2);

            // 设置TileEntity等级
            net.minecraft.tileentity.TileEntity te = world.getTileEntity(altarPos);
            if (te instanceof com.adversity.sanctuary.TileEntitySanctuary) {
                ((com.adversity.sanctuary.TileEntitySanctuary) te).setTier(tier);
            }
        }

        Adversity.LOGGER.info("Generated Spherical Tier {} Sanctuary at {}", tier, center);
    }
}
