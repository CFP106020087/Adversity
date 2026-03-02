package com.adversity.difficulty;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IPlayerDifficulty;
import com.adversity.config.AdversityConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 时间难度提供者 - 基于游戏时间计算难度
 * 
 * 支持两种模式：
 * - 玩家个人游玩时间（多人推荐）：每个玩家有独立的难度进度
 * - 全局服务器时间（传统模式）：所有玩家共享时间难度
 */
public class TimeDifficultyProvider implements IDifficultyProvider {

    // 一天的 tick 数
    private static final long TICKS_PER_DAY = 24000L;

    @Override
    public String getId() {
        return "time";
    }

    @Override
    public float getWeight() {
        return (float) AdversityConfig.difficultySource.timeWeight;
    }

    @Override
    public float calculateDifficulty(World world, BlockPos pos, @Nullable EntityPlayer player) {
        long timeInTicks;

        // 根据配置决定使用哪种时间模式
        if (AdversityConfig.difficultySource.usePerPlayerTime && player != null) {
            // 使用玩家个人游玩时间
            IPlayerDifficulty playerDiff = CapabilityHandler.getPlayerDifficulty(player);
            if (playerDiff != null) {
                timeInTicks = playerDiff.getPlayTime();
            } else {
                // 回退到世界时间
                timeInTicks = world.getTotalWorldTime();
            }
        } else {
            // 传统模式：使用全局服务器时间
            timeInTicks = world.getTotalWorldTime();
        }

        float days = timeInTicks / (float) TICKS_PER_DAY;

        // 从配置读取参数
        double daysPerDifficulty = AdversityConfig.difficultySource.daysPerDifficulty;
        double maxDifficulty = AdversityConfig.difficultySource.maxTimeDifficulty;

        float difficulty = (float) (days / daysPerDifficulty);

        // 应用上限（0 表示无上限）
        if (maxDifficulty > 0) {
            return (float) Math.min(difficulty, maxDifficulty);
        }
        return difficulty;
    }

    @Override
    public boolean isApplicable(World world, BlockPos pos, @Nullable EntityPlayer player) {
        // 在所有维度生效
        return true;
    }
}

