package com.adversity.client;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端缓存 - 存储从服务端同步过来的怪物数据
 * 用于客户端渲染词条信息
 */
@SideOnly(Side.CLIENT)
public class ClientAdversityCache {

    /**
     * 缓存的实体数据
     * 注意：怪物词条数据在生成后不会改变，因此缓存不会过期
     * 只有当实体死亡或被卸载时才会清除缓存
     */
    public static class CachedEntityData {
        public final int tier;
        public final float difficultyLevel;
        public final float healthMultiplier;
        public final float damageMultiplier;
        public final List<ResourceLocation> affixIds;

        public CachedEntityData(int tier, float difficultyLevel, float healthMultiplier,
                                float damageMultiplier, List<ResourceLocation> affixIds) {
            this.tier = tier;
            this.difficultyLevel = difficultyLevel;
            this.healthMultiplier = healthMultiplier;
            this.damageMultiplier = damageMultiplier;
            this.affixIds = new ArrayList<>(affixIds);
        }
    }

    // 实体ID -> 缓存数据
    private static final Map<Integer, CachedEntityData> CACHE = new ConcurrentHashMap<>();

    /**
     * 更新实体数据
     */
    public static void updateEntity(int entityId, int tier, float difficultyLevel,
                                    float healthMultiplier, float damageMultiplier,
                                    List<ResourceLocation> affixIds) {
        CachedEntityData data = new CachedEntityData(tier, difficultyLevel,
                                                      healthMultiplier, damageMultiplier, affixIds);
        CACHE.put(entityId, data);
    }

    /**
     * 获取实体数据
     * 缓存不会过期，只有实体死亡/卸载时才会被清除
     */
    @Nullable
    public static CachedEntityData getEntityData(int entityId) {
        return CACHE.get(entityId);
    }

    /**
     * 移除实体数据（当实体被移除时）
     */
    public static void removeEntity(int entityId) {
        CACHE.remove(entityId);
    }

    /**
     * 清除所有缓存（如切换世界时）
     */
    public static void clearAll() {
        CACHE.clear();
    }

    /**
     * 获取缓存大小（调试用）
     */
    public static int getCacheSize() {
        return CACHE.size();
    }

    /**
     * 清理不在指定 ID 集合中的缓存（清理死亡/移除的实体）
     * 由 AdversityClientHandler 定期调用
     */
    public static void retainOnly(java.util.Set<Integer> validIds) {
        CACHE.keySet().retainAll(validIds);
    }

    /**
     * 检查实体是否在缓存中
     */
    public static boolean hasEntity(int entityId) {
        return CACHE.containsKey(entityId);
    }
}
