package com.adversity.effect;

import com.adversity.Adversity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 压制效果管理器
 * 管理由宁静之尘等物品创建的临时压制区域
 *
 * 设计理念：
 * - 使用高效的空间数据结构存储压制区域
 * - 支持多维度多区域同时存在
 * - 自动清理过期的压制效果
 * - 提供快速的位置查询接口
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class SuppressionManager {

    /**
     * 压制区域数据
     */
    public static class SuppressionZone {
        public final int dimension;
        public final BlockPos center;
        public final int radius;
        public final int radiusSquared;
        public int remainingTicks;
        public final long createdTime;

        public SuppressionZone(int dimension, BlockPos center, int radius, int duration) {
            this.dimension = dimension;
            this.center = center;
            this.radius = radius;
            this.radiusSquared = radius * radius;
            this.remainingTicks = duration;
            this.createdTime = System.currentTimeMillis();
        }

        /**
         * 检查位置是否在压制区域内
         */
        public boolean contains(BlockPos pos) {
            double dx = pos.getX() - center.getX();
            double dy = pos.getY() - center.getY();
            double dz = pos.getZ() - center.getZ();
            // 使用球形范围检测
            return (dx * dx + dy * dy + dz * dz) <= radiusSquared;
        }

        /**
         * 检查位置是否在压制区域内 (忽略Y轴，使用圆柱形)
         */
        public boolean containsXZ(BlockPos pos) {
            double dx = pos.getX() - center.getX();
            double dz = pos.getZ() - center.getZ();
            return (dx * dx + dz * dz) <= radiusSquared;
        }
    }

    // 按维度存储的压制区域 (线程安全)
    private static final Map<Integer, List<SuppressionZone>> activeZones = new ConcurrentHashMap<>();

    /**
     * 添加新的压制区域
     *
     * @param dimension 维度ID
     * @param center    中心坐标
     * @param radius    半径 (格)
     * @param duration  持续时间 (ticks)
     */
    public static void addSuppression(int dimension, BlockPos center, int radius, int duration) {
        SuppressionZone zone = new SuppressionZone(dimension, center, radius, duration);

        activeZones.computeIfAbsent(dimension, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(zone);

        Adversity.LOGGER.debug("Added suppression zone at {} in dimension {} (radius={}, duration={})",
            center, dimension, radius, duration);
    }

    /**
     * 检查位置是否被压制
     * 用于在怪物生成时检查是否应该生成精英
     *
     * @param dimension 维度ID
     * @param pos       位置
     * @return true 如果该位置被压制 (不应生成精英)
     */
    public static boolean isSuppressed(int dimension, BlockPos pos) {
        List<SuppressionZone> zones = activeZones.get(dimension);
        if (zones == null || zones.isEmpty()) {
            return false;
        }

        synchronized (zones) {
            for (SuppressionZone zone : zones) {
                if (zone.containsXZ(pos)) { // 使用圆柱形检测，对Y轴宽容
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取位置的压制强度 (用于调试/UI显示)
     *
     * @return 0.0-1.0 的压制强度值，0表示无压制
     */
    public static float getSuppressionStrength(int dimension, BlockPos pos) {
        List<SuppressionZone> zones = activeZones.get(dimension);
        if (zones == null || zones.isEmpty()) {
            return 0.0f;
        }

        float maxStrength = 0.0f;

        synchronized (zones) {
            for (SuppressionZone zone : zones) {
                double dx = pos.getX() - zone.center.getX();
                double dz = pos.getZ() - zone.center.getZ();
                double distSq = dx * dx + dz * dz;

                if (distSq <= zone.radiusSquared) {
                    // 越靠近中心，压制强度越高
                    float dist = (float) Math.sqrt(distSq);
                    float strength = 1.0f - (dist / zone.radius);
                    maxStrength = Math.max(maxStrength, strength);
                }
            }
        }

        return maxStrength;
    }

    /**
     * 获取离位置最近的压制区域信息
     *
     * @return 压制区域，如果无则返回null
     */
    public static SuppressionZone getNearestZone(int dimension, BlockPos pos) {
        List<SuppressionZone> zones = activeZones.get(dimension);
        if (zones == null || zones.isEmpty()) {
            return null;
        }

        SuppressionZone nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        synchronized (zones) {
            for (SuppressionZone zone : zones) {
                double dx = pos.getX() - zone.center.getX();
                double dz = pos.getZ() - zone.center.getZ();
                double distSq = dx * dx + dz * dz;

                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = zone;
                }
            }
        }

        return nearest;
    }

    /**
     * 每 tick 更新压制区域状态
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        int dimension = event.world.provider.getDimension();
        List<SuppressionZone> zones = activeZones.get(dimension);

        if (zones == null || zones.isEmpty()) return;

        // 更新并清理过期区域
        synchronized (zones) {
            Iterator<SuppressionZone> it = zones.iterator();
            while (it.hasNext()) {
                SuppressionZone zone = it.next();
                zone.remainingTicks--;

                if (zone.remainingTicks <= 0) {
                    it.remove();
                    Adversity.LOGGER.debug("Suppression zone expired at {} in dimension {}",
                        zone.center, dimension);
                }
            }
        }
    }

    /**
     * 清除指定维度的所有压制区域 (用于维度卸载时)
     */
    public static void clearDimension(int dimension) {
        activeZones.remove(dimension);
    }

    /**
     * 获取当前激活的压制区域数量 (调试用)
     */
    public static int getActiveZoneCount() {
        int count = 0;
        for (List<SuppressionZone> zones : activeZones.values()) {
            count += zones.size();
        }
        return count;
    }
}
