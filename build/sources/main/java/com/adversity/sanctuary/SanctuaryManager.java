package com.adversity.sanctuary;

import com.adversity.Adversity;
import com.adversity.config.AdversityConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 圣所管理器
 * 提供静态API供其他系统查询和操作圣所
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class SanctuaryManager {

    // 每小时燃料消耗（天然圣所）
    public static final int NATURAL_FUEL_PER_HOUR = 100;
    // 每小时燃料消耗（人造圣所，2倍）
    public static final int ARTIFICIAL_FUEL_PER_HOUR = 200;

    // 燃料消耗间隔（每分钟检查一次）
    private static final int FUEL_TICK_INTERVAL = 20 * 60; // 1200 ticks = 1 minute

    // ==================== 查询 API ====================

    /**
     * 检查位置是否在激活的圣所内
     */
    public static boolean isInActiveSanctuary(World world, BlockPos pos) {
        if (world == null)
            return false;
        SanctuaryData data = SanctuaryData.get(world);
        SanctuaryZone zone = data.getSanctuaryAt(world.provider.getDimension(), pos);
        return zone != null && zone.isActive();
    }

    /**
     * 获取位置所在的激活圣所
     */
    public static SanctuaryZone getActiveSanctuaryAt(World world, BlockPos pos) {
        if (world == null)
            return null;
        SanctuaryData data = SanctuaryData.get(world);
        SanctuaryZone zone = data.getSanctuaryAt(world.provider.getDimension(), pos);
        return (zone != null && zone.isActive()) ? zone : null;
    }

    /**
     * 获取玩家所在的激活圣所
     */
    public static SanctuaryZone getPlayerSanctuary(EntityPlayer player) {
        if (player == null || player.world == null)
            return null;
        return getActiveSanctuaryAt(player.world, player.getPosition());
    }

    /**
     * 检查玩家是否在激活的圣所内
     */
    public static boolean isPlayerInSanctuary(EntityPlayer player) {
        return getPlayerSanctuary(player) != null;
    }

    /**
     * 获取位置的精英生成减少率
     * 
     * @return 0.0 = 无减少, 1.0 = 完全阻止
     */
    public static float getEliteReduction(World world, BlockPos pos) {
        SanctuaryZone zone = getActiveSanctuaryAt(world, pos);
        return zone != null ? zone.getEliteReduction() : 0.0f;
    }

    /**
     * 获取玩家的饰品效果倍率
     * 
     * @return 1.0 = 完整效果, 0.5 = 半效果（圣所外或人造圣所）
     */
    public static float getBaubleEffectMultiplier(EntityPlayer player) {
        SanctuaryZone zone = getPlayerSanctuary(player);
        if (zone == null) {
            return 0.5f; // 圣所外饰品50%效果
        }
        return zone.getBaubleEffectMultiplier();
    }

    /**
     * 检查玩家是否在天然圣所内
     */
    public static boolean isInNaturalSanctuary(EntityPlayer player) {
        SanctuaryZone zone = getPlayerSanctuary(player);
        return zone != null && zone.type == SanctuaryType.NATURAL;
    }

    /**
     * 获取玩家的词条伤害压制比例
     * 圣所内受到的词条伤害会被削减
     * 
     * @return 0.0 = 无压制, 0.5 = 50%伤害压制
     */
    public static float getAffixDamageReduction(EntityPlayer player) {
        SanctuaryZone zone = getPlayerSanctuary(player);
        return zone != null ? zone.getAffixDamageReduction() : 0.0f;
    }

    /**
     * 获取位置的圣所难度倍率
     * EASE模式降低难度
     * 
     * @return 1.0 = 无修正, 0.5 = 半难度
     */
    public static double getDifficultyMultiplier(World world, BlockPos pos) {
        SanctuaryZone zone = getActiveSanctuaryAt(world, pos);
        return zone != null ? zone.getDifficultyMultiplier() : 1.0;
    }

    /**
     * 获取位置的圣所最高允许精英等级
     * FARM模式限制精英等级上限
     * 
     * @return 0 = 无限制, >0 = 最高允许等级
     */
    public static int getMaxAllowedTier(World world, BlockPos pos) {
        SanctuaryZone zone = getActiveSanctuaryAt(world, pos);
        return zone != null ? zone.getMaxAllowedTier() : 0;
    }

    /**
     * 获取指定世界所有已激活的圣所列表
     */
    public static java.util.List<SanctuaryZone> getActivatedSanctuaries(World world) {
        java.util.List<SanctuaryZone> result = new java.util.ArrayList<>();
        if (world == null)
            return result;

        SanctuaryData data = SanctuaryData.get(world);
        for (SanctuaryZone zone : data.getAllSanctuaries()) {
            if (zone.isActive()) {
                result.add(zone);
            }
        }
        return result;
    }

    /**
     * 获取所有维度的已激活圣所列表（需要传入主世界）
     */
    public static java.util.List<SanctuaryZone> getAllActivatedSanctuaries(World overworld) {
        java.util.List<SanctuaryZone> result = new java.util.ArrayList<>();
        if (overworld == null)
            return result;

        SanctuaryData data = SanctuaryData.get(overworld);
        for (SanctuaryZone zone : data.getAllSanctuaries()) {
            if (zone.isActive()) {
                result.add(zone);
            }
        }
        return result;
    }

    // ==================== 操作 API ====================

    /**
     * 激活天然圣所
     */
    public static boolean activateNaturalSanctuary(World world, BlockPos center, int tier) {
        if (world == null || world.isRemote)
            return false;

        SanctuaryData data = SanctuaryData.get(world);
        int dimension = world.provider.getDimension();

        // 检查是否已存在
        if (data.getSanctuaryAt(dimension, center) != null) {
            return false;
        }

        SanctuaryZone zone = SanctuaryZone.createNatural(dimension, center, tier);
        data.addSanctuary(zone);
        return true;
    }

    /**
     * 激活人造圣所
     */
    public static boolean activateArtificialSanctuary(World world, BlockPos center) {
        if (world == null || world.isRemote)
            return false;

        SanctuaryData data = SanctuaryData.get(world);
        int dimension = world.provider.getDimension();

        // 检查是否已存在
        if (data.getSanctuaryAt(dimension, center) != null) {
            return false;
        }

        SanctuaryZone zone = SanctuaryZone.createArtificial(dimension, center);
        data.addSanctuary(zone);
        return true;
    }

    /**
     * 向圣所添加燃料
     * 
     * @return 添加后的燃料值，-1表示失败
     */
    public static int addFuel(World world, BlockPos center, int amount) {
        if (world == null || world.isRemote)
            return -1;
        SanctuaryData data = SanctuaryData.get(world);
        return data.addFuel(world.provider.getDimension(), center, amount);
    }

    /**
     * 停用圣所
     */
    public static boolean deactivateSanctuary(World world, BlockPos center) {
        if (world == null || world.isRemote)
            return false;
        SanctuaryData data = SanctuaryData.get(world);
        return data.removeSanctuary(world.provider.getDimension(), center);
    }

    /**
     * 检查是否有激活圣所（精确位置）
     */
    public static boolean isSanctuaryAt(World world, BlockPos pos) {
        SanctuaryZone zone = getActiveSanctuaryAt(world, pos);
        // 检查pos是否与zone.center完全一致，防止传送到区域内其他位置
        return zone != null && zone.center.equals(pos);
    }

    /**
     * 消耗燃料
     */
    public static void consumeFuel(World world, BlockPos center, int amount) {
        if (world == null || world.isRemote)
            return;
        SanctuaryData data = SanctuaryData.get(world);
        data.consumeFuel(world.provider.getDimension(), center, amount);
    }

    /**
     * 设置圣所模式
     */
    public static boolean setSanctuaryMode(World world, BlockPos center, SanctuaryMode mode) {
        if (world == null || world.isRemote)
            return false;
        SanctuaryData data = SanctuaryData.get(world);
        SanctuaryZone zone = data.getSanctuaryAt(world.provider.getDimension(), center);
        if (zone != null) {
            zone.setMode(mode);
            data.markDirty();
            return true;
        }
        return false;
    }

    /**
     * 升级圣所等级 (仅限天然圣所)
     */
    public static boolean upgradeSanctuary(World world, BlockPos center, int newTier) {
        if (world == null || world.isRemote)
            return false;
        SanctuaryData data = SanctuaryData.get(world);
        SanctuaryZone zone = data.getSanctuaryAt(world.provider.getDimension(), center);
        if (zone != null && zone.type == SanctuaryType.NATURAL) {
            zone.tier = newTier;
            zone.radius = AdversityConfig.sanctuarySettings.naturalBaseRadius +
                    (newTier - 1) * AdversityConfig.sanctuarySettings.naturalRadiusPerTier;
            zone.maxFuel = AdversityConfig.sanctuarySettings.naturalFuelCapacity * newTier;
            data.markDirty();
            return true;
        }
        return false;
    }

    // ==================== 燃料消耗 Tick ====================

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (event.world.isRemote)
            return;
        if (event.world.provider.getDimension() != 0)
            return; // 只在主世界处理

        long worldTime = event.world.getTotalWorldTime();
        if (worldTime % FUEL_TICK_INTERVAL != 0)
            return;

        SanctuaryData data = SanctuaryData.get(event.world);

        for (SanctuaryZone zone : data.getAllSanctuaries()) {
            if (!zone.isActive())
                continue;

            // 计算每分钟消耗 (每小时/60)
            int baseUsage;
            if (zone.type == SanctuaryType.NATURAL) {
                baseUsage = AdversityConfig.sanctuarySettings.naturalFuelUsage;
            } else {
                baseUsage = AdversityConfig.sanctuarySettings.artificialFuelUsage;
            }

            // 应用模式倍率
            double multiplier = zone.getFuelUsageMultiplier();
            int fuelPerHour = (int) (baseUsage * multiplier);

            // 将每小时转为每分钟
            int fuelPerMinute = fuelPerHour / 60;

            // 最少消耗1
            fuelPerMinute = Math.max(1, fuelPerMinute);

            data.consumeFuel(zone.dimension, zone.center, fuelPerMinute);
        }
    }
}
