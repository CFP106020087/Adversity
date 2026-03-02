package com.adversity.sanctuary;

import com.adversity.Adversity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.InputStream;

/**
 * Schematic文件加载器
 * 支持标准MCEdit .schematic格式（NBT + GZip）
 * 
 * 用于从资源文件加载预制圣所结构
 */
public class SchematicLoader {

    /**
     * 从mod资源加载schematic并放置到世界中
     * 结构以底部中心对齐到指定位置
     * 
     * @param world 目标世界
     * @param center 放置中心点（底部中心）
     * @param tier 圣所等级(1-4)
     * @return 是否成功放置
     */
    public static boolean loadAndPlace(World world, BlockPos center, int tier) {
        String path = "/assets/adversity/schematics/sanctuary_t" + tier + ".schematic";

        try {
            InputStream is = SchematicLoader.class.getResourceAsStream(path);
            if (is == null) {
                Adversity.LOGGER.warn("Schematic not found: {}", path);
                return false;
            }

            NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
            is.close();

            int width = nbt.getShort("Width");
            int height = nbt.getShort("Height");
            int length = nbt.getShort("Length");
            byte[] blocks = nbt.getByteArray("Blocks");
            byte[] data = nbt.getByteArray("Data");

            if (blocks.length != width * height * length) {
                Adversity.LOGGER.error("Schematic {} has invalid block data (expected {}, got {})",
                        path, width * height * length, blocks.length);
                return false;
            }

            // 计算偏移：底部中心对齐
            int offsetX = -width / 2;
            int offsetY = 0; // 底部对齐
            int offsetZ = -length / 2;

            // 放置方块
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        int index = (y * length + z) * width + x;
                        int blockId = blocks[index] & 0xFF;
                        int meta = data[index] & 0x0F;

                        // 跳过空气
                        if (blockId == 0) continue;

                        Block block = Block.getBlockById(blockId);
                        if (block == null || block == Blocks.AIR) continue;

                        IBlockState state = block.getStateFromMeta(meta);
                        BlockPos pos = center.add(offsetX + x, offsetY + y, offsetZ + z);
                        world.setBlockState(pos, state, 2);
                    }
                }
            }

            Adversity.LOGGER.info("Placed T{} sanctuary schematic ({}x{}x{}) at {}",
                    tier, width, height, length, center);
            return true;

        } catch (Exception e) {
            Adversity.LOGGER.error("Failed to load schematic: {}", path, e);
            return false;
        }
    }

    /**
     * 获取指定等级schematic的尺寸
     * 用于升级时清空旧建筑
     * 
     * @return int[3] = {width, height, length}，失败返回null
     */
    public static int[] getSchematicSize(int tier) {
        String path = "/assets/adversity/schematics/sanctuary_t" + tier + ".schematic";
        try {
            InputStream is = SchematicLoader.class.getResourceAsStream(path);
            if (is == null) return null;

            NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
            is.close();

            return new int[] {
                nbt.getShort("Width"),
                nbt.getShort("Height"),
                nbt.getShort("Length")
            };
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 清空指定区域（用于升级时清除旧建筑）
     * 以中心点为基准，清空足够大的区域
     * 
     * @param world 目标世界
     * @param center 中心点
     * @param radius 清空半径（XZ方向）
     * @param height 清空高度
     */
    public static void clearArea(World world, BlockPos center, int radius, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    Block existing = world.getBlockState(pos).getBlock();
                    // 不清除基岩
                    if (existing != Blocks.BEDROCK) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
        Adversity.LOGGER.info("Cleared area {}x{}x{} at {}", radius * 2 + 1, height, radius * 2 + 1, center);
    }
}
