package com.adversity.handler;

import com.adversity.sanctuary.TileEntitySanctuary;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 圣所方块保护处理器
 * 防止圣所结构内的方块被破坏（玩家、生物、或其他模组）
 * 使用多层保护：事件取消 + 定时恢复
 */
@Mod.EventBusSubscriber
public class SanctuaryProtectionHandler {

    /** 圣所生成网格间隔 (与SanctuaryGenerator一致) */
    private static final int SPACING = 1000;
    private static final int OFFSET = 500;
    private static final int UNDERGROUND_Y = 32;

    /** 检查间隔 (每20tick = 1秒检查一次) */
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20;

    /**
     * 阻止玩家破坏圣所结构内的方块
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote)
            return;

        BlockPos pos = event.getPos();
        if (isWithinSanctuaryStructure(event.getWorld(), pos)) {
            event.setCanceled(true);
        }
    }

    /**
     * 阻止爆炸破坏圣所结构
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getWorld().isRemote)
            return;

        Iterator<BlockPos> it = event.getAffectedBlocks().iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (isWithinSanctuaryStructure(event.getWorld(), pos)) {
                it.remove();
            }
        }
    }

    /**
     * 定时检查圣所方块完整性 (防止绕过事件的修改)
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (event.world.isRemote)
            return;

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL)
            return;
        tickCounter = 0;

        // 收集活跃圣所位置 (复制列表避免ConcurrentModificationException)
        List<BlockPos> activeSanctuaries = new ArrayList<>();
        List<TileEntity> tileEntityCopy = new ArrayList<>(event.world.loadedTileEntityList);
        for (TileEntity te : tileEntityCopy) {
            if (te instanceof TileEntitySanctuary) {
                TileEntitySanctuary sanctuary = (TileEntitySanctuary) te;
                if (sanctuary.isActive()) {
                    activeSanctuaries.add(te.getPos());
                    validateSanctuaryBlocks(event.world, te.getPos(), sanctuary.getTier());
                }
            }
        }

        // 推出圣所范围内的怪物 (50格)
        if (!activeSanctuaries.isEmpty()) {
            repelMonstersFromSanctuaries(event.world, activeSanctuaries);
        }
    }

    /** 圣所驱离半径 */
    private static final int REPEL_RADIUS = 50;

    /**
     * 阻止怪物在圣所50格内生成
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMobSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.getWorld().isRemote)
            return;
        if (!(event.getEntityLiving() instanceof IMob))
            return;

        BlockPos spawnPos = new BlockPos(event.getX(), event.getY(), event.getZ());

        // 检查是否在任何活跃圣所50格内
        for (TileEntity te : event.getWorld().loadedTileEntityList) {
            if (te instanceof TileEntitySanctuary) {
                TileEntitySanctuary sanctuary = (TileEntitySanctuary) te;
                if (sanctuary.isActive()) {
                    double distSq = spawnPos.distanceSq(te.getPos());
                    if (distSq <= REPEL_RADIUS * REPEL_RADIUS) {
                        event.setResult(Event.Result.DENY);
                        return;
                    }
                }
            }
        }
    }

    /**
     * 将怪物推出圣所范围
     */
    private static void repelMonstersFromSanctuaries(World world, List<BlockPos> sanctuaries) {
        for (net.minecraft.entity.Entity entity : world.loadedEntityList) {
            if (!(entity instanceof IMob))
                continue;
            if (!entity.isEntityAlive())
                continue;

            BlockPos entityPos = entity.getPosition();

            for (BlockPos center : sanctuaries) {
                double distSq = entityPos.distanceSq(center);
                if (distSq <= REPEL_RADIUS * REPEL_RADIUS && distSq > 1) {
                    // 计算推出方向 (从圣所中心向外)
                    double dx = entity.posX - center.getX();
                    double dz = entity.posZ - center.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    if (dist > 0.1) {
                        // 归一化方向并施加速度
                        double pushStrength = 0.5;
                        entity.motionX += (dx / dist) * pushStrength;
                        entity.motionZ += (dz / dist) * pushStrength;
                        entity.velocityChanged = true;
                    }
                    break;
                }
            }
        }
    }

    /**
     * 验证并恢复圣所方块
     */
    private static void validateSanctuaryBlocks(World world, BlockPos center, int tier) {
        int radius = getStructureRadius(tier);
        int floorY = center.getY() - (radius / 2);

        IBlockState floorState = Blocks.QUARTZ_BLOCK.getStateFromMeta(1);
        IBlockState pillarState = Blocks.QUARTZ_BLOCK.getStateFromMeta(2);

        // 检查地板 (只检查关键方块，不是全部)
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                if (dx * dx + dz * dz <= (radius - 1) * (radius - 1)) {
                    BlockPos floorPos = new BlockPos(center.getX() + dx, floorY, center.getZ() + dz);
                    IBlockState current = world.getBlockState(floorPos);
                    if (current.getBlock() == Blocks.AIR || current.getBlock() != Blocks.QUARTZ_BLOCK) {
                        world.setBlockState(floorPos, floorState, 2);
                    }
                }
            }
        }
    }

    /**
     * 检查坐标是否在圣所结构范围内
     */
    private static boolean isWithinSanctuaryStructure(World world, BlockPos pos) {
        int gridX = Math.round((float) (pos.getX() - OFFSET) / SPACING);
        int gridZ = Math.round((float) (pos.getZ() - OFFSET) / SPACING);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int sanctuaryX = (gridX + dx) * SPACING + OFFSET;
                int sanctuaryZ = (gridZ + dz) * SPACING + OFFSET;
                BlockPos center = new BlockPos(sanctuaryX, UNDERGROUND_Y, sanctuaryZ);

                TileEntity te = world.getTileEntity(center);
                if (te instanceof TileEntitySanctuary) {
                    TileEntitySanctuary sanctuary = (TileEntitySanctuary) te;
                    int tier = sanctuary.getTier();
                    int radius = getStructureRadius(tier);

                    double distX = pos.getX() - center.getX();
                    double distY = pos.getY() - center.getY();
                    double distZ = pos.getZ() - center.getZ();
                    double distSq = distX * distX + distY * distY + distZ * distZ;

                    if (distSq <= radius * radius) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 获取圣所结构半径 (与SanctuaryGenerator一致)
     */
    private static int getStructureRadius(int tier) {
        if (tier < 1)
            tier = 1;
        if (tier > 4)
            tier = 4;
        int radius = 4 + tier * 2;
        if (tier == 4)
            radius = 14;
        return radius;
    }
}
