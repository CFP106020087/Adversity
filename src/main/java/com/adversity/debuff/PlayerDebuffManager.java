package com.adversity.debuff;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家Debuff管理器
 * 管理所有自定义debuff效果，支持叠层机制
 */
public class PlayerDebuffManager {

    /**
     * 玩家UUID -> Debuff类型 -> Debuff数据
     */
    private static final Map<UUID, Map<DebuffType, DebuffData>> playerDebuffs = new ConcurrentHashMap<>();

    /**
     * 添加或更新debuff叠层
     */
    public static void addStacks(EntityPlayer player, DebuffType type, int stacks, int durationTicks, int sourceEntityId) {
        UUID playerId = player.getUniqueID();

        playerDebuffs.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Map<DebuffType, DebuffData> debuffs = playerDebuffs.get(playerId);

        DebuffData existing = debuffs.get(type);
        if (existing != null) {
            // 叠加层数，刷新持续时间
            existing.stacks = Math.min(existing.stacks + stacks, type.getMaxStacks());
            existing.remainingTicks = Math.max(existing.remainingTicks, durationTicks);
            existing.sourceEntityId = sourceEntityId;
        } else {
            debuffs.put(type, new DebuffData(Math.min(stacks, type.getMaxStacks()), durationTicks, sourceEntityId));
        }
    }

    /**
     * 设置debuff到指定层数
     */
    public static void setStacks(EntityPlayer player, DebuffType type, int stacks, int durationTicks, int sourceEntityId) {
        UUID playerId = player.getUniqueID();

        playerDebuffs.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Map<DebuffType, DebuffData> debuffs = playerDebuffs.get(playerId);

        stacks = Math.min(stacks, type.getMaxStacks());
        if (stacks <= 0) {
            debuffs.remove(type);
        } else {
            DebuffData existing = debuffs.get(type);
            if (existing != null) {
                existing.stacks = stacks;
                existing.remainingTicks = Math.max(existing.remainingTicks, durationTicks);
                existing.sourceEntityId = sourceEntityId;
            } else {
                debuffs.put(type, new DebuffData(stacks, durationTicks, sourceEntityId));
            }
        }
    }

    /**
     * 减少debuff层数
     */
    public static void removeStacks(EntityPlayer player, DebuffType type, int stacks) {
        UUID playerId = player.getUniqueID();
        Map<DebuffType, DebuffData> debuffs = playerDebuffs.get(playerId);
        if (debuffs == null) return;

        DebuffData data = debuffs.get(type);
        if (data != null) {
            data.stacks -= stacks;
            if (data.stacks <= 0) {
                debuffs.remove(type);
            }
        }
    }

    /**
     * 移除debuff
     */
    public static void removeDebuff(EntityPlayer player, DebuffType type) {
        UUID playerId = player.getUniqueID();
        Map<DebuffType, DebuffData> debuffs = playerDebuffs.get(playerId);
        if (debuffs != null) {
            debuffs.remove(type);
        }
    }

    /**
     * 清除玩家所有debuff
     */
    public static void clearAllDebuffs(EntityPlayer player) {
        playerDebuffs.remove(player.getUniqueID());
    }

    /**
     * 获取debuff数据
     */
    public static DebuffData getDebuff(EntityPlayer player, DebuffType type) {
        UUID playerId = player.getUniqueID();
        Map<DebuffType, DebuffData> debuffs = playerDebuffs.get(playerId);
        return debuffs != null ? debuffs.get(type) : null;
    }

    /**
     * 检查是否有debuff
     */
    public static boolean hasDebuff(EntityPlayer player, DebuffType type) {
        return getDebuff(player, type) != null;
    }

    /**
     * 获取debuff层数
     */
    public static int getStacks(EntityPlayer player, DebuffType type) {
        DebuffData data = getDebuff(player, type);
        return data != null ? data.stacks : 0;
    }

    /**
     * 获取debuff强度（归一化 0.0-1.0）
     */
    public static float getIntensity(EntityPlayer player, DebuffType type) {
        DebuffData data = getDebuff(player, type);
        if (data == null) return 0.0f;
        return (float) data.stacks / type.getMaxStacks();
    }

    /**
     * 获取玩家所有debuff
     */
    public static Map<DebuffType, DebuffData> getAllDebuffs(EntityPlayer player) {
        return playerDebuffs.getOrDefault(player.getUniqueID(), new HashMap<>());
    }

    /**
     * 每tick更新所有debuff（减少持续时间）
     */
    public static void tickAll() {
        for (Map.Entry<UUID, Map<DebuffType, DebuffData>> playerEntry : playerDebuffs.entrySet()) {
            Map<DebuffType, DebuffData> debuffs = playerEntry.getValue();
            Iterator<Map.Entry<DebuffType, DebuffData>> iter = debuffs.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry<DebuffType, DebuffData> entry = iter.next();
                DebuffData data = entry.getValue();
                data.remainingTicks--;

                // 层数随时间自然衰减 - 独立于remainingTicks
                // 只要距离上次被命中超过衰减间隔，就减少层数
                DebuffType type = entry.getKey();
                if (type.getDecayInterval() > 0) {
                    data.ticksSinceLastHit++;
                    if (data.ticksSinceLastHit >= type.getDecayInterval()) {
                        data.stacks = Math.max(0, data.stacks - 1);
                        data.ticksSinceLastHit = 0; // 重置计时器，准备下一次衰减
                    }
                }

                // 移除过期或空层数的debuff
                if (data.remainingTicks <= 0 || data.stacks <= 0) {
                    iter.remove();
                }
            }

            // 清理空的玩家记录
            if (debuffs.isEmpty()) {
                playerDebuffs.remove(playerEntry.getKey());
            }
        }
    }

    /**
     * Debuff数据类
     */
    public static class DebuffData {
        public int stacks;
        public int remainingTicks;
        public int sourceEntityId;
        public int ticksSinceLastHit;

        public DebuffData(int stacks, int remainingTicks, int sourceEntityId) {
            this.stacks = stacks;
            this.remainingTicks = remainingTicks;
            this.sourceEntityId = sourceEntityId;
            this.ticksSinceLastHit = 0;
        }

        /**
         * 重置命中计时器（被再次命中时调用）
         */
        public void resetHitTimer() {
            this.ticksSinceLastHit = 0;
        }
    }
}
