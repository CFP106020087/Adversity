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
            int tier = 1 + (int) (dist / 2500);
            if (tier > 4)
                tier = 4;
            generateSanctuaryAtPosition(world, pos, tier);
            Adversity.LOGGER.info("Generated underground quartz sanctuary at {} in dim {}",
                    pos, world.provider.getDimension());
        }
    }

    /**
     * 生成圣所结构
     * T1-T4: 优先使用schematic文件，失败则回退到程序化生成
     */
    public static void generateSanctuaryAtPosition(World world, BlockPos center, int tier) {
        if (tier >= 1 && tier <= 4) {
            // 先清空区域
            int[] size = SchematicLoader.getSchematicSize(tier);
            if (size != null) {
                int clearRadius = Math.max(size[0], size[2]) / 2 + 2;
                int clearHeight = size[1] + 2;
                SchematicLoader.clearArea(world, center, clearRadius, clearHeight);
            }
            // 放置schematic
            if (SchematicLoader.loadAndPlace(world, center, tier)) {
                placeAltar(world, center, tier);
                return;
            }
            Adversity.LOGGER.warn("Schematic load failed for T{}, falling back to procedural", tier);
        }
        // 回退：程序化生成
        new SanctuaryGenerator().generateSphericalSanctuary(world, center, tier);
    }

    /**
     * 升级圣所：清空旧建筑 → 放置新等级schematic → 补祭坛
     * 
     * @param world   目标世界
     * @param center  圣所中心点
     * @param oldTier 旧等级
     * @param newTier 新等级
     * @return 是否升级成功
     */
    public static boolean upgradeSanctuary(World world, BlockPos center, int oldTier, int newTier) {
        if (newTier < 1 || newTier > 4) {
            Adversity.LOGGER.warn("Invalid upgrade tier: {}", newTier);
            return false;
        }

        // 1. 计算需要清空的范围（取新旧中较大的）
        int clearRadius = getClearRadius(oldTier, newTier);
        int clearHeight = getClearHeight(oldTier, newTier);

        // 2. 清空旧建筑
        SchematicLoader.clearArea(world, center, clearRadius, clearHeight);

        // 3. 放置新schematic
        if (!SchematicLoader.loadAndPlace(world, center, newTier)) {
            Adversity.LOGGER.error("Failed to place T{} schematic during upgrade!", newTier);
            // 回退到程序化
            new SanctuaryGenerator().generateSphericalSanctuary(world, center, newTier);
        }

        // 4. 放置祭坛
        placeAltar(world, center, newTier);

        Adversity.LOGGER.info("Upgraded sanctuary at {} from T{} to T{}", center, oldTier, newTier);
        return true;
    }

    /**
     * 在中心放置圣所祭坛 + 设置等级
     */
    private static void placeAltar(World world, BlockPos center, int tier) {
        Block altarBlock = com.adversity.block.BlockRegistry.SANCTUARY_ALTAR;
        if (altarBlock != null) {
            // 底座（黑曜石）
            world.setBlockState(center, Blocks.OBSIDIAN.getDefaultState(), 2);
            // 祭坛（中心上方1格）
            BlockPos altarPos = center.up();
            world.setBlockState(altarPos, altarBlock.getDefaultState(), 2);

            // 设置TileEntity等级
            net.minecraft.tileentity.TileEntity te = world.getTileEntity(altarPos);
            if (te instanceof com.adversity.sanctuary.TileEntitySanctuary) {
                ((com.adversity.sanctuary.TileEntitySanctuary) te).setTier(tier);
            }
        }
    }

    /**
     * 计算清空半径（XZ方向），取新旧schematic中较大的
     */
    private static int getClearRadius(int oldTier, int newTier) {
        // 各等级schematic宽度: T1=7, T2=14, T3=16, T4=25
        int[] widths = { 0, 7, 14, 16, 25 };
        int oldW = (oldTier >= 1 && oldTier <= 4) ? widths[oldTier] : 14;
        int newW = (newTier >= 1 && newTier <= 4) ? widths[newTier] : 14;
        return Math.max(oldW, newW) / 2 + 2; // 额外2格余量
    }

    /**
     * 计算清空高度
     */
    private static int getClearHeight(int oldTier, int newTier) {
        // 各等级schematic高度: T1=17, T2=31, T3=50, T4=47
        int[] heights = { 0, 17, 31, 50, 47 };
        int oldH = (oldTier >= 1 && oldTier <= 4) ? heights[oldTier] : 31;
        int newH = (newTier >= 1 && newTier <= 4) ? heights[newTier] : 31;
        return Math.max(oldH, newH) + 2; // 额外2格余量
    }

    /**
     * 生成球形圣所结构（程序化 - 用于回退和T0）
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
