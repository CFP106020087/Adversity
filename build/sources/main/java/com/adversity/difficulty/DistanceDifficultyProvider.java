package com.adversity.difficulty;

import com.adversity.config.AdversityConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 距离难度提供者 - 基于与世界出生点的距离计算难度
 */
public class DistanceDifficultyProvider implements IDifficultyProvider {

    @Override
    public String getId() {
        return "distance";
    }

    @Override
    public float getWeight() {
        return (float) AdversityConfig.difficultySource.distanceWeight;
    }

    @Override
    public float calculateDifficulty(World world, BlockPos pos, @Nullable EntityPlayer player) {
        BlockPos spawnPoint = world.getSpawnPoint();

        // 计算水平距离
        double dx = pos.getX() - spawnPoint.getX();
        double dz = pos.getZ() - spawnPoint.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        // 从配置读取参数
        double safeDistance = AdversityConfig.difficultySource.safeDistance;
        double blocksPerDifficulty = AdversityConfig.difficultySource.blocksPerDifficulty;
        double maxDifficulty = AdversityConfig.difficultySource.maxDistanceDifficulty;

        // 减去安全距离
        double effectiveDistance = Math.max(0, distance - safeDistance);

        // 计算难度
        float difficulty = (float) (effectiveDistance / blocksPerDifficulty);

        // 应用上限（0 表示无上限）
        if (maxDifficulty > 0) {
            return (float) Math.min(difficulty, maxDifficulty);
        }
        return difficulty;
    }

    @Override
    public boolean isApplicable(World world, BlockPos pos, @Nullable EntityPlayer player) {
        // 在所有维度生效（包括模组维度）
        return true;
    }
}
